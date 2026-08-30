# Add project specific ProGuard rules here.
# By default, the flags in this file are appended to flags specified
# in C:\Users\anton\AppData\Local\Android\Sdk/tools/proguard/proguard-android.txt
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Add any project specific keep rules here:

# Room library rules
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>(...);
}

-keep class * extends androidx.room.RoomDatabase
-keep class com.tibarra.gymhelper.data.model.** { *; }
-keep interface com.tibarra.gymhelper.data.dao.** { *; }

# Maintain source file names and line numbers for easier debugging of obfuscated traces
-keepattributes SourceFile,LineNumberTable

# If you use Compose, R8 handles most things, but keep these just in case for reflection
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
