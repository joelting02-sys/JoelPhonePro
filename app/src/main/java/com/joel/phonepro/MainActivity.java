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
import android.os.Message;
import android.text.InputType;
import android.text.TextUtils;
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

    // Native Web Media Sniffer overlay + Multi-window OAuth Popup
    private LinearLayout snifferOverlay;
    private FrameLayout snifferWebContainer;
    private WebView snifferWebView;
    private EditText snifferUrlInput;
    private Button snifferUaBtn;
    private TextView snifferCountBadge;
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

    private LocalPreviewProxy previewProxy;
    private volatile int previewProxyPort = 0;

    private volatile boolean pageLoaded = false;
    private volatile String pendingSharedText = "";
    private volatile String currentSniffPageUrl = "";
    private volatile boolean snifferUseMobileUa = true;

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
            obj.put("uaMode", uaMode != null ? uaMode : "mobile");
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

        initUserAgents();
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
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE);
        s.setUserAgentString(useMobile ? mobileUa : desktopUa);
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
        LinearLayout.LayoutParams closeLp = new LinearLayout.LayoutParams(dp(36), dp(36));
        closeLp.rightMargin = dp(5);
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
        snifferUrlInput.setPadding(dp(8), dp(5), dp(8), dp(5));
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
        urlLp.rightMargin = dp(5);
        bar.addView(snifferUrlInput, urlLp);

        Button pasteBtn = new Button(this);
        pasteBtn.setText("📋");
        pasteBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        pasteBtn.setTextColor(Color.parseColor("#0F172A"));
        pasteBtn.setBackground(pillBg("#F3F4F6", 10));
        pasteBtn.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams pasteLp = new LinearLayout.LayoutParams(dp(34), dp(36));
        pasteLp.rightMargin = dp(5);
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
        snifferUaBtn.setText("📱");
        snifferUaBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        snifferUaBtn.setTextColor(Color.parseColor("#1D4ED8"));
        snifferUaBtn.setBackground(pillBg("#EFF6FF", 10));
        snifferUaBtn.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams uaLp = new LinearLayout.LayoutParams(dp(34), dp(36));
        uaLp.rightMargin = dp(5);
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

        Button scanNowBtn = new Button(this);
        scanNowBtn.setText("🔍");
        scanNowBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        scanNowBtn.setTextColor(Color.parseColor("#1D4ED8"));
        scanNowBtn.setBackground(pillBg("#EFF6FF", 10));
        scanNowBtn.setPadding(0, 0, 0, 0);
        LinearLayout.LayoutParams scanLp = new LinearLayout.LayoutParams(dp(34), dp(36));
        scanLp.rightMargin = dp(5);
        scanNowBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "正在扫描本页视频与图片…", Toast.LENGTH_SHORT).show();
                injectDomMediaScanner(snifferWebView, true);
            }
        });
        bar.addView(scanNowBtn, scanLp);

        snifferCountBadge = new TextView(this);
        snifferCountBadge.setText("0");
        snifferCountBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        snifferCountBadge.setTypeface(Typeface.DEFAULT_BOLD);
        snifferCountBadge.setTextColor(Color.WHITE);
        snifferCountBadge.setGravity(Gravity.CENTER);
        snifferCountBadge.setPadding(dp(9), dp(4), dp(9), dp(4));
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

        snifferWebContainer = new FrameLayout(this);

        snifferWebView = new WebView(this);
        configureBrowserWebSettings(snifferWebView.getSettings(), true);
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(snifferWebView, true);
        snifferWebView.addJavascriptInterface(new JoelSniffHook(), "JoelSniffHook");

        snifferWebView.setWebChromeClient(new WebChromeClient() {
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
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                if (request != null && request.getUrl() != null) {
                    final String reqUrl = request.getUrl().toString();
                    final Map<String, String> hdrs = request.getRequestHeaders();
                    String secFetchDest = getHeaderIgnoreCase(hdrs, "sec-fetch-dest");
                    String accept = getHeaderIgnoreCase(hdrs, "accept");
                    String mediaType = detectSniffTypeFromRequest(reqUrl, secFetchDest, accept);
                    if (mediaType != null) {
                        String headerRef = getHeaderIgnoreCase(hdrs, "referer");
                        String pageUrl = !headerRef.isEmpty()
                                ? headerRef
                                : (currentSniffPageUrl != null ? currentSniffPageUrl : "");
                        String cookies = resolveCookiesForUrl(reqUrl, pageUrl);
                        reportSniffedResource(
                                reqUrl,
                                pageUrl,
                                cookies,
                                "NET",
                                mediaType,
                                "",
                                0,
                                0,
                                -1.0);
                    }
                }
                return super.shouldInterceptRequest(view, request);
            }
        });

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
        popupCloseBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        popupCloseBtn.setTextColor(Color.parseColor("#334155"));
        popupCloseBtn.setBackground(pillBg("#E2E8F0", 8));
        popupCloseBtn.setPadding(dp(10), 0, dp(10), 0);
        popupCloseBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closePopupWindow();
            }
        });
        popupBar.addView(popupCloseBtn, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, dp(30)));

        View popupDivider = new View(this);
        popupDivider.setBackgroundColor(Color.parseColor("#E2E8F0"));

        popupWebSlot = new FrameLayout(this);
        popupOverlay.addView(popupBar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        popupOverlay.addView(popupDivider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        popupOverlay.addView(popupWebSlot, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        snifferWebContainer.addView(snifferWebView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        snifferWebContainer.addView(popupOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        snifferOverlay.addView(bar, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        snifferOverlay.addView(divider, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, dp(1)));
        snifferOverlay.addView(snifferWebContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
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

    private void injectDomMediaScanner(final WebView view, final boolean forceHtmlScan) {
        if (view == null) return;
        final String js = "(function(){"
                + "if(!window.__joelSentUrls){window.__joelSentUrls={};}"
                + "var forceHtml=" + (forceHtmlScan ? "true" : "false") + ";"
                + "function isIgnoredUrl(u){"
                + "return /(avatar|icon|emoji|logo|favicon|qrcode|100x100|c50x50|c60x60|\\.svg)/i.test(u);"
                + "}"
                + "function isTwitterInitChunk(u){"
                + "var l=u.toLowerCase();"
                + "if(l.indexOf('video.twimg.com')>=0&&(l.indexOf('/aud/')>=0||l.indexOf('/0/0/')>=0||l.indexOf('init.mp4')>=0||l.indexOf('.m4s')>=0))return true;"
                + "return false;"
                + "}"
                + "function sendMedia(u,kind,title,w,h,dur){"
                + "if(!u||u.indexOf('http')!==0)return;"
                + "if(isTwitterInitChunk(u))return;"
                + "var key=kind+':'+u;"
                + "if(window.__joelSentUrls[key])return;"
                + "window.__joelSentUrls[key]=1;"
                + "if(window.JoelSniffHook){window.JoelSniffHook.onDomMedia(u,kind,title||'',w||0,h||0,dur>=0?dur:-1);}"
                + "}"
                + "function sendImage(u,title,w,h){"
                + "if(!u||u.indexOf('http')!==0)return;"
                + "if(isIgnoredUrl(u))return;"
                + "var key='img:'+u;"
                + "if(window.__joelSentUrls[key])return;"
                + "window.__joelSentUrls[key]=1;"
                + "if(window.JoelSniffHook){window.JoelSniffHook.onDomImage(u,title||'',w||0,h||0);}"
                + "}"
                + "function scanStructuredNode(root,fallbackTitle){"
                + "if(!root||typeof root!=='object')return;"
                + "var queue=[{obj:root,depth:0,title:fallbackTitle}];"
                + "var visited=0;"
                + "while(queue.length&&visited<2500){"
                + "var cur=queue.shift();var o=cur.obj;var d=cur.depth;var ttl=cur.title;"
                + "if(!o||typeof o!=='object'||d>12)continue;"
                + "visited++;"
                + "if(typeof o.desc==='string'&&o.desc.trim())ttl=o.desc.trim();"
                + "else if(typeof o.item_title==='string'&&o.item_title.trim())ttl=o.item_title.trim();"
                + "else if(typeof o.full_text==='string'&&o.full_text.trim())ttl=o.full_text.trim();"
                + "else if(typeof o.text==='string'&&o.text.trim())ttl=o.text.trim();"
                + "else if(o.legacy&&typeof o.legacy.full_text==='string'&&o.legacy.full_text.trim())ttl=o.legacy.full_text.trim();"
                + "if(o.video_info&&Array.isArray(o.video_info.variants)){"
                + "var mp4Vars=[];var hlsVars=[];"
                + "for(var vi=0;vi<o.video_info.variants.length;vi++){"
                + "var vr=o.video_info.variants[vi];"
                + "if(!vr||typeof vr.url!=='string'||vr.url.indexOf('http')!==0)continue;"
                + "if(vr.content_type==='video/mp4'||vr.url.indexOf('.mp4')>=0)mp4Vars.push(vr);"
                + "else if(vr.url.indexOf('.m3u8')>=0)hlsVars.push(vr);"
                + "}"
                + "mp4Vars.sort(function(a,b){return Number(b.bitrate||0)-Number(a.bitrate||0);});"
                + "for(var mi=0;mi<mp4Vars.length;mi++){sendMedia(mp4Vars[mi].url,'video',ttl,0,0,-1);}"
                + "if(!mp4Vars.length&&hlsVars.length){sendMedia(hlsVars[0].url,'video',ttl,0,0,-1);}"
                + "}"
                + "if(typeof o.media_url_https==='string'&&o.media_url_https.indexOf('https://pbs.twimg.com/media/')===0){"
                + "var xImg=o.media_url_https.indexOf('?')>=0?o.media_url_https:(o.media_url_https+'?format=jpg&name=large');"
                + "sendImage(xImg,ttl,0,0);"
                + "}"
                + "var pa=o.play_addr||(o.video&&o.video.play_addr);"
                + "if(pa&&typeof pa==='object'){"
                + "if(typeof pa.uri==='string'&&pa.uri.length>4&&pa.uri.indexOf('http')!==0){"
                + "sendMedia('https://www.iesdouyin.com/aweme/v1/play/?video_id='+encodeURIComponent(pa.uri)+'&ratio=1080p&line=0','video',ttl,Number(o.width||0),Number(o.height||0),-1);"
                + "}"
                + "if(Array.isArray(pa.url_list)){"
                + "for(var i=0;i<pa.url_list.length;i++){"
                + "var vu=pa.url_list[i];"
                + "if(typeof vu==='string'&&vu.indexOf('http')===0){"
                + "sendMedia(vu.replace('/playwm/','/play/'),'video',ttl,Number(o.width||0),Number(o.height||0),-1);"
                + "break;"
                + "}"
                + "}"
                + "}"
                + "}"
                + "var imgsList=o.images||(o.image_post_info&&o.image_post_info.images);"
                + "if(Array.isArray(imgsList)){"
                + "for(var j=0;j<imgsList.length;j++){"
                + "var im=imgsList[j];if(!im||typeof im!=='object')continue;"
                + "var list=(im.display_image&&im.display_image.url_list)||(im.owner_watermark_image&&im.owner_watermark_image.url_list)||im.url_list;"
                + "if(Array.isArray(list)&&list.length){"
                + "var picked=list[list.length-1]||list[0];"
                + "if(typeof picked==='string'&&picked.indexOf('http')===0){sendImage(picked,ttl,Number(im.width||0),Number(im.height||0));}"
                + "}"
                + "}"
                + "}"
                + "if(Array.isArray(o)){"
                + "for(var a=0;a<o.length&&a<40;a++){if(o[a]&&typeof o[a]==='object')queue.push({obj:o[a],depth:d+1,title:ttl});}"
                + "}else{"
                + "var keys=Object.keys(o);"
                + "for(var k=0;k<keys.length;k++){"
                + "var child=o[keys[k]];"
                + "if(child&&typeof child==='object')queue.push({obj:child,depth:d+1,title:ttl});"
                + "}"
                + "}"
                + "}"
                + "}"
                + "window.__joelScanMedia=function(triggerHtml){"
                + "try{"
                + "var docTitle=(document.title||'').trim();"
                + "var els=document.querySelectorAll('video,audio,video source,audio source');"
                + "for(var i=0;i<els.length;i++){"
                + "var el=els[i];"
                + "var raw=el.currentSrc||el.src||el.getAttribute('src')||'';"
                + "if(!raw||raw.indexOf('http')!==0)continue;"
                + "var tag=el.tagName?el.tagName.toLowerCase():'video';"
                + "var parent=(tag==='source'&&el.parentElement)?el.parentElement:el;"
                + "var isAudio=tag==='audio'||(parent.tagName&&parent.tagName.toLowerCase()==='audio');"
                + "var w=Number(parent.videoWidth||0)||0;var h=Number(parent.videoHeight||0)||0;"
                + "var dur=Number(parent.duration);if(!isFinite(dur)||dur<0)dur=-1;"
                + "sendMedia(raw,isAudio?'audio':'video',docTitle,w,h,dur);"
                + "}"
                + "var imgs=document.querySelectorAll('img');"
                + "for(var m=0;m<imgs.length;m++){"
                + "var im=imgs[m];"
                + "var isrc=im.currentSrc||im.src||im.getAttribute('data-src')||'';"
                + "if(!isrc||isrc.indexOf('http')!==0)continue;"
                + "var nw=Number(im.naturalWidth||0);var nh=Number(im.naturalHeight||0);"
                + "var rw=Number(im.width||0);var rh=Number(im.height||0);"
                + "var isCdn=/(douyinpic\\.com|byteimg\\.com|toutiaoimg\\.com|zjcdn\\.com|douyinstatic\\.com|xhscdn\\.com|pbs\\.twimg\\.com\\/media)/i.test(isrc);"
                + "if((nw>=240&&nh>=240)||(isCdn&&((nw>=120&&nh>=120)||(rw>=120&&rh>=120)))){"
                + "sendImage(isrc,docTitle,nw||rw,nh||rh);"
                + "}"
                + "}"
                + "var bgEls=document.querySelectorAll('[style*=\"url\"]');"
                + "for(var bi=0;bi<bgEls.length&&bi<120;bi++){"
                + "var st=bgEls[bi].getAttribute('style')||'';"
                + "var bm=st.match(/url\\([\"']?(https?:\\/\\/[^\"')]+)[\"']?\\)/i);"
                + "if(bm&&bm[1]&&/(\\.webp|\\.jpg|\\.jpeg|\\.png|douyinpic\\.com|byteimg\\.com|toutiaoimg\\.com|xhscdn\\.com)/i.test(bm[1])){"
                + "sendImage(bm[1],docTitle,0,0);"
                + "}"
                + "}"
                + "if(window._ROUTER_DATA){scanStructuredNode(window._ROUTER_DATA,docTitle);}"
                + "if(window.__INITIAL_SSR_STATE__){scanStructuredNode(window.__INITIAL_SSR_STATE__,docTitle);}"
                + "if(document.documentElement&&window.JoelSniffHook){"
                + "var htmlStr=document.documentElement.innerHTML||'';"
                + "var htmlSig=location.href+':'+htmlStr.length;"
                + "if(triggerHtml||window.__joelLastHtmlSig!==htmlSig){"
                + "window.__joelLastHtmlSig=htmlSig;"
                + "window.JoelSniffHook.onHtmlSource(htmlStr,docTitle);"
                + "}"
                + "}"
                + "}catch(e){}"
                + "};"
                + "if(!window.__joelSniffInstalled){"
                + "window.__joelSniffInstalled=true;"
                + "try{"
                + "var origPlay=HTMLMediaElement.prototype.play;"
                + "HTMLMediaElement.prototype.play=function(){"
                + "setTimeout(function(){window.__joelScanMedia&&window.__joelScanMedia(false);},300);"
                + "return origPlay.apply(this,arguments);"
                + "};"
                + "}catch(_){}"
                + "try{"
                + "if(window.fetch&&!window.fetch.__joelHooked){"
                + "var origFetch=window.fetch;"
                + "window.fetch=function(){"
                + "return origFetch.apply(this,arguments).then(function(resp){"
                + "try{"
                + "var ct=(resp.headers&&resp.headers.get('content-type'))||'';"
                + "if(ct.indexOf('application/json')>=0||ct.indexOf('text/plain')>=0){"
                + "resp.clone().text().then(function(txt){"
                + "if(txt&&txt.length>20&&txt.length<1200000){try{scanStructuredNode(JSON.parse(txt),(document.title||'').trim());}catch(__){}}"
                + "}).catch(function(){});"
                + "}"
                + "}catch(_){}"
                + "return resp;"
                + "});"
                + "};"
                + "window.fetch.__joelHooked=true;"
                + "}"
                + "}catch(_){}"
                + "try{"
                + "var origOpen=XMLHttpRequest.prototype.open;"
                + "XMLHttpRequest.prototype.open=function(method,url){"
                + "this.addEventListener('load',function(){"
                + "try{"
                + "var ct=(this.getResponseHeader('content-type')||'').toLowerCase();"
                + "if((ct.indexOf('application/json')>=0||ct.indexOf('text/plain')>=0)&&typeof this.responseText==='string'&&this.responseText.length>20&&this.responseText.length<1200000){"
                + "var parsed=JSON.parse(this.responseText);"
                + "scanStructuredNode(parsed,(document.title||'').trim());"
                + "}"
                + "}catch(_){}"
                + "});"
                + "return origOpen.apply(this,arguments);"
                + "};"
                + "}catch(_){}"
                + "setInterval(function(){window.__joelScanMedia(false);},1800);"
                + "}"
                + "window.__joelScanMedia(forceHtml);"
                + "if(forceHtml){setTimeout(function(){window.__joelScanMedia(true);},900);}"
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
            snifferWebView.getSettings().setUserAgentString(useMobile ? mobileUa : desktopUa);
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
                || noQuery.endsWith(".ts")) {
            return true;
        }
        if ((lower.contains("video.twimg.com") || lower.contains("twimg.com/"))
                && (lower.contains("/aud/")
                        || lower.contains("/0/0/")
                        || lower.contains("init.mp4")
                        || lower.contains(".m4s"))) {
            return true;
        }
        return lower.contains("mon.zijieapi.com")
                || lower.contains("mcs.snssdk.com")
                || lower.contains("doubleclick.net")
                || lower.contains("google-analytics.com")
                || lower.contains("arkoselabs.com");
    }

    private boolean isImageNoiseUrl(String lower) {
        return lower.contains("avatar")
                || lower.contains("icon")
                || lower.contains("emoji")
                || lower.contains("logo")
                || lower.contains("favicon")
                || lower.contains("qrcode")
                || lower.contains("100x100")
                || lower.contains("c50x50")
                || lower.contains("c60x60")
                || lower.contains(".svg")
                || lower.contains(".ico")
                || lower.contains("gstatic.com")
                || lower.contains("googleapis.com")
                || lower.contains("bytegoofy.com/ies/douyin_pc_web")
                || lower.contains("bytescm.com/obj/static");
    }

    private boolean isImageExtension(String noQuery) {
        return noQuery.endsWith(".webp")
                || noQuery.endsWith(".jpg")
                || noQuery.endsWith(".jpeg")
                || noQuery.endsWith(".png")
                || noQuery.endsWith(".gif");
    }

    private String detectSniffTypeFromRequest(String url, String secFetchDest, String acceptHeader) {
        if (url == null || !url.startsWith("http")) return null;
        String lower = url.toLowerCase(Locale.ROOT);
        if (isIgnoredStaticAsset(lower)) return null;

        String dest = secFetchDest != null ? secFetchDest.toLowerCase(Locale.ROOT) : "";
        String acc = acceptHeader != null ? acceptHeader.toLowerCase(Locale.ROOT) : "";
        int q = lower.indexOf('?');
        String noQuery = q > 0 ? lower.substring(0, q) : lower;

        if ("audio".equals(dest)
                || acc.startsWith("audio/")
                || noQuery.endsWith(".mp3")
                || noQuery.endsWith(".m4a")
                || noQuery.endsWith(".wav")
                || noQuery.endsWith(".flac")
                || noQuery.endsWith(".ogg")
                || noQuery.endsWith(".aac")
                || lower.contains("ies-music")
                || lower.contains("mime=audio")
                || lower.contains("mime_type=audio")
                || lower.contains(".m4a?")
                || lower.contains(".mp3?")) {
            return "audio";
        }

        boolean isKnownImageHost = lower.contains("douyinpic.com")
                || lower.contains("byteimg.com")
                || lower.contains("toutiaoimg.com")
                || lower.contains("douyinstatic.com")
                || lower.contains("xhscdn.com")
                || lower.contains("pbs.twimg.com/media")
                || lower.contains("cdninstagram.com")
                || lower.contains("fbcdn.net");
        if (("image".equals(dest)
                || acc.startsWith("image/")
                || isImageExtension(noQuery)
                || isKnownImageHost)
                && !isImageNoiseUrl(lower)
                && !noQuery.endsWith(".mp4")
                && !lower.contains("mime=video")
                && !lower.contains("mime_type=video")
                && !lower.contains("/video/")) {
            return "image";
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
                || lower.contains("bytegoofy.com")
                || lower.contains("ixigua.com")
                || lower.contains("snssdk.com/aweme/")
                || lower.contains("iesdouyin.com/aweme/")
                || lower.contains("/aweme/v1/play")
                || lower.contains("/video/tos/")
                || lower.contains("bilivideo.com")
                || lower.contains("hdslb.com")
                || lower.contains("video.twimg.com")
                || lower.contains("twimg.com/ext_tw_video")
                || lower.contains("cdninstagram.com")
                || lower.contains("googlevideo.com/videoplayback")
                || lower.contains(".mp4?")
                || lower.contains("mime=video")
                || lower.contains("mime_type=video")) {
            return "video";
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
        if (isImageExtension(noQuery)
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

    private void emitSniffedUpdateToJs(final SniffedResource res, final boolean invalid) {
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

        @JavascriptInterface
        public void onDomImage(String imgUrl, String title, int width, int height) {
            if (imgUrl == null || !imgUrl.startsWith("http")) return;
            String pageUrl = currentSniffPageUrl != null ? currentSniffPageUrl : "";
            String cookies = resolveCookiesForUrl(imgUrl, pageUrl);
            reportSniffedResource(
                    imgUrl,
                    pageUrl,
                    cookies,
                    "DOM",
                    "image",
                    title != null ? title.trim() : "",
                    width,
                    height,
                    -1.0);
        }

        @JavascriptInterface
        public void onHtmlSource(final String html, final String pageTitle) {
            if (html == null || html.isEmpty()) return;
            final String pageUrl = currentSniffPageUrl != null ? currentSniffPageUrl : "";
            probeExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    parseHtmlForMedia(html, pageUrl, pageTitle != null ? pageTitle.trim() : "");
                }
            });
        }
    }

    private void parseHtmlForMedia(String rawHtml, String pageUrl, String pageTitle) {
        if (rawHtml == null || rawHtml.isEmpty()) return;
        String clipped = rawHtml.length() > 3500000 ? rawHtml.substring(0, 3500000) : rawHtml;

        StringBuilder buf = new StringBuilder(clipped.length() + 4096);
        buf.append(clipped);
        Matcher rdm = RENDER_DATA_PATTERN.matcher(clipped);
        while (rdm.find()) {
            try {
                buf.append('\n').append(URLDecoder.decode(rdm.group(1), "UTF-8"));
            } catch (Exception ignored) {
            }
        }

        String normalized = buf.toString()
                .replace("\\u002F", "/")
                .replace("\\u002f", "/")
                .replace("\\/", "/")
                .replace("&amp;", "&");

        Matcher vidMatcher = DOUYIN_VIDEO_ID_PATTERN.matcher(normalized);
        while (vidMatcher.find()) {
            String vid = vidMatcher.group(1);
            if (vid != null && !vid.isEmpty()) {
                try {
                    String playUrl = "https://www.iesdouyin.com/aweme/v1/play/?video_id="
                            + URLEncoder.encode(vid, "UTF-8") + "&ratio=1080p&line=0";
                    reportSniffedResource(
                            playUrl,
                            pageUrl,
                            resolveCookiesForUrl(playUrl, pageUrl),
                            "DOM",
                            "video",
                            pageTitle,
                            0,
                            0,
                            -1.0);
                } catch (Exception ignored) {
                }
            }
        }

        Matcher urlMatcher = URL_EXTRACT_PATTERN.matcher(normalized);
        int captured = 0;
        while (urlMatcher.find() && captured < 120) {
            String rawUrl = urlMatcher.group().replaceAll("[),.;!?'\"\\]\\\\]+$", "");
            String lower = rawUrl.toLowerCase(Locale.ROOT);
            if (isIgnoredStaticAsset(lower)) continue;

            int q = lower.indexOf('?');
            String noQuery = q > 0 ? lower.substring(0, q) : lower;

            if (lower.startsWith("https://video.twimg.com/") && noQuery.endsWith(".mp4")) {
                captured++;
                reportSniffedResource(
                        rawUrl,
                        pageUrl,
                        resolveCookiesForUrl(rawUrl, pageUrl),
                        "DOM",
                        "video",
                        pageTitle,
                        0,
                        0,
                        -1.0);
                continue;
            }

            if (lower.contains("douyinvod.com")
                    || lower.contains("bytevcloudcdn.com")
                    || lower.contains("/aweme/v1/play")
                    || (lower.contains("zjcdn.com") && !isImageExtension(noQuery))
                    || noQuery.endsWith(".mp4")
                    || noQuery.endsWith(".m3u8")) {
                String cleanVideo = rawUrl.contains("/playwm/")
                        ? rawUrl.replace("/playwm/", "/play/")
                        : rawUrl;
                captured++;
                reportSniffedResource(
                        cleanVideo,
                        pageUrl,
                        resolveCookiesForUrl(cleanVideo, pageUrl),
                        "DOM",
                        "video",
                        pageTitle,
                        0,
                        0,
                        -1.0);
                continue;
            }

            if (noQuery.endsWith(".mp3") || noQuery.endsWith(".m4a")) {
                captured++;
                reportSniffedResource(
                        rawUrl,
                        pageUrl,
                        resolveCookiesForUrl(rawUrl, pageUrl),
                        "DOM",
                        "audio",
                        pageTitle,
                        0,
                        0,
                        -1.0);
                continue;
            }

            boolean isContentImgHost = lower.contains("douyinpic.com")
                    || lower.contains("byteimg.com")
                    || lower.contains("toutiaoimg.com")
                    || lower.contains("xhscdn.com")
                    || lower.contains("pbs.twimg.com/media")
                    || lower.contains("~tplv-dy-");
            if (isContentImgHost
                    && (isImageExtension(noQuery) || lower.contains("pbs.twimg.com/media") || lower.contains("douyinpic.com"))
                    && !isImageNoiseUrl(lower)) {
                captured++;
                reportSniffedResource(
                        rawUrl,
                        pageUrl,
                        resolveCookiesForUrl(rawUrl, pageUrl),
                        "DOM",
                        "image",
                        pageTitle,
                        0,
                        0,
                        -1.0);
            }
        }
    }

    private void openSnifferOverlay(final String rawUrl) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                closePopupWindow();
                synchronized (sniffLock) {
                    seenSniffUrls.clear();
                    sniffedByUrl.clear();
                }
                snifferCountBadge.setText("0");
                snifferOverlay.setVisibility(View.VISIBLE);
                applySnifferUa(true);
                String clean = extractCleanUrlFromText(rawUrl);
                String target = !clean.isEmpty() ? clean : (rawUrl != null ? rawUrl.trim() : "");
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
        currentSniffPageUrl = u;
        snifferUrlInput.setText(u);
        snifferWebView.loadUrl(u);
    }

    private void closeSnifferOverlay() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                closePopupWindow();
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
