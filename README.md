# Nusa

Nusa adalah **Android local music player** yang dirancang untuk pemutaran musik lokal/offline dengan antarmuka yang bersih, vinyl player yang realistis, dan kontrol playback yang halus.

> **Project status:** Active development / pre-release  
> **Current version:** `3.9.2`  
> **versionCode:** `36`  
> **Branch:** `main`  
> **Last updated:** 6 October 2026

---

## Sejauh Mana Project Ini Berjalan?

Nusa sudah berkembang jauh dari prototype awal. **Fungsi inti music player sudah berjalan**, termasuk pemutaran musik lokal, background playback, MediaSession, library musik, folder scanning, shuffle/repeat, playback persistence, crossfade, metadata audio, artist information, serta pengaturan tampilan.

Saat ini project berada pada tahap **advanced development / pre-release validation**.

Artinya, fondasi aplikasi sudah terbentuk dan dapat digunakan, sedangkan pengembangan berikutnya lebih banyak berfokus pada:

- stabilitas playback
- penyempurnaan UI/UX
- optimasi performa
- regression testing
- penyempurnaan fitur audio
- persiapan release production

## Progress Utama

| Area | Status | Keterangan |
|---|---|---|
| Local music playback | ✅ | Pemutaran musik lokal |
| Background playback | ✅ | Playback tetap berjalan saat aplikasi di background |
| MediaSession | ✅ | Integrasi kontrol media Android |
| Notification controls | ✅ | Kontrol playback melalui media notification |
| Shuffle | ✅ | Mode acak |
| Repeat | ✅ | Repeat mode Media3 |
| Crossfade | ✅ | Transisi antar lagu 1–12 detik |
| Crossfade queue | ✅ | Next / Previous tetap bekerja setelah handoff |
| Playback persistence | ✅ | Lagu, posisi, shuffle dan repeat disimpan |
| MediaStore scanning | ✅ | Memindai musik yang tersedia di MediaStore |
| Folder scanning | ✅ | Folder lokal dapat dipilih manual |
| Recursive scanning | ✅ | Subfolder ikut dipindai |
| Minimum track filter | ✅ | Lagu < 10 detik dikeluarkan |
| Automatic library refresh | ✅ | Perubahan MediaStore dipantau |
| Album artwork | ✅ | Artwork loading + caching |
| Main player | ✅ | Player utama dengan desain vinyl |
| Vinyl animation | ✅ | Rotasi halus dengan tonearm |
| Song library | ✅ | Mode list |
| Active song indicator | ✅ | Highlight lembut pada lagu aktif |
| A–Z index | ✅ | Tap + drag untuk navigasi alfabet |
| Sorting | ✅ | Title, Artist, Recently Added, Album, Duration |
| Artist portrait | ✅ | Lookup + caching |
| Artist biography | ✅ | Last.fm, bahasa Indonesia |
| Multi-artist handling | ✅ | Artist pertama digunakan untuk lookup |
| Share | ✅ | Berbagi informasi lagu |
| Favorite | ✅ | Favorite control |
| Lossless / Hi-Res badge | ✅ | Berdasarkan metadata codec dan bit depth |
| Hi-Res information dialog | ✅ | Informasi kualitas audio |
| System title fonts | ✅ | Beberapa pilihan font sistem |
| Custom title font | ✅ | Mendukung `.TTF` dan `.OTF` |
| Device language | ✅ | Indonesian / English mengikuti bahasa perangkat |
| USB DAC playback | ⏸️ | Saat ini belum aktif pada build utama |
| Bit-perfect validation | ⏳ | Belum dilakukan |
| Production release | ⏳ | Masih dalam tahap validasi |

---

# UI Saat Ini

## Main Player

Halaman utama menggunakan vinyl sebagai elemen utama player.

Fitur visual yang sudah tersedia:

- realistic rotating vinyl
- physical shadow
- visible vinyl thickness
- pressed grooves
- PVC grain
- micro-scuff dan hairline details
- reflected light pada permukaan vinyl
- album artwork pada label vinyl
- tonearm yang mengikuti posisi playback
- song title
- artist name
- progress bar
- playback controls
- Lossless / Hi-Res badge
- artist portrait
- artist biography
- settings

