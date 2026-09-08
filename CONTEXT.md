# CONTEXT

## Date
- 2026-09-05

## What Changed
- **Target API Level Upgrade (Android 16 - API level 36)**:
  - Updated `compileSdk` to `36`.
  - Updated `targetSdkVersion` to `36`.
  - Updated `buildToolsVersion` to `'36.0.0'`.
  - Cleaned redundant `compileSdk` in `defaultConfig`.
  - Migrated `buildConfig true` into `buildFeatures { ... }` in `app/build.gradle` and removed deprecated `android.defaults.buildfeatures.buildconfig=true` from `gradle.properties`.
  - Configured `org.gradle.java.home` to Microsoft JDK 17 in `gradle.properties`.
  - Fixed `sdk.dir` in `local.properties` to point to the local Android SDK directory (`C:\Users\Admin\AppData\Local\Android\Sdk`).
- **Permissions & Security Compliance (Google Play Requirements)**:
  - Added `android:maxSdkVersion="28"` for `android.permission.WRITE_EXTERNAL_STORAGE` in `AndroidManifest.xml` (scoped storage compliant for Android 10+ and 16).
- **Code Fixes & Modernization**:
  - Added missing SharedPreferences keys and constants (`PREFS_NAME`, `PREF_PROVIDER`, `PREF_AZURE_KEY`, `PREF_AZURE_REGION`, `PREF_AZURE_VOICE`) in `MainActivity.kt`'s companion object to resolve unresolved reference compilation errors.
  - Replaced deprecated `Locale("vi", "VN")` constructor with `Locale.forLanguageTag("vi-VN")` in `MainActivity.kt`.
- **App Signing & Release Configuration**:
  - Configured `key.properties` (added to `.gitignore`) pointing to `G:\NODEJS\doctiengviet.jks` with alias `doctiengviet`.
  - Configured `signingConfigs.release` in `app/build.gradle` dynamically loading `key.properties`.
  - Keystore Certificate Fingerprints:
    - SHA1: `98:DD:43:EF:26:5A:5B:E4:43:F9:7A:00:36:AB:20:2F:EA:B7:CA:48`
    - SHA256: `22:EF:A5:D1:20:92:E6:43:E5:B9:85:2E:0B:E3:7D:5D:E5:9D:39:56:F0:9A:15:52:E8:0F:1B:49:62:76:D7:93`
    - Signer DN: `C=84, ST=Ha Noi, L=Ha Noi, O=Hung Nguyen page, OU=company, CN=Hung Nguyen`
- **Project Tracking**:
  - Created and updated `ROADMAP.md` tracking all Target SDK 36 and signing milestones.

## Provider Notes
- Integrated provider: Azure Speech (Neural TTS) via REST API.
- Free tier reference (F0): 0.5M TTS characters/month (subject to Azure policy changes).
- Default Vietnamese voices in app:
  - `vi-VN-HoaiMyNeural`
  - `vi-VN-NamMinhNeural`

## Runtime Behavior
- `Speak`:
  - Local mode: Android local TTS speak.
  - Azure mode: Cloud synth -> temporary MP3 -> playback.
- `Save MP3`:
  - Enabled for Azure mode.
  - Saved to Downloads/DocTiengViet (MediaStore on Android Q+).

## Build Verification
- Commands verified:
  - `cmd.exe /c "set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.20.101-hotspot&& gradlew.bat :app:bundleRelease :app:assembleRelease"`
- Badging verification (`aapt.exe dump badging`):
  - `targetSdkVersion: '36'`
  - `compileSdkVersion: '36'`
  - `platformBuildVersionCode: '36'`
  - `platformBuildVersionName: '16'`
- App Version:
  - `versionCode: 31`
  - `versionName: "3.30"`
- Signature verification (`apksigner verify --verbose`):
  - `Verified using v2 scheme (APK Signature Scheme v2): true`
  - `Number of signers: 1`
  - `Signer DN: CN=Hung Nguyen, O=Hung Nguyen page...`
- Result:
  - `BUILD SUCCESSFUL` cho:
    - Release AAB (Signed): `app/build/outputs/bundle/release/app-release.aab` (~12.25MB) - **File chính thức tải lên Google Play Console**.
    - Release APK (Signed): `app/build/outputs/apk/release/app-release.apk` (~13.18MB) - Đã ký bằng `doctiengviet.jks`.
    - Debug APK: `app/build/outputs/apk/debug/app-debug.apk` (~17.3MB).

## Google Play Store Compliance (Privacy Policy)
- **Date**: 2026-09-08
- **Issue Addressed**:
  - Rejection/Warning: *"App or developer details don’t match. Your privacy policy does not clearly identify the app, developer name, or legal entity associated with your Google Play store listing."*
- **Action Taken**:
  - Created [privacy_policy.html](file:///g:/NODEJS/doctiengviet2/privacy_policy.html) in the root directory.
  - Explicitly declared matching identifiers in a dedicated top-level card:
    - **App Name**: `Đọc Tiếng Việt 2.0` (also referenced as `Doc Tieng Viet 2.0`)
    - **Package Name / Application ID**: `com.hajima.vip7009pro.doctiengviet`
    - **Developer Name / Legal Entity**: `Hung Nguyen` (Store entity: `Hung Nguyen page` / `vip7009pro`)
    - **Developer Contact Email**: `vip7009pro@gmail.com`
    - **Play Store URL**: `https://play.google.com/store/apps/details?id=com.hajima.vip7009pro.doctiengviet`
  - Comprehensive disclosures:
    - Permission scoping (`INTERNET`, `WRITE_EXTERNAL_STORAGE maxSdkVersion="28"`).
    - Integrated third parties: Google AdMob (`com.google.android.gms:play-services-ads`), Microsoft Azure Speech Services.
    - Data retention, zero PII storage on custom servers, COPPA children's privacy statement.
  - Interactive bilingual support (English for Google Play automated review bots, Vietnamese switch for local users).

