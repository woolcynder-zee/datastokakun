# Stok Akun v2

Aplikasi Android lokal untuk mencatat stok akun game dan banyak screenshot fullspek.

## Data & privasi
- Metadata akun disimpan di Room (database lokal).
- Screenshot disimpan sebagai file asli di `filesDir/screenshots`.
- Tidak menggunakan Supabase, Firebase, cloud, atau Base64 untuk gambar.
- Password akun dienkripsi dengan Android Keystore AES-GCM.
- Aplikasi tidak meminta izin internet.
- Backup ZIP berisi metadata JSON + file gambar asli; password tetap berupa ciphertext Keystore.

> **Penting:** ciphertext password terikat pada Android Keystore di instalasi/perangkat. Backup dapat memulihkan data dan gambar, tetapi password terenkripsi dari instalasi lama tidak dapat didekripsi setelah aplikasi di-uninstall/reinstall atau dipindahkan ke perangkat lain.

## Fitur v2
- Dashboard dengan jumlah Total / Available / Reserved / Sold dan nilai stok aktif.
- Search berdasarkan game, nama/ID stok, dan username.
- Filter Available / Reserved / Sold.
- Sorting terbaru, terlama, nama A–Z/Z–A, harga tertinggi/terendah.
- Tambah, edit, hapus akun.
- Pencegahan duplikasi akun pada data identitas yang sama.
- Banyak screenshot fullspek per akun.
- Gallery + fullscreen image viewer.
- Batas screenshot 25 MB per file.
- Storage Manager untuk melihat ukuran penyimpanan dan membersihkan orphan file.
- Bulk select untuk ubah status atau hapus banyak akun sekaligus.
- Copy username/password dari detail akun.
- Clipboard credential dibersihkan otomatis setelah 30 detik bila isinya masih credential yang disalin aplikasi.
- Share detail akun.
- App Lock PIN 4–8 digit dengan PBKDF2 + salt.
- Lockout setelah terlalu banyak percobaan PIN.
- App tidak mengunci ulang hanya karena membuka file picker/share sheet; penguncian background memakai grace period 30 detik.
- Export/import backup ZIP dengan validasi archive, perlindungan path traversal, batas ukuran, deduplikasi, dan rollback database.

## Struktur project
Project Android yang dipakai build berada di modul `app/`:

- `app/src/main/java/com/stokakun/app/data` — Room entity, DAO, database, converter.
- `app/src/main/java/com/stokakun/app/repository` — akses data dan penyimpanan screenshot.
- `app/src/main/java/com/stokakun/app/viewmodel` — state dan aksi UI.
- `app/src/main/java/com/stokakun/app/ui` — screen, navigation, component, theme.
- `app/src/main/java/com/stokakun/app/util` — backup, enkripsi, app lock, dan file gambar.

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
