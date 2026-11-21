# DIZOP Baseline Project & Architecture Audit

**Date:** 2026-11-21  
**Auditor:** DIZOP Engineering Team  
**Version:** 1.0.0-baseline  
**Target:** Production-Grade Universal Printing, AI Document Scanner & PDF Studio  

---

## 1. Current Architecture & Codebase Overview

- **Namespace / Package Name:** `com.dizop.print`
- **Application Class:** `com.dizop.print.DizopApp`
- **Build System:** Gradle 8.11.1 with Kotlin DSL (`build.gradle.kts`), AGP 8.7.3, Kotlin 2.0.21.
- **Java Toolchain:** OpenJDK 17 (`/usr/lib/jvm/java-1.17.0-openjdk-amd64`).
- **Target SDK:** 35 (Android 15) | **Min SDK:** 26 (Android 8.0) | **Compile SDK:** 35.
- **UI Toolkit:** 100% Jetpack Compose (BOM `2024.11.00`), Material 3, AndroidX Lifecycle 2.8.7.

---

## 2. Existing Modules & Source Inventory

| Component / File | Purpose | Working Status | Defects / Incomplete Areas |
| :--- | :--- | :--- | :--- |
| `MainActivity.kt` | Single Activity container initializing discovery, print engine, and router. | Working | Directly instantiates managers in `onCreate` instead of DI / ViewModel. |
| `UniversalPrinterDiscovery.kt` | mDNS / DNS-SD discovery (`_ipp._tcp`) + Wi-Fi Direct + USB scan. | Working | Hardcoded simulated ink telemetry values (`Cyan 85%...`); needs real IPP/SNMP attribute queries. Deprecated `resolveService`. |
| `UniversalPrintEngine.kt` | Routes print jobs to Raw JetDirect (9100), IPP (631), USB, or System Spooler. | Working | IPP socket currently streams raw bytes without proper IPP 2.0 binary chunked envelope validation. |
| `DualNetworkRouter.kt` | Binds process and sockets to local Wi-Fi when SIM Mobile Data is active. | Working | Successfully tested on device. Needs graceful fallback when Wi-Fi is temporarily disconnected. |
| `UsbPrinterManager.kt` | Android USB Host discovery and bulk endpoint transfer. | Working | Works for basic class 7 printers; lacks page description translation (e.g. JBIG/CAPT for LaserJet 1020/Canon LBP2900). |
| `WifiDirectPrinterManager.kt` | P2P Wi-Fi Direct peer discovery and connection negotiation. | Working | Peer discovery code handling verified; needs automatic service-discovery (`discoverServices`) support. |
| `SmartDocumentProcessor.kt` | Matrix-based shadow removal, document contrast whitening, 2-in-1 ID card layout. | Working | Operates on downsampled Bitmaps; needs OpenCV / ML Kit on-device edge detection for automatic paper polygon cropping. |
| `SmartPdfTools.kt` | PDF compression, page merging, digital signature overlay. | Working | Signature is purely visual overlay; needs proper UI canvas coordinates and distinction from cryptographically signed certificates. |
| `DynamicIslandPill.kt` | In-app floating pill showing real-time print status with spring physics. | Working | Smooth UI; needs integration with an Android Ongoing Notification for background print jobs. |
| `InAppUpdateManager.kt` | HTTPS JSON manifest check, DownloadManager integration, FileProvider APK install. | Working | Missing cryptographic SHA-256 APK checksum verification before firing package installer intent. |

---

## 3. High-Risk Problems & Technical Debt Identified

1. **Fake / Hardcoded Ink Readings:**
   - In `UniversalPrinterDiscovery.kt`, printers are populated with static `85%, 70%, 92%, 60%` ink levels.
   - *Requirement violation:* Rule Part 3 & Part 5.7 strictly forbid fake ink or toner readings. If ink level cannot be retrieved via IPP `marker-levels` or SNMP MIB, it must be marked as `Unknown` or `Not Reported`.
2. **Missing Real IPP 2.0 Protocol Envelope:**
   - `UniversalPrintEngine.kt` sends raw PDF streams to port 631 without wrapping them in RFC 8011 IPP binary request headers (`operation-id=0x0002 Print-Job`, attributes-charset, natural-language, document-format).
3. **Architecture Monolith in UI Layer:**
   - `MainDashboardScreen.kt` holds state for scanning, printers, updates, signatures, and ID cards in a single composable rather than an MVI / MVVM state machine with Room database caching.
4. **Offline OCR Missing:**
   - Text extraction mentioned in specifications is not yet wired to an on-device OCR engine (such as Google ML Kit Text Recognition on-device).
5. **Print Preview & Configuration Missing:**
   - No granular print preview screen (Copies, Duplex, Color/B&W, Page Ranges, Paper Size A4/A3/Letter, Custom Scaling).

---

## 4. Phase-by-Phase Upgrade Execution Plan

- **Phase 1: Architecture & Data Integrity**
  - Remove all fake/simulated telemetry. Implement `Unknown` fallback.
  - Setup Room database (`core/database`) for persistent printer profiles, print jobs, and document history.
  - Introduce ViewModel (`MainViewModel`, `PrintQueueViewModel`) with structured UI state.
- **Phase 2: True IPP 2.0 & Network Printing Core**
  - Build binary IPP 2.0 parser & encoder (`printer/protocol/ipp/IppPacket.kt`): `Get-Printer-Attributes` (query real marker-levels/status) and `Print-Job` envelope.
  - Build JetDirect RAW 9100 adapter and SNMP MIB fallback for toner readings.
- **Phase 3: Comprehensive Print Preview & Queue System**
  - Implement full print configuration: Paper sizes (A4, A3, A5, Letter, Legal, 4x6), Duplex (Long/Short edge), Color/Grayscale, Page ranges (`1-3, 5`), Copies, and Fit/Fill scale.
  - Live page-by-page interactive Preview using `PdfRenderer`.
  - Background Print Queue manager with foreground service notification.
- **Phase 4: Smart Scanner & Offline OCR**
  - Real document edge detection and perspective warp.
  - On-device offline text extraction.
- **Phase 5: Professional PDF Studio**
  - PDF Merge, Split, Compress (configurable quality), Reorder, Visual Signature & Stamp canvas.
- **Phase 6: Update Security & Verification**
  - Add SHA-256 signature verification in `InAppUpdateManager` before installation handoff.
- **Phase 7: Verification & Release**
  - Compile debug and release builds, run instrumentation/unit tests, and verify on real device.
