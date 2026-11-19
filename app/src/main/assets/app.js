// Joel Phone Pro — Mobile Frontend & Android Bridge
const $ = (id) => document.getElementById(id);

const I18N = {
  zh: {
    brandSub: "多线程极速下载",
    pasteTitle: "智能提取与粘贴",
    shareTipBadge: "支持系统分享直达",
    pastePlaceholder: "粘贴任意链接或带杂字的分享文案（如抖音、B站、直链、APK、压缩包）…",
    pasteClipBtn: "📋 一键粘贴剪贴板",
    clearInputBtn: "清空",
    extractBadgeNoise: "已自动过滤杂字提取纯链",
    extractBadgeClean: "已识别下载链接",
    copyCleanUrlBtn: "复制纯链",
    fileNameLabel: "保存文件名",
    toggleHeadersShow: "▾ 收起高级请求头",
    toggleHeadersHide: "▸ 高级请求头 (Referer / Cookie 选填)",
    threadSelectLabel: "并发线程数",
    threadHint: "Range 多线程分片提速",
    startDownloadBtn: "⚡ 开始多线程下载",
    openInSnifferBtn: "🌐 如果是网页，点此内置嗅探真实资源",
    tip1Title: "真 · 多线程 HTTP Range 分片加速",
    tip1Desc: "把大文件切分为 4~32 个字节分段并发拉取，断网暂停不丢分片进度。",
    tip2Title: "自动去杂字 + 手机系统「分享」接入",
    tip2Desc: "在微信、浏览器、B站点分享 → 选择 Joel Phone Pro，自动提取纯净下载链接。",
    tip3Title: "统一存入手机 Download/JoelPhonePro/",
    tip3Desc: "下载完直接出现在系统文件管理器与下载目录，可一键打开或转发。",
    filterAllBtn: "全部",
    filterActiveBtn: "进行中",
    filterDoneBtn: "已完成",
    clearDoneBtn: "清理已完成",
    emptyTasks: "暂无下载任务，去「智能提取」粘贴链接即可开始多线程下载。",
    emptyFiltered: "当前筛选条件下没有任务。",
    goExtractBtn: "前往粘贴链接",
    snifferSmartBadge: "自动去杂字",
    snifferTitle: "内置网页媒体与资源嗅探",
    snifferDesc: "支持直接粘贴抖音 / 小红书 / B站整段分享文案或网页链接，自动剔除多余文字后打开内置浏览器嗅探真实视频直链，并支持下载前「预浏览」确认内容。",
    snifferPlaceholder: "粘贴抖音/小红书分享文案（如：7.46 复制打开抖音… https://v.douyin.com/…）或网页链接…",
    snifferExtractBadge: "已自动过滤多余分享文字",
    snifferPasteGoBtn: "📋 粘贴并嗅探",
    launchSnifferBtn: "🌐 打开嗅探",
    snifferClearInputBtn: "清空",
    sniffedListTitle: "已捕获资源",
    clearSniffedBtn: "清空列表",
    sniffFilterAllBtn: "全部",
    sniffFilterVideoBtn: "🎬 视频",
    sniffFilterAudioBtn: "🎵 音频",
    sniffFilterImageBtn: "🖼️ 图片",
    sniffFilterFileBtn: "📦 文件",
    emptySniffed: "暂无捕获到的媒体直链。粘贴抖音或网页链接打开嗅探并播放，资源会显示在这里。",
    emptySniffedFiltered: "当前分类下没有捕获到的资源。",
    btnPreviewOpen: "👁️ 预浏览",
    btnPreviewClose: "🔼 收起预览",
    btnPreviewDownloadNow: "⚡ 立即下载",
    previewLoading: "正在加载媒体元信息…",
    previewLoadError: "无法在线预浏览此流（可能需鉴权或为加密流），仍可尝试多线程下载。",
    previewFileLabel: "文件预览信息",
    previewRenamePlaceholder: "可重命名保存文件名…",
    mediaLabelVideo: "🎬 视频",
    mediaLabelAudio: "🎵 音频",
    mediaLabelImage: "🖼️ 图片",
    mediaLabelFile: "📦 文件",
    badgeDomPrimary: "页面主媒体",
    sizeProbing: "探测大小中…",
    toastExtractedSniff: "已剔除杂字提取链接，正在打开嗅探",
    toastSniffNoUrl: "未检测到有效 http/https 链接，请检查粘贴内容",
    sniffSelectAll: "☑️ 全选本页",
    sniffDeselectAll: "☐ 取消全选",
    sniffBatchDownload: (n) => `⚡ 批量下载选中 (${n})`,
    toastBatchStarted: (n) => `已批量启动 ${n} 个多线程下载任务`,
    toastSelectOneFirst: "请先勾选至少一个要下载的资源",
    settingsHeading: "下载引擎设置",
    setDefaultThreadsLabel: "默认并发线程数",
    setConcurrentLabel: "同时运行任务上限",
    setUaLabel: "请求伪装标识 (User-Agent)",
    uaDesktopBtn: "桌面端 Chrome",
    uaMobileBtn: "移动端 Android",
    setPathLabel: "文件保存目录",
    setPathNote: "遵循 Android 公共下载目录规范，无需授予高危全盘读写权限，系统文件管理器直接可见。",
    navExtractLabel: "智能提取",
    navDownloadsLabel: "下载队列",
    navSnifferLabel: "网页嗅探",
    navSettingsLabel: "设置",
    threadsWord: "线程",
    recTag: "推荐",
    fastTag: "极速",
    statusQueued: "排队中",
    statusProbing: "探测分片中...",
    statusMulti: (n) => `${n} 线程并发`,
    statusSingle: "单流下载",
    statusPaused: "已暂停",
    statusCompleted: "已完成",
    statusError: "下载失败",
    chunkLabel: (n, ok) => (ok ? `分片并发 (${n} 线程)` : "服务器不支持分片 · 单流接收"),
    btnPause: "暂停",
    btnResume: "继续",
    btnRetry: "重试",
    btnOpen: "打开文件",
    btnShare: "分享",
    btnDelete: "删除",
    btnSniffDownload: (n) => `⚡ ${n} 线程下载`,
    btnSniffFill: "填入提取器",
    etaPrefix: "剩余",
    unknownSize: "大小未知",
    toastCopied: "已复制纯净链接",
    toastClipboardEmpty: "剪贴板为空，或未读取到文本",
    toastTaskStarted: (name, n) => `已启动 ${n} 线程下载：${name}`,
    toastNeedUrl: "请先输入有效网页或下载链接",
    toastSharedLoaded: "已接收系统分享内容并提取链接",
    toastSettingsSaved: "设置已保存",
    toastBrowserPreview: "当前为网页预览模式，安装到手机 APK 后将启用原生多线程引擎",
  },
  en: {
    brandSub: "Multi-Thread Mobile Downloader",
    pasteTitle: "Smart Link Extractor",
    shareTipBadge: "System Share Supported",
    pastePlaceholder: "Paste any link or messy share text (mixed text + URL, MP4, APK, ZIP, PDF)…",
    pasteClipBtn: "📋 Paste Clipboard",
    clearInputBtn: "Clear",
    extractBadgeNoise: "Cleaned noise & extracted URL",
    extractBadgeClean: "URL Ready",
    copyCleanUrlBtn: "Copy URL",
    fileNameLabel: "Output Filename",
    toggleHeadersShow: "▾ Hide Request Headers",
    toggleHeadersHide: "▸ Request Headers (Referer / Cookie)",
    threadSelectLabel: "Concurrent Threads",
    threadHint: "Parallel HTTP Range chunks",
    startDownloadBtn: "⚡ Start Multi-Thread Download",
    openInSnifferBtn: "🌐 Webpage link? Open in Built-in Media Sniffer",
    tip1Title: "True Multi-Thread HTTP Range Engine",
    tip1Desc: "Splits files into 4–32 byte-range segments concurrently with resume support.",
    tip2Title: "Noise Filter & Android Share Sheet",
    tip2Desc: "Share from any app to Joel Phone Pro to extract clean URLs automatically.",
    tip3Title: "Saved to Download/JoelPhonePro/",
    tip3Desc: "Completed files appear cleanly in Android Files with 1-tap open and share.",
    filterAllBtn: "All",
    filterActiveBtn: "Active",
    filterDoneBtn: "Done",
    clearDoneBtn: "Clear Done",
    emptyTasks: "No downloads yet. Paste a link in Extract tab to start fast parallel downloading.",
    emptyFiltered: "No tasks match this filter.",
    goExtractBtn: "Go Extract Link",
    snifferSmartBadge: "Auto URL Clean",
    snifferTitle: "Built-in Web Media & File Sniffer",
    snifferDesc: "Paste Douyin / Xiaohongshu / Bilibili share copy or webpage links directly. Extra text is stripped automatically, with inline Mini Preview before downloading.",
    snifferPlaceholder: "Paste share copy (mixed text + URL) or webpage URL…",
    snifferExtractBadge: "Noise stripped · Clean URL ready",
    snifferPasteGoBtn: "📋 Paste & Sniff",
    launchSnifferBtn: "🌐 Open Sniffer",
    snifferClearInputBtn: "Clear",
    sniffedListTitle: "Captured Resources",
    clearSniffedBtn: "Clear",
    sniffFilterAllBtn: "All",
    sniffFilterVideoBtn: "🎬 Video",
    sniffFilterAudioBtn: "🎵 Audio",
    sniffFilterImageBtn: "🖼️ Image",
    sniffFilterFileBtn: "📦 File",
    emptySniffed: "No media streams captured yet. Paste a share text or webpage URL above to detect media links.",
    emptySniffedFiltered: "No captured resources match this category.",
    btnPreviewOpen: "👁️ Preview",
    btnPreviewClose: "🔼 Hide",
    btnPreviewDownloadNow: "⚡ Download",
    previewLoading: "Loading media stream metadata…",
    previewLoadError: "Cannot stream inline preview (token/DRM restriction), you can still try downloading.",
    previewFileLabel: "File Info",
    previewRenamePlaceholder: "Optional filename…",
    mediaLabelVideo: "🎬 Video",
    mediaLabelAudio: "🎵 Audio",
    mediaLabelImage: "🖼️ Image",
    mediaLabelFile: "📦 File",
    badgeDomPrimary: "DOM Media",
    sizeProbing: "Probing size…",
    toastExtractedSniff: "Cleaned share text & opening sniffer",
    toastSniffNoUrl: "No valid http/https URL detected in input",
    sniffSelectAll: "☑️ Select All",
    sniffDeselectAll: "☐ Deselect All",
    sniffBatchDownload: (n) => `⚡ Download Selected (${n})`,
    toastBatchStarted: (n) => `Started ${n} batch download tasks`,
    toastSelectOneFirst: "Select at least one resource first",
    settingsHeading: "Engine Settings",
    setDefaultThreadsLabel: "Default Thread Count",
    setConcurrentLabel: "Max Concurrent Downloads",
    setUaLabel: "User-Agent Identity",
    uaDesktopBtn: "Desktop Chrome",
    uaMobileBtn: "Mobile Android",
    setPathLabel: "Public Save Folder",
    setPathNote: "Uses Android Scoped Storage Downloads collection cleanly — visible in any file manager without broad storage permissions.",
    navExtractLabel: "Extract",
    navDownloadsLabel: "Downloads",
    navSnifferLabel: "Sniffer",
    navSettingsLabel: "Settings",
    threadsWord: "Threads",
    recTag: "Rec",
    fastTag: "Turbo",
    statusQueued: "Queued",
    statusProbing: "Probing...",
    statusMulti: (n) => `${n} Threads`,
    statusSingle: "Single Stream",
    statusPaused: "Paused",
    statusCompleted: "Completed",
    statusError: "Failed",
    chunkLabel: (n, ok) => (ok ? `Parallel Chunks (${n} threads)` : "Range unsupported · Single stream"),
    btnPause: "Pause",
    btnResume: "Resume",
    btnRetry: "Retry",
    btnOpen: "Open",
    btnShare: "Share",
    btnDelete: "Delete",
    btnSniffDownload: (n) => `⚡ ${n}T Download`,
    btnSniffFill: "Fill Extractor",
    etaPrefix: "ETA",
    unknownSize: "Unknown size",
    toastCopied: "Clean URL copied",
    toastClipboardEmpty: "Clipboard is empty",
    toastTaskStarted: (name, n) => `Started ${n}-thread download: ${name}`,
    toastNeedUrl: "Enter a valid http/https URL first",
    toastSharedLoaded: "Loaded shared link cleanly",
    toastSettingsSaved: "Settings saved",
    toastBrowserPreview: "Web preview mode — install APK on Android for real multi-thread downloads",
  },
};

