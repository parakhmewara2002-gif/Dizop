# DIZOP — DEFECT & BUG TRACKER

| Bug ID | Severity | Status | Module | Summary |
|---|---|---|---|---|
| **DIZ-SEC-002** | **P0** | **AUDITED / CONFIRMED** | `update` | In-App update SHA-256 integrity check skipped if hash is blank or omitted |
| **DIZ-SCAN-001** | **P0** | **AUDITED / CONFIRMED** | `scanner` | CameraX completely unused; scanner takes 160x120 avatar thumbnails |
| **DIZ-SCAN-003** | **P0** | **AUDITED / CONFIRMED** | `pdf` | Flattening vector PDF text into raster bitmaps; 4000% compression file bloat |
| **DIZ-SCAN-004** | **P0** | **AUDITED / CONFIRMED** | `memory` | Missing `bitmap.recycle()` on multi-page operations; 35MB A4 unconstrained allocation |
| **DIZ-REV-001** | **P0** | **AUDITED / CONFIRMED** | `ui/printer` | "Print Complete" falsely announced immediately upon socket spool write |
| **DIZ-PRINT-005** | **P0** | **AUDITED / CONFIRMED** | `wifidirect` | Hardcoded `192.168.49.1:9100` without group negotiation or connect handshake |
| **DIZ-PRINT-011** | **P0** | **AUDITED / CONFIRMED** | `hardware` | No driver / firmware loader for host-based HP 1020 / Canon LBP2900 |
| **DIZ-QA-001** | **P0** | **AUDITED / CONFIRMED** | `qa` | Zero unit/UI/integration tests; missing `/app/src/test` directory |
| **DIZ-ARCH-001** | **P0** | **AUDITED / CONFIRMED** | `architecture` | 1,102-line God Composable with all state destroyed on screen rotation |
| **DIZ-UI-001** | **P0** | **AUDITED / CONFIRMED** | `ui/threading` | File I/O, bitmap decompress, and PDF compression running on main UI thread |
| **DIZ-SEC-001** | **P1** | **AUDITED / CONFIRMED** | `security` | `path="."` in `file_paths.xml` exposes internal app files and database |
| **DIZ-SEC-003** | **P1** | **AUDITED / CONFIRMED** | `security` | Update APK saved to external storage vulnerable to TOCTOU hijacking |
| **DIZ-SEC-004** | **P1** | **AUDITED / CONFIRMED** | `security` | Implicit PackageInstaller intent with temporary URI grant |
| **DIZ-SEC-005** | **P1** | **AUDITED / CONFIRMED** | `privacy` | Sensitive ID card scans and signed contracts retained in cache without cleanup |
| **DIZ-SEC-006** | **P1** | **AUDITED / CONFIRMED** | `security` | Mutable implicit PendingIntent without package scoping for USB permission |
| **DIZ-PRINT-001** | **P1** | **AUDITED / CONFIRMED** | `ipp` | Hardcoded Request IDs (1, 2) and unvalidated response request-id matching |
| **DIZ-PRINT-002** | **P1** | **AUDITED / CONFIRMED** | `ipp` | HTTP 200 with empty body accepted as print success |
| **DIZ-PRINT-006** | **P1** | **AUDITED / CONFIRMED** | `network` | OkHttp IPP client does not bind to Wi-Fi socket; fails when cellular data active |
| **DIZ-PRINT-007** | **P1** | **AUDITED / CONFIRMED** | `usb` | Partial USB bulk write handling discards bytes on secondary retry |
| **DIZ-PRINT-009** | **P1** | **AUDITED / CONFIRMED** | `raw/usb` | Raw PDF streams sent to port 9100/USB without PDL compatibility check |
| **DIZ-SCAN-002** | **P1** | **AUDITED / CONFIRMED** | `scanner` | Static ColorMatrix contrast filter falsely marketed as "AI Shadow Removal" |
| **DIZ-ARCH-002** | **P1** | **AUDITED / CONFIRMED** | `persistence` | Zero Room entities or DataStore; all manual printers lost on app restart |
| **DIZ-ARCH-003** | **P1** | **AUDITED / CONFIRMED** | `lifecycle` | Activity context leaks and unreleased Wi-Fi multicast lock on orientation flip |
| **DIZ-ARCH-004** | **P1** | **AUDITED / CONFIRMED** | `ui` | Unattached headless WebView used for web print hangs on network failure |
| **DIZ-REV-005** | **P1** | **AUDITED / CONFIRMED** | `discovery` | Concurrent `nsdManager.resolveService()` triggers `FAILURE_ALREADY_ACTIVE` |
| **DIZ-UI-002** | **P1** | **AUDITED / CONFIRMED** | `ui` | Synchronous bitmap drawing on main thread freezes digital signature submit |
| **DIZ-UI-006** | **P2** | **AUDITED / CONFIRMED** | `accessibility` | Sub-48dp touch targets; Clear button lacks onClick; unlabeled buttons |
| **DIZ-SEC-008** | **P2** | **AUDITED / CONFIRMED** | `security` | Over-privileged unused permissions (`CHANGE_NETWORK_STATE`, broad storage) |
| **DIZ-PRINT-008** | **P2** | **AUDITED / CONFIRMED** | `usb` | `claimInterface` lacks `finally` block; USB handles leak on transfer error |
| **DIZ-PRINT-010** | **P2** | **AUDITED / CONFIRMED** | `discovery` | DNS-SD ignores `pdl`, `URF`, `ty` TXT records; brand heuristic naive |
| **DIZ-QA-010** | **P2** | **AUDITED / CONFIRMED** | `ci` | CI executes 0 tests, lacks linting, and lacks pull request checks |