Vinyl masih terus disempurnakan secara visual agar pantulan cahaya dan materialnya semakin mendekati vinyl fisik.

## Song Library

Library sudah diubah dari grid menjadi **list view**.

Setiap item menampilkan:

```text
[Artwork]  Song Title
           Artist
```

Lagu yang sedang aktif diberi **active-tab style** dengan background ber-opacity rendah sehingga tidak terlalu mencolok.

Background library menggunakan warna yang sama dengan halaman utama.

### A–Z Index

Index alfabet berada di sisi kanan dan dapat digunakan dengan:

- tap huruf
- drag secara vertikal

Navigasi dibuat langsung agar responsif ketika pengguna menggeser index.

### Sorting

Pilihan sorting saat ini:

```text
Title A–Z
Title Z–A
Artist A–Z
Recently Added
Album A–Z
Shortest duration
Longest duration
```

---

# Music Scanning

Nusa memiliki dua sumber library:

### 1. Android MediaStore

Musik yang sudah diindeks Android dapat dibaca langsung oleh Nusa.

### 2. Manual Folder Access

Pengguna dapat memilih folder musik melalui Android Storage Access Framework.

Folder dipindai secara recursive sehingga musik di dalam subfolder juga dapat ditemukan.

Format audio yang dikenali:

```text
AAC
ALAC
FLAC
M4A
MP3
OGG
OPUS
WAV
WMA
```

Lagu dengan durasi di bawah **10 detik** tidak dimasukkan ke library.

---

# Playback & Crossfade

Playback menggunakan **AndroidX Media3 / ExoPlayer**.

Crossfade menggunakan dua player selama transisi:

```text
Current song
    ↓
Primary player  → fade out
                     ↘
                      overlap
                     ↗
Secondary player → fade in
    ↓
Seamless handoff
```

Durasi crossfade dapat diatur dari:

```text
1–12 seconds
Default: 5 seconds
```

Queue playlist dipulihkan setelah handoff agar:

- Next tetap bekerja
- Previous tetap bekerja
- Repeat tetap bekerja
- Shuffle order tetap dipertahankan

Crossfade sudah melalui beberapa perbaikan stabilitas, termasuk perbaikan kasus **Previous setelah lagu hasil crossfade selesai**.

---

# Playback Persistence

Nusa menyimpan:

- current media ID
- playback position
- play/pause state
- shuffle mode
- repeat mode

Saat aplikasi dibuka kembali, playback dapat dilanjutkan dari lagu dan posisi terakhir yang tersimpan.

---

# Artist Information

## Artist Biography

Biografi artis menggunakan Last.fm.

Untuk saat ini:

- biography diarahkan ke bahasa Indonesia
- tidak fallback ke biography bahasa Inggris
- artist pertama digunakan untuk lookup ketika metadata memiliki beberapa artist
- biography panjang dapat discroll di dalam panel

API key disimpan melalui `local.properties`:

```properties
LASTFM_API_KEY=YOUR_LASTFM_API_KEY
```

Jangan commit `local.properties` ke repository.

## Artist Portrait

Artist portrait menggunakan lookup artist dan caching lokal.

Pada metadata multi-artist, artist pertama digunakan sebagai sumber pencarian portrait.

---

# Audio Quality

Nusa membaca metadata audio untuk menampilkan indikasi:

```text
Lossless
Hi-Res
```

Contoh klasifikasi:

```text
Apple Lossless → Lossless / Hi-Res
FLAC           → Lossless / Hi-Res
```

Saat ini:

```text
< 24-bit → Lossless
≥ 24-bit → Hi-Res
```

**Catatan:** badge Hi-Res merupakan klasifikasi berdasarkan metadata file. Badge tersebut tidak otomatis berarti output Android sedang berjalan secara bit-perfect.

---

# Custom Title Font

Nusa sudah mendukung penggantian font khusus untuk **judul lagu pada halaman utama**.

Font sistem yang tersedia:

```text
Default
Sans Serif
Serif
Monospace
Cursive
```

Selain itu pengguna dapat memilih file font sendiri:

```text
.TTF
.OTF
```

