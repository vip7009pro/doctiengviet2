# ROADMAP - Dự án DocTiengViet

## Giai đoạn hiện tại: Nâng cấp Target SDK theo yêu cầu Google Play Store (Android 16 - API 36)
- [x] Cập nhật cấu hình môi trường xây dựng (Android SDK path, JDK 17, Gradle properties)
- [x] Cập nhật `compileSdk` lên 36 trong `app/build.gradle`
- [x] Cập nhật `targetSdkVersion` lên 36 trong `app/build.gradle`
- [x] Cập nhật `buildToolsVersion` lên 36.0.0
- [x] Chuẩn hóa quyền lưu trữ `WRITE_EXTERNAL_STORAGE` (`maxSdkVersion="28"`) trong `AndroidManifest.xml`
- [x] Chuyển `buildConfig true` vào `buildFeatures` trong `app/build.gradle`
- [x] Sửa lỗi thiếu constants SharedPreferences (`PREFS_NAME`, ...) và cập nhật `Locale.forLanguageTag` trong `MainActivity.kt`
- [x] Thực thi build Gradle `:app:assembleDebug` và `:app:assembleRelease` thành công
- [x] Kiểm tra và xác nhận artifact APK đầu ra (targetSdkVersion = 36, platformBuildVersionCode = 36)
- [x] Thực thi build Android App Bundle release (`:app:bundleRelease`) với `versionCode 31` thành công
- [x] Cấu hình `signingConfigs.release` đọc từ `key.properties` và ký số thành công bằng `G:\NODEJS\doctiengviet.jks` (alias `doctiengviet`) cho cả AAB và APK release
- [x] Cập nhật tài liệu kỹ thuật `CONTEXT.md` và hoàn tất roadmap

## Kế hoạch tương lai
- [ ] Kiểm thử tương thích sâu trên Android 16 thực tế / emulator
- [ ] Tối ưu hóa giao diện Jetpack Compose (Edge-to-Edge display cho Android 15/16)
- [ ] Thêm các nhà cung cấp TTS khác ngoài Azure nếu có nhu cầu
