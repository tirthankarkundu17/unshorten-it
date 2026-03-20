# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Preserve Gson and Retrofit annotations
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod

# Keep the data classes used for API requests and responses
-keep class in.bitmaskers.unshortenit.data.api.** { *; }
-keep class in.bitmaskers.unshortenit.data.model.** { *; }

# Keep the Retrofit service interface
-keep interface in.bitmaskers.unshortenit.data.api.UnshortenApiService { *; }

# Gson specific rules
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Keep members of data classes to avoid losing default constructors
-keepclassmembers class in.bitmaskers.unshortenit.data.api.** {
    <fields>;
    <methods>;
}

# Keep the line number information for debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile