# MetaClean — Native Android Image Metadata Privacy Toolkit

MetaClean is an offline-first image metadata inspection and personal privacy utility for Android, built natively using **Kotlin**, **Jetpack Compose**, **Material 3**, and **Clean Architecture**.

---

## 🌟 Key Features

### 1. 🛡️ 100% Offline & Private
- Zero external network calls.
- Zero analytics, tracking SDKs, or account requirements.
- Strictly on-device processing of image copies — the original file is **never** overwritten.

### 2. 🔍 Deep Metadata Inspector
- **File Information**: Name, MIME type, file size, dimensions, megapixels, orientation, and container format.
- **EXIF Hardware & Capture**: Camera Make/Model, Lens info, ISO, Aperture, Shutter Speed, Focal length, Flash, White Balance, Metering mode, and embedded thumbnails.
- **GPS Coordinates**: Exact Latitude, Longitude, Altitude, and formatted geographical coordinate strings with sensitive privacy warnings.
- **IPTC & Authorship**: Creator, Copyright notices, Captions, and Contact information.
- **Adobe XMP**: Inspection of embedded XMP packets, namespaces (Dublin Core, Photoshop, Camera Raw), and properties.
- **PNG Chunks Breakdown**: Detailed inspection of critical (IHDR, PLTE, IDAT, IEND) and auxiliary chunks (iCCP, sRGB, gAMA, tEXt, zTXt, iTXt, pHYs, eXIf).
- **ICC Color Profile**: Profile names, color spaces, and profile sizes.
- **Content Credentials / Provenance**: Detection of C2PA manifests, JUMBF boxes, and AI generation tags (Midjourney, DALL-E, Stable Diffusion, Adobe Firefly) as **read-only informational signals**.
- **Technical Hashes & Hex Viewer**: Streaming calculation of SHA-256, SHA-1, MD5, and CRC32 with 256-byte Hexadecimal + ASCII memory dump.
- **Raw JSON Export**: One-tap export or clipboard copy of full metadata JSON.

### 3. 🧹 Privacy-Focused Cleaning Pipeline
- **Lossless PNG Cleaning**: Filters textual (tEXt/zTXt/iTXt) and eXIf chunks while preserving bit-for-bit raw compressed image data (IDAT).
- **EXIF Scrubbing**: Precision removal of GPS coordinates, device serials, camera make/model, timestamps, author names, and embedded thumbnails.
- **WebP & Bitmap Fallbacks**: Clean processing with strict dimension preservation.
- **Configurable Scrubbing**: Custom options dialog to selectively scrub GPS, Camera info, Timestamps, Author fields, Thumbnails, or XMP.

### 4. ✅ Before & After Verification Audit
- Side-by-side visual and quantitative comparison of Original vs. Cleaned copies.
- Exact audit breakdown:
  - **Removed Fields**: Green checkmarked list of stripped personal tags.
  - **Retained Fields**: Confirmation of preserved resolution, dimensions, and color profiles.
  - **Read-Only Provenance**: Unaltered AI/C2PA signals.
  - **Footprint Delta**: Size reduction in bytes and percentage.

### 5. 📦 Batch Processing & Export System
- Handles batches of 10, 20, 50+ images smoothly without memory leaks.
- Real queue with thumbnail previews (via Coil), individual progress indicators, retry, and cancellation.
- Export as single image, batch to directory via Storage Access Framework (SAF), or **ZIP archive compression**.
- Direct sharing via Android Sharesheet (`FileProvider`).
- Inbound share support: Share images directly to MetaClean from Gallery, Camera, or Files.

### 6. 🗄️ Room Processing History & Custom Settings
- Room-backed local processing history logging dates, file counts, success/failure counts, and bytes saved.
- Theme customization: Dark-first interface, Light mode, or follow Android system.
- Output filename suffix customization (`_clean`).
- Automatic temporary cache purge options.

---

## 🏗️ Architecture & Tech Stack

```
com.metaclean.app/
├── core/
│   ├── common/           # Dispatchers, results, constants
│   ├── file/             # FileUtils, HashUtils, HexUtils, ZipUtils
│   ├── image/            # FormatDetector, MetadataExtractor, PrivacyCleaner
│   └── ui/
│       ├── components/   # StatusBadge, MetadataSectionCard, HexViewerCard, CleanOptionsDialog
│       └── theme/        # Color, Theme, Type
├── data/
│   ├── local/            # AppDatabase, HistoryDao, HistoryEntity
│   ├── model/            # ImageMetadata, ExifData, PngData, TechnicalData, ProvenanceData
│   └── repository/       # MetadataRepositoryImpl, HistoryRepositoryImpl, SettingsRepositoryImpl
├── domain/
│   ├── model/            # MediaItem, CleanOptions, VerificationResult, HistoryRecord, ProcessingStatus
│   └── repository/       # MetadataRepository, HistoryRepository, SettingsRepository
├── presentation/
│   ├── batch/            # BatchScreen, BatchViewModel
│   ├── history/          # HistoryScreen, HistoryViewModel
│   ├── home/             # HomeScreen, HomeViewModel
│   ├── inspector/        # InspectorScreen, InspectorViewModel
│   ├── navigation/       # Screen, NavGraph, BottomNavBar
│   ├── settings/         # SettingsScreen, SettingsViewModel
│   └── verification/     # VerificationScreen, VerificationViewModel
├── di/                   # Hilt DatabaseModule & RepositoryModule
├── MainActivity.kt
└── MetaCleanApp.kt
```

- **Language**: Kotlin 2.2.10
- **UI Toolkit**: Jetpack Compose with Material 3 (BOM 2026.02.01)
- **Dependency Injection**: Google Hilt 2.60.1
- **Database**: AndroidX Room 2.8.5
- **Image Parsing**: AndroidX ExifInterface 1.3.7 + custom binary chunk parsers
- **Image Previews**: Coil Compose 2.7.0
- **Asynchronous**: Kotlin Coroutines & StateFlow
- **Build System**: Gradle 9.6.0 with AGP 9.5.0, KSP 2.2.10-2.0.2

---

## 🧪 Testing

The test suite covers:
- Magic-byte format detection for JPEG, PNG, WebP, GIF, TIFF, BMP, and unknown streams.
- Streaming checksum calculations (MD5, SHA-1, SHA-256, CRC32) against known test vectors.
- Hexadecimal preview formatting and memory dump generation.
- Privacy cleaning options presets and customization.
- Verification result metrics, dimension preservation checks, and footprint reduction calculations.

Run unit tests:
```bash
./gradlew testDebugUnitTest
```

Build debug APK:
```bash
./gradlew assembleDebug
```
The compiled APK is located at:
`app/build/outputs/apk/debug/app-debug.apk`
