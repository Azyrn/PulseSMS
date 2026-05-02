# PulseSMS 💬

A fast, private SMS app for Android built with Kotlin.

## Features

- **Fast** — lightweight and responsive messaging experience
- **Private** — your messages stay on your device
- **Clean UI** — minimal interface focused on what matters

## Tech Stack

- **Language:** Kotlin
- **Platform:** Android
- **Build System:** Gradle (Kotlin DSL)
- **Architecture:** Multi-module (`app`, `core`, `feature`)

## Project Structure

```
PulseSMS/
├── app/          # Main application module
├── core/         # Shared utilities and base components
├── feature/      # Feature-specific modules
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
