package com.joel.phonepro;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MultiThreadDownloader {
    public interface Listener {
        void onTasksUpdated();
    }

    private static final String PREFS_NAME = "joel_phone_pro_prefs";
    private static final String KEY_TASKS = "tasks_json";
    private static final String KEY_SETTINGS = "settings_json";

    private static final String UA_MOBILE =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36";
    private static final String UA_DESKTOP =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    private final Context context;
    private final Listener listener;
    private final List<DownloadTask> tasks = Collections.synchronizedList(new ArrayList<DownloadTask>());
    private final Object schedulerLock = new Object();

    private volatile int defaultThreads = 16;
    private volatile int concurrentLimit = 2;
    private volatile String defaultUaMode = "desktop";
    private volatile String lang = "zh";

    private volatile int activeRunningCount = 0;
    private final ScheduledExecutorService tickerExecutor;
    private volatile long lastPersistAt = 0L;

    public MultiThreadDownloader(Context context, Listener listener) {
        this.context = context.getApplicationContext();
        this.listener = listener;
        loadSettings();
        loadTasks();

        this.tickerExecutor = Executors.newSingleThreadScheduledExecutor();
        this.tickerExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                sampleActiveSpeeds();
            }
        }, 450, 450, TimeUnit.MILLISECONDS);
    }

    public void shutdown() {
        tickerExecutor.shutdownNow();
        synchronized (tasks) {
            for (DownloadTask t : tasks) {
                t.pauseRequested = true;
            }
        }
        persistTasks(true);
    }

    private void loadSettings() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_SETTINGS, "");
        if (raw == null || raw.isEmpty()) return;
        try {
            JSONObject obj = new JSONObject(raw);
            defaultThreads = clampThreads(obj.optInt("defaultThreads", 16));
            concurrentLimit = Math.max(1, Math.min(5, obj.optInt("concurrentLimit", 2)));
            String ua = obj.optString("defaultUaMode", "desktop");
            defaultUaMode = "mobile".equals(ua) ? "mobile" : "desktop";
            String l = obj.optString("lang", "zh");
            lang = "en".equals(l) ? "en" : "zh";
        } catch (JSONException ignored) {
        }
    }

    public void saveSettings(String jsonPatch) {
        if (jsonPatch == null || jsonPatch.trim().isEmpty()) return;
        try {
            JSONObject obj = new JSONObject(jsonPatch);
            if (obj.has("defaultThreads")) {
                defaultThreads = clampThreads(obj.optInt("defaultThreads", defaultThreads));
            }
            if (obj.has("concurrentLimit")) {
                concurrentLimit = Math.max(1, Math.min(5, obj.optInt("concurrentLimit", concurrentLimit)));
            }
            if (obj.has("defaultUaMode")) {
                String ua = obj.optString("defaultUaMode", defaultUaMode);
                defaultUaMode = "mobile".equals(ua) ? "mobile" : "desktop";
            }
            if (obj.has("lang")) {
                String l = obj.optString("lang", lang);
                lang = "en".equals(l) ? "en" : "zh";
            }
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(KEY_SETTINGS, getSettingsJson().toString()).apply();
            dispatchQueue();
            notifyUpdated();
        } catch (JSONException ignored) {
        }
    }

    public JSONObject getSettingsJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("defaultThreads", defaultThreads);
            obj.put("concurrentLimit", concurrentLimit);
            obj.put("defaultUaMode", defaultUaMode);
            obj.put("lang", lang);
            obj.put("saveLocationLabel", "Download/JoelPhonePro");
        } catch (JSONException ignored) {
        }
        return obj;
    }

    private void loadTasks() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String raw = prefs.getString(KEY_TASKS, "");
        if (raw == null || raw.isEmpty()) return;
        try {
            JSONArray arr = new JSONArray(raw);
            synchronized (tasks) {
                tasks.clear();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject o = arr.optJSONObject(i);
                    if (o != null) {
                        tasks.add(DownloadTask.fromJson(o));
                    }
                }
            }
        } catch (JSONException ignored) {
        }
    }

    private void persistTasks(boolean force) {
        long now = System.currentTimeMillis();
        if (!force && now - lastPersistAt < 1200) return;
        lastPersistAt = now;

        JSONArray arr = new JSONArray();
        synchronized (tasks) {
            for (DownloadTask t : tasks) {
                arr.put(t.toJson());
            }
        }
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_TASKS, arr.toString()).apply();
    }

    public String getTasksJsonString() {
        JSONArray arr = new JSONArray();
        synchronized (tasks) {
            for (DownloadTask t : tasks) {
                arr.put(t.toJson());
            }
        }
        return arr.toString();
    }

    public DownloadTask findTask(String id) {
        if (id == null) return null;
        synchronized (tasks) {
            for (DownloadTask t : tasks) {
                if (id.equals(t.id)) return t;
            }
        }
        return null;
    }

    public DownloadTask enqueue(String url, String fileName, int threads, String referer, String cookie, String uaMode) {
        DownloadTask task = new DownloadTask();
        task.id = "dl-" + System.currentTimeMillis() + "-" + ((int) (Math.random() * 900) + 100);
        task.url = url.trim();
        task.finalUrl = task.url;
        task.fileName = sanitizeFileName(fileName, inferFileNameFromUrl(task.url));
        task.threads = clampThreads(threads > 0 ? threads : defaultThreads);
        task.rangeSupported = task.threads > 1;
        if (task.threads > 1) {
            task.chunkStarts = new long[task.threads];
            task.chunkEnds = new long[task.threads];
            task.chunkDownloaded = new long[task.threads];
            for (int i = 0; i < task.threads; i++) {
                task.chunkStarts[i] = 0L;
                task.chunkEnds[i] = 99L;
                task.chunkDownloaded[i] = 0L;
            }
        }
        task.referer = referer != null ? referer.trim() : "";
        task.cookie = cookie != null ? cookie.trim() : "";
        String mode = (uaMode != null && !uaMode.isEmpty()) ? uaMode : defaultUaMode;
        task.userAgent = "mobile".equals(mode) ? UA_MOBILE : UA_DESKTOP;
        task.mimeType = inferMimeType(task.fileName);
        task.status = "queued";

        File partDir = getPartDir();
        task.tempPath = new File(partDir, "part_" + task.id + ".tmp").getAbsolutePath();

        synchronized (tasks) {
            tasks.add(0, task);
        }
        persistTasks(true);
        notifyUpdated();
        dispatchQueue();
        return task;
    }

    public void pauseTask(String id) {
        DownloadTask t = findTask(id);
        if (t == null) return;
        t.pauseRequested = true;
        if ("queued".equals(t.status)) {
            t.status = "paused";
            t.speedBps = 0L;
            t.etaSeconds = -1L;
            persistTasks(true);
            notifyUpdated();
        }
    }

    public void resumeTask(String id) {
        DownloadTask t = findTask(id);
        if (t == null) return;
        if ("downloading".equals(t.status) || "probing".equals(t.status) || "completed".equals(t.status)) return;
        t.pauseRequested = false;
        t.cancelRequested = false;
        t.error = "";
        t.speedBps = 0L;
        t.status = "queued";
        persistTasks(true);
        notifyUpdated();
        dispatchQueue();
    }

    public void retryTask(String id) {
        DownloadTask t = findTask(id);
        if (t == null) return;
        t.pauseRequested = false;
        t.cancelRequested = false;
        t.error = "";
        t.speedBps = 0L;
        t.etaSeconds = -1L;
        t.status = "queued";
        persistTasks(true);
        notifyUpdated();
        dispatchQueue();
    }

    public void deleteTask(String id, boolean deletePublicFile) {
        DownloadTask t = findTask(id);
        if (t == null) return;
        t.cancelRequested = true;
        synchronized (tasks) {
            tasks.remove(t);
        }
        if (t.tempPath != null && !t.tempPath.isEmpty()) {
            try {
                File f = new File(t.tempPath);
                if (f.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    f.delete();
                }
            } catch (Exception ignored) {
            }
        }
        if (deletePublicFile && t.publicUri != null && !t.publicUri.isEmpty()) {
            try {
                Uri uri = Uri.parse(t.publicUri);
                if ("content".equals(uri.getScheme())) {
                    context.getContentResolver().delete(uri, null, null);
                } else if ("file".equals(uri.getScheme()) && uri.getPath() != null) {
                    //noinspection ResultOfMethodCallIgnored
                    new File(uri.getPath()).delete();
                }
            } catch (Exception ignored) {
            }
        }
        persistTasks(true);
        notifyUpdated();
        dispatchQueue();
    }

    public void clearCompleted() {
        synchronized (tasks) {
            for (int i = tasks.size() - 1; i >= 0; i--) {
                if ("completed".equals(tasks.get(i).status)) {
                    tasks.remove(i);
                }
            }
        }
        persistTasks(true);
        notifyUpdated();
    }

    private void dispatchQueue() {
        synchronized (schedulerLock) {
            while (activeRunningCount < concurrentLimit) {
                DownloadTask next = null;
                synchronized (tasks) {
                    // Pick oldest queued task so FIFO order feels natural
                    for (int i = tasks.size() - 1; i >= 0; i--) {
                        DownloadTask candidate = tasks.get(i);
                        if ("queued".equals(candidate.status) && !candidate.cancelRequested && !candidate.pauseRequested) {
                            next = candidate;
                            break;
                        }
                    }
                }
                if (next == null) break;
                final DownloadTask taskToRun = next;
                taskToRun.status = "probing";
                activeRunningCount++;
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            executeTask(taskToRun);
                        } finally {
                            synchronized (schedulerLock) {
                                activeRunningCount = Math.max(0, activeRunningCount - 1);
                            }
                            dispatchQueue();
                        }
                    }
                }, "TaskRunner-" + taskToRun.id).start();
            }
        }
    }

    private final java.util.Map<String, Long> prevBytesMap = new java.util.HashMap<String, Long>();
    private long lastTickMs = System.currentTimeMillis();

    private void sampleActiveSpeeds() {
        long now = System.currentTimeMillis();
        long elapsedMs = Math.max(100L, now - lastTickMs);
        lastTickMs = now;
        boolean anyActive = false;

        synchronized (tasks) {
            for (DownloadTask t : tasks) {
                if ("downloading".equals(t.status)) {
                    anyActive = true;
                    if (t.rangeSupported && t.chunkDownloaded != null && t.chunkDownloaded.length > 0) {
                        t.recalculateDownloadedFromChunks();
                    }
                    long cur = t.downloadedBytes;
                    Long prev = prevBytesMap.get(t.id);
                    prevBytesMap.put(t.id, cur);
                    if (prev != null) {
                        long delta = Math.max(0L, cur - prev);
                        long instantBps = (delta * 1000L) / elapsedMs;
                        t.speedBps = t.speedBps > 0 ? (t.speedBps * 5 + instantBps * 5) / 10 : instantBps;
                        if (t.totalBytes > 0 && t.speedBps > 0 && t.downloadedBytes <= t.totalBytes) {
                            t.etaSeconds = (t.totalBytes - t.downloadedBytes) / t.speedBps;
                        } else {
                            t.etaSeconds = -1L;
                        }
                    }
                } else if ("probing".equals(t.status)) {
                    anyActive = true;
                } else {
                    prevBytesMap.remove(t.id);
                }
            }
        }

        if (anyActive) {
            persistTasks(false);
            notifyUpdated();
        }
    }

    private void executeTask(final DownloadTask task) {
        notifyUpdated();
        try {
            probeRemote(task);
            if (task.cancelRequested) return;
            if (task.pauseRequested) {
                finalizePaused(task);
                return;
            }

            File tempFile = new File(task.tempPath);
            if (!tempFile.getParentFile().exists()) {
                //noinspection ResultOfMethodCallIgnored
                tempFile.getParentFile().mkdirs();
            }

            boolean canMulti = task.rangeSupported && task.totalBytes > (256L * 1024L) && task.threads > 1;
            task.status = "downloading";
            notifyUpdated();

            if (canMulti) {
                runMultiThreadRange(task, tempFile);
            } else {
                task.threads = 1;
                runSingleStream(task, tempFile);
            }

            if (task.cancelRequested) {
                if (tempFile.exists()) {
                    //noinspection ResultOfMethodCallIgnored
                    tempFile.delete();
                }
                return;
            }
            if (task.pauseRequested) {
                finalizePaused(task);
                return;
            }

            // Export completed temp file to public Download/JoelPhonePro/
            exportToPublicDownloads(task, tempFile);
            task.speedBps = 0L;
            task.etaSeconds = 0L;
            if (task.totalBytes > 0) {
                task.downloadedBytes = task.totalBytes;
            }
            if (task.chunkStarts != null && task.chunkEnds != null && task.chunkDownloaded != null) {
                for (int i = 0; i < task.chunkStarts.length; i++) {
                    task.chunkDownloaded[i] = task.chunkEnds[i] - task.chunkStarts[i] + 1L;
                }
            }
            task.status = "completed";
            persistTasks(true);
            notifyUpdated();
        } catch (Exception ex) {
            if (task.cancelRequested) return;
            if (task.pauseRequested) {
                finalizePaused(task);
                return;
            }
            task.speedBps = 0L;
            task.etaSeconds = -1L;
            task.status = "error";
            task.error = ex.getMessage() != null && !ex.getMessage().isEmpty()
                    ? ex.getMessage()
                    : ex.getClass().getSimpleName();
            persistTasks(true);
            notifyUpdated();
        }
    }

    private void finalizePaused(DownloadTask task) {
        task.speedBps = 0L;
        task.etaSeconds = -1L;
        if (task.rangeSupported) {
            task.recalculateDownloadedFromChunks();
        }
        task.status = "paused";
        persistTasks(true);
        notifyUpdated();
    }

    private HttpURLConnection openConnectionFollowRedirects(
            String rawUrl, String referer, String cookie, String userAgent, String rangeHeader) throws IOException {
        String currentUrl = rawUrl;

        for (int redirect = 0; redirect < 6; redirect++) {
            HttpURLConnection conn = (HttpURLConnection) new URL(currentUrl).openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(20000);
            conn.setRequestMethod("GET");
            if (userAgent != null && !userAgent.isEmpty()) {
                conn.setRequestProperty("User-Agent", userAgent);
            }
            if (referer != null && !referer.isEmpty()) {
                conn.setRequestProperty("Referer", referer);
            }
            if (cookie != null && !cookie.isEmpty()) {
                conn.setRequestProperty("Cookie", cookie);
            }
            conn.setRequestProperty("Accept", "*/*");
            conn.setRequestProperty("Accept-Encoding", "identity");
            if (rangeHeader != null && !rangeHeader.isEmpty()) {
                conn.setRequestProperty("Range", rangeHeader);
            }
            conn.connect();

            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_MOVED_PERM
                    || code == HttpURLConnection.HTTP_MOVED_TEMP
                    || code == HttpURLConnection.HTTP_SEE_OTHER
                    || code == 307
                    || code == 308) {
                String loc = conn.getHeaderField("Location");
                conn.disconnect();
                if (loc == null || loc.isEmpty()) {
                    throw new IOException("Redirect without Location header");
                }
                currentUrl = new URL(new URL(currentUrl), loc).toString();
                continue;
            }
            return conn;
        }
        throw new IOException("Too many redirects");
    }

    private void applyProbeHeaders(DownloadTask task, HttpURLConnection conn, boolean isSingleByteProbe) {
        int code;
        try {
            code = conn.getResponseCode();
        } catch (IOException e) {
            return;
        }
        task.finalUrl = conn.getURL().toString();
        String contentDisposition = conn.getHeaderField("Content-Disposition");
        String serverName = extractFileNameFromDisposition(contentDisposition);
        if (serverName != null && !serverName.isEmpty() && isGenericFileName(task.fileName)) {
            task.fileName = sanitizeFileName(serverName, task.fileName);
        }
        String contentType = conn.getContentType();
        if (contentType != null && !contentType.isEmpty()) {
            int semi = contentType.indexOf(';');
            String cleanMime = (semi > 0 ? contentType.substring(0, semi) : contentType).trim();
            if (!cleanMime.isEmpty() && !"application/octet-stream".equalsIgnoreCase(cleanMime)) {
                task.mimeType = cleanMime;
            }
        }

        String acceptRanges = conn.getHeaderField("Accept-Ranges");
        String contentRange = conn.getHeaderField("Content-Range");
        long headerContentLength = parseLongHeader(conn.getHeaderField("Content-Length"));

        if (code == 206) {
            task.rangeSupported = true;
            long totalFromRange = parseTotalFromContentRange(contentRange);
            if (totalFromRange > 0) {
                task.totalBytes = totalFromRange;
            } else if (!isSingleByteProbe && headerContentLength > 0) {
                task.totalBytes = headerContentLength;
            } else if (headerContentLength > 1) {
                task.totalBytes = headerContentLength;
            }
        } else {
            if (headerContentLength > 0) {
                task.totalBytes = headerContentLength;
            }
            task.rangeSupported = "bytes".equalsIgnoreCase(acceptRanges) && task.totalBytes > 0;
        }
    }

    private void probeRemote(DownloadTask task) throws IOException {
        HttpURLConnection conn = openConnectionFollowRedirects(
                task.url, task.referer, task.cookie, task.userAgent, "bytes=0-0");
        try {
            int code = conn.getResponseCode();
            if (code >= 400) {
                // Retry once without Range in case upstream rejects Range header cleanly
                conn.disconnect();
                conn = openConnectionFollowRedirects(
                        task.url, task.referer, task.cookie, task.userAgent, null);
                code = conn.getResponseCode();
            }
            if (code < 200 || code >= 400) {
                throw new IOException("HTTP " + code + " " + conn.getResponseMessage());
            }
            applyProbeHeaders(task, conn, true);
        } finally {
            try {
                InputStream in = conn.getInputStream();
                if (in != null) in.close();
            } catch (Exception ignored) {
            }
            conn.disconnect();
        }

        // Fallback probe with Range: bytes=0- if server returned 206 without total in Content-Range
        if (task.totalBytes <= 0) {
            HttpURLConnection openEndedConn = null;
            try {
                openEndedConn = openConnectionFollowRedirects(
                        task.url, task.referer, task.cookie, task.userAgent, "bytes=0-");
                int fallbackCode = openEndedConn.getResponseCode();
                if (fallbackCode >= 200 && fallbackCode < 400) {
                    applyProbeHeaders(task, openEndedConn, false);
                }
            } catch (Exception ignored) {
            } finally {
                if (openEndedConn != null) {
                    openEndedConn.disconnect();
                }
            }
        }
    }

    private void runMultiThreadRange(final DownloadTask task, File tempFile) throws Exception {
        int n = task.threads;
        boolean canReuseExistingChunks =
                task.chunkStarts != null
                        && task.chunkEnds != null
                        && task.chunkDownloaded != null
                        && task.chunkStarts.length == n
                        && tempFile.exists()
                        && tempFile.length() == task.totalBytes;

        if (!canReuseExistingChunks) {
            task.chunkStarts = new long[n];
            task.chunkEnds = new long[n];
            task.chunkDownloaded = new long[n];
            long partSize = task.totalBytes / n;
            for (int i = 0; i < n; i++) {
                long start = i * partSize;
                long end = (i == n - 1) ? (task.totalBytes - 1L) : ((i + 1) * partSize - 1L);
                task.chunkStarts[i] = start;
                task.chunkEnds[i] = end;
                task.chunkDownloaded[i] = 0L;
            }
            task.downloadedBytes = 0L;
        }
        notifyUpdated();

        final RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
        raf.setLength(task.totalBytes);
        final FileChannel channel = raf.getChannel();

        ExecutorService pool = Executors.newFixedThreadPool(n);
        final CountDownLatch latch = new CountDownLatch(n);
        final AtomicReference<Exception> firstError = new AtomicReference<Exception>(null);

        try {
            for (int idx = 0; idx < n; idx++) {
                final int chunkIdx = idx;
                pool.submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            downloadChunkWithRetry(task, channel, chunkIdx);
                        } catch (Exception e) {
                            firstError.compareAndSet(null, e);
                            task.pauseRequested = true; // Abort remaining chunks cleanly
                        } finally {
                            latch.countDown();
                        }
                    }
                });
            }
            latch.await();
        } finally {
            pool.shutdownNow();
            try {
                channel.close();
            } catch (Exception ignored) {
            }
            try {
                raf.close();
            } catch (Exception ignored) {
            }
        }

        Exception err = firstError.get();
        if (err != null) {
            task.pauseRequested = false;
            throw err;
        }
        task.recalculateDownloadedFromChunks();
    }

    private void downloadChunkWithRetry(DownloadTask task, FileChannel channel, int chunkIdx) throws Exception {
        long totalChunkSize = task.chunkEnds[chunkIdx] - task.chunkStarts[chunkIdx] + 1L;
        if (task.chunkDownloaded[chunkIdx] >= totalChunkSize) {
            return;
        }

        Exception lastErr = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (task.cancelRequested || task.pauseRequested) return;
            try {
                downloadSingleChunk(task, channel, chunkIdx);
                return;
            } catch (Exception e) {
                lastErr = e;
                if (task.cancelRequested || task.pauseRequested) return;
                if (attempt < 3) {
                    try {
                        Thread.sleep(700L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
        throw lastErr != null ? lastErr : new IOException("Chunk " + chunkIdx + " failed");
    }

    private void downloadSingleChunk(DownloadTask task, FileChannel channel, int chunkIdx) throws IOException {
        long start = task.chunkStarts[chunkIdx] + task.chunkDownloaded[chunkIdx];
        long end = task.chunkEnds[chunkIdx];
        if (start > end) return;

        String rangeHeader = "bytes=" + start + "-" + end;
        HttpURLConnection conn = openConnectionFollowRedirects(
                task.finalUrl, task.referer, task.cookie, task.userAgent, rangeHeader);
        InputStream in = null;
        try {
            int code = conn.getResponseCode();
            if (code != 206 && !(code == 200 && start == 0)) {
                throw new IOException("Chunk rejected Range HTTP " + code);
            }
            in = conn.getInputStream();
            byte[] buf = new byte[32 * 1024];
            long writeOffset = start;
            long remaining = end - start + 1L;

            while (remaining > 0 && !task.cancelRequested && !task.pauseRequested) {
                int toRead = (int) Math.min(buf.length, remaining);
                int read = in.read(buf, 0, toRead);
                if (read == -1) break;
                ByteBuffer bb = ByteBuffer.wrap(buf, 0, read);
                while (bb.hasRemaining()) {
                    writeOffset += channel.write(bb, writeOffset);
                }
                task.chunkDownloaded[chunkIdx] += read;
                remaining -= read;
            }

            if (remaining > 0 && !task.cancelRequested && !task.pauseRequested) {
                throw new IOException("Unexpected EOF at chunk " + chunkIdx + " (remaining " + remaining + "B)");
            }
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
            conn.disconnect();
        }
    }

    private void runSingleStream(DownloadTask task, File tempFile) throws IOException {
        task.chunkStarts = new long[]{0L};
        task.chunkEnds = new long[]{Math.max(0L, task.totalBytes - 1L)};
        task.chunkDownloaded = new long[]{0L};
        task.downloadedBytes = 0L;

        HttpURLConnection conn = openConnectionFollowRedirects(
                task.finalUrl, task.referer, task.cookie, task.userAgent, null);
        InputStream in = null;
        FileOutputStream out = null;
        try {
            int code = conn.getResponseCode();
            if (code < 200 || code >= 400) {
                throw new IOException("HTTP " + code);
            }
            long len = parseLongHeader(conn.getHeaderField("Content-Length"));
            if (len > 0 && task.totalBytes <= 0) {
                task.totalBytes = len;
                task.chunkEnds[0] = len - 1L;
            }
            in = conn.getInputStream();
            out = new FileOutputStream(tempFile, false);
            byte[] buf = new byte[32 * 1024];
            int read;
            while (!task.cancelRequested && !task.pauseRequested && (read = in.read(buf)) != -1) {
                out.write(buf, 0, read);
                task.downloadedBytes += read;
                task.chunkDownloaded[0] = task.downloadedBytes;
            }
            out.flush();
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (Exception ignored) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (Exception ignored) {
                }
            }
            conn.disconnect();
        }
    }

    private void exportToPublicDownloads(DownloadTask task, File tempFile) throws IOException {
        if (!tempFile.exists()) {
            throw new IOException("Temp file missing after download");
        }
        String finalMime = task.mimeType != null && !task.mimeType.isEmpty()
                ? task.mimeType
                : inferMimeType(task.fileName);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContentResolver resolver = context.getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.Downloads.DISPLAY_NAME, task.fileName);
            values.put(MediaStore.Downloads.MIME_TYPE, finalMime);
            values.put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/JoelPhonePro");
            values.put(MediaStore.Downloads.IS_PENDING, 1);

            Uri itemUri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (itemUri == null) {
                throw new IOException("Failed to create MediaStore download entry");
            }
            try {
                OutputStream os = resolver.openOutputStream(itemUri);
                if (os == null) throw new IOException("Cannot open output stream");
                copyFileToStream(tempFile, os);
                values.clear();
                values.put(MediaStore.Downloads.IS_PENDING, 0);
                resolver.update(itemUri, values, null, null);
                task.publicUri = itemUri.toString();
                task.publicDisplayPath = "Download/JoelPhonePro/" + task.fileName;
            } catch (IOException e) {
                resolver.delete(itemUri, null, null);
                throw e;
            }
        } else {
            File pubDir = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    "JoelPhonePro");
            if (!pubDir.exists() && !pubDir.mkdirs()) {
                throw new IOException("Cannot create Download/JoelPhonePro directory");
            }
            File destFile = uniqueDestFile(pubDir, task.fileName);
            OutputStream os = new FileOutputStream(destFile);
            copyFileToStream(tempFile, os);
            task.fileName = destFile.getName();
            task.publicDisplayPath = "Download/JoelPhonePro/" + destFile.getName();

            final AtomicReference<Uri> scannedUri = new AtomicReference<Uri>(Uri.fromFile(destFile));
            final CountDownLatch scanLatch = new CountDownLatch(1);
            MediaScannerConnection.scanFile(
                    context,
                    new String[]{destFile.getAbsolutePath()},
                    new String[]{finalMime},
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                            if (uri != null) {
                                scannedUri.set(uri);
                            }
                            scanLatch.countDown();
                        }
                    });
            try {
                scanLatch.await(3, TimeUnit.SECONDS);
            } catch (InterruptedException ignored) {
            }
            task.publicUri = scannedUri.get().toString();
        }

        // Delete temp file only after public export succeeded
        //noinspection ResultOfMethodCallIgnored
        tempFile.delete();
        task.tempPath = "";
    }

    private static void copyFileToStream(File src, OutputStream os) throws IOException {
        FileInputStream fis = new FileInputStream(src);
        try {
            byte[] buf = new byte[64 * 1024];
            int r;
            while ((r = fis.read(buf)) != -1) {
                os.write(buf, 0, r);
            }
            os.flush();
        } finally {
            try {
                fis.close();
            } catch (Exception ignored) {
            }
            try {
                os.close();
            } catch (Exception ignored) {
            }
        }
    }

    private File getPartDir() {
        File base = context.getExternalFilesDir(null);
        if (base == null) base = context.getFilesDir();
        File partDir = new File(base, "parts");
        if (!partDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            partDir.mkdirs();
        }
        return partDir;
    }

    private File uniqueDestFile(File dir, String name) {
        File f = new File(dir, name);
        if (!f.exists()) return f;
        int dot = name.lastIndexOf('.');
        String stem = dot > 0 ? name.substring(0, dot) : name;
        String ext = dot > 0 ? name.substring(dot) : "";
        for (int i = 1; i < 100; i++) {
            File cand = new File(dir, stem + " (" + i + ")" + ext);
            if (!cand.exists()) return cand;
        }
        return new File(dir, stem + "_" + System.currentTimeMillis() + ext);
    }

    private void notifyUpdated() {
        if (listener != null) {
            listener.onTasksUpdated();
        }
    }

    private static int clampThreads(int threads) {
        if (threads >= 32) return 32;
        if (threads >= 16) return 16;
        if (threads >= 8) return 8;
        if (threads >= 4) return 4;
        return 1;
    }

    private static long parseLongHeader(String v) {
        if (v == null || v.isEmpty()) return -1L;
        try {
            return Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return -1L;
        }
    }

    private static long parseTotalFromContentRange(String header) {
        if (header == null || header.isEmpty()) return -1L;
        int slash = header.lastIndexOf('/');
        if (slash < 0 || slash >= header.length() - 1) return -1L;
        String totalStr = header.substring(slash + 1).trim();
        if ("*".equals(totalStr)) return -1L;
        return parseLongHeader(totalStr);
    }

    private static String extractFileNameFromDisposition(String cd) {
        if (cd == null || cd.isEmpty()) return null;
        try {
            Matcher mStar = Pattern.compile("filename\\*=\\s*(?:UTF-8''|utf-8'')([^;\\r\\n]+)").matcher(cd);
            if (mStar.find()) {
                return URLDecoder.decode(mStar.group(1).trim().replace("\"", ""), "UTF-8");
            }
            Matcher mPlain = Pattern.compile("filename\\s*=\\s*\"?([^\";\\r\\n]+)\"?").matcher(cd);
            if (mPlain.find()) {
                return URLDecoder.decode(mPlain.group(1).trim(), "UTF-8");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static boolean isGenericFileName(String name) {
        if (name == null || name.trim().isEmpty()) return true;
        String lower = name.toLowerCase(Locale.ROOT);
        return "download.bin".equals(lower)
                || "download_package.bin".equals(lower)
                || "extracted_resource.zip".equals(lower);
    }

    private static String inferFileNameFromUrl(String rawUrl) {
        try {
            String path = Uri.parse(rawUrl).getLastPathSegment();
            if (path != null && !path.trim().isEmpty()) {
                return URLDecoder.decode(path, "UTF-8");
            }
        } catch (Exception ignored) {
        }
        return "download_" + (System.currentTimeMillis() % 100000) + ".bin";
    }

    private static String sanitizeFileName(String raw, String fallback) {
        String candidate = (raw != null && !raw.trim().isEmpty()) ? raw.trim() : fallback;
        candidate = candidate.replaceAll("[\\\\/:*?\"<>|\\r\\n]+", "_").trim();
        if (candidate.isEmpty()) candidate = "download.bin";
        if (candidate.length() > 140) {
            int dot = candidate.lastIndexOf('.');
            if (dot > 0 && candidate.length() - dot <= 12) {
                String ext = candidate.substring(dot);
                candidate = candidate.substring(0, 140 - ext.length()) + ext;
            } else {
                candidate = candidate.substring(0, 140);
            }
        }
        return candidate;
    }

    private static String inferMimeType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        int dot = fileName.lastIndexOf('.');
        if (dot >= 0 && dot < fileName.length() - 1) {
            String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
            if ("mp4".equals(ext)) return "video/mp4";
            if ("mkv".equals(ext)) return "video/x-matroska";
            if ("webm".equals(ext)) return "video/webm";
            if ("mov".equals(ext)) return "video/quicktime";
            if ("mp3".equals(ext)) return "audio/mpeg";
            if ("m4a".equals(ext)) return "audio/mp4";
            if ("wav".equals(ext)) return "audio/wav";
            if ("flac".equals(ext)) return "audio/flac";
            if ("pdf".equals(ext)) return "application/pdf";
            if ("apk".equals(ext)) return "application/vnd.android.package-archive";
            if ("zip".equals(ext)) return "application/zip";
            String guessed = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            if (guessed != null && !guessed.isEmpty()) return guessed;
        }
        return "application/octet-stream";
    }
}
