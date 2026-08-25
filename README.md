# Stok Akun

Aplikasi Android lokal untuk mencatat stok dan data akun game kamu.

## Data & privasi
- Metadata akun disimpan di Room (database lokal).
- Screenshot disimpan sebagai file asli di `filesDir/screenshots`.
- Tidak menggunakan Supabase, Firebase, cloud, atau Base64 untuk gambar.
- Password akun dienkripsi dengan Android Keystore AES-GCM.
- Aplikasi tidak meminta izin internet.
- Backup ZIP berisi metadata JSON + file gambar asli; password tetap berupa ciphertext Keystore.

> **Penting:** ciphertext password terikat pada Android Keystore di instalasi/perangkat. Backup dapat memulihkan data dan gambar, tetapi password terenkripsi dari instalasi lama tidak dapat didekripsi setelah aplikasi di-uninstall/reinstall atau dipindahkan ke perangkat lain.

## Fitur
- Dashboard stok.
- Search dan filter Available / Reserved / Sold.
- Tambah, edit, hapus akun.
- Banyak screenshot fullspek per akun.
- Gallery + fullscreen image viewer.
- Export/import backup ZIP tanpa Base64 untuk gambar.

## Struktur project
Project Android yang dipakai build berada di modul `app/`:

- `app/src/main/java/com/stokakun/app/data` — Room entity, DAO, database, converter.
- `app/src/main/java/com/stokakun/app/repository` — akses data dan penyimpanan screenshot.
- `app/src/main/java/com/stokakun/app/viewmodel` — state dan aksi UI.
- `app/src/main/java/com/stokakun/app/ui` — screen, navigation, component, theme.
- `app/src/main/java/com/stokakun/app/util` — backup, enkripsi, dan file gambar.

## Membuka di Android Studio
1. Clone/download repository ini.
2. Buka folder repository sebagai project di Android Studio versi baru.
3. Gunakan JDK 17.
4. Pastikan Android SDK 35 tersedia.
5. Lakukan Gradle Sync.
6. Pilih **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
7. APK debug biasanya muncul di `app/build/outputs/apk/debug/app-debug.apk`.

## Build APK lewat GitHub Actions
Workflow berada di `.github/workflows/build-debug-apk.yml`.

Workflow CI memasang Gradle 8.9 dan membuat Gradle Wrapper sementara sebelum menjalankan `./gradlew assembleDebug`. Artifact yang dihasilkan bernama `app-debug-apk`.

## Release APK
Untuk distribusi, gunakan **Build > Generate Signed Bundle / APK > APK** di Android Studio dan buat keystore release sendiri.
