# 拼音乐园 - R8/ProGuard 规则
# Compose、Kotlin、AndroidX 的规则已由各 AAR 的 consumer-rules 自带，这里只做必要补充。

# 保留注解与泛型签名信息（反射/序列化场景需要）
-keepattributes Signature,InnerClasses,EnclosingMethod,*Annotation*

# Kotlin 元数据
-keep class kotlin.Metadata { *; }
-dontwarn org.jetbrains.annotations.**

# 崩溃栈可读性
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
