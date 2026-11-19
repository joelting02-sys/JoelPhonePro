package com.joel.phonepro;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DownloadTask {
    public String id;
    public String url;
    public String finalUrl;
    public String fileName;
    public String referer;
    public String cookie;
    public String userAgent;
    public String mimeType;

    public int threads;
    public boolean rangeSupported;
    public volatile long totalBytes;
    public volatile long downloadedBytes;
    public volatile long speedBps;
    public volatile long etaSeconds;

    // queued | probing | downloading | paused | completed | error
    public volatile String status;
    public volatile String error;
    public long createdAt;

    public String tempPath;
    public String publicUri;
    public String publicDisplayPath;

    public long[] chunkStarts = new long[0];
    public long[] chunkEnds = new long[0];
    public long[] chunkDownloaded = new long[0];

    public volatile boolean cancelRequested;
    public volatile boolean pauseRequested;

    public DownloadTask() {
        this.status = "queued";
        this.createdAt = System.currentTimeMillis();
        this.etaSeconds = -1;
    }

    public synchronized void recalculateDownloadedFromChunks() {
        if (chunkDownloaded == null || chunkDownloaded.length == 0) return;
        long sum = 0;
        for (long v : chunkDownloaded) {
            sum += Math.max(0L, v);
        }
        this.downloadedBytes = totalBytes > 0 ? Math.min(totalBytes, sum) : sum;
    }

    public synchronized JSONObject toJson() {
        JSONObject obj = new JSONObject();
        try {
            obj.put("id", nonNull(id));
            obj.put("url", nonNull(url));
            obj.put("finalUrl", nonNull(finalUrl));
            obj.put("fileName", nonNull(fileName));
            obj.put("referer", nonNull(referer));
            obj.put("cookie", nonNull(cookie));
            obj.put("userAgent", nonNull(userAgent));
            obj.put("mimeType", nonNull(mimeType));
            obj.put("threads", threads);
            obj.put("rangeSupported", rangeSupported);
            obj.put("totalBytes", totalBytes);
            obj.put("downloadedBytes", downloadedBytes);
            obj.put("speedBps", speedBps);
            obj.put("etaSeconds", etaSeconds);
            obj.put("status", nonNull(status));
            obj.put("error", nonNull(error));
            obj.put("createdAt", createdAt);
            obj.put("tempPath", nonNull(tempPath));
            obj.put("publicUri", nonNull(publicUri));
            obj.put("publicDisplayPath", nonNull(publicDisplayPath));

            JSONArray chunksPct = new JSONArray();
            JSONArray startsArr = new JSONArray();
            JSONArray endsArr = new JSONArray();
            JSONArray downArr = new JSONArray();
            if (chunkStarts != null && chunkEnds != null && chunkDownloaded != null) {
                for (int i = 0; i < chunkStarts.length; i++) {
                    long len = Math.max(1L, chunkEnds[i] - chunkStarts[i] + 1L);
                    long dl = i < chunkDownloaded.length ? chunkDownloaded[i] : 0L;
                    int pct = (int) Math.min(100L, Math.max(0L, (dl * 100L) / len));
                    chunksPct.put(pct);
                    startsArr.put(chunkStarts[i]);
                    endsArr.put(chunkEnds[i]);
                    downArr.put(dl);
                }
            }
            obj.put("chunks", chunksPct);
            obj.put("chunkStarts", startsArr);
            obj.put("chunkEnds", endsArr);
            obj.put("chunkDownloaded", downArr);
        } catch (JSONException ignored) {
        }
        return obj;
    }

    public static DownloadTask fromJson(JSONObject obj) {
        DownloadTask t = new DownloadTask();
        t.id = obj.optString("id", "");
        t.url = obj.optString("url", "");
        t.finalUrl = obj.optString("finalUrl", t.url);
        t.fileName = obj.optString("fileName", "download.bin");
        t.referer = obj.optString("referer", "");
        t.cookie = obj.optString("cookie", "");
        t.userAgent = obj.optString("userAgent", "");
        t.mimeType = obj.optString("mimeType", "application/octet-stream");
        t.threads = Math.max(1, obj.optInt("threads", 16));
        t.rangeSupported = obj.optBoolean("rangeSupported", false);
        t.totalBytes = Math.max(0L, obj.optLong("totalBytes", 0L));
        t.downloadedBytes = Math.max(0L, obj.optLong("downloadedBytes", 0L));
        t.speedBps = 0L;
        t.etaSeconds = -1L;

        String rawStatus = obj.optString("status", "paused");
        if ("downloading".equals(rawStatus) || "probing".equals(rawStatus) || "queued".equals(rawStatus)) {
            t.status = "paused";
        } else {
            t.status = rawStatus;
        }
        t.error = obj.optString("error", "");
        t.createdAt = obj.optLong("createdAt", System.currentTimeMillis());
        t.tempPath = obj.optString("tempPath", "");
        t.publicUri = obj.optString("publicUri", "");
        t.publicDisplayPath = obj.optString("publicDisplayPath", "");

        JSONArray startsArr = obj.optJSONArray("chunkStarts");
        JSONArray endsArr = obj.optJSONArray("chunkEnds");
        JSONArray downArr = obj.optJSONArray("chunkDownloaded");
        if (startsArr != null && endsArr != null && downArr != null
                && startsArr.length() == endsArr.length()
                && startsArr.length() == downArr.length()) {
            int n = startsArr.length();
            t.chunkStarts = new long[n];
            t.chunkEnds = new long[n];
            t.chunkDownloaded = new long[n];
            for (int i = 0; i < n; i++) {
                t.chunkStarts[i] = startsArr.optLong(i, 0L);
                t.chunkEnds[i] = endsArr.optLong(i, 0L);
                t.chunkDownloaded[i] = downArr.optLong(i, 0L);
            }
        }
        return t;
    }

    private static String nonNull(String s) {
        return s == null ? "" : s;
    }
}
