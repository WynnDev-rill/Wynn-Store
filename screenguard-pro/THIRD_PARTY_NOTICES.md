# Lisensi dan sumber

ScreenGuard Pro dilisensikan **GPL-3.0**; lihat `LICENSE`. Lisensi ini berlaku pada subproyek ini. Lisensi root dan proyek lain tidak diubah. API Android yang dipanggil tidak menjamin izin/dukungan identik pada setiap vendor.

| Komponen / referensi | Lisensi | Penggunaan |
| --- | --- | --- |
| [AndroidX / Compose / Material3 / WorkManager](https://github.com/androidx/androidx) | Apache-2.0 | UI native, lifecycle, tugas terjadwal |
| [Kotlin / kotlinx.coroutines / kotlinx.serialization](https://github.com/JetBrains/kotlin) | Apache-2.0 | Bahasa, concurrency, serialisasi |
| [Shizuku API](https://github.com/RikkaApps/Shizuku-API), 13.1.5 | Apache-2.0 | Binder, permission, UserService |
| [Gradle wrapper](https://github.com/gradle/gradle) | Apache-2.0 | Build reproducible; wrapper disalin dari subproyek yang sudah ada |
| [JUnit 4](https://github.com/junit-team/junit4) | EPL-1.0 | Tes JVM, hanya test scope |
| [Robolectric](https://github.com/robolectric/robolectric) | MIT | Tes storage Android, hanya test scope |
| [ShizuWall](https://github.com/AhmetCanArslan/ShizuWall/tree/8a51d90ec7fed3d2e53e027fe997052403ec6859) | GPL-3.0 | Referensi OEM_DENY_3, per-UID firewall, dan batas perangkat |
| [Hail](https://github.com/aistra0528/Hail/tree/60742d0608f2d0abb4fa7e8bbc1b66b3888d22bc) | GPL-3.0 | Referensi enabled state, suspend, dan verifikasi status paket |

Tidak ada dependensi biner terhadap ShizuWall atau Hail. Implementasi bridge bersifat khusus untuk ScreenGuard dan endpoint AIDL bertipe. Distribusi turunan subproyek ini harus memenuhi GPL-3.0 dan menyediakan source yang sesuai dengan APK. Nama/logo aplikasi pihak ketiga diambil dari paket terinstal, tidak didistribusikan sebagai aset aplikasi.

Dokumentasi resmi yang diperiksa:

- [UsageStatsManager](https://developer.android.com/reference/android/app/usage/UsageStatsManager)
- [UsageEvents.Event](https://developer.android.com/reference/android/app/usage/UsageEvents.Event)
- [Shizuku API UserService dan restart tanpa root](https://github.com/RikkaApps/Shizuku-API#userservice)
- [Foreground service types / specialUse](https://developer.android.com/develop/background-work/services/fgs/service-types)
- [Batas memulai foreground service dari latar](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start)
- [WorkManager periodic work](https://developer.android.com/develop/background-work/background-tasks/persistent/getting-started/define-work#schedule_periodic_work)
- [Android app signing](https://developer.android.com/studio/publish/app-signing)
