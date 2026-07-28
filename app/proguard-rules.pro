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

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# OkHttp Rules
-dontwarn org.bouncycastle.jsse.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# Keep Retrofit and OkHttp classes
-keep class retrofit2.** { *; }
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keepattributes Signature
-keepattributes Exceptions
-keepattributes *Annotation*

# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# Модели для Retrofit и GSON
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Тензорфлоу Лайт
-keep class org.tensorflow.lite.** { *; }

# Сохраняем Generic Signatures для дженериков
-keepattributes Signature
-keepattributes InnerClasses
-keepattributes EnclosingMethod
-keepattributes InnerClasses,EnclosingMethod,Signature

# Правила для LiveData и ViewModel
-keep class androidx.lifecycle.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends androidx.lifecycle.AndroidViewModel { *; }
-keep class * implements androidx.lifecycle.LifecycleObserver { *; }
-keepclassmembers class * implements androidx.lifecycle.LifecycleObserver {
    <methods>;
}
-keepclassmembers class android.arch.** { *; }
-keep class * implements androidx.lifecycle.LiveData { *; }

# Правила для сохранения Generic types в ModelView и LiveData
-keepclassmembers class ** {
    androidx.lifecycle.MutableLiveData *;
    androidx.lifecycle.LiveData *;
}

# GSON
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes RuntimeVisibleParameterAnnotations
-keepattributes RuntimeInvisibleParameterAnnotations
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
-keep class com.google.gson.** { *; }
# Keep anonymous subclasses of TypeToken (important for List<T> type resolution)
-keep class * extends com.google.gson.reflect.TypeToken { *; }
-keep class * implements java.io.Serializable { *; }

# Модели данных приложения
-keep class com.martist.vitamove.workout.data.model.** { *; }
-keep class com.martist.vitamove.db.entity.** { *; }
-keep class com.martist.vitamove.models.** { *; }
-keep class com.martist.vitamove.workout.data.model.cache.** { *; }
-keep class com.martist.vitamove.workout.data.model.room.** { *; }

# EventBus
-keepattributes *Annotation*
-keepclassmembers class ** {
    @org.greenrobot.eventbus.Subscribe <methods>;
}
-keep enum org.greenrobot.eventbus.ThreadMode { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class * extends androidx.room.Dao { *; }
-dontwarn android.arch.util.paging.CountedDataSource
-dontwarn androidx.room.paging.LimitOffsetDataSource

# Правила для устранения предупреждений R8 о недостающих классах AWT/Swing
# Эти классы недоступны в Android и используются библиотекой demidko:aot
# которая транзитивно включает org.jetbrains.skiko (предназначенную для desktop)
-dontwarn java.awt.Canvas
-dontwarn java.awt.Color
-dontwarn java.awt.Component
-dontwarn java.awt.DisplayMode
-dontwarn java.awt.Font
-dontwarn java.awt.FontFormatException
-dontwarn java.awt.Graphics
-dontwarn java.awt.GraphicsConfiguration
-dontwarn java.awt.GraphicsDevice
-dontwarn java.awt.Image
-dontwarn java.awt.LayoutManager
-dontwarn java.awt.Point
-dontwarn java.awt.Window
-dontwarn java.awt.color.ColorSpace
-dontwarn java.awt.event.ActionListener
-dontwarn java.awt.event.ComponentAdapter
-dontwarn java.awt.event.HierarchyListener
-dontwarn java.awt.event.InputMethodListener
-dontwarn java.awt.event.KeyListener
-dontwarn java.awt.event.MouseListener
-dontwarn java.awt.event.MouseMotionListener
-dontwarn java.awt.event.MouseWheelListener
-dontwarn java.awt.image.BufferedImage
-dontwarn java.awt.image.ColorModel
-dontwarn java.awt.image.ComponentColorModel
-dontwarn java.awt.image.DataBuffer
-dontwarn java.awt.image.DataBufferByte
-dontwarn java.awt.image.ImageObserver
-dontwarn java.awt.image.Raster
-dontwarn java.awt.image.WritableRaster
-dontwarn javax.accessibility.Accessible
-dontwarn javax.swing.JPanel
-dontwarn javax.swing.JRootPane
-dontwarn javax.swing.SwingUtilities
-dontwarn javax.swing.Timer
-dontwarn javax.swing.UIManager
