# Speedometer Pro — Catatan Sesi (jangan dihapus)

Dokumen ini menyimpan konteks percakapan/openai sesi pengembangan biar tidak hilang.
Simpan bersama source code project ini.

## 1. Konteks / tujuan user

- User punya video TikTok @touring.go berisi demo app "Touring Go": map 3D Google Maps asli,
  tracking rider dengan animasi motor mengikuti rute GMaps, kamera, button jalur/rute, chat,
  PTT (push-to-talk), open mic, "terdekat" (cari jalan alternatif), indikator arah+jarak sisa,
  kecepatan, jarak tempuh, estimasi waktu.
- User bertanya kenapa app itu bisa realtime & maps-nya real. Jawaban: Google Maps SDK/API
  (data asli + lisensi API key) + GPS untuk posisi/kecepatan + cloud (Firebase/WebSocket) untuk
  sinkronisasi antar rombongan + WebRTC/Agora untuk PTT. Sebagian besar masuk free tier
  ($200/bulan Google Maps Platform, Spark plan Firebase).
- User lalu ingin memperbaiki app **Speedometer Pro** lamanya yang TIDAK AKURAT:
  kecepatan & jarak tidak cocok dengan kenyataan. Fitur lama: spedo & racebox.

## 2. Nasib project lama

- Project lama "SpeedometerApp" hanya tersisa kerangka folder KOSONG di
  `C:\Users\user\AppData\Local\Temp\opencode\SpeedometerApp` (0 file — source terhapus).
- Kesimpulan: jangan pernah taruh project di folder Temp; pakai lokasi permanen.

## 3. Project baru (ini)

- Lokasi: **`D:\Projects\SpeedometerPro`** (permanen, bukan Temp).
- Package: `com.speedometer.app`, minSdk 24, targetSdk 34, compileSdk 35.
- Stack: Kotlin 1.9.22, AGP 8.7.3, Gradle 8.10.2, Room 2.6.1 (KSP),
  play-services-location 21.0.1, Material 3, ViewBinding, XML layouts (bukan Compose).
- Android SDK di `C:\Users\user\AppData\Local\Android\Sdk\Sdk`.
- Gradle lokal: `C:\Users\user\.gradle\wrapper\dists\gradle-8.10.2-all\7iv73wktx1xtkvlq19urqw1wm\gradle-8.10.2\bin\gradle.bat`

### Struktur kode penting
```
app/src/main/java/com/speedometer/app/
├── MainActivity.kt               (izin runtime + BottomNav: Spedo/Racebox/Riwayat/Setelan)
├── SpeedometerApp.kt             (Application, TripRepository)
├── gps/
│   ├── TripTracker.kt            ★ MESIN AKURASI (inti)
│   ├── GpsService.kt             (foreground service, HIGH_ACCURACY, 1 dtk)
│   ├── GpsStateHolder.kt         (StateFlow singleton antar service-UI)
│   └── SpeedSmoother.kt          (EMA tampilan kecepatan)
├── data/db/                      (Room: Trip, TripDao, AppDatabase)
├── data/repository/TripRepository.kt
├── ui/speedometer/  ui/racebox/  ui/history/  ui/settings/
└── util/Format.kt
```

## 4. Kenapa app lama tidak akurat & solusi yang dipasang (PENTING)

| Masalah lama | Solusi di TripTracker |
|---|---|
| Kecepatan dihitung dari beda posisi GPS | Pakai **Doppler speed** `Location.getSpeed()` × 3.6. Fallback haversine/dt HANYA jika segmen bergerak nyata (≥ minSegment 1,5 m), biar diam → 0. |
| Jarak ngaco / phantom meter saat diam | Perpindahan < 1,5 m TIDAK dihitung; akurasi > 20 m ditolak; jeda antar fix > 10 s tidak dihitung; lompatan mustahil (teleport tanpa konfirmasi speed) ditolak. |
| Angka bergetar | `SpeedSmoother` EMA ringan (alpha 0,25 stabil / 0,6 saat perubahan besar). |
| Realtime | `GpsService` foreground + wakelock, priority HIGH_ACCURACY, min update 500 ms, `setWaitForAccurateLocation(true)`. |

Konstanta di `TripTracker`: `maxAccuracyM=20`, `minSegmentM=1.5`, `maxGapMs=10_000`, `maxTeleportSpeedMs=60`.

## 5. Build & test

- Build debug: `gradle.bat -p D:\Projects\SpeedometerPro assembleDebug`
- Unit test: `gradle.bat -p D:\Projects\SpeedometerPro testDebugUnitTest`
  → `app/src/test/java/com/speedometer/app/gps/TripTrackerTest.kt` (9 test, semua PASS)
- APK: `D:\Projects\SpeedometerPro\app\build\outputs\apk\debug\app-debug.apk`
- Instal ke HP: `adb install -r <apk>` (adb di `...\platform-tools\adb.exe`)

## 6. Update v2 (perbaikan delay & selisih)

Keluhan user setelah test riding: pergerakan spedo agak delay, dan ada selisih
kecepatan dengan speedometer motor. Yang diubah:

1. **Interval GPS lebih cepat**: 1000 ms → **500 ms** (min update 250 ms),
   `setWaitForAccurateLocation(true)` dihapus (menambah latensi).
   → `GpsService.requestUpdates()`
2. **Smoothing lebih responsif**: `SpeedSmoother` baru — alpha adaptif
   (slow 0.4 / mid 0.7 / fast 0.9) berdasar besarnya perubahan, sehingga
   naik/turun kecepatan nyaris instan tanpa angka bergetar.
3. **Fallback window 5 → 3** di `TripTracker` (sumber delay saat perangkat
   tidak mengirim Doppler speed).
4. **Kalibrasi kecepatan** (baru): slider 100–120% di tab Setelan.
   - `util/UserPrefs.kt` (read/write), key `calibration` di prefs `settings`.
   - Diterapkan di SpeedometerFragment & RaceboxFragment pada tampilan
     kecepatan & max (jarak/waktu tidak ikut).
   - 100% = kecepatan asli GPS. Naikkan kalau mau disamakan dengan angka
     spidometer motor (yang biasanya 5–10% lebih tinggi).
   - Toggle mph juga sudah benar-benar diterapkan di tampilan.

Catatan: selisih antara GPS dan spidometer motor SIFATNYA normal (spidometer
pabrikan memang di-set lebih tinggi). GPS = kecepatan asli. Kalau mau persis
dengan spidometer, pakai kalibrasi.

## 7. Status sesi terakhir

- v2 sudah di-build (`app-debug.apk` ±7,45 MB), test 9/9 lulus.
- User akan debug ulang sendiri ke HP-nya (HP tidak terpasang saat v2 selesai).
- APK lama sudah terinstal di Infinix X678B via adb (device 102682537R000692).
  Instal v2: `adb install -r D:\Projects\SpeedometerPro\app\build\outputs\apk\debug\app-debug.apk`

## 8. Bila lanjut nanti (roadmap)
- Tampilan map GPS (Google Maps SDK) ala Touring Go.
- Trip detail (grafik kecepatan), export GPX.
- Estimasi waktu (ETA) & avg speed yang sudah ada sebagian di History.
