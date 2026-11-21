# DIZOP V2 System Architecture

```text
com.dizop.print/
├── core/
│   ├── database/          # Room Database, DAOs, Entities (Printers, Jobs, Vault)
│   ├── network/           # DualNetworkRouter, Socket Factories, Network Callbacks
│   ├── security/          # Keystore Helper, Biometric Manager, FileProvider
│   └── common/            # Result wrappers, Coroutine dispatchers, Extensions
├── printer/
│   ├── discovery/         # Multi-service mDNS DNS-SD, TXT record parser
│   ├── protocol/
│   │   ├── ipp/           # RFC 8011 binary encoder, dynamic URI client, parser
│   │   ├── raw/           # Port 9100 JetDirect TCP stream
│   │   ├── usb/           # USB Host Manager, Bulk transfer, Driver check
│   │   └── wifidirect/    # P2P WifiP2pManager, Group Owner IP resolution
│   ├── config/            # PrintJobConfiguration (PaperSize, ColorMode, Duplex, Copies)
│   └── queue/             # Persistent Print Job Queue, Foreground Service
├── scanner/
│   ├── camera/            # CameraX full-resolution ImageCapture lifecycle
│   ├── detection/         # Document boundary edge detection & quadrilateral crop
│   └── processor/         # Contrast boost, shadow reduction, 2-in-1 ID sheet
├── pdf/
│   ├── editor/            # Vector-preserving merge, split, compress, watermark
│   └── signature/         # Digital signature canvas & visual embedding
├── update/                # Secure manifest reader, DownloadManager, SHA-256 verifier
└── ui/
    ├── theme/             # iOS-inspired liquid-glass color scheme, typography
    ├── components/        # IosCard, SegmentedControl, DynamicIslandPill, PrintPreviewSheet
    ├── viewmodels/        # MainViewModel, ScannerViewModel, PdfViewModel
    └── screens/           # MainDashboardScreen, ScannerScreen, PdfStudioScreen
```
