# Fake GPS — ZTE Z2335CC build

Target device: ZTE Z2335CC, Android 10 (API 29), small non-touch display.

Source project: FakeGPS-2.0.zip supplied in the ChatGPT conversation. The source archive still needs to be added to this folder before the Android build can run.

Build requirements identified from the supplied project:
- Gradle wrapper: 8.11.1
- Android Gradle Plugin: 8.7.3
- JDK: 17
- Compile SDK: 34
- Minimum SDK: 21
- Target SDK: 34
- Expected debug APK: FakeGPS_v2.0.apk

Device note: the original app includes touch-operated joystick controls and a floating overlay. Those controls may not be usable on the ZTE's non-touch keypad display. Do not claim full device usability until the UI is adapted and tested on the device.

Security/build note: do not commit the supplied debug keystore. Use the standard generated debug signing configuration for CI.
