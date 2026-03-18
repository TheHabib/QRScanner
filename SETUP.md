# Setup Guide — Fixing Gradle Wrapper

The `gradle-wrapper.jar` binary is not included in this zip (it's a compiled Java binary
that can't be stored in source control meaningfully). You need it once before Android Studio
can sync. There are two easy ways:

---

## Option A — Let Android Studio generate it (Easiest)

1. Open the project in Android Studio: `File → Open → QRScanner`
2. Android Studio will show a banner: **"Gradle wrapper not found"** or similar
3. Click **"Use Gradle wrapper"** or **"OK"** — it will auto-generate the JAR
4. Sync will complete successfully

If that doesn't appear automatically:
- Go to `File → Settings → Build, Execution, Deployment → Build Tools → Gradle`
- Under **Gradle JDK**, make sure a valid JDK is selected (17 or 21 recommended)
- Click `File → Invalidate Caches → Invalidate and Restart`

---

## Option B — Run the Python helper script

If you have Python 3 and internet access:
```bash
cd QRScanner
python3 download_wrapper.py
```
Then open in Android Studio normally.

---

## Option C — Copy from another Android project

If you have any other Android project on your machine:
```
copy  <other-project>/gradle/wrapper/gradle-wrapper.jar
  to  QRScanner/gradle/wrapper/gradle-wrapper.jar
```
All Android projects use the same `gradle-wrapper.jar` regardless of Gradle version.

---

## Gradle Version Compatibility

This project uses:
| Tool | Version |
|------|---------|
| Gradle | **8.6** |
| Android Gradle Plugin (AGP) | **8.3.2** |
| Kotlin | **1.9.22** |
| Android Studio | **Hedgehog (2023.1.1)** or newer |

These versions are fully compatible with each other and resolve the `HasConvention` error.