Font custom disalin ke storage internal aplikasi dan dapat digunakan kembali setelah aplikasi dibuka ulang.

---

# Performance

Optimasi performa sudah dilakukan pada beberapa bagian utama:

- Lazy list rendering
- stable item keys
- artwork caching
- background artwork loading
- downsampled artwork decoding
- reduced unnecessary recomposition
- pager optimization
- optimized A–Z scrolling
- lightweight custom vinyl rendering
- Media3 buffering configuration

Fokus berikutnya adalah menjaga kestabilan dan frame-rate saat digunakan dengan library musik yang besar.

---

# Hi-Res / USB DAC

Integrasi USB Hi-Res menggunakan engine eksternal sempat diuji di repository, tetapi **jalur USB playback saat ini dipause dan tidak menjadi bagian dari build utama Nusa 3.9.2**.

Karena itu Nusa saat ini **belum mengklaim USB DAC atau bit-perfect playback sebagai fitur production-ready**.

Statusnya:

```text
USB engine experiment → Pernah diintegrasikan
Current main build    → Tidak aktif
Hardware validation   → Belum dilakukan
```

Pengembangan USB DAC dapat dilanjutkan setelah core playback dan UI dianggap cukup stabil.

---

# Known Remaining Work

Project belum mencapai final release.

Pekerjaan yang masih tersisa:

### UI / UX

Penyempurnaan visual vinyl, reflection, typography dan spacing masih berlangsung.

### Recently Added persistence

Metadata `dateAdded` sudah dikumpulkan saat scanning. Persistence pada cache library masih perlu dirapikan agar sorting **Recently Added** tetap konsisten setelah restart.

### Audio / USB

Jalur USB DAC perlu diuji kembali pada hardware nyata sebelum dapat dianggap production-ready.

### Release hardening

Masih diperlukan pengujian:

- beberapa perangkat Android
- library musik besar
- playback jangka panjang
- permission recovery
- crossfade regression
- folder scanning edge cases
- release APK

---

# Technology Stack

- Kotlin
- Jetpack Compose
- Material 3
- AndroidX Media3
- ExoPlayer
- MediaSession
- Kotlin Coroutines
- Android MediaStore
- Storage Access Framework
- Last.fm API
- Deezer artist lookup

## Android Configuration

```text
compileSdk  = 36
targetSdk   = 36
minSdk      = 29
Java        = 17
Media3      = 1.9.3
Compose BOM = 2025.10.01
```

---

# Project Structure

```text
app/
└─ src/main/kotlin/com/yohandeku32/nusamusic/
   ├─ MainActivity.kt
   ├─ data/
   │  ├─ ArtworkLoader.kt
   │  ├─ ArtistImageLoader.kt
   │  ├─ ArtistBiographyLoader.kt
   │  ├─ AudioCodecLoader.kt
   │  └─ MusicRepository.kt
   ├─ model/
   │  └─ Song.kt
   └─ playback/
      └─ PlaybackService.kt
```

Package:

```text
com.yohandeku32.nusamusic
```

---

# Development Stage

Saat ini Nusa berada pada:

> **Advanced development / pre-release validation**

Gambaran keseluruhan:

```text
Core playback         → ✅
Background playback   → ✅
MediaSession           → ✅
Library               → ✅
Folder scanning       → ✅
Sorting & A–Z         → ✅
Playback persistence  → ✅
Crossfade             → ✅
Artist metadata       → ✅
Custom fonts          → ✅
Performance           → 🟡 Ongoing
Vinyl realism         → 🟡 Ongoing
USB DAC               → ⏸️ Paused
Hardware validation   → ⏳ Pending
Production release    → ⏳ Pending
```

Fokus project sekarang adalah **membuat fitur yang sudah ada semakin stabil dan matang**, bukan sekadar menambah banyak fitur baru.

## Roadmap Berikutnya

```text
UI refinement
    ↓
Playback / queue stability
    ↓
Library refinement
    ↓
Performance testing
    ↓
Audio / USB validation
    ↓
Release hardening
    ↓
Production release
```

---

# Repository

GitHub:

https://github.com/yohandeku32/nusamusic

Current branch:

`main`

Current version:

`3.9.2`
