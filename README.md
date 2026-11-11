# TITAN PRO GOV v2.1.8 CALIBRATED — Android APK

TITAN PRO GOV **Calibrated + Neon Studio + Governor + Core + Learning + Paper Trading** as a **self-contained Android APK**.

- Full modular Flask engine (numpy, pandas, flask, waitress) bundled via **Chaquopy**
- Data locked to **Internal Storage / XXX**
- Analysis-only crypto multi-timeframe dashboard + Studio + Calibration
- **New neon icon** (bright cyan T glow)

## Build APK

1. Upload the source ZIP `TITAN_PRO_GOV_v2_1_8_CALIBRATED.zip` (or any name like `Titan.zip`) to the **root** of this repository
2. Go to **Actions** → **Build Titan PRO GOV v2.1.8 APK** → **Run workflow**
3. Wait ~15–25 min
4. Download the APK from **Artifacts** or the **Release**

## Install

1. Uninstall any previous Titan app
2. Install the APK
3. Grant **All files access** (Android 11+)
4. Wait 30–90 seconds for the engine to boot
5. Data & logs appear in `/storage/emulated/0/XXX/`

## Source

The original Python source is in the uploaded ZIP.  
The workflow unpacks it into `app/src/main/python/` at build time.
