# Turn Timer - Android App

A simple and elegant turn timer app for Android, built with Kotlin.

## Features

- ⏱️ Precise countdown timer with millisecond accuracy
- 👥 Track turns for multiple players
- 🎮 Perfect for board games, debates, or any turn-based activities
- ⚙️ Quick time presets (30s, 60s, 120s)
- 🎨 Modern Material Design UI with dark theme
- ▶️ Start, pause, and reset controls
- ➡️ Easy "Next Turn" button to advance players

## Building the Project

### Prerequisites

- Android Studio (latest version recommended)
- Android SDK 24 or higher
- JDK 8 or higher

### Steps to Build

1. Open Android Studio
2. Click "Open an Existing Project"
3. Navigate to this directory and select it
4. Wait for Gradle sync to complete
5. Click the "Run" button or press Shift+F10

### Building from Command Line

```bash
./gradlew assembleDebug
```

The APK will be generated in `app/build/outputs/apk/debug/`

## Installation

### On Emulator
1. Start an Android emulator from Android Studio
2. Click the "Run" button to install and launch the app

### On Physical Device
1. Enable "Developer Options" and "USB Debugging" on your Android device
2. Connect your device via USB
3. Click the "Run" button and select your device

## Usage

1. **Select Time**: Choose a time preset (30s, 60s, or 120s) at the bottom
2. **Start Timer**: Tap the green START button to begin countdown
3. **Pause/Resume**: Tap PAUSE to stop, tap again to resume
4. **Reset**: Reset the current player's timer to the selected preset
5. **Next Turn**: Move to the next player and reset the timer

## Tech Stack

- **Language**: Kotlin
- **UI**: Material Design 3, View Binding
- **Architecture**: Single Activity
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## License

This project is open source and available under the MIT License.
