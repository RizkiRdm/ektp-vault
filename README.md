# LOKER

**LOKER** is a hardware-anchored credential vault, application locker, and system autofill service engineered around zero-trust cryptographic isolation. Built for Android using Kotlin and Jetpack Compose, LOKER anchors encryption keys to physical contactless security cards (ISO 14443-4 / NFC) paired with biometric verification.

---

## Key Highlights

- **Hardware-Anchored Key Hierarchy**:
  - **Vault Master Key (VMK)**: 256-bit AES cryptographic key generated via Android Keystore `SecureRandom`.
  - **Card Key Encryption Key (KEK)**: Derived directly from the physical security card's cryptographic UID using HKDF-SHA256.
  - **AES-256-GCM Envelope Encryption**: VMK is stored only in its wrapped state encrypted under the card KEK. No plaintext master key ever touches persistent storage.
  - **Zero-Storage Security**: When locked, the master key is zeroed out in memory (`securityEngine.wipeBytes()`). Decryption is mathematically impossible without physical card presence.

- **System-Wide Autofill Engine**:
  - Native `AutofillService` implementation providing seamless username and password credential autofill in web browsers and native apps.
  - Interactive on-screen overlay modal for credential picking with quick biometric authorization.

- **Application Locker with Hardware Authentication**:
  - Accessibility and overlay services enforce security lockouts over user-selected high-risk applications (banking, crypto, social, email).
  - PIN and hardware security card scan required to release application locks.

- **Golden Ratio (φ ≈ 1.618) Spatial & Typographic Grid**:
  - Strict mathematical visual hierarchy applying Fibonacci proportions across typography scale, margins, card radii, and component layouts.
  - Consistent editorial Brutalist aesthetic: Carbon Black (`#000000`), Sovereign Ink (`#0D1117`), Warm Canvas (`#F9F9FB`), Signal White (`#FFFFFF`), and Cyber Crimson (`#DC2626`).

- **Multi-Key Hardware Management**:
  - Register backup contactless hardware keys to guarantee business continuity and eliminate single points of failure.
  - Cryptographic backup export and import using standard JSON envelopes with Argon2/PBKDF2 password derivation.

---

## Architecture Overview

```
com.example
├── data
│   ├── local (Room DB: Credentials, PhysicalKeys, SecurityAuditLog)
│   ├── nfc (NfcReaderServiceImpl)
│   └── repository (VaultRepository, AppLockRepository)
├── domain
│   ├── model (Credential, PhysicalKey, AuditEvent, SecurityCategory)
│   ├── repository (Interfaces)
│   └── usecase (VaultUseCases)
├── security
│   ├── CryptoManager (AES-256-GCM, HKDF, PBKDF2)
│   ├── KeyStorage (In-memory ephemeral VMK cache)
│   └── AppLockManager (Foreground app detector & security barriers)
├── service
│   ├── KtpAutofillService (Android Autofill Framework)
│   ├── KtpAppLockAccessibilityService (Accessibility-based lock barrier)
│   └── NfcReaderService (ISO 14443-4 smart card transceiver)
└── ui
    ├── components (AuthBottomSheet, AutofillPromptModal)
    ├── screens (Dashboard, AppLock, AutofillManager, PhysicalKeyManager)
    └── theme (GoldenRatio, Color, Type, Theme)
```

---

## Security Model

1. **Physical Card Tap**: NFC transceiver interacts with contactless security card to read verified hardware UID.
2. **Key Derivation**: HKDF-SHA256 expands the card UID and salt into a 256-bit AES-GCM Key Encryption Key (KEK).
3. **Unwrapping**: Encrypted VMK blob is unwrapped in volatile RAM.
4. **Session Lock**: Ephemeral memory is strictly wiped upon screen off, manual lock, or app backgrounding timeout.

---

## Setup & Compilation

- Build system: Gradle Kotlin DSL (`build.gradle.kts`)
- Compile SDK: Android 35, Min SDK: 26
- Verified with Android Edge-to-Edge (`enableEdgeToEdge()`) and Material 3 design system.
