// Joel Phone Pro v1.5.0 — Single-Video Complete Quality Picker & Native Sniffer Bridge
const $ = (id) => document.getElementById(id);

const I18N = {
  zh: {
    brandSub: "视频嗅探 · 多线程极速下载",
    searchPlaceholder: "搜索或输入网址",
    searchGo: "前往",
    copiedCardTitle: "🔗 您复制的网址",
    copiedOpenBrowserBtn: "🌐 打开网页嗅探",
    copiedDirectDownloadBtn: "⬇ 直链下载",
    sitesSectionTitle: "热门视频网站",
    sitesSectionHint: "打开即自动嗅探",
    guideCardTitle: "只需两步，即可下载视频",
    guideStep1Title: "进入网站并播放要下载的视频",
    guideStep1Desc: "点击上方网站图标或粘贴分享口令，打开目标视频/图集页面。",
    guideStep2Title: "点击右下角亮起的下载按钮，再选画质",
    guideStep2Desc: "网页右下角只悬浮唯一一个下载标志，检测到当前完整视频（含声音）后点开即可选清晰度并多线程下载。",
    homePasteOpenBtn: "📋 粘贴链接并打开",
    homeResumeBrowserBtn: "🌐 返回网页",
    navBrowserLabel: "窗口",
    navProgressLabel: "下载中",
    navFinishedLabel: "已下载",
    progressStabilityText: "多线程分片加速 · 提升下载稳定性",
    progressBrowserText: "🌐 网页仍在窗口保持打开",
    progressBackToBrowserBtn: "返回网页继续浏览",
    pauseAllBtn: "⏸ 全部暂停",
    resumeAllBtn: "▶ 全部开始",
    clearFinishedBtn: "🧹 清理记录",
    finFilterAllBtn: "全部",
    finFilterVideoBtn: "🎬 视频",
    finFilterImageBtn: "🖼️ 图片",
    finFilterAudioBtn: "🎵 音频",
    drawerTitle: "下载当前视频",
    drawerCompleteBadge: "🎬 完整视频（含声音）",
    drawerPreviewOpen: "👁️ 预浏览",
    drawerPreviewClose: "🔼 收起",
    drawerQualityLabel: "选择清晰度",
    drawerThreadTitle: "⚡ 多线程分片加速",
    drawerThreadBadge: (n) => `已开启 ${n} 线程并发`,
    drawerThreadBtnLabel: (n) => `${n} 线程`,
    drawerCloseBtn: "✕ 关闭",
    drawerRenameBtn: "✎ 重命名",
    drawerRenameDoneBtn: "确定",
    drawerRenamePlaceholder: "输入保存文件名…",
    drawerDownloadBtn: (q, th = 16) => (q ? `⚡ ${th}线程高速下载 (${q})` : `⚡ ${th}线程高速下载`),
    emptyDrawer: "暂未检测到当前完整视频，请在网页中先播放要下载的视频。",
    previewLoading: "正在加载完整视频预览…",
    previewLoadError: "无法在线预浏览此流（可能需鉴权或加密），仍可尝试下载。",
    qualityFallbackSub: "完整 MP4（含声音）",
    directModalTitle: "新建直链 / 口令下载",
    directPlaceholder: "粘贴链接或带文字的分享口令，自动清理杂字…",
    directCleanBadge: "已识别纯净链接",
    directFileNameLabel: "保存文件名",
    directHeadersShow: "▾ 收起高级请求头",
    directHeadersHide: "▸ 高级请求头 (Referer / Cookie 选填)",
    directThreadsLabel: "并发线程数",
    directThreadsHint: "HTTP Range 多分片提速",
    directPasteBtn: "📋 粘贴",
    directSniffBtn: "🌐 网页嗅探",
    directStartBtn: "⬇ 下载",
    settingsModalTitle: "设置",
    setLangLabel: "界面语言 / Language",
    setDefaultThreadsLabel: "默认并发线程数",
    setConcurrentLabel: "同时运行任务上限",
    setUaLabel: "默认下载请求 UA",
    uaDesktopBtn: "桌面 Chrome",
    uaMobileBtn: "手机 Android",
    setPathLabel: "下载目录",
    setPathNote: "遵循 Android 公共下载目录规范，系统相册和文件管理器直接可见。",
    clearBrowserCacheBtn: "🧹 清除浏览器缓存与登录 Cookie",
    emptyProgress: "暂无正在下载的任务。去「窗口」打开网站，播放视频后点击右下角下载标志即可。",
    emptyFinished: "暂无已下载完成的文件。",
    emptyFinishedFiltered: "当前分类下暂无已完成文件。",
    goBrowserBtn: "去发现视频",
    statusQueued: "排队中",
    statusProbing: "探测分片中...",
    statusMulti: (n) => `${n} 线程并发`,
    statusSingle: "单流下载",
    statusPaused: "下载暂停",
    statusCompleted: "已完成",
    statusError: "下载失败",
    chunkLabel: (n, ok) => (ok ? `多线程分片并发 (${n} 线程)` : "单流接收"),
    btnPause: "暂停",
    btnResume: "继续",
    btnRetry: "重试",
    btnCancel: "取消",
    btnOpen: "▶ 打开",
    btnShare: "📤 分享",
    btnCopyLink: "🔗 复制链接",
    btnDelete: "删除",
    etaPrefix: "剩余",
    unknownSize: "大小未知",
    toastClipboardEmpty: "剪贴板为空，未检测到链接",
    toastNoUrlInText: "未识别到链接，将以关键词搜索打开",
    toastTaskStarted: (name, n) => `已添加 ${n} 线程下载：${name}`,
    toastCopied: "链接已复制到剪贴板",
    toastNeedUrl: "请先输入网址或关键词",
    toastSharedLoaded: "已接收系统分享链接",
    toastSettingsSaved: "设置已保存",
    toastPausedAll: "已暂停全部进行中任务",
    toastResumedAll: "已恢复暂停的任务",
    toastBrowserPreview: "当前为网页预览模式，安装 APK 后可启用原生浏览与下载",
  },
  en: {
    brandSub: "Video Sniffer · Multi-Thread Downloader",
    searchPlaceholder: "Search or Type URL",
    searchGo: "Go",
    copiedCardTitle: "🔗 Link you copied",
    copiedOpenBrowserBtn: "🌐 Open & Sniff",
    copiedDirectDownloadBtn: "⬇ Direct Download",
    sitesSectionTitle: "Popular Video Websites",
    sitesSectionHint: "Auto-detect active video",
    guideCardTitle: "Two steps to download videos",
    guideStep1Title: "Go to website and play the video",
    guideStep1Desc: "Tap any site above or paste a shared link to open the video post.",
    guideStep2Title: "Tap the glowing download icon, then choose quality",
    guideStep2Desc: "Only one download button floats on the active video; tap it to pick resolution and download with video + audio together.",
    homePasteOpenBtn: "📋 Paste Link & Open",
    homeResumeBrowserBtn: "🌐 Back to Webpage",
    navBrowserLabel: "Tab",
    navProgressLabel: "Progress",
    navFinishedLabel: "Finished",
    progressStabilityText: "Multi-thread chunks · Improve download stability",
    progressBrowserText: "🌐 Webpage stays open in Tab",
    progressBackToBrowserBtn: "Back to Webpage",
    pauseAllBtn: "⏸ Pause All",
    resumeAllBtn: "▶ Resume All",
    clearFinishedBtn: "🧹 Clear List",
    finFilterAllBtn: "All",
    finFilterVideoBtn: "🎬 Video",
    finFilterImageBtn: "🖼️ Image",
    finFilterAudioBtn: "🎵 Audio",
    drawerTitle: "Download Video",
    drawerCompleteBadge: "🎬 Full Video + Audio",
    drawerPreviewOpen: "👁️ Preview",
    drawerPreviewClose: "🔼 Hide",
    drawerQualityLabel: "Choose Quality",
    drawerThreadTitle: "⚡ Multi-Thread Acceleration",
    drawerThreadBadge: (n) => `${n} Threads Active`,
    drawerThreadBtnLabel: (n) => `${n} Threads`,
    drawerCloseBtn: "✕ Close",
    drawerRenameBtn: "✎ Rename",
    drawerRenameDoneBtn: "Done",
    drawerRenamePlaceholder: "Custom filename…",
    drawerDownloadBtn: (q, th = 16) => (q ? `⚡ ${th}-Thread Download (${q})` : `⚡ ${th}-Thread Download`),
    emptyDrawer: "No active complete video detected yet. Play the video on the webpage first.",
    previewLoading: "Loading video stream preview…",
    previewLoadError: "Cannot stream inline preview, you can still try downloading.",
    qualityFallbackSub: "Full MP4 (Video + Audio)",
    directModalTitle: "New Direct URL Download",
    directPlaceholder: "Paste URL or share text — extra words are stripped automatically…",
    directCleanBadge: "Clean URL Ready",
    directFileNameLabel: "Filename",
    directHeadersShow: "▾ Hide Request Headers",
    directHeadersHide: "▸ Request Headers (Referer / Cookie)",
    directThreadsLabel: "Concurrent Threads",
    directThreadsHint: "Parallel HTTP Range chunks",
    directPasteBtn: "📋 Paste",
    directSniffBtn: "🌐 Sniff Page",
    directStartBtn: "⬇ Download",
    settingsModalTitle: "Settings",
    setLangLabel: "Language / 界面语言",
    setDefaultThreadsLabel: "Default Threads",
    setConcurrentLabel: "Max Concurrent Downloads",
    setUaLabel: "Default Download UA",
    uaDesktopBtn: "Desktop Chrome",
    uaMobileBtn: "Mobile Android",
    setPathLabel: "Save Folder",
    setPathNote: "Uses Android public Downloads folder — visible in Gallery & Files.",
    clearBrowserCacheBtn: "🧹 Clear Browser Cache & Cookies",
    emptyProgress: "No downloads in progress. Open a site in Tab and play any video.",
    emptyFinished: "No finished files yet.",
    emptyFinishedFiltered: "No finished files in this category.",
    goBrowserBtn: "Discover Videos",
    statusQueued: "Queued",
    statusProbing: "Probing...",
    statusMulti: (n) => `${n} Threads`,
    statusSingle: "Single Stream",
    statusPaused: "Paused",
    statusCompleted: "Completed",
    statusError: "Failed",
    chunkLabel: (n, ok) => (ok ? `Parallel Chunks (${n} threads)` : "Single stream"),
    btnPause: "Pause",
    btnResume: "Resume",
    btnRetry: "Retry",
    btnCancel: "Cancel",
    btnOpen: "▶ Open",
    btnShare: "📤 Share",
    btnCopyLink: "🔗 Copy URL",
    btnDelete: "Delete",
    etaPrefix: "ETA",
    unknownSize: "Unknown size",
    toastClipboardEmpty: "Clipboard is empty",
    toastNoUrlInText: "Opening web search for keywords",
    toastTaskStarted: (name, n) => `Started ${n}-thread download: ${name}`,
    toastCopied: "Link copied to clipboard",
    toastNeedUrl: "Enter a URL or search query first",
    toastSharedLoaded: "Loaded shared link",
    toastSettingsSaved: "Settings saved",
    toastPausedAll: "Paused active downloads",
    toastResumedAll: "Resumed paused downloads",
    toastBrowserPreview: "Web preview mode — install Android APK for native sniffer & downloader",
  },
};

