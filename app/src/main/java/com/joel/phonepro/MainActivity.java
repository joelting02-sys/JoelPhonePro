package com.joel.phonepro;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
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
import android.widget.ProgressBar;
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
    private static final String DESKTOP_UA_FALLBACK =
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Safari/537.36";
    private static final String MOBILE_UA_FALLBACK =
            "Mozilla/5.0 (Linux; Android 14; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36";

    private static final Pattern URL_EXTRACT_PATTERN = Pattern.compile(
            "https?://[A-Za-z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DOMAIN_LIKE_PATTERN = Pattern.compile(
            "^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}(/.*)?$");
    private static final Pattern CONTENT_RANGE_TOTAL_PATTERN = Pattern.compile("/(\\d+)$");
    private static final Pattern DISPOSITION_FILENAME_PATTERN = Pattern.compile(
            "filename\\*?=(?:UTF-8''|\"?)([^\";]+)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern CHROME_VERSION_PATTERN = Pattern.compile("Chrome/([0-9.]+)");
    private static final Pattern RENDER_DATA_PATTERN = Pattern.compile(
            "<script[^>]*id=[\"']RENDER_DATA[\"'][^>]*>([^<]+)</script>",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern DOUYIN_VIDEO_ID_PATTERN = Pattern.compile(
            "\"uri\"\\s*:\\s*\"(v0[0-9a-zA-Z_]{16,40})\"");

    private String mobileUa = MOBILE_UA_FALLBACK;
    private String desktopUa = DESKTOP_UA_FALLBACK;

    private FrameLayout rootLayout;
    private WebView mainWebView;
    private MultiThreadDownloader downloader;

    // Native Web Media Sniffer overlay + Floating Download FAB + Multi-window OAuth Popup
    private LinearLayout snifferOverlay;
    private ProgressBar snifferProgressBar;
    private FrameLayout snifferWebContainer;
    private WebView snifferWebView;
    private EditText snifferUrlInput;
    private Button snifferUaBtn;
    private FrameLayout rlDownloadFab;
    private TextView fabCircleBtn;
    private LinearLayout popupOverlay;
    private TextView popupTitleView;
    private FrameLayout popupWebSlot;
    private WebView popupWebView;

    private final Object sniffLock = new Object();
    private final Set<String> seenSniffUrls = new HashSet<String>();
    private final ConcurrentHashMap<String, SniffedResource> sniffedById =
            new ConcurrentHashMap<String, SniffedResource>();
    private final Map<String, SniffedResource> sniffedByUrl = new HashMap<String, SniffedResource>();
    private final AtomicInteger sniffIdSeq = new AtomicInteger(0);
    private final ExecutorService probeExecutor = Executors.newFixedThreadPool(3);
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private LocalPreviewProxy previewProxy;
    private volatile int previewProxyPort = 0;

    private volatile boolean pageLoaded = false;
    private volatile String pendingSharedText = "";
    private volatile String currentSniffPageUrl = "";
    private volatile boolean snifferUseMobileUa = true;
    private volatile boolean browserHasPageLoaded = false;
    private volatile boolean browserShowingInTab1 = false;
    private volatile boolean downloadDrawerOpen = false;
    private volatile String currentBottomTab = "browser";
    private volatile boolean fabScanningState = false;
    private volatile String latestNetVideoUrl = "";
    private volatile String latestNetVideoReferer = "";
    private volatile String latestNetVideoCookie = "";
    private volatile long lastNetRescanAt = 0L;
    private volatile String currentActiveVideoKey = "";
    private volatile String currentActiveVideoTitle = "";
    private volatile String currentActiveCoverUrl = "";
    private volatile double currentActiveDurationSec = -1.0;

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
        volatile String qualityLabel;
        volatile String coverUrl;
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
            this.qualityLabel = "";
            this.coverUrl = "";
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
            obj.put("uaMode", uaMode != null ? uaMode : "mobile");
            obj.put("source", source != null ? source : "NET");
            obj.put("mediaType", mediaType != null ? mediaType : "video");
            obj.put("title", title != null ? title : "");
            obj.put("qualityLabel", qualityLabel != null ? qualityLabel : "");
            obj.put("coverUrl", coverUrl != null ? coverUrl : "");
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

        initUserAgents();
        downloader = new MultiThreadDownloader(this, this);
        startPreviewProxy();
        captureSharedIntent(getIntent());

        rootLayout = new FrameLayout(this);
        rootLayout.setBackgroundColor(Color.WHITE);
        applySystemBarInsets();

        mainWebView = new WebView(this);
        mainWebView.setBackgroundColor(Color.TRANSPARENT);
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
        FrameLayout.LayoutParams snifferLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT);
        snifferLp.bottomMargin = dp(60);
        rootLayout.addView(snifferOverlay, snifferLp);

        setContentView(rootLayout);
        configureSystemBarAppearance();
        rootLayout.requestApplyInsets();
        mainWebView.loadUrl("file:///android_asset/index.html");
    }

    private void initUserAgents() {
        try {
            String defUa = WebSettings.getDefaultUserAgent(this);
            if (defUa != null && !defUa.trim().isEmpty()) {
                String cleanMobile = defUa
                        .replace("; wv", "")
                        .replace("Version/4.0 ", "")
                        .replaceAll(" +", " ")
                        .trim();
                mobileUa = cleanMobile;
                Matcher cm = CHROME_VERSION_PATTERN.matcher(cleanMobile);
                if (cm.find()) {
                    desktopUa = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/"
                            + cm.group(1) + " Safari/537.36";
                }
            }
        } catch (Exception ignored) {
            mobileUa = MOBILE_UA_FALLBACK;
            desktopUa = DESKTOP_UA_FALLBACK;
        }
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

    @SuppressLint("SetJavaScriptEnabled")
    private void configureBrowserWebSettings(WebSettings s, boolean useMobile) {
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setJavaScriptCanOpenWindowsAutomatically(true);
        s.setSupportMultipleWindows(true);
        s.setUseWideViewPort(!useMobile);
        s.setLoadWithOverviewMode(!useMobile);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setUserAgentString(useMobile ? mobileUa : desktopUa);
    }

    @SuppressLint({"SetJavaScriptEnabled", "AddJavascriptInterface", "ClickableViewAccessibility"})
    private void buildSnifferOverlay() {
        snifferOverlay = new LinearLayout(this);
        snifferOverlay.setOrientation(LinearLayout.VERTICAL);
        snifferOverlay.setBackgroundColor(Color.WHITE);
        snifferOverlay.setVisibility(View.GONE);

        // Top thin webpage loading progress bar
        snifferProgressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        snifferProgressBar.setMax(100);
        snifferProgressBar.setProgress(0);
        snifferProgressBar.setProgressTintList(ColorStateList.valueOf(Color.parseColor("#FF3366")));
        snifferProgressBar.setProgressBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#E2E8F0")));
        snifferProgressBar.setVisibility(View.GONE);

        // Bottom Browser Control Bar (Thumb Zone, sits right above the 60dp Bottom Navigation Bar)
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp(8), dp(6), dp(8), dp(6));
        bar.setBackgroundColor(Color.parseColor("#F8FAFC"));

        Button homeBtn = new Button(this);
        homeBtn.setText("🏠");
        homeBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        homeBtn.setTextColor(Color.parseColor("#0F172A"));
        homeBtn.setBackground(pillBg("#FFFFFF", 10));
        homeBtn.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams homeLp = new LinearLayout.LayoutParams(dp(38), dp(40));
        homeLp.rightMargin = dp(5);
        homeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic();
                minimizeBrowserToHome();
            }
        });
        bar.addView(homeBtn, homeLp);

        Button backBtn = new Button(this);
        backBtn.setText("◀");
        backBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        backBtn.setTextColor(Color.parseColor("#0F172A"));
        backBtn.setBackground(pillBg("#FFFFFF", 10));
        backBtn.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams backLp = new LinearLayout.LayoutParams(dp(38), dp(40));
        backLp.rightMargin = dp(5);
        backBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic();
                if (snifferWebView != null && snifferWebView.canGoBack()) {
                    snifferWebView.goBack();
                } else {
                    minimizeBrowserToHome();
                }
            }
        });
        bar.addView(backBtn, backLp);

        snifferUrlInput = new EditText(this);
        snifferUrlInput.setSingleLine(true);
        snifferUrlInput.setInputType(InputType.TYPE_TEXT_VARIATION_URI);
        snifferUrlInput.setImeOptions(EditorInfo.IME_ACTION_GO);
        snifferUrlInput.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        snifferUrlInput.setTextColor(Color.parseColor("#0F172A"));
        snifferUrlInput.setHint("搜索或输入网址...");
        snifferUrlInput.setPadding(dp(10), dp(4), dp(10), dp(4));
        snifferUrlInput.setBackground(pillBg("#FFFFFF", 10));
        snifferUrlInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_GO || actionId == EditorInfo.IME_ACTION_DONE) {
                    loadSnifferUrl(snifferUrlInput.getText().toString(), true);
                    return true;
                }
                return false;
            }
        });
        LinearLayout.LayoutParams urlLp = new LinearLayout.LayoutParams(0, dp(40), 1f);
        urlLp.rightMargin = dp(5);
        bar.addView(snifferUrlInput, urlLp);

        Button pasteBtn = new Button(this);
        pasteBtn.setText("📋");
        pasteBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        pasteBtn.setTextColor(Color.parseColor("#0F172A"));
        pasteBtn.setBackground(pillBg("#FFFFFF", 10));
        pasteBtn.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams pasteLp = new LinearLayout.LayoutParams(dp(38), dp(40));
        pasteLp.rightMargin = dp(5);
        pasteBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic();
                String clip = readClipboardSync();
                String clean = extractCleanUrlFromText(clip);
                if (!clean.isEmpty()) {
                    Toast.makeText(MainActivity.this, "已自动提取并加载链接", Toast.LENGTH_SHORT).show();
                    loadSnifferUrl(clean, true);
                } else {
                    Toast.makeText(MainActivity.this, "剪贴板未检测到链接", Toast.LENGTH_SHORT).show();
                }
            }
        });
        bar.addView(pasteBtn, pasteLp);

        snifferUaBtn = new Button(this);
        snifferUaBtn.setText("📱");
        snifferUaBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        snifferUaBtn.setTextColor(Color.parseColor("#067EFF"));
        snifferUaBtn.setBackground(pillBg("#EFF6FF", 10));
        snifferUaBtn.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams uaLp = new LinearLayout.LayoutParams(dp(38), dp(40));
        uaLp.rightMargin = dp(5);
        snifferUaBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic();
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

        Button refreshBtn = new Button(this);
        refreshBtn.setText("🔄");
        refreshBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        refreshBtn.setTextColor(Color.parseColor("#067EFF"));
        refreshBtn.setBackground(pillBg("#EFF6FF", 10));
        refreshBtn.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams scanLp = new LinearLayout.LayoutParams(dp(38), dp(40));
        refreshBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic();
                fabScanningState = true;
                updateFloatingFabUi(false);
                injectDomMediaScanner(snifferWebView, true);
                Toast.makeText(MainActivity.this, "正在重新嗅探本页媒体…", Toast.LENGTH_SHORT).show();
                uiHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        fabScanningState = false;
                        updateFloatingFabUi(false);
                    }
                }, 1200L);
            }
        });
        bar.addView(refreshBtn, scanLp);

        View divider = new View(this);
        divider.setBackgroundColor(Color.parseColor("#E2E8F0"));

        snifferWebContainer = new FrameLayout(this);

        snifferWebView = new WebView(this);
        configureBrowserWebSettings(snifferWebView.getSettings(), true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(snifferWebView, true);
        snifferWebView.addJavascriptInterface(new JoelSniffHook(), "JoelSniffHook");

        snifferWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                if (snifferProgressBar != null) {
                    if (newProgress >= 100) {
                        snifferProgressBar.setVisibility(View.GONE);
                    } else {
                        snifferProgressBar.setVisibility(View.VISIBLE);
                        snifferProgressBar.setProgress(newProgress);
                    }
                }
            }

            @Override
            public boolean onCreateWindow(
                    WebView view,
                    boolean isDialog,
                    boolean isUserGesture,
                    Message resultMsg) {
                if (resultMsg == null || !(resultMsg.obj instanceof WebView.WebViewTransport)) {
                    return false;
                }
                openPopupWindow((WebView.WebViewTransport) resultMsg.obj);
                resultMsg.sendToTarget();
                return true;
            }
        });
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
                CookieManager.getInstance().flush();
                injectDomMediaScanner(view, true);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mainWebView.evaluateJavascript(
                                "window.onBrowserPageChanged && window.onBrowserPageChanged("
                                        + JSONObject.quote(url != null ? url : "") + ");",
                                null);
                    }
                });
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    final String reqUrl = request.getUrl().toString();
                    final Map<String, String> hdrs = request.getRequestHeaders();
                    String secFetchDest = getHeaderIgnoreCase(hdrs, "sec-fetch-dest");
                    String accept = getHeaderIgnoreCase(hdrs, "accept");
                    String mediaType = detectSniffTypeFromRequest(reqUrl, secFetchDest, accept);
                    if ("video".equals(mediaType)) {
                        String headerRef = getHeaderIgnoreCase(hdrs, "referer");
                        String pageUrl = !headerRef.isEmpty()
                                ? headerRef
                                : (currentSniffPageUrl != null ? currentSniffPageUrl : "");
                        latestNetVideoUrl = reqUrl;
                        latestNetVideoReferer = pageUrl;
                        latestNetVideoCookie = resolveCookiesForUrl(reqUrl, pageUrl);

                        boolean needsUpgrade = false;
                        synchronized (sniffLock) {
                            if (sniffedById.isEmpty()) {
                                needsUpgrade = true;
                            } else {
                                boolean allFallback = true;
                                for (SniffedResource r : sniffedById.values()) {
                                    if (r.url == null || !r.url.contains("iesdouyin.com/aweme/v1/play")) {
                                        allFallback = false;
                                        break;
                                    }
                                }
                                needsUpgrade = allFallback;
                            }
                        }
                        if (needsUpgrade && isRealVideoCdnUrl(reqUrl)) {
                            long now = System.currentTimeMillis();
                            if (now - lastNetRescanAt > 400L) {
                                lastNetRescanAt = now;
                                uiHandler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (snifferWebView != null) {
                                            injectDomMediaScanner(snifferWebView, false);
                                        }
                                    }
                                }, 250L);
                            }
                        }
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

        // Native Draggable Floating Download FAB (Video Downloader rl_download_fab signature)
        rlDownloadFab = buildFloatingDownloadFab();

        // Multi-window OAuth Popup container (e.g. Sign in with Google on X)
        popupOverlay = new LinearLayout(this);
        popupOverlay.setOrientation(LinearLayout.VERTICAL);
        popupOverlay.setBackgroundColor(Color.WHITE);
        popupOverlay.setVisibility(View.GONE);

        LinearLayout popupBar = new LinearLayout(this);
        popupBar.setOrientation(LinearLayout.HORIZONTAL);
        popupBar.setGravity(Gravity.CENTER_VERTICAL);
        popupBar.setPadding(dp(10), dp(6), dp(10), dp(6));
        popupBar.setBackgroundColor(Color.parseColor("#F8FAFC"));

        popupTitleView = new TextView(this);
        popupTitleView.setText("账号授权窗口");
        popupTitleView.setSingleLine(true);
        popupTitleView.setEllipsize(TextUtils.TruncateAt.END);
        popupTitleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        popupTitleView.setTypeface(Typeface.DEFAULT_BOLD);
        popupTitleView.setTextColor(Color.parseColor("#0F172A"));
        popupBar.addView(popupTitleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button popupCloseBtn = new Button(this);
        popupCloseBtn.setText("✕ 关闭");
        popupCloseBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        popupCloseBtn.setTextColor(Color.parseColor("#334155"));
        popupCloseBtn.setBackground(pillBg("#E2E8F0", 10));
        popupCloseBtn.setPadding(dp(12), 0, dp(12), 0);
        popupCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerHaptic();
                closePopupWindow();
            }
        });
        popupBar.addView(popupCloseBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(36)));

        View popupDivider = new View(this);
        popupDivider.setBackgroundColor(Color.parseColor("#E2E8F0"));

        popupWebSlot = new FrameLayout(this);
        popupOverlay.addView(popupWebSlot, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        popupOverlay.addView(popupDivider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        popupOverlay.addView(popupBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        snifferWebContainer.addView(snifferWebView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        FrameLayout.LayoutParams fabLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        fabLp.gravity = Gravity.BOTTOM | Gravity.END;
        fabLp.rightMargin = dp(14);
        fabLp.bottomMargin = dp(14);
        snifferWebContainer.addView(rlDownloadFab, fabLp);

        snifferWebContainer.addView(popupOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        snifferOverlay.addView(snifferProgressBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(3)));
        snifferOverlay.addView(snifferWebContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        snifferOverlay.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        snifferOverlay.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        updateFloatingFabUi(false);
    }

    @SuppressLint("ClickableViewAccessibility")
    private FrameLayout buildFloatingDownloadFab() {
        final FrameLayout wrap = new FrameLayout(this);

        fabCircleBtn = new TextView(this);
        fabCircleBtn.setText("⬇");
        fabCircleBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22);
        fabCircleBtn.setTypeface(Typeface.DEFAULT_BOLD);
        fabCircleBtn.setTextColor(Color.WHITE);
        fabCircleBtn.setGravity(Gravity.CENTER);
        fabCircleBtn.setElevation(dp(8));
        wrap.addView(fabCircleBtn, new FrameLayout.LayoutParams(dp(56), dp(56)));

        wrap.setOnTouchListener(new View.OnTouchListener() {
            float downRawX;
            float downRawY;
            float startTx;
            float startTy;
            boolean dragging;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downRawX = event.getRawX();
                        downRawY = event.getRawY();
                        startTx = v.getTranslationX();
                        startTy = v.getTranslationY();
                        dragging = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dx = event.getRawX() - downRawX;
                        float dy = event.getRawY() - downRawY;
                        if (Math.hypot(dx, dy) >= dp(8)) {
                            dragging = true;
                        }
                        if (dragging && snifferWebContainer != null) {
                            float minTx = -(snifferWebContainer.getWidth() - v.getWidth() - dp(24));
                            float maxTx = dp(6);
                            float minTy = -(snifferWebContainer.getHeight() - v.getHeight() - dp(24));
                            float maxTy = dp(6);
                            float nextTx = Math.max(minTx, Math.min(maxTx, startTx + dx));
                            float nextTy = Math.max(minTy, Math.min(maxTy, startTy + dy));
                            v.setTranslationX(nextTx);
                            v.setTranslationY(nextTy);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (!dragging) {
                            onFloatingDownloadFabClicked();
                        }
                        return true;
                    default:
                        return false;
                }
            }
        });
        return wrap;
    }

    private int getValidSniffCount() {
        synchronized (sniffLock) {
            return sniffedById.size();
        }
    }

    private void updateFloatingFabUi(final boolean pulse) {
        if (fabCircleBtn == null) return;
        final int validCount = getValidSniffCount();
        if (validCount > 0) {
            fabCircleBtn.setBackground(circleBg("#FF3366", "#FFFFFF", 2));
            fabCircleBtn.setText("⬇");
            if (pulse) {
                fabCircleBtn.animate().cancel();
                fabCircleBtn.animate()
                        .scaleX(1.16f)
                        .scaleY(1.16f)
                        .setDuration(140)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                if (fabCircleBtn != null) {
                                    fabCircleBtn.animate().scaleX(1f).scaleY(1f).setDuration(140).start();
                                }
                            }
                        })
                        .start();
            }
        } else if (fabScanningState) {
            fabCircleBtn.setBackground(circleBg("#067EFF", "#FFFFFF", 2));
            fabCircleBtn.setText("⏳");
        } else {
            fabCircleBtn.setBackground(circleBg("#64748B", "#E2E8F0", 2));
            fabCircleBtn.setText("⬇");
        }
    }

    private void onFloatingDownloadFabClicked() {
        triggerHaptic();
        if (snifferWebView != null) {
            injectDomMediaScanner(snifferWebView, false);
        }
        if (getValidSniffCount() > 0) {
            openDownloadDrawerFromBrowser();
            return;
        }
        fabScanningState = true;
        updateFloatingFabUi(false);
        if (snifferWebView != null) {
            injectDomMediaScanner(snifferWebView, true);
        }
        uiHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fabScanningState = false;
                updateFloatingFabUi(false);
                if (getValidSniffCount() > 0) {
                    openDownloadDrawerFromBrowser();
                } else if (promoteLatestNetVideoAsActiveGroup()) {
                    openDownloadDrawerFromBrowser();
                } else {
                    Toast.makeText(
                            MainActivity.this,
                            "请先播放要下载的视频，按钮变红后点击选画质下载",
                            Toast.LENGTH_SHORT).show();
                }
            }
        }, 1100L);
    }

    private void openDownloadDrawerFromBrowser() {
        downloadDrawerOpen = true;
        if (snifferWebView != null) {
            snifferWebView.evaluateJavascript(
                    "document.querySelectorAll('video,audio').forEach(function(e){try{e.pause();}catch(_){}});",
                    null);
        }
        mainWebView.bringToFront();
        if (rootLayout != null) {
            rootLayout.requestLayout();
            rootLayout.invalidate();
        }
        mainWebView.evaluateJavascript("window.openDownloadDrawer && window.openDownloadDrawer();", null);
    }

    private boolean isRealVideoCdnUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        return lower.contains("douyinvod.com")
                || lower.contains("zjcdn.com")
                || lower.contains("bytevcloudcdn.com")
                || lower.contains("bilivideo.com")
                || lower.contains("video.twimg.com")
                || lower.contains(".mp4");
    }

    private boolean isFallbackPlayUrl(String url) {
        return url != null && url.contains("iesdouyin.com/aweme/v1/play");
    }

    private java.util.List<SniffedResource> expandSingleVariantToQualityTiers(SniffedResource base) {
        java.util.List<SniffedResource> expanded = new java.util.ArrayList<SniffedResource>();
        String[] labels = new String[]{"1080P 超清", "720P 高清", "540P 标清"};
        int[] widths = new int[]{base.width > 0 ? base.width : 1080, 720, 540};
        int[] heights = new int[]{base.height > 0 ? base.height : 1920, 1280, 960};
        for (int i = 0; i < labels.length; i++) {
            String id = (i == 0 && base.id != null && !base.id.isEmpty())
                    ? base.id
                    : ("sniff_" + sniffIdSeq.incrementAndGet());
            SniffedResource tier = new SniffedResource(
                    id,
                    base.url,
                    base.referer,
                    base.cookie,
                    base.userAgent,
                    base.uaMode,
                    base.source,
                    "video",
                    base.title,
                    widths[i],
                    heights[i],
                    base.durationSec);
            tier.qualityLabel = labels[i];
            tier.coverUrl = base.coverUrl;
            tier.contentType = base.contentType;
            tier.suggestedName = base.suggestedName;
            if (i == 0 && base.sizeBytes > 0) {
                tier.sizeBytes = base.sizeBytes;
            }
            expanded.add(tier);
        }
        return expanded;
    }

    private boolean promoteLatestNetVideoAsActiveGroup() {
        String netUrl = latestNetVideoUrl;
        if (netUrl == null || !netUrl.startsWith("http")
                || isIgnoredStaticAsset(netUrl.toLowerCase(Locale.ROOT))) {
            return false;
        }
        String pageUrl = (latestNetVideoReferer != null && !latestNetVideoReferer.isEmpty())
                ? latestNetVideoReferer
                : (currentSniffPageUrl != null ? currentSniffPageUrl : "");
        String cookies = (latestNetVideoCookie != null && !latestNetVideoCookie.isEmpty())
                ? latestNetVideoCookie
                : resolveCookiesForUrl(netUrl, pageUrl);
        String activeUaMode = snifferUseMobileUa ? "mobile" : "desktop";
        String activeUa = snifferUseMobileUa ? mobileUa : desktopUa;
        String title = (currentActiveVideoTitle != null && !currentActiveVideoTitle.isEmpty())
                ? currentActiveVideoTitle
                : "当前播放视频";
        String cover = currentActiveCoverUrl != null ? currentActiveCoverUrl : "";
        double dur = currentActiveDurationSec;

        SniffedResource base = new SniffedResource(
                "sniff_" + sniffIdSeq.incrementAndGet(),
                netUrl, pageUrl, cookies, activeUa, activeUaMode,
                "NET", "video", title, 1080, 1920, dur);
        base.coverUrl = cover;
        java.util.List<SniffedResource> variants = expandSingleVariantToQualityTiers(base);
        String vKey = "net:" + netUrl.split("\\?")[0];
        publishActiveVideoGroup(vKey, title, cover, dur, variants);
        return true;
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void openPopupWindow(WebView.WebViewTransport transport) {
        closePopupWindow();
        popupWebView = new WebView(this);
        configureBrowserWebSettings(popupWebView.getSettings(), snifferUseMobileUa);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(popupWebView, true);

        popupWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onCloseWindow(WebView window) {
                closePopupWindow();
            }

            @Override
            public void onReceivedTitle(WebView view, String title) {
                if (popupTitleView != null && title != null && !title.trim().isEmpty()) {
                    popupTitleView.setText(title.trim());
                }
            }
        });
        popupWebView.setWebViewClient(new WebViewClient() {
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
                CookieManager.getInstance().flush();
                if (popupTitleView != null && url != null && url.startsWith("http")) {
                    try {
                        URL u = new URL(url);
                        popupTitleView.setText(u.getHost());
                    } catch (Exception ignored) {
                    }
                }
            }
        });

        popupWebSlot.addView(popupWebView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        popupTitleView.setText("账号授权窗口");
        popupOverlay.setVisibility(View.VISIBLE);
        transport.setWebView(popupWebView);
    }

    private void closePopupWindow() {
        if (popupOverlay != null) {
            popupOverlay.setVisibility(View.GONE);
        }
        if (popupWebSlot != null) {
            popupWebSlot.removeAllViews();
        }
        if (popupWebView != null) {
            try {
                popupWebView.stopLoading();
                popupWebView.onPause();
                popupWebView.destroy();
            } catch (Exception ignored) {
            }
            popupWebView = null;
        }
        CookieManager.getInstance().flush();
    }

    private boolean shouldBlockNonHttpScheme(String url) {
        if (url == null || url.isEmpty()) return false;
        String lower = url.toLowerCase(Locale.ROOT);
        if (lower.startsWith("http://")
                || lower.startsWith("https://")
                || lower.startsWith("about:")
                || lower.startsWith("blob:")
                || lower.startsWith("data:")
                || lower.startsWith("javascript:")) {
            return false;
        }
        return true;
    }

    private String getHeaderIgnoreCase(Map<String, String> headers, String key) {
        if (headers == null || key == null) return "";
        for (Map.Entry<String, String> e : headers.entrySet()) {
            if (e.getKey() != null && key.equalsIgnoreCase(e.getKey()) && e.getValue() != null) {
                return e.getValue();
            }
        }
        return "";
    }

    private void injectDomMediaScanner(final WebView view, final boolean forceScan) {
        if (view == null) return;
        final String js = "(function(){"
                + "if(!window.__joelCatalog){window.__joelCatalog={byId:{},items:[]};}"
                + "function cleanText(s){return String(s||'').replace(/\\s+/g,' ').trim();}"
                + "function isBadStreamUrl(u){"
                + "if(!u||typeof u!=='string'||u.indexOf('http')!==0)return true;"
                + "var l=u.toLowerCase();"
                + "if(l.indexOf('bytednsdoc.com')>=0||l.indexOf('bytegoofy.com')>=0||l.indexOf('bytescm.com')>=0||l.indexOf('lf3-static')>=0||l.indexOf('lf1-cdn-tos')>=0)return true;"
                + "if(l.indexOf('media-audio-')>=0||l.indexOf('/aud/')>=0||l.indexOf('/0/0/')>=0||l.indexOf('init.mp4')>=0||l.indexOf('.m4s')>=0)return true;"
                + "if(/\\.(mp3|m4a|aac|wav|flac|ogg|jpg|jpeg|png|webp|gif|svg|css|js)(\\?|$)/i.test(l))return true;"
                + "return false;"
                + "}"
                + "function isDirectCdnUrl(u){"
                + "var l=String(u||'').toLowerCase();"
                + "return l.indexOf('douyinvod.com')>=0||l.indexOf('zjcdn.com')>=0||l.indexOf('bytevcloudcdn.com')>=0||l.indexOf('bilivideo.com')>=0||l.indexOf('video.twimg.com')>=0;"
                + "}"
                + "function pickBestCdnUrl(list){"
                + "if(!Array.isArray(list))return '';"
                + "for(var i=0;i<list.length;i++){var u=list[i];if(!isBadStreamUrl(u)&&isDirectCdnUrl(u)&&u.toLowerCase().indexOf('media-video-')<0)return u;}"
                + "for(var j=0;j<list.length;j++){var u2=list[j];if(!isBadStreamUrl(u2)&&isDirectCdnUrl(u2))return u2;}"
                + "for(var k=0;k<list.length;k++){var u3=list[k];if(!isBadStreamUrl(u3)&&u3.indexOf('aweme/v1/play')<0)return u3;}"
                + "for(var m=0;m<list.length;m++){if(!isBadStreamUrl(list[m]))return list[m];}"
                + "return '';"
                + "}"
                + "function normKey(u){return String(u||'').split('?')[0];}"
                + "function labelForHeight(h,gear){"
                + "var g=String(gear||'').toLowerCase();"
                + "if(h>=2160||g.indexOf('4k')>=0)return '4K';"
                + "if(h>=1080||g.indexOf('1080')>=0)return '1080P';"
                + "if(h>=720||g.indexOf('720')>=0)return '720P';"
                + "if(h>=540||g.indexOf('540')>=0)return '540P';"
                + "if(h>=480||g.indexOf('480')>=0)return '480P';"
                + "if(h>=360||g.indexOf('360')>=0)return '360P';"
                + "return h>0?(h+'P'):'高清';"
                + "}"
                + "function addVariantUnique(list,seenTier,seenUrl,label,url,w,h,sz){"
                + "if(isBadStreamUrl(url))return;"
                + "var cu=url.replace('/playwm/','/play/');"
                + "if(seenUrl[cu])return;"
                + "var tier=label||'高清';"
                + "if(seenTier[tier])return;"
                + "seenUrl[cu]=1;seenTier[tier]=1;"
                + "list.push({label:tier,url:cu,width:w||0,height:h||0,sizeBytes:sz||0});"
                + "}"
                + "function upsertCatalogEntry(id,title,cover,durSec,variants){"
                + "if(!variants||!variants.length)return;"
                + "var k=id||('cap:'+cleanText(title).slice(0,24));"
                + "if(!k)return;"
                + "var entry={id:k,title:cleanText(title),cover:cover||'',durationSec:durSec>0?durSec:-1,variants:variants};"
                + "window.__joelCatalog.byId[k]=entry;"
                + "for(var i=0;i<variants.length;i++){window.__joelCatalog.byId['u:'+normKey(variants[i].url)]=entry;}"
                + "if(entry.title&&entry.title.length>=4){window.__joelCatalog.byId['t:'+entry.title.slice(0,16)]=entry;}"
                + "var replaced=false;"
                + "for(var j=0;j<window.__joelCatalog.items.length;j++){if(window.__joelCatalog.items[j].id===k){window.__joelCatalog.items[j]=entry;replaced=true;break;}}"
                + "if(!replaced){window.__joelCatalog.items.push(entry);if(window.__joelCatalog.items.length>60)window.__joelCatalog.items.shift();}"
                + "}"
                + "function indexStructuredNode(root,fallbackTitle){"
                + "if(!root||typeof root!=='object')return;"
                + "var q=[{obj:root,depth:0,title:fallbackTitle||''}];var vis=0;"
                + "while(q.length&&vis<5000){"
                + "var cur=q.shift();var o=cur.obj;var d=cur.depth;var ttl=cur.title;"
                + "if(!o||typeof o!=='object'||d>18)continue;vis++;"
                + "if(typeof o.desc==='string'&&o.desc.trim())ttl=o.desc.trim();"
                + "else if(typeof o.item_title==='string'&&o.item_title.trim())ttl=o.item_title.trim();"
                + "else if(typeof o.full_text==='string'&&o.full_text.trim())ttl=o.full_text.trim();"
                + "else if(o.legacy&&typeof o.legacy.full_text==='string'&&o.legacy.full_text.trim())ttl=o.legacy.full_text.trim();"
                + "if(o.video&&typeof o.video==='object'&&((o.video.play_addr&&typeof o.video.play_addr==='object')||(o.video.play_addr_h264&&typeof o.video.play_addr_h264==='object')||Array.isArray(o.video.bit_rate))){"
                + "var vid=String(o.aweme_id||o.awemeId||o.id||'');"
                + "var v=o.video;var dur=Number(v.duration||o.duration||0);"
                + "if(dur>1000)dur=dur/1000;else if(!isFinite(dur)||dur<=0)dur=-1;"
                + "var cov=(v.cover&&Array.isArray(v.cover.url_list)&&v.cover.url_list[0])||(v.origin_cover&&Array.isArray(v.origin_cover.url_list)&&v.origin_cover.url_list[0])||'';"
                + "var vars=[];var sTier={};var sUrl={};"
                + "if(v.play_addr_h264&&Array.isArray(v.play_addr_h264.url_list)){"
                + "var h264U=pickBestCdnUrl(v.play_addr_h264.url_list);"
                + "if(h264U){addVariantUnique(vars,sTier,sUrl,labelForHeight(Number(v.play_addr_h264.height||v.height||0),''),h264U,Number(v.play_addr_h264.width||v.width||0),Number(v.play_addr_h264.height||v.height||0),Number(v.play_addr_h264.data_size||0));}"
                + "}"
                + "if(Array.isArray(v.bit_rate)){"
                + "var sortedBr=v.bit_rate.slice().sort(function(a,b){"
                + "var ah=Number((a.play_addr&&a.play_addr.height)||0);var bh=Number((b.play_addr&&b.play_addr.height)||0);"
                + "if(bh!==ah)return bh-ah;"
                + "return Number(b.bit_rate||0)-Number(a.bit_rate||0);"
                + "});"
                + "for(var bi=0;bi<sortedBr.length;bi++){"
                + "var br=sortedBr[bi];var bpa=br&&br.play_addr;if(!bpa||!Array.isArray(bpa.url_list))continue;"
                + "var vh=Number(bpa.height||0);var vw=Number(bpa.width||0);var vsz=Number(bpa.data_size||0);"
                + "var picked=pickBestCdnUrl(bpa.url_list);"
                + "if(picked){addVariantUnique(vars,sTier,sUrl,labelForHeight(vh,br.gear_name),picked,vw,vh,vsz);}"
                + "}"
                + "}"
                + "if(v.play_addr&&Array.isArray(v.play_addr.url_list)){"
                + "var paH=Number(v.height||(v.play_addr&&v.play_addr.height)||0);var paW=Number(v.width||(v.play_addr&&v.play_addr.width)||0);var paSz=Number(v.play_addr.data_size||0);"
                + "var pu=pickBestCdnUrl(v.play_addr.url_list);"
                + "if(pu){addVariantUnique(vars,sTier,sUrl,labelForHeight(paH,''),pu,paW,paH,paSz);}"
                + "}"
                + "if(!vars.length&&v.play_addr&&typeof v.play_addr.uri==='string'&&v.play_addr.uri.length>5){"
                + "var uri=encodeURIComponent(v.play_addr.uri);"
                + "addVariantUnique(vars,sTier,sUrl,'1080P','https://www.iesdouyin.com/aweme/v1/play/?video_id='+uri+'&ratio=1080p&line=0',0,1080,0);"
                + "addVariantUnique(vars,sTier,sUrl,'720P','https://www.iesdouyin.com/aweme/v1/play/?video_id='+uri+'&ratio=720p&line=0',0,720,0);"
                + "addVariantUnique(vars,sTier,sUrl,'540P','https://www.iesdouyin.com/aweme/v1/play/?video_id='+uri+'&ratio=540p&line=0',0,540,0);"
                + "}"
                + "upsertCatalogEntry(vid,ttl,cov,dur,vars);"
                + "}"
                + "if(o.video_info&&Array.isArray(o.video_info.variants)){"
                + "var xid=String(o.id_str||o.conversation_id_str||o.media_key||'');"
                + "var mp4v=[];for(var xi=0;xi<o.video_info.variants.length;xi++){var xr=o.video_info.variants[xi];if(xr&&xr.url&&(xr.content_type==='video/mp4'||xr.url.indexOf('.mp4')>=0)&&!isBadStreamUrl(xr.url))mp4v.push(xr);}"
                + "mp4v.sort(function(a,b){return Number(b.bitrate||0)-Number(a.bitrate||0);});"
                + "var xvars=[];var xsT={};var xsU={};"
                + "for(var xm=0;xm<mp4v.length;xm++){"
                + "var xu=mp4v[xm].url;var dm=xu.match(/\\/(\\d+)x(\\d+)\\//);"
                + "var xw=dm?Number(dm[1]):0;var xh=dm?Number(dm[2]):0;"
                + "var xl=xh?labelForHeight(xh,''):('画质'+(xm+1));"
                + "addVariantUnique(xvars,xsT,xsU,xl,xu,xw,xh,0);"
                + "}"
                + "if(xvars.length){upsertCatalogEntry(xid,ttl,o.media_url_https||'',Number(o.video_info.duration_millis||0)/1000,xvars);}"
                + "}"
                + "if(Array.isArray(o)){for(var a=0;a<o.length&&a<80;a++){if(o[a]&&typeof o[a]==='object')q.push({obj:o[a],depth:d+1,title:ttl});}}"
                + "else{var ks=Object.keys(o);for(var k=0;k<ks.length;k++){var ch=o[ks[k]];if(ch&&typeof ch==='object')q.push({obj:ch,depth:d+1,title:ttl});}}"
                + "}"
                + "}"
                + "function findPageContainerCaptionAndCover(v){"
                + "var el=v;var cap='';var cov=v.poster||'';"
                + "for(var step=0;step<8&&el&&el!==document.body;step++){"
                + "el=el.parentElement;if(!el)break;"
                + "if(!cap){"
                + "var cands=el.querySelectorAll('[data-e2e=\"video-desc\"],[data-testid=\"tweetText\"],.video-info-detail,[class*=\"desc\"],[class*=\"Desc\"],[class*=\"title\"],h1');"
                + "for(var ci=0;ci<cands.length;ci++){var txt=cleanText(cands[ci].innerText||cands[ci].textContent);if(txt&&txt.length>=3&&txt.length<200){cap=txt;break;}}"
                + "}"
                + "if(!cov){"
                + "var imgs=el.querySelectorAll('img');"
                + "for(var ii=0;ii<imgs.length;ii++){var ir=imgs[ii].getBoundingClientRect();var isrc=imgs[ii].currentSrc||imgs[ii].src||'';if(isrc.indexOf('http')===0&&ir.width>=120&&ir.height>=120&&!/(avatar|icon|logo)/i.test(isrc)){cov=isrc;break;}}"
                + "}"
                + "var vidAttr=el.getAttribute('data-e2e-vid')||el.getAttribute('data-aweme-id')||'';"
                + "if(vidAttr)return {cap:cap,cov:cov,domId:vidAttr};"
                + "}"
                + "return {cap:cap,cov:cov,domId:''};"
                + "}"
                + "function pickActiveVideo(){"
                + "var vids=document.querySelectorAll('video');var best=null;var bestScore=-1;"
                + "var vh=window.innerHeight||800;"
                + "for(var i=0;i<vids.length;i++){"
                + "var v=vids[i];var src=v.currentSrc||v.src||(v.querySelector('source')?v.querySelector('source').src:'')||'';"
                + "if(src&&src.indexOf('http')===0&&isBadStreamUrl(src))continue;"
                + "var r=v.getBoundingClientRect();if(r.width<140||r.height<140)continue;"
                + "var cy=r.top+r.height/2;if(cy<0||cy>vh)continue;"
                + "var sc=(!v.paused?1000:0)+(v.readyState>=2?200:0)+(200-Math.min(200,Math.abs(cy-vh/2)));"
                + "if(sc>bestScore){bestScore=sc;best=v;}"
                + "}"
                + "return best;"
                + "}"
                + "function matchCatalogEntry(domId,src,cap){"
                + "var m=location.href.match(/(?:video|status|note)\\/(\\d{6,})/)||location.href.match(/(?:modal_id|aweme_id)=(\\d{6,})/);"
                + "var urlId=m?m[1]:'';"
                + "var cat=window.__joelCatalog.byId;"
                + "if(domId&&cat[domId])return cat[domId];"
                + "if(urlId&&cat[urlId])return cat[urlId];"
                + "if(src&&src.indexOf('http')===0&&cat['u:'+normKey(src)])return cat['u:'+normKey(src)];"
                + "if(cap&&cap.length>=4&&cat['t:'+cap.slice(0,16)])return cat['t:'+cap.slice(0,16)];"
                + "return null;"
                + "}"
                + "window.__joelReportActiveVideo=function(){"
                + "try{"
                + "if(window._ROUTER_DATA&&!window.__joelRouterDone){window.__joelRouterDone=1;indexStructuredNode(window._ROUTER_DATA,document.title);}"
                + "if(window.__INITIAL_SSR_STATE__&&!window.__joelSsrDone){window.__joelSsrDone=1;indexStructuredNode(window.__INITIAL_SSR_STATE__,document.title);}"
                + "var rd=document.getElementById('RENDER_DATA');"
                + "if(rd&&!window.__joelRdDone&&rd.textContent){window.__joelRdDone=1;try{indexStructuredNode(JSON.parse(decodeURIComponent(rd.textContent)),document.title);}catch(__){}}"
                + "var v=pickActiveVideo();"
                + "if(v){"
                + "var src=v.currentSrc||v.src||(v.querySelector('source')?v.querySelector('source').src:'')||'';"
                + "var info=findPageContainerCaptionAndCover(v);"
                + "var matched=matchCatalogEntry(info.domId,src,info.cap);"
                + "var title=cleanText(info.cap||(matched&&matched.title)||document.title||'').replace(/\\s*-\\s*抖音.*$/,'');"
                + "var cover=info.cov||(matched&&matched.cover)||'';"
                + "var dur=(isFinite(v.duration)&&v.duration>0)?v.duration:(matched&&matched.durationSec>0?matched.durationSec:-1);"
                + "var vw=Number(v.videoWidth||0)||0;var vh2=Number(v.videoHeight||0)||0;"
                + "var variants=[];var sTier={};var sUrl={};"
                + "if(matched&&matched.variants&&matched.variants.length){"
                + "for(var mi=0;mi<matched.variants.length;mi++){var mv=matched.variants[mi];addVariantUnique(variants,sTier,sUrl,mv.label,mv.url,mv.width,mv.height,mv.sizeBytes);}"
                + "}"
                + "if(src&&src.indexOf('http')===0&&!isBadStreamUrl(src)){"
                + "if(src.indexOf('/aweme/v1/play')>=0&&src.indexOf('ratio=')>=0){"
                + "addVariantUnique(variants,sTier,sUrl,'1080P',src.replace(/ratio=[^&]+/,'ratio=1080p'),vw,1080,0);"
                + "addVariantUnique(variants,sTier,sUrl,'720P',src.replace(/ratio=[^&]+/,'ratio=720p'),vw,720,0);"
                + "addVariantUnique(variants,sTier,sUrl,'540P',src.replace(/ratio=[^&]+/,'ratio=540p'),vw,540,0);"
                + "}else{"
                + "addVariantUnique(variants,sTier,sUrl,labelForHeight(vh2,''),src,vw,vh2,0);"
                + "}"
                + "}"
                + "var vKey=(matched&&matched.id)||info.domId||(variants.length?normKey(variants[0].url):(src.indexOf('blob:')===0?('blob:'+title.slice(0,20)):normKey(src)));"
                + "if(vKey&&(variants.length||src.indexOf('blob:')===0)){"
                + "var payload={videoKey:vKey,title:title,coverUrl:cover,durationSec:dur,pageUrl:location.href,useLatestNetStream:(variants.length===0&&src.indexOf('blob:')===0),width:vw,height:vh2,variants:variants};"
                + "var vSig='';for(var si=0;si<variants.length;si++){vSig+=(si?',':'')+variants[si].label+':'+variants[si].url;}"
                + "var sig=vKey+'|'+variants.length+'|'+(vSig||'blob');"
                + "if(sig!==window.__joelLastActiveSig){window.__joelLastActiveSig=sig;if(window.JoelSniffHook&&window.JoelSniffHook.onActiveVideoGroup){window.JoelSniffHook.onActiveVideoGroup(JSON.stringify(payload));}}"
                + "return;"
                + "}"
                + "}"
                + "var detailMatch=matchCatalogEntry('','','');"
                + "if(detailMatch&&detailMatch.variants&&detailMatch.variants.length){"
                + "var dSig=detailMatch.id+'|'+detailMatch.variants.length;"
                + "if(dSig!==window.__joelLastActiveSig){window.__joelLastActiveSig=dSig;if(window.JoelSniffHook&&window.JoelSniffHook.onActiveVideoGroup){window.JoelSniffHook.onActiveVideoGroup(JSON.stringify({videoKey:detailMatch.id,title:detailMatch.title,coverUrl:detailMatch.cover,durationSec:detailMatch.durationSec,pageUrl:location.href,useLatestNetStream:false,width:0,height:0,variants:detailMatch.variants}));}}"
                + "}"
                + "}catch(e){}"
                + "};"
                + "if(!window.__joelSniffInstalled){"
                + "window.__joelSniffInstalled=true;"
                + "try{var origPlay=HTMLMediaElement.prototype.play;HTMLMediaElement.prototype.play=function(){setTimeout(function(){window.__joelReportActiveVideo&&window.__joelReportActiveVideo();},260);return origPlay.apply(this,arguments);};}catch(_){}"
                + "try{if(window.fetch&&!window.fetch.__joelHooked){var origFetch=window.fetch;window.fetch=function(){return origFetch.apply(this,arguments).then(function(resp){try{var ct=(resp.headers&&resp.headers.get('content-type'))||'';if(ct.indexOf('application/json')>=0||ct.indexOf('text/plain')>=0){resp.clone().text().then(function(txt){if(txt&&txt.length>20&&txt.length<1400000){try{indexStructuredNode(JSON.parse(txt),document.title);window.__joelReportActiveVideo&&window.__joelReportActiveVideo();}catch(__){}}}).catch(function(){});}}catch(_){}return resp;});};window.fetch.__joelHooked=true;}}catch(_){}"
                + "try{var origOpen=XMLHttpRequest.prototype.open;XMLHttpRequest.prototype.open=function(m,u){this.addEventListener('load',function(){try{var ct=(this.getResponseHeader('content-type')||'').toLowerCase();if((ct.indexOf('application/json')>=0||ct.indexOf('text/plain')>=0)&&typeof this.responseText==='string'&&this.responseText.length>20&&this.responseText.length<1400000){indexStructuredNode(JSON.parse(this.responseText),document.title);window.__joelReportActiveVideo&&window.__joelReportActiveVideo();}}catch(_){}});return origOpen.apply(this,arguments);};}catch(_){}"
                + "setInterval(function(){window.__joelReportActiveVideo&&window.__joelReportActiveVideo();},900);"
                + "}"
                + (forceScan ? "window.__joelLastActiveSig='';window.__joelRouterDone=0;window.__joelSsrDone=0;window.__joelRdDone=0;" : "")
                + "window.__joelReportActiveVideo();"
                + "if(" + (forceScan ? "true" : "false") + "){setTimeout(function(){window.__joelReportActiveVideo();},700);}"
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

    private void applySnifferUa(boolean useMobile) {
        snifferUseMobileUa = useMobile;
        if (snifferWebView != null) {
            WebSettings s = snifferWebView.getSettings();
            s.setUseWideViewPort(!useMobile);
            s.setLoadWithOverviewMode(!useMobile);
            s.setUserAgentString(useMobile ? mobileUa : desktopUa);
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
                || noQuery.endsWith(".map")
                || noQuery.endsWith(".m4s")
                || noQuery.endsWith(".ts")
                || noQuery.endsWith(".mp3")
                || noQuery.endsWith(".m4a")
                || noQuery.endsWith(".wav")
                || noQuery.endsWith(".flac")
                || noQuery.endsWith(".ogg")
                || noQuery.endsWith(".aac")
                || noQuery.endsWith(".webp")
                || noQuery.endsWith(".jpg")
                || noQuery.endsWith(".jpeg")
                || noQuery.endsWith(".png")
                || noQuery.endsWith(".gif")) {
            return true;
        }
        if (lower.contains("media-audio-und")
                || lower.contains("mime=audio")
                || lower.contains("mime_type=audio")
                || lower.contains("ies-music")) {
            return true;
        }
        if ((lower.contains("video.twimg.com") || lower.contains("twimg.com/"))
                && (lower.contains("/aud/")
                        || lower.contains("/0/0/")
                        || lower.contains("init.mp4")
                        || lower.contains(".m4s"))) {
            return true;
        }
        return lower.contains("bytednsdoc.com")
                || lower.contains("bytegoofy.com")
                || lower.contains("bytescm.com")
                || lower.contains("lf3-static")
                || lower.contains("lf1-cdn-tos")
                || lower.contains("mon.zijieapi.com")
                || lower.contains("mcs.snssdk.com")
                || lower.contains("doubleclick.net")
                || lower.contains("google-analytics.com")
                || lower.contains("arkoselabs.com");
    }

    private String detectSniffTypeFromRequest(String url, String secFetchDest, String acceptHeader) {
        if (url == null || !url.startsWith("http")) return null;
        String lower = url.toLowerCase(Locale.ROOT);
        if (isIgnoredStaticAsset(lower)) return null;

        String dest = secFetchDest != null ? secFetchDest.toLowerCase(Locale.ROOT) : "";
        String acc = acceptHeader != null ? acceptHeader.toLowerCase(Locale.ROOT) : "";
        int q = lower.indexOf('?');
        String noQuery = q > 0 ? lower.substring(0, q) : lower;

        if ("image".equals(dest) || acc.startsWith("image/") || "audio".equals(dest) || acc.startsWith("audio/")) {
            return null;
        }

        if ("video".equals(dest)
                || acc.startsWith("video/")
                || noQuery.endsWith(".mp4")
                || noQuery.endsWith(".mkv")
                || noQuery.endsWith(".mov")
                || noQuery.endsWith(".webm")
                || noQuery.endsWith(".flv")
                || noQuery.endsWith(".m3u8")
                || lower.contains("douyinvod.com")
                || lower.contains("zjcdn.com")
                || lower.contains("bytevcloudcdn.com")
                || lower.contains("ixigua.com")
                || lower.contains("snssdk.com/aweme/")
                || lower.contains("iesdouyin.com/aweme/")
                || lower.contains("/aweme/v1/play")
                || lower.contains("/video/tos/")
                || lower.contains("bilivideo.com")
                || lower.contains("video.twimg.com")
                || lower.contains("twimg.com/ext_tw_video")
                || lower.contains("googlevideo.com/videoplayback")
                || lower.contains(".mp4?")
                || lower.contains("mime=video")
                || lower.contains("mime_type=video")) {
            return "video";
        }
        return null;
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
        if (noQuery.endsWith(".webp")
                || noQuery.endsWith(".jpg")
                || noQuery.endsWith(".jpeg")
                || noQuery.endsWith(".png")
                || noQuery.endsWith(".gif")
                || lower.contains("douyinpic.com")
                || lower.contains("byteimg.com")
                || lower.contains("toutiaoimg.com")
                || lower.contains("douyinstatic.com")
                || lower.contains("pbs.twimg.com/media")) {
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
            String activeUa = snifferUseMobileUa ? mobileUa : desktopUa;
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
            count = seenSniffUrls.size();
        }

        final String jsPayload;
        try {
            jsPayload = existingOrNew.toJson(previewProxyPort).toString();
        } catch (JSONException e) {
            return;
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateFloatingFabUi(true);
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

    private void emitSniffedUpdateToJs(final SniffedResource res, final boolean invalid) {
        if (res == null) return;
        synchronized (sniffLock) {
            if (!sniffedById.containsKey(res.id)) {
                return;
            }
            if (invalid) {
                sniffedById.remove(res.id);
                sniffedByUrl.remove(res.url);
                seenSniffUrls.remove(res.url);
            }
        }
        final String jsPayload;
        try {
            JSONObject obj = res.toJson(previewProxyPort);
            obj.put("invalid", invalid);
            jsPayload = obj.toString();
        } catch (JSONException e) {
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateFloatingFabUi(false);
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

                // Never drop valid CDN streams just because background probe lacked a one-shot token (403/404)
                if (code >= 400) {
                    return;
                }

                String ct = conn.getContentType();
                String lowerCt = ct != null ? ct.toLowerCase(Locale.ROOT) : "";
                boolean isNonMediaPage = "NET".equals(res.source)
                        && code == 200
                        && (lowerCt.startsWith("text/html")
                                || lowerCt.startsWith("text/plain")
                                || lowerCt.startsWith("application/json")
                                || lowerCt.startsWith("application/javascript"))
                        && !res.url.toLowerCase(Locale.ROOT).contains(".m3u8");
                if (isNonMediaPage) {
                    emitSniffedUpdateToJs(res, true);
                    return;
                }

                if (ct != null && !ct.isEmpty()) {
                    int semi = ct.indexOf(';');
                    res.contentType = semi > 0 ? ct.substring(0, semi).trim() : ct.trim();
                    if (!"DOM".equals(res.source)
                            && !res.contentType.toLowerCase(Locale.ROOT).startsWith("application/octet-stream")) {
                        res.mediaType = classifyMediaType(res.url, res.contentType);
                    }
                }

                long totalSize = parseTotalBytesFromHeaders(conn);
                if (totalSize > 0) {
                    res.sizeBytes = totalSize;
                }

                // Filter out tiny fMP4 init headers (e.g. 786 B X audio init box) and tiny UI icons (< 18 KB)
                if (!"DOM".equals(res.source)) {
                    if ("video".equals(res.mediaType)
                            && totalSize > 0
                            && totalSize < 32768L
                            && !res.url.toLowerCase(Locale.ROOT).contains(".m3u8")) {
                        emitSniffedUpdateToJs(res, true);
                        return;
                    }
                    if ("image".equals(res.mediaType)
                            && totalSize > 0
                            && totalSize < 15360L) {
                        emitSniffedUpdateToJs(res, true);
                        return;
                    }
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

                // Propagate probed size/mime to sibling quality variants sharing the same URL
                java.util.List<SniffedResource> matches = new java.util.ArrayList<SniffedResource>();
                synchronized (sniffLock) {
                    for (SniffedResource cand : sniffedById.values()) {
                        if (res.url.equals(cand.url)) {
                            if (res.sizeBytes > 0) cand.sizeBytes = res.sizeBytes;
                            if (res.contentType != null && !res.contentType.isEmpty()) cand.contentType = res.contentType;
                            if (res.suggestedName != null && !res.suggestedName.isEmpty()) cand.suggestedName = res.suggestedName;
                            matches.add(cand);
                        }
                    }
                }
                if (matches.isEmpty()) {
                    emitSniffedUpdateToJs(res, false);
                } else {
                    for (SniffedResource m : matches) {
                        emitSniffedUpdateToJs(m, false);
                    }
                }
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

    private void publishActiveVideoGroup(
            final String videoKey,
            final String title,
            final String coverUrl,
            final double durationSec,
            final java.util.List<SniffedResource> rawVariants) {
        if (rawVariants == null || rawVariants.isEmpty()) return;

        final java.util.List<SniffedResource> finalVariants =
                rawVariants.size() == 1
                        ? expandSingleVariantToQualityTiers(rawVariants.get(0))
                        : rawVariants;

        final boolean isNewVideo;
        final JSONObject groupJson = new JSONObject();
        try {
            synchronized (sniffLock) {
                isNewVideo = !videoKey.equals(currentActiveVideoKey);
                clearSniffStateLocked();
                currentActiveVideoKey = videoKey;
                currentActiveVideoTitle = title;
                currentActiveCoverUrl = coverUrl;
                currentActiveDurationSec = durationSec;

                JSONArray jsVars = new JSONArray();
                for (SniffedResource r : finalVariants) {
                    seenSniffUrls.add(r.url);
                    sniffedByUrl.put(r.url, r);
                    sniffedById.put(r.id, r);
                    jsVars.put(r.toJson(previewProxyPort));
                }
                groupJson.put("videoKey", videoKey);
                groupJson.put("title", title);
                groupJson.put("coverUrl", coverUrl);
                groupJson.put("durationSec", durationSec);
                groupJson.put("variants", jsVars);
            }
        } catch (JSONException ignored) {
            return;
        }

        final String payloadStr = groupJson.toString();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateFloatingFabUi(isNewVideo);
                if (mainWebView != null) {
                    mainWebView.evaluateJavascript(
                            "window.onActiveVideoGroupUpdate && window.onActiveVideoGroupUpdate(" + payloadStr + ");",
                            null);
                }
            }
        });

        java.util.Set<String> scheduledUrls = new java.util.HashSet<String>();
        for (final SniffedResource r : finalVariants) {
            if (r.url == null || !scheduledUrls.add(r.url)) continue;
            probeExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    probeSniffedResourceAsync(r);
                }
            });
        }
    }

    private class JoelSniffHook {
        @JavascriptInterface
        public void onActiveVideoGroup(String jsonStr) {
            if (jsonStr == null || jsonStr.trim().isEmpty()) return;
            try {
                JSONObject obj = new JSONObject(jsonStr);
                final String videoKey = obj.optString("videoKey", "").trim();
                final String title = obj.optString("title", "").trim();
                final String coverUrl = obj.optString("coverUrl", "").trim();
                final double durationSec = obj.optDouble("durationSec", -1.0);
                final String pageUrl = obj.optString("pageUrl", currentSniffPageUrl != null ? currentSniffPageUrl : "");
                final boolean useLatestNet = obj.optBoolean("useLatestNetStream", false);
                final int domW = obj.optInt("width", 0);
                final int domH = obj.optInt("height", 0);
                JSONArray variantsArr = obj.optJSONArray("variants");

                final java.util.List<SniffedResource> nextVariants = new java.util.ArrayList<SniffedResource>();
                String activeUaMode = snifferUseMobileUa ? "mobile" : "desktop";
                String activeUa = snifferUseMobileUa ? mobileUa : desktopUa;

                if (variantsArr != null) {
                    for (int i = 0; i < variantsArr.length(); i++) {
                        JSONObject vo = variantsArr.optJSONObject(i);
                        if (vo == null) continue;
                        String vUrl = vo.optString("url", "").trim();
                        if (vUrl.isEmpty() || !vUrl.startsWith("http")) continue;
                        if (isIgnoredStaticAsset(vUrl.toLowerCase(Locale.ROOT))) continue;

                        String id = "sniff_" + sniffIdSeq.incrementAndGet();
                        String qLabel = vo.optString("label", "高清").trim();
                        int w = vo.optInt("width", domW);
                        int h = vo.optInt("height", domH);
                        long sz = vo.optLong("sizeBytes", -1L);
                        String cookies = resolveCookiesForUrl(vUrl, pageUrl);

                        SniffedResource r = new SniffedResource(
                                id, vUrl, pageUrl, cookies, activeUa, activeUaMode,
                                "DOM", "video", title, w, h, durationSec);
                        r.qualityLabel = qLabel.isEmpty() ? "高清" : qLabel;
                        r.coverUrl = coverUrl;
                        if (sz > 0) r.sizeBytes = sz;
                        nextVariants.add(r);
                    }
                }

                boolean allFallback = !nextVariants.isEmpty();
                for (SniffedResource r : nextVariants) {
                    if (!isFallbackPlayUrl(r.url)) {
                        allFallback = false;
                        break;
                    }
                }

                if ((nextVariants.isEmpty() && useLatestNet) || allFallback) {
                    String netUrl = latestNetVideoUrl;
                    if (netUrl != null && netUrl.startsWith("http")
                            && !isIgnoredStaticAsset(netUrl.toLowerCase(Locale.ROOT))) {
                        nextVariants.clear();
                        String id = "sniff_" + sniffIdSeq.incrementAndGet();
                        String ref = (latestNetVideoReferer != null && !latestNetVideoReferer.isEmpty())
                                ? latestNetVideoReferer : pageUrl;
                        String cookies = (latestNetVideoCookie != null && !latestNetVideoCookie.isEmpty())
                                ? latestNetVideoCookie : resolveCookiesForUrl(netUrl, ref);
                        SniffedResource r = new SniffedResource(
                                id, netUrl, ref, cookies, activeUa, activeUaMode,
                                "NET", "video", title, domW, domH, durationSec);
                        r.qualityLabel = domH >= 1080 ? "1080P 超清" : (domH >= 720 ? "720P 高清" : (domH > 0 ? domH + "P" : "高清原画"));
                        r.coverUrl = coverUrl;
                        nextVariants.add(r);
                    }
                }

                if (nextVariants.isEmpty()) return;
                publishActiveVideoGroup(videoKey, title, coverUrl, durationSec, nextVariants);
            } catch (Exception ignored) {
            }
        }
    }

    private void clearSniffStateLocked() {
        seenSniffUrls.clear();
        sniffedByUrl.clear();
        sniffedById.clear();
    }

    private void clearSniffStateAndSyncJs() {
        synchronized (sniffLock) {
            clearSniffStateLocked();
            currentActiveVideoKey = "";
            currentActiveVideoTitle = "";
            currentActiveCoverUrl = "";
            currentActiveDurationSec = -1.0;
            latestNetVideoUrl = "";
            latestNetVideoReferer = "";
            latestNetVideoCookie = "";
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateFloatingFabUi(false);
                if (mainWebView != null) {
                    mainWebView.evaluateJavascript(
                            "window.onSniffedCleared && window.onSniffedCleared();",
                            null);
                }
            }
        });
    }

    private void openSnifferOverlay(final String rawUrl) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                closePopupWindow();
                clearSniffStateAndSyncJs();
                fabScanningState = false;
                downloadDrawerOpen = false;
                currentBottomTab = "browser";
                browserHasPageLoaded = true;
                browserShowingInTab1 = true;
                snifferOverlay.setVisibility(View.VISIBLE);
                snifferOverlay.bringToFront();
                applySnifferUa(true);
                String clean = extractCleanUrlFromText(rawUrl);
                String target = !clean.isEmpty() ? clean : (rawUrl != null ? rawUrl.trim() : "");
                loadSnifferUrl(target, false);
                mainWebView.evaluateJavascript(
                        "window.onBrowserVisibilityChanged && window.onBrowserVisibilityChanged(true, "
                                + JSONObject.quote(currentSniffPageUrl != null ? currentSniffPageUrl : "") + ");",
                        null);
            }
        });
    }

    private void loadSnifferUrl(String rawUrl, boolean clearPrevious) {
        String raw = rawUrl != null ? rawUrl.trim() : "";
        if (raw.isEmpty()) return;
        if (clearPrevious) {
            clearSniffStateAndSyncJs();
        }
        String clean = extractCleanUrlFromText(raw);
        String u;
        if (!clean.isEmpty()) {
            u = clean;
        } else if (DOMAIN_LIKE_PATTERN.matcher(raw).matches()) {
            u = "https://" + raw;
        } else {
            try {
                u = "https://www.bing.com/search?q=" + URLEncoder.encode(raw, "UTF-8");
            } catch (Exception e) {
                Toast.makeText(this, "未识别到有效链接", Toast.LENGTH_SHORT).show();
                return;
            }
        }
        currentSniffPageUrl = u;
        if (snifferUrlInput != null) {
            snifferUrlInput.setText(u);
        }
        snifferWebView.loadUrl(u);
        mainWebView.evaluateJavascript(
                "window.onBrowserPageChanged && window.onBrowserPageChanged(" + JSONObject.quote(u) + ");",
                null);
    }

    private void minimizeBrowserToHome() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                closePopupWindow();
                downloadDrawerOpen = false;
                browserShowingInTab1 = false;
                if (snifferWebView != null) {
                    snifferWebView.evaluateJavascript(
                            "document.querySelectorAll('video,audio').forEach(function(e){try{e.pause();}catch(_){}});",
                            null);
                }
                snifferOverlay.setVisibility(View.GONE);
                mainWebView.bringToFront();
                mainWebView.evaluateJavascript(
                        "window.onBrowserVisibilityChanged && window.onBrowserVisibilityChanged(false, "
                                + JSONObject.quote(currentSniffPageUrl != null ? currentSniffPageUrl : "") + ");",
                        null);
            }
        });
    }

    private void resumeBrowserPage() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!browserHasPageLoaded) return;
                downloadDrawerOpen = false;
                browserShowingInTab1 = true;
                currentBottomTab = "browser";
                snifferOverlay.setVisibility(View.VISIBLE);
                snifferOverlay.bringToFront();
                mainWebView.evaluateJavascript(
                        "window.onBrowserVisibilityChanged && window.onBrowserVisibilityChanged(true, "
                                + JSONObject.quote(currentSniffPageUrl != null ? currentSniffPageUrl : "") + ");",
                        null);
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
        if (downloadDrawerOpen && mainWebView != null) {
            mainWebView.evaluateJavascript("window.closeDownloadDrawer && window.closeDownloadDrawer();", null);
            return;
        }
        if (snifferOverlay != null && snifferOverlay.getVisibility() == View.VISIBLE) {
            if (popupOverlay != null && popupOverlay.getVisibility() == View.VISIBLE) {
                if (popupWebView != null && popupWebView.canGoBack()) {
                    popupWebView.goBack();
                } else {
                    closePopupWindow();
                }
                return;
            }
            if (snifferWebView.canGoBack()) {
                snifferWebView.goBack();
            } else {
                minimizeBrowserToHome();
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
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && pageLoaded && mainWebView != null) {
            mainWebView.evaluateJavascript("window.onAppWindowFocus && window.onAppWindowFocus();", null);
        }
    }

    @Override
    protected void onDestroy() {
        uiHandler.removeCallbacksAndMessages(null);
        closePopupWindow();
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

    private GradientDrawable circleBg(String fillHex, String strokeHex, int strokeDp) {
        GradientDrawable g = new GradientDrawable();
        g.setShape(GradientDrawable.OVAL);
        g.setColor(Color.parseColor(fillHex));
        if (strokeHex != null && strokeDp > 0) {
            g.setStroke(dp(strokeDp), Color.parseColor(strokeHex));
        }
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
                            res.userAgent != null && !res.userAgent.isEmpty() ? res.userAgent : mobileUa);
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
            final DownloadTask t = downloader.enqueue(url, fileName, threads, referer, cookie, uaMode);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(
                            MainActivity.this,
                            "⚡ 已启动 " + t.threads + " 线程高速下载：" + t.fileName,
                            Toast.LENGTH_SHORT).show();
                }
            });
            JSONObject res = new JSONObject();
            try {
                res.put("ok", true);
                res.put("id", t.id);
                res.put("threads", t.threads);
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

        @JavascriptInterface
        public void resumeBrowserPage() {
            MainActivity.this.resumeBrowserPage();
        }

        @JavascriptInterface
        public void minimizeBrowserToHome() {
            MainActivity.this.minimizeBrowserToHome();
        }

        @JavascriptInterface
        public void onBottomTabChanged(final String tab) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    currentBottomTab = tab != null ? tab : "browser";
                    if ("browser".equals(currentBottomTab)) {
                        if (browserHasPageLoaded && browserShowingInTab1 && !downloadDrawerOpen) {
                            snifferOverlay.setVisibility(View.VISIBLE);
                            snifferOverlay.bringToFront();
                        } else {
                            snifferOverlay.setVisibility(View.GONE);
                            mainWebView.bringToFront();
                        }
                    } else {
                        downloadDrawerOpen = false;
                        if (snifferWebView != null) {
                            snifferWebView.evaluateJavascript(
                                    "document.querySelectorAll('video,audio').forEach(function(e){try{e.pause();}catch(_){}});",
                                    null);
                        }
                        snifferOverlay.setVisibility(View.GONE);
                        mainWebView.bringToFront();
                    }
                }
            });
        }

        @JavascriptInterface
        public void onDownloadDrawerOpened() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    downloadDrawerOpen = true;
                    mainWebView.bringToFront();
                    if (rootLayout != null) {
                        rootLayout.requestLayout();
                        rootLayout.invalidate();
                    }
                }
            });
        }

        @JavascriptInterface
        public void onDownloadDrawerClosed() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    downloadDrawerOpen = false;
                    if ("browser".equals(currentBottomTab) && browserHasPageLoaded && browserShowingInTab1) {
                        snifferOverlay.setVisibility(View.VISIBLE);
                        snifferOverlay.bringToFront();
                    } else {
                        mainWebView.bringToFront();
                    }
                }
            });
        }

        @JavascriptInterface
        public void clearSniffedResources() {
            clearSniffStateAndSyncJs();
        }

        @JavascriptInterface
        public void clearBrowserData() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (snifferWebView != null) {
                        snifferWebView.clearCache(true);
                        snifferWebView.clearHistory();
                    }
                    CookieManager.getInstance().removeAllCookies(null);
                    CookieManager.getInstance().flush();
                    Toast.makeText(MainActivity.this, "已清除浏览器缓存与 Cookie", Toast.LENGTH_SHORT).show();
                }
            });
        }

        @JavascriptInterface
        public void hapticTick() {
            triggerHaptic();
        }
    }

    private void triggerHaptic() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (rootLayout != null) {
                    rootLayout.performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY);
                }
            }
        });
    }
}
