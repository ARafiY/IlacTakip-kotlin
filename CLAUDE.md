# IlacTakip — AI Agent Talimatları

Bu dosya, bu depoda çalışan yapay zeka agent'ları (Claude Code vb.) için
bağlayıcı talimatlar içerir. Kod değişikliğine başlamadan önce oku.

## Branch iş akışı (ÖNEMLİ)

- **Tüm çalışmalar `develop` branch'inde yapılır.** Aktif geliştirme dalı
  budur; `main` sürüm/yayın dalıdır ve doğrudan buraya commit atma.
- Bir oturuma başlarken önce doğru daldaki olduğunu kontrol et:
  ```bash
  git rev-parse --abbrev-ref HEAD   # "develop" olmalı
  ```
  Değilse: `git checkout develop`
- Yeni bir iş için `develop`'tan konu dalı aç (`git checkout -b feature/...`)
  ve işi bitince PR'ı **`develop`'a** (main'e değil) hedefle.
- **`main`'e doğrudan commit/push YOK.** `develop`'tan `main`'e geçiş yalnızca
  **Pull Request** ile ve insan onayıyla yapılır (sürüm çıkışlarında).
- Kullanıcı açıkça istemedikçe commit/push/PR yapma.

## Proje hakkında

- İsim "IlacTakip-java" olsa da proje **Kotlin + Jetpack Compose** ile yazılmış
  bir Android uygulamasıdır (ilaç hatırlatma/alarm). Yeni kodu Kotlin yaz.
- Paket: `com.YucelDigital.ilactakip` — kaynak: `app/src/main/java/...`
- Veri kalıcılığı: `MedicineRepository` (SharedPreferences + Gson, thread-safe).
  Serileştirilen model `Medicine` reflection ile okunur; alan adları JSON
  anahtarı olur — bu yüzden `app/proguard-rules.pro` içinde korunur.
- Alarmlar: `AlarmReceiver` / `AlarmHelper` (AlarmManager), açılışta yeniden
  kurulum `BootReceiver` ile yapılır.

## Derleme ve test

```bash
sh ./gradlew :app:assembleDebug        # debug APK
sh ./gradlew :app:testDebugUnitTest    # birim testler (JUnit + Robolectric)
sh ./gradlew :app:assembleRelease      # R8/minify ile release (ProGuard'ı test eder)
```

## Değişiklik yaparken

- İlgili dosyaları düzenlemeden önce oku; mevcut yapı ve isimlendirmeye uy.
- Model alanı ekler/çıkarırsan `proguard-rules.pro` ve serileştirme
  uyumluluğunu (eski kayıtlar) gözden geçir.
- Değişiklikten sonra ilgili testleri çalıştır.
