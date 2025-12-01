# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ====================
# DISABLE OBFUSCATION
# ====================
# Don't obfuscate class names, method names, or field names
-dontobfuscate

# ====================
# DEBUGGING & STACK TRACES
# ====================
# Keep source file names and line numbers for better stack traces
-keepattributes SourceFile,LineNumberTable

# Keep parameter names for better debugging
-keepattributes LocalVariableTable,LocalVariableTypeTable

# Keep annotations
-keepattributes *Annotation*

# Keep signature for generics
-keepattributes Signature

# Keep exceptions
-keepattributes Exceptions

# ====================
# ANDROID CORE
# ====================
# Keep all native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep all enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep Parcelable implementations
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# Keep Serializable implementations
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ====================
# ANDROIDX & MATERIAL
# ====================
# AndroidX and Material libraries are already handled by their consumer ProGuard rules
# No additional rules needed since we're not obfuscating

# ====================
# KOTLIN
# ====================
# Keep Kotlin metadata
-keepattributes *Annotation*, InnerClasses
-dontnote kotlin.internal.PlatformImplementationsKt

# Keep Kotlin coroutines
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# ====================
# APP SPECIFIC
# ====================
# Keep all application classes
-keep class redhead.app.gnuxon.** { *; }

# Keep R class and its inner classes
-keep class **.R
-keep class **.R$* { *; }

# Keep BuildConfig
-keep class **.BuildConfig { *; }
