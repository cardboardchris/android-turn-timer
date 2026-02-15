# Turn Timer - Android App

A multi-player turn timer app for Android, built with Kotlin. Track cumulative time per player across rounds — perfect for board games, debates, or any turn-based activities.

## Features

- 👥 Add up to 5 players by name
- 🔀 Drag-to-reorder player turn order
- ⏱️ Cumulative timer per player, counting up in MM:SS format
- ▶️ Start, pause, and resume the game at any time
- 👆 Tap anywhere to end the current player's turn
- 💾 Player names saved between sessions
- 🔄 Automatic turn cycling — loops back to the first player after the last
- 📊 Game summary screen showing total accumulated time for each player
- 📳 Haptic vibration feedback on turn changes
- 💡 Screen stays on during active play
- 🛡️ Back button confirmation to prevent accidental game exit
- 🎨 Modern Material Design 3 dark theme

## Building the Project

### Prerequisites

- Android Studio (latest version recommended)
- Android SDK 24 or higher
- JDK 17 or higher

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

1. **Player Setup**: Enter player names (2–5 players required). Previously saved player names are automatically loaded. Drag handles to reorder turn sequence. Tap "START GAME" when ready.
2. **Game Screen**: The active player's name and timer are shown prominently. All players and their cumulative times are listed below. Tap anywhere on the screen to end the current player's turn. Use "PAUSE" to pause the game, or "END GAME" to finish.
3. **Game Summary**: After ending the game, view each player's total accumulated time. Tap "NEW GAME" to return to setup and start fresh.

## Tech Stack

- **Language**: Kotlin
- **UI**: Material Design 3, View Binding, XML layouts
- **Architecture**: Single Activity + Fragments, MVVM pattern
- **State Management**: ViewModel + StateFlow
- **Timer**: Coroutine-based flow with wall-clock timestamps
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)

## License

This project is open source and available under the MIT License.
