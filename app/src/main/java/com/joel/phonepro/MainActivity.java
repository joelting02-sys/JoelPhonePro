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
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity implements MultiThreadDownloader.Listener {
    private static final String DESKTOP_UA =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36";
    private static final String MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Mobile Safari/537.36";

    private static final Pattern URL_EXTRACT_PATTERN = Pattern.compile(
            "https?://[A-Za-z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DOMAIN_LIKE_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$");
    private static final Pattern CONTENT_RANGE_TOTAL_PATTERN = Pattern.compile("/(\\d+)$");
    private static final Pattern DISPOSITION_FILENAME_PATTERN = Pattern.compile(
            "filename\\*?=(?:UTF-8''|\"?)([^\";]+)",
            Pattern.CASE_INSENSITIVE);

    private FrameLayout rootLayout;
    private WebView mainWebView;
    private MultiThreadDownloader downloader;

    // Native Web Media Sniffer overlay
    private LinearLayout snifferOverlay;
    private WebView snifferWebView;
    private EditText snifferUrlInput;
    private Button snifferUaBtn;
    private TextView snifferCountBadge;

    private final Object sniffLock = new Object();
    private final Set<String> seenSniffUrls = new HashSet<String>();
    private final ConcurrentHashMap<String, SniffedResource> sniffedById =
            new ConcurrentHashMap<String, SniffedResource>();
    private final Map<String, SniffedResource> sniffedByUrl = new HashMap<String, SniffedResource>();
    private final AtomicInteger sniffIdSeq = new AtomicInteger(0);
    private final ExecutorService probeExecutor = Executors.newFixedThreadPool(3);

    private LocalPreviewProxy previewProxy;
    private volatile int previewProxyPort = 0;

    private volatile boolean pageLoaded = false;
    private volatile String pendingSharedText = "";
    private volatile String currentSniffPageUrl = "";
    private volatile boolean snifferUseMobileUa = false;

    private static class SniffedResource {
        final String id;
        final String url;
        final String referer;
        final String cookie;
        final String userAgent;
        final String uaMode;
        volatile String source;
        volatile String mediaType;
        volatile String title;
        volatile String contentType;
        volatile String suggestedName;
        volatile long sizeBytes;
        volatile int width;
        volatile int height;
        volatile double durationSec;
        final long capturedAt;

        SniffedResource(
                String id,
                String url,
                String referer,
                String cookie,
                String userAgent,
                String uaMode,
                String source,
                String mediaType,
                String title,
                int width,
                int height,
                double durationSec) {
            this.id = id;
            this.url = url;
            this.referer = referer;
            this.cookie = cookie;
            this.userAgent = userAgent;
            this.uaMode = uaMode;
            this.source = source;
            this.mediaType = mediaType;
            this.title = title;
            this.contentType = "";
            this.suggestedName = "";
            this.sizeBytes = -1L;
            this.width = width;
            this.height = height;
            this.durationSec = durationSec;
            this.capturedAt = System.currentTimeMillis();
        }

        JSONObject toJson(int proxyPort) throws JSONException {
            JSONObject obj = new JSONObject();
            obj.put("id", id);
            obj.put("url", url);
            obj.put("referer", referer != null ? referer : "");
            obj.put("cookie", cookie != null ? cookie : "");
            obj.put("uaMode", uaMode != null ? uaMode : "desktop");
            obj.put("source", source != null ? source : "NET");
            obj.put("mediaType", mediaType != null ? mediaType : "video");
            obj.put("title", title != null ? title : "");
            obj.put("contentType", contentType != null ? contentType : "");
            obj.put("suggestedName", suggestedName != null ? suggestedName : "");
            obj.put("sizeBytes", sizeBytes);
            obj.put("width", width);
            obj.put("height", height);
            obj.put("durationSec", durationSec);
            obj.put("capturedAt", capturedAt);
            if (proxyPort > 0) {
                try {
                    obj.put("previewUrl",
                            "http://127.0.0.1:" + proxyPort + "/preview?id=" + URLEncoder.encode(id, "UTF-8"));
                } catch (Exception ignored) {
                    obj.put("previewUrl", url);
                }
            } else {
                obj.put("previewUrl", url);
            }
            return obj;
        }
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        downloader = new MultiThreadDownloader(this, this);
        startPreviewProxy();
        captureSharedIntent(getIntent());

        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.WHITE);
        applySystemBarInsets();

        mainWebView = new WebView(this);
        WebSettings ws = mainWebView.getSettings();
        ws.setJavaScriptEnabled(true);
        ws.setDomStorageEnabled(true);
        ws.setAllowFileAccess(true);
        ws.setAllowContentAccess(true);
        ws.setMediaPlaybackRequiresUserGesture(false);
        ws.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
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
        configureSystemBarAppearance();
        rootLayout.requestApplyInsets();
        mainWebView.loadUrl("file:///android_asset/index.html");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        captureSharedIntent(intent);
        deliverPendingSharedText();
    }

    private void applySystemBarInsets() {
        rootLayout.setPadding(0, getFallbackStatusBarHeight(), 0, 0);
        rootLayout.setOnApplyWindowInsetsListener(new View.OnApplyWindowInsetsListener() {
            @Override
            public WindowInsets onApplyWindowInsets(View v, WindowInsets insets) {
                int left = 0;
                int top = 0;
                int right = 0;
                int bottom = 0;
                if (Build.VERSION.SDK_INT >= 30) {
                    android.graphics.Insets sys = insets.getInsets(
                            WindowInsets.Type.systemBars() | WindowInsets.Type.displayCutout());
                    android.graphics.Insets ime = insets.getInsets(WindowInsets.Type.ime());
                    left = sys.left;
                    top = sys.top;
                    right = sys.right;
                    bottom = Math.max(sys.bottom, ime.bottom);
                } else {
                    left = insets.getSystemWindowInsetLeft();
                    top = insets.getSystemWindowInsetTop();
                    right = insets.getSystemWindowInsetRight();
                    bottom = insets.getSystemWindowInsetBottom();
                }
                if (top <= 0) {
                    top = getFallbackStatusBarHeight();
                }
                v.setPadding(left, top, right, bottom);
                return insets;
            }
        });
    }

    private int getFallbackStatusBarHeight() {
        int resId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resId > 0) {
            int h = getResources().getDimensionPixelSize(resId);
            if (h > 0) return h;
        }
        return dp(24);
    }

    @SuppressWarnings("deprecation")
    private void configureSystemBarAppearance() {
        Window window = getWindow();
        if (window == null) return;
        if (Build.VERSION.SDK_INT < 35) {
            window.setStatusBarColor(Color.WHITE);
            window.setNavigationBarColor(Color.WHITE);
        }
        View decor = window.getDecorView();
        if (Build.VERSION.SDK_INT >= 30) {
            WindowInsetsController ic = decor != null ? decor.getWindowInsetsController() : window.getInsetsController();
            if (ic != null) {
                int mask = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                        | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
                ic.setSystemBarsAppearance(mask, mask);
            }
        } else if (decor != null) {
            int flags = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
            if (Build.VERSION.SDK_INT >= 26) {
                flags |= View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR;
            }
            decor.setSystemUiVisibility(flags);
        }
    }

    private void startPreviewProxy() {
        previewProxy = new LocalPreviewProxy();
        previewProxyPort = previewProxy.start();
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

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface"})
    private void buildSnifferOverlay() {
        snifferOverlay = new LinearLayout(this);
        snifferOverlay.setOrientation(LinearLayout.VERTICAL);
        snifferOverlay.setBackgroundColor(Color.WHITE);
        snifferOverlay.setVisibility(View.GONE);

        // Toolbar
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(8), dp(8), dp(8));
        bar.setBackgroundColor(Color.WHITE);

        Button closeBtn = new Button(this);
        closeBtn.setText("←");
        closeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        closeBtn.setTextColor(Color.parseColor("#0F172A"));
        closeBtn.setBackground(pillBg("#F3F4F6", 10));
        closeBtn.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(dp(38), dp(36));
        closeLp.rightMargin = dp(6);
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
        snifferUrlInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        snifferUrlInput.setTextColor(Color.parseColor("#0F172A"));
        snifferUrlInput.setHint("https://...");
        snifferUrlInput.setPadding(dp(9), dp(5), dp(9), dp(5));
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
        LinearLayout.LayoutParams urlLp = new LinearLayout.LayoutParams(0, dp(36), 1f);
        urlLp.rightMargin = dp(6);
        bar.addView(snifferUrlInput, urlLp);

        Button pasteBtn = new Button(this);
        pasteBtn.setText("📋");
        pasteBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        pasteBtn.setTextColor(Color.parseColor("#0F172A"));
        pasteBtn.setBackground(pillBg("#F3F4F6", 10));
        pasteBtn.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams pasteLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        pasteLp.rightMargin = dp(6);
        pasteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String clip = readClipboardSync();
                String clean = extractCleanUrlFromText(clip);
                if (!clean.isEmpty()) {
                    Toast.makeText(MainActivity.this, "已自动提取并加载链接", Toast.LENGTH_SHORT).show();
                    loadSnifferUrl(clean);
                } else {
                    Toast.makeText(MainActivity.this, "剪贴板未检测到链接", Toast.LENGTH_SHORT).show();
                }
            }
        });
        bar.addView(pasteBtn, pasteLp);

        snifferUaBtn = new Button(this);
        snifferUaBtn.setText("💻");
        snifferUaBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        snifferUaBtn.setTextColor(Color.parseColor("#1D4ED8"));
        snifferUaBtn.setBackground(pillBg("#EFF6FF", 10));
        snifferUaBtn.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams uaLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        uaLp.rightMargin = dp(6);
        snifferUaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                applySnifferUa(!snifferUseMobileUa);
                Toast.makeText(
                        MainActivity.this,
                        snifferUseMobileUa ? "已切换手机 UA" : "已切换桌面 UA",
                        Toast.LENGTH_SHORT).show();
                if (currentSniffPageUrl != null && !currentSniffPageUrl.isEmpty()) {
                    snifferWebView.reload();
                }
            }
        });
        bar.addView(snifferUaBtn, uaLp);

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
        sws.setMediaPlaybackRequiresUserGesture(false);
        sws.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        sws.setUserAgentString(DESKTOP_UA);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(snifferWebView, true);
        snifferWebView.addJavascriptInterface(new JoelSniffHook(), "JoelSniffHook");

        snifferWebView.setWebChromeClient(new WebChromeClient());
        snifferWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                if (request == null || request.getUrl() == null) return false;
                return shouldBlockNonHttpScheme(request.getUrl().toString());
            }

            @SuppressWarnings("deprecation")
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return shouldBlockNonHttpScheme(url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                currentSniffPageUrl = url;
                if (snifferUrlInput != null) {
                    snifferUrlInput.setText(url);
                }
                injectDomMediaScanner(view);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    final String reqUrl = request.getUrl().toString();
                    if (isSniffableMediaOrArchive(reqUrl)) {
                        String pageUrl = currentSniffPageUrl != null ? currentSniffPageUrl : "";
                        String cookies = resolveCookiesForUrl(reqUrl, pageUrl);
                        reportSniffedResource(
                                reqUrl,
                                pageUrl,
                                cookies,
                                "NET",
                                classifyMediaType(reqUrl, ""),
                                "",
                                0,
                                0,
                                -1.0);
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

    private boolean shouldBlockNonHttpScheme(String url) {
        if (url == null || url.isEmpty()) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.startsWith("about:")) {
            return false;
        }
        return true;
    }

    private void injectDomMediaScanner(final WebView view) {
        if (view == null) return;
        final String js = "(function(){"
                + "if(window.__joelSniffInstalled){window.__joelScanMedia&&window.__joelScanMedia();return;}"
                + "window.__joelSniffInstalled=true;"
                + "window.__joelScanMedia=function(){"
                + "try{"
                + "var title=(document.title||'').trim();"
                + "var els=document.querySelectorAll('video,audio,video source,audio source');"
                + "for(var i=0;i<els.length;i++){"
                + "var el=els[i];"
                + "var raw=el.currentSrc||el.src||el.getAttribute('src')||'';"
                + "if(!raw||raw.indexOf('http')!==0)continue;"
                + "var tag=el.tagName?el.tagName.toLowerCase():'video';"
                + "var parent=(tag==='source'&&el.parentElement)?el.parentElement:el;"
                + "var isAudio=tag==='audio'||(parent.tagName&&parent.tagName.toLowerCase()==='audio');"
                + "var w=Number(parent.videoWidth||0)||0;"
                + "var h=Number(parent.videoHeight||0)||0;"
                + "var dur=Number(parent.duration);"
                + "if(!isFinite(dur)||dur<0)dur=-1;"
                + "if(window.JoelSniffHook){"
                + "window.JoelSniffHook.onDomMedia(raw,isAudio?'audio':'video',title,w,h,dur);"
                + "}"
                + "}"
                + "}catch(e){}"
                + "};"
                + "window.__joelScanMedia();"
                + "setTimeout(window.__joelScanMedia,800);"
                + "setInterval(window.__joelScanMedia,1800);"
                + "})();";
        view.evaluateJavascript(js, null);
    }

    private String resolveCookiesForUrl(String url, String pageUrl) {
        String cookies = CookieManager.getInstance().getCookie(url);
        if ((cookies == null || cookies.isEmpty()) && pageUrl != null && !pageUrl.isEmpty()) {
            cookies = CookieManager.getInstance().getCookie(pageUrl);
        }
        return cookies != null ? cookies : "";
    }

    private String extractCleanUrlFromText(String raw) {
        if (raw == null) return "";
        Matcher m = URL_EXTRACT_PATTERN.matcher(raw);
        if (!m.find()) return "";
        String url = m.group();
        return url.replaceAll("[),.;!?'\"\\]]+$", "");
    }

    private boolean shouldUseMobileUaForUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("douyin.com")
                || lower.contains("iesdouyin.com")
                || lower.contains("xhslink.com")
                || lower.contains("xiaohongshu.com")
                || lower.contains("b23.tv")
                || lower.contains("tiktok.com")
                || lower.contains("kuaishou.com");
    }

    private void applySnifferUa(boolean useMobile) {
        snifferUseMobileUa = useMobile;
        if (snifferWebView != null) {
            snifferWebView.getSettings().setUserAgentString(useMobile ? MOBILE_UA : DESKTOP_UA);
        }
        if (snifferUaBtn != null) {
            snifferUaBtn.setText(useMobile ? "📱" : "💻");
        }
    }

    private boolean isIgnoredStaticAsset(String lower) {
        int q = lower.indexOf('?');
        String noQuery = q > 0 ? lower.substring(0, q) : lower;
        if (noQuery.endsWith(".js")
                || noQuery.endsWith(".css")
                || noQuery.endsWith(".svg")
                || noQuery.endsWith(".woff")
                || noQuery.endsWith(".woff2")
                || noQuery.endsWith(".ttf")
                || noQuery.endsWith(".eot")
                || noQuery.endsWith(".ico")
                || noQuery.endsWith(".json")
                || noQuery.endsWith(".html")
                || noQuery.endsWith(".htm")
                || noQuery.endsWith(".shtml")
                || noQuery.endsWith(".map")) {
            return true;
        }
        return lower.contains("mon.zijieapi.com")
                || lower.contains("mcs.snssdk.com")
                || lower.contains("doubleclick.net")
                || lower.contains("google-analytics.com");
    }

    private boolean isSniffableMediaOrArchive(String url) {
        if (url == null || !url.startsWith("http")) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        if (isIgnoredStaticAsset(lower)) return false;

        int q = lower.indexOf('?');
        String noQuery = q > 0 ? lower.substring(0, q) : lower;
        if (noQuery.endsWith(".mp4")
                || noQuery.endsWith(".mkv")
                || noQuery.endsWith(".mov")
                || noQuery.endsWith(".webm")
                || noQuery.endsWith(".flv")
                || noQuery.endsWith(".m3u8")
                || noQuery.endsWith(".mp3")
                || noQuery.endsWith(".m4a")
                || noQuery.endsWith(".wav")
                || noQuery.endsWith(".flac")
                || noQuery.endsWith(".ogg")
                || noQuery.endsWith(".aac")
                || noQuery.endsWith(".apk")
                || noQuery.endsWith(".zip")
                || noQuery.endsWith(".rar")
                || noQuery.endsWith(".7z")
                || noQuery.endsWith(".tar.gz")
                || noQuery.endsWith(".pdf")) {
            return true;
        }
        if (lower.contains("douyinvod.com")
                || lower.contains("snssdk.com/aweme/")
                || lower.contains("/aweme/v1/play")
                || lower.contains("/video/tos/")
                || lower.contains("bilivideo.com")) {
            return true;
        }
        if (lower.contains("xhscdn.com") && (lower.contains(".mp4") || lower.contains("stream"))) {
            return true;
        }
        return lower.contains(".mp4?")
                || lower.contains(".m4a?")
                || lower.contains(".mp3?")
                || lower.contains(".apk?")
                || lower.contains("mime=video")
                || lower.contains("mime_type=video")
                || lower.contains("mime=audio")
                || lower.contains("mime_type=audio");
    }

    private String classifyMediaType(String url, String contentType) {
        String ct = contentType != null ? contentType.toLowerCase(Locale.ROOT) : "";
        if (ct.startsWith("video/") || ct.contains("mpegurl")) return "video";
        if (ct.startsWith("audio/")) return "audio";
        if (ct.startsWith("image/")) return "image";

        String lower = url != null ? url.toLowerCase(Locale.ROOT) : "";
        int q = lower.indexOf('?');
        String noQuery = q > 0 ? lower.substring(0, q) : lower;
        if (noQuery.endsWith(".mp3")
                || noQuery.endsWith(".m4a")
                || noQuery.endsWith(".wav")
                || noQuery.endsWith(".flac")
                || noQuery.endsWith(".ogg")
                || noQuery.endsWith(".aac")
                || lower.contains("mime=audio")
                || lower.contains("mime_type=audio")
                || lower.contains(".m4a?")
                || lower.contains(".mp3?")) {
            return "audio";
        }
        if (noQuery.endsWith(".jpg")
                || noQuery.endsWith(".jpeg")
                || noQuery.endsWith(".png")
                || noQuery.endsWith(".webp")
                || noQuery.endsWith(".gif")) {
            return "image";
        }
        if (noQuery.endsWith(".apk")
                || noQuery.endsWith(".zip")
                || noQuery.endsWith(".rar")
                || noQuery.endsWith(".7z")
                || noQuery.endsWith(".tar.gz")
                || noQuery.endsWith(".pdf")
                || lower.contains(".apk?")) {
            return "file";
        }
        return "video";
    }

    private void reportSniffedResource(
            final String url,
            final String referer,
            final String cookie,
            final String source,
            final String mediaType,
            final String title,
            final int width,
            final int height,
            final double durationSec) {
        if (url == null || !url.startsWith("http")) return;

        final SniffedResource existingOrNew;
        final boolean isNew;
        final int count;
        synchronized (sniffLock) {
            SniffedResource existing = sniffedByUrl.get(url);
            if (existing != null) {
                boolean updated = false;
                if ("DOM".equals(source) && !"DOM".equals(existing.source)) {
                    existing.source = "DOM";
                    updated = true;
                }
                if (title != null && !title.isEmpty() && (existing.title == null || existing.title.isEmpty())) {
                    existing.title = title;
                    updated = true;
                }
                if (width > 0 && existing.width <= 0) {
                    existing.width = width;
                    existing.height = height;
                    updated = true;
                }
                if (durationSec > 0 && existing.durationSec <= 0) {
                    existing.durationSec = durationSec;
                    updated = true;
                }
                if (mediaType != null && !mediaType.isEmpty() && !mediaType.equals(existing.mediaType)
                        && "DOM".equals(source)) {
                    existing.mediaType = mediaType;
                    updated = true;
                }
                if (updated) {
                    emitSniffedUpdateToJs(existing, false);
                }
                return;
            }
            seenSniffUrls.add(url);
            String id = "sniff_" + sniffIdSeq.incrementAndGet();
            String activeUaMode = snifferUseMobileUa ? "mobile" : "desktop";
            String activeUa = snifferUseMobileUa ? MOBILE_UA : DESKTOP_UA;
            existingOrNew = new SniffedResource(
                    id,
                    url,
                    referer,
                    cookie,
                    activeUa,
                    activeUaMode,
                    source,
                    mediaType,
                    title,
                    width,
                    height,
                    durationSec);
            sniffedByUrl.put(url, existingOrNew);
            sniffedById.put(id, existingOrNew);
            isNew = true;
            count = seenSniffUrls.size();
        }

        if (!isNew) return;
        final String jsPayload;
        try {
            jsPayload = existingOrNew.toJson(previewProxyPort).toString();
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

        probeExecutor.execute(new Runnable() {
            @Override
            public void run() {
                probeSniffedResourceAsync(existingOrNew);
            }
        });
    }

    private void emitSniffedUpdateToJs(final SniffedResource res, final boolean isHtmlTrap) {
        final String jsPayload;
        try {
            JSONObject obj = res.toJson(previewProxyPort);
            obj.put("invalid", isHtmlTrap);
            jsPayload = obj.toString();
        } catch (JSONException e) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mainWebView.evaluateJavascript(
                        "window.onSniffedMediaUpdate && window.onSniffedMediaUpdate(" + jsPayload + ");",
                        null);
            }
        });
    }

    private void probeSniffedResourceAsync(SniffedResource res) {
        HttpURLConnection conn = null;
        try {
            String current = res.url;
            for (int hop = 0; hop < 5; hop++) {
                URL u = new URL(current);
                conn = (HttpURLConnection) u.openConnection();
                conn.setInstanceFollowRedirects(false);
                conn.setConnectTimeout(7000);
                conn.setReadTimeout(7000);
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Range", "bytes=0-0");
                conn.setRequestProperty("User-Agent", res.userAgent);
                if (res.referer != null && !res.referer.isEmpty()) {
                    conn.setRequestProperty("Referer", res.referer);
                }
                if (res.cookie != null && !res.cookie.isEmpty()) {
                    conn.setRequestProperty("Cookie", res.cookie);
                }
                int code = conn.getResponseCode();
                if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                    String loc = conn.getHeaderField("Location");
                    conn.disconnect();
                    conn = null;
                    if (loc == null || loc.isEmpty()) return;
                    current = new URL(u, loc).toString();
                    continue;
                }

                String ct = conn.getContentType();
                String lowerCt = ct != null ? ct.toLowerCase(Locale.ROOT) : "";
                boolean isHtmlTrap = !"DOM".equals(res.source)
                        && (lowerCt.contains("text/html") || code == 403 || code == 404);
                if (isHtmlTrap) {
                    emitSniffedUpdateToJs(res, true);
                    return;
                }

                if (ct != null && !ct.isEmpty()) {
                    int semi = ct.indexOf(';');
                    res.contentType = semi > 0 ? ct.substring(0, semi).trim() : ct.trim();
                    if (!"DOM".equals(res.source)) {
                        res.mediaType = classifyMediaType(res.url, res.contentType);
                    }
                }

                long totalSize = parseTotalBytesFromHeaders(conn);
                if (totalSize > 0) {
                    res.sizeBytes = totalSize;
                }

                String disp = conn.getHeaderField("Content-Disposition");
                if (disp != null && !disp.isEmpty()) {
                    Matcher dm = DISPOSITION_FILENAME_PATTERN.matcher(disp);
                    if (dm.find()) {
                        try {
                            res.suggestedName = URLDecoder.decode(dm.group(1).trim(), "UTF-8");
                        } catch (Exception ignored) {
                            res.suggestedName = dm.group(1).trim();
                        }
                    }
                }

                emitSniffedUpdateToJs(res, false);
                return;
            }
        } catch (Exception ignored) {
        } finally {
            if (conn != null) {
                try {
                    conn.disconnect();
                } catch (Exception ignored) {
                }
            }
        }
    }

    private long parseTotalBytesFromHeaders(HttpURLConnection conn) {
        String contentRange = conn.getHeaderField("Content-Range");
        if (contentRange != null) {
            Matcher m = CONTENT_RANGE_TOTAL_PATTERN.matcher(contentRange.trim());
            if (m.find()) {
                try {
                    return Long.parseLong(m.group(1));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        String len = conn.getHeaderField("Content-Length");
        if (len != null && !len.trim().isEmpty()) {
            try {
                long v = Long.parseLong(len.trim());
                if (v > 1) return v;
            } catch (NumberFormatException ignored) {
            }
        }
        return -1L;
    }

    private class JoelSniffHook {
        @JavascriptInterface
        public void onDomMedia(String mediaUrl, String tagType, String title, int width, int height, double durationSec) {
            if (mediaUrl == null || !mediaUrl.startsWith("http")) return;
            String pageUrl = currentSniffPageUrl != null ? currentSniffPageUrl : "";
            String cookies = resolveCookiesForUrl(mediaUrl, pageUrl);
            String type = "audio".equals(tagType) ? "audio" : "video";
            reportSniffedResource(
                    mediaUrl,
                    pageUrl,
                    cookies,
                    "DOM",
                    type,
                    title != null ? title.trim() : "",
                    width,
                    height,
                    durationSec);
        }
    }

    private void openSnifferOverlay(final String rawUrl) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                synchronized (sniffLock) {
                    seenSniffUrls.clear();
                    sniffedByUrl.clear();
                }
                snifferCountBadge.setText("0");
                snifferOverlay.setVisibility(View.VISIBLE);
                String clean = extractCleanUrlFromText(rawUrl);
                String target = !clean.isEmpty() ? clean : (rawUrl != null ? rawUrl.trim() : "");
                boolean defaultMobile = false;
                try {
                    defaultMobile = "mobile".equals(downloader.getSettingsJson().optString("defaultUaMode", "desktop"));
                } catch (Exception ignored) {
                }
                applySnifferUa(shouldUseMobileUaForUrl(target) || defaultMobile);
                loadSnifferUrl(target);
            }
        });
    }

    private void loadSnifferUrl(String rawUrl) {
        String raw = rawUrl != null ? rawUrl.trim() : "";
        if (raw.isEmpty()) return;
        String clean = extractCleanUrlFromText(raw);
        String u;
        if (!clean.isEmpty()) {
            u = clean;
        } else if (DOMAIN_LIKE_PATTERN.matcher(raw).matches()) {
            u = "https://" + raw;
        } else {
            Toast.makeText(this, "未识别到有效链接", Toast.LENGTH_SHORT).show();
            return;
        }
        if (shouldUseMobileUaForUrl(u) && !snifferUseMobileUa) {
            applySnifferUa(true);
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
                snifferWebView.evaluateJavascript(
                        "document.querySelectorAll('video,audio').forEach(function(e){try{e.pause();}catch(_){}});",
                        null);
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
            root.put("tasks", new JSONArray(downloader.getTasksJsonString()));
            root.put("settings", downloader.getSettingsJson());
        } catch (JSONException ignored) {
        }
        return root.toString();
    }

    @SuppressWarnings("deprecation")
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
        if (previewProxy != null) {
            previewProxy.stop();
        }
        probeExecutor.shutdownNow();
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

    private String readClipboardSync() {
        final String[] result = new String[]{""};
        final CountDownLatch latch = new CountDownLatch(1);
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
            latch.await(1500, TimeUnit.MILLISECONDS);
        } catch (InterruptedException ignored) {
        }
        return result[0];
    }

    private class LocalPreviewProxy {
        private volatile boolean running = false;
        private ServerSocket serverSocket;
        private final ExecutorService workers = Executors.newCachedThreadPool();

        int start() {
            try {
                serverSocket = new ServerSocket(0, 32, InetAddress.getByName("127.0.0.1"));
                running = true;
                int port = serverSocket.getLocalPort();
                Thread acceptThread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        while (running && serverSocket != null && !serverSocket.isClosed()) {
                            try {
                                final Socket client = serverSocket.accept();
                                workers.execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        handleClient(client);
                                    }
                                });
                            } catch (IOException ignored) {
                                if (!running) break;
                            }
                        }
                    }
                }, "JoelPreviewProxyAccept");
                acceptThread.setDaemon(true);
                acceptThread.start();
                return port;
            } catch (IOException e) {
                return 0;
            }
        }

        void stop() {
            running = false;
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException ignored) {
                }
            }
            workers.shutdownNow();
        }

        private void handleClient(Socket client) {
            HttpURLConnection conn = null;
            try {
                client.setSoTimeout(15000);
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(client.getInputStream(), StandardCharsets.US_ASCII));
                String requestLine = reader.readLine();
                if (requestLine == null || requestLine.trim().isEmpty()) return;

                String[] parts = requestLine.split(" ");
                if (parts.length < 2) return;
                String method = parts[0].toUpperCase(Locale.ROOT);
                boolean isHead = "HEAD".equals(method);
                String path = parts[1];

                String rangeHeader = null;
                String line;
                while ((line = reader.readLine()) != null && !line.isEmpty()) {
                    int colon = line.indexOf(':');
                    if (colon > 0) {
                        String key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
                        if ("range".equals(key)) {
                            rangeHeader = line.substring(colon + 1).trim();
                        }
                    }
                }

                OutputStream out = new BufferedOutputStream(client.getOutputStream());
                String id = extractQueryParam(path, "id");
                SniffedResource res = id != null ? sniffedById.get(id) : null;
                if (res == null) {
                    writeSimpleResponse(out, 404, "Not Found");
                    return;
                }

                String currentUrl = res.url;
                for (int hop = 0; hop < 5; hop++) {
                    URL u = new URL(currentUrl);
                    conn = (HttpURLConnection) u.openConnection();
                    conn.setInstanceFollowRedirects(false);
                    conn.setConnectTimeout(12000);
                    conn.setReadTimeout(20000);
                    conn.setRequestMethod(isHead ? "HEAD" : "GET");
                    conn.setRequestProperty(
                            "User-Agent",
                            res.userAgent != null && !res.userAgent.isEmpty() ? res.userAgent : MOBILE_UA);
                    conn.setRequestProperty("Accept", "*/*");
                    if (res.referer != null && !res.referer.isEmpty()) {
                        conn.setRequestProperty("Referer", res.referer);
                    }
                    String cookie = res.cookie;
                    String liveCookie = CookieManager.getInstance().getCookie(currentUrl);
                    if (liveCookie != null && !liveCookie.isEmpty()) {
                        cookie = liveCookie;
                    }
                    if (cookie != null && !cookie.isEmpty()) {
                        conn.setRequestProperty("Cookie", cookie);
                    }
                    if (rangeHeader != null && !rangeHeader.isEmpty()) {
                        conn.setRequestProperty("Range", rangeHeader);
                    }

                    int code = conn.getResponseCode();
                    if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                        String loc = conn.getHeaderField("Location");
                        conn.disconnect();
                        conn = null;
                        if (loc == null || loc.isEmpty()) break;
                        currentUrl = new URL(u, loc).toString();
                        continue;
                    }

                    String ct = conn.getContentType();
                    if (ct == null || ct.isEmpty() || ct.toLowerCase(Locale.ROOT).startsWith("application/octet-stream")) {
                        if ("video".equals(res.mediaType)) {
                            ct = "video/mp4";
                        } else if ("audio".equals(res.mediaType)) {
                            ct = "audio/mpeg";
                        } else if ("image".equals(res.mediaType)) {
                            ct = "image/jpeg";
                        } else {
                            ct = "application/octet-stream";
                        }
                    }

                    StringBuilder hdr = new StringBuilder();
                    hdr.append("HTTP/1.1 ").append(code).append(" ").append(reasonPhrase(code)).append("\r\n");
                    hdr.append("Content-Type: ").append(ct).append("\r\n");
                    hdr.append("Accept-Ranges: bytes\r\n");
                    hdr.append("Access-Control-Allow-Origin: *\r\n");
                    hdr.append("Access-Control-Allow-Headers: Range\r\n");
                    hdr.append("Cache-Control: no-store\r\n");
                    hdr.append("Connection: close\r\n");

                    String contentRange = conn.getHeaderField("Content-Range");
                    if (contentRange != null && !contentRange.isEmpty()) {
                        hdr.append("Content-Range: ").append(contentRange).append("\r\n");
                    }
                    String contentLen = conn.getHeaderField("Content-Length");
                    if (contentLen != null && !contentLen.isEmpty()) {
                        hdr.append("Content-Length: ").append(contentLen).append("\r\n");
                    }
                    hdr.append("\r\n");

                    out.write(hdr.toString().getBytes(StandardCharsets.US_ASCII));
                    out.flush();

                    if (!isHead && code < 400) {
                        InputStream in = new BufferedInputStream(conn.getInputStream());
                        byte[] buf = new byte[16384];
                        int n;
                        while ((n = in.read(buf)) != -1) {
                            out.write(buf, 0, n);
                        }
                        out.flush();
                        in.close();
                    }
                    return;
                }
                writeSimpleResponse(out, 502, "Bad Gateway");
            } catch (IOException ignored) {
            } finally {
                if (conn != null) {
                    try {
                        conn.disconnect();
                    } catch (Exception ignored) {
                    }
                }
                try {
                    client.close();
                } catch (IOException ignored) {
                }
            }
        }

        private void writeSimpleResponse(OutputStream out, int code, String reason) throws IOException {
            String resp = "HTTP/1.1 " + code + " " + reason
                    + "\r\nContent-Length: 0\r\nAccess-Control-Allow-Origin: *\r\nConnection: close\r\n\r\n";
            out.write(resp.getBytes(StandardCharsets.US_ASCII));
            out.flush();
        }

        private String extractQueryParam(String path, String name) {
            int q = path.indexOf('?');
            if (q < 0) return null;
            String query = path.substring(q + 1);
            for (String pair : query.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0 && name.equals(pair.substring(0, eq))) {
                    try {
                        return URLDecoder.decode(pair.substring(eq + 1), "UTF-8");
                    } catch (Exception ignored) {
                        return pair.substring(eq + 1);
                    }
                }
            }
            return null;
        }

        private String reasonPhrase(int code) {
            if (code == 206) return "Partial Content";
            if (code == 200) return "OK";
            if (code == 403) return "Forbidden";
            if (code == 404) return "Not Found";
            if (code == 416) return "Range Not Satisfiable";
            return "Status";
        }
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
            return readClipboardSync();
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
