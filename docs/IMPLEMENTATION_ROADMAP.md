# DIZOP V2 Implementation Roadmap

- [x] **Phase 0 — Baseline Audit & Source Checkpoint**
  - Git repository initialized, clean checkpoint created.
  - Complete source audit performed and documented.
- [ ] **Phase 1 — Print Engine & IPP Protocol Upgrades**
  - Pass `PrintJobConfiguration` (copies, duplex, paper size, color mode) directly to IPP encoder.
  - Upgrade `IppPacketEncoder` to encode media attributes, copies, and sides.
  - Implement real IPP 2.0 binary response parser for `printer-state` and `marker-levels`.
  - Dynamic mDNS TXT record parsing for resource paths (`rp=...`).
- [ ] **Phase 2 — Wi-Fi Direct & USB Hardware Hardening**
  - Extract dynamic Group Owner IP from `WifiP2pInfo`.
  - Add printer compatibility checks for USB host endpoints.
- [ ] **Phase 3 — High-Resolution CameraX Scanner Pipeline**
  - Implement full-resolution `ImageCapture` with quadrilateral boundary detection.
- [ ] **Phase 4 — PDF Studio & Vector Preservation**
  - Implement non-destructive vector PDF merging and multi-level compression presets.
- [ ] **Phase 5 — Build Verification & Live Device Testing**
  - Recompile debug APK, run tests, and verify on real device (TECNO CG7).