const appState = {
  lang: "zh",
  activeTab: "browser",
  finishedFilter: "all",
  activeVideo: null, // { videoKey, title, coverUrl, durationSec, variants: [] }
  selectedVariantId: "",
  selectedVariantUrl: "",
  customFileName: "",
  renameExpanded: false,
  previewExpanded: false,
  selectedThreads: 16,
  directHeadersExpanded: false,
  directCleanUrl: "",
  copiedDetectedUrl: "",
  dismissedCopiedUrl: "",
  browserHasPage: false,
  browserShowingInTab1: false,
  activeBrowserUrl: "",
  drawerOpen: false,
  tasks: [],
  settings: {
    defaultThreads: 16,
    concurrentLimit: 2,
    defaultUaMode: "desktop",
    lang: "zh",
  },
};

function t() {
  return I18N[appState.lang] || I18N.zh;
}

function showToast(msg) {
  const el = $("toast");
  if (!el) return;
  el.textContent = msg;
  el.classList.remove("hidden");
  clearTimeout(showToast._timer);
  showToast._timer = setTimeout(() => el.classList.add("hidden"), 2500);
}

function formatBytes(bytes) {
  if (!bytes || bytes <= 0) return "0 B";
  const units = ["B", "KB", "MB", "GB"];
  let idx = 0;
  let val = bytes;
  while (val >= 1024 && idx < units.length - 1) {
    val /= 1024;
    idx++;
  }
  return `${val.toFixed(idx === 0 ? 0 : idx === 1 ? 0 : 1)} ${units[idx]}`;
}

function formatSpeed(bps) {
  if (!bps || bps <= 0) return "0 B/s";
  return `${formatBytes(bps)}/s`;
}

function formatEta(sec) {
  if (sec == null || sec < 0 || !isFinite(sec)) return "";
  const s = Math.floor(sec);
  const h = Math.floor(s / 3600);
  const m = Math.floor((s % 3600) / 60);
  const r = s % 60;
  if (h > 0) return `${h}h ${m}m`;
  if (m > 0) return `${m}m ${r}s`;
  return `${r}s`;
}

