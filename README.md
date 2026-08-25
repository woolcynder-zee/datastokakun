# Stok Akun

Aplikasi Android lokal untuk menyimpan stok akun game dan banyak screenshot fullspek.

## Penyimpanan
- Metadata akun disimpan di Room (database lokal).
- Screenshot disimpan sebagai file asli di `filesDir/screenshots`.
- Tidak menggunakan Supabase, Firebase, cloud, atau Base64 untuk penyimpanan gambar.
- Kapasitas praktis mengikuti ruang penyimpanan perangkat (tetap dibatasi oleh storage Android yang tersedia).

## Fitur
- Dashboard stok.
- Search dan filter Available / Reserved / Sold.
- Tambah, edit, hapus akun.
- Banyak screenshot Fullspek per akun.
- Gallery + fullscreen image viewer.
- Password akun dienkripsi dengan Android Keystore.
- Export/import backup ZIP (metadata JSON + file gambar, bukan Base64).

## Membuka di Android Studio
1. Ekstrak ZIP ini.
2. Buka folder `StokAkun` sebagai project di Android Studio versi baru.
3. Gunakan JDK 17.
4. Pastikan Android SDK 35 tersedia.
5. Biarkan Android Studio melakukan Gradle sync. Internet diperlukan saat dependency belum ada di cache.
6. Pilih **Build > Build Bundle(s) / APK(s) > Build APK(s)**.
7. APK debug biasanya muncul di:
   `app/build/outputs/apk/debug/app-debug.apk`

### Release APK
Gunakan **Build > Generate Signed Bundle / APK > APK** untuk membuat APK release yang bisa dipasang/distribusikan.

## Catatan build
ZIP sumber yang diberikan Claude tidak menyertakan `gradle-wrapper.jar`, sehingga build tidak bisa dijalankan lewat `./gradlew` sampai wrapper dibuat/dilengkapi. Source project sudah dirapikan dan konfigurasi Gradle tetap dipertahankan.
