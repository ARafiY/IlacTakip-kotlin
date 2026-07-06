# İlaç Takip — Kod Açıklaması

Bu belge, projedeki **tüm Kotlin kaynak dosyalarını** satır satır (veya anlamlı gruplar hâlinde) açıklar. XML kaynakları (layout, drawable, tema/renk dosyaları) ve Gradle yapılandırması da özetlenir, ama onlar "kod" değil "yapılandırma/görsel tanım" olduğu için daha kısa tutuldu — asıl derinlik Kotlin dosyalarında.

## İçindekiler

1. [Proje Genel Bakış](#proje-genel-bakış)
2. [Yapılandırma Dosyaları](#yapılandırma-dosyaları)
3. [Medicine.kt — Veri Modeli](#medicinekt--veri-modeli)
4. [MedicineRepository.kt — Kalıcı Depolama](#medicinerepositorykt--kalıcı-depolama)
5. [AlarmHelper.kt — Alarm İptal Yardımcısı](#alarmhelperkt--alarm-iptal-yardımcısı)
6. [BootReceiver.kt — Cihaz Yeniden Başlatıldığında](#bootreceiverkt--cihaz-yeniden-başlatıldığında)
7. [NotificationActionReceiver.kt — Bildirim Butonları](#notificationactionreceiverkt--bildirim-butonları)
8. [AlarmReceiver.kt — Alarmın Kalbi](#alarmreceiverkt--alarmın-kalbi)
9. [AlarmActivity.kt — Tam Ekran Alarm Ekranı (Compose)](#alarmactivitykt--tam-ekran-alarm-ekranı-compose)
10. [AddMedicineActivity.kt — İlaç Ekle/Düzenle Ekranı (Compose)](#addmedicineactivitykt--i̇laç-ekledüzenle-ekranı-compose)
11. [MainActivity.kt — Ana Ekran (Jetpack Compose)](#mainactivitykt--ana-ekran-jetpack-compose)
12. [IlacTakipTheme.kt](#ilactakipthemekt)
13. [Test Dosyaları](#test-dosyaları)
14. [Kaynak (XML) Dosyaları — Özet](#kaynak-xml-dosyaları--özet)

---

## Proje Genel Bakış

Bu, **ilaç hatırlatma** yapan bir Android uygulaması. Kullanıcı bir ilaç ekliyor (adı, saat(ler)i, opsiyonel tarih aralığı veya haftanın belirli günleri), uygulama o saatlerde **tam ekran alarm** (telefon kilit ekranındayken bile açılan bir ekran + ses) gösteriyor. Kullanıcı "İlacı Aldım" diyebiliyor ya da erteleyebiliyor.

**Paket adı:** `com.YucelDigital.ilactakip`
**Dil:** %100 Kotlin (proje başlangıçta Java'ydı, tamamı Kotlin'e çevrildi)
**UI:** %100 Jetpack Compose + native Material3 — üç ekranın (`MainActivity`, `AddMedicineActivity`, `AlarmActivity`) hepsi `setContent { }` ile çiziliyor, projede artık hiç XML layout veya View sistemi Material Components widget'ı (`MaterialButton`, `Chip`, `TextInputLayout` vb.) yok. `com.google.android.material:material` bağımlılığı sadece launch/pencere teması için (`Theme.IlacTakip` → `Theme.Material3.DayNight.NoActionBar`) kalıyor — bu XML tema o kütüphaneden geliyor, ekrandaki hiçbir bileşenle ilgisi yok.

### Dosya haritası

```
app/src/main/java/com/YucelDigital/ilactakip/
├── Medicine.kt                  → Bir ilacı temsil eden veri sınıfı
├── MedicineRepository.kt        → İlaç listesini SharedPreferences'a kaydet/oku
├── AlarmHelper.kt                → Bir ilacın tüm alarmlarını iptal etme mantığı
├── BootReceiver.kt               → Telefon yeniden başlayınca alarmları yeniden kur
├── NotificationActionReceiver.kt → Bildirimdeki "Aldım"/"Ertele" butonları
├── AlarmReceiver.kt               → Alarm tetiklendiğinde ne olacağı (bildirim + zincirleme)
├── AlarmActivity.kt               → Tam ekran alarm ekranı (Compose UI + ses çalma, erteleme)
├── AddMedicineActivity.kt         → İlaç ekleme/düzenleme formu (Compose UI)
├── MainActivity.kt                → Ana liste ekranı (Compose)
└── ui/theme/IlacTakipTheme.kt     → Compose Material3 tema tanımı
```

---

## Yapılandırma Dosyaları

### `build.gradle` (proje kökü)

```gradle
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
}
```

- Bu, **kök** build dosyası; alt modüllerin (burada tek modül var: `app`) kullanabileceği plugin'leri *tanımlar* ama henüz *uygulamaz* (`apply false`).
- `libs.plugins.android.application` → Android uygulama derleme eklentisi (APK üretimi).
- `libs.plugins.kotlin.android` → Kotlin dilini Android projesinde derleyebilme desteği.
- `libs.plugins.kotlin.compose` → Jetpack Compose'un Kotlin derleyici eklentisi (Compose 2.0+'da bu ayrı bir plugin oldu).
- `libs.plugins.*` referansları `gradle/libs.versions.toml` dosyasından geliyor (aşağıda).

### `gradle/libs.versions.toml` — Sürüm Kataloğu

Bu dosya, tüm kütüphane/plugin sürümlerini **tek yerden** yönetmek için var (Gradle'ın "version catalog" özelliği).

- **`[versions]`** bloğu: her bağımlılığın sürüm numarasını bir isimle eşler (örn. `kotlin = "2.4.0"`).
- **`[libraries]`** bloğu: her kütüphanenin `group:name:version` üçlüsünü tanımlar. Örn. `material = { group = "com.google.android.material", name = "material", version.ref = "material" }` → Gradle bunu `com.google.android.material:material:1.13.0` olarak okur.
- **`[plugins]`** bloğu: Gradle plugin ID'lerini isimlendirir (örn. `kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", ... }`).
- `app/build.gradle` içinde `libs.appcompat`, `libs.plugins.kotlin.android` gibi referanslar bu dosyadan gelir; Gradle bunları derleme zamanında gerçek koordinatlara çevirir.

### `app/build.gradle` — Modül Yapılandırması

```gradle
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}
```
Üç plugin de burada **gerçekten uygulanıyor** (kökteki `apply false` burada aktive ediliyor).

```gradle
android {
    namespace 'com.YucelDigital.ilactakip'
    compileSdk 36
```
- `namespace`: Üretilen `R` sınıfının (kaynaklara erişim sınıfı) hangi pakette olacağını belirler.
- `compileSdk 36`: Hangi Android SDK sürümünün API'leriyle derleneceği (en yeni Android 16 API seviyesi).

```gradle
    defaultConfig {
        applicationId "com.YucelDigital.ilactakip"
        minSdk 24
        targetSdk 36
        versionCode 3
        versionName "3.0"
        testInstrumentationRunner "androidx.test.runner.AndroidJUnitRunner"
    }
```
- `applicationId`: Play Store / cihazdaki benzersiz uygulama kimliği.
- `minSdk 24`: En eski desteklenen Android sürümü (Android 7.0, Nougat).
- `targetSdk 36`: Uygulamanın "hedeflediği" davranış seti — Android bu numaraya göre yeni güvenlik/izin kurallarını (örn. bildirim izni, tam ekran intent izni) uygulamaya zorlar.
- `versionCode`/`versionName`: İç sürüm numarası / kullanıcıya gösterilen sürüm metni.
- `testInstrumentationRunner`: Cihaz üzerinde çalışan (`androidTest`) testler için runner sınıfı.

```gradle
    buildTypes {
        release {
            minifyEnabled true
            shrinkResources true
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
```
`release` (yayın) derlemesinde: `minifyEnabled true` kodu küçültür/obfuscate eder (R8), `shrinkResources true` kullanılmayan kaynakları (resim, string vs.) APK'dan atar, `proguardFiles` bu küçültme kurallarının nereden geleceğini belirtir.

```gradle
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_11
        targetCompatibility JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose true
    }
    testOptions {
        unitTests {
            includeAndroidResources = true
        }
    }
}
```
- `compileOptions`/`kotlinOptions`: Java/Kotlin kodunun hangi JVM bayt kodu sürümüne (Java 11) derleneceği.
- `buildFeatures { compose true }`: Jetpack Compose desteğini açar (Compose derleyici eklentisinin devreye girmesi için şart).
- `testOptions.unitTests.includeAndroidResources = true`: `app/src/test` altındaki (Robolectric) testlerin, gerçek Android kaynaklarına (renkler, layout'lar) erişebilmesini sağlar — Robolectric testleri gerçek bir cihaz olmadan Android API'lerini simüle ettiği için buna ihtiyaç duyar.

```gradle
dependencies {
    implementation libs.appcompat
    implementation 'com.google.code.gson:gson:2.10.1'
    implementation libs.material
    implementation libs.activity

    implementation platform(libs.compose.bom)
    implementation libs.compose.ui
    implementation libs.compose.ui.tooling.preview
    implementation libs.compose.material3
    implementation libs.activity.compose
    debugImplementation libs.compose.ui.tooling

    testImplementation libs.junit
    testImplementation 'org.robolectric:robolectric:4.14.1'
    testImplementation 'androidx.test:core:1.6.1'
    androidTestImplementation libs.ext.junit
    androidTestImplementation libs.espresso.core
}
```
- `appcompat`: `AppCompatActivity`'nin geldiği kütüphane — projenin üç Activity'si de (artık hepsi Compose olsa da) bu temel sınıftan türüyor.
- `material`: View sistemi Material Components kütüphanesi. Projede artık **hiç** View widget'ı (Button, Chip, TextInputLayout vb.) yok, ama `Theme.IlacTakip`'in ana teması (`Theme.Material3.DayNight.NoActionBar`) bu kütüphaneden geldiği ve Compose çizime başlamadan önceki pencere/launch teması için hâlâ gerektiği için bağımlılık kalıyor. (`androidx.constraintlayout:constraintlayout` ise tamamen kaldırıldı — tek kullanıcısı olan `activity_alarm.xml` silinince gereksiz kaldı.)
- `gson`: JSON serileştirme kütüphanesi — `MedicineRepository` ilaç listesini JSON'a çevirip `SharedPreferences`'a yazmak için kullanıyor.
- `platform(libs.compose.bom)`: Compose "Bill of Materials" — altındaki tüm `compose.*` kütüphanelerinin birbiriyle uyumlu sürümlerini otomatik seçer (ayrı ayrı versiyon yazmaya gerek kalmaz).
- `compose.ui`, `compose.material3`, `activity.compose`: Compose UI'ın temel taşları + Material Design 3 bileşenleri (Card, Switch, Scaffold vs.) + bir Activity'de `setContent { }` ile Compose başlatabilme desteği.
- `debugImplementation libs.compose.ui.tooling`: Sadece debug derlemesinde Compose önizleme/inceleme araçları (release APK'ya dahil edilmez).
- `testImplementation ... robolectric`: `app/src/test` altındaki testlerin gerçek bir Android çerçevesini (sahte/simüle cihaz) kullanabilmesi için — `Context`, `NotificationManager` gibi Android sınıflarını JVM üzerinde çalıştırır.

### `AndroidManifest.xml`

Uygulamanın "kimlik kartı" — hangi izinlere ihtiyacı olduğunu ve hangi bileşenlerden (Activity, Receiver) oluştuğunu Android sistemine bildirir.

```xml
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_EXACT_ALARM" />
<uses-permission android:name="android.permission.USE_FULL_SCREEN_INTENT" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
```
Sırasıyla: titreşim, ekranı/CPU'yu uyanık tutma, **dakikası dakikasına alarm kurma**, tam ekran bildirim açma, bildirim gönderme, ve **telefon açılışında** çalışabilme izinleri. Bunların çoğu ilaç hatırlatıcının doğru zamanda ve güvenilir şekilde çalması için zorunlu.

```xml
<application
    ...
    android:theme="@style/Theme.IlacTakip">
```
> **Not:** Bu satırda eskiden `android:name=".IlacTakipApplication"` da vardı — Material You dinamik rengini klasik XML temalı ekranlara (`AddMedicineActivity`, `AlarmActivity`) sonradan uygulayan (`DynamicColors.applyToActivitiesIfAvailable`) küçük bir `Application` alt sınıfıydı. Her iki ekran da Compose'a geçip kendi `IlacTakipTheme`'i üzerinden dinamik rengi zaten okuduğu için bu sınıf tamamen gereksiz kaldı ve silindi — proje artık özel bir `Application` sınıfı kullanmıyor.

```xml
<activity android:name=".AddMedicineActivity" android:exported="false"
    android:windowSoftInputMode="adjustPan" />
```
`exported="false"` → başka uygulamalar bu ekranı doğrudan açamaz (sadece bizim uygulamamız). `windowSoftInputMode="adjustPan"` → klavye açıldığında ekranı klavyenin üstüne kaydırır (input alanları klavyenin altında kalmasın diye).

```xml
<activity android:name=".MainActivity" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>
</activity>
```
`MAIN`/`LAUNCHER` intent-filter'ı → bu, uygulama ikonuna dokunulduğunda açılan ekran demek. `exported="true"` zorunlu çünkü launcher (ana ekran) dışarıdan bu Activity'yi başlatıyor.

```xml
<activity android:name=".AlarmActivity"
    android:theme="@style/Theme.IlacTakip"
    android:showOnLockScreen="true"
    android:showWhenLocked="true"
    android:turnScreenOn="true"
    android:launchMode="singleTask"
    android:excludeFromRecents="true"
    android:screenOrientation="portrait"
    android:configChanges="orientation|keyboardHidden|screenSize" />
```
Bu, alarm çaldığında açılan tam ekran ekran. `showWhenLocked`/`turnScreenOn` → kilit ekranındayken bile üstte açılır ve ekranı otomatik açar. `launchMode="singleTask"` → aynı anda birden fazla kopyası açılmaz (üst üste alarm penceresi yığılmasın diye). `excludeFromRecents="true"` → "son kullanılan uygulamalar" listesinde görünmez (geçici bir ekran olduğu için mantıklı). `configChanges="..."` → ekran döndüğünde/klavye açılınca Activity'nin yeniden yaratılmasını (ve dolayısıyla çalan sesin kesilmesini) engeller.

```xml
<receiver android:name=".BootReceiver" android:enabled="true" android:exported="false">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.QUICKBOOT_POWERON" />
    </intent-filter>
</receiver>

<receiver android:name=".AlarmReceiver" android:enabled="true" android:exported="false" />

<receiver android:name=".NotificationActionReceiver" android:enabled="true" android:exported="false">
    <intent-filter>
        <action android:name="com.YucelDigital.ilactakip.ACTION_TAKEN" />
        <action android:name="com.YucelDigital.ilactakip.ACTION_SNOOZE" />
    </intent-filter>
</receiver>
```
Üç `BroadcastReceiver` (arka planda "yayın" dinleyen bileşenler):
- `BootReceiver`: telefon yeniden başladığında (`BOOT_COMPLETED`) tetiklenir, tüm alarmları yeniden kurar (çünkü telefon kapanıp açılınca `AlarmManager`'a kayıtlı tüm alarmlar silinir).
- `AlarmReceiver`: `AlarmManager` tarafından belirlenen saatte tetiklenir — asıl "alarm zamanı geldi" mantığı burada.
- `NotificationActionReceiver`: bildirimdeki "İlaç Aldım"/"Ertele" butonlarına basılınca tetiklenir (kendi özel `ACTION_TAKEN`/`ACTION_SNOOZE` action'ları var).

Hepsi `exported="false"` — sadece bizim uygulamamızın kendi içinden tetiklenebilirler, başka uygulamalar bu yayınları taklit edip sahte alarm/bildirim tetikleyemez.

---

## Medicine.kt — Veri Modeli

Bir ilacı temsil eden basit veri sınıfı. Tüm diğer dosyalar bu sınıfı kullanır.

```kotlin
package com.YucelDigital.ilactakip

import java.io.Serializable

class Medicine(
    var name: String,
    var time: String?,
    var dateRange: String?,
    var note: String?,
) : Serializable {
```
- `: Serializable` → bu sınıfın nesnelerinin **Intent extra'sı olarak** bir ekrandan diğerine (örn. `MainActivity` → `AddMedicineActivity`, ya da `AlarmReceiver` → `AlarmActivity`) taşınabilmesini sağlar. Android'in `Intent.putExtra(String, Serializable)` metodu bunu gerektiriyor.
- Constructor'daki 4 parametre (`name`, `time`, `dateRange`, `note`) **hepsi `var`** — yani her ilaç oluşturulduktan sonra da bu alanlar değiştirilebilir.
- `time`, `dateRange`, `note` **nullable** (`String?`) çünkü bazı durumlarda olmayabilirler (örn. güne özel modda `dateRange` hep boş string, not girilmemişse `note` null olabilir).

```kotlin
    var isActive: Boolean = true
    var isTaken: Boolean = false
    var startDate: Long = 0
    var endDate: Long = 0
    var intervalDays: Int = 1
    var soundUri: String? = null
```
Constructor dışında tanımlanan, varsayılan değerleri olan alanlar:
- `isActive`: ilacın alarmı aktif mi (kullanıcı ana ekrandaki switch'i kapatabilir). Varsayılan `true` — yeni eklenen ilaç aktif başlar.
- `isTaken`: bugünkü/bu dozun "alındı" olarak işaretlenip işaretlenmediği. `AlarmReceiver` her doz saatinden ~1 saat önce bunu otomatik `false`'a çeker (aşağıda görülecek).
- `startDate`/`endDate`: milisaniye cinsinden Unix zaman damgası — ilacın kullanım aralığı. `0L` = "sınırsız/ayarlanmamış".
- `intervalDays`: kaç günde bir tekrarlanacağı (1 = her gün, 2 = 2 günde bir, 7 = haftada bir).
- `soundUri`: kullanıcının seçtiği özel alarm sesinin `content://` adresi; `null` ise varsayılan alarm sesi çalınır.

```kotlin
    // Güne özel zamanlama
    var isUseCustomDays: Boolean = false
    // Key: Calendar gün sabiti (1=Pazar, 2=Pazartesi, ... 7=Cumartesi)
    // Value: Başlangıç saati "09:00"
    var customDayTimes: HashMap<Int, String>? = null
```
- `isUseCustomDays`: `true` ise bu ilaç "her gün aynı saat" yerine "haftanın belirli günlerinde, günden güne farklı saatlerde" çalışır (örn. Pazartesi 09:00, Çarşamba 14:00).
- `customDayTimes`: gün → saat(ler) eşlemesi. Anahtar, Java/Kotlin'in `Calendar.MONDAY` gibi sabitleri (1-7 arası int). Değer, o günün saat(ler)i — birden fazla doz varsa `"09:00, 21:00"` gibi virgülle ayrılmış.

```kotlin
    companion object {
        private const val serialVersionUID: Long = 1L
    }
}
```
`serialVersionUID`, Java'nın `Serializable` mekanizmasının sınıf sürümünü takip etmesi için kullandığı sabit bir sayı — sınıfın alanları değişse bile serileştirme/deserileştirme uyumluluğunu korumaya yardımcı olur. `companion object` içindeki `private const val`, Kotlin'de bunun tam olarak Java'daki `private static final long serialVersionUID` karşılığı olarak derlenmesini sağlıyor (JVM seviyesinde gerçek bir statik alan).

> **Not:** Bu sınıf bilerek `data class` değil, düz `class`. Çünkü Kotlin'in `data class`'ı otomatik olarak `equals()`/`hashCode()` üretir ve bu, listeden silme (`medicineList.remove(medicine)`) gibi işlemlerin referans eşitliği yerine "içerik eşitliği" ile çalışmasına yol açardı — burada referans eşitliği (varsayılan `Object.equals`) istendiği için düz sınıf tercih edildi.

---

## MedicineRepository.kt — Kalıcı Depolama

İlaç listesinin diske (aslında `SharedPreferences`'a, JSON metni olarak) kaydedilip okunmasından sorumlu **tek** yer. Diğer hiçbir dosya doğrudan `SharedPreferences`'a dokunmaz — hep bu sınıf üzerinden geçer.

```kotlin
package com.YucelDigital.ilactakip

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object MedicineRepository {
```
`object` → Kotlin'de bu, **singleton** demek. Yani `MedicineRepository` hiç `new`'lenmez, tek bir örneği vardır ve tüm metotlarına `MedicineRepository.metotAdı(...)` şeklinde doğrudan erişilir (Java'daki `static` metotlara benzer).

```kotlin
    private const val PREFS_NAME = "MedicineApp"
    private const val KEY_MEDICINE_LIST = "medicine_list"
    private val LOCK = Any()
    private val gson = Gson()
    private val listType = object : TypeToken<ArrayList<Medicine>>() {}.type
```
- `PREFS_NAME`/`KEY_MEDICINE_LIST`: `SharedPreferences` dosyasının adı ve içindeki tek bir anahtarın adı (bütün ilaç listesi bu **tek** anahtarın altında, tek bir JSON string olarak duruyor).
- `LOCK`: eşzamanlılık (thread-safety) için kullanılan boş bir kilit nesnesi — aşağıdaki `synchronized(LOCK) { }` blokları bu nesne üzerinden kilitleniyor, böylece iki farklı thread (örn. UI thread ile bir `AlarmReceiver`'ın arka plan thread'i) aynı anda okuma/yazma yaparsa veri bozulmaz.
- `gson`: JSON ↔ Kotlin nesnesi dönüşümü yapan kütüphanenin tek bir paylaşılan örneği.
- `listType`: Gson'a "bu JSON'ı `ArrayList<Medicine>` olarak çöz" demek için gereken tip bilgisi. Kotlin/Java'da generic tipler derleme zamanında silindiği (type erasure) için, bu tür bir `TypeToken` "hile"si olmadan Gson listenin **içindeki** tipin `Medicine` olduğunu bilemez.

```kotlin
    /** İlaç listesini oku (thread-safe) */
    @JvmStatic
    fun loadMedicineList(context: Context): List<Medicine> {
        synchronized(LOCK) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val json = prefs.getString(KEY_MEDICINE_LIST, null) ?: return ArrayList()
            val list: List<Medicine>? = gson.fromJson(json, listType)
            return list ?: ArrayList()
        }
    }
```
- `@JvmStatic`: Kotlin `object`'lerinin metotları normalde Java'dan `MedicineRepository.INSTANCE.loadMedicineList(...)` şeklinde çağrılır; bu annotation sayesinde Java tarafı da (bu projede artık Java yok ama ileride olabilir) doğrudan `MedicineRepository.loadMedicineList(...)` diyebilir.
- `context.getSharedPreferences(PREFS_NAME, MODE_PRIVATE)`: uygulamaya özel, başka uygulamaların erişemeyeceği bir anahtar-değer deposu açar.
- `prefs.getString(KEY_MEDICINE_LIST, null) ?: return ArrayList()`: kayıtlı JSON yoksa (uygulama ilk kez açılıyorsa) doğrudan boş liste döndür.
- `gson.fromJson(json, listType)`: JSON metnini `List<Medicine>`'e çevirir. Bozuk/eksik JSON gelirse Gson `null` dönebilir, o yüzden `?: ArrayList()` ile yine boş listeye düşülüyor.
- Fonksiyonun dönüş tipi `List<Medicine>` (nullable değil) — yani bu fonksiyonu çağıran hiçbir yer `null` kontrolü yapmak zorunda değil, her zaman (boş da olsa) bir liste garanti edilir.

```kotlin
    /** İlaç listesini kaydet (thread-safe) */
    @JvmStatic
    fun saveMedicineList(context: Context, list: List<Medicine>) {
        synchronized(LOCK) {
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_MEDICINE_LIST, gson.toJson(list)).apply() // apply: asenkron yazma
        }
    }
```
- `gson.toJson(list)`: listeyi tek bir JSON string'ine çevirir.
- `.edit().putString(...).apply()`: `SharedPreferences.Editor` ile yazma işlemi başlatılır; `.apply()` işlemi **arka planda asenkron** olarak diske yazar (UI thread'i bloklamaz) — buna karşılık `.commit()` senkron yazardı ve çağrıldığı thread'i bekletirdi.

```kotlin
    /**
     * Belirli bir ilacı "alındı" olarak işaretle (thread-safe).
     * İsimle eşleştirilir — güne özel modda Medicine.time sadece tek bir günün
     * saatini tuttuğu için (bkz. AddMedicineActivity.saveCustomDayAlarm), saat
     * bazlı eşleştirme farklı günler/saatler için hatalı biçimde eşleşmeyi
     * kaçırıyordu. İsim tekilliği zaten UI tarafında (isDuplicateName) garanti
     * edildiğinden sadece isimle eşleştirmek güvenlidir.
     */
    @JvmStatic
    fun markAsTaken(context: Context, medicineName: String?, medicineTime: String?) {
        synchronized(LOCK) {
            val list = loadMedicineListInternal(context) ?: return

            var changed = false
            for (m in list) {
                if (m.name.equals(medicineName, ignoreCase = true)) {
                    m.isTaken = true
                    changed = true
                    break
                }
            }
            if (changed) {
                saveMedicineListInternal(context, list)
            }
        }
    }
```
- Bu fonksiyon, bir bildirimde "İlaç Aldım"a basıldığında ya da alarm ekranında "Durdur"a basıldığında çağrılır.
- **Neden isimle eşleştiriyor, saatle değil?** Yorum bunu açıklıyor: güne özel modda `Medicine.time` alanı sadece *bir* günün saatini tutuyor (rastgele/ilk gün), ama gerçek alarm farklı bir günün farklı saatinde tetiklenebilir. Saat bazlı eşleştirme yapılsaydı, "İlaç Aldım" bazı durumlarda **hiçbir şeyi güncellemezdi** — bu, projenin ilk incelemesinde bulunup düzeltilen bir hataydı.
- `medicineName`/`medicineTime` parametreleri **nullable** (`String?`) — çünkü bu değerler bir `Intent`'in extra'sından geliyor ve Intent extra'ları her zaman dolu olmayabilir (eksikse `null` gelir). Eğer parametre tipi non-null (`String`) olsaydı, Kotlin arka planda otomatik bir null kontrolü ekler ve `null` geldiğinde **çökerdi** — burada nullable bırakılarak, orijinal Java davranışı (sessizce hiçbir eşleşme bulamayıp no-op yapma) korunuyor.
- Döngü: isimle eşleşen ilk ilacı bulur, `isTaken = true` yapar, `break` ile döngüden çıkar (aynı isimde birden fazla ilaç zaten UI tarafında engellendiği için tek eşleşme yeterli).
- `changed` bayrağı: sadece gerçekten bir değişiklik olduysa diske yazar (gereksiz yazmayı önler).

```kotlin
    /** Belirli bir ilacın isTaken durumunu sıfırla (thread-safe) */
    @JvmStatic
    fun resetTakenStatus(context: Context, medicineName: String?) {
        synchronized(LOCK) {
            val list = loadMedicineListInternal(context) ?: return

            var changed = false
            for (m in list) {
                if (m.name.equals(medicineName, ignoreCase = true)) {
                    m.isTaken = false
                    changed = true
                    break
                }
            }
            if (changed) {
                saveMedicineListInternal(context, list)
            }
        }
    }
```
`markAsTaken`'ın simetriği — `isTaken`'ı `false`'a çeker. Bu, **her doz saatinden yaklaşık 1 saat önce** otomatik olarak `AlarmReceiver` tarafından çağrılır (bir sonraki doz için "alındı" durumunu sıfırlamak amacıyla) — detaylar `AlarmReceiver.kt` bölümünde.

```kotlin
    // ── Internal (LOCK zaten tutulmuş durumda) ──

    private fun loadMedicineListInternal(context: Context): List<Medicine>? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_MEDICINE_LIST, null) ?: return null
        return gson.fromJson(json, listType)
    }

    private fun saveMedicineListInternal(context: Context, list: List<Medicine>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_MEDICINE_LIST, gson.toJson(list)).apply()
    }
}
```
Bunlar, yukarıdaki `public` fonksiyonların **kilit zaten tutulmuşken** çağırdığı `private` yardımcılar — `loadMedicineList`/`saveMedicineList` ile neredeyse aynı iş ama bunlar kendi başlarına `synchronized` blok açmıyorlar (çünkü çağıran fonksiyon zaten `LOCK`'u tutuyor; iç içe kilitlenmeye gerek yok). İsimdeki "Internal" son eki bu ayrımı vurguluyor.

---

## AlarmHelper.kt — Alarm İptal Yardımcısı

Bir ilacın **tüm** alarmlarını (ana alarm + "sıfırlama" alarmı, standart mod veya güne özel mod fark etmeksizin) iptal etme mantığını tek yerde toplar. İlaç silindiğinde, pasif hâle getirildiğinde veya düzenlenip yeniden kurulacağı zaman kullanılır.

```kotlin
package com.YucelDigital.ilactakip

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object AlarmHelper {
```
Yine bir `object` (singleton) — `MedicineRepository` gibi.

```kotlin
    /**
     * hashCode()'dan negatif olmayan bir kimlik üretir.
     * Math.abs(hashCode()) kullanılmıyor çünkü hashCode() tam olarak
     * Integer.MIN_VALUE döndürürse Math.abs onu pozitife çeviremez
     * (Integer.MIN_VALUE'nin mutlak değeri int aralığında temsil edilemez).
     */
    @JvmStatic
    fun safeId(key: String): Int = key.hashCode() and 0x7fffffff
```
Bu tek satırlık fonksiyon, projede **her yerde** alarm kimliği (`alarmId`) üretmek için kullanılıyor (örn. `"Parol" + "08:00"` gibi bir string'den benzersiz bir `Int` kimlik türetmek için).
- `key.hashCode()`: Kotlin/Java `String.hashCode()`, `-2147483648` ile `2147483647` arasında herhangi bir `Int` döndürebilir (negatif de olabilir).
- `and 0x7fffffff`: bitwise **AND** işlemi — `0x7fffffff` (onaltılık), ikilik tabanda `0111 1111 ... 1111` yani en üstteki **işaret biti hariç** tüm bitler 1. Bu maskeyle AND'lemek, sayının işaret bitini (en üst biti) sıfırlar, yani sonucu **her zaman 0 veya pozitif** yapar.
- Neden `Math.abs()` değil de bu yöntem? Çünkü `Int.MIN_VALUE`'nin (`-2147483648`) mutlak değeri, `Int` aralığında **temsil edilemez** (`+2147483648` bir `Int`'e sığmaz, taşma/overflow olur ve sonuç yine negatif `-2147483648` olarak kalır). Yani `Math.abs(Int.MIN_VALUE) == Int.MIN_VALUE` — hâlâ negatif! Bit maskeleme bu köşe durumunda bile her zaman doğru pozitif sonuç verir. Bu, projenin ilk incelemesinde bulunan gerçek bir hataydı (`AlarmHelperTest.kt`'de `"polygenelubricants".hashCode() == Int.MIN_VALUE` özel örneğiyle test ediliyor).
- `PendingIntent`'lerin kimliği (`requestCode`) `Int` olmak zorunda ve negatif olması **teknik olarak** sorun çıkarmasa da, tutarlılık ve öngörülebilirlik için hep pozitif üretiliyor.

```kotlin
    /**
     * Bir ilaca ait tüm alarmları (ana alarm + reset alarm) iptal eder.
     * Güne özel ve standart mod destekler.
     */
    @JvmStatic
    fun cancelAlarm(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val customDayTimes = medicine.customDayTimes
        if (medicine.isUseCustomDays && customDayTimes != null) {
            cancelCustomDayAlarms(context, alarmManager, medicine, customDayTimes)
        } else {
            cancelStandardAlarms(context, alarmManager, medicine)
        }
    }
```
- `getSystemService(Context.ALARM_SERVICE) as? AlarmManager`: sistemin alarm servisini ister; `as?` (güvenli tip dönüşümü) sonucu `AlarmManager?` — eğer bir şekilde `null` gelirse `?: return` ile fonksiyondan sessizce çıkılır (servis yoksa yapacak bir şey yok).
- `val customDayTimes = medicine.customDayTimes`: değeri **önce yerel bir değişkene** alıyor. Bunun sebebi Kotlin dilinin bir kuralı: `medicine.customDayTimes` bir sınıfın **değişebilir** (`var`) alanı olduğu için, `if (medicine.isUseCustomDays && medicine.customDayTimes != null)` yazılsaydı bile, derleyici bu null kontrolünden sonra `medicine.customDayTimes`'ı otomatik olarak "null değil" tipine geçiremez (çünkü teoride başka bir thread o arada değerini değiştirebilir). Yerel `val`'e (değişmez) atayınca bu sorun ortadan kalkar — bu proje boyunca tekrar tekrar karşılaşacağınız bir kalıp.
- Sonra `isUseCustomDays` ve `customDayTimes != null` ikisi birden doğruysa güne özel iptal, değilse standart mod iptali çağrılıyor.

```kotlin
    /** Standart mod alarmlarını iptal et */
    private fun cancelStandardAlarms(context: Context, alarmManager: AlarmManager, medicine: Medicine) {
        val time = medicine.time ?: return

        val timeArray = time.split(", ")
        for (rawTime in timeArray) {
            val singleTime = rawTime.trim()
            val alarmId = safeId(medicine.name + singleTime)
            val resetAlarmId = safeId(medicine.name + singleTime + "_reset")

            val intent = Intent(context, AlarmReceiver::class.java)

            val pi = PendingIntent.getBroadcast(
                context, alarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.cancel(pi)
            pi.cancel()

            val resetPi = PendingIntent.getBroadcast(
                context, resetAlarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            alarmManager.cancel(resetPi)
            resetPi.cancel()
        }
    }
```
- `medicine.time ?: return`: saat bilgisi yoksa iptal edilecek bir şey de yok, çık.
- `time.split(", ")`: `"08:00, 14:00, 20:00"` gibi bir string'i `["08:00", "14:00", "20:00"]` dizisine ayırır — çünkü bir ilacın günde birden fazla dozu (saat) olabilir, her biri **ayrı bir alarm**.
- Her saat için: `alarmId` (ana alarmın kimliği) ve `resetAlarmId` (o dozun "alındı" bayrağını sıfırlayan alarmın kimliği) hesaplanır — ikisi de isim+saat kombinasyonundan `safeId()` ile türetiliyor (aynı isim+saat her zaman aynı kimliği üretir, böylece daha önce `AlarmManager`'a kurulmuş olan **aynı** alarm hedef alınabilir).
- `PendingIntent.getBroadcast(context, alarmId, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)`: `AlarmManager`'a bir alarm kurarken/iptal ederken kullanılan "gelecekte tetiklenecek bir Intent" nesnesi. **Aynı `alarmId` ve aynı hedef sınıfla** oluşturulan bir `PendingIntent`, sistemde önceden kayıtlı olanla **eşdeğer** sayılır — bu sayede burada yepyeni bir `PendingIntent` üretilse bile, `alarmManager.cancel(pi)` çağrısı, daha önce `MainActivity` tarafından kurulmuş olan **gerçek** alarmı iptal eder.
- `FLAG_IMMUTABLE`: Android 12+'da zorunlu hâle gelen bir bayrak — bu `PendingIntent`'in içeriğinin başka bir uygulama tarafından değiştirilemeyeceğini belirtir (güvenlik).
- `alarmManager.cancel(pi)`: `AlarmManager`'daki kaydı iptal eder. `pi.cancel()`: `PendingIntent`'in kendisini de sistemden siler (temizlik).
- Aynı işlem hem ana alarm (`pi`) hem de sıfırlama alarmı (`resetPi`) için ayrı ayrı yapılır.

```kotlin
    /** Güne özel mod alarmlarını iptal et */
    private fun cancelCustomDayAlarms(
        context: Context,
        alarmManager: AlarmManager,
        medicine: Medicine,
        dayTimes: HashMap<Int, String>,
    ) {
        val intent = Intent(context, AlarmReceiver::class.java)

        for ((calDay, timesForDay) in dayTimes) {
            val times = timesForDay.split(", ")

            for (rawTime in times) {
                val singleTime = rawTime.trim()
                val alarmId = safeId(medicine.name + "_day" + calDay + "_" + singleTime)
                val resetAlarmId = safeId(medicine.name + "_day" + calDay + "_" + singleTime + "_reset")

                val pi = PendingIntent.getBroadcast(
                    context, alarmId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                alarmManager.cancel(pi)
                pi.cancel()

                // Reset alarm'ı da iptal et
                val resetPi = PendingIntent.getBroadcast(
                    context, resetAlarmId, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                alarmManager.cancel(resetPi)
                resetPi.cancel()
            }
        }
    }
}
```
Yukarıdakiyle neredeyse aynı mantık, tek fark: `for ((calDay, timesForDay) in dayTimes)` ile **her gün** için ayrı ayrı dolaşılıyor (Kotlin'in `Map` üzerinde `(anahtar, değer)` şeklinde **destructuring** ile döngü kurma özelliği), ve alarm kimliği hesaplanırken isme ek olarak `"_day" + calDay` de katılıyor — böylece aynı ilacın Pazartesi alarmıyla Çarşamba alarmı **farklı** kimliklere sahip olur, biri iptal edilirken diğeri etkilenmez.

---

## BootReceiver.kt — Cihaz Yeniden Başlatıldığında

`AlarmManager`'a kurulan tüm alarmlar, **telefon kapanıp açıldığında sistem tarafından silinir**. Bu dosya, telefon açıldığında devreye girip kayıtlı tüm aktif ilaçların alarmlarını yeniden kurar.

```kotlin
package com.YucelDigital.ilactakip

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (Intent.ACTION_BOOT_COMPLETED != action && "android.intent.action.QUICKBOOT_POWERON" != action) return

        val medicineList = MedicineRepository.loadMedicineList(context)

        for (medicine in medicineList) {
            if (medicine.isActive) {
                val customDayTimes = medicine.customDayTimes
                if (medicine.isUseCustomDays && customDayTimes != null) {
                    scheduleCustomDayAlarms(context, medicine, customDayTimes)
                } else {
                    scheduleStandardAlarms(context, medicine)
                }
            }
        }
    }
```
- `: BroadcastReceiver()` → bu sınıf, Android'in "yayın dinleyicisi" temel sınıfından türetiliyor. `AndroidManifest.xml`'de `BOOT_COMPLETED` yayınına abone olduğu tanımlanmıştı.
- `onReceive`, sistem bir yayın gönderdiğinde otomatik çağrılır. **Ana thread üzerinde**, kısa sürmesi beklenen bir metottur.
- `intent.action`: gelen yayının hangi olay olduğunu söyler. `BOOT_COMPLETED` (standart Android olayı) veya `QUICKBOOT_POWERON` (bazı üreticilerin — örn. eski Samsung/HTC cihazların — kullandığı, "hızlı yeniden başlatma" için özel/standart-olmayan olay) değilse fonksiyondan hemen çıkılır — bu receiver başka hiçbir yayına tepki vermemeli.
- `MedicineRepository.loadMedicineList(context)`: kayıtlı **tüm** ilaçları okur (bu fonksiyon hiçbir zaman `null` döndürmediği için, orijinal Java koddaki `if (medicineList == null) return` kontrolüne artık gerek kalmadı — Kotlin'in tip sistemi bunu zaten garanti ediyor).
- Her ilaç için: sadece `isActive == true` olanlar için (kullanıcı pasife almışsa alarm kurulmaz), moduna göre (güne özel/standart) uygun kurulum fonksiyonu çağrılır.
- `val customDayTimes = medicine.customDayTimes`: yine, mutable alanın smart-cast sorununu aşmak için yerel değişkene alma kalıbı (bkz. `AlarmHelper` bölümü).

```kotlin
    /** Standart mod alarmlarını boot sonrası yeniden kur */
    private fun scheduleStandardAlarms(context: Context, medicine: Medicine) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val time = medicine.time ?: return

        for (rawTime in time.split(", ")) {
            val singleTime = rawTime.trim()
            if (!singleTime.contains(":")) continue
```
- `time.split(", ")` ile her doz saatine ayrılır (örn. `"08:00, 20:00"` → iki ayrı alarm).
- `if (!singleTime.contains(":")) continue`: bozuk/eksik bir saat string'i varsa (":" içermiyorsa) o dozu atla, diğerlerine devam et — tek bir bozuk kayıt yüzünden **tüm** ilacın alarmlarının kurulamaması engellenmiş oluyor.

```kotlin
            val timeParts = singleTime.split(":")
            val hour: Int
            val minute: Int
            try {
                hour = timeParts[0].toInt()
                minute = timeParts[1].toInt()
            } catch (e: NumberFormatException) {
                continue
            }
```
`"08:00"` → `["08", "00"]` → `hour = 8`, `minute = 0`. Sayıya çevirme başarısız olursa (`NumberFormatException`, yine bozuk veri durumunda) o saati atlayıp devam ediliyor.

```kotlin
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, hour)
            calendar.set(Calendar.MINUTE, minute)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)

            if (calendar.timeInMillis <= System.currentTimeMillis()) {
                calendar.add(Calendar.DAY_OF_YEAR, 1)
            }
```
- `Calendar.getInstance()`: **şu anki** tarih/saat ile bir takvim nesnesi oluşturur.
- Saat/dakika/saniye/milisaniye bilgisi istenen değerlere ayarlanır — yani takvim artık "bugün, saat X:Y'de" anlamına gelir.
- Eğer bu an **şu ândan önceyse** (yani bugünkü doz saati zaten geçmişse), `DAY_OF_YEAR`'a 1 eklenerek **yarına** kaydırılır — telefon yeniden başlatıldığında bugünkü saat geçmiş olabilir, o zaman bir sonraki alarm yarına kurulmalı.

```kotlin
            val alarmIntent = Intent(context, AlarmReceiver::class.java)
            alarmIntent.putExtra("MEDICINE_NAME", medicine.name)
            alarmIntent.putExtra("MEDICINE_TIME", singleTime)
            alarmIntent.putExtra("MEDICINE_NOTE", medicine.note)
            alarmIntent.putExtra("START_DATE", medicine.startDate)
            alarmIntent.putExtra("END_DATE", medicine.endDate)
            alarmIntent.putExtra("INTERVAL_DAYS", medicine.intervalDays)
            alarmIntent.putExtra("SOUND_URI", medicine.soundUri)
            alarmIntent.putExtra("IS_CUSTOM_DAY", false)
            alarmIntent.putExtra(AlarmReceiver.EXTRA_IS_RESET, false)

            val alarmId = AlarmHelper.safeId(medicine.name + singleTime)
            alarmIntent.putExtra("ALARM_ID", alarmId)

            val pi = PendingIntent.getBroadcast(
                context, alarmId, alarmIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            setExactAlarm(alarmManager, calendar.timeInMillis, pi)

            AlarmReceiver.scheduleNextResetAlarm(
                context,
                medicine.name, singleTime, alarmId,
                medicine.startDate, medicine.endDate,
                medicine.intervalDays, medicine.soundUri,
            )
        }
    }
```
- `Intent(context, AlarmReceiver::class.java)`: alarm tetiklendiğinde **hangi** `BroadcastReceiver`'ın çalışacağını belirtir (`AlarmReceiver`).
- `putExtra(...)` çağrıları, alarm tetiklendiğinde `AlarmReceiver.onReceive`'in ihtiyaç duyacağı **tüm bilgiyi** (ilaç adı, saat, not, tarih aralığı, tekrar sıklığı, ses, güne-özel mi, ve "bu bir sıfırlama alarmı mı" bayrağı) intent'in içine gömüyor — çünkü `AlarmReceiver` tetiklendiğinde `Medicine` nesnesinin kendisine değil, sadece bu intent'e erişebilecek.
- `setExactAlarm(...)` ile bu intent, hesaplanan zamanda tetiklenmek üzere sisteme kaydedilir (fonksiyonun kendisi birazdan açıklanıyor).
- `AlarmReceiver.scheduleNextResetAlarm(...)`: ana alarmın yanında, o dozdan ~1 saat önce tetiklenecek **ayrı bir "sıfırlama" alarmı** da kurulur (isTaken bayrağını temizlemek için) — bu, `AlarmReceiver.kt` bölümünde detaylandırılıyor.

```kotlin
    /** Güne özel mod alarmlarını boot sonrası yeniden kur */
    private fun scheduleCustomDayAlarms(context: Context, medicine: Medicine, dayTimes: HashMap<Int, String>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        for ((calDay, timesForDay) in dayTimes) {
            for (rawTime in timesForDay.split(", ")) {
                val singleTime = rawTime.trim()
                if (!singleTime.contains(":")) continue

                val timeParts = singleTime.split(":")
                val hour: Int
                val minute: Int
                try {
                    hour = timeParts[0].toInt()
                    minute = timeParts[1].toInt()
                } catch (e: NumberFormatException) {
                    continue
                }

                val calendar = Calendar.getInstance()
                calendar.set(Calendar.HOUR_OF_DAY, hour)
                calendar.set(Calendar.MINUTE, minute)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                calendar.set(Calendar.DAY_OF_WEEK, calDay)

                if (calendar.timeInMillis <= System.currentTimeMillis()) {
                    calendar.add(Calendar.WEEK_OF_YEAR, 1)
                }
```
Standart moda çok benzer, tek fark: `calendar.set(Calendar.DAY_OF_WEEK, calDay)` ile takvim, **o haftanın belirli günü** olarak da ayarlanıyor (örn. "bu haftanın Çarşambası, saat 14:00"). Eğer bu an geçmişse (bu haftanın o günü zaten geçtiyse), 1 hafta ileri atılıyor (`WEEK_OF_YEAR`'a 1 ekleniyor) — yani standart moddaki "1 gün ileri al" yerine burada "1 hafta ileri al" var, çünkü güne özel alarm haftada bir tekrarlanıyor.

```kotlin
                val alarmIntent = Intent(context, AlarmReceiver::class.java)
                alarmIntent.putExtra("MEDICINE_NAME", medicine.name)
                alarmIntent.putExtra("MEDICINE_TIME", singleTime)
                alarmIntent.putExtra("MEDICINE_NOTE", medicine.note)
                alarmIntent.putExtra("START_DATE", 0L)
                alarmIntent.putExtra("END_DATE", 0L)
                alarmIntent.putExtra("INTERVAL_DAYS", 1)
                alarmIntent.putExtra("SOUND_URI", medicine.soundUri)
                alarmIntent.putExtra("IS_CUSTOM_DAY", true)
                alarmIntent.putExtra("CUSTOM_DAY", calDay)
                alarmIntent.putExtra(AlarmReceiver.EXTRA_IS_RESET, false)

                val alarmId = AlarmHelper.safeId(medicine.name + "_day" + calDay + "_" + singleTime)
                alarmIntent.putExtra("ALARM_ID", alarmId)

                val pi = PendingIntent.getBroadcast(
                    context, alarmId, alarmIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )

                setExactAlarm(alarmManager, calendar.timeInMillis, pi)

                AlarmReceiver.scheduleNextCustomDayResetAlarm(
                    context,
                    medicine.name, singleTime, alarmId,
                    calDay, medicine.soundUri,
                )
            }
        }
    }
```
`START_DATE`/`END_DATE` her zaman `0L` (güne özel modda tarih aralığı kavramı yok, süresiz çalışır), `INTERVAL_DAYS` her zaman `1` (anlamsız ama tutarlılık için), `IS_CUSTOM_DAY = true` ve `CUSTOM_DAY = calDay` eklenerek `AlarmReceiver`'a "bu güne özel bir alarm, işte hangi gün" bilgisi taşınıyor. Sonda yine karşılık gelen bir sıfırlama alarmı (`scheduleNextCustomDayResetAlarm`) kuruluyor.

```kotlin
    private fun setExactAlarm(am: AlarmManager, triggerAt: Long, pi: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (am.canScheduleExactAlarms()) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            } else {
                am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        } else {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
        }
    }
}
```
- `Build.VERSION.SDK_INT >= Build.VERSION_CODES.S`: Android 12 (API 31) ve üzeri.
- Android 12'de "kesin alarm" kurmak için **özel bir kullanıcı izni** gerekiyor (`SCHEDULE_EXACT_ALARM`). `canScheduleExactAlarms()` bu izin verilmiş mi diye kontrol eder.
- İzin varsa `setExactAndAllowWhileIdle`: **tam** belirtilen zamanda tetiklenir, cihaz Doze (uyku) modundaysa bile.
- İzin yoksa `setAndAllowWhileIdle`: yaklaşık zamanda tetiklenir (sistem birkaç dakika erteleyebilir) — izinsiz de olsa alarm tamamen iptal olmasın diye bir yedek plan.
- Android 12 öncesinde bu izin kavramı yoktu, doğrudan kesin alarm kurulabiliyordu.
- `RTC_WAKEUP`: "gerçek zaman saati" bazlı (duvar saati, cihaz kapalı/uykuda bile cihazı uyandırarak tetikler) bir alarm tipi.

---

## NotificationActionReceiver.kt — Bildirim Butonları

Bildirimin üzerindeki **"✓ İlaç Aldım"** ve **"⏰ Ertele"** butonlarına basıldığında çalışır.

```kotlin
package com.YucelDigital.ilactakip

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast
import java.util.Calendar
import java.util.Random

class NotificationActionReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_TAKEN = "com.YucelDigital.ilactakip.ACTION_TAKEN"
        const val ACTION_SNOOZE = "com.YucelDigital.ilactakip.ACTION_SNOOZE"
    }
```
- İki özel "action" string'i tanımlanıyor — bunlar `AndroidManifest.xml`'deki `<intent-filter>` içinde de birebir aynı metinlerle geçiyordu. `AlarmReceiver`, bildirim butonlarını kurarken bu sabitleri kullanarak hangi butona basıldığını ayırt edilebilir kılıyor.
- `const val` (companion object içinde) → Java'daki `public static final String` karşılığı; başka dosyalar `NotificationActionReceiver.ACTION_TAKEN` diye doğrudan erişebiliyor.

```kotlin
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        val medicineName = intent.getStringExtra("MEDICINE_NAME")
        val medicineTime = intent.getStringExtra("MEDICINE_TIME")
        val medicineNote = intent.getStringExtra("MEDICINE_NOTE")
        val alarmId = intent.getIntExtra("ALARM_ID", 0)
        val startDate = intent.getLongExtra("START_DATE", 0)
        val endDate = intent.getLongExtra("END_DATE", 0)
        val intervalDays = intent.getIntExtra("INTERVAL_DAYS", 1)
        val soundUri = intent.getStringExtra("SOUND_URI")
        val isCustomDay = intent.getBooleanExtra("IS_CUSTOM_DAY", false)
        val customDay = intent.getIntExtra("CUSTOM_DAY", 0)

        // Bildirimi kapat
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        nm?.cancel(alarmId)
```
- `intent.action ?: return`: action bilgisi yoksa (olmaması gereken bir durum ama savunma amaçlı) hemen çık.
- Intent'in içine gömülmüş tüm bilgiler tek tek okunuyor (bunlar `AlarmReceiver`'ın bildirimi kurarken koyduğu extra'lar).
- `nm?.cancel(alarmId)`: kullanıcı bir butona bastığı an, ekrandaki bildirim **kapatılır** (artık gereksiz).

```kotlin
        if (ACTION_TAKEN == action) {
            MedicineRepository.markAsTaken(context, medicineName, medicineTime)
            Toast.makeText(context, "$medicineName alındı ✓", Toast.LENGTH_SHORT).show()
        } else if (ACTION_SNOOZE == action) {
            val possibleMinutes = intArrayOf(5, 10, 15, 20, 25, 30, 35, 40, 45)
            val snoozeMinutes = possibleMinutes[Random().nextInt(possibleMinutes.size)]
            scheduleSnooze(
                context, medicineName, medicineTime, medicineNote,
                alarmId, startDate, endDate, intervalDays, soundUri, snoozeMinutes,
                isCustomDay, customDay,
            )
            Toast.makeText(context, "$snoozeMinutes dakika ertelendi", Toast.LENGTH_SHORT).show()
        }
    }
```
- `"İlaç Aldım"a basılınca`: `MedicineRepository.markAsTaken(...)` çağrılır (ilaç "alındı" olarak işaretlenir) ve kullanıcıya kısa bir onay mesajı (Toast) gösterilir.
- `"Ertele"ye basılınca`: 5 ile 45 dakika arasında **rastgele** bir erteleme süresi seçilir (`possibleMinutes` dizisinden), ve bu süre sonra tekrar çalacak yeni bir alarm kurulur (`scheduleSnooze`). Rastgele süre seçimi, muhtemelen kullanıcının hep aynı süreyi seçip mekanik bir alışkanlık geliştirmesini önlemek için bilinçli bir tasarım tercihi.

```kotlin
    /** Belirtilen dakika kadar ertele alarmını yeniden kur */
    private fun scheduleSnooze(
        context: Context,
        medicineName: String?,
        medicineTime: String?,
        medicineNote: String?,
        alarmId: Int,
        startDate: Long,
        endDate: Long,
        intervalDays: Int,
        soundUri: String?,
        snoozeMinutes: Int,
        isCustomDay: Boolean,
        customDay: Int,
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, AlarmReceiver::class.java)
        intent.putExtra("MEDICINE_NAME", medicineName)
        intent.putExtra("MEDICINE_TIME", medicineTime)
        intent.putExtra("MEDICINE_NOTE", medicineNote)
        intent.putExtra("ALARM_ID", alarmId)
        intent.putExtra("START_DATE", startDate)
        intent.putExtra("END_DATE", endDate)
        intent.putExtra("INTERVAL_DAYS", intervalDays)
        intent.putExtra("SOUND_URI", soundUri)
        intent.putExtra("IS_CUSTOM_DAY", isCustomDay)
        intent.putExtra("CUSTOM_DAY", customDay)
        intent.putExtra("IS_RESET", false)

        val pi = PendingIntent.getBroadcast(
            context, alarmId, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val cal = Calendar.getInstance()
        cal.add(Calendar.MINUTE, snoozeMinutes)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
            }
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
        }
    }
}
```
- Tüm orijinal bilgilerle (isim, saat, not, tarih aralığı vb.) **aynı** `alarmId`'ye sahip yeni bir `AlarmReceiver` intent'i hazırlanıyor — yani ertelenen alarm, orijinal alarmın **yerine geçiyor** (aynı `PendingIntent` kimliği kullanıldığı için `FLAG_UPDATE_CURRENT` sayesinde eskisinin üzerine yazılıyor).
- `cal.add(Calendar.MINUTE, snoozeMinutes)`: şu andan itibaren `snoozeMinutes` dakika sonrasını hesaplar.
- Kesin alarm izni kontrolü, `BootReceiver`'daki ile birebir aynı mantık (Android 12+ için izin kontrolü, yoksa yaklaşık zamanlı yedek).

---

## AlarmReceiver.kt — Alarmın Kalbi

Bu, projenin **en kritik** dosyası. `AlarmManager`'ın kurduğu her alarm (ana alarm veya sıfırlama alarmı) tetiklendiğinde çalışan yer burası. Hem bildirim gösterir hem de **kendi kendini bir sonraki tetiklenme için yeniden kurar** (bu yüzden alarmlar "zincirleme" ilerler — her tetiklenme bir sonrakini planlar).

```kotlin
package com.YucelDigital.ilactakip

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import java.util.Calendar

class AlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val CHANNEL_ID = "medicine_alarm_channel"

        /**
         * IS_RESET = true olduğunda bu alarm "ön sıfırlama" alarmıdır.
         * Asıl alarmdan 60 dakika önce tetiklenerek isTaken bayrağını temizler.
         */
        const val EXTRA_IS_RESET = "IS_RESET"
```
- `CHANNEL_ID`: Android 8+'ta her bildirimin ait olması gereken bir "bildirim kanalı" var (kullanıcı kanal bazında ses/titreşim/önem ayarı yapabiliyor) — bu uygulamanın tek kanalı.
- `EXTRA_IS_RESET`: bu dosyanın en önemli kavramlarından biri. Uygulama, her doz için **iki türlü** alarm kurar:
  1. **Ana alarm** — asıl doz saatinde tetiklenir, bildirimi gösterir.
  2. **Sıfırlama alarmı** — ana alarmdan **60 dakika önce** tetiklenir ve tek işi `isTaken` bayrağını `false`'a çekmektir (böylece "dünkü doz alındı" işareti bugünkü/bir sonraki doza sızmaz).
  Bu iki alarm türü de **aynı `AlarmReceiver.onReceive`** metodundan geçer; `EXTRA_IS_RESET` bayrağı hangisi olduğunu ayırt eder.

### `onReceive` — Giriş Noktası

```kotlin
    override fun onReceive(context: Context, intent: Intent) {
        // Ortak veriler
        val name = intent.getStringExtra("MEDICINE_NAME")
        val time = intent.getStringExtra("MEDICINE_TIME")
        val note = intent.getStringExtra("MEDICINE_NOTE")
        val alarmId = intent.getIntExtra("ALARM_ID", 0)
        val startDate = intent.getLongExtra("START_DATE", 0)
        val endDate = intent.getLongExtra("END_DATE", 0)
        val intervalDays = intent.getIntExtra("INTERVAL_DAYS", 1)
        val soundUriStr = intent.getStringExtra("SOUND_URI")
        val isReset = intent.getBooleanExtra(EXTRA_IS_RESET, false)
        val isCustomDay = intent.getBooleanExtra("IS_CUSTOM_DAY", false)
        val customDay = intent.getIntExtra("CUSTOM_DAY", 0)
```
`BootReceiver`/`MainActivity`'nin intent'e gömdüğü tüm bilgiler burada tek tek okunuyor — bu metodun geri kalanı bu yerel değişkenlerle çalışıyor.

```kotlin
        // ---- RESET ALARMIYSA: isTaken'ı temizle, çık ----
        if (isReset) {
            MedicineRepository.resetTakenStatus(context, name)
            if (isCustomDay && customDay != 0) {
                scheduleNextCustomDayResetAlarm(context, name, time, alarmId, customDay, soundUriStr)
            } else {
                scheduleNextResetAlarm(context, name, time, alarmId, startDate, endDate, intervalDays, soundUriStr)
            }
            return
        }
```
Eğer bu bir **sıfırlama** alarmıysa: `isTaken`'ı temizle, bir sonraki sıfırlama alarmını yeniden kur (kendi kendini zincirle), ve **hemen çık** — sıfırlama alarmında bildirim gösterme gibi bir iş yok.

```kotlin
        // ---- ANA ALARM: Tarih / Gün Kontrolü (sadece standart mod) ----
        if (!isCustomDay && startDate != 0L) {
            val today = Calendar.getInstance()
            today.set(Calendar.HOUR_OF_DAY, 0)
            today.set(Calendar.MINUTE, 0)
            today.set(Calendar.SECOND, 0)
            today.set(Calendar.MILLISECOND, 0)
            val todayMillis = today.timeInMillis

            if (todayMillis < startDate) {
                rescheduleNextMainAlarm(context, name, time, note, alarmId, startDate, endDate, intervalDays, soundUriStr)
                return
            }
```
Bu blok **sadece standart modda ve bir tarih aralığı belirtilmişse** çalışır. `today` takvimi, günün başlangıcına (00:00:00.000) sıfırlanıyor — yani `todayMillis`, "bugünün tarihi, saat farkı olmadan" demek.
- Eğer bugün, ilacın **başlangıç tarihinden önceyse** (kullanıcı ileri bir tarihte başlayacak şekilde ayarladıysa), bildirim gösterilmez — direkt bir sonraki döngüye kurulur ve çıkılır.

```kotlin
            if (endDate != 0L) {
                val endCal = Calendar.getInstance()
                endCal.timeInMillis = endDate
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                if (todayMillis > endCal.timeInMillis) return
            }
```
Eğer bir bitiş tarihi varsa ve bugün o tarihi (günün **sonu**, 23:59 olarak hesaplanarak) geçtiyse, hiçbir şey yapmadan (yeniden kurmadan bile) çıkılır — ilacın kullanım süresi bitmiştir, alarm zinciri burada sonlanır.

```kotlin
            if (intervalDays > 1) {
                val diffMillis = todayMillis - startDate
                val diffDays = diffMillis / (1000L * 60 * 60 * 24)
                if (diffDays % intervalDays != 0L) {
                    rescheduleNextMainAlarm(context, name, time, note, alarmId, startDate, endDate, intervalDays, soundUriStr)
                    return
                }
            }
        }
```
"X günde bir" (`intervalDays > 1`) durumunda: başlangıçtan bugüne kaç gün geçtiği hesaplanır (`diffMillis` → milisaniyeyi gün sayısına çevirmek için `1000 * 60 * 60 * 24`'e bölünür), ve bu gün sayısının `intervalDays`'e göre **kalanı** (`%`) sıfır değilse, bugün bu ilacın **sırası değildir** — bildirim gösterilmeden bir sonraki uygun güne kurulup çıkılır. (Örn. 3 günde bir ilaç, başlangıçtan 1 gün sonra tetiklenirse `1 % 3 = 1 ≠ 0`, yani bugün değil.)

```kotlin
        // ---- ANA ALARM: Bildirim inşası + isTaken kontrolü disk I/O gerektirir.
        // onReceive'i (ve dolayısıyla ana thread'i) hızlıca serbest bırakmak için
        // bu kısmı arka planda çalıştırıyoruz.
        val appContext = context.applicationContext
        val pendingResult = goAsync()
        Thread {
            try {
                handleMainAlarm(appContext, name, time, note, alarmId, startDate, endDate,
                    intervalDays, soundUriStr, isCustomDay, customDay)
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
```
Bu blok, kodun **thread yönetimi** açısından en önemli kısmı:
- `context.applicationContext`: `onReceive`'e gelen `context`, bir Activity'ye değil bu spesifik yayına bağlıdır ve `onReceive` bittikten sonra geçersiz sayılabilir; `applicationContext` ise uygulamanın **tüm ömrü boyunca** geçerli, daha güvenli bir referans — arka plan thread'ine onu taşıyoruz.
- `goAsync()`: Android'in `BroadcastReceiver` API'sinin bir parçası. Normalde `onReceive` metodu **bitince** sistem bu receiver'ı "işi bitti" sayıp süreci sonlandırabilir. `goAsync()` çağrısı sisteme "bekle, işim henüz bitmedi, arka planda devam ediyorum" der ve bir `PendingResult` nesnesi döndürür.
- `Thread { ... }.start()`: gerçek işi (bildirim oluşturma, `SharedPreferences` okuma gibi disk erişimi gerektiren adımlar) **ayrı bir arka plan thread'inde** çalıştırır — böylece `onReceive`'in kendisi (ana thread) hemen serbest kalır, sistem bu receiver'ı "yavaş/asılı kaldı" diye işaretlemez.
- `try { handleMainAlarm(...) } finally { pendingResult.finish() }`: iş bitince (hata olsa da olmasa da, `finally` sayesinde) `pendingResult.finish()` çağrılarak sisteme "artık gerçekten bitti, receiver'ı öldürebilirsin" denir. Bu çağrı **unutulursa** sistem kaynak sızıntısı/ANR benzeri sorunlar yaşayabilir — `finally` bloğu bunun her koşulda çağrılmasını garanti ediyor.
- Bu tasarım, ilk kod incelemesinde bulunan "ana thread'de disk I/O" sorununun düzeltilmiş hâli.

### `handleMainAlarm` — Asıl İş (Arka Plan Thread'inde)

```kotlin
    /**
     * Ana alarm tetiklendiğinde bildirim inşası + kendini yeniden kurma (arka planda çalışır).
     * Test kodu (Robolectric) goAsync()/arka plan thread'ini devreye sokmadan bu mantığı
     * doğrudan ve senkron çağırabilsin diye public bırakıldı.
     */
    fun handleMainAlarm(
        context: Context, name: String?, time: String?, note: String?, alarmId: Int,
        startDate: Long, endDate: Long, intervalDays: Int, soundUriStr: String?,
        isCustomDay: Boolean, customDay: Int,
    ) {
```
Bu fonksiyon **kasıtlı olarak `public`** — normalde tamamen iç uygulamaya özel (Java'daki "package-private" gibi) olabilirdi, ama testlerin (`AlarmReceiverHandleMainAlarmTest`) `goAsync()`/thread mekanizmasını hiç tetiklemeden bu mantığı **doğrudan ve senkron** çağırabilmesi için erişilebilir bırakıldı.

```kotlin
        // ---- ANA ALARM: Bildirim Göster ----
        // İsimle eşleştirilir (bkz. MedicineRepository.markAsTaken açıklaması) —
        // güne özel modda Medicine.time sadece tek bir günün saatini tuttuğu için
        // saat bazlı eşleştirme hatalı biçimde eşleşmeyi kaçırıyordu.
        var isAlreadyTaken = false
        val medicineList = MedicineRepository.loadMedicineList(context)
        for (m in medicineList) {
            if (m.name.equals(name, ignoreCase = true)) {
                if (m.isTaken) {
                    isAlreadyTaken = true
                }
                break
            }
        }
```
Kayıtlı ilaç listesi yüklenir, isimle eşleşen ilaç bulunur, o ilaç zaten "alındı" olarak işaretliyse `isAlreadyTaken = true` yapılır — bu, aynı doz için **iki kere bildirim göstermeyi** engellemek için (örn. kullanıcı erken davranıp ilacı zaten aldıysa, planlanan alarm yine de tetiklenir ama bildirim göstermez).

```kotlin
        if (!isAlreadyTaken) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

            val fullScreenIntent = Intent(context, AlarmActivity::class.java)
            fullScreenIntent.putExtra("MEDICINE_NAME", name)
            fullScreenIntent.putExtra("MEDICINE_TIME", time)
            fullScreenIntent.putExtra("MEDICINE_NOTE", note)
            fullScreenIntent.putExtra("ALARM_ID", alarmId)
            fullScreenIntent.putExtra("START_DATE", startDate)
            fullScreenIntent.putExtra("END_DATE", endDate)
            fullScreenIntent.putExtra("INTERVAL_DAYS", intervalDays)
            fullScreenIntent.putExtra("SOUND_URI", soundUriStr)
            fullScreenIntent.putExtra("IS_CUSTOM_DAY", isCustomDay)
            fullScreenIntent.putExtra("CUSTOM_DAY", customDay)
            fullScreenIntent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_NO_USER_ACTION or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP,
            )

            val fullScreenPI = PendingIntent.getActivity(
                context, alarmId, fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
```
- `fullScreenIntent`: kilit ekranındayken bile açılacak **tam ekran alarm ekranını** (`AlarmActivity`) başlatacak intent.
- `FLAG_ACTIVITY_NEW_TASK`: bir `BroadcastReceiver`'dan Activity başlatırken zorunlu bayrak (receiver'ın kendi "task"ı olmadığı için yeni bir task içinde açılmalı).
- `FLAG_ACTIVITY_NO_USER_ACTION`: bu Activity'nin kullanıcı etkileşimiyle değil, sistem tarafından tetiklendiğini belirtir.
- `FLAG_ACTIVITY_SINGLE_TOP`: aynı alarm ekranı zaten en üstteyse tekrar yeni bir örnek oluşturulmaz.
- `PendingIntent.getActivity(...)`: bu intent'i, bir bildirimin "tam ekran" özelliğine bağlayabilmek için bir `PendingIntent`'e sarar (aşağıda `setFullScreenIntent` ile kullanılacak).

```kotlin
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID, "İlaç Hatırlatıcı", NotificationManager.IMPORTANCE_HIGH,
                )
                channel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC)
                channel.enableVibration(true)
                channel.setSound(null, null)
                nm?.createNotificationChannel(channel)
            }
            buildAndNotify(context, nm, CHANNEL_ID, name, time, note,
                alarmId, startDate, endDate, intervalDays, soundUriStr,
                isCustomDay, customDay, fullScreenPI)
        }
```
- Android 8+'ta (`VERSION_CODES.O`) bildirim göstermeden önce bir **kanal** oluşturulması zorunlu. `IMPORTANCE_HIGH` → kullanıcıya sesli/görsel olarak öne çıkan bir bildirim. `VISIBILITY_PUBLIC` → kilit ekranında da tam içerik görünür. `enableVibration(true)` → titreşim açık.
- `channel.setSound(null, null)`: kanalın **kendi** sesi kasıtlı olarak `null` — sesin **tek kaynağı**, biraz sonra göreceğimiz gibi, `AlarmActivity`'nin çaldığı `MediaPlayer`. (Aşağıdaki `buildAndNotify` içindeki uzun yorum bunu detaylandırıyor.)
- Kanal, sadece Android 8+ üzerinde bir kere oluşturulur (`createNotificationChannel` zaten var olan bir kanalı tekrar oluşturmaya çalışılırsa da güvenlidir, hata vermez); ama `buildAndNotify` çağrısı **her durumda** (eski/yeni Android fark etmeksizin) yapılıyor.

```kotlin
        // ---- KENDİNİ YENİDEN KUR ----
        if (isCustomDay && customDay != 0) {
            // Güne özel: Gelecek haftanın aynı günü için yeniden kur
            rescheduleCustomDayAlarm(context, name, time, note, alarmId, soundUriStr, customDay)
            scheduleNextCustomDayResetAlarm(context, name, time, alarmId, customDay, soundUriStr)
        } else {
            // Standart: Bir sonraki döngü
            rescheduleNextMainAlarm(context, name, time, note, alarmId, startDate, endDate, intervalDays, soundUriStr)
            scheduleNextResetAlarm(context, name, time, alarmId, startDate, endDate, intervalDays, soundUriStr)
        }
    }
```
Bildirim gösterilsin ya da gösterilmesin (isAlreadyTaken olsa bile!), alarm **her zaman** kendini bir sonraki tetiklenme için yeniden kurar — hem ana alarmı hem de karşılık gelen sıfırlama alarmını. Bu, alarm zincirinin hiç kopmamasını sağlıyor.

### `buildAndNotify` — Bildirimi İnşa Et

```kotlin
    private fun buildAndNotify(
        context: Context, nm: NotificationManager?, channelId: String,
        name: String?, time: String?, note: String?, alarmId: Int,
        startDate: Long, endDate: Long, intervalDays: Int,
        soundUriStr: String?, isCustomDay: Boolean, customDay: Int,
        fullScreenPI: PendingIntent,
    ) {
        // "İlaç Aldım" aksiyon
        val takenIntent = Intent(context, NotificationActionReceiver::class.java)
        takenIntent.action = NotificationActionReceiver.ACTION_TAKEN
        takenIntent.putExtra("MEDICINE_NAME", name)
        takenIntent.putExtra("MEDICINE_TIME", time)
        takenIntent.putExtra("ALARM_ID", alarmId)
        val takenPI = PendingIntent.getBroadcast(
            context, alarmId + 1000, takenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
```
Bildirimin üzerine konacak "✓ İlaç Aldım" butonu için, tıklanınca `NotificationActionReceiver`'ı `ACTION_TAKEN` action'ıyla tetikleyecek bir `PendingIntent` hazırlanıyor. `alarmId + 1000`: aynı `alarmId` ana alarm için de kullanıldığından, çakışmayı önlemek için bu buton-intent'ine **farklı** bir kimlik (ana kimliğe 1000 eklenmiş hâli) veriliyor.

```kotlin
        // "Ertele 5 dk" aksiyon
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java)
        snoozeIntent.action = NotificationActionReceiver.ACTION_SNOOZE
        snoozeIntent.putExtra("MEDICINE_NAME", name)
        snoozeIntent.putExtra("MEDICINE_TIME", time)
        snoozeIntent.putExtra("MEDICINE_NOTE", note)
        snoozeIntent.putExtra("ALARM_ID", alarmId)
        snoozeIntent.putExtra("START_DATE", startDate)
        snoozeIntent.putExtra("END_DATE", endDate)
        snoozeIntent.putExtra("INTERVAL_DAYS", intervalDays)
        snoozeIntent.putExtra("SOUND_URI", soundUriStr)
        snoozeIntent.putExtra("IS_CUSTOM_DAY", isCustomDay)
        snoozeIntent.putExtra("CUSTOM_DAY", customDay)
        snoozeIntent.putExtra("SNOOZE_MINUTES", 5)
        val snoozePI = PendingIntent.getBroadcast(
            context, alarmId + 2000, snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
```
Aynı mantık "Ertele" butonu için — bu sefer `alarmId + 2000` (üçüncü bir çakışmasız kimlik alanı). Not: `"SNOOZE_MINUTES", 5` extra'sı burada konsa da, `NotificationActionReceiver.onReceive`'in ACTION_SNOOZE dalı bu değeri **kullanmıyor**, kendi rastgele süresini üretiyor (bkz. bir önceki bölüm) — bu extra fiilen okunmuyor.

```kotlin
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_medicine)
            .setContentTitle("İlaç Vakti: $name")
            .setContentText("$time — İlacınızı almayı unutmayın.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 500, 200, 500))
            .setFullScreenIntent(fullScreenPI, true)
            .addAction(R.drawable.ic_medicine_white, "✓ İlaç Aldım", takenPI)
            .addAction(R.drawable.ic_note, "⏰ Ertele", snoozePI)
```
Bildirimin görsel/davranışsal her özelliği burada zincirleme (`builder pattern`) çağrılarla ayarlanıyor:
- `setPriority(PRIORITY_MAX)`/`setCategory(CATEGORY_ALARM)`: sisteme "bu çok önemli, bir alarm bildirimi" der.
- `setAutoCancel(false)`: kullanıcı bildirime dokunsa bile otomatik kapanmaz (sadece butonlara basınca ya da `NotificationManager.cancel()` çağrılınca kapanır).
- `setOngoing(true)`: "devam eden" bildirim — kullanıcı kaydırarak silemez, sadece kod tarafından kapatılabilir.
- `setVibrate(longArrayOf(0, 500, 200, 500))`: titreşim deseni — `[bekle 0ms, titreşim 500ms, bekle 200ms, titreşim 500ms]`.
- `setFullScreenIntent(fullScreenPI, true)`: bu bildirimin, cihaz kilitliyken (veya belirli koşullarda) **tam ekran** olarak (yani `AlarmActivity` otomatik açılarak) gösterilmesini sağlayan asıl mekanizma.
- `addAction(...)` iki kez: bildirime iki buton ekler.

```kotlin
        // Bildirime KASITLI OLARAK ses eklenmiyor (builder.setSound(...) çağırmayın).
        // androidx.core.app.NotificationCompatBuilder, channelId'li bir builder'da API 26+
        // için Builder.setSound(...)'u her zaman sessizce mBuilder.setSound(null) ile
        // geçersiz kılıyor (bkz. NotificationCompatBuilder.buildInternal()) — yani bu
        // satıra ne yazarsak yazalım gerçek cihazda (Android 8+) hiçbir zaman çalmaz,
        // sadece yanıltıcı olur. Kanal da (yukarıda) zaten sessiz oluşturuluyor.
        // Sesin TEK kaynağı AlarmActivity.playAlarmSound() — tam ekran intent
        // (yukarıdaki setFullScreenIntent) hem kilitli hem kilitsiz durumda güvenilir
        // şekilde açılıp AlarmActivity'yi tetikler, çift ses riski de böylece ortadan kalkar.

        nm?.notify(alarmId, builder.build())
    }
}
```
Bu uzun yorum, gerçek dünyada karşılaşılan ince bir Android davranışını belgeliyor: Android 8+'ta bildirim sesi **kanal seviyesinde** belirlenir, `Notification.Builder`/`NotificationCompat.Builder` üzerinden ayarlanan ses her zaman görmezden gelinir (AndroidX'in kendi kütüphane davranışı gereği). Bu yüzden kod bilinçli olarak bildirimin kendi sesini **hiç ayarlamıyor**; ses tamamen `AlarmActivity`'nin kendi `MediaPlayer`'ına bırakılmış — hem daha güvenilir (çünkü tam ekran intent her koşulda `AlarmActivity`'yi açar) hem de "iki yerden aynı anda ses çalma" riskini ortadan kaldırıyor. Bu, projenin ilk versiyonunda bulunup düzeltilen bir başka gerçek hataydı.
- `nm?.notify(alarmId, builder.build())`: bildirim, `alarmId`'yi kimlik olarak kullanarak sisteme gönderilir (`?.` → `nm` her ihtimale karşı `null` olabileceği için güvenli çağrı).

### Kendini Yeniden Kurma Fonksiyonları (companion object içinde, `@JvmStatic`)

Bunların hepsi `companion object` içinde ve `@JvmStatic` işaretli — yani hem `AlarmReceiver.fonksiyonAdı(...)` şeklinde dışarıdan (örn. `MainActivity`, `BootReceiver`) çağrılabiliyorlar, hem de `AlarmReceiver`'ın kendi instance metotlarından erişilebiliyorlar.

```kotlin
        @JvmStatic
        fun rescheduleNextMainAlarm(
            context: Context, name: String?, time: String?, note: String?,
            alarmId: Int, startDate: Long, endDate: Long,
            intervalDays: Int, soundUriStr: String?,
        ) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            if (time == null || !time.contains(":")) return

            val parts = time.split(":")
            val hour: Int
            val minute: Int
            try {
                hour = parts[0].toInt()
                minute = parts[1].toInt()
            } catch (e: Exception) {
                return
            }

            val next = Calendar.getInstance()
            next.set(Calendar.HOUR_OF_DAY, hour)
            next.set(Calendar.MINUTE, minute)
            next.set(Calendar.SECOND, 0)
            next.set(Calendar.MILLISECOND, 0)
            next.add(Calendar.DAY_OF_YEAR, intervalDays) // Bir sonraki döngü
```
Ana alarmı **bir sonraki** tekrar için kurar — `intervalDays` kadar gün ekleyerek (standart günlük ilaçlarda `intervalDays = 1`, yani yarına; "3 günde bir" ilaçlarda `intervalDays = 3`).

```kotlin
            // Bitiş tarihi geçtiyse yeniden kurma
            if (endDate != 0L) {
                val endCal = Calendar.getInstance()
                endCal.timeInMillis = endDate
                endCal.set(Calendar.HOUR_OF_DAY, 23)
                endCal.set(Calendar.MINUTE, 59)
                if (next.timeInMillis > endCal.timeInMillis) return
            }
```
Hesaplanan bir sonraki tarih, ilacın bitiş tarihini geçiyorsa, **hiçbir şey kurmadan** çıkılır — zincir burada doğal olarak sonlanır (ilacın kullanım süresi bitmiştir).

```kotlin
            val intent = Intent(context, AlarmReceiver::class.java)
            intent.putExtra("MEDICINE_NAME", name)
            intent.putExtra("MEDICINE_TIME", time)
            intent.putExtra("MEDICINE_NOTE", note)
            intent.putExtra("ALARM_ID", alarmId)
            intent.putExtra("START_DATE", startDate)
            intent.putExtra("END_DATE", endDate)
            intent.putExtra("INTERVAL_DAYS", intervalDays)
            intent.putExtra("SOUND_URI", soundUriStr)
            intent.putExtra(EXTRA_IS_RESET, false)

            val pi = PendingIntent.getBroadcast(
                context, alarmId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            setExact(alarmManager, next.timeInMillis, pi)
        }
```
Aynı `alarmId` ile yeni bir `AlarmReceiver` intent'i kurulup `setExact` (bu dosyanın en altındaki ortak yardımcı fonksiyon) ile sisteme kaydediliyor. `EXTRA_IS_RESET = false` → bu, bir **ana** alarm.

```kotlin
        @JvmStatic
        fun rescheduleCustomDayAlarm(
            context: Context, name: String?, time: String?, note: String?,
            alarmId: Int, soundUriStr: String?, calendarDay: Int,
        ) {
            ...
            next.set(Calendar.DAY_OF_WEEK, calendarDay)
            next.add(Calendar.WEEK_OF_YEAR, 1) // Gelecek hafta
            ...
        }
```
Yukarıdakinin güne-özel-mod karşılığı: bitiş tarihi kontrolü **yok** (güne özel modda süresiz çalışır), gün doğrudan `calendarDay`'e ayarlanıp bir **hafta** ileri atılıyor (çünkü güne özel alarm haftalık tekrarlıyor).

```kotlin
        @JvmStatic
        fun scheduleNextResetAlarm(
            context: Context, name: String?, time: String?, alarmId: Int,
            startDate: Long, endDate: Long,
            intervalDays: Int, soundUriStr: String?,
        ) {
            ...
            var resetMillis = reset.timeInMillis - (60 * 60 * 1000) // asıl alarmdan 60 dk öncesi

            // Eğer bu reset saati (bugün için) geçmişse, gelecekteki uygun saate kadar interval ekle
            while (resetMillis <= System.currentTimeMillis()) {
                resetMillis += (intervalDays.toLong() * 24 * 60 * 60 * 1000)
            }
```
Sıfırlama alarmı, ana alarmın saatinden **1 saat (60×60×1000 ms) önce** hesaplanır. `while` döngüsü: eğer bu hesaplanan an zaten geçmişse, ilacın tekrar aralığı kadar (`intervalDays` gün) ekleyerek **gelecekteki ilk uygun ana** bulunur — bu bir `if` değil `while`, çünkü örneğin günlük bir ilaçta birden fazla gün atlanmış olabilir (uygulama uzun süre açılmadıysa).

```kotlin
            val resetAlarmId = AlarmHelper.safeId(name + time + "_reset")
            ...
            intent.putExtra(EXTRA_IS_RESET, true)
            ...
            setExact(alarmManager, reset.timeInMillis, pi)
        }
```
Sıfırlama alarmının kimliği, ana alarmdan **farklı** (sonuna `"_reset"` eklenerek) — böylece ikisi birbirinin üzerine yazmaz, ikisi de `AlarmManager`'da ayrı ayrı bekler. `EXTRA_IS_RESET = true` bu alarmın "sıfırlama" türünde olduğunu işaretler.

```kotlin
        /** Ses URI'sini çözümle: null/boş → varsayılan alarm sesi */
        @JvmStatic
        fun resolveSoundUri(soundUriStr: String?): Uri? {
            if (!soundUriStr.isNullOrEmpty()) {
                return Uri.parse(soundUriStr)
            }
            return android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM)
                ?: android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
        }
```
Kullanıcı özel bir ses seçtiyse onu `Uri`'ye çevirir; seçmediyse sistemin varsayılan **alarm** sesini, o da yoksa varsayılan **bildirim** sesini döndürür. `AlarmActivity`'nin çalacağı sesi belirleyen tek yer burası.

```kotlin
        @JvmStatic
        fun scheduleNextCustomDayResetAlarm(
            context: Context, name: String?, time: String?, alarmId: Int,
            customDay: Int, soundUriStr: String?,
        ) {
            ...
            reset.set(Calendar.DAY_OF_WEEK, customDay)
            var resetMillis = reset.timeInMillis - (60 * 60 * 1000) // 1 saat öncesi

            while (resetMillis <= System.currentTimeMillis()) {
                resetMillis += (7L * 24 * 60 * 60 * 1000)
            }
            ...
        }

        private fun setExact(alarmManager: AlarmManager, triggerAt: Long, pi: PendingIntent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pi)
            }
        }
    }
}
```
`scheduleNextCustomDayResetAlarm`, standart moddaki sıfırlama kurulumunun güne-özel karşılığı (haftada bir tekrarladığı için `7 gün` ekleniyor, `intervalDays` gün değil). `setExact`, bu dosyadaki **tüm** alarm kurma fonksiyonlarının ortak son adımı — Android 12+ izin kontrolü dahil (bkz. `BootReceiver` bölümündeki aynı mantık).

---

## AlarmActivity.kt — Tam Ekran Alarm Ekranı (Compose)

`AlarmReceiver`'ın `setFullScreenIntent` ile açtığı, kullanıcının fiilen gördüğü alarm ekranı. Ses çalar, "Durdur" ve "Ertele" butonları vardır, 5 dakika içinde cevap verilmezse otomatik ertelenir. **Bu dosya artık tamamen Jetpack Compose** — eskiden `activity_alarm.xml` (ConstraintLayout + View Material Components) kullanıyordu, şimdi tüm ekran `setContent { }` içinde Kotlin koduyla çiziliyor.

### Activity kısmı — değişmeyen mantık

```kotlin
class AlarmActivity : AppCompatActivity() {

    private var mediaPlayer: MediaPlayer? = null
    private val timeoutHandler = Handler(Looper.getMainLooper())
    private val screenOffReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (Intent.ACTION_SCREEN_OFF == intent.action) {
                stopAlarmSound()
            }
        }
    }

    private var medicineName: String? = null
    private var medicineTime: String? = null
    private var medicineNote: String? = null
    private var notificationId = 0
    private var startDate: Long = 0
    private var endDate: Long = 0
    private var intervalDays = 1
    private var soundUriStr: String? = null
    private var isCustomDay = false
    private var customDay = 0
```
Bu alan tanımları, Compose'a geçmeden **önceki** View sürümüyle birebir aynı — burada hiçbir şey değişmedi. `mediaPlayer`, `timeoutHandler`, `screenOffReceiver`, ve intent'ten okunacak ilaç bilgileri Compose'dan tamamen bağımsız, saf Kotlin/Android durumu.

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val filter = IntentFilter(Intent.ACTION_SCREEN_OFF)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            registerReceiver(screenOffReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenOffReceiver, filter)
        }

        // Kilit Ekranında Açılma
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager
            km?.requestDismissKeyguard(this, null)
        } else {
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    or WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    or WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        medicineName = intent.getStringExtra("MEDICINE_NAME")
        medicineTime = intent.getStringExtra("MEDICINE_TIME")
        medicineNote = intent.getStringExtra("MEDICINE_NOTE")
        notificationId = intent.getIntExtra("ALARM_ID", 0)
        startDate = intent.getLongExtra("START_DATE", 0)
        endDate = intent.getLongExtra("END_DATE", 0)
        intervalDays = intent.getIntExtra("INTERVAL_DAYS", 1)
        soundUriStr = intent.getStringExtra("SOUND_URI")
        isCustomDay = intent.getBooleanExtra("IS_CUSTOM_DAY", false)
        customDay = intent.getIntExtra("CUSTOM_DAY", 0)

        playAlarmSound()

        setContent {
            IlacTakipTheme {
                AlarmScreen(
                    medicineName = medicineName ?: "İlaç Zamanı",
                    medicineTime = medicineTime ?: "",
                    medicineNote = medicineNote,
                    onStop = {
                        timeoutHandler.removeCallbacksAndMessages(null)
                        stopAlarmSound()
                        clearNotification()
                        markAsTaken()
                        finish()
                    },
                    onSnooze = { handleRandomSnooze() },
                )
            }
        }

        timeoutHandler.postDelayed({
            stopAlarmSound()
            clearNotification()
            snoozeAlarm(5)
            finish()
        }, ALARM_TIMEOUT_MS)
    }
```
Pencere bayrakları (`setShowWhenLocked`, `setTurnScreenOn`, `requestDismissKeyguard`, `FLAG_KEEP_SCREEN_ON`), `screenOffReceiver` kaydı, intent verilerinin okunması ve `timeoutHandler.postDelayed` — **hiçbiri değişmedi**. Tek fark: eskiden `setContentView(R.layout.activity_alarm)` + `findViewById` ile view'lar bağlanıp `.text = ...` atanıyordu; şimdi `setContent { IlacTakipTheme { AlarmScreen(...) } }` çağrılıyor ve ilaç bilgileri/callback'ler doğrudan **parametre olarak** Composable'a geçiliyor. `AlarmScreen`'in kendisi durumsuz (stateless) — sadece verilen veriyi çiziyor, tıklamalarda dışarıdan verilen `onStop`/`onSnooze` lambda'larını çağırıyor.

`clearNotification`, `handleRandomSnooze`, `markAsTaken`, `playAlarmSound`/`playDefaultAlarmSound`/`startPlayback` (ses + varsayılana düşme mantığı), `stopAlarmSound`, `snoozeAlarm`, `onDestroy`, `onKeyDown` — bunların **hepsi aynı kaldı**, View'a hiç bağımlı değillerdi zaten. (Ses mantığı için bkz. bu dosyanın en son güncellemesi: özel ses hazırlanamazsa artık sessiz kalmak yerine sistemin varsayılan alarm sesine düşüyor.)

### `AlarmScreen` — Compose UI

```kotlin
@Composable
private fun AlarmScreen(
    medicineName: String,
    medicineTime: String,
    medicineNote: String?,
    onStop: () -> Unit,
    onSnooze: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                ),
            ),
    ) {
        // Statik "nabız" dairesi — orijinal bg_circle_pulse.xml'in sadık taşıması (o da animasyonsuzdu)
        Box(
            modifier = Modifier
                .size(200.dp)
                .align(Alignment.Center)
                .background(Color.White.copy(alpha = 0.12f), CircleShape),
        )
```
- Arka plan: eskiden `bg_gradient_header.xml` (statik XML gradient drawable) kullanılıyordu; şimdi `Brush.verticalGradient(listOf(primary, secondary))` ile **aynı görsel** doğrudan Compose'da üretiliyor. (Not: `MainActivity` ve `AddMedicineActivity` ekranları artık gradyan banner yerine düz M3 `TopAppBar` kullanıyor — bu tam ekran alarm gradyanı yalnızca `AlarmActivity`'ye özgü, bilinçli bir görsel tercih.)
- Ortadaki daire: orijinal `bg_circle_pulse.xml` ismine rağmen **hiç animasyonlu değildi** — sabit, %30 opak, 120dp'lik bir oval `shape` drawable'ıydı. Sadık bir taşıma olarak burada da statik, yarı saydam bir `Box(CircleShape)` kullanıldı (gerçek bir "nabız" animasyonu istenirse `rememberInfiniteTransition` ile kolayca eklenebilir — şimdilik orijinaliyle bire bir).

```kotlin
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_medicine_white),
                    contentDescription = "İlaç",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(72.dp),
                )
                Text("İLAÇ VAKTİ", color = Color.White.copy(alpha = 0.7f), fontSize = 14.sp, ...)
                Text(medicineName, color = Color.White, fontSize = 28.sp, ...)
                Text(medicineTime, color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Light, ...)
                if (!medicineNote.isNullOrEmpty()) {
                    Text("Not: $medicineNote", color = Color.White.copy(alpha = 0.7f), ...)
                }
            }
```
- `Icon(..., tint = Color.Unspecified, ...)`: `ic_medicine_white.xml` zaten kendi içinde beyaz renkli bir vektör; `Icon` composable'ı varsayılan olarak kendi tema rengiyle **yeniden boyardı** (tint uygular) — `Color.Unspecified` vererek bu otomatik tint'i devre dışı bırakıp ikonun kendi (beyaz) rengini koruyoruz.
- Ortadaki `Column` `Modifier.weight(1f)` ile "kalan tüm dikey alanı kapla, içeriği dikeyde ortala" — böylece ikon/isim/saat/not bloğu ekranın ortasında, butonlar hep en altta kalıyor (ekran boyutundan bağımsız).
- Metinler ("İLAÇ VAKTİ" etiketi, ilaç adı, büyük saat, not) birebir orijinal `activity_alarm.xml`'deki boyut/renk/kalınlık değerleriyle eşleşiyor.

```kotlin
            Button(
                onClick = onStop,
                modifier = Modifier.fillMaxWidth().height(64.dp).padding(bottom = 12.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.White,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text("✓  İlacı Aldım", fontSize = 18.sp, fontWeight = FontWeight.Medium)
            }

            OutlinedButton(
                onClick = onSnooze,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                border = BorderStroke(1.5.dp, Color.White.copy(alpha = 0.5f)),
            ) {
                Text("⏰  Ertele", fontSize = 16.sp)
            }
        }
    }
}
```
- "İlacı Aldım": dolu (`Button`) M3 bileşeni, beyaz zemin + primary renginde yazı — orijinal `MaterialButton`'ın `backgroundTint="@color/white"` + `textColor="@color/primary"` kombinasyonunun Compose karşılığı.
- "Ertele": çerçeveli (`OutlinedButton`), beyaz yazı + yarı saydam beyaz kenarlık (`BorderStroke`) — orijinaldeki `strokeColor="#80FFFFFF"` ile aynı görsel.
- İkisi de `onClick`'te doğrudan Activity'den gelen `onStop`/`onSnooze` lambda'larını çağırıyor; buton kendisi hiçbir iş mantığı bilmiyor.

---

## AddMedicineActivity.kt — İlaç Ekle/Düzenle Ekranı (Compose)

Projenin en büyük dosyası — hem yeni ilaç ekleme hem de var olan bir ilacı düzenleme, **tek** ekran ve **tek** sınıf üzerinden yapılıyor (`isEditMode` bayrağıyla ayrılıyor). **Artık tamamen Compose** — eskiden `activity_add_medicine.xml` (View Material Components: `MaterialCardView`, `TextInputLayout`, `MaterialButtonToggleGroup`, `Chip`, `MaterialDatePicker`) kullanıyordu.

### Genel mimari: form durumu ayrı bir sınıfta

```kotlin
class AddMedicineActivity : AppCompatActivity() {

    private var isEditMode = false
    private var editPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val editingMedicine = intent.getSerializableExtra("edit_medicine") as? Medicine
        isEditMode = editingMedicine != null
        editPosition = intent.getIntExtra("edit_position", -1)

        setContent {
            IlacTakipTheme {
                val state = remember {
                    AddMedicineFormState().apply {
                        editingMedicine?.let { loadFrom(it, this@AddMedicineActivity) }
                    }
                }

                AddMedicineScreen(
                    state = state,
                    isEditMode = isEditMode,
                    onBack = { finish() },
                    onSave = { saveAlarm(state) },
                )
            }
        }
    }
```
Eskiden bu ekranın **tüm** durumu (isim, saat, seçili günler, frekans, ses, tarih aralığı...) Activity üzerinde tek tek `lateinit var`/`var` alanlar ve `findViewById` ile bağlanan view referanslarıydı. Şimdi bunların hepsi ayrı, küçük bir **form durumu sınıfı** (`AddMedicineFormState`) içinde, Compose'un `mutableStateOf` alanları olarak toplanıyor. `remember { AddMedicineFormState().apply { ... } }`: bu nesne **sadece bir kere**, ilk composition'da oluşturuluyor ve döndürülen aynı nesne sonraki her yeniden çizimde korunuyor (Activity yeniden yaratılmadığı sürece). Düzenleme modundaysa, oluşturulur oluşturulmaz `loadFrom(medicine, context)` ile var olan ilacın verileriyle dolduruluyor.

```kotlin
private class AddMedicineFormState {
    var name by mutableStateOf("")
    var note by mutableStateOf("")
    var nameError by mutableStateOf<String?>(null)

    var isCustomDayMode by mutableStateOf(false)

    var hour by mutableIntStateOf(8)
    var minute by mutableIntStateOf(0)
    var freqText by mutableStateOf(FREQUENCIES[0])

    val customDayTimes = mutableStateMapOf<Int, String>()
    var customFreqText by mutableStateOf(CUSTOM_FREQUENCIES[0])

    var selectedStartDate by mutableLongStateOf(0L)
    var selectedEndDate by mutableLongStateOf(0L)
    var dateRangeText by mutableStateOf("")

    var soundUri by mutableStateOf<String?>(null)
    var soundTitle by mutableStateOf("🔔 Varsayılan Alarm Sesi")

    var dayTimeDialogFor by mutableStateOf<Int?>(null)
    var showDateRangeDialog by mutableStateOf(false)
    ...
}
```
- `var x by mutableStateOf(...)`: Kotlin'in **property delegation** özelliği (`by` anahtar kelimesi) + Compose'un `mutableStateOf`'u birleşince, `x`'e her atama Compose'a "bir şey değişti, ilgili UI'ı yeniden çiz" sinyali gönderiyor — ama kodun geri kalanında `x` sanki düz bir `var` gibi okunup yazılabiliyor.
- `mutableIntStateOf`/`mutableLongStateOf`: `Int`/`Long` için otomatik kutulama (autoboxing) yapmayan, hafif performans optimizasyonlu özel versiyonlar.
- `mutableStateMapOf<Int, String>()`: Compose'un gözlemlenebilir `Map`'i — eskiden `HashMap<Int, String>()` olan `customDayTimes` artık bu; içine ekleme/çıkarma (`.remove(key)`, `[key] = value`) otomatik olarak ekranı günceller, elle `refreshCustomDayTimesList()`/`removeAllViews()`/`addView()` çağırmaya gerek kalmıyor.
- `dayTimeDialogFor: Int?`: `null` ise hiçbir gün-saat dialogu açık değil; bir `Calendar` gün sabitiyse, o günün saat seçme dialogu açık demektir — eskiden ayrı bir `TimePickerDialog(...).show()` çağrısıyla yönetilen bu akış, artık tek bir "hangi dialog açık" state'ine indirgendi.

```kotlin
    fun loadFrom(medicine: Medicine, context: Context) {
        name = medicine.name
        note = medicine.note ?: ""

        val medicineCustomDayTimes = medicine.customDayTimes
        if (medicine.isUseCustomDays && medicineCustomDayTimes != null) {
            isCustomDayMode = true
            customDayTimes.clear()
            customDayTimes.putAll(medicineCustomDayTimes)
            customFreqText = resolveFrequencyText(medicine)
        } else {
            isCustomDayMode = false
            freqText = resolveFrequencyText(medicine)
            // ... firstTime ayrıştırma, hour/minute ataması, tarih aralığı ...
        }

        soundUri = medicine.soundUri
        // ... RingtoneManager ile ses başlığı çözümü ...
    }
```
**Önemli basitleşme:** eski View sürümünde, düzenleme modunda Chip'leri programatik olarak işaretlemek (`chip.isChecked = true`) o Chip'in `OnCheckedChangeListener`'ını **kullanıcı tıklamasıyla aynı şekilde** tetikliyordu — bu da istenmeyen bir saat-seçme dialogu açılmasına yol açıyordu. Bunu engellemek için `isLoadingEditData` adında bir bayrak + `View.post { }` ile "async callback'leri yakalayıp bayrağı sonra kaldırma" gibi kırılgan bir hack gerekiyordu. Compose'da bu sorun **kavramsal olarak yok**: `loadFrom` içinde `customDayTimes.putAll(...)` yapmak sadece veriyi dolduruyor; hiçbir "dinleyici" tetiklenmiyor (bir Chip'in `selected` durumu artık bir olay değil, doğrudan `customDayTimes.containsKey(day)`'den **türetilen** bir değer). Bu yüzden `isLoadingEditData` bayrağının Compose sürümünde hiç karşılığı yok — tamamen ortadan kalktı.

`resolveFrequencyText` (Medicine'den "12 Saatte Bir" gibi bir metin türetme mantığı) artık dosyanın üstünde **top-level bir fonksiyon** — hem `loadFrom` hem de (dolaylı olarak) form varsayılanları için kullanılabiliyor, Activity'ye bağımlı değil.

### Kaydetme mantığı — aynı, sadece View yerine state okuyor

```kotlin
    private fun saveAlarm(state: AddMedicineFormState) {
        val name = state.name.trim()
        val note = state.note.trim()

        if (name.isEmpty()) {
            state.nameError = "İlaç adı gerekli"
            return
        }
        if (isDuplicateName(name)) {
            state.nameError = "Bu isimde bir ilaç zaten mevcut"
            Toast.makeText(this, "Aynı isimde ilaç eklenemez", Toast.LENGTH_SHORT).show()
            return
        }

        val med = if (state.isCustomDayMode) {
            saveCustomDayAlarm(state, name, note)
        } else {
            saveStandardAlarm(state, name, note)
        } ?: return

        med.soundUri = state.soundUri
        // ... isEditMode ise eski isTaken korunur, sonuç Intent'i MainActivity'ye döner ...
    }
```
`saveAlarm`, `saveStandardAlarm`, `saveCustomDayAlarm`, `isDuplicateName` — **mantığın kendisi hiç değişmedi** (saat→frekans hesaplama matematiği, tarih varsayılanı, isim çakışma kontrolü birebir aynı). Tek fark: `timePicker.hour`/`frequencyDropdown.text.toString()` gibi View okumaları yerine `state.hour`/`state.freqText` gibi state okumaları var.

> **Gizli özellik açığa çıkarıldı:** Orijinal XML'de güne özel moddaki frekans seçici (`customFrequencyInputLayout`) `android:visibility="gone"` olarak sabitti ve kodda **hiçbir yerde** görünür yapılmıyordu — yani kullanıcılar güne özel ilaçlarda asla "6/8/12 saatte bir" seçemiyor, hep sessizce "Günde 1 defa" kalıyordu. Compose sürümünde bu dropdown artık standart moddaki gibi her zaman gösteriliyor — kullanıcının onayıyla, gerçek bir özelliğe dönüştürüldü.

### Compose UI — `AddMedicineScreen`

Ekran, `MainActivity`'deki gibi tek bir `Scaffold` üzerine kuruluyor: `topBar` yuvasında bir native M3 `TopAppBar` (sol üstte kapat ikonu + "Yeni İlaç Ekle"/"İlacı Düzenle" başlığı), altında dikey kaydırılabilir (`verticalScroll`) bir `Column` içinde kartlar. Eskiden başlık, üstte primary→secondary gradyanlı özel bir `Box` banner'dı; native M3 için `TopAppBar`'a çevrildi.

```kotlin
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "İlacı Düzenle" else "Yeni İlaç Ekle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "Kapat",
                            // 45° döndürülmüş "+" ikonu bir "×" (kapat) simgesi veriyor
                            modifier = Modifier.rotate(45f),
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
        ) {
            FormCard(title = "💊 İlaç Bilgileri") { ... }
            ...
        }
    }
```
- `navigationIcon`: ayrı bir "kapat" (×) vektörü eklemek yerine, mevcut `ic_add` ("+") vektörü `Modifier.rotate(45f)` ile 45° döndürülüyor — döndürülmüş bir artı işareti görsel olarak bir çarpı (×) veriyor.
- `.padding(padding)`: `Scaffold`'ın verdiği inset (üst çubuk yüksekliği + edge-to-edge sistem çubukları) uygulanıyor; sonra `.verticalScroll(...)` içeriği kaydırılabilir yapıyor; en içte `.padding(20.dp)` içerik kenar boşluğu veriyor.

Tekrarlanan "kart" yapısını sadeleştirmek için küçük bir yardımcı var:

```kotlin
@Composable
private fun FormCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        shape = RoundedCornerShape(16.dp),
        // containerColor elle verilmiyor — bkz. MedicineCard'daki aynı not: M3'ün
        // varsayılanı (surfaceContainerLow) arka plandan otomatik ayrışan ton veriyor.
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp, fontWeight = FontWeight.Medium, ...)
            content()
        }
    }
}
```
Orijinal XML'deki 5 `MaterialCardView` bölümü (İlaç Bilgileri, Zamanlama Modu, Standart/Güne Özel, Tarih & Ses) hepsi aynı "başlık + kart" kalıbını tekrarlıyordu — burada tek bir `FormCard` fonksiyonuna indirgendi, `content: @Composable ColumnScope.() -> Unit` sayesinde her çağıran kendi içeriğini (metin kutuları, butonlar vb.) doğrudan bir `Column` içindeymiş gibi yazabiliyor. (Kart rengi elle verilmiyor — `MedicineCard` bölümündeki tonal renk açıklamasıyla aynı sebep.)

Eşleme tablosu (View → Compose M3):

| Eski (View) | Yeni (Compose M3) |
|---|---|
| `TextInputLayout` + `TextInputEditText` (isim, not) | `OutlinedTextField` |
| `MaterialButtonToggleGroup` (Standart/Güne Özel) | `SingleChoiceSegmentedButtonRow` + 2× `SegmentedButton` |
| `TimePicker` (spinner, standart moddaki inline saat seçici) | `TimeInput` (kart içine gömülü, kompakt) |
| Güne özel modda her gün için `TimePickerDialog` | `AlertDialog` içinde `TimePicker` (dial) + Tamam/İptal |
| `AutoCompleteTextView` + `ExposedDropdownMenu` (2 dropdown) | `ExposedDropdownMenuBox` + `ExposedDropdownMenu` + salt-okunur `OutlinedTextField` (bkz. `FrequencyDropdown` yardımcı fonksiyonu) |
| `ChipGroup` + 7× `Chip` (gün seçimi) | `FlowRow` + 7× `FilterChip` |
| Kodda elle üretilen `LinearLayout` satırları (seçili gün/saat listesi) | `DAY_ORDER.filter { customDayTimes.containsKey(it) }` + düz bir `Column`/`Row` döngüsü |
| `MaterialButton` (tarih aralığı, ses seçimi) | `OutlinedButton` |
| `MaterialDatePicker.dateRangePicker()` | `DateRangePicker` (`rememberDateRangePickerState`) bir `DatePickerDialog` içinde |
| `MaterialButton` (Kaydet/Güncelle) | `Button` (dolu, tam genişlik) |
| Header'daki geri ikonu (`ImageView` + 45° rotasyon) | `TopAppBar`'ın `navigationIcon`'u: `IconButton` + `Icon` + `Modifier.rotate(45f)` — aynı `ic_add` vektörü, aynı görsel hile |

Öne çıkan noktalar:

```kotlin
if (!state.isCustomDayMode) {
    FormCard(title = "⏰ Alarm Zamanı") {
        val timePickerState = rememberTimePickerState(
            initialHour = state.hour, initialMinute = state.minute, is24Hour = true,
        )
        LaunchedEffect(timePickerState.hour, timePickerState.minute) {
            state.hour = timePickerState.hour
            state.minute = timePickerState.minute
        }
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            TimeInput(state = timePickerState)
        }
        ...
    }
}
```
`rememberTimePickerState` kendi iç durumunu tutan bir nesne döndürür; kullanıcı saat/dakikayı değiştirdikçe bu nesnenin `hour`/`minute` alanları değişir. `LaunchedEffect(timePickerState.hour, timePickerState.minute) { ... }`: bu iki değerden **herhangi biri** değiştiğinde bloğu çalıştırıp `state.hour`/`state.minute`'a kopyalıyor — böylece asıl form durumu (`saveAlarm` içinde okunan) her zaman güncel kalıyor.

```kotlin
state.dayTimeDialogFor?.let { day ->
    val existing = state.customDayTimes[day]
    val (initHour, initMinute) = remember(day) {
        if (existing != null) {
            val parts = existing.split(":")
            parts[0].toInt() to parts[1].toInt()
        } else {
            val now = Calendar.getInstance()
            now.get(Calendar.HOUR_OF_DAY) to now.get(Calendar.MINUTE)
        }
    }
    val dayPickerState = rememberTimePickerState(initialHour = initHour, initialMinute = initMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = { state.dayTimeDialogFor = null },
        confirmButton = {
            TextButton(onClick = {
                state.customDayTimes[day] = String.format(Locale.getDefault(), "%02d:%02d", dayPickerState.hour, dayPickerState.minute)
                state.dayTimeDialogFor = null
            }) { Text("Tamam") }
        },
        dismissButton = { TextButton(onClick = { state.dayTimeDialogFor = null }) { Text("İptal") } },
        text = { TimePicker(state = dayPickerState) },
    )
}
```
Güne özel moddaki gün-saat seçme dialogu — bir Chip'e (henüz seçili olmayan bir güne) tıklanınca `state.dayTimeDialogFor = calDay` atanır, bu blok görünür olur. **Sadece "Tamam"a basılırsa** `customDayTimes` haritasına yeni giriş eklenir. Bunun ilginç bir yan etkisi var: eski View sürümünde, kullanıcı bir Chip'i işaretleyip dialogu **iptal ederse**, Chip görsel olarak "seçili" kalırdı ama haritada hiçbir kayıt oluşmazdı (tutarsız bir ara durum). Compose sürümünde Chip'in `selected` durumu doğrudan `customDayTimes.containsKey(calDay)`'den türetildiği için, iptal edilen bir dialog haritaya hiç yazmaz ve Chip otomatik olarak **işaretsiz** görünür — tek-kaynak-doğruluk (single source of truth) tasarımının doğal, bilinçli olarak aranmamış ama hoş bir yan faydası.

```kotlin
if (state.showDateRangeDialog) {
    val rangeState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = state.selectedStartDate.takeIf { it != 0L },
        initialSelectedEndDateMillis = state.selectedEndDate.takeIf { it != 0L },
    )
    DatePickerDialog(
        onDismissRequest = { state.showDateRangeDialog = false },
        confirmButton = {
            TextButton(onClick = {
                val start = rangeState.selectedStartDateMillis
                val end = rangeState.selectedEndDateMillis
                if (start != null && end != null) {
                    state.selectedStartDate = start
                    state.selectedEndDate = end
                    state.dateRangeText = formatDateRange(start, end)
                }
                state.showDateRangeDialog = false
            }) { Text("Tamam") }
        },
        dismissButton = { TextButton(onClick = { state.showDateRangeDialog = false }) { Text("İptal") } },
    ) {
        DateRangePicker(state = rangeState)
    }
}
```
Eskiden `MaterialDatePicker` kendi başlık metnini (`headerText`, örn. `"1 Oca - 31 Oca"`) otomatik üretiyordu. Compose'un `DateRangePicker`'ı bu görsel özeti üretmiyor — sadece seçilen başlangıç/bitiş milisaniyelerini (`selectedStartDateMillis`/`selectedEndDateMillis`) veriyor, bu yüzden küçük bir `formatDateRange(start, end)` yardımcı fonksiyonu (`SimpleDateFormat("d MMM", Locale.forLanguageTag("tr"))`) eklendi.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrequencyDropdown(label: String, options: Array<String>, selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
        OutlinedTextField(
            value = selected, onValueChange = {}, readOnly = true, label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}
```
Bu, hem standart hem güne özel moddaki frekans seçiciler için **ortak** bir yardımcı — M3'ün standart "açılır menü + salt-okunur metin kutusu" kalıbı. `ExposedDropdownMenu`, `ExposedDropdownMenuBox`'ın **kapsam içi (scope) üyesi** bir fonksiyon olduğu için ayrıca import edilmiyor, doğrudan `ExposedDropdownMenuBox { ... }` bloğunun içinde çağrılabiliyor. `@OptIn(ExperimentalMaterial3Api::class)`: bu API'ler Material3 kütüphanesinde hâlâ "deneysel" işaretli (kararsız oldukları için değil, imza yüzeyinin henüz kesinleşmediği belirtildiği için) — geniş çapta üretimde kullanılan, ama açık onay gerektiren bir işaretleme.

---

## MainActivity.kt — Ana Ekran (Jetpack Compose)

Uygulama ikonuna basınca açılan ilk ekran; ilaç listesini gösterir. Bu dosya **iki kısımdan** oluşuyor: `MainActivity` sınıfı (Activity yaşam döngüsü + alarm/izin mantığı — az önceki dosyalarla aynı üslupta, klasik View kodu) ve altında bir grup `@Composable` fonksiyon (Jetpack Compose ile **bildirimsel** UI tanımı — bu, projede Compose kullanan **tek** ekran).

### Sınıf Alanları ve `launcher`

```kotlin
class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQ_NOTIFICATION = 101
    }

    private val medicineList = mutableStateListOf<Medicine>()
```
- `REQ_NOTIFICATION`: bildirim izni isteğinin sonucunu (`onRequestPermissionsResult`'ta) tanımak için kullanılan sabit bir kod.
- `mutableStateListOf<Medicine>()`: Compose'a özgü bir liste türü. Normal bir `ArrayList`'ten farkı: bu listenin içeriği **değiştiğinde** (eleman eklenince/silinince/değişince), bu listeyi okuyan **tüm Compose UI otomatik olarak yeniden çizilir** — Compose'un "state okunan yerde, state değişince otomatik yeniden çizim" felsefesinin temeli.

```kotlin
    private val launcher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            val data = result.data!!
            val isEditMode = data.getBooleanExtra("is_edit", false)
            val newMedicine = data.getSerializableExtra("new_medicine") as? Medicine

            if (newMedicine != null) {
                if (isEditMode) {
                    val editPosition = data.getIntExtra("edit_position", -1)
                    if (editPosition != -1) {
                        cancelAlarm(medicineList[editPosition])
                        medicineList[editPosition] = newMedicine
                        Toast.makeText(this, "İlaç güncellendi ✓", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    medicineList.add(newMedicine)
                    Toast.makeText(this, "İlaç eklendi ✓", Toast.LENGTH_SHORT).show()
                }
                scheduleAlarm(newMedicine)
                saveData()
            }
        }
    }
```
`AddMedicineActivity`'nin `setResult(RESULT_OK, resultIntent)` ile geri döndürdüğü sonucu burada yakalanıyor:
- Düzenleme modundaysa (`is_edit == true`): **eski** ilacın alarmları önce iptal edilir (`cancelAlarm`, çünkü saatler değişmiş olabilir), sonra listedeki o konumdaki (`editPosition`) ilaç yeni haliyle **değiştirilir**.
- Yeni eklemeyse: liste sonuna eklenir.
- Her iki durumda da: yeni/güncel ilaç için alarm **yeniden kurulur** (`scheduleAlarm`) ve tüm liste diske kaydedilir (`saveData`).
- `medicineList[editPosition] = newMedicine`: `mutableStateListOf` bir `MutableList` olduğu için `[]` ile eleman ataması çalışıyor (Kotlin operatör kısayolu, `.set(index, value)`'un yerine).

### `onCreate` / `onResume`

```kotlin
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        loadData()

        setContent {
            IlacTakipTheme {
                MainScreen(
                    medicines = medicineList,
                    onAddClick = {
                        launcher.launch(Intent(this, AddMedicineActivity::class.java))
                    },
                    onEditClick = { medicine, position ->
                        val intent = Intent(this, AddMedicineActivity::class.java)
                        intent.putExtra("edit_medicine", medicine)
                        intent.putExtra("edit_position", position)
                        launcher.launch(intent)
                    },
                    onActiveChanged = { medicine, isChecked ->
                        medicine.isActive = isChecked
                        if (!isChecked) {
                            cancelAlarm(medicine)
                            Toast.makeText(this, "Alarm pasif edildi", Toast.LENGTH_SHORT).show()
                        } else {
                            scheduleAlarm(medicine)
                            Toast.makeText(this, "Alarm aktif edildi", Toast.LENGTH_SHORT).show()
                        }
                        saveData()
                    },
                    onTakenChanged = { medicine, isChecked ->
                        medicine.isTaken = isChecked
                        saveData()
                    },
                    onDeleteConfirmed = { medicine ->
                        cancelAlarm(medicine)
                        medicineList.remove(medicine)
                        saveData()
                        Toast.makeText(this, "İlaç silindi", Toast.LENGTH_SHORT).show()
                    },
                )
            }
        }

        requestAllPermissions()
    }
```
- `enableEdgeToEdge()`: uygulamayı **kenardan kenara** (edge-to-edge) çizime alır — status bar ve gesture navigation bar şeffaf olur, uygulama arka planı onların altına kadar uzanır. Bu, native Material3 görünümünün temel taşı; sistem çubuklarının uygulama temasıyla uyumlu görünmesini sağlar (ikon kontrastı da aydınlık/karanlık moda göre otomatik ayarlanır). Sistem çubuğu boşlukları, aşağıdaki `TopAppBar`/`Scaffold` tarafından otomatik "inset" olarak eklenir.
- `loadData()`: `MedicineRepository`'den kayıtlı ilaçlar okunup `medicineList`'e doldurulur (aşağıda).
- `setContent { ... }`: Compose'un **giriş kapısı** — bir Activity'de klasik `setContentView(R.layout...)` yerine, burada tüm ekran **Kotlin kodu olarak** (bildirimsel şekilde) tanımlanıyor.
- `IlacTakipTheme { ... }`: Material3 renk/tema tanımını (bkz. `IlacTakipTheme.kt` bölümü) tüm alt UI ağacına uygulayan sarmalayıcı.
- `MainScreen(...)`: asıl ekranı çizen `@Composable` fonksiyon (aşağıda). Buraya **veri** (`medicines = medicineList`) ve **davranış** (`onAddClick`, `onEditClick` vb. — hepsi lambda/fonksiyon) parametre olarak geçiliyor. Bu, Compose'un yaygın tasarım deseni: Composable fonksiyonlar "aptal"dır (sadece verilen veriyi çizer), gerçek iş mantığı (Activity, izin, alarm, kayıt) dışarıda kalır.
  - `onAddClick`: yeni ilaç ekleme ekranını açar.
  - `onEditClick`: düzenleme ekranını, mevcut ilaç ve konumuyla açar.
  - `onActiveChanged`: kullanıcı bir ilacın switch'ini açıp/kapatınca — alarmı iptal eder ya da yeniden kurar.
  - `onTakenChanged`: kullanıcı "alındı" kutucuğunu işaretleyince/kaldırınca — sadece veriyi günceller (alarm mantığına dokunmaz).
  - `onDeleteConfirmed`: kullanıcı silmeyi onaylayınca — alarmı iptal edip listeden çıkarır.
- Her durumda sonda `saveData()` çağrılarak değişiklik kalıcı hâle getiriliyor.
- `requestAllPermissions()`: ekran çizildikten sonra izin akışı başlatılıyor (aşağıda).

```kotlin
    override fun onResume() {
        super.onResume()
        loadData()
    }
```
Kullanıcı `AddMedicineActivity`'den (veya başka bir ekrandan/uygulamadan) bu ekrana her **geri döndüğünde** (`onResume`), liste diskten yeniden okunur — bu, verinin her zaman güncel kalmasını sağlayan basit ama etkili bir yöntem (yukarıdaki `launcher` zaten anlık günceller, ama `onResume` ekstra bir güvenlik/tazeleme katmanı).

### İzin Yönetimi (üç aşamalı zincir)

```kotlin
    private fun requestAllPermissions() {
        requestNotificationPermission()
    }
```
Zincirleme izin isteme akışının başlangıç noktası — her izin adımı, kendi sonucuna göre **bir sonrakini** çağırıyor (aşağıda göreceğiz), böylece kullanıcı üç ayrı izni sırayla, tek tek onaylıyor.

```kotlin
    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)) {
                    AlertDialog.Builder(this)
                        .setTitle("Bildirim İzni Gerekli")
                        .setMessage(
                            "İlaç hatırlatıcıların çalışabilmesi için bildirim izni gereklidir. " +
                                "Bu izin olmadan ilaç saatlerinizde uyarı alamazsınız."
                        )
                        .setPositiveButton("İzin Ver") { _, _ ->
                            ActivityCompat.requestPermissions(
                                this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATION
                            )
                        }
                        .setNegativeButton("Daha Sonra") { _, _ -> checkAlarmPermission() }
                        .setCancelable(false)
                        .show()
                } else {
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_NOTIFICATION
                    )
                }
            } else {
                checkAlarmPermission()
            }
        } else {
            checkAlarmPermission()
        }
    }
```
**1. adım — Bildirim izni** (sadece Android 13+/`TIRAMISU`'da gerekli, daha eski sürümlerde bu izin kavramı bile yok, direkt `checkAlarmPermission()`'a atlanır):
- İzin zaten verilmişse doğrudan bir sonraki adıma geç.
- Verilmemişse: `shouldShowRequestPermissionRationale` → kullanıcı **daha önce bir kere reddetmişse** `true` döner — bu durumda önce **neden gerekli olduğunu açıklayan** bir dialog gösterilir (kullanıcıyı ikna etmek için), "İzin Ver"e basarsa gerçek sistem izin isteği (`requestPermissions`) tetiklenir, "Daha Sonra"ya basarsa izin istenmeden bir sonraki adıma geçilir.
- Kullanıcı **hiç sorulmamışsa** (rationale gösterilmesi gerekmiyorsa) direkt sistem izin isteği açılır — gereksiz bir açıklama dialogu ile kullanıcıyı yormamak için.

```kotlin
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQ_NOTIFICATION) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                checkAlarmPermission()
            } else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.POST_NOTIFICATIONS)
                ) {
                    AlertDialog.Builder(this)
                        .setTitle("Bildirim İzni Kapalı")
                        .setMessage(
                            "İlaç hatırlatıcılar bildirim gönderemez. " +
                                "Lütfen uygulama ayarlarından bildirim iznini açın."
                        )
                        .setPositiveButton("Ayarlara Git") { _, _ -> openAppSettings() }
                        .setNegativeButton("Kapat", null)
                        .show()
                } else {
                    Toast.makeText(this, "⚠ Bildirim izni olmadan hatırlatıcılar çalışmaz", Toast.LENGTH_LONG).show()
                }
                checkAlarmPermission()
            }
        }
    }
```
Sistemin izin isteği penceresi kapandığında (kullanıcı "İzin Ver"/"Reddet" seçtiğinde) bu geri çağrı tetiklenir:
- Verildiyse → sıradaki izne geç.
- Reddedildiyse: `!shouldShowRequestPermissionRationale(...)` **artık** `true` ise (yani rationale gösterilmiyor **VE** izin yok), bu genelde kullanıcının **"Bir daha sorma"** seçeneğini işaretleyerek reddettiği anlamına gelir — bu durumda kullanıcıyı doğrudan **uygulama ayarları**na yönlendiren bir dialog gösterilir (çünkü artık uygulama içinden tekrar izin istenemez). Aksi hâlde basit bir Toast uyarısı yeterli görülüyor.
- Her koşulda (izin verilse de verilmese de) zincir **devam eder** — `checkAlarmPermission()` çağrılır. Yani kullanıcı bildirim iznini reddetse bile, uygulama diğer izinleri sormaya devam eder.

```kotlin
    private fun checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                AlertDialog.Builder(this)
                    .setTitle("Alarm İzni Gerekli")
                    .setMessage(
                        "İlaç saatlerinizde kesin alarm kurabilmek için bu izin gereklidir. " +
                            "İzin vermezseniz alarmlar birkaç dakika gecikebilir."
                    )
                    .setPositiveButton("İzin Ver") { _, _ ->
                        startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
                    }
                    .setNegativeButton("Daha Sonra") { _, _ -> checkFullScreenIntentPermission() }
                    .setCancelable(false)
                    .show()
                return
            }
        }
        checkFullScreenIntentPermission()
    }
```
**2. adım — Kesin alarm izni** (Android 12+/`S`). Bu izin, normal `requestPermissions` API'siyle değil, kullanıcıyı **sistem ayarları ekranına** yönlendirerek (`Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM`) alınır — Android bu özel izin türü için ayrı bir akış tanımlamış. İzin zaten varsa veya bu Android sürümünde gerekmiyorsa doğrudan 3. adıma geçilir.

```kotlin
    private fun checkFullScreenIntentPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            if (nm != null && !nm.canUseFullScreenIntent()) {
                AlertDialog.Builder(this)
                    .setTitle("Kilit Ekranı İzni")
                    .setMessage(
                        "İlaç alarmının kilit ekranında tam ekran görünmesi için bu izin gereklidir. " +
                            "İzin vermezseniz alarm sadece bildirim olarak görünür."
                    )
                    .setPositiveButton("İzin Ver") { _, _ ->
                        startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                Uri.parse("package:$packageName"),
                            )
                        )
                    }
                    .setNegativeButton("Daha Sonra", null)
                    .setCancelable(false)
                    .show()
            }
        }
    }
```
**3. adım — Tam ekran bildirim izni** (Android 14+/`UPSIDE_DOWN_CAKE`). Bu en yeni Android sürümlerinde eklenen bir kısıtlama: `setFullScreenIntent(...)` kullanan bildirimlerin gerçekten tam ekran açılabilmesi için ayrı bir izin gerekiyor — yoksa bildirim sadece normal bir bildirim olarak görünür, `AlarmActivity` **otomatik açılmaz**. Bu da yine bir sistem ayarları ekranına yönlendirilerek çözülüyor. Bu zincirin **sonu** — "Daha Sonra"ya basılsa bile bir sonraki adım yok, zincir burada biter.

```kotlin
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.data = Uri.parse("package:$packageName")
        startActivity(intent)
    }
```
Uygulamanın sistem ayarları sayfasını (izinlerin elle değiştirilebildiği ekran) açar — bildirim izni "bir daha sorma" ile reddedildiyse kullanıcıyı buraya yönlendirmek gerekiyordu.

### İlaç Yönetimi Yardımcıları

```kotlin
    private fun cancelAlarm(medicine: Medicine) {
        AlarmHelper.cancelAlarm(this, medicine)
    }

    private fun saveData() {
        MedicineRepository.saveMedicineList(this, medicineList)
    }

    private fun loadData() {
        medicineList.clear()
        medicineList.addAll(MedicineRepository.loadMedicineList(this))
        medicineList.sortBy { it.time ?: "" }
    }
```
Üç küçük yardımcı: `AlarmHelper`/`MedicineRepository`'ye ince birer sarmalayıcı (wrapper). `loadData()`: önce liste **temizlenir**, sonra diskten okunan tüm ilaçlar eklenir, sonunda **saate göre sıralanır** (`sortBy { it.time ?: "" }` — `time` `null` ise boş string kullanılır ki sıralama çökmesin, boş string alfabetik olarak en başa gelir).

```kotlin
    private fun scheduleAlarm(medicine: Medicine) {
        if (medicine.isUseCustomDays && medicine.customDayTimes != null) {
            scheduleCustomDayAlarm(medicine)
        } else {
            scheduleStandardAlarm(medicine)
        }
    }
```
Moda göre doğru kurulum fonksiyonuna yönlendirir. (`scheduleStandardAlarm`, `scheduleCustomDayAlarm`, `setExactAlarm` fonksiyonlarının **tamamı**, `BootReceiver.kt` bölümünde açıklanan mantığın **birebir aynısı** — tek fark, oradaki bir ilaç listesinin tamamı için, burada tek bir ilaç için çalışması. Kod tekrarını önlemek adına burada yeniden açıklamıyoruz; `BootReceiver.kt` bölümüne bakın.)

### Compose UI — `MainScreen`

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    medicines: List<Medicine>,
    onAddClick: () -> Unit,
    onEditClick: (Medicine, Int) -> Unit,
    onActiveChanged: (Medicine, Boolean) -> Unit,
    onTakenChanged: (Medicine, Boolean) -> Unit,
    onDeleteConfirmed: (Medicine) -> Unit,
) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                title = { Text("İlaç Takip") },
                scrollBehavior = scrollBehavior,
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddClick) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = "Yeni İlaç Ekle")
            }
        },
    ) { innerPadding ->
        ...
    }
}
```
- `@Composable`: bu fonksiyonun bir Compose UI parçası tanımladığını belirtir. `@OptIn(ExperimentalMaterial3Api::class)`: `TopAppBar` ve scroll-behavior API'leri Material3'te hâlâ "deneysel" işaretli (kararsız oldukları için değil, imza yüzeyi henüz kesinleşmediği için) — açık onay gerektiriyor.
- Parametreler: **veri** (`medicines`) + **olay işleyiciler** (lambda'lar). Bu fonksiyon hiçbir iş mantığına dokunmuyor, sadece verilen lambda'ları uygun anda çağırıyor — gerçek iş `MainActivity`'de.
- **`TopAppBar` (native M3 üst çubuk):** ekranın en üstünde, status bar'ın hemen altında, "İlaç Takip" başlığını gösteren kompakt Material3 üst çubuğu. Eskiden bu, özel bir gradyan banner (`Header()` composable'ı) idi; native M3 hissi için standart `TopAppBar`'a çevrildi.
- **`scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()`:** çubuk tepede **sabit** kalır ama içerik altından kayınca M3'ün ince "yükseltilmiş" renk tonunu alır. Bunun çalışması için Scaffold'a `Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)` bağlanıyor (kaydırma olaylarını çubuğa ileten köprü).
- **`FloatingActionButton`:** sağ-alt köşedeki yüzen ekle butonu. Özel renk verilmiyor — M3'ün varsayılanı (`primaryContainer`) kullanılıyor, native görünüm için.
- `{ innerPadding -> ... }`: `Scaffold`'ın içeriği, üst çubuk + (edge-to-edge sayesinde) sistem çubuğu boşluklarını içeren otomatik hesaplı bir `padding` değeri alıyor.

```kotlin
    ) { innerPadding ->
        if (medicines.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = innerPadding.calculateTopPadding() + 8.dp,
                    bottom = innerPadding.calculateBottomPadding() + 96.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                itemsIndexed(medicines) { index, medicine ->
                    MedicineCard(
                        medicine = medicine,
                        onClick = { onEditClick(medicine, index) },
                        onActiveChanged = { onActiveChanged(medicine, it) },
                        onTakenChanged = { onTakenChanged(medicine, it) },
                        onDelete = { onDeleteConfirmed(medicine) },
                    )
                }
            }
        }
    }
```
- Liste boşsa `EmptyState` (aşağıda), doluysa `LazyColumn` (Compose'un **performanslı** kaydırılabilir liste bileşeni — View sistemindeki `RecyclerView`'ın karşılığı, sadece ekranda görünen elemanları render eder).
- `contentPadding`: Scaffold'ın verdiği `innerPadding` doğrudan kullanılmıyor, parçalanıyor — `calculateTopPadding()` (üst çubuk yüksekliği + status bar) üste, `calculateBottomPadding()` (gesture nav bar) + 96dp (FAB'ın altında kalmasın diye) alta, yanlara 16dp. Yani liste tüm ekranı kaplıyor ama içeriği bu boşlukların içinde kalıyor — kaydırınca içerik çubuğun/sistem çubuklarının **altından** akıyor (edge-to-edge görünümün gereği).
- `itemsIndexed(medicines) { index, medicine -> ... }`: her ilaç için hem elemanı hem **konumunu** (index — düzenleme sonucunu doğru konuma yazmak için gerekli) vererek bir `MedicineCard` çizer.

> **Not:** Bu ekranda eskiden ayrı bir `Header()` composable'ı (primary→secondary dikey gradyanlı özel banner) vardı. Native Material3 görünümü için o kaldırılıp yukarıdaki `TopAppBar` ile değiştirildi; başlığın altındaki "Sağlığınız bizim önceliğimiz" alt yazısı da bu geçişte çıkarıldı (kompakt bir M3 üst çubuğunda doğal bir alt-başlık yeri yok).

```kotlin
@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("💊", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Henüz ilaç eklenmedi",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Aşağıdaki + butonuna basarak\nilk ilacınızı ekleyin",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 19.5.sp,
        )
    }
}
```
Liste boşken gösterilen "boş durum" ekranı — büyük bir emoji + iki açıklayıcı metin, ortalanmış. `modifier: Modifier = Modifier` parametresi: Compose'un yaygın kalıbı, çağıran tarafın (`MainScreen`) bu bileşene ek stil (burada `.weight(1f)`) **dışarıdan** enjekte edebilmesini sağlıyor.

### `MedicineCard` — Listedeki Her Bir İlaç Satırı

```kotlin
@Composable
private fun MedicineCard(
    medicine: Medicine,
    onClick: () -> Unit,
    onActiveChanged: (Boolean) -> Unit,
    onTakenChanged: (Boolean) -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
```
- `var showDeleteDialog by remember { mutableStateOf(false) }`: Compose'un **yerel state** tanımlama kalıbı. `mutableStateOf(false)` bir "gözlemlenebilir kutu" oluşturur; `by` (Kotlin property delegation) sayesinde `showDeleteDialog` sanki düz bir `Boolean` değişkenmiş gibi okunup yazılabiliyor, ama arkada her değişiklik Compose'a "bu ekranı yeniden çiz" sinyali gönderiyor. `remember`, bu state'in ekran her yeniden çizildiğinde **sıfırlanmamasını**, aynı değeri korumasını sağlıyor. Bu, "silme onayı dialogu şu an açık mı" bilgisini tutuyor — tamamen bu kartın kendi iç (geçici, UI'a özel) durumu, `Medicine` verisiyle ilgisi yok.

```kotlin
    val isExpired = remember(medicine.endDate) {
        val endDate = medicine.endDate
        if (endDate == 0L) {
            false
        } else {
            val endCal = Calendar.getInstance()
            endCal.timeInMillis = endDate
            endCal.set(Calendar.HOUR_OF_DAY, 23)
            endCal.set(Calendar.MINUTE, 59)
            System.currentTimeMillis() > endCal.timeInMillis
        }
    }
```
`remember(medicine.endDate) { ... }`: bu hesaplamanın sonucu **hatırlanır**, ama sadece `medicine.endDate` **değişmediği sürece** — değişirse yeniden hesaplanır (gereksiz tekrar hesaplamayı önlemek için Compose'un "anahtarlı önbellek" mekanizması). Mantığın kendisi: bitiş tarihi `0` ise hiç süresi yok demek (asla "geçmiş" sayılmaz); varsa, o günün **sonuyla** (23:59) şu anki zaman karşılaştırılır.

```kotlin
    val statusText: String
    val statusColor: Color
    var indicatorColor: Color

    when {
        isExpired -> {
            statusText = "Tarihi Geçti"
            statusColor = colorResource(R.color.indicator_expired)
            indicatorColor = colorResource(R.color.indicator_expired)
        }
        medicine.isTaken -> {
            statusText = "İlacını Aldın ✓"
            statusColor = colorResource(R.color.status_taken)
            indicatorColor = colorResource(R.color.indicator_taken)
        }
        else -> {
            statusText = "İlacını Almadın"
            statusColor = colorResource(R.color.status_missed)
            indicatorColor = colorResource(R.color.indicator_missed)
        }
    }
    if (!medicine.isActive && !isExpired) {
        indicatorColor = colorResource(R.color.indicator_expired)
    }
```
`when { ... }` (parametresiz, yani her dalın kendi koşulunu yazdığı Kotlin `when` biçimi — Java'daki `if/else if` zincirinin daha okunaklı hâli): karta gösterilecek durum metni ve rengi, önceliğe göre belirleniyor — önce süresi geçmiş mi, sonra alınmış mı, yoksa "almadın" mı. Ek olarak: ilaç **pasifse** (kullanıcı switch'i kapatmışsa) ve süresi de geçmemişse, sol kenar şeridinin rengi yine "geçmiş/pasif" rengine çekiliyor (görsel olarak "bu ilaç şu an aktif değil" izlenimi vermek için).

```kotlin
    val customDayTimes = medicine.customDayTimes
    val timeText = if (medicine.isUseCustomDays && customDayTimes != null) {
        formatCustomDayTimes(customDayTimes)
    } else {
        medicine.time ?: ""
    }
    val dateRange = medicine.dateRange
    val note = medicine.note
```
Gösterilecek saat metni hesaplanıyor: güne özelse `formatCustomDayTimes` (dosyanın en altında) ile `"Pzt 09:00, Çar 14:00"` gibi kısa bir özet üretilir; değilse ilacın ham `time` alanı (`"08:00, 20:00"`) doğrudan gösterilir.

```kotlin
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        // containerColor elle verilmiyor: M3'ün varsayılanı (surfaceContainerLow) arka
        // plandan otomatik ayrışan bir ton veriyor.
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(indicatorColor),
            )
```
`Card`: Material Design'ın gölgeli, yuvarlak köşeli kutu bileşeni; tıklanabilir (`onClick = onClick` → karta her yerine dokunmak, düzenleme ekranını açar). İçinde bir `Row` (yatay yerleşim) var; ilk eleman, 4dp genişliğinde, kartın tam yüksekliğinde, durum rengiyle boyanmış ince bir **şerit** (`Box`) — solda görünen renkli çizgi budur. **Önemli ince nokta:** kartın `containerColor`'ı **elle verilmiyor**. Erken bir sürümde `MaterialTheme.colorScheme.surface`'e sabitlenmişti, ama koyu modda `surface` ile arka plan (`background`) neredeyse aynı renk olduğu için kartlar arka plana karışıp görünmez oluyordu. Elle rengi kaldırınca M3'ün gerçek varsayılanı (`surfaceContainerLow` — arka plandan bir kademe ayrışan tonal renk) devreye girip kartlara "yükseltilmiş" görünümünü otomatik veriyor. Aynı düzeltme `AddMedicineActivity`'deki `FormCard`'da da uygulandı.

```kotlin
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = statusText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = statusColor,
                    )
                    Switch(
                        checked = medicine.isActive,
                        onCheckedChange = onActiveChanged,
                        enabled = !isExpired,
                    )
                }
```
Şeridin yanında, kalan tüm genişliği kaplayan (`weight(1f)`) bir `Column`. İçindeki ilk satır: solda durum metni, sağda aktif/pasif `Switch` — `Arrangement.SpaceBetween` ikisini **en uçlara** iter (aralarındaki boşluğu eşit dağıtarak). `Switch`'in `checked`/`onCheckedChange` parametreleri, Compose'un **tek yönlü veri akışı** kalıbının klasik örneği: switch **kendi** state'ini tutmuyor, dışarıdan verilen `medicine.isActive`'i gösteriyor, kullanıcı dokununca da dışarıdaki `onActiveChanged` (yani `MainActivity`'deki gerçek güncelleme mantığı) çağrılıyor — "kaynak of truth" hep `Medicine` nesnesinde. `enabled = !isExpired`: süresi geçmiş ilaçlarda switch devre dışı (zaten anlamsız).

```kotlin
                Spacer(Modifier.height(4.dp))

                Text(
                    text = medicine.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
```
İlacın adı — `maxLines = 1` + `TextOverflow.Ellipsis`: çok uzun bir isim tek satıra sığmazsa sonunu `"..."` ile keser (satır taşıp kartın düzenini bozmasın diye).

```kotlin
                Row(
                    modifier = Modifier.padding(top = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = timeText,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (!medicine.isUseCustomDays && !dateRange.isNullOrEmpty()) {
                        Spacer(Modifier.width(12.dp))
                        Text("📅 ", fontSize = 12.sp)
                        Text(
                            text = dateRange,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
```
Saat bilgisi her zaman gösterilir; standart moddaysa **ve** bir tarih aralığı belirtilmişse, yanına bir takvim emojisiyle tarih aralığı da eklenir (güne özel modda tarih aralığı kavramı olmadığı için orada hiç gösterilmiyor).

```kotlin
                if (!note.isNullOrEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("📝 ", fontSize = 12.sp)
                        Text(
                            text = note,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
```
Not varsa, bir not emojisiyle birlikte gösterilir. Yoksa bu satır hiç çizilmez (koşullu Composable çağrısı — Compose'da `if` bloğu içine bir Composable koymak, "bu koşul doğruysa bu UI'ı ekle" demektir).

```kotlin
            Column(
                modifier = Modifier.padding(end = 12.dp, top = 14.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Checkbox(
                    checked = medicine.isTaken,
                    onCheckedChange = { if (!isExpired) onTakenChanged(it) },
                    enabled = !isExpired,
                    colors = CheckboxDefaults.colors(checkedColor = MaterialTheme.colorScheme.primary),
                )
                Spacer(Modifier.height(8.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_delete),
                    contentDescription = "Sil",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { showDeleteDialog = true },
                )
            }
        }
    }
```
Kartın **sağ** tarafında, dikey olarak: "alındı" kutucuğu (yine `enabled = !isExpired` ile süresi geçmişse devre dışı) ve altında bir **çöp kutusu ikonu**. İkon aslında bir buton değil (`Icon` bileşeni tıklanabilir değildir), ama `Modifier.clickable { ... }` ile **herhangi bir Composable'a** tıklama davranışı eklenebiliyor — burada tıklanınca doğrudan silme işlemi yapılmıyor, sadece `showDeleteDialog = true` yapılarak bir **onay dialogu** açılıyor (yanlışlıkla silmeyi önlemek için).

```kotlin
    if (showDeleteDialog) {
        ComposeAlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("İlacı Sil") },
            text = { Text("${medicine.name} ilacını silmek istediğinize emin misiniz?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteDialog = false
                    onDelete()
                }) { Text("Sil") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("İptal") }
            },
        )
    }
}
```
`showDeleteDialog` `true` olduğunda (kullanıcı çöp kutusuna bastığında), Compose Material3'ün kendi `AlertDialog`'u gösterilir — dosyanın en üstünde `import androidx.compose.material3.AlertDialog as ComposeAlertDialog` ile **takma isim** (`as ComposeAlertDialog`) verildiğine dikkat edin: çünkü bu dosyada `androidx.appcompat.app.AlertDialog` (klasik View sisteminin dialogu, izin isteme akışında kullanılıyor) **da** kullanılıyor — aynı isimde iki farklı sınıf çakışmasın diye biri yeniden adlandırılmış. "Sil"e basılırsa dialog kapanır ve gerçek silme (`onDelete()`) tetiklenir; "İptal"e basılırsa sadece dialog kapanır, hiçbir şey silinmez.

```kotlin
/** HashMap<calDay, times> → "Pzt 09:00, Sal 10:30" formatına dönüştür */
private fun formatCustomDayTimes(dayTimes: Map<Int, String>): String {
    val shortNames = arrayOf("", "Paz", "Pzt", "Sal", "Çar", "Per", "Cum", "Cmt")
    val ordered = intArrayOf(2, 3, 4, 5, 6, 7, 1) // Pzt-Paz sırası

    val sb = StringBuilder()
    for (day in ordered) {
        val times = dayTimes[day] ?: continue
        val firstTime = if (times.contains(",")) times.split(",")[0].trim() else times
        if (sb.isNotEmpty()) sb.append(", ")
        sb.append(shortNames[day]).append(" ").append(firstTime)
    }
    return sb.toString()
}
```
Bu **normal** (Composable olmayan) bir yardımcı fonksiyon — `AddMedicineActivity`'deki gün-sıralama mantığına benzer şekilde, günleri Pazartesi'den başlatarak sıralar (`ordered` dizisi), her günün **sadece ilk** saatini (o günde birden fazla doz olsa bile, kart üzerinde yer kaplamasın diye) kısa gün adıyla (`Pzt`, `Sal` vb.) birleştirip tek bir özet string üretir. Örnek çıktı: `"Pzt 09:00, Çar 14:00"`.

---

## IlacTakipTheme.kt

Compose tarafının renk temasını tanımlar. Artık projedeki tüm ekranlar Compose olduğu için, üç Compose ekranının (`MainActivity`, `AddMedicineActivity`, `AlarmActivity`) hepsi bu tema fonksiyonunu (`IlacTakipTheme { ... }`) sarmalayıcı olarak kullanıyor.

```kotlin
package com.YucelDigital.ilactakip.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.YucelDigital.ilactakip.R

@Composable
fun IlacTakipTheme(
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
```
- `dynamicColor: Boolean = true`: **varsayılan değerli** bir parametre — çağıran taraf (`MainActivity`) bu parametreyi hiç belirtmeden `IlacTakipTheme { ... }` diye çağırdığında otomatik `true` alınır (yani dinamik renk varsayılan olarak açık).
- `content: @Composable () -> Unit`: bu fonksiyonun **son parametresi**, "içerik olarak hangi Composable'lar çizilecek" — Kotlin'in "trailing lambda" sözdizimi sayesinde çağırırken `IlacTakipTheme { MainScreen(...) }` gibi süslü parantezle doğrudan yazılabiliyor (`content = { ... }` yazmaya gerek yok).
- `isSystemInDarkTheme()`: cihazın şu an **karanlık mod**da olup olmadığını okuyan Compose fonksiyonu — sistem ayarı değişirse bu otomatik güncellenir ve UI yeniden çizilir.
- `LocalContext.current`: Compose'da bir `Context`'e ihtiyaç duyulduğunda (burada dinamik renk paleti hesaplamak için) kullanılan standart yol.

```kotlin
    val colorScheme = if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else if (darkTheme) {
        darkColorScheme(
            primary = colorResource(R.color.primary),
            onPrimary = colorResource(R.color.text_on_primary),
            secondary = colorResource(R.color.secondary),
            onSecondary = colorResource(R.color.black),
            background = colorResource(R.color.app_background),
            onBackground = colorResource(R.color.text_primary),
            surface = colorResource(R.color.surface_color),
            onSurface = colorResource(R.color.text_primary),
            surfaceVariant = colorResource(R.color.card_background),
            onSurfaceVariant = colorResource(R.color.text_secondary),
            error = colorResource(R.color.error),
            onError = colorResource(R.color.white),
        )
    } else {
        lightColorScheme(
            primary = colorResource(R.color.primary),
            onPrimary = colorResource(R.color.text_on_primary),
            secondary = colorResource(R.color.secondary),
            onSecondary = colorResource(R.color.black),
            background = colorResource(R.color.app_background),
            onBackground = colorResource(R.color.text_primary),
            surface = colorResource(R.color.surface_color),
            onSurface = colorResource(R.color.text_primary),
            surfaceVariant = colorResource(R.color.card_background),
            onSurfaceVariant = colorResource(R.color.text_secondary),
            error = colorResource(R.color.error),
            onError = colorResource(R.color.white),
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
```
Üç yollu bir karar ağacı:
1. **Dinamik renk açık VE Android 12+ (`S`)**: `dynamicLightColorScheme`/`dynamicDarkColorScheme` — Android'in kendi sistem API'sinden, kullanıcının duvar kağıdından türetilmiş **tam** bir Material3 renk paleti alınır (Material You).
2. **Dinamik renk kapalı ya da eski Android + karanlık mod**: elle tanımlanmış sabit koyu renk paleti (`res/values-night/colors.xml`'den okunan renkler).
3. **Dinamik renk kapalı ya da eski Android + aydınlık mod**: elle tanımlanmış sabit açık renk paleti (`res/values/colors.xml`'den).
- `colorResource(R.color.primary)` gibi çağrılar, gece/gündüz renk dosyalarının **hangisinin** kullanılacağına Android'in kendi kaynak seçim sistemi (`values/` vs `values-night/`) karar veriyor — kod bunu bilmek zorunda değil, sadece "primary rengi getir" diyor.
- `primary`/`onPrimary` gibi isimler, Material Design'ın **rol tabanlı** renk sistemi: `primary` = ana marka rengi, `onPrimary` = o rengin **üzerine** yazılacak metin/ikon rengi (kontrast garantili), `surface`/`onSurface` = kart/yüzey rengi ve üzerindeki metin, vb.
- En sonda `MaterialTheme(colorScheme = colorScheme, content = content)`: hesaplanan renk paleti, Compose'un tema sağlayıcısına verilir — bundan sonra `content` içindeki **her** Composable, `MaterialTheme.colorScheme.primary` gibi çağrılarla bu paletten renk okuyabilir (bkz. `MainActivity.kt`'deki `Header`, `MedicineCard` vb.).

---

## Test Dosyaları

`app/src/test/java/com/YucelDigital/ilactakip/` altında, tamamı **JUnit 4** ile yazılmış 6 test sınıfı var. Bunlar bir cihaz/emülatör gerektirmeden JVM üzerinde çalışıyor (bazıları **Robolectric** sayesinde Android sınıflarını simüle ediyor). Her satırını tek tek açıklamak yerine (çoğu tekrarlayan `assertEquals` çağrısı), her dosyanın **neyi ve neden** test ettiğini özetliyoruz.

### `MedicineTest.kt`
`Medicine` sınıfının kendisini test eder: constructor'ın varsayılan değerleri doğru atadığını (`isActive = true`, `isTaken = false`, `intervalDays = 1` vb.), her `var` alanının get/set ile doğru çalıştığını, `customDayTimes` haritasının doğru tutulduğunu, ve **Serializable round-trip**'i (bir `Medicine`'i baytlara çevirip geri Kotlin nesnesine çevirdiğinde tüm alanların korunduğunu — bu, `Intent.putExtra`/`getSerializableExtra` ile ekranlar arası taşımanın gerçekten çalıştığının kanıtı). Ayrıca `AlarmHelper.safeId`'nin dayandığı ham `hashCode()` aritmetiğinin (isim+saat kombinasyonlarının farklı/aynı kimlik üretmesi) tutarlılığını da burada test ediyor.

### `FrequencyCalculationTest.kt`
Android'e **hiç bağımlı olmayan saf mantığı** test eder — `AddMedicineActivity`'deki saat-genişletme (`"8 saatte bir" → "08:00, 16:00, 00:00"`), güne-özel saat genişletme, frekans-metni çözümleme (`resolveFrequencyText`), süre-geçmişliği kontrolü (`isExpired` mantığı) ve interval-günü kontrolünün (`AlarmReceiver`'daki "bugün bu ilacın sırası mı" hesabı) **aynı algoritmaların yerel kopyalarını** çalıştırıp doğru sonuç verdiğini doğrular. "Yerel kopya" olmasının sebebi: bu mantıklar `private` fonksiyonların içinde gömülü olduğu için doğrudan çağrılamıyor, bu yüzden testler aynı algoritmayı burada tekrar yazıp doğruluğunu kanıtlıyor.

### `AlarmAdapterFormatTest.kt`
`MainActivity.kt`'deki `formatCustomDayTimes` fonksiyonunun (güne özel saatleri `"Pzt 09:00, Çar 14:00"` gibi kısa bir özete çeviren mantık) doğru gün sıralaması, doğru kısaltmalar, ve "günde birden fazla saat varsa sadece ilkini göster" davranışını test eder. (Dosya adı tarihi bir sebepten `AlarmAdapter` diyor — proje Java'dan Kotlin'e/Compose'a geçmeden önce bu mantık `AlarmAdapter` adlı ayrı bir sınıftaydı; Compose'a geçişte o sınıf kaldırıldı ve mantığı `MainActivity.kt` içine taşındı, ama test dosyasının adı değişmedi.)

### `AlarmHelperTest.kt`
`AlarmHelper.safeId()`'yi (negatif olmayan alarm kimliği üretimi) doğrudan test eder. En dikkat çekici testi: `"polygenelubricants".hashCode()` değerinin **tam olarak** `Int.MIN_VALUE` olduğunu (bilinen, gerçek bir Java/Kotlin köşe durumu) doğrulayıp, `safeId`'nin bu uç durumda bile pozitif bir sonuç ürettiğini kanıtlıyor — yani `Math.abs()` kullanılsaydı çökecek/yanlış sonuç verecek tam olay burada test ediliyor.

### `AlarmReceiverHandleMainAlarmTest.kt` (Robolectric)
`AlarmReceiver.handleMainAlarm()`'ı **gerçekten çağırarak** (Robolectric'in simüle Android ortamında) test eder: bir bildirim gerçekten postalandı mı, bildirimin **kendi sesi hiç yok mu** (çünkü ses kaynağı yalnızca `AlarmActivity` olmalı — bkz. `AlarmReceiver.kt` bölümündeki uzun yorum), ve zaten "alındı" işaretli bir ilaç için **tekrar** bildirim gösterilmediğini. `@Config(sdk = [35])`: Robolectric kütüphanesinin desteklediği en yüksek Android sürümü 35 olduğu için (proje `targetSdk 36` olsa da), testler bilinçli olarak 35 üzerinde simüle ediliyor.

### `MedicineRepositoryRobolectricTest.kt` (Robolectric)
`MedicineRepository.markAsTaken()`/`resetTakenStatus()`'u gerçek bir `SharedPreferences` (Robolectric'in simülasyonu) üzerinden uçtan uca test eder. En önemli testi (`markAsTaken_customDay_differentTimeThanStoredDisplayTime_stillMarksTaken`): güne özel bir ilaçta, `Medicine.time` alanında saklanan (rastgele/gösterim amaçlı) saatten **farklı** bir saatte alarm tetiklense bile, isimle eşleştirme sayesinde ilacın doğru şekilde "alındı" işaretlendiğini kanıtlıyor — bu, projenin ilk incelemesinde bulunup düzeltilen ana hatanın **kalıcı regresyon testi**.

> Testleri çalıştırmak için: `./gradlew testDebugUnitTest` (proje kökünden). Sonuçlar `app/build/test-results/testDebugUnitTest/` altında XML olarak, `app/build/reports/tests/testDebugUnitTest/index.html` altında ise okunabilir bir HTML rapor olarak oluşur.

---

## Kaynak (XML) Dosyaları — Özet

Bu dosyalar **kod** değil, Android'in **bildirimsel kaynak** sistemi — bu yüzden satır satır değil, ne işe yaradıkları özetlenerek anlatılıyor.

> **Not:** `res/layout/` klasörü artık boş/yok — `activity_add_medicine.xml` ve `activity_alarm.xml`, ait oldukları ekranlar Compose'a geçince silindi. Projede hiç XML layout kalmadı.

| Dosya | Ne işe yarar |
|---|---|
| `res/values/strings.xml` | Sabit metinler (şu an sadece uygulama adı gibi birkaç temel string). |
| `res/values/colors.xml` / `res/values-night/colors.xml` | Aydınlık/karanlık mod için sabit renk paleti (`primary`, `surface_color`, `status_taken` vb.) — `IlacTakipTheme.kt`'nin dinamik renk kapalıyken okuduğu kaynak, hem de `Theme.IlacTakip` XML temasının okuduğu kaynak. Android, hangisinin kullanılacağına cihazın o anki tema moduna göre otomatik karar verir. |
| `res/values/themes.xml` / `res/values-night/themes.xml` | `Theme.IlacTakip` — artık sadece **launch/pencere teması** olarak kullanılıyor (Compose çizime başlamadan önceki ilk kare için pencere arka planı); `Theme.Material3.DayNight.NoActionBar`'dan türüyor. Sistem çubukları (`android:statusBarColor` + `android:navigationBarColor`) **şeffaf** ayarlı — böylece hem çalışma zamanındaki `enableEdgeToEdge()` ile uyumlu, hem de uygulama açılırken kısa launch anında beyaz/renkli bir çubuk flaşı görünmüyor. Eskiden XML Button'lara stil veren `Widget.IlacTakip.Button`/`.Outlined` stilleri, hiç XML View kalmadığı için kaldırıldı. |
| `res/drawable/*.xml` | Vektör ikonlar (`ic_add`, `ic_delete`, `ic_medicine`, `ic_calendar`, `ic_note` vb.) — Compose ekranları bunları `painterResource(R.drawable.*)` ile okuyor. Eskiden XML layout'ların kullandığı `bg_gradient_header`/`bg_circle_pulse`/`bg_rounded_primary`/`bg_indicator_strip` şekil drawable'ları, karşılıkları Compose'da (`Brush.verticalGradient`, `Box(CircleShape)` vb.) doğrudan koda taşındığı için silindi. |
| `res/mipmap-anydpi-v26/ic_launcher*.xml` | Uygulama ikonunun adaptif (farklı cihaz temalarına uyan) tanımları. |
| `res/xml/backup_rules.xml`, `data_extraction_rules.xml` | Android'in otomatik yedekleme/veri çıkarma sistemine, `SharedPreferences` verisinin nasıl (veya yedeklenip yedeklenmeyeceği) yönetileceğini söyleyen kurallar — `AndroidManifest.xml`'deki `android:fullBackupContent`/`android:dataExtractionRules` bu dosyalara işaret ediyor. |

---

## Kapanış

Bu belge, projedeki **her Kotlin dosyasını** satır/blok bazında; yapılandırma ve kaynak dosyalarını ise özet düzeyde kapsıyor. Kod okurken en sık karşınıza çıkacak üç tekrarlayan kalıp:

1. **`val x = nesne.mutableAlan; if (x != null) { ... x ... }`** — Kotlin, bir sınıfın `var` alanını `null` kontrolünden sonra bile otomatik "null değil" tipine geçiremediği için, önce yerel bir `val`'e alınıyor (`AlarmHelper`, `BootReceiver`, `AddMedicineActivity`, `MainActivity`'de defalarca görülür).
2. **`@JvmStatic` + `companion object`** — Kotlin `object`/`companion object` üyelerinin Java'dan (ve bazen daha temiz Kotlin çağrısı için) doğrudan `SınıfAdı.fonksiyon(...)` şeklinde çağrılabilmesini sağlıyor.
3. **Alarm zincirleme** — hemen hemen her alarm tetiklenmesi, kendini bir sonraki tetiklenme için yeniden kurarak "sonsuz döngü" hissi veren bir zincir oluşturuyor; bu zincir sadece bitiş tarihi geçtiğinde ya da ilaç silinip alarmı iptal edildiğinde kopuyor.


---
