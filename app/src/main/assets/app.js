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
    snifferTitle: "内置网页媒体与资源嗅探",
    snifferDesc: "遇到网盘落地页或网页内嵌视频时，在内置浏览器打开它，自动捕获真实媒体直链与当前 Cookie / Referer 并多线程拉取。",
    launchSnifferBtn: "🌐 打开嗅探",
    sniffedListTitle: "已捕获资源",
    clearSniffedBtn: "清空列表",
    emptySniffed: "暂无捕获到的媒体直链。打开任意视频或资源网页播放后，捕获的资源会显示在这里。",
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
    snifferTitle: "Built-in Web Media & File Sniffer",
    snifferDesc: "Open video or landing pages in the built-in browser to capture real media/file URLs together with active Cookie & Referer headers.",
    launchSnifferBtn: "🌐 Open Sniffer",
    sniffedListTitle: "Captured Resources",
    clearSniffedBtn: "Clear",
    emptySniffed: "No media streams captured yet. Open a webpage above and play media to detect direct links.",
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
  selectedThreads: 16,
  headersExpanded: false,
  extractedUrl: "",
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

  syncText("snifferTitle", dict.snifferTitle);
  syncText("snifferDesc", dict.snifferDesc);
  syncText("launchSnifferBtn", dict.launchSnifferBtn);
  syncText("sniffedListTitle", dict.sniffedListTitle);
  syncText("clearSniffedBtn", dict.clearSniffedBtn);

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

  const box = $("sniffedListContainer");
  if (!list.length) {
    box.innerHTML = `<div class="card empty-card">${escapeHtml(dict.emptySniffed)}</div>`;
    return;
  }

  box.innerHTML = "";
  for (const item of list) {
    const info = extractCleanUrl(item.url);
    const fileName = info.fileName || "captured_media.mp4";
    const card = document.createElement("div");
    card.className = "task-card";
    let host = "";
    try {
      host = new URL(item.url).hostname;
    } catch {}

    card.innerHTML = `
      <div class="task-top">
        <div class="task-title-wrap">
          <span class="ext-badge">${escapeHtml(inferExtBadge(fileName))}</span>
          <span class="task-name">${escapeHtml(fileName)}</span>
        </div>
        <span class="chip-soft">${escapeHtml(host)}</span>
      </div>
      <div class="sniff-url">${escapeHtml(item.url)}</div>
      <div class="task-actions">
        <button type="button" class="btn btn-primary btn-sm" data-sniff-act="dl">${escapeHtml(dict.btnSniffDownload(appState.selectedThreads))}</button>
        <button type="button" class="btn btn-subtle btn-sm" data-sniff-act="fill">${escapeHtml(dict.btnSniffFill)}</button>
      </div>
    `;

    card.querySelector('[data-sniff-act="dl"]').addEventListener("click", () => {
      triggerNativeDownload({
        url: item.url,
        fileName,
        threads: appState.selectedThreads,
        referer: item.referer || "",
        cookie: item.cookie || "",
      });
    });

    card.querySelector('[data-sniff-act="fill"]').addEventListener("click", () => {
      $("rawInput").value = item.url;
      $("fileNameInput").value = fileName;
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

function triggerNativeDownload({ url, fileName, threads, referer, cookie }) {
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
    appState.settings.defaultUaMode || "desktop"
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
  $("launchSnifferBtn").addEventListener("click", () => {
    const raw = $("snifferUrlBox").value.trim();
    if (!raw) {
      showToast(t().toastNeedUrl);
      return;
    }
    if (window.JoelNative) {
      window.JoelNative.openBrowserSniffer(raw);
    } else {
      showToast(t().toastBrowserPreview);
    }
  });

  $("clearSniffedBtn").addEventListener("click", () => {
    appState.sniffed = [];
    renderSniffed();
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
