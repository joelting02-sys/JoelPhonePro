# Joel Phone Pro

Mobile-first Android multi-threaded downloader and smart media sniffer built in pure Android SDK + WebView UI (`中文 | EN` bilingual).

## Features

- **3-Tab Thumb-Friendly Video Downloader UI (`窗口 | 下载中 | 已下载`)**: Browser tab stays cleanly separated from active multi-thread progress and finished downloads, with popular sites, link extractor, and `Direct URL` / `Settings` modals.
- **Single Floating Download Button & Active-Video Scoping**: Only a single draggable circular download button (`⬇`) floats on the active webpage — illuminating pink (`#FF3366`) when a playable active video stream is detected instead of flooding the drawer with unrelated feed cards or separate audio/image clutter.
- **Single-Video Complete Quality Picker & Multi-Thread Drawer**: Tapping the single `⬇` button opens a bottom-sheet quality picker (`1080P 超清` / `720P 高清` / `540P 标清` / etc.) with inline video preview, rename bar, prominent **`⚡ 多线程分片加速`** selector (`4 线程` | `8 线程` | `16 线程` | `32 线程`), and one-tap switch to `下载中`.
- **True Multi-Threaded HTTP `Range` Engine**: Splits large files across `4`, `8`, `16`, or `32` concurrent worker threads using NIO `FileChannel.write(ByteBuffer, position)` with cross-redirect cookie forwarding, `bytes=0-` fallback probing, and automatic single-stream fallback when servers do not support byte ranges.
- **Pause, Resume & Chunk Matrix**: Preserves per-chunk downloaded byte offsets cleanly across pauses/app restarts and renders live progress for every concurrent chunk.
- **Smart Link Extractor & System Share Target**: Automatically strips noise from messy Chinese/English share text and registers as an Android `ACTION_SEND` share receiver.
- **Public Downloads Export**: Saves finished files directly to `Download/JoelPhonePro/` via `MediaStore.Downloads` (Android 10+) or `MediaScannerConnection` (Android 7–9) with one-tap Open and Share.

## Architecture

- `app/src/main/java/com/joel/phonepro/DownloadTask.java`: Task state model & JSON serializer.
- `app/src/main/java/com/joel/phonepro/MultiThreadDownloader.java`: HTTP `Range` prober (`bytes=0-0` + `bytes=0-` fallback), `FileChannel` positional worker pool, speed/ETA sampler, and `MediaStore` exporter.
- `app/src/main/java/com/joel/phonepro/MainActivity.java`: Hybrid WebView shell, `JoelNative` & `JoelSniffHook` JavaScript bridges, draggable single Floating Download FAB, `ACTION_SEND` receiver, and built-in media sniffer browser overlay.
- `app/src/main/assets/`: Mobile-first White & Pink/Blue UI (`index.html`, `styles.css`, `app.js`).

## Download

Get the pre-built APK from [GitHub Releases](https://github.com/joelting02-sys/JoelPhonePro/releases) or [`apk/JoelPhonePro-v1.5.0.apk`](apk/JoelPhonePro-v1.5.0.apk) (`SHA-256`: `0a557c02758cca25aed4f675dc4644893468d1079347702874156b3c0bcbe278`).

## Build

```bash
gradle --no-daemon clean assembleDebug
```

A reference GitHub Actions workflow template is provided at `docs/ci-build-apk.yml` (copy to `.github/workflows/build-apk.yml` with a token that includes the `workflow` scope to enable automatic CI builds).
