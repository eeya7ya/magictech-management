# Notification Sounds

This directory contains audio files for notification alerts.

## Required Files

- **notification.wav** - Main notification sound (played when any notification appears)

## Adding Your Own Sound

1. Find or create a notification sound file (WAV format recommended)
2. Name it `notification.wav`
3. Place it in this directory: `src/main/resources/sounds/`
4. Rebuild the project

## Sound Requirements

- **Format**: WAV (recommended) or MP3
- **Duration**: 0.5 - 2 seconds (short and pleasant)
- **Volume**: Medium (the app will play at 50% volume)
- **Sample Rate**: 44100 Hz recommended

## Free Sound Resources

You can download free notification sounds from:
- https://notificationsounds.com/
- https://freesound.org/
- https://mixkit.co/free-sound-effects/notification/

## Fallback Behavior

If `notification.wav` is not found, the application will fall back to the system beep sound.

## Example Sound Generation (using online tools)

You can generate a simple notification tone using:
1. Go to https://www.wavtones.com/
2. Select frequency: 800-1200 Hz
3. Duration: 0.3 seconds
4. Generate and download as `notification.wav`
5. Place in this directory
