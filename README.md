# QR & Barcode Scanner — Android App (Kotlin)

A feature-rich QR and Barcode scanner built with **CameraX** and **ML Kit**, following Material Design on a dark theme.

---

## Features

### Scanner Screen
- **Live camera preview** — full-screen with a centered scan frame (corner brackets)
- **Animated scan line** — green line that sweeps up and down inside the frame
- **Auto-detect** — scans as soon as a code enters the frame; frame turns green on success
- **Top bar icons:**
  - ☰ Hamburger → opens left-side navigation drawer
  - 🖼 Gallery → pick image from gallery and scan it
  - ⚡ Torch → toggle flashlight (disabled automatically on front camera)
  - 🔄 Flip → switch between front and back camera
- **Zoom bar** — bottom seekbar to zoom in/out smoothly

### Result Screen
Displays full decoded data with smart context:

| Code Type | Action Buttons |
|-----------|----------------|
| URL | Open · Share · Copy |
| Email | Send Email · Share · Copy |
| Phone | Call · Share · Copy |
| SMS | Message · Share · Copy |
| Location | Maps · Share · Copy |
| Contact | Add Contact · Share · Copy |
| Calendar | Add Event · Share · Copy |
| Text | Open · Share · Copy |
| **WiFi** | **Connect · Share · Copy · Copy Password** |

**WiFi extras:**
- Details card showing SSID, security type, password (hidden by default with eye toggle)
- Connect button adds the network using WifiNetworkSuggestion (Android 10+) or WifiConfiguration (Android 9-)
- Copy Password button copies just the password

### Navigation Drawer
- Scan History *(placeholder)*
- Create QR Code *(placeholder)*
- Settings *(placeholder)*
- About

---

## Tech Stack

| Component | Library |
|-----------|---------|
| Camera | CameraX 1.3.1 |
| Barcode scanning | ML Kit Barcode Scanning 17.2.0 |
| UI | Material Components 1.11.0 |
| View binding | AndroidX ViewBinding |
| Async | Kotlin Coroutines |
| Parceling | kotlin-parcelize |

---

## Setup

### Requirements
- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Kotlin 1.9.0
- Java 8+
- Physical device recommended (camera required)

### Steps

1. **Open in Android Studio**
   ```
   File → Open → select the QRScanner folder
   ```

2. **Sync Gradle**
   Android Studio will prompt to sync. Click **Sync Now**.

3. **Run on device**
   - Connect an Android device (API 24+) via USB with Developer Options enabled
   - Click **Run ▶**
   - Grant camera permission when prompted

### Minimum SDK
- **minSdk 24** (Android 7.0 Nougat)
- **targetSdk 34** (Android 14)

---

## Project Structure

```
app/src/main/
├── java/com/example/qrscanner/
│   ├── model/
│   │   └── ScanResult.kt          # Data models (ScanResult, WifiData, ScanResultType)
│   ├── ui/
│   │   ├── scanner/
│   │   │   └── ScannerActivity.kt # Camera + scanning logic
│   │   └── result/
│   │       └── ResultActivity.kt  # Result display + actions
│   └── utils/
│       └── BarcodeParser.kt       # Converts ML Kit Barcode → ScanResult
├── res/
│   ├── layout/
│   │   ├── activity_scanner.xml   # Main camera screen with DrawerLayout
│   │   ├── activity_result.xml    # Result screen
│   │   └── nav_header.xml         # Drawer header
│   ├── drawable/                  # 35+ vector icons + scan frame assets
│   ├── animator/
│   │   └── scan_line_anim.xml     # Scan line bounce animation
│   ├── menu/
│   │   └── nav_menu.xml           # Drawer menu items
│   └── values/
│       ├── colors.xml             # Dark theme color palette
│       ├── strings.xml
│       └── themes.xml
└── AndroidManifest.xml
```

---

## Permissions Used

| Permission | Reason |
|-----------|--------|
| `CAMERA` | Live scanning |
| `FLASHLIGHT` | Torch toggle |
| `READ_MEDIA_IMAGES` | Gallery scanning (Android 13+) |
| `READ_EXTERNAL_STORAGE` | Gallery scanning (Android ≤12) |
| `CHANGE_WIFI_STATE` | Connect to WiFi networks |
| `ACCESS_WIFI_STATE` | Read WiFi status |
| `ACCESS_FINE_LOCATION` | Required by Android for WiFi API |
| `ACCESS_NETWORK_STATE` | Network connectivity checks |

---

## Supported Code Formats

QR Code, Aztec, Code 128, Code 39, Code 93, EAN-8, EAN-13, ITF, UPC-A, UPC-E, PDF417, Data Matrix

---

## Extending the App

The placeholder menu items (History, Create QR, Settings) are ready for implementation:

- **History** — persist `ScanResult` objects using Room database; list them in a `RecyclerView`
- **Create QR** — use `zxing` or `qrcode-kotlin` library to generate QR images
- **Settings** — use `PreferenceFragmentCompat` for scan sound, vibration, auto-open, etc.
