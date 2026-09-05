# Emulator test tooling

These fixtures are emulator-only. They do not use or package proprietary XGIMI
files from `local-vendor/`, and they are not used by the production APK build in
`scripts/build_apk.sh`.

## Build artifacts

Set the same Android SDK variables used by the main build. If macOS has no
default Java runtime, point `JAVA_HOME` at Android Studio's bundled runtime:

```bash
export ANDROID_SDK_ROOT="$HOME/Library/Android/sdk"
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

Build the mock XGIMI service:

```bash
./tools/emulator/mock-xgimi-service/build.sh
```

Build the app with emulator-only fake GMPF classes:

```bash
./tools/emulator/build_app_with_fake_vendor.sh
```

Install both APKs on an emulator:

```bash
adb install -r build/mock-xgimi-service/mock-xgimi-service.apk
adb install -r build/emulator-app/xh25l-keystone-presets-emulator.apk
```

## Scenario control

The mock service is controlled through exported adb broadcasts. After changing a
connection scenario, force-stop and relaunch the app under test so it binds
again:

```bash
adb shell am force-stop com.xgimirom.presets
adb shell monkey -p com.xgimirom.presets 1
```

Reset to the default success state with retained environment values:

```bash
./tools/emulator/mock-xgimi-service/control.sh reset
```

Success scenario. The mock retains environment writes and returns a non-empty
`kst_ofs`, so save/apply can complete when the emulator fake GMPF classes are
installed in the app APK:

```bash
./tools/emulator/mock-xgimi-service/control.sh scenario success
```

Connection failure scenario. The service binds but returns no Common binder:

```bash
./tools/emulator/mock-xgimi-service/control.sh scenario no_common
adb shell am force-stop com.xgimirom.presets
adb shell monkey -p com.xgimirom.presets 1
```

Connection timeout setup. The root binder sleeps during Common binder lookup.
The optional number is the deterministic delay in milliseconds:

```bash
./tools/emulator/mock-xgimi-service/control.sh scenario timeout 15000
adb shell am force-stop com.xgimirom.presets
adb shell monkey -p com.xgimirom.presets 1
```

Environment write failure. The optional key defaults to `kst_ofs`; choose
`shape_type` to fail earlier in restore:

```bash
./tools/emulator/mock-xgimi-service/control.sh scenario env_write_failure shape_type
```

Readback mismatch. The optional key defaults to `kst_ofs`; the mock accepts the
write but returns a deterministic mismatched value on reads:

```bash
./tools/emulator/mock-xgimi-service/control.sh scenario env_readback_mismatch kst_ofs
```

Override an environment value for the success scenario:

```bash
./tools/emulator/mock-xgimi-service/control.sh env kst_ofs "1,11,22,33,44,55,66,77,88"
```

## Runtime limits

The fake GMPF classes only model the reflected API surface needed by this app:
`DisplayManager.getInstance()`, `getCorrectKeystone`,
`checkTrapezoidCoordinate`, `correctKeystone`, `KeyStoneFullCoordinates`, and
`KeyStonePoint`. They verify Java integration and app-side save/apply control
flow, but they do not emulate projector optics, vendor native libraries,
firmware services, or real hardware timing.
