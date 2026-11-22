# DIZOP — MULTI-AGENT STAGED IMPLEMENTATION ROADMAP

All changes are gated by User Approval (Stage 3). No application source code is modified without approval.

---

### Phase 1: Test Infrastructure & Verification Harnesses (QA & Security)
1. **Initialize `/app/src/test` Source Set**: Add JUnit 4, MockK, Kotlinx Coroutines Test, Turbine, Google Truth, Robolectric, and OkHttp MockWebServer.
2. **Implement Core Protocol Tests**:
   - `IppPacketEncoderTest`: Binary packet verification for RFC 8011 tags and encoding.
   - `IppResponseParserTest`: Valid status codes, ink marker extraction, and malformed packet fuzzing.
   - `InAppUpdateManagerTest`: SHA-256 calculation, tamper rejection, and version comparison.

---

### Phase 2: Security & Privacy Hardening
1. **Mandatory SHA-256 & APK Integrity (`DIZ-SEC-002`, `DIZ-SEC-003`, `DIZ-SEC-004`)**:
   - Reject any update missing a valid 64-char hex SHA-256 digest.
   - Relocate update download directory from external storage to internal private storage (`context.filesDir/updates/`).
   - Use explicit PackageInstaller intent scoping.
2. **FileProvider Hardening (`DIZ-SEC-001`)**:
   - Replace broad `path="."` with isolated subdirectories (`updates/` and `print_docs/`).
3. **Sensitive Document Cache Cleanup (`DIZ-SEC-005`)**:
   - Deterministic cache file cleanup in `finally` blocks and upon app launch.
4. **Permissions & Manifest Hygiene (`DIZ-SEC-008`)**:
   - Remove redundant `CHANGE_NETWORK_STATE` and broad storage permissions.

---

### Phase 3: Printer Protocol & Hardware Conformance
1. **IPP Request-ID & Response Verification (`DIZ-PRINT-001`, `DIZ-PRINT-002`, `DIZ-PRINT-003`)**:
   - Generate monotonic `AtomicInteger` request IDs and validate `response.requestId == request.requestId`.
   - Reject HTTP 200 responses with empty/truncated IPP payloads.
   - Proper RFC 8011 status code range parsing.
2. **True State Modeling (`DIZ-REV-001`, `DIZ-PRINT-004`)**:
   - Transition UI to `Job Submitted` / `Spooling` rather than `Print Complete`.
3. **Network Routing Fix (`DIZ-PRINT-006`)**:
   - Bind `OkHttpClient` socket factory to `_printerWifiNetwork.value` so IPP requests never route over cellular SIM data when printer Wi-Fi lacks internet.
4. **USB Transfer Loop & Resource Safety (`DIZ-PRINT-007`, `DIZ-PRINT-008`)**:
   - Complete multi-chunk loop for partial bulk transfers.
   - Guaranteed `finally` release of USB interfaces and connection handles.
5. **Wi-Fi Direct Handshake & Host-Based Printer Truthfulness (`DIZ-PRINT-005`, `DIZ-PRINT-011`)**:
   - Implement real P2P connect flow; replace hardcoded IP.
   - Display honest advisory for host-based HP 1020 / Canon LBP2900 printers.

---

### Phase 4: Scanner, PDF Pipeline & Architecture Refactoring
1. **CameraX Full-Resolution Capture (`DIZ-SCAN-001`, `DIZ-SCAN-002`)**:
   - Replace `TakePicturePreview()` with CameraX full-res capture pipeline.
   - Remove deceptive "AI" labels from static ColorMatrix filters.
2. **Memory Leaks & Bitmap Recycling (`DIZ-SCAN-004`)**:
   - Recycle native bitmaps immediately after rendering.
3. **Architecture & ViewModel Extraction (`DIZ-ARCH-001`, `DIZ-ARCH-002`, `DIZ-UI-001`)**:
   - Extract `MainDashboardViewModel` to preserve state across rotation.
   - Offload heavy I/O and PDF processing to background threads (`Dispatchers.IO`).
   - Add Room database for persisting manual printers and print history.
