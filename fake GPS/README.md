# Fake GPS for Oilam

This folder builds the upstream FakeGPS 2.0 Android app as a separate APK.
Source is pinned to the upstream `2.0` release tag:
https://github.com/xiangtailiang/FakeGPS/tree/2.0

Target device: ZTE Z2335CC, Android 10 (API 29).
The app itself supports Android 5.0+ and uses Android's mock-location provider.

## Build
From this folder's CI job, the pinned source is checked out into `source/` and built with its committed Gradle wrapper:

```sh
cd source
./gradlew assembleDebug
```

The APK is uploaded as the `FakeGPS-ZTE2335CC-debug` GitHub Actions artifact.
