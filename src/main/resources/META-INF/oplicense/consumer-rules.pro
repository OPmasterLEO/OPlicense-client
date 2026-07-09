# Consumer rules for shading OPlicense-client into an obfuscated plugin JAR.
# Include from ProGuard/R8:
#   -include META-INF/oplicense/consumer-rules.pro

-keep class net.opmasterleo.license.** { *; }
-keepnames class net.opmasterleo.license.** { *; }

-keepclassmembers class net.opmasterleo.license.api.ResponseVerifier$* {
    public boolean verify(java.lang.String, java.lang.String, java.lang.String);
}

-keepclassmembers class net.opmasterleo.license.internal.platform.PlatformSupport {
    public static ** call(...);
    public static ** readTextFile(java.lang.String);
}

-dontwarn java.net.http.**
