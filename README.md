# Turn Timer - Android App

A multi-player turn timer app for Android, built with Kotlin. Track cumulative time per player across rounds — perfect for board games, debates, or any turn-based activities.

## Features

- 👥 Add up to 5 players by name
- 🎨 Player Color Selection — Choose from 8 vibrant colors during setup; auto-assigned or tap to customize
- 🔀 Drag-to-reorder player turn order
- ⏱️ Cumulative timer per player, counting up in MM:SS format
- ▶️ Start, pause, and resume the game at any time
- ➡️ "End Turn" button to advance to the next player
- 🔄 Automatic turn cycling — loops back to the first player after the last
- 📊 Game summary screen showing total accumulated time for each player
- 📳 Haptic vibration feedback on turn changes
- 💡 Screen stays on during active play
- 🛡️ Back button confirmation to prevent accidental game exit
- 🎨 Modern Material Design 3 dark theme with animated background color per active player

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

1. **Player Setup**: Enter player names (2–5 players required). Drag handles to reorder turn sequence. Tap any color circle to customize player color from an 8-color palette (auto-assigned by default). Tap "START GAME" when ready.
2. **Game Screen**: The active player's name and timer are shown prominently with their assigned color as a smooth animated background. All players are listed below with color dots showing their assigned colors. Use "END TURN" to advance to the next player, "PAUSE" to pause the game, or "END GAME" to finish. Text contrast automatically adapts to the background color for readability.
3. **Game Summary**: After ending the game, view each player's total accumulated time with their assigned color dots. Tap "NEW GAME" to return to setup and start fresh.

## Tech Stack

- **Language**: Kotlin
- **UI**: Material Design 3, View Binding, XML layouts
- **Architecture**: Single Activity + Fragments, MVVM pattern
- **State Management**: ViewModel + StateFlow
- **Timer**: Coroutine-based flow with wall-clock timestamps
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 36 (Android 16)

## License

This project is open source and available under the MIT License.
