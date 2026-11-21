# DIZOP Known Issues & Technical Limitations

1. **Simulated Ink Readings in Discovery**:
   - `UniversalPrinterDiscovery.kt` currently assigns hardcoded supply levels (`85%`, `70%`, etc.).
   - *Fix:* Remove hardcoded supply levels; query real IPP attributes via `Get-Printer-Attributes` and display `Not Reported` or `Unknown` if unsupported.

2. **IPP 2.0 Binary Transport**:
   - Raw byte streaming to Port 631 without HTTP/1.1 POST wrapper and RFC 8011 binary frame (`0x02, 0x00` version, `0x00, 0x02` operation-id).
   - *Fix:* Implement RFC 8011 binary IPP encoder & HTTP POST client.

3. **Print Preview & Settings Dialog**:
   - Currently, selecting a file immediately prints without allowing the user to configure paper size (A4 vs Letter), copies, duplex, or page range.
   - *Fix:* Add an interactive iOS-style Print Configuration & Preview sheet prior to job dispatch.

4. **USB Driver Compatibility**:
   - Legacy host-based printers (like HP LaserJet 1020, Canon LBP2900) require custom rasterization (e.g. Zenographics ZJS or CAPT) rather than raw PDF streaming.
   - *Status:* Documented as Hardware-Unverified for proprietary USB printer drivers. Generic USB printers supporting PostScript/PCL are supported.

5. **Security - In-App Update Verification**:
   - `InAppUpdateManager` downloads APKs without computing and matching the SHA-256 hash.
   - *Fix:* Add SHA-256 validation before launching `application/vnd.android.package-archive` Intent.
