# Doc Scan

**Doc Scan** is an Android document scanner, Clear Mode enhancer, multi-script offline OCR engine, and PDF signature & toolkit built with Kotlin and Jetpack Compose featuring a seamless **Liquid Glass UI** (Light Glass & Dark Glass modes).

---

## Features

- **Liquid Glass UI (Light & Dark Mode)**
  - Seamless floating frosted glass cards, specular borders, and a swipe-linked `HorizontalPager` + `LiquidGlassBottomBar` with tactile vibration haptics (`HapticController`).
  - Switch between **Library**, **Scan**, **Tools**, and **Settings** via horizontal swipe or tap.
- **Document Clear Mode & Live Filter Studio**
  - Captures and gallery imports automatically apply **Clear Mode** (paper background whitening + crisp dark ink enhancement while preserving stamps and colors).
  - Switch between **Clear Mode**, **Original**, **Clear B&W**, **Magic Color**, and **Grayscale**.
  - Instant real-time GPU **Brightness & Contrast** slider preview with non-destructive filter pipelines.
- **Sign PDF & PDF Toolkit**
  - Draw custom digital signatures and impose them directly onto scanned document pages or any imported PDF.
  - Merge multiple PDFs, Split PDF ranges, Compress PDF size, Add Diagonal Watermarks, Images to PDF, PDF to Images, and Text to PDF.
- **Offline Multi-Script ML Kit OCR**
  - Extract text across Latin, Devanagari, Chinese, Japanese, and Korean scripts.
  - Copy text, export `.txt`, or convert extracted OCR text into a formatted PDF.
- **Security & Cloud Sync**
  - Biometric / 4-digit PIN App Lock gate (`BiometricPrompt`).
  - Local Room database + optional Google Sign-In & Firebase Firestore cloud sync.

---

## GitHub Release & APK Builds

This repository includes an automated GitHub Actions CI/CD workflow (`.github/workflows/build-apk.yml`) that runs on every push and manual trigger:

1. Compiles both the signed **`Doc-Scan-v1.0.0-release.apk`** and **`Doc-Scan-v1.0.0-debug.apk`** using JDK 21 and Gradle 9.3.1.
2. Uploads the APKs as workflow artifacts.
3. Automatically creates/updates the **Doc Scan v1.0.0** release on the repository's **GitHub Releases** page with both `.apk` files attached.
