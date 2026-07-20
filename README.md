# 🌐 Android Internet Speed Test

**Aplikasi pengukuran performa koneksi internet** — Ping, Download Speed, dan Upload Speed — dibangun dengan **Jetpack Compose**, **Material 3**, dan arsitektur **MVVM**.

> **Author:** Kukuh Yudhistiro, S.Kom., M.Kom.  
> **Mata Kuliah:** Pemrograman Mobile Android — Semester 6  
> **Tech Stack:** Kotlin • Jetpack Compose • Material 3 • Coroutines/Flow • MVVM

---

##  Daftar Isi

1. [Gambaran Umum](#-gambaran-umum)
2. [Arsitektur & Struktur Project](#-arsitektur--struktur-project)
3. [Prasyarat](#-prasyarat)
4. [Panduan Setup: New Project → Siap Run](#-panduan-setup-new-project--siap-run)
5. [Penjelasan Komponen Utama](#-penjelasan-komponen-utama)
6. [Cara Kerja Pengukuran](#-cara-kerja-pengukuran)
7. [Customization & Pengembangan Lanjutan](#-customization--pengembangan-lanjutan)
8. [Troubleshooting](#-troubleshooting)
9. [Lisensi](#-lisensi)

---

## Gambaran Umum

Aplikasi ini mengukur tiga parameter koneksi internet:

| Parameter | Metode | Target |
|-----------|--------|--------|
| **Ping (Latency)** | ICMP ping via `Runtime.exec()` | google.com |
| **Download Speed** | HTTP GET stream dari Cloudflare CDN | ~10 MB test file |
| **Upload Speed** | HTTP POST dummy bytes ke Cloudflare | ~5 MB payload |

### Fitur UI

- ✅ **Circular Speedometer Gauge** — custom Canvas Compose dengan animasi smooth
- ✅ **Real-time speed update** — via StateFlow + Flow
- ✅ **Material 3 Design** — mendukung Light & Dark theme
- ✅ **Result Cards** — kartu hasil Ping/Download/Upload dengan warna aksen berbeda
- ✅ **Reactive State** — UI otomatis berubah sesuai fase: Idle → Ping → Download → Upload → Finished
- ✅ **Progress Indicator** — linear progress bar dan pulsating status text

---

## Arsitektur & Struktur Project

Menggunakan pola **MVVM (Model-View-ViewModel)** dengan **Unidirectional Data Flow (UDF)**:

```
┌─────────────┐     ┌──────────────────┐     ┌───────────────────┐
│   UI Layer  │ ←── │   ViewModel      │ ←── │   Data Layer      │
│  (Compose)  │     │  (StateFlow)     │     │  (SpeedTestMgr)   │
│             │ ──→ │                  │ ──→ │                   │
│ Observe     │     │ Process events   │     │ Network I/O       │
│ StateFlow   │     │ Update UiState   │     │ via Coroutines    │
└─────────────┘     └──────────────────┘     └───────────────────┘
     [Event]              [State]                  [Flow<T>]
```

### Struktur Folder

```
app/src/main/java/com/example/speedtest/
├── MainActivity.kt                         ← Entry point
├── data/
│   └── SpeedTestManager.kt                 ← Logika network (Ping/DL/UL)
├── model/
│   └── SpeedTestModels.kt                  ← Data classes & sealed classes
├── viewmodel/
│   └── SpeedTestViewModel.kt               ← State management
└── ui/
    ├── screen/
    │   └── SpeedTestScreen.kt               ← Layar utama
    ├── components/
    │   ├── SpeedGauge.kt                    ← Custom circular gauge (Canvas)
    │   └── ResultCard.kt                    ← Kartu hasil M3
    └── theme/
        ├── Color.kt                         ← Palet warna
        ├── Theme.kt                         ← Material 3 theme setup
        └── Type.kt                          ← Typography
```

---

## ✅ Prasyarat

Sebelum memulai, pastikan sudah terinstall:

| Software | Versi Minimum | Keterangan |
|----------|---------------|------------|
| **Android Studio** | Koala (2024.1) atau lebih baru | IDE utama |
| **JDK** | 17 | Biasanya bundled dengan Android Studio |
| **Android SDK** | API 35 (compileSdk) | Install via SDK Manager |
| **Kotlin Plugin** | 2.0.21 | Sudah termasuk di Android Studio terbaru |
| **Emulator / Device** | API 26+ (Android 8.0) | Untuk testing |

> ⚠️ **Penting:** Pastikan emulator/device terhubung ke internet untuk menjalankan speed test.

---

## Panduan Setup: New Project → Siap Run

Ikuti langkah-langkah berikut secara berurutan:

### Langkah 1: Buat New Project di Android Studio

1. Buka **Android Studio** → **File** → **New** → **New Project**
2. Pilih template: **Empty Activity** (yang berlabel "Compose")
3. Isi konfigurasi:
   - **Name:** `SpeedTest`
   - **Package name:** `com.example.speedtest`
   - **Save location:** pilih folder yang diinginkan
   - **Minimum SDK:** `API 26: Android 8.0 (Oreo)`
   - **Build configuration language:** `Kotlin DSL (Recommended)`
4. Klik **Finish** dan tunggu Gradle sync selesai

### Langkah 2: Konfigurasi Version Catalog

Buka file `gradle/libs.versions.toml` dan **ganti seluruh isinya** dengan konten dari file `libs.versions.toml` yang disediakan di project ini.

File ini mendefinisikan semua versi dependency secara terpusat:
- AGP 8.7.3
- Kotlin 2.0.21 + Compose Compiler Plugin
- Compose BOM 2024.12.01
- Material Icons Extended
- ViewModel Compose
- Coroutines Android

### Langkah 3: Konfigurasi Project-level build.gradle.kts

Buka file `build.gradle.kts` **di root project** (bukan yang di folder `app/`).
Pastikan isinya sesuai dengan file `build.gradle.kts` (project-level) yang disediakan:

```kotlin
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

### Langkah 4: Konfigurasi App-level build.gradle.kts

Buka file `app/build.gradle.kts` dan **ganti seluruh isinya** dengan konten dari file `app/build.gradle.kts` yang disediakan.

Perhatikan dependency penting yang ditambahkan:
```kotlin
// Material Icons Extended (untuk ikon Ping/Download/Upload)
implementation(libs.androidx.material.icons.extended)

// ViewModel + Compose integration
implementation(libs.androidx.lifecycle.viewmodel.compose)
implementation(libs.androidx.lifecycle.runtime.compose)

// Coroutines
implementation(libs.kotlinx.coroutines.android)
```

### Langkah 5: Konfigurasi settings.gradle.kts

Buka `settings.gradle.kts` di root project dan pastikan:
```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SpeedTest"
include(":app")
```

### Langkah 6: Konfigurasi gradle.properties

Buka `gradle.properties` dan pastikan flag berikut ada:
```properties
android.useAndroidX=true
kotlin.code.style=official
android.nonTransitiveRClass=true
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
```

### Langkah 7: Sync Gradle

Klik **Sync Now** pada notification bar di atas editor, atau:
**File** → **Sync Project with Gradle Files**

Tunggu hingga sync berhasil (centang hijau di Build tab).

> 💡 Jika ada error versi, pastikan SDK API 35 sudah terinstall:
> **Tools** → **SDK Manager** → centang **Android API 35** → **Apply**

### Langkah 8: Salin Source Code

Salin seluruh file Kotlin dari project ini ke dalam project Android Studio:

1. **Buat package structure** di `app/src/main/java/com/example/speedtest/`:
   - Klik kanan pada package `com.example.speedtest` → **New** → **Package**
   - Buat: `data`, `model`, `viewmodel`, `ui.screen`, `ui.components`, `ui.theme`

2. **Salin file-file berikut** (ganti file yang sudah ada jika perlu):

   | File Sumber | Tujuan di Project |
   |-------------|-------------------|
   | `model/SpeedTestModels.kt` | `com.example.speedtest.model` |
   | `data/SpeedTestManager.kt` | `com.example.speedtest.data` |
   | `viewmodel/SpeedTestViewModel.kt` | `com.example.speedtest.viewmodel` |
   | `ui/theme/Color.kt` | `com.example.speedtest.ui.theme` |
   | `ui/theme/Type.kt` | `com.example.speedtest.ui.theme` |
   | `ui/theme/Theme.kt` | `com.example.speedtest.ui.theme` |
   | `ui/components/SpeedGauge.kt` | `com.example.speedtest.ui.components` |
   | `ui/components/ResultCard.kt` | `com.example.speedtest.ui.components` |
   | `ui/screen/SpeedTestScreen.kt` | `com.example.speedtest.ui.screen` |
   | `MainActivity.kt` | `com.example.speedtest` |

3. **Hapus** file `ui/theme/Color.kt`, `Theme.kt`, dan `Type.kt` yang di-generate otomatis oleh Android Studio (jika ada), lalu ganti dengan versi dari project ini.

### Langkah 9: Konfigurasi AndroidManifest.xml

Buka `app/src/main/AndroidManifest.xml` dan pastikan dua permission ini ada **sebelum tag `<application>`**:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
```

Dan tambahkan atribut pada tag `<application>`:
```xml
android:usesCleartextTraffic="true"
```

### Langkah 10: Konfigurasi Resources

1. Buka `app/src/main/res/values/strings.xml`:
   ```xml
   <resources>
       <string name="app_name">Speed Test</string>
   </resources>
   ```

2. Buka `app/src/main/res/values/themes.xml` dan pastikan:
   ```xml
   <style name="Theme.SpeedTest" parent="android:Theme.Material.Light.NoActionBar">
       <item name="android:statusBarColor">@android:color/transparent</item>
       <item name="android:navigationBarColor">@android:color/transparent</item>
   </style>
   ```

### Langkah 11: Build & Run

1. Pilih device/emulator target di toolbar atas
2. Klik **Run** (▶️) atau tekan `Shift + F10`
3. Tunggu build selesai dan aplikasi terbuka di device
4. Tekan tombol **"Mulai Test"** dan lihat hasilnya!

---

## 🔧 Penjelasan Komponen Utama

### 1. SpeedTestManager (Data Layer)

Bertanggung jawab atas seluruh operasi jaringan. Setiap metode mengembalikan `Flow<T>` yang di-collect oleh ViewModel.

**Ping:**
```
Runtime.exec("ping -c 3 google.com")
  → Parse output dengan Regex: time=(\d+\.?\d*)\s*ms
  → Ambil setiap RTT individual
  → Hitung rata-rata: avg = sum(rtt) / count
```

**Download:**
```
HttpURLConnection GET → speed.cloudflare.com/__down?bytes=10000000
  → Baca stream per 8KB buffer
  → Hitung: speedMbps = (totalBytes × 8) / (elapsedSec × 1.000.000)
  → Emit progress setiap 150ms
```

**Upload:**
```
HttpURLConnection POST → speed.cloudflare.com/__up
  → Kirim dummy 5MB secara chunked (8KB per write)
  → Hitung: speedMbps = (totalBytes × 8) / (elapsedSec × 1.000.000)
  → Emit progress setiap 150ms
```

### 2. SpeedTestViewModel (ViewModel Layer)

- Mengelola `MutableStateFlow<SpeedTestUiState>` sebagai single source of truth
- Menjalankan tiga tes secara sekuensial dalam `viewModelScope`
- Menggunakan `update { }` untuk modifikasi state yang thread-safe
- Meng-collect Flow dari SpeedTestManager dan memetakan hasilnya ke UiState

### 3. SpeedGauge (UI Component)

Custom circular gauge menggunakan **Canvas Compose**:
- Arc background (track): 270° sweep dari sudut 135° ke 405°
- Arc progress: proporsional terhadap kecepatan / maxSpeed
- Tick marks: 10 segmen utama dengan garis besar/kecil
- Needle: garis dari pusat ke tepi arc sesuai fraksi kecepatan
- Gradient warna: Cyan → Green → Amber → Red berdasarkan kecepatan

### 4. SpeedTestScreen (UI Layer)

Layar utama yang menyusun semua komponen:
- `SpeedGauge` untuk visualisasi real-time
- `LinearProgressIndicator` untuk progress keseluruhan
- `ResultCardsRow` untuk menampilkan hasil akhir
- `Button` yang berubah state (Mulai → Testing → Test Ulang)
- Semua data di-observe dari ViewModel via `collectAsState()`

---

##  Cara Kerja Pengukuran

### Rumus Konversi Kecepatan

```
                    totalBytes × 8
speed (Mbps) = ─────────────────────
                elapsedSeconds × 1.000.000

Di mana:
  totalBytes     = jumlah byte yang sudah ditransfer
  × 8            = konversi Bytes → Bits (1 Byte = 8 Bits)
  elapsedSeconds = waktu yang sudah berlalu (dari System.nanoTime())
  × 1.000.000    = konversi Bits → Megabits (1 Mbit = 1.000.000 bits)
```

### Contoh Kalkulasi

Download 2.500.000 bytes dalam 1,5 detik:
```
speed = (2.500.000 × 8) / (1,5 × 1.000.000)
      = 20.000.000 / 1.500.000
      = 13,33 Mbps
```

### Parsing Output Ping

Output command `ping -c 3 google.com`:
```
64 bytes from 142.250.80.46: icmp_seq=1 ttl=116 time=10.1 ms
64 bytes from 142.250.80.46: icmp_seq=2 ttl=116 time=11.2 ms
64 bytes from 142.250.80.46: icmp_seq=3 ttl=116 time=9.8 ms
```

Regex `time=(\d+\.?\d*)\s*ms` menangkap:
- Group[1] dari baris 1: `10.1`
- Group[1] dari baris 2: `11.2`
- Group[1] dari baris 3: `9.8`

Rata-rata: `(10.1 + 11.2 + 9.8) / 3 = 10.37 ms`

---

##  Customization & Pengembangan Lanjutan

### Mengganti Test Server

Di `SpeedTestManager.kt`, ubah konstanta:
```kotlin
// Download — ganti dengan server lain jika Cloudflare tidak responsif
private const val DOWNLOAD_URL = "https://proof.ovh.net/files/10Mb.dat"

// Upload — alternatif endpoint
private const val UPLOAD_URL = "https://httpbin.org/post"
```

### Menambah Ukuran Test File

```kotlin
// Untuk test yang lebih akurat, gunakan file lebih besar:
private const val DOWNLOAD_URL =
    "https://speed.cloudflare.com/__down?bytes=25000000"  // 25MB

private const val UPLOAD_SIZE = 10_000_000  // 10MB
```

### Mengganti Max Speed di Gauge

Di `SpeedTestScreen.kt`, fungsi `resolveMaxSpeed()`:
```kotlin
private fun resolveMaxSpeed(state: SpeedTestUiState): Float {
    return when (state.phase) {
        TestPhase.TESTING_PING -> 500f    // Max ping 500ms
        else -> 500f                       // Max speed 500 Mbps
    }
}
```

### Menambah Dependency Injection (Hilt)

Untuk mengikuti best practice arsitektur:
1. Tambahkan dependency Hilt di `build.gradle.kts`
2. Anotasi `SpeedTestManager` dengan `@Singleton`
3. Anotasi `SpeedTestViewModel` dengan `@HiltViewModel`
4. Inject `SpeedTestManager` via constructor

### Fitur Lanjutan yang Bisa Ditambahkan

- [ ] **History** — Simpan hasil test ke Room Database
- [ ] **Grafik trend** — Tampilkan riwayat kecepatan dalam chart
- [ ] **Server selection** — Pilih server test terdekat
- [ ] **Jitter measurement** — Variasi latency antar-paket
- [ ] **Network info** — Tampilkan tipe koneksi (WiFi/Cellular), SSID, dll.
- [ ] **Export hasil** — Share atau export ke CSV/PDF
- [ ] **Widget** — Home screen widget untuk quick test

---

##  Troubleshooting

### Build Error: "Unresolved reference"

**Penyebab:** Package import tidak sesuai.
**Solusi:** Pastikan semua file Kotlin berada di package `com.example.speedtest.*` yang benar. Cek baris `package` di awal setiap file.

### Ping Gagal di Emulator

**Penyebab:** Beberapa emulator/ROM memblokir ICMP ping via Runtime.
**Solusi:** Test di device fisik, atau modifikasi `measurePing()` untuk menggunakan HTTP HEAD request sebagai fallback:

```kotlin
// Fallback: HTTP HEAD request ke google.com
val startTime = System.nanoTime()
val url = URL("https://www.google.com")
val conn = url.openConnection() as HttpURLConnection
conn.requestMethod = "HEAD"
conn.connectTimeout = 5000
conn.connect()
val elapsed = (System.nanoTime() - startTime) / 1_000_000.0  // ms
conn.disconnect()
```

### Download/Upload Speed = 0 Mbps

**Penyebab:** Endpoint Cloudflare tidak responsif atau diblokir.
**Solusi:**
1. Pastikan device terhubung ke internet
2. Coba ganti URL ke alternatif (lihat bagian Customization)
3. Periksa apakah ada firewall/VPN yang memblokir

### Dark Theme Tidak Muncul

**Penyebab:** System theme tidak diset ke dark mode.
**Solusi:** Ubah pengaturan device: **Settings** → **Display** → **Dark theme** → **On**
Atau force dark theme di kode:
```kotlin
SpeedTestTheme(darkTheme = true) { ... }
```

### Gradle Sync Failed

**Penyebab:** Versi SDK atau plugin tidak cocok.
**Solusi:**
1. **Tools** → **SDK Manager** → Install **Android API 35**
2. Pastikan `gradle-wrapper.properties` menggunakan Gradle 8.9+:
   ```properties
   distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
   ```
3. **File** → **Invalidate Caches** → **Invalidate and Restart**

---

## Lisensi

Project ini dibuat untuk keperluan pembelajaran mata kuliah **Pemrograman Mobile Android** Semester 6.

© 2025 Kukuh Yudhistiro, S.Kom., M.Kom. — Program Studi S1 Sistem Informasi