function escapeHtml(str) {
  return String(str ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function extractCleanUrl(rawText) {
  if (!rawText) return { cleanUrl: "", hadNoise: false, fileName: "" };
  const match = String(rawText).match(/https?:\/\/[A-Za-z0-9\-._~:/?#[\]@!$&'()*+,;=%]+/i);
  if (!match) return { cleanUrl: "", hadNoise: false, fileName: "" };
  const cleanUrl = match[0].replace(/[),.;!?'"\]]+$/g, "");
  const hadNoise = String(rawText).trim() !== cleanUrl;

  let fileName = "video_download.mp4";
  try {
    const parsed = new URL(cleanUrl);
    const segs = parsed.pathname.split("/").filter(Boolean);
    const last = segs[segs.length - 1];
    if (last && last.includes(".")) {
      fileName = decodeURIComponent(last);
    } else if (last) {
      fileName = `${decodeURIComponent(last)}.mp4`;
    } else {
      fileName = `${parsed.hostname.replace(/^www\./, "")}.mp4`;
    }
  } catch {
    fileName = "video_download.mp4";
  }
  return { cleanUrl, hadNoise, fileName };
}

function resolveBrowserTarget(rawText) {
  const extracted = extractCleanUrl(rawText);
  if (extracted.cleanUrl) {
    return { target: extracted.cleanUrl, isUrl: true, hadNoise: extracted.hadNoise };
  }
  const trimmed = String(rawText || "").trim();
  if (/^[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(\/.*)?$/.test(trimmed)) {
    return { target: `https://${trimmed}`, isUrl: true, hadNoise: false };
  }
  return { target: trimmed, isUrl: false, hadNoise: false };
}

function inferExtBadge(fileName) {
  const parts = String(fileName || "").split(".");
  if (parts.length > 1) {
    return parts[parts.length - 1].slice(0, 5).toUpperCase();
  }
  return "MP4";
}

function inferTaskCategory(task) {
  const mt = String(task.mimeType || "").toLowerCase();
  const fn = String(task.fileName || "").toLowerCase();
  if (mt.startsWith("image/") || /\.(jpg|jpeg|png|webp|gif)$/.test(fn)) return "image";
  if (mt.startsWith("audio/") || /\.(mp3|m4a|wav|flac|aac|ogg)$/.test(fn)) return "audio";
  if (mt.startsWith("video/") || /\.(mp4|mkv|mov|webm|flv|m3u8)$/.test(fn)) return "video";
  return "other";
}

function syncText(id, text) {
  const el = $(id);
  if (el) el.textContent = text;
}

function applyI18n() {
  const dict = t();
  document.documentElement.lang = appState.lang === "en" ? "en" : "zh-CN";

  syncText("brandSub", dict.brandSub);
  if ($("topSearchInput")) $("topSearchInput").placeholder = dict.searchPlaceholder;
  syncText("topSearchGoBtn", dict.searchGo);
  syncText("copiedCardTitle", dict.copiedCardTitle);
  syncText("copiedOpenBrowserBtn", dict.copiedOpenBrowserBtn);
  syncText("copiedDirectDownloadBtn", dict.copiedDirectDownloadBtn);
  syncText("sitesSectionTitle", dict.sitesSectionTitle);
  syncText("sitesSectionHint", dict.sitesSectionHint);
  syncText("guideCardTitle", dict.guideCardTitle);
  syncText("guideStep1Title", dict.guideStep1Title);
  syncText("guideStep1Desc", dict.guideStep1Desc);
  syncText("guideStep2Title", dict.guideStep2Title);
  syncText("guideStep2Desc", dict.guideStep2Desc);
  syncText("homePasteOpenBtn", dict.homePasteOpenBtn);
  syncText("homeResumeBrowserBtn", dict.homeResumeBrowserBtn);

  syncText("navBrowserLabel", dict.navBrowserLabel);
  syncText("navProgressLabel", dict.navProgressLabel);
  syncText("navFinishedLabel", dict.navFinishedLabel);

  syncText("progressStabilityText", dict.progressStabilityText);
  syncText("progressBrowserText", dict.progressBrowserText);
  syncText("progressBackToBrowserBtn", dict.progressBackToBrowserBtn);
  syncText("pauseAllBtn", dict.pauseAllBtn);
  syncText("resumeAllBtn", dict.resumeAllBtn);
  syncText("clearFinishedBtn", dict.clearFinishedBtn);
  syncText("finFilterAllBtn", dict.finFilterAllBtn);
  syncText("finFilterVideoBtn", dict.finFilterVideoBtn);
  syncText("finFilterImageBtn", dict.finFilterImageBtn);
  syncText("finFilterAudioBtn", dict.finFilterAudioBtn);

  syncText("drawerTitle", dict.drawerTitle);
  syncText("drawerCompleteBadge", dict.drawerCompleteBadge);
  syncText("drawerQualityLabel", dict.drawerQualityLabel);
  syncText("drawerThreadTitle", dict.drawerThreadTitle);
  document.querySelectorAll("#drawerThreadSelector .drawer-thread-btn").forEach((btn) => {
    btn.textContent = dict.drawerThreadBtnLabel(Number(btn.dataset.threads));
  });
  syncText("drawerCloseBtn", dict.drawerCloseBtn);
  syncText("drawerRenameBtn", dict.drawerRenameBtn);
  syncText("drawerRenameDoneBtn", dict.drawerRenameDoneBtn);
  if ($("drawerRenameInput")) $("drawerRenameInput").placeholder = dict.drawerRenamePlaceholder;

  syncText("directModalTitle", dict.directModalTitle);
  if ($("directRawInput")) $("directRawInput").placeholder = dict.directPlaceholder;
  syncText("directCleanBadge", dict.directCleanBadge);
  syncText("directFileNameLabel", dict.directFileNameLabel);
  syncText(
    "directHeadersToggle",
    appState.directHeadersExpanded ? dict.directHeadersShow : dict.directHeadersHide
  );
  syncText("directThreadsLabel", dict.directThreadsLabel);
  syncText("directThreadsHint", dict.directThreadsHint);
  syncText("directPasteBtn", dict.directPasteBtn);
  syncText("directSniffBtn", dict.directSniffBtn);
  syncText("directStartBtn", dict.directStartBtn);

  syncText("settingsModalTitle", dict.settingsModalTitle);
  syncText("setLangLabel", dict.setLangLabel);
  syncText("setDefaultThreadsLabel", dict.setDefaultThreadsLabel);
  syncText("setConcurrentLabel", dict.setConcurrentLabel);
  syncText("setUaLabel", dict.setUaLabel);
  syncText("uaDesktopBtn", dict.uaDesktopBtn);
  syncText("uaMobileBtn", dict.uaMobileBtn);
  syncText("setPathLabel", dict.setPathLabel);
  syncText("setPathNote", dict.setPathNote);
  syncText("clearBrowserCacheBtn", dict.clearBrowserCacheBtn);

  document.querySelectorAll("#settingsLangSwitch .seg-btn").forEach((b) => {
    b.classList.toggle("active", b.dataset.lang === appState.lang);
  });

  renderTasks();
  renderDownloadQualitySheet();
  renderSettings();
  renderHomeStatus();
}

function renderHomeStatus() {
  const resumeBtn = $("homeResumeBrowserBtn");
  if (resumeBtn) {
    resumeBtn.classList.toggle("hidden", !appState.browserHasPage);
  }
  const progressReturnBar = $("progressReturnBrowserBar");
  if (progressReturnBar) {
    progressReturnBar.classList.toggle("hidden", !appState.browserHasPage);
  }
}

window.switchTab = function switchTab(tab) {
  if (appState.drawerOpen && tab !== "browser") {
    window.closeDownloadDrawer(true);
  }
  appState.activeTab = tab;
  document.querySelectorAll(".tab-panel").forEach((p) => {
    p.classList.toggle("active", p.id === `panel-${tab}`);
  });
  document.querySelectorAll(".nav-btn").forEach((b) => {
    b.classList.toggle("active", b.dataset.tab === tab);
  });
  if (window.JoelNative && window.JoelNative.onBottomTabChanged) {
    window.JoelNative.onBottomTabChanged(tab);
  }
};

window.handleAndroidBack = function handleAndroidBack() {
  if (appState.drawerOpen) {
    window.closeDownloadDrawer(false);
    return true;
  }
  if ($("directModal") && !$("directModal").classList.contains("hidden")) {
    closeDirectModal();
    return true;
  }
  if ($("settingsModal") && !$("settingsModal").classList.contains("hidden")) {
    closeSettingsModal();
    return true;
  }
  if (appState.activeTab !== "browser") {
    window.switchTab("browser");
    return true;
  }
  return false;
};

function openUrlInBrowserTab(rawOrUrl) {
  const res = resolveBrowserTarget(rawOrUrl);
  if (!res.target) {
    showToast(t().toastNeedUrl);
    return;
  }
  if (!res.isUrl) {
    showToast(t().toastNoUrlInText);
  }
  appState.browserHasPage = true;
  appState.browserShowingInTab1 = true;
  appState.activeBrowserUrl = res.target;
  renderHomeStatus();
  if (window.JoelNative) {
    window.JoelNative.openBrowserSniffer(res.target);
  } else {
    showToast(t().toastBrowserPreview);
  }
}

window.onBrowserPageChanged = function onBrowserPageChanged(url) {
  appState.activeBrowserUrl = url || "";
  if (url) appState.browserHasPage = true;
  renderHomeStatus();
};

window.onBrowserVisibilityChanged = function onBrowserVisibilityChanged(visible, url) {
  appState.browserShowingInTab1 = Boolean(visible);
  if (url) {
    appState.activeBrowserUrl = url;
    appState.browserHasPage = true;
  }
  renderHomeStatus();
};

/* --- Single-Video Quality Picker Drawer (download_manual_drawer.xml) --- */
window.openDownloadDrawer = function openDownloadDrawer() {
  appState.drawerOpen = true;
  appState.renameExpanded = false;
  appState.previewExpanded = false;
  document.body.classList.add("drawer-open");
  document.body.classList.toggle("over-browser", Boolean(appState.browserShowingInTab1));
  const modal = $("downloadDrawerModal");
  if (modal) modal.classList.remove("hidden");
  syncDrawerThreadPills();
  renderDownloadQualitySheet();
  if (window.JoelNative && window.JoelNative.onDownloadDrawerOpened) {
    window.JoelNative.onDownloadDrawerOpened();
  }
};

window.closeDownloadDrawer = function closeDownloadDrawer(skipNativeRestore) {
  appState.drawerOpen = false;
  appState.renameExpanded = false;
  stopInlinePreview();
  document.body.classList.remove("drawer-open", "over-browser");
  const modal = $("downloadDrawerModal");
  if (modal) modal.classList.add("hidden");
  if (!skipNativeRestore && window.JoelNative && window.JoelNative.onDownloadDrawerClosed) {
    window.JoelNative.onDownloadDrawerClosed();
  }
};

function stopInlinePreview() {
  appState.previewExpanded = false;
  const pvWrap = $("drawerInlinePreview");
  const pvVideo = $("drawerPreviewVideo");
  if (pvVideo) {
    try {
      pvVideo.pause();
      pvVideo.removeAttribute("src");
      pvVideo.load();
    } catch {}
  }
  if (pvWrap) pvWrap.classList.add("hidden");
}

function syncDrawerThreadPills() {
  const dict = t();
  document.querySelectorAll("#drawerThreadSelector .drawer-thread-btn").forEach((b) => {
    b.classList.toggle("active", Number(b.dataset.threads) === Number(appState.selectedThreads));
  });
  document.querySelectorAll("#directThreadSelector .thread-pill").forEach((b) => {
    b.classList.toggle("active", Number(b.dataset.threads) === Number(appState.selectedThreads));
  });
  syncText("drawerThreadBadge", dict.drawerThreadBadge(appState.selectedThreads));
  const dlBtn = $("drawerDownloadBtn");
  if (dlBtn) {
    const sel = getSelectedVariant();
    dlBtn.textContent = dict.drawerDownloadBtn(sel ? sel.qualityLabel : "", appState.selectedThreads);
  }
}

function deriveVideoBaseFileName() {
  if (appState.customFileName && appState.customFileName.trim()) {
    const raw = appState.customFileName.trim().replace(/[\\/:*?"<>|\r\n]+/g, "_");
    return /\.[a-z0-9]{2,5}$/i.test(raw) ? raw : `${raw}.mp4`;
  }
  const v = appState.activeVideo;
  if (v && v.title && v.title.trim()) {
    const clean = v.title
      .trim()
      .replace(/[\\/:*?"<>|#\r\n]+/g, " ")
      .replace(/\s+/g, " ")
      .trim()
      .slice(0, 50);
    if (clean) return `${clean}.mp4`;
  }
  const sel = getSelectedVariant();
  if (sel && sel.suggestedName) {
    return sel.suggestedName.replace(/[\\/:*?"<>|\r\n]+/g, "_");
  }
  return "video_download.mp4";
}

function getSelectedVariant() {
  const v = appState.activeVideo;
  if (!v || !Array.isArray(v.variants) || !v.variants.length) return null;
  if (appState.selectedVariantId) {
    const byId = v.variants.find((item) => item.id === appState.selectedVariantId);
    if (byId) return byId;
  }
  if (appState.selectedVariantUrl) {
    const byUrl = v.variants.find((item) => item.url === appState.selectedVariantUrl);
    if (byUrl) return byUrl;
  }
  return v.variants[0];
}

function renderDownloadQualitySheet() {
  const dict = t();
  const v = appState.activeVideo;
  const grid = $("drawerQualityGrid");
  const dlBtn = $("drawerDownloadBtn");
  const renameBar = $("drawerRenameBar");

  if (renameBar) {
    renameBar.classList.toggle("hidden", !appState.renameExpanded);
  }
  syncText("drawerPreviewTag", appState.previewExpanded ? dict.drawerPreviewClose : dict.drawerPreviewOpen);
  syncText("drawerThreadBadge", dict.drawerThreadBadge(appState.selectedThreads));

  if (!v || !Array.isArray(v.variants) || !v.variants.length) {
    syncText("drawerVideoTitle", dict.emptyDrawer);
    syncText("drawerSelectedMeta", "");
    if ($("drawerCoverImg")) $("drawerCoverImg").classList.add("hidden");
    if ($("drawerThumbFallback")) $("drawerThumbFallback").classList.remove("hidden");
    if ($("drawerDurBadge")) $("drawerDurBadge").classList.add("hidden");
    if (grid) grid.innerHTML = `<div class="empty-card" style="grid-column:span 2;padding:20px 10px;">${escapeHtml(dict.emptyDrawer)}</div>`;
    if (dlBtn) {
      dlBtn.textContent = dict.drawerDownloadBtn("", appState.selectedThreads);
      dlBtn.disabled = true;
    }
    return;
  }

  const selected = getSelectedVariant();
  if (selected) {
    appState.selectedVariantId = selected.id || "";
    appState.selectedVariantUrl = selected.url || "";
  }

  const displayName = deriveVideoBaseFileName();
  syncText("drawerVideoTitle", displayName);

  const covImg = $("drawerCoverImg");
  const covFallback = $("drawerThumbFallback");
  if (v.coverUrl && v.coverUrl.startsWith("http") && covImg && covFallback) {
    if (covImg.src !== v.coverUrl) covImg.src = v.coverUrl;
    covImg.classList.remove("hidden");
    covFallback.classList.add("hidden");
  } else if (covImg && covFallback) {
    covImg.classList.add("hidden");
    covFallback.classList.remove("hidden");
  }

  const durBadge = $("drawerDurBadge");
  if (durBadge) {
    if (v.durationSec > 0) {
      durBadge.textContent = formatEta(v.durationSec);
      durBadge.classList.remove("hidden");
    } else {
      durBadge.classList.add("hidden");
    }
  }

  const metaParts = [];
  if (selected) {
    if (selected.qualityLabel) metaParts.push(selected.qualityLabel);
    if (selected.width > 0 && selected.height > 0) metaParts.push(`${selected.width}×${selected.height}`);
    metaParts.push(selected.sizeBytes > 0 ? formatBytes(selected.sizeBytes) : dict.qualityFallbackSub);
  }
  syncText("drawerSelectedMeta", metaParts.join(" · "));

  if (dlBtn) {
    dlBtn.textContent = dict.drawerDownloadBtn(selected ? selected.qualityLabel : "", appState.selectedThreads);
    dlBtn.disabled = !selected;
  }

  if (!grid) return;
  grid.innerHTML = "";
  for (const variant of v.variants) {
    const isSel = selected && (selected.id ? variant.id === selected.id : variant.url === selected.url);
    const subParts = [`⚡ ${appState.selectedThreads}线程`];
    if (variant.sizeBytes > 0) {
      subParts.push(formatBytes(variant.sizeBytes));
    } else {
      subParts.push(dict.qualityFallbackSub);
    }
    if (variant.width > 0 && variant.height > 0) {
      subParts.push(`${variant.width}×${variant.height}`);
    }

    const card = document.createElement("button");
    card.type = "button";
    card.className = `quality-card${isSel ? " selected" : ""}`;
    card.innerHTML = `
      ${isSel ? '<span class="quality-check">✓</span>' : ""}
      <span class="quality-res">${escapeHtml(variant.qualityLabel || "高清")}</span>
      <span class="quality-sub">${escapeHtml(subParts.join(" · "))}</span>
    `;
    card.addEventListener("click", () => {
      appState.selectedVariantId = variant.id || "";
      appState.selectedVariantUrl = variant.url || "";
      renderDownloadQualitySheet();
      if (appState.previewExpanded) {
        loadInlinePreviewStream(variant);
      }
    });
    grid.appendChild(card);
  }
}

function loadInlinePreviewStream(variant) {
  const pvWrap = $("drawerInlinePreview");
  const pvVideo = $("drawerPreviewVideo");
  const pvMeta = $("drawerPreviewMeta");
  const dict = t();
  if (!pvWrap || !pvVideo || !variant) return;
  pvWrap.classList.remove("hidden");
  pvVideo.src = variant.previewUrl || variant.url;
  if (pvMeta) {
    pvMeta.textContent = `${variant.qualityLabel || ""} ${variant.sizeBytes > 0 ? "· " + formatBytes(variant.sizeBytes) : ""} — ${dict.previewLoading}`;
  }
  pvVideo.onloadedmetadata = () => {
    const dims = pvVideo.videoWidth > 0 ? `${pvVideo.videoWidth}×${pvVideo.videoHeight}` : "";
    const dur = isFinite(pvVideo.duration) && pvVideo.duration > 0 ? formatEta(pvVideo.duration) : "";
    if (pvMeta) {
      pvMeta.textContent = [variant.qualityLabel, dims, dur, variant.sizeBytes > 0 ? formatBytes(variant.sizeBytes) : ""]
        .filter(Boolean)
        .join(" · ");
    }
  };
  pvVideo.onerror = () => {
    if (pvMeta) pvMeta.textContent = dict.previewLoadError;
  };
}

/* --- Progress & Finished Tabs --- */
function renderTasks() {
  const dict = t();
  const tasks = appState.tasks || [];

  let activeCount = 0;
  let runningSpeed = 0;
  const progressTasks = [];
  const finishedTasks = [];

  for (const task of tasks) {
    if (task.status === "completed") {
      finishedTasks.push(task);
    } else {
      progressTasks.push(task);
      if (task.status === "downloading" || task.status === "probing" || task.status === "queued") {
        activeCount++;
        runningSpeed += Number(task.speedBps || 0);
      }
    }
  }

  const speedPill = $("globalSpeedPill");
  if (speedPill) speedPill.classList.toggle("active", activeCount > 0);
  syncText("globalSpeedText", formatSpeed(runningSpeed));

  const pBadge = $("progressNavBadge");
  if (pBadge) {
    pBadge.textContent = String(progressTasks.length);
    pBadge.classList.toggle("hidden", progressTasks.length === 0);
  }

  const fBadge = $("finishedNavBadge");
  if (fBadge) {
    fBadge.textContent = String(finishedTasks.length);
    fBadge.classList.toggle("hidden", finishedTasks.length === 0);
  }

  const clearFinBtn = $("clearFinishedBtn");
  if (clearFinBtn) {
    clearFinBtn.classList.toggle("hidden", finishedTasks.length === 0);
  }

  renderProgressList(progressTasks, dict);
  renderFinishedList(finishedTasks, dict);
}

function renderProgressList(list, dict) {
  const box = $("progressListContainer");
  if (!box) return;
  if (!list.length) {
    box.innerHTML = `
      <div class="card empty-card">
        <div>${escapeHtml(dict.emptyProgress)}</div>
        <div style="margin-top:12px;">
          <button type="button" class="btn btn-accent" onclick="window.switchTab('browser')">${escapeHtml(dict.goBrowserBtn)}</button>
        </div>
      </div>`;
    return;
  }

  box.innerHTML = "";
  for (const task of list) {
    let statusLabel = dict.statusQueued;
    let statusClass = "status-queued";
    let barClass = "";
    if (task.status === "probing") {
      statusLabel = dict.statusProbing;
      statusClass = "status-probing";
    } else if (task.status === "downloading") {
      statusLabel = task.rangeSupported && task.threads > 1
        ? dict.statusMulti(task.threads)
        : dict.statusSingle;
      statusClass = "status-downloading";
    } else if (task.status === "paused") {
      statusLabel = dict.statusPaused;
      statusClass = "status-paused";
      barClass = "paused";
    } else if (task.status === "error") {
      statusLabel = dict.statusError;
      statusClass = "status-error";
      barClass = "error";
    }

    const total = Number(task.totalBytes || 0);
    const dl = Number(task.downloadedBytes || 0);
    const pct = total > 0 ? Math.min(100, Math.max(0, Math.round((dl * 100) / total))) : 0;
    const sizeStr = total > 0
      ? `${formatBytes(dl)} / ${formatBytes(total)} (${pct}%)`
      : dl > 0 ? `${formatBytes(dl)} (${dict.unknownSize})` : dict.unknownSize;
    const etaStr = task.status === "downloading" && task.etaSeconds >= 0
      ? `${dict.etaPrefix} ${formatEta(task.etaSeconds)}`
      : "";

    const chunks = Array.isArray(task.chunks) ? task.chunks : [];
    const chunkHtml = chunks.length > 0
      ? `
        <div class="chunk-section">
          <div class="chunk-caption">
            <span>${escapeHtml(dict.chunkLabel(chunks.length, task.rangeSupported && chunks.length > 1))}</span>
            <span class="mono">${pct}%</span>
          </div>
          <div class="chunk-grid ${chunks.length <= 4 ? "cols-4" : ""}">
            ${chunks
              .map(
                (cp) =>
                  `<div class="chunk-cell"><div class="chunk-bar ${cp >= 100 ? "full" : ""}" style="width:${Math.min(100, Math.max(0, cp))}%"></div></div>`
              )
              .join("")}
          </div>
        </div>`
      : "";

    let actionsHtml = "";
    if (task.status === "downloading" || task.status === "probing" || task.status === "queued") {
      actionsHtml = `
        <button type="button" class="btn btn-subtle btn-sm" data-act="pause" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnPause)}</button>
        <button type="button" class="btn btn-danger-soft btn-sm" data-act="delete" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnCancel)}</button>
      `;
    } else if (task.status === "paused") {
      actionsHtml = `
        <button type="button" class="btn btn-primary btn-sm" data-act="resume" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnResume)}</button>
        <button type="button" class="btn btn-danger-soft btn-sm" data-act="delete" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnCancel)}</button>
      `;
    } else {
      actionsHtml = `
        <button type="button" class="btn btn-accent btn-sm" data-act="retry" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnRetry)}</button>
        <button type="button" class="btn btn-danger-soft btn-sm" data-act="delete" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnDelete)}</button>
      `;
    }

    const card = document.createElement("div");
    card.className = "task-card";
    card.innerHTML = `
      <div class="task-top">
        <div class="task-title-wrap">
          <span class="ext-badge">${escapeHtml(inferExtBadge(task.fileName))}</span>
          <span class="task-name">${escapeHtml(task.fileName)}</span>
        </div>
        <span class="status-tag ${statusClass}">${escapeHtml(statusLabel)}</span>
      </div>
      <div class="task-stats">
        <span>${escapeHtml(sizeStr)}</span>
        <span>${task.status === "downloading" ? escapeHtml(formatSpeed(task.speedBps)) : ""}${etaStr ? ` · ${escapeHtml(etaStr)}` : ""}</span>
      </div>
      <div class="progress-track">
        <div class="progress-fill ${barClass}" style="width:${pct}%"></div>
      </div>
      ${chunkHtml}
      ${task.error && task.status === "error" ? `<div class="task-error">${escapeHtml(task.error)}</div>` : ""}
      <div class="task-actions">${actionsHtml}</div>
    `;
    bindTaskActionButtons(card, dict);
    box.appendChild(card);
  }
}

function renderFinishedList(list, dict) {
  document.querySelectorAll("#finishedFilterSwitch .seg-btn").forEach((b) => {
    b.classList.toggle("active", b.dataset.type === appState.finishedFilter);
  });
  const box = $("finishedListContainer");
  if (!box) return;

  const filtered = list.filter((task) => {
    if (appState.finishedFilter === "all") return true;
    return inferTaskCategory(task) === appState.finishedFilter;
  });

  if (!list.length) {
    box.innerHTML = `<div class="card empty-card">${escapeHtml(dict.emptyFinished)}</div>`;
    return;
  }
  if (!filtered.length) {
    box.innerHTML = `<div class="card empty-card">${escapeHtml(dict.emptyFinishedFiltered)}</div>`;
    return;
  }

  box.innerHTML = "";
  for (const task of filtered) {
    const cat = inferTaskCategory(task);
    const typeEmoji = cat === "image" ? "🖼️" : cat === "audio" ? "🎵" : cat === "video" ? "🎬" : "📦";
    const total = Number(task.totalBytes || task.downloadedBytes || 0);
    const card = document.createElement("div");
    card.className = "task-card";
    card.innerHTML = `
      <div class="finished-row">
        <div class="finished-thumb-box">
          <span class="finished-thumb-type">${typeEmoji}</span>
          <span class="finished-thumb-ext">${escapeHtml(inferExtBadge(task.fileName))}</span>
        </div>
        <div class="finished-info">
          <div class="task-name">${escapeHtml(task.fileName)}</div>
          <div class="finished-sub mono">${escapeHtml(total > 0 ? formatBytes(total) : dict.statusCompleted)} · ${escapeHtml(task.publicDisplayPath || "Download/JoelPhonePro")}</div>
        </div>
        <span class="status-tag status-completed">100%</span>
      </div>
      <div class="task-actions">
        <button type="button" class="btn btn-primary btn-sm" data-act="open" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnOpen)}</button>
        <button type="button" class="btn btn-subtle btn-sm" data-act="share" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnShare)}</button>
        <button type="button" class="btn btn-subtle btn-sm" data-act="copy" data-url="${escapeHtml(task.url || "")}">${escapeHtml(dict.btnCopyLink)}</button>
        <button type="button" class="btn btn-danger-soft btn-sm" data-act="delete" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnDelete)}</button>
      </div>
    `;
    bindTaskActionButtons(card, dict);
    box.appendChild(card);
  }
}

function bindTaskActionButtons(card, dict) {
  card.querySelectorAll("[data-act]").forEach((btn) => {
    btn.addEventListener("click", () => {
      const act = btn.dataset.act;
      const id = btn.dataset.id;
      if (act === "copy") {
        const u = btn.dataset.url || "";
        if (window.JoelNative) window.JoelNative.setClipboardText(u);
        showToast(dict.toastCopied);
        return;
      }
      if (!window.JoelNative) {
        showToast(dict.toastBrowserPreview);
        return;
      }
      if (act === "pause") window.JoelNative.pauseTask(id);
      else if (act === "resume") window.JoelNative.resumeTask(id);
      else if (act === "retry") window.JoelNative.retryTask(id);
      else if (act === "delete") window.JoelNative.deleteTask(id, false);
      else if (act === "open") window.JoelNative.openFile(id);
      else if (act === "share") window.JoelNative.shareFile(id);
    });
  });
}

/* --- Direct Modal & Settings Modal --- */
function openDirectModal(prefillRaw) {
  const modal = $("directModal");
  if (!modal) return;
  modal.classList.remove("hidden");
  if (typeof prefillRaw === "string" && prefillRaw.trim()) {
    $("directRawInput").value = prefillRaw;
    delete $("directFileNameInput").dataset.userEdited;
  }
  syncDrawerThreadPills();
  renderDirectExtractor();
}

function closeDirectModal() {
  const modal = $("directModal");
  if (modal) modal.classList.add("hidden");
}

function renderDirectExtractor() {
  const el = $("directRawInput");
  if (!el) return;
  const info = extractCleanUrl(el.value);
  appState.directCleanUrl = info.cleanUrl;
  const cleanBox = $("directCleanBox");
  if (!info.cleanUrl) {
    if (cleanBox) cleanBox.classList.add("hidden");
    return;
  }
  if (cleanBox) cleanBox.classList.remove("hidden");
  syncText("directCleanPreview", info.cleanUrl);
  const fnEl = $("directFileNameInput");
  if (fnEl && (!fnEl.dataset.userEdited || fnEl.dataset.lastAutoUrl !== info.cleanUrl)) {
    fnEl.value = info.fileName;
    fnEl.dataset.lastAutoUrl = info.cleanUrl;
    delete fnEl.dataset.userEdited;
  }
}

function openSettingsModal() {
  const modal = $("settingsModal");
  if (modal) modal.classList.remove("hidden");
  renderSettings();
}

function closeSettingsModal() {
  const modal = $("settingsModal");
  if (modal) modal.classList.add("hidden");
}

function renderSettings() {
  const s = appState.settings;
  document.querySelectorAll("#defaultThreadsGrid .thread-pill").forEach((b) => {
    b.classList.toggle("active", Number(b.dataset.threads) === Number(s.defaultThreads));
  });
  document.querySelectorAll("#concurrentLimitGrid .thread-pill").forEach((b) => {
    b.classList.toggle("active", Number(b.dataset.limit) === Number(s.concurrentLimit));
  });
  document.querySelectorAll("#uaSwitch .seg-btn").forEach((b) => {
    b.classList.toggle("active", b.dataset.ua === s.defaultUaMode);
  });
  document.querySelectorAll("#settingsLangSwitch .seg-btn").forEach((b) => {
    b.classList.toggle("active", b.dataset.lang === appState.lang);
  });
}

function saveSettingsPatch(patch) {
  appState.settings = { ...appState.settings, ...patch };
  if (patch.defaultThreads) {
    appState.selectedThreads = patch.defaultThreads;
    syncDrawerThreadPills();
  }
  if (window.JoelNative) {
    window.JoelNative.saveSettings(JSON.stringify(patch));
  }
  renderSettings();
}

function triggerNativeDownload({ url, fileName, threads, referer, cookie, uaMode, switchToProgress }) {
  const dict = t();
  if (!url) {
    showToast(dict.toastNeedUrl);
    return;
  }
  if (!window.JoelNative) {
    showToast(dict.toastBrowserPreview);
    return;
  }
  const tCount = threads || appState.selectedThreads || 16;
  const resRaw = window.JoelNative.startDownload(
    url,
    fileName || "",
    tCount,
    referer || "",
    cookie || "",
    uaMode || appState.settings.defaultUaMode || "desktop"
  );
  try {
    const res = JSON.parse(resRaw || "{}");
    if (res.ok) {
      showToast(dict.toastTaskStarted(fileName || url, tCount));
      if (switchToProgress) {
        window.switchTab("progress");
      }
      return;
    }
    showToast(res.error || "Failed");
  } catch {
    if (switchToProgress) window.switchTab("progress");
  }
}

/* --- Clipboard & Native Callbacks --- */
async function readClipboardAny() {
  if (window.JoelNative) {
    return window.JoelNative.getClipboardText() || "";
  }
  if (navigator.clipboard?.readText) {
    try {
      return await navigator.clipboard.readText();
    } catch {}
  }
  return "";
}

async function checkClipboardBanner() {
  const raw = await readClipboardAny();
  const info = extractCleanUrl(raw);
  const card = $("copiedLinkCard");
  if (!card) return;
  if (!info.cleanUrl || info.cleanUrl === appState.dismissedCopiedUrl) {
    if (!appState.copiedDetectedUrl) card.classList.add("hidden");
    return;
  }
  appState.copiedDetectedUrl = info.cleanUrl;
  syncText("copiedLinkText", info.cleanUrl);
  card.classList.remove("hidden");
}

window.onAppWindowFocus = function onAppWindowFocus() {
  if (appState.activeTab === "browser" && !appState.browserShowingInTab1) {
    checkClipboardBanner();
  }
};

window.onNativeStateUpdate = function onNativeStateUpdate(payload) {
  if (!payload || typeof payload !== "object") return;
  if (Array.isArray(payload.tasks)) {
    appState.tasks = payload.tasks;
  }
  if (payload.settings) {
    const firstSync = !window._initialSettingsSynced;
    window._initialSettingsSynced = true;
    appState.settings = { ...appState.settings, ...payload.settings };
    if (firstSync) {
      appState.selectedThreads = appState.settings.defaultThreads || 16;
      syncDrawerThreadPills();
      if (payload.settings.lang === "en" || payload.settings.lang === "zh") {
        appState.lang = payload.settings.lang;
        applyI18n();
        return;
      }
    }
  }
  renderTasks();
  renderSettings();
};

window.onExternalSharedText = function onExternalSharedText(text) {
  if (!text) return;
  const info = extractCleanUrl(text);
  if (info.cleanUrl) {
    appState.copiedDetectedUrl = info.cleanUrl;
    syncText("copiedLinkText", info.cleanUrl);
    if ($("copiedLinkCard")) $("copiedLinkCard").classList.remove("hidden");
    window.switchTab("browser");
    showToast(t().toastSharedLoaded);
    openUrlInBrowserTab(info.cleanUrl);
  }
};

window.onActiveVideoGroupUpdate = function onActiveVideoGroupUpdate(group) {
  if (!group || typeof group !== "object") return;
  const prevKey = appState.activeVideo ? appState.activeVideo.videoKey : "";
  const isNewVideo = group.videoKey !== prevKey;
  appState.activeVideo = group;
  if (isNewVideo) {
    appState.customFileName = "";
    appState.renameExpanded = false;
    stopInlinePreview();
    const first = Array.isArray(group.variants) && group.variants.length ? group.variants[0] : null;
    appState.selectedVariantId = first ? (first.id || "") : "";
    appState.selectedVariantUrl = first ? (first.url || "") : "";
  } else if (Array.isArray(group.variants) && group.variants.length) {
    const stillValid = group.variants.find(
      (x) => (appState.selectedVariantId && x.id === appState.selectedVariantId) || x.url === appState.selectedVariantUrl
    );
    if (!stillValid) {
      appState.selectedVariantId = group.variants[0].id || "";
      appState.selectedVariantUrl = group.variants[0].url || "";
    }
  }
  if (appState.drawerOpen) {
    renderDownloadQualitySheet();
  }
};

window.onSniffedMediaUpdate = function onSniffedMediaUpdate(payload) {
  if (!payload || !payload.url || !appState.activeVideo || !Array.isArray(appState.activeVideo.variants)) return;
  const list = appState.activeVideo.variants;
  let updated = false;
  for (let i = list.length - 1; i >= 0; i--) {
    const item = list[i];
    const matchById = payload.id && item.id === payload.id;
    const matchByUrl = !payload.id && item.url === payload.url;
    if (!matchById && !matchByUrl) continue;

    if (payload.invalid && payload.source !== "DOM") {
      list.splice(i, 1);
      if (appState.selectedVariantId === item.id || appState.selectedVariantUrl === item.url) {
        appState.selectedVariantId = list.length ? (list[0].id || "") : "";
        appState.selectedVariantUrl = list.length ? (list[0].url || "") : "";
      }
      updated = true;
    } else if (!payload.invalid) {
      list[i] = {
        ...item,
        sizeBytes: payload.sizeBytes > 0 ? payload.sizeBytes : item.sizeBytes,
        contentType: payload.contentType || item.contentType,
        suggestedName: payload.suggestedName || item.suggestedName,
      };
      updated = true;
    }
  }

  if (updated && appState.drawerOpen) {
    renderDownloadQualitySheet();
  }
};

window.onSniffedCleared = function onSniffedCleared() {
  appState.activeVideo = null;
  appState.selectedVariantId = "";
  appState.selectedVariantUrl = "";
  appState.customFileName = "";
  appState.renameExpanded = false;
  stopInlinePreview();
  if (appState.drawerOpen) {
    renderDownloadQualitySheet();
  }
};

function setAppLanguage(nextLang) {
  appState.lang = nextLang === "en" ? "en" : "zh";
  saveSettingsPatch({ lang: appState.lang });
  applyI18n();
}

function bindEvents() {
  // Global tactile haptic tick
  document.addEventListener(
    "pointerdown",
    (ev) => {
      if (
        ev.target &&
        ev.target.closest &&
        ev.target.closest(".btn, .nav-btn, .site-item, .toolbar-icon-btn, .search-go-btn, .thread-pill, .drawer-thread-btn, .seg-btn, .quality-card")
      ) {
        if (window.JoelNative && window.JoelNative.hapticTick) {
          window.JoelNative.hapticTick();
        }
      }
    },
    { passive: true }
  );

  // Bottom 3-Tab Navigation
  document.querySelectorAll(".nav-btn").forEach((btn) => {
    btn.addEventListener("click", () => window.switchTab(btn.dataset.tab));
  });

  // Top Toolbar
  $("topHomeBtn").addEventListener("click", () => {
    window.switchTab("browser");
    if (appState.browserShowingInTab1 && window.JoelNative && window.JoelNative.minimizeBrowserToHome) {
      window.JoelNative.minimizeBrowserToHome();
    }
  });

  $("topSearchGoBtn").addEventListener("click", () => {
    openUrlInBrowserTab($("topSearchInput").value);
  });

  $("topSearchInput").addEventListener("keydown", (ev) => {
    if (ev.key === "Enter") {
      ev.preventDefault();
      openUrlInBrowserTab($("topSearchInput").value);
    }
  });

  $("openDirectModalBtn").addEventListener("click", () => {
    openDirectModal();
  });

  $("openSettingsBtn").addEventListener("click", () => {
    openSettingsModal();
  });

  // Copied Link Banner
  $("copiedDismissBtn").addEventListener("click", () => {
    appState.dismissedCopiedUrl = appState.copiedDetectedUrl;
    $("copiedLinkCard").classList.add("hidden");
  });

  $("copiedOpenBrowserBtn").addEventListener("click", () => {
    if (appState.copiedDetectedUrl) {
      openUrlInBrowserTab(appState.copiedDetectedUrl);
    }
  });

  $("copiedDirectDownloadBtn").addEventListener("click", () => {
    if (appState.copiedDetectedUrl) {
      openDirectModal(appState.copiedDetectedUrl);
    }
  });

  // 8 Popular Video Websites
  document.querySelectorAll("#sitesGrid .site-item").forEach((btn) => {
    btn.addEventListener("click", () => {
      openUrlInBrowserTab(btn.dataset.url);
    });
  });

  // Browser Home Thumb Dock
  $("homePasteOpenBtn").addEventListener("click", async () => {
    const raw = await readClipboardAny();
    if (!raw.trim()) {
      showToast(t().toastClipboardEmpty);
      return;
    }
    const info = extractCleanUrl(raw);
    openUrlInBrowserTab(info.cleanUrl || raw.trim());
  });

  $("homeResumeBrowserBtn").addEventListener("click", () => {
    if (window.JoelNative && window.JoelNative.resumeBrowserPage) {
      window.JoelNative.resumeBrowserPage();
    } else {
      showToast(t().toastBrowserPreview);
    }
  });

  // Progress Tab Actions
  $("pauseAllBtn").addEventListener("click", () => {
    if (!window.JoelNative) {
      showToast(t().toastBrowserPreview);
      return;
    }
    for (const task of appState.tasks || []) {
      if (task.status === "downloading" || task.status === "probing" || task.status === "queued") {
        window.JoelNative.pauseTask(task.id);
      }
    }
    showToast(t().toastPausedAll);
  });

  $("resumeAllBtn").addEventListener("click", () => {
    if (!window.JoelNative) {
      showToast(t().toastBrowserPreview);
      return;
    }
    for (const task of appState.tasks || []) {
      if (task.status === "paused" || task.status === "error") {
        window.JoelNative.resumeTask(task.id);
      }
    }
    showToast(t().toastResumedAll);
  });

  $("progressBackToBrowserBtn").addEventListener("click", () => {
    window.switchTab("browser");
    if (window.JoelNative && window.JoelNative.resumeBrowserPage) {
      window.JoelNative.resumeBrowserPage();
    }
  });

  // Finished Tab Actions & Filters
  document.querySelectorAll("#finishedFilterSwitch .seg-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      appState.finishedFilter = btn.dataset.type || "all";
      renderTasks();
    });
  });

  $("clearFinishedBtn").addEventListener("click", () => {
    if (window.JoelNative) {
      window.JoelNative.clearCompleted();
    } else {
      appState.tasks = appState.tasks.filter((x) => x.status !== "completed");
      renderTasks();
    }
  });

  // Single-Video Quality Picker Drawer Events
  $("drawerBackdrop").addEventListener("click", () => window.closeDownloadDrawer(false));
  $("drawerTopCloseBtn").addEventListener("click", () => window.closeDownloadDrawer(false));
  $("drawerCloseBtn").addEventListener("click", () => window.closeDownloadDrawer(false));

  $("drawerThumbBox").addEventListener("click", () => {
    const sel = getSelectedVariant();
    if (!sel) return;
    if (appState.previewExpanded) {
      stopInlinePreview();
    } else {
      appState.previewExpanded = true;
      loadInlinePreviewStream(sel);
    }
    renderDownloadQualitySheet();
  });

  $("drawerRenameBtn").addEventListener("click", () => {
    appState.renameExpanded = !appState.renameExpanded;
    if (appState.renameExpanded && $("drawerRenameInput")) {
      $("drawerRenameInput").value = deriveVideoBaseFileName();
    }
    renderDownloadQualitySheet();
    if (appState.renameExpanded && $("drawerRenameInput")) {
      $("drawerRenameInput").focus();
    }
  });

  $("drawerRenameDoneBtn").addEventListener("click", () => {
    if ($("drawerRenameInput")) {
      appState.customFileName = $("drawerRenameInput").value;
    }
    appState.renameExpanded = false;
    renderDownloadQualitySheet();
  });

  $("drawerRenameInput").addEventListener("input", () => {
    appState.customFileName = $("drawerRenameInput").value;
    syncText("drawerVideoTitle", deriveVideoBaseFileName());
  });

  document.querySelectorAll("#drawerThreadSelector .drawer-thread-btn").forEach((pill) => {
    pill.addEventListener("click", () => {
      appState.selectedThreads = Number(pill.dataset.threads);
      syncDrawerThreadPills();
      saveSettingsPatch({ defaultThreads: appState.selectedThreads });
      if (appState.drawerOpen) {
        renderDownloadQualitySheet();
      }
    });
  });

  $("drawerDownloadBtn").addEventListener("click", () => {
    const dict = t();
    const sel = getSelectedVariant();
    if (!sel) {
      showToast(dict.emptyDrawer);
      return;
    }
    if (!window.JoelNative) {
      showToast(dict.toastBrowserPreview);
      return;
    }
    const finalName = deriveVideoBaseFileName();
    window.closeDownloadDrawer(true);
    triggerNativeDownload({
      url: sel.url,
      fileName: finalName,
      threads: appState.selectedThreads,
      referer: sel.referer || "",
      cookie: sel.cookie || "",
      uaMode: sel.uaMode || appState.settings.defaultUaMode || "mobile",
      switchToProgress: true,
    });
  });

  // Direct Modal Sheet
  $("directBackdrop").addEventListener("click", closeDirectModal);
  $("directCloseBtn").addEventListener("click", closeDirectModal);
  $("directRawInput").addEventListener("input", renderDirectExtractor);
  $("directFileNameInput").addEventListener("input", () => {
    $("directFileNameInput").dataset.userEdited = "1";
  });
  $("directHeadersToggle").addEventListener("click", () => {
    appState.directHeadersExpanded = !appState.directHeadersExpanded;
    $("directHeadersBox").classList.toggle("hidden", !appState.directHeadersExpanded);
    syncText(
      "directHeadersToggle",
      appState.directHeadersExpanded ? t().directHeadersShow : t().directHeadersHide
    );
  });
  document.querySelectorAll("#directThreadSelector .thread-pill").forEach((pill) => {
    pill.addEventListener("click", () => {
      appState.selectedThreads = Number(pill.dataset.threads);
      syncDrawerThreadPills();
    });
  });
  $("directPasteBtn").addEventListener("click", async () => {
    const raw = await readClipboardAny();
    if (!raw.trim()) {
      showToast(t().toastClipboardEmpty);
      return;
    }
    $("directRawInput").value = raw;
    delete $("directFileNameInput").dataset.userEdited;
    renderDirectExtractor();
  });
  $("directSniffBtn").addEventListener("click", () => {
    const target = appState.directCleanUrl || $("directRawInput").value.trim();
    if (!target) {
      showToast(t().toastNeedUrl);
      return;
    }
    closeDirectModal();
    openUrlInBrowserTab(target);
  });
  $("directStartBtn").addEventListener("click", () => {
    if (!appState.directCleanUrl) {
      showToast(t().toastNeedUrl);
      return;
    }
    const fn = $("directFileNameInput").value.trim();
    const ref = $("directRefererInput").value.trim();
    const ck = $("directCookieInput").value.trim();
    closeDirectModal();
    triggerNativeDownload({
      url: appState.directCleanUrl,
      fileName: fn,
      threads: appState.selectedThreads,
      referer: ref,
      cookie: ck,
      switchToProgress: true,
    });
  });

  // Settings Modal Sheet
  $("settingsBackdrop").addEventListener("click", closeSettingsModal);
  $("settingsCloseBtn").addEventListener("click", closeSettingsModal);

  document.querySelectorAll("#settingsLangSwitch .seg-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      setAppLanguage(btn.dataset.lang);
      showToast(t().toastSettingsSaved);
    });
  });
  document.querySelectorAll("#defaultThreadsGrid .thread-pill").forEach((pill) => {
    pill.addEventListener("click", () => {
      saveSettingsPatch({ defaultThreads: Number(pill.dataset.threads) });
      showToast(t().toastSettingsSaved);
    });
  });
  document.querySelectorAll("#concurrentLimitGrid .thread-pill").forEach((pill) => {
    pill.addEventListener("click", () => {
      saveSettingsPatch({ concurrentLimit: Number(pill.dataset.limit) });
      showToast(t().toastSettingsSaved);
    });
  });
  document.querySelectorAll("#uaSwitch .seg-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      saveSettingsPatch({ defaultUaMode: btn.dataset.ua });
      showToast(t().toastSettingsSaved);
    });
  });
  $("clearBrowserCacheBtn").addEventListener("click", () => {
    if (window.JoelNative && window.JoelNative.clearBrowserData) {
      window.JoelNative.clearBrowserData();
    } else {
      showToast(t().toastBrowserPreview);
    }
  });
}

// Init
bindEvents();
applyI18n();
if (window.JoelNative) {
  try {
    const initial = JSON.parse(window.JoelNative.getStateJson() || "{}");
    window.onNativeStateUpdate(initial);
  } catch {}
  setTimeout(checkClipboardBanner, 300);
}
