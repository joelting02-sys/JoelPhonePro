package com.joel.phonepro;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends Activity implements MultiThreadDownloader.Listener {
    private FrameLayout rootLayout;
    private WebView mainWebView;
    private MultiThreadDownloader downloader;

    // Native Web Media Sniffer overlay
    private LinearLayout snifferOverlay;
    private WebView snifferWebView;
    private EditText snifferUrlInput;
    private TextView snifferCountBadge;
    private final Set<String> seenSniffUrls = new HashSet<String>();

    private volatile boolean pageLoaded = false;
    private volatile String pendingSharedText = "";
    private volatile String currentSniffPageUrl = "";

    private static final String DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        downloader = new MultiThreadDownloader(this, this);
        captureSharedIntent(getIntent());

        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.parseColor("#F8F9FA"));

        mainWebView = new WebView(this);
        WebSettings ws = mainWebView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        mainWebView.addJavascriptInterface(new JoelNativeBridge(), "JoelNative");
        mainWebView.setWebChromeClient(new WebChromeClient());
        mainWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                pageLoaded = true;
                pushStateToJs();
                deliverPendingSharedText();
            }
        });

        rootLayout.addView(mainWebView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        buildSnifferOverlay();
        rootLayout.addView(snifferOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        setContentView(rootLayout);
        mainWebView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        captureSharedIntent(intent);
        deliverPendingSharedText();
    }

    private void captureSharedIntent(Intent intent) {
        if (intent == null) return;
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            String extra = intent.getStringExtra(Intent.EXTRA_TEXT);
            if (extra != null && !extra.trim().isEmpty()) {
                pendingSharedText = extra.trim();
            }
        }
    }

    private void deliverPendingSharedText() {
        final String text = pendingSharedText;
        if (!pageLoaded || text == null || text.isEmpty()) return;
        pendingSharedText = "";
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainWebView.evaluateJavascript(
                        "window.onExternalSharedText && window.onExternalSharedText("
                                + JSONObject.quote(text) + ");",
                        null);
            }
        });
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void buildSnifferOverlay() {
        snifferOverlay = new LinearLayout(this);
        snifferOverlay.setOrientation(LinearLayout.VERTICAL);
        snifferOverlay.setBackgroundColor(Color.WHITE);
        snifferOverlay.setVisibility(View.GONE);

        // Toolbar
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(10), dp(8), dp(10), dp(8));
        bar.setBackgroundColor(Color.WHITE);

        Button closeBtn = new Button(this);
        closeBtn.setText("←");
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        closeBtn.setTextColor(Color.parseColor("#0F172A"));
        closeBtn.setBackground(pillBg("#F3F4F6", 10));
        closeBtn.setPadding(dp(10), 0, dp(10), 0);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(dp(42), dp(38));
        closeLp.rightMargin = dp(8);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSnifferOverlay();
            }
        });
        bar.addView(closeBtn, closeLp);

        snifferUrlInput = new EditText(this);
        snifferUrlInput.setSingleLine(true);
        snifferUrlInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        snifferUrlInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        snifferUrlInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        snifferUrlInput.setTextColor(Color.parseColor("#0F172A"));
        snifferUrlInput.setHint("https://...");
        snifferUrlInput.setPadding(dp(10), dp(6), dp(10), dp(6));
        snifferUrlInput.setBackground(pillBg("#F8F9FA", 10));
        snifferUrlInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                    loadSnifferUrl(snifferUrlInput.getText().toString());
                    return true;
                }
                return false;
            }
        });
        LinearLayout.LayoutParams urlLp = new LinearLayout.LayoutParams(0, dp(38), 1f);
        urlLp.rightMargin = dp(8);
        bar.addView(snifferUrlInput, urlLp);

        snifferCountBadge = new TextView(this);
        snifferCountBadge.setText("0");
        snifferCountBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        snifferCountBadge.setTypeface(Typeface.DEFAULT_BOLD);
        snifferCountBadge.setTextColor(Color.WHITE);
        snifferCountBadge.setGravity(Gravity.CENTER);
        snifferCountBadge.setPadding(dp(10), dp(4), dp(10), dp(4));
        snifferCountBadge.setBackground(pillBg("#2563EB", 14));
        snifferCountBadge.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeSnifferOverlay();
                mainWebView.evaluateJavascript("window.switchTab && window.switchTab('sniffer');", null);
            }
        });
        LinearLayout.LayoutParams badgeLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(34));
        bar.addView(snifferCountBadge, badgeLp);

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#E5E7EB"));

        snifferWebView = new WebView(this);
        WebSettings sws = snifferWebView.getSettings();
        sws.setJavaScriptEnabled(true);
        sws.setDomStorageEnabled(true);
        sws.setMediaPlaybackRequiresUserGesture(true);
        sws.setUserAgentString(DESKTOP_UA);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(snifferWebView, true);

        snifferWebView.setWebChromeClient(new WebChromeClient());
        snifferWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                currentSniffPageUrl = url;
                if (snifferUrlInput != null) {
                    snifferUrlInput.setText(url);
                }
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    final String reqUrl = request.getUrl().toString();
                    if (isSniffableMediaOrArchive(reqUrl)) {
                        String pageUrl = currentSniffPageUrl != null ? currentSniffPageUrl : "";
                        String cookies = CookieManager.getInstance().getCookie(reqUrl);
                        if ((cookies == null || cookies.isEmpty()) && !pageUrl.isEmpty()) {
                            cookies = CookieManager.getInstance().getCookie(pageUrl);
                        }
                        reportSniffedResource(reqUrl, pageUrl, cookies != null ? cookies : "");
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        snifferOverlay.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        snifferOverlay.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        snifferOverlay.addView(snifferWebView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
    }

    private boolean isSniffableMediaOrArchive(String url) {
        if (url == null || !url.startsWith("http")) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        int q = lower.indexOf('?');
        String noQuery = q > 0 ? lower.substring(0, q) : lower;
        if (noQuery.endsWith(".mp4")
                || noQuery.endsWith(".mkv")
                || noQuery.endsWith(".mov")
                || noQuery.endsWith(".webm")
                || noQuery.endsWith(".mp3")
                || noQuery.endsWith(".m4a")
                || noQuery.endsWith(".wav")
                || noQuery.endsWith(".flac")
                || noQuery.endsWith(".apk")
                || noQuery.endsWith(".zip")
                || noQuery.endsWith(".pdf")) {
            return true;
        }
        return lower.contains(".mp4?") || lower.contains(".m4a?") || lower.contains("mime=video%2f");
    }

    private void reportSniffedResource(final String url, final String referer, final String cookie) {
        synchronized (seenSniffUrls) {
            if (!seenSniffUrls.add(url)) return;
        }
        final int count = seenSniffUrls.size();
        final String jsPayload;
        try {
            JSONObject obj = new JSONObject();
            obj.put("url", url);
            obj.put("referer", referer != null ? referer : "");
            obj.put("cookie", cookie != null ? cookie : "");
            obj.put("capturedAt", System.currentTimeMillis());
            jsPayload = obj.toString();
        } catch (JSONException e) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (snifferCountBadge != null) {
                    snifferCountBadge.setText(count + " ✓");
                }
                mainWebView.evaluateJavascript(
                        "window.onSniffedMedia && window.onSniffedMedia(" + jsPayload + ");",
                        null);
            }
        });
    }

    private void openSnifferOverlay(final String rawUrl) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (seenSniffUrls) {
                    seenSniffUrls.clear();
                }
                snifferCountBadge.setText("0");
                snifferOverlay.setVisibility(View.VISIBLE);
                loadSnifferUrl(rawUrl);
            }
        });
    }

    private void loadSnifferUrl(String rawUrl) {
        String u = rawUrl != null ? rawUrl.trim() : "";
        if (u.isEmpty()) return;
        if (!u.startsWith("http://") && !u.startsWith("https://")) {
            u = "https://" + u;
        }
        currentSniffPageUrl = u;
        snifferUrlInput.setText(u);
        snifferWebView.loadUrl(u);
    }

    private void closeSnifferOverlay() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                snifferWebView.stopLoading();
                snifferOverlay.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onTasksUpdated() {
        pushStateToJs();
    }

    private void pushStateToJs() {
        if (!pageLoaded || mainWebView == null) return;
        final String json = buildStateJson();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainWebView.evaluateJavascript(
                        "window.onNativeStateUpdate && window.onNativeStateUpdate(" + json + ");",
                        null);
            }
        });
    }

    private String buildStateJson() {
        JSONObject root = new JSONObject();
        try {
            root.put("tasks", new org.json.JSONArray(downloader.getTasksJsonString()));
            root.put("settings", downloader.getSettingsJson());
        } catch (JSONException ignored) {
        }
        return root.toString();
    }

    @Override
    public void onBackPressed() {
        if (snifferOverlay != null && snifferOverlay.getVisibility() == View.VISIBLE) {
            if (snifferWebView.canGoBack()) {
                snifferWebView.goBack();
            } else {
                closeSnifferOverlay();
            }
            return;
        }
        if (mainWebView != null) {
            mainWebView.evaluateJavascript(
                    "window.handleAndroidBack ? window.handleAndroidBack() : false",
                    new android.webkit.ValueCallback<String>() {
                        @Override
                        public void onReceiveValue(String value) {
                            if (!"true".equals(value)) {
                                moveTaskToBack(true);
                            }
                        }
                    });
            return;
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        if (downloader != null) {
            downloader.shutdown();
        }
        super.onDestroy();
    }

    private int dp(int n) {
        return (int) (n * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable pillBg(String hex, int radiusDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(Color.parseColor(hex));
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private class JoelNativeBridge {
        @JavascriptInterface
        public String getStateJson() {
            return buildStateJson();
        }

        @JavascriptInterface
        public String startDownload(String url, String fileName, int threads, String referer, String cookie, String uaMode) {
            if (url == null || url.trim().isEmpty()) {
                return "{\"ok\":false,\"error\":\"Empty URL\"}";
            }
            DownloadTask t = downloader.enqueue(url, fileName, threads, referer, cookie, uaMode);
            JSONObject res = new JSONObject();
            try {
                res.put("ok", true);
                res.put("id", t.id);
            } catch (JSONException ignored) {
            }
            return res.toString();
        }

        @JavascriptInterface
        public void pauseTask(String id) {
            downloader.pauseTask(id);
        }

        @JavascriptInterface
        public void resumeTask(String id) {
            downloader.resumeTask(id);
        }

        @JavascriptInterface
        public void retryTask(String id) {
            downloader.retryTask(id);
        }

        @JavascriptInterface
        public void deleteTask(String id, boolean deleteFile) {
            downloader.deleteTask(id, deleteFile);
        }

        @JavascriptInterface
        public void clearCompleted() {
            downloader.clearCompleted();
        }

        @JavascriptInterface
        public void saveSettings(String jsonPatch) {
            downloader.saveSettings(jsonPatch);
        }

        @JavascriptInterface
        public String getClipboardText() {
            final String[] result = new String[]{""};
            final java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(1);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                        if (cm != null && cm.hasPrimaryClip()) {
                            ClipData clip = cm.getPrimaryClip();
                            if (clip != null && clip.getItemCount() > 0) {
                                CharSequence cs = clip.getItemAt(0).coerceToText(MainActivity.this);
                                if (cs != null) result[0] = cs.toString();
                            }
                        }
                    } catch (Exception ignored) {
                    } finally {
                        latch.countDown();
                    }
                }
            });
            try {
                latch.await(1500, java.util.concurrent.TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
            }
            return result[0];
        }

        @JavascriptInterface
        public void setClipboardText(final String text) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                    if (cm != null) {
                        cm.setPrimaryClip(ClipData.newPlainText("url", text != null ? text : ""));
                    }
                }
            });
        }

        @JavascriptInterface
        public void openFile(final String id) {
            final DownloadTask t = downloader.findTask(id);
            if (t == null || t.publicUri == null || t.publicUri.isEmpty()) return;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Uri uri = Uri.parse(t.publicUri);
                        Intent viewIntent = new Intent(Intent.ACTION_VIEW);
                        viewIntent.setDataAndType(uri, t.mimeType != null && !t.mimeType.isEmpty() ? t.mimeType : "*/*");
                        viewIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(Intent.createChooser(viewIntent, t.fileName));
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "No app available to open file", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @JavascriptInterface
        public void shareFile(final String id) {
            final DownloadTask t = downloader.findTask(id);
            if (t == null || t.publicUri == null || t.publicUri.isEmpty()) return;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Uri uri = Uri.parse(t.publicUri);
                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.setType(t.mimeType != null && !t.mimeType.isEmpty() ? t.mimeType : "*/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(Intent.createChooser(shareIntent, t.fileName));
                    } catch (Exception e) {
                        Toast.makeText(MainActivity.this, "Unable to share file", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }

        @JavascriptInterface
        public void openBrowserSniffer(String url) {
            openSnifferOverlay(url);
        }
    }
}
