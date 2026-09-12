# 💊 İlaç Takip (Medicine Tracker)

Modern, güvenilir ve kullanıcı dostu bir Android ilaç takip ve hatırlatıcı uygulaması.  
**%100 Kotlin** ve **Jetpack Compose (Material 3)** kullanılarak geliştirilmiştir.

[![Android Version](https://img.shields.io/badge/Platform-Android%208.0%2B%20(API%2026%2B)-green.svg)](https://developer.android.com)
[![Language](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)
[![UI Toolkit](https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg)](https://developer.android.com/jetpack/compose)
[![Material Design](https://img.shields.io/badge/Design-Material%203-orange.svg)](https://m3.material.io)
[![License](https://img.shields.io/badge/License-MIT-lightgrey.svg)](LICENSE)

---

## 📱 Ekran Görüntüleri

| Ana Ekran (İlaç Listesi) | İlaç Ekleme / Düzenleme | Tam Ekran Alarm Ekranı |
|:---:|:---:|:---:|
| *(Ekran görüntünüzü buraya ekleyin)* | *(Ekran görüntünüzü buraya ekleyin)* | *(Ekran görüntünüzü buraya ekleyin)* |

---

## ✨ Öne Çıkan Özellikler

- ⏰ **Gelişmiş Alarm ve Hatırlatıcı Sistemi:**
  - Kilit ekranında tam ekran çalan güvenilir alarm arayüzü (`USE_FULL_SCREEN_INTENT`, `SHOW_WHEN_LOCKED`).
  - Ekran açıkken sesli ve titreşimli bildirim garantisi (`USAGE_ALARM`).
  - **10 Dakika Sabit Erteleme:** Bildirim üzerinden, alarm ekranından veya donanım ses tuşlarıyla anında erteleme.
  - Cihaz yeniden başlatıldığında (`BOOT_COMPLETED`) süresi dolmamış alarmların otomatik olarak tekrar planlanması.

- 🍽️ **Açlık / Tokluk Durumu Belirleme:**
  - İlaçları "Aç Karnına", "Tok Karnına" veya "Fark Etmez" olarak tanımlama.
  - Hem ana ekrandaki kartlarda hem de alarm ekranında belirgin hatırlatma rozetleri.

- 📦 **Kutu & Stok Takibi:**
  - İlacın kutusundaki mevcut tablet / adet sayısını girme.
  - İlaç "Alındı" işaretlendikçe stoktan otomatik 1 adet düşme.
  - Kalan adet 3 veya daha az olduğunda ana ekranda **"⚠ Az Kaldı"** uyarısı.

- 🗓️ **Esnek Zamanlama & Tarih Aralığı:**
  - Farklı sıklık seçenekleri (Günde 1 defa, 6-8-12 saatte bir, 2-6 günde bir, haftada 1 kez).
  - **Haftanın Belirli Günlerine Özel Mod:** Her gün için ayrı saat belirleme imkânı.
  - İsteğe bağlı başlangıç ve bitiş tarihi tanımlama.

- 📜 **İlaç Kullanım Geçmişi (Log):**
  - İlacın ne zaman alındığını tarih ve saatle kayıt altına alan geçmiş günlüğü.
  - Ana ekrandaki saat ikonuna dokunarak geçmişi görüntüleme ve tek tuşla temizleme.

- 🔋 **Üretici & Pil Optimizasyonu Rehberi:**
  - Xiaomi, Huawei, Samsung gibi üreticilerin arka planda alarmı susturmasını önlemek için adım adım yönlendirici rehber.

- 🔒 **Tamamen Çevrimdışı ve Güvenli:**
  - Verileriniz sadece cihazınızda (`SharedPreferences`) saklanır. İnternet bağlantısı gerektirmez, kişisel sağlık verileriniz üçüncü şahıslarla paylaşılmaz.

---

## 🛠️ Kullanılan Teknolojiler ve Kütüphaneler

- **Dil:** [Kotlin](https://kotlinlang.org/)
- **Kullanıcı Arayüzü:** [Jetpack Compose](https://developer.android.com/jetpack/compose) & [Material Design 3](https://m3.material.io/)
- **Mimari:** Modern Declarative UI, State Hoisting, Repository Pattern
- **Arka Plan & Zamanlama:** `AlarmManager`, `BroadcastReceiver`, `NotificationCompat`
- **Serileştirme:** [Google Gson](https://github.com/google/gson)
- **Test:** JUnit 4, Robolectric

---

## 🚀 Projeyi Derleme ve Çalıştırma

### Gereksinimler
- **Android Studio:** Ladybug / Meerkat veya üzeri
- **JDK:** Java 17 veya 21 (Android Studio yerleşik JBR önerilir)
- **Minimum SDK:** Android 8.0 (API Seviyesi 26)
- **Hedef SDK:** Android 15 (API Seviyesi 35)

### Kurulum Adımları

1. Projeyi klonlayın:
   ```bash
   git clone https://github.com/ARafiY/IlacTakip-kotlin.git
   cd IlacTakip-kotlin
   ```

2. Projeyi Android Studio ile açın:
   - **File** -> **Open...** -> `IlacTakip-kotlin` dizinini seçin.
   - Gradle senkronizasyonunun tamamlanmasını bekleyin.

3. Debug APK derlemek için:
   ```bash
   ./gradlew assembleDebug
   ```

4. Google Play için imzalı Release App Bundle (AAB) derlemek için:
   ```bash
   ./gradlew bundleRelease
   ```
   *(Çıktı dosyası `app/build/outputs/bundle/release/app-release.aab` konumunda oluşturulacaktır).*

---

## 📂 Proje Dizin Yapısı

```text
com.YucelDigital.ilactakip/
├── AddMedicineActivity.kt       # İlaç ekleme ve düzenleme formu & Composable arayüzü
├── AlarmActivity.kt             # Tam ekran alarm arayüzü, erteleme ve zil sesi yönetimi
├── AlarmHelper.kt               # Exact alarm zamanlama, iptal ve ID hesaplama yardımcıları
├── AlarmReceiver.kt             # Zamanı geldiğinde alarmı ve bildirimi tetikleyen alıcı
├── BootReceiver.kt              # Cihaz açıldığında alarmları yeniden kuran alıcı
├── MainActivity.kt              # Ana ekran, ilaç listesi, geçmiş ve ayarlar
├── Medicine.kt                  # İlaç veri modeli
├── MedicineRepository.kt        # Veri kaydetme, okuma ve geçmiş log yönetimi
├── NotificationActionReceiver.kt# Bildirim üzerinden "Aldım" ve "Ertele" aksiyonları
└── ui/theme/                    # Renkler, tipografi ve tema tanımları
```

---

## 📄 Lisans

Bu proje [MIT Lisansı](LICENSE) altında lisanslanmıştır.

---

## 👨‍💻 Geliştirici & İletişim

**Abdullah Rafi Yücel**  
- GitHub: [@ARafiY](https://github.com/ARafiY)  
- Kuruluş: YucelDigital  
