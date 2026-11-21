# DIZOP V2 Printer Compatibility Matrix

| Brand / Model Class | Protocols Supported | Transport Types | Ink/Toner Telemetry | Notes & Tested Status |
| :--- | :--- | :--- | :--- | :--- |
| **HP DeskJet / LaserJet / Envy** | IPP Everywhere, RAW 9100 | Wi-Fi LAN, Wi-Fi Direct, USB | Supported via IPP `marker-levels` query | High compatibility across network & USB |
| **Canon PIXMA / imageCLASS** | IPP Everywhere, RAW 9100 | Wi-Fi LAN, Wi-Fi Direct | Supported via IPP attributes | PCL/UFR II network printing supported |
| **Epson EcoTank / WorkForce** | IPP Everywhere, RAW 9100, ESC/P-R | Wi-Fi LAN, Wi-Fi Direct, USB | Supported via IPP query | Excellent IPP Everywhere support |
| **Brother HL / MFC Series** | IPP Everywhere, RAW 9100, LPR | Wi-Fi LAN, Wi-Fi Direct, USB | Supported via IPP query | Standard Port 9100 & IPP tested |
| **Xerox / Ricoh / Lexmark** | IPP 2.0, PostScript, RAW 9100 | Wi-Fi LAN, Ethernet LAN | Supported via IPP attributes | Universal enterprise compatibility |
| **Generic AirPrint / Mopria** | IPP 1.1 / 2.0 | Wi-Fi LAN, Wi-Fi Direct | Dynamic RFC 8011 parsing | Works on all Mopria/AirPrint certified units |
| **Legacy Host-based USB (HP 1020, Canon LBP2900)** | Proprietary (ZJS / CAPT) | USB OTG Cable | Not reported over raw USB | Requires vendor host rasterizer. Flagged as *Hardware-Unverified* |
| **Bluetooth / USB POS Thermal** | ESC/POS 58mm / 80mm | Bluetooth, USB | Not applicable | Direct receipt and barcode printing |
