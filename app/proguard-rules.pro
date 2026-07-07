# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Preserve line number information so release crash traces stay readable,
# and hide the original source file name.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ──────────────────────────────────────────────────────────────
#  Gson
# ──────────────────────────────────────────────────────────────
# Gson relies on generic type information and annotations at runtime.
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

# Keep @SerializedName-annotated fields so R8 does not leave them null.
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep TypeAdapter / factory / (de)serializer implementations used via @JsonAdapter.
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Retain generic signatures of TypeToken and its (anonymous) subclasses (R8 3.0+).
# MedicineRepository builds an anonymous TypeToken<ArrayList<Medicine>>.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

# ──────────────────────────────────────────────────────────────
#  App model
# ──────────────────────────────────────────────────────────────
# Medicine is (de)serialized both by Gson (field names become JSON keys) and by
# Java serialization when passed through Intent extras (getSerializableExtra).
# Both mechanisms use reflection over field names, so keep the class and members.
-keep class com.YucelDigital.ilactakip.Medicine { *; }
-keep class com.YucelDigital.ilactakip.Medicine$* { *; }
