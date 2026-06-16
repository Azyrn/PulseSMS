# PulseSMS 💬

A fast, private SMS app for Android built with Kotlin.

## Features

- **Fast** — lightweight and responsive messaging experience
- **Private** — your messages stay on your device
- **Clean UI** — minimal interface focused on what matters
- **MMS Support** — send and receive photos with automatic downscaling
- **In‑app Camera** — take photos directly from the composer
- **Gallery Picker** — native gallery with multi‑select (tap multiple photos, then confirm)
- **MMS Image Viewer** — tap an MMS image to view it full‑screen; download button saves to your gallery and opens it automatically
- **Voice Messages** — record and send audio messages as MMS attachments; tap to play received voice messages inline
- **Message Threading** — conversations grouped by contact with inbox filtering (All, Personal, Business, OTP)
- **SIM Selection** — choose which SIM to send from (dual‑SIM support)
- **Biometric Lock** — secure the app with fingerprint or passcode
- **Auto‑Copy Codes** — OTP and verification codes are detected and copied automatically
- **Sync Support** — optional sync module for cross‑device message history
- **Theming** — dynamic color (Material You), light/dark/black themes, language preferences (English, French)

## Tech Stack

- **Language:** Kotlin
- **Platform:** Android (min SDK 26, target SDK 36)
- **Build System:** Gradle (Kotlin DSL)
- **Architecture:** Multi-module (`app`, `core`, `feature`) with MVVM
- **UI Framework:** Jetpack Compose + Material 3
- **Image Loading:** Coil
- **State Management:** ViewModels, StateFlow, Compose state
- **Persistence:** Room (SQLite), Android SMS/MMS ContentProvider
- **DI:** Manual constructor injection
- **Background Work:** WorkManager, coroutines
- **Testing:** JUnit 5, kotlinx.coroutines test

## Project Structure

```
PulseSMS/
├── app/          # Main application module (UI, ViewModels, SMS integration)
│   ├── ui/       # Compose screens (inbox, conversation, settings, etc.)
│   ├── sms/      # SMS/MMS reading, sending, MMS part resolution
│   └── contact/  # Contact lookup and display helpers
├── core/         # Shared utilities (design system, database, security, observability)
│   ├── design/   # Design tokens, theme, common components
│   ├── database/ # Room database, DAOs, migrations
│   └── security/ # Biometric lock, password, compliance
├── feature/      # Feature-specific modules
│   ├── messaging/  # Conversation repository, sync logic
│   └── sync/       # Cross-device message sync
└── docs/         # Documentation and skills
```

## Getting Started

### Prerequisites

- Android Studio (latest stable)
- JDK 11 or higher
- Android SDK

### Build & Run

1. Clone the repository:
   ```bash
   git clone https://github.com/Azyrn/PulseSMS.git
   cd PulseSMS
   ```

2. Open the project in Android Studio.

3. Sync Gradle and run on a device or emulator:
   ```bash
   ./gradlew assembleDebug
   ```

## License

This project is open source. See [LICENSE](LICENSE) for details.
