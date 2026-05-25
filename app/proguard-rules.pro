# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes InnerClasses

# Gson
-keep class com.google.gson.** { *; }
-keepclassmembers class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-keep class okhttp3.** { *; }
-dontwarn okio.**

# LangChain4j
-keep class dev.langchain4j.** { *; }
-dontwarn dev.langchain4j.**

# Feishu SDK
-keep class com.lark.oapi.** { *; }
-dontwarn com.lark.oapi.**

# MMKV
-keep class com.tencent.mmkv.** { *; }

# NanoHTTPD
-keep class fi.iki.elonen.** { *; }

# Keep tool classes (used by reflection in ToolRegistry)
-keep class com.clawp.android.tool.** { *; }

# Keep model classes for serialization
-keep class com.clawp.android.agent.** { *; }
-keep class com.clawp.android.task.** { *; }
