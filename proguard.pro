
#-dontskipnonpubliclibraryclasses
-allowaccessmodification
-repackageclasses dx.signer
#-optimizations !method/inlining/*
-dontoptimize
#-renamesourcefileattribute SourceFile
-keepattributes AnnotationDefault,RuntimeVisible*Annotations,Signature,SourceFile,LineNumberTable

#-dontwarn **
-ignorewarnings

# Keep - Applications. Keep all application classes, along with their 'main' methods.
-keep public class dx.signer.UX {
    public static void main(java.lang.String[]);
}


# Also keep - Enumerations. Keep the special static methods that are required in
# enumeration classes.
-keepclassmembers enum  * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Also keep - Swing UI L&F. Keep all extensions of javax.swing.plaf.ComponentUI,
# along with the special 'createUI' method.
-keep class * extends javax.swing.plaf.ComponentUI {
    public static javax.swing.plaf.ComponentUI createUI(javax.swing.JComponent);
}

-keep class org.slf4j.** {
    *;
}

-keep class javax.annotation.** {
    *;
}

-keep class dx.channel.ApkSigns {
    public *;
}

-keep class com.android.apksig.internal.x509.** {
    *;
}
-keep class com.android.apksig.internal.pkcs7.** {
    *;
}