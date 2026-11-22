# Fake GPS — ZTE Z2335CC build

Target device: ZTE Z2335CC, Android 10 (API 29), small non-touch keypad display.

The adapted source project is prepared as a separate ZIP in the ChatGPT conversation: `FakeGPS-2.0-ZTE-2335CC.zip`. It includes the original FakeGPS 2.0 project plus a keypad-control adaptation.

## Keypad behavior added
- Focus the floating joystick's arrow controls using the D-pad.
- Press OK/center while an arrow is focused to select GPS movement mode.
- Use the D-pad to move the simulated GPS location.
- Press OK/center again to exit movement mode.
- Outside movement mode, D-pad navigation and OK activation operate the floating controls.

## Build requirements
- Gradle wrapper: 8.11.1
- Android Gradle Plugin: 8.7.3
- JDK: 17
- Compile SDK: 34
- Minimum SDK: 21
- Target SDK: 34
- Expected debug APK: FakeGPS_v2.0.apk

The bundled debug keystore has been excluded; Android Gradle Plugin's generated debug signing is used.

## Current status
The adapted ZIP is prepared, but its binary contents have not yet been committed to this repository because the available GitHub connection only supports text file uploads. No APK has been built or tested on the ZTE handset yet. The ZIP can be uploaded to this folder manually, or through a GitHub upload workflow that accepts a file attachment.
