# Joel Phone Pro

Mobile-first Android multi-threaded downloader and smart media sniffer built in pure Android SDK + WebView UI (`中文 | EN` bilingual).

## Features

- **True Multi-Threaded HTTP `Range` Engine**: Splits large files across `4`, `8`, `16`, or `32` concurrent worker threads using NIO `FileChannel.write(ByteBuffer, position)` with automatic single-stream fallback when servers do not support byte ranges.
- **Pause, Resume & Chunk Matrix**: Preserves per-chunk downloaded byte offsets cleanly across pauses/app restarts and renders live progress for every concurrent chunk.
- **Smart Link Extractor & System Share Target**: Automatically strips noise from messy Chinese/English share text and registers as an Android `ACTION_SEND` share receiver.
- **Built-in Web Media Sniffer**: Opens landing/video pages in a Desktop-UA WebView and intercepts `.mp4`, `.mkv`, `.mov`, `.webm`, `.mp3`, `.m4a`, `.wav`, `.flac`, `.apk`, `.zip`, `.pdf` streams together with active `Referer` and `Cookie` headers.
- **Public Downloads Export**: Saves finished files directly to `Download/JoelPhonePro/` via `MediaStore.Downloads` (Android 10+) or `MediaScannerConnection` (Android 7–9) with one-tap Open and Share.

## Architecture

- `app/src/main/java/com/joel/phonepro/DownloadTask.java`: Task state model & JSON serializer.
- `app/src/main/java/com/joel/phonepro/MultiThreadDownloader.java`: HTTP `Range` prober, `FileChannel` positional worker pool, speed/ETA sampler, and `MediaStore` exporter.
- `app/src/main/java/com/joel/phonepro/MainActivity.java`: Hybrid WebView shell, `JoelNative` JavaScript bridge, `ACTION_SEND` receiver, and WebView media sniffer overlay.
- `app/src/main/assets/`: Mobile-first White & Blue UI (`index.html`, `styles.css`, `app.js`).

## Download

Get the pre-built APK from [GitHub Releases](https://github.com/joelting02-sys/JoelPhonePro/releases) or `apk/JoelPhonePro-v1.2.0.apk`.

## Build

```bash
gradle --no-daemon clean assembleDebug
```

A reference GitHub Actions workflow template is provided at `docs/ci-build-apk.yml` (copy to `.github/workflows/build-apk.yml` with a token that includes the `workflow` scope to enable automatic CI builds).