const appState = {
  lang: "zh",
  activeTab: "extract",
  filter: "all",
  sniffTypeFilter: "all",
  activePreviewUrl: null,
  selectedSniffUrls: new Set(),
  selectedThreads: 16,
  headersExpanded: false,
  extractedUrl: "",
  snifferCleanUrl: "",
  tasks: [],
  sniffed: [],
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
  const match = rawText.match(/https?:\/\/[A-Za-z0-9\-._~:/?#[\]@!$&'()*+,;=%]+/i);
  if (!match) return { cleanUrl: "", hadNoise: false, fileName: "" };
  const cleanUrl = match[0].replace(/[),.;!?'"\]]+$/g, "");
  const hadNoise = rawText.trim() !== cleanUrl;

  let fileName = "download_file.bin";
  try {
    const parsed = new URL(cleanUrl);
    const segs = parsed.pathname.split("/").filter(Boolean);
    const last = segs[segs.length - 1];
    if (last && last.includes(".")) {
      fileName = decodeURIComponent(last);
    } else if (last) {
      fileName = `${decodeURIComponent(last)}.mp4`;
    } else {
      fileName = `${parsed.hostname.replace(/^www\./, "")}.bin`;
    }
  } catch {
    fileName = "download_file.bin";
  }
  return { cleanUrl, hadNoise, fileName };
}

function inferExtBadge(fileName) {
  const parts = String(fileName || "").split(".");
  if (parts.length > 1) {
    return parts[parts.length - 1].slice(0, 5).toUpperCase();
  }
  return "FILE";
}

function syncText(id, text) {
  const el = $(id);
  if (el) el.textContent = text;
}

function applyI18n() {
  const dict = t();
  document.documentElement.lang = appState.lang === "en" ? "en" : "zh-CN";
  document.querySelectorAll(".lang-btn").forEach((b) => {
    b.classList.toggle("active", b.dataset.lang === appState.lang);
  });

  syncText("brandSub", dict.brandSub);
  syncText("pasteTitle", dict.pasteTitle);
  syncText("shareTipBadge", dict.shareTipBadge);
  if ($("rawInput")) $("rawInput").placeholder = dict.pastePlaceholder;
  syncText("pasteClipBtn", dict.pasteClipBtn);
  syncText("clearInputBtn", dict.clearInputBtn);
  syncText("copyCleanUrlBtn", dict.copyCleanUrlBtn);
  syncText("fileNameLabel", dict.fileNameLabel);
  syncText("toggleHeadersLabel", appState.headersExpanded ? dict.toggleHeadersShow : dict.toggleHeadersHide);
  syncText("threadSelectLabel", dict.threadSelectLabel);
  syncText("threadHint", dict.threadHint);
  syncText("startDownloadBtn", dict.startDownloadBtn);
  syncText("openInSnifferBtn", dict.openInSnifferBtn);
  syncText("tip1Title", dict.tip1Title);
  syncText("tip1Desc", dict.tip1Desc);
  syncText("tip2Title", dict.tip2Title);
  syncText("tip2Desc", dict.tip2Desc);
  syncText("tip3Title", dict.tip3Title);
  syncText("tip3Desc", dict.tip3Desc);

  syncText("filterAllBtn", dict.filterAllBtn);
  syncText("filterActiveBtn", dict.filterActiveBtn);
  syncText("filterDoneBtn", dict.filterDoneBtn);
  syncText("clearDoneBtn", dict.clearDoneBtn);

  syncText("snifferSmartBadge", dict.snifferSmartBadge);
  syncText("snifferTitle", dict.snifferTitle);
  syncText("snifferDesc", dict.snifferDesc);
  if ($("snifferUrlBox")) $("snifferUrlBox").placeholder = dict.snifferPlaceholder;
  syncText("snifferExtractBadge", dict.snifferExtractBadge);
  syncText("snifferPasteGoBtn", dict.snifferPasteGoBtn);
  syncText("launchSnifferBtn", dict.launchSnifferBtn);
  syncText("snifferClearInputBtn", dict.snifferClearInputBtn);
  syncText("sniffedListTitle", dict.sniffedListTitle);
  syncText("clearSniffedBtn", dict.clearSniffedBtn);
  syncText("sniffFilterAllBtn", dict.sniffFilterAllBtn);
  syncText("sniffFilterVideoBtn", dict.sniffFilterVideoBtn);
  syncText("sniffFilterAudioBtn", dict.sniffFilterAudioBtn);
  syncText("sniffFilterImageBtn", dict.sniffFilterImageBtn);
  syncText("sniffFilterFileBtn", dict.sniffFilterFileBtn);

  syncText("settingsHeading", dict.settingsHeading);
  syncText("setDefaultThreadsLabel", dict.setDefaultThreadsLabel);
  syncText("setConcurrentLabel", dict.setConcurrentLabel);
  syncText("setUaLabel", dict.setUaLabel);
  syncText("uaDesktopBtn", dict.uaDesktopBtn);
  syncText("uaMobileBtn", dict.uaMobileBtn);
  syncText("setPathLabel", dict.setPathLabel);
  syncText("setPathNote", dict.setPathNote);

  syncText("navExtractLabel", dict.navExtractLabel);
  syncText("navDownloadsLabel", dict.navDownloadsLabel);
  syncText("navSnifferLabel", dict.navSnifferLabel);
  syncText("navSettingsLabel", dict.navSettingsLabel);

  // Update thread pills labels cleanly
  document.querySelectorAll("#threadSelector .thread-pill").forEach((pill) => {
    const n = Number(pill.dataset.threads);
    if (n === 16) {
      pill.innerHTML = `${n} ${dict.threadsWord}<small>${dict.recTag}</small>`;
    } else if (n === 32) {
      pill.innerHTML = `${n} ${dict.threadsWord}<small>${dict.fastTag}</small>`;
    } else {
      pill.textContent = `${n} ${dict.threadsWord}`;
    }
  });

  renderExtractor();
  renderSnifferInput();
  renderTasks();
  renderSniffed();
  renderSettings();
}

window.switchTab = function switchTab(tab) {
  appState.activeTab = tab;
  document.querySelectorAll(".tab-panel").forEach((p) => {
    p.classList.toggle("active", p.id === `panel-${tab}`);
  });
  document.querySelectorAll(".nav-btn").forEach((b) => {
    b.classList.toggle("active", b.dataset.tab === tab);
  });
};

window.handleAndroidBack = function handleAndroidBack() {
  if (appState.activeTab !== "extract") {
    window.switchTab("extract");
    return true;
  }
  return false;
};

function renderExtractor() {
  const dict = t();
  const raw = $("rawInput").value;
  const info = extractCleanUrl(raw);
  appState.extractedUrl = info.cleanUrl;

  if (!info.cleanUrl) {
    $("extractResultCard").classList.add("hidden");
    $("extractEmptyHint").classList.remove("hidden");
    return;
  }

  $("extractResultCard").classList.remove("hidden");
  $("extractEmptyHint").classList.add("hidden");
  $("extractBadge").textContent = info.hadNoise ? dict.extractBadgeNoise : dict.extractBadgeClean;
  $("cleanUrlPreview").textContent = info.cleanUrl;

  const fnEl = $("fileNameInput");
  if (!fnEl.dataset.userEdited || fnEl.dataset.lastAutoUrl !== info.cleanUrl) {
    fnEl.value = info.fileName;
    fnEl.dataset.lastAutoUrl = info.cleanUrl;
    delete fnEl.dataset.userEdited;
  }
}

function renderTasks() {
  const dict = t();
  const tasks = appState.tasks || [];

  let activeCount = 0;
  let totalSpeed = 0;
  let completedCount = 0;
  for (const task of tasks) {
    if (task.status === "downloading" || task.status === "probing" || task.status === "queued") {
      activeCount++;
      totalSpeed += Number(task.speedBps || 0);
    } else if (task.status === "completed") {
      completedCount++;
    }
  }

  // Update global speed pill & nav badge
  const speedPill = $("globalSpeedPill");
  speedPill.classList.toggle("active", activeCount > 0);
  $("globalSpeedText").textContent = formatSpeed(totalSpeed);

  const navBadge = $("navDownloadsBadge");
  if (activeCount > 0) {
    navBadge.textContent = String(activeCount);
    navBadge.classList.remove("hidden");
  } else {
    navBadge.classList.add("hidden");
  }

  $("clearDoneBtn").classList.toggle("hidden", completedCount === 0);

  const filtered = tasks.filter((task) => {
    if (appState.filter === "active") {
      return task.status === "downloading" || task.status === "probing" || task.status === "queued" || task.status === "paused";
    }
    if (appState.filter === "done") {
      return task.status === "completed";
    }
    return true;
  });

  const box = $("taskListContainer");
  if (!filtered.length) {
    box.innerHTML = `
      <div class="card empty-card">
        <div>${escapeHtml(tasks.length ? dict.emptyFiltered : dict.emptyTasks)}</div>
        ${!tasks.length ? `<div style="margin-top:12px;"><button type="button" class="btn btn-subtle" onclick="window.switchTab('extract')">${escapeHtml(dict.goExtractBtn)}</button></div>` : ""}
      </div>`;
    return;
  }

  box.innerHTML = "";
  for (const task of filtered) {
    const card = document.createElement("div");
    card.className = "task-card";

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
    } else if (task.status === "completed") {
      statusLabel = dict.statusCompleted;
      statusClass = "status-completed";
      barClass = "done";
    } else if (task.status === "error") {
      statusLabel = dict.statusError;
      statusClass = "status-error";
      barClass = "error";
    }

    const total = Number(task.totalBytes || 0);
    const dl = Number(task.downloadedBytes || 0);
    const pct = total > 0
      ? Math.min(100, Math.max(0, Math.round((dl * 100) / total)))
      : task.status === "completed" ? 100 : 0;

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
        <button type="button" class="btn btn-danger-soft btn-sm" data-act="delete" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnDelete)}</button>
      `;
    } else if (task.status === "paused") {
      actionsHtml = `
        <button type="button" class="btn btn-primary btn-sm" data-act="resume" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnResume)}</button>
        <button type="button" class="btn btn-danger-soft btn-sm" data-act="delete" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnDelete)}</button>
      `;
    } else if (task.status === "error") {
      actionsHtml = `
        <button type="button" class="btn btn-primary btn-sm" data-act="retry" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnRetry)}</button>
        <button type="button" class="btn btn-danger-soft btn-sm" data-act="delete" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnDelete)}</button>
      `;
    } else if (task.status === "completed") {
      actionsHtml = `
        <button type="button" class="btn btn-primary btn-sm" data-act="open" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnOpen)}</button>
        <button type="button" class="btn btn-subtle btn-sm" data-act="share" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnShare)}</button>
        <button type="button" class="btn btn-danger-soft btn-sm" data-act="delete" data-id="${escapeHtml(task.id)}">${escapeHtml(dict.btnDelete)}</button>
      `;
    }

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
        <span>
          ${task.status === "downloading" ? escapeHtml(formatSpeed(task.speedBps)) : ""}
          ${etaStr ? ` · ${escapeHtml(etaStr)}` : ""}
        </span>
      </div>

      <div class="progress-track">
        <div class="progress-fill ${barClass}" style="width:${pct}%"></div>
      </div>

      ${chunkHtml}

      ${task.publicDisplayPath && task.status === "completed" ? `<div class="task-path">📂 ${escapeHtml(task.publicDisplayPath)}</div>` : ""}
      ${task.error && task.status === "error" ? `<div class="task-error">${escapeHtml(task.error)}</div>` : ""}

      <div class="task-actions">${actionsHtml}</div>
    `;

    card.querySelectorAll("[data-act]").forEach((btn) => {
      btn.addEventListener("click", () => {
        const act = btn.dataset.act;
        const id = btn.dataset.id;
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

    box.appendChild(card);
  }
}

function resolveSnifferTarget(rawText) {
  const extracted = extractCleanUrl(rawText);
  if (extracted.cleanUrl) {
    return extracted;
  }
  const trimmed = String(rawText || "").trim();
  if (/^[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(\/.*)?$/.test(trimmed)) {
    return { cleanUrl: `https://${trimmed}`, hadNoise: false, fileName: "" };
  }
  return { cleanUrl: "", hadNoise: false, fileName: "" };
}

function renderSnifferInput() {
  const el = $("snifferUrlBox");
  const wrap = $("snifferCleanPreviewWrap");
  const urlEl = $("snifferCleanUrlText");
  if (!el || !wrap || !urlEl) return;
  const res = resolveSnifferTarget(el.value);
  appState.snifferCleanUrl = res.cleanUrl;
  if (res.cleanUrl && res.hadNoise) {
    urlEl.textContent = res.cleanUrl;
    wrap.classList.remove("hidden");
  } else {
    urlEl.textContent = "";
    wrap.classList.add("hidden");
  }
}

function deriveSniffedFileName(item) {
  if (item.customFileName && item.customFileName.trim()) {
    return item.customFileName.trim();
  }
  if (item.suggestedName && item.suggestedName.trim()) {
    return item.suggestedName.trim().replace(/[\\/:*?"<>|\r\n]+/g, "_");
  }
  const urlInfo = extractCleanUrl(item.url);
  if (item.title && item.title.trim()) {
    const cleanTitle = item.title
      .trim()
      .replace(/[\\/:*?"<>|#\r\n]+/g, " ")
      .replace(/\s+/g, " ")
      .trim()
      .slice(0, 48);
    if (cleanTitle) {
      let ext = ".mp4";
      const lowerUrl = String(item.url || "").toLowerCase();
      const lowerCt = String(item.contentType || "").toLowerCase();
      if (item.mediaType === "audio") {
        ext = ".m4a";
      } else if (item.mediaType === "image") {
        if (lowerCt.includes("webp") || lowerUrl.includes(".webp")) ext = ".webp";
        else if (lowerCt.includes("png") || lowerUrl.includes(".png")) ext = ".png";
        else ext = ".jpg";
      } else if (item.mediaType === "file" && urlInfo.fileName && urlInfo.fileName.includes(".")) {
        ext = `.${urlInfo.fileName.split(".").pop()}`;
      } else if (urlInfo.fileName && /\.(mp4|mkv|mov|webm|m3u8|mp3|m4a|wav|flac|apk|zip|pdf)$/i.test(urlInfo.fileName)) {
        ext = `.${urlInfo.fileName.split(".").pop().toLowerCase()}`;
      }
      return `${cleanTitle}${ext}`;
    }
  }
  return urlInfo.fileName || "captured_media.mp4";
}

function getSniffMediaLabel(dict, mediaType) {
  if (mediaType === "audio") return dict.mediaLabelAudio;
  if (mediaType === "image") return dict.mediaLabelImage;
  if (mediaType === "file") return dict.mediaLabelFile;
  return dict.mediaLabelVideo;
}

function buildSniffMetaPillsHtml(item, dict) {
  const pills = [];
  pills.push(`<span class="meta-pill">${escapeHtml(getSniffMediaLabel(dict, item.mediaType))}</span>`);
  if (item.source === "DOM") {
    pills.push(`<span class="meta-pill dom-pill">${escapeHtml(dict.badgeDomPrimary)}</span>`);
  }
  if (item.sizeBytes && Number(item.sizeBytes) > 0) {
    pills.push(`<span class="meta-pill" data-sniff-size="1">${escapeHtml(formatBytes(Number(item.sizeBytes)))}</span>`);
  } else {
    pills.push(`<span class="meta-pill" data-sniff-size="1">${escapeHtml(dict.sizeProbing)}</span>`);
  }
  if (item.width > 0 && item.height > 0) {
    pills.push(`<span class="meta-pill">${item.width}×${item.height}</span>`);
  }
  if (item.durationSec > 0) {
    pills.push(`<span class="meta-pill">${escapeHtml(formatEta(item.durationSec))}</span>`);
  }
  return pills.join("");
}

function matchesSniffFilter(item, filterType) {
  if (filterType === "all") return true;
  return (item.mediaType || "video") === filterType;
}

function sniffSortPriority(item) {
  const mt = item.mediaType || "video";
  if (mt === "video") return item.source === "DOM" ? 5 : 4;
  if (mt === "image") return 3;
  if (mt === "audio") return 2;
  return 1;
}

function updateActivePreviewMetaText(metaBarEl, item, videoEl, dict) {
  if (!metaBarEl) return;
  const parts = [];
  if (item.sizeBytes > 0) parts.push(formatBytes(item.sizeBytes));
  const w = videoEl && videoEl.videoWidth ? videoEl.videoWidth : item.width;
  const h = videoEl && videoEl.videoHeight ? videoEl.videoHeight : item.height;
  if (w > 0 && h > 0) parts.push(`${w}×${h}`);
  const dur = videoEl && isFinite(videoEl.duration) && videoEl.duration > 0 ? videoEl.duration : item.durationSec;
  if (dur > 0) parts.push(formatEta(dur));
  if (item.contentType) parts.push(item.contentType);
  metaBarEl.textContent = parts.length ? parts.join(" · ") : dict.previewLoading;
}

function getFilteredSniffed() {
  return (appState.sniffed || [])
    .filter((item) => matchesSniffFilter(item, appState.sniffTypeFilter))
    .slice()
    .sort((a, b) => {
      const pa = sniffSortPriority(a);
      const pb = sniffSortPriority(b);
      if (pa !== pb) return pb - pa;
      return (b.capturedAt || 0) - (a.capturedAt || 0);
    });
}

function updateSniffBatchBar(filtered) {
  const dict = t();
  const bar = $("sniffBatchBar");
  const selectAllBtn = $("sniffSelectAllBtn");
  const batchBtn = $("sniffBatchDownloadBtn");
  if (!bar || !selectAllBtn || !batchBtn) return;

  if (!filtered || !filtered.length) {
    bar.classList.add("hidden");
    return;
  }

  bar.classList.remove("hidden");
  let selectedInFilter = 0;
  for (const item of filtered) {
    if (appState.selectedSniffUrls.has(item.url)) {
      selectedInFilter++;
    }
  }
  const allSelected = selectedInFilter > 0 && selectedInFilter === filtered.length;
  selectAllBtn.textContent = allSelected ? dict.sniffDeselectAll : dict.sniffSelectAll;

  const totalSelected = appState.selectedSniffUrls.size;
  batchBtn.textContent = dict.sniffBatchDownload(totalSelected);
  batchBtn.disabled = totalSelected === 0;
}

function renderSniffed() {
  const dict = t();
  const list = appState.sniffed || [];
  const badge = $("navSnifferBadge");
  if (list.length > 0) {
    badge.textContent = String(list.length);
    badge.classList.remove("hidden");
  } else {
    badge.classList.add("hidden");
  }

  document.querySelectorAll("#sniffTypeFilterSwitch .seg-btn").forEach((b) => {
    b.classList.toggle("active", b.dataset.type === appState.sniffTypeFilter);
  });

  const box = $("sniffedListContainer");
  if (!list.length) {
    updateSniffBatchBar([]);
    box.innerHTML = `<div class="card empty-card">${escapeHtml(dict.emptySniffed)}</div>`;
    return;
  }

  const filtered = getFilteredSniffed();
  updateSniffBatchBar(filtered);

  if (!filtered.length) {
    box.innerHTML = `<div class="card empty-card">${escapeHtml(dict.emptySniffedFiltered)}</div>`;
    return;
  }

  box.innerHTML = "";
  for (const item of filtered) {
    const fileName = deriveSniffedFileName(item);
    const isPreviewOpen = appState.activePreviewUrl === item.url;
    const isChecked = appState.selectedSniffUrls.has(item.url);
    const previewSrc = item.previewUrl || item.url;
    const mt = item.mediaType || "video";
    const card = document.createElement("div");
    card.className = "task-card";
    card.dataset.sniffUrl = item.url;
    let host = "";
    try {
      host = new URL(item.url).hostname;
    } catch {}

    const thumbHtml =
      mt === "image" && !isPreviewOpen
        ? `<div class="sniff-thumb-strip" data-sniff-thumb="1"><img class="sniff-inline-thumb" loading="lazy" src="${escapeHtml(previewSrc)}" alt="thumb" /></div>`
        : "";

    let previewDrawerHtml = "";
    if (isPreviewOpen) {
      let playerHtml = "";
      if (mt === "audio") {
        playerHtml = `<audio class="mini-preview-audio" controls autoplay preload="metadata" src="${escapeHtml(previewSrc)}"></audio>`;
      } else if (mt === "image") {
        playerHtml = `<img class="mini-preview-img" src="${escapeHtml(previewSrc)}" alt="preview" />`;
      } else if (mt === "file") {
        playerHtml = `<div class="preview-file-box">${escapeHtml(dict.previewFileLabel)}: ${escapeHtml(fileName)}${item.contentType ? ` (${escapeHtml(item.contentType)})` : ""}</div>`;
      } else {
        playerHtml = `<video class="mini-preview-video" controls playsinline autoplay preload="metadata" src="${escapeHtml(previewSrc)}"></video>`;
      }
      previewDrawerHtml = `
        <div class="preview-drawer">
          ${playerHtml}
          <div class="preview-meta-bar" data-preview-meta="1">${escapeHtml(dict.previewLoading)}</div>
          <div class="preview-rename-row">
            <input type="text" class="preview-rename-input" data-preview-name="1" value="${escapeHtml(fileName)}" placeholder="${escapeHtml(dict.previewRenamePlaceholder)}" />
            <button type="button" class="btn btn-primary btn-sm" data-sniff-act="dl-now">${escapeHtml(dict.btnPreviewDownloadNow)}</button>
          </div>
        </div>
      `;
    }

    card.innerHTML = `
      <div class="task-top">
        <div class="task-title-wrap">
          <label class="sniff-check-wrap">
            <input type="checkbox" class="sniff-checkbox" data-sniff-check="1" ${isChecked ? "checked" : ""} />
          </label>
          <span class="ext-badge" data-sniff-ext="1">${escapeHtml(inferExtBadge(fileName))}</span>
          <span class="task-name" data-sniff-title="1">${escapeHtml(fileName)}</span>
        </div>
        <span class="chip-soft">${escapeHtml(host)}</span>
      </div>
      <div class="sniff-meta-row" data-sniff-meta="1">${buildSniffMetaPillsHtml(item, dict)}</div>
      ${thumbHtml}
      <div class="sniff-url">${escapeHtml(item.url)}</div>
      <div class="task-actions">
        <button type="button" class="btn ${isPreviewOpen ? "btn-preview-active" : "btn-subtle"} btn-sm" data-sniff-act="preview">${escapeHtml(isPreviewOpen ? dict.btnPreviewClose : dict.btnPreviewOpen)}</button>
        <button type="button" class="btn btn-primary btn-sm" data-sniff-act="dl">${escapeHtml(dict.btnSniffDownload(appState.selectedThreads))}</button>
        <button type="button" class="btn btn-subtle btn-sm" data-sniff-act="fill">${escapeHtml(dict.btnSniffFill)}</button>
      </div>
      ${previewDrawerHtml}
    `;

    card.querySelector('[data-sniff-check="1"]').addEventListener("change", (ev) => {
      if (ev.target.checked) {
        appState.selectedSniffUrls.add(item.url);
      } else {
        appState.selectedSniffUrls.delete(item.url);
      }
      updateSniffBatchBar(getFilteredSniffed());
    });

    const thumbEl = card.querySelector('[data-sniff-thumb="1"]');
    if (thumbEl) {
      thumbEl.addEventListener("click", () => {
        appState.activePreviewUrl = item.url;
        renderSniffed();
      });
    }

    card.querySelector('[data-sniff-act="preview"]').addEventListener("click", () => {
      appState.activePreviewUrl = isPreviewOpen ? null : item.url;
      renderSniffed();
    });

    card.querySelector('[data-sniff-act="dl"]').addEventListener("click", () => {
      triggerNativeDownload({
        url: item.url,
        fileName: deriveSniffedFileName(item),
        threads: appState.selectedThreads,
        referer: item.referer || "",
        cookie: item.cookie || "",
        uaMode: item.uaMode || appState.settings.defaultUaMode || "desktop",
      });
    });

    card.querySelector('[data-sniff-act="fill"]').addEventListener("click", () => {
      const currentName = deriveSniffedFileName(item);
      $("rawInput").value = item.url;
      $("fileNameInput").value = currentName;
      $("fileNameInput").dataset.userEdited = "1";
      $("refererInput").value = item.referer || "";
      $("cookieInput").value = item.cookie || "";
      if (item.referer || item.cookie) {
        appState.headersExpanded = true;
        $("headersDrawer").classList.remove("hidden");
      }
      renderExtractor();
      window.switchTab("extract");
    });

    if (isPreviewOpen) {
      const metaBarEl = card.querySelector('[data-preview-meta="1"]');
      const mediaEl = card.querySelector("video, audio");
      const imgEl = card.querySelector("img.mini-preview-img");
      const nameInput = card.querySelector('[data-preview-name="1"]');
      updateActivePreviewMetaText(metaBarEl, item, mediaEl, dict);

      if (mediaEl) {
        mediaEl.addEventListener("loadedmetadata", () => {
          if (mediaEl.videoWidth > 0) {
            item.width = mediaEl.videoWidth;
            item.height = mediaEl.videoHeight;
          }
          if (isFinite(mediaEl.duration) && mediaEl.duration > 0) {
            item.durationSec = mediaEl.duration;
          }
          updateActivePreviewMetaText(metaBarEl, item, mediaEl, dict);
        });
        mediaEl.addEventListener("error", () => {
          if (metaBarEl) {
            metaBarEl.textContent = dict.previewLoadError;
          }
        });
      }

      if (imgEl) {
        imgEl.addEventListener("load", () => {
          if (imgEl.naturalWidth > 0) {
            item.width = imgEl.naturalWidth;
            item.height = imgEl.naturalHeight;
          }
          updateActivePreviewMetaText(metaBarEl, item, null, dict);
        });
        imgEl.addEventListener("error", () => {
          if (metaBarEl) {
            metaBarEl.textContent = dict.previewLoadError;
          }
        });
      }

      if (nameInput) {
        nameInput.addEventListener("input", () => {
          item.customFileName = nameInput.value;
          const titleEl = card.querySelector('[data-sniff-title="1"]');
          const extEl = card.querySelector('[data-sniff-ext="1"]');
          const nextName = deriveSniffedFileName(item);
          if (titleEl) titleEl.textContent = nextName;
          if (extEl) extEl.textContent = inferExtBadge(nextName);
        });
      }

      const dlNowBtn = card.querySelector('[data-sniff-act="dl-now"]');
      if (dlNowBtn) {
        dlNowBtn.addEventListener("click", () => {
          triggerNativeDownload({
            url: item.url,
            fileName: deriveSniffedFileName(item),
            threads: appState.selectedThreads,
            referer: item.referer || "",
            cookie: item.cookie || "",
            uaMode: item.uaMode || appState.settings.defaultUaMode || "desktop",
          });
        });
      }
    }

    box.appendChild(card);
  }
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
}

function triggerNativeDownload({ url, fileName, threads, referer, cookie, uaMode }) {
  const dict = t();
  if (!url) {
    showToast(dict.toastNeedUrl);
    return;
  }
  if (!window.JoelNative) {
    showToast(dict.toastBrowserPreview);
    return;
  }
  const resRaw = window.JoelNative.startDownload(
    url,
    fileName || "",
    threads || appState.selectedThreads,
    referer || "",
    cookie || "",
    uaMode || appState.settings.defaultUaMode || "desktop"
  );
  try {
    const res = JSON.parse(resRaw || "{}");
    if (res.ok) {
      showToast(dict.toastTaskStarted(fileName || url, threads));
      window.switchTab("downloads");
      return;
    }
    showToast(res.error || "Failed");
  } catch {
    window.switchTab("downloads");
  }
}

function saveSettingsPatch(patch) {
  appState.settings = { ...appState.settings, ...patch };
  if (patch.defaultThreads) {
    appState.selectedThreads = patch.defaultThreads;
    document.querySelectorAll("#threadSelector .thread-pill").forEach((b) => {
      b.classList.toggle("active", Number(b.dataset.threads) === Number(patch.defaultThreads));
    });
  }
  if (window.JoelNative) {
    window.JoelNative.saveSettings(JSON.stringify(patch));
  }
  renderSettings();
}

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
      document.querySelectorAll("#threadSelector .thread-pill").forEach((b) => {
        b.classList.toggle("active", Number(b.dataset.threads) === Number(appState.selectedThreads));
      });
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
  $("rawInput").value = text;
  delete $("fileNameInput").dataset.userEdited;
  renderExtractor();
  window.switchTab("extract");
  showToast(t().toastSharedLoaded);
};

window.onSniffedMedia = function onSniffedMedia(item) {
  if (!item || !item.url) return;
  if (appState.sniffed.some((x) => x.url === item.url)) return;
  appState.sniffed.unshift(item);
  renderSniffed();
};

window.onSniffedMediaUpdate = function onSniffedMediaUpdate(payload) {
  if (!payload || !payload.url) return;
  const idx = appState.sniffed.findIndex((x) => x.url === payload.url);
  if (idx < 0) return;

  if (payload.invalid) {
    appState.sniffed.splice(idx, 1);
    appState.selectedSniffUrls.delete(payload.url);
    if (appState.activePreviewUrl === payload.url) {
      appState.activePreviewUrl = null;
    }
    renderSniffed();
    return;
  }

  const prev = appState.sniffed[idx];
  const merged = { ...prev, ...payload, customFileName: prev.customFileName };
  appState.sniffed[idx] = merged;

  // Non-destructive update if this card currently has an active inline preview player
  if (appState.activePreviewUrl === payload.url) {
    const cards = document.querySelectorAll("#sniffedListContainer .task-card");
    for (const card of cards) {
      if (card.dataset.sniffUrl === payload.url) {
        const dict = t();
        const metaRow = card.querySelector('[data-sniff-meta="1"]');
        if (metaRow) {
          metaRow.innerHTML = buildSniffMetaPillsHtml(merged, dict);
        }
        const derivedName = deriveSniffedFileName(merged);
        const titleEl = card.querySelector('[data-sniff-title="1"]');
        const extEl = card.querySelector('[data-sniff-ext="1"]');
        if (titleEl) titleEl.textContent = derivedName;
        if (extEl) extEl.textContent = inferExtBadge(derivedName);
        const metaBar = card.querySelector('[data-preview-meta="1"]');
        const mediaEl = card.querySelector("video, audio");
        updateActivePreviewMetaText(metaBar, merged, mediaEl, dict);
        return;
      }
    }
  }

  renderSniffed();
};

function bindEvents() {
  // Bottom Nav
  document.querySelectorAll(".nav-btn").forEach((btn) => {
    btn.addEventListener("click", () => window.switchTab(btn.dataset.tab));
  });

  // Top EN / 中文 Toggle
  document.querySelectorAll(".lang-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      const nextLang = btn.dataset.lang === "en" ? "en" : "zh";
      appState.lang = nextLang;
      saveSettingsPatch({ lang: nextLang });
      applyI18n();
    });
  });

  // Smart Paste Input
  $("rawInput").addEventListener("input", () => {
    renderExtractor();
  });

  $("fileNameInput").addEventListener("input", () => {
    $("fileNameInput").dataset.userEdited = "1";
  });

  $("pasteClipBtn").addEventListener("click", async () => {
    let text = "";
    if (window.JoelNative) {
      text = window.JoelNative.getClipboardText() || "";
    } else if (navigator.clipboard?.readText) {
      try {
        text = await navigator.clipboard.readText();
      } catch {}
    }
    if (!text.trim()) {
      showToast(t().toastClipboardEmpty);
      return;
    }
    $("rawInput").value = text;
    delete $("fileNameInput").dataset.userEdited;
    renderExtractor();
  });

  $("clearInputBtn").addEventListener("click", () => {
    $("rawInput").value = "";
    $("refererInput").value = "";
    $("cookieInput").value = "";
    delete $("fileNameInput").dataset.userEdited;
    renderExtractor();
  });

  $("copyCleanUrlBtn").addEventListener("click", () => {
    if (!appState.extractedUrl) return;
    if (window.JoelNative) {
      window.JoelNative.setClipboardText(appState.extractedUrl);
    } else if (navigator.clipboard?.writeText) {
      navigator.clipboard.writeText(appState.extractedUrl).catch(() => {});
    }
    showToast(t().toastCopied);
  });

  $("toggleHeadersBtn").addEventListener("click", () => {
    appState.headersExpanded = !appState.headersExpanded;
    $("headersDrawer").classList.toggle("hidden", !appState.headersExpanded);
    syncText(
      "toggleHeadersLabel",
      appState.headersExpanded ? t().toggleHeadersShow : t().toggleHeadersHide
    );
  });

  document.querySelectorAll("#threadSelector .thread-pill").forEach((pill) => {
    pill.addEventListener("click", () => {
      const n = Number(pill.dataset.threads);
      appState.selectedThreads = n;
      document.querySelectorAll("#threadSelector .thread-pill").forEach((p) => {
        p.classList.toggle("active", p === pill);
      });
    });
  });

  $("startDownloadBtn").addEventListener("click", () => {
    triggerNativeDownload({
      url: appState.extractedUrl,
      fileName: $("fileNameInput").value.trim(),
      threads: appState.selectedThreads,
      referer: $("refererInput").value.trim(),
      cookie: $("cookieInput").value.trim(),
    });
  });

  $("openInSnifferBtn").addEventListener("click", () => {
    const targetUrl = appState.extractedUrl;
    if (!targetUrl) {
      showToast(t().toastNeedUrl);
      return;
    }
    $("snifferUrlBox").value = targetUrl;
    renderSnifferInput();
    if (window.JoelNative) {
      window.JoelNative.openBrowserSniffer(targetUrl);
    } else {
      showToast(t().toastBrowserPreview);
    }
  });

  // Downloads Tab Filter & Clear
  document.querySelectorAll("#taskFilterSwitch .seg-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      appState.filter = btn.dataset.filter;
      document.querySelectorAll("#taskFilterSwitch .seg-btn").forEach((b) => {
        b.classList.toggle("active", b === btn);
      });
      renderTasks();
    });
  });

  $("clearDoneBtn").addEventListener("click", () => {
    if (window.JoelNative) {
      window.JoelNative.clearCompleted();
    } else {
      appState.tasks = appState.tasks.filter((x) => x.status !== "completed");
      renderTasks();
    }
  });

  // Sniffer Tab
  $("snifferUrlBox").addEventListener("input", () => {
    renderSnifferInput();
  });

  $("snifferClearInputBtn").addEventListener("click", () => {
    $("snifferUrlBox").value = "";
    renderSnifferInput();
  });

  $("snifferPasteGoBtn").addEventListener("click", async () => {
    let text = "";
    if (window.JoelNative) {
      text = window.JoelNative.getClipboardText() || "";
    } else if (navigator.clipboard?.readText) {
      try {
        text = await navigator.clipboard.readText();
      } catch {}
    }
    if (!text.trim()) {
      showToast(t().toastClipboardEmpty);
      return;
    }
    $("snifferUrlBox").value = text;
    renderSnifferInput();
    const res = resolveSnifferTarget(text);
    if (!res.cleanUrl) {
      showToast(t().toastSniffNoUrl);
      return;
    }
    showToast(res.hadNoise ? t().toastExtractedSniff : t().launchSnifferBtn);
    if (window.JoelNative) {
      window.JoelNative.openBrowserSniffer(res.cleanUrl);
    } else {
      showToast(t().toastBrowserPreview);
    }
  });

  $("launchSnifferBtn").addEventListener("click", () => {
    const raw = $("snifferUrlBox").value.trim();
    if (!raw) {
      showToast(t().toastNeedUrl);
      return;
    }
    const res = resolveSnifferTarget(raw);
    if (!res.cleanUrl) {
      showToast(t().toastSniffNoUrl);
      return;
    }
    if (res.hadNoise) {
      showToast(t().toastExtractedSniff);
    }
    if (window.JoelNative) {
      window.JoelNative.openBrowserSniffer(res.cleanUrl);
    } else {
      showToast(t().toastBrowserPreview);
    }
  });

  document.querySelectorAll("#sniffTypeFilterSwitch .seg-btn").forEach((btn) => {
    btn.addEventListener("click", () => {
      appState.sniffTypeFilter = btn.dataset.type || "all";
      renderSniffed();
    });
  });

  $("clearSniffedBtn").addEventListener("click", () => {
    appState.sniffed = [];
    appState.selectedSniffUrls.clear();
    appState.activePreviewUrl = null;
    renderSniffed();
  });

  $("sniffSelectAllBtn").addEventListener("click", () => {
    const filtered = getFilteredSniffed();
    if (!filtered.length) return;
    const allSelected = filtered.every((item) => appState.selectedSniffUrls.has(item.url));
    if (allSelected) {
      for (const item of filtered) {
        appState.selectedSniffUrls.delete(item.url);
      }
    } else {
      for (const item of filtered) {
        appState.selectedSniffUrls.add(item.url);
      }
    }
    renderSniffed();
  });

  $("sniffBatchDownloadBtn").addEventListener("click", () => {
    const dict = t();
    const selectedItems = (appState.sniffed || []).filter((item) =>
      appState.selectedSniffUrls.has(item.url)
    );
    if (!selectedItems.length) {
      showToast(dict.toastSelectOneFirst);
      return;
    }
    if (!window.JoelNative) {
      showToast(dict.toastBrowserPreview);
      return;
    }
    const nameCounts = new Map();
    let started = 0;
    for (const item of selectedItems) {
      let fn = deriveSniffedFileName(item);
      const count = (nameCounts.get(fn) || 0) + 1;
      nameCounts.set(fn, count);
      if (count > 1) {
        const dot = fn.lastIndexOf(".");
        fn = dot > 0 ? `${fn.slice(0, dot)}_${count}${fn.slice(dot)}` : `${fn}_${count}`;
      }
      const resRaw = window.JoelNative.startDownload(
        item.url,
        fn,
        appState.selectedThreads,
        item.referer || "",
        item.cookie || "",
        item.uaMode || appState.settings.defaultUaMode || "desktop"
      );
      try {
        const parsed = JSON.parse(resRaw || "{}");
        if (parsed.ok) started++;
      } catch {
        started++;
      }
    }
    appState.selectedSniffUrls.clear();
    renderSniffed();
    if (started > 0) {
      showToast(dict.toastBatchStarted(started));
      window.switchTab("downloads");
    }
  });

  // Settings Tab
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
}

// Init
bindEvents();
applyI18n();
if (window.JoelNative) {
  try {
    const initial = JSON.parse(window.JoelNative.getStateJson() || "{}");
    window.onNativeStateUpdate(initial);
  } catch {}
}
