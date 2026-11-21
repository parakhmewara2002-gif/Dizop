# DIZOP V2 Security Audit & Policy

1. **Local-First & Data Sovereignty**:
   - Documents are stored in application-private storage (`context.filesDir` / `context.cacheDir`).
   - No analytics SDKs, trackers, or telemetry payloads sent to external servers.
   - Zero mandatory cloud accounts.

2. **Android Scoped Storage & FileProvider**:
   - `FileProvider` authorities scoped to `${applicationId}.provider` with restricted grant flags.
   - Storage Access Framework (SAF) used for reading and writing user-selected documents.
   - Temporary PDF/image cache files automatically sanitized post print-dispatch.

3. **In-App Updater Cryptographic Validation**:
   - Enforces SHA-256 hash comparison between the signed manifest and the downloaded binary prior to dispatching package installation intents.
   - Protects against Man-in-the-Middle (MitM) payload substitution.

4. **Network Sandboxing**:
   - Socket binding isolated to printer communications without mutating global process interfaces unless requested.
   - No cleartext HTTP traffic allowed outside local intranet subnets (RFC 1918).
