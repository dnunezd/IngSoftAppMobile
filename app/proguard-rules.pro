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
# add-cache-consultas (design.md, D2): la base local guarda como JSON, con Gson, las listas de los
# detalles. Hoy isMinifyEnabled = false; si se activa, R8 renombraría sus campos y lo guardado no se
# podría leer. En ese caso, descomentar:
#-keep class com.example.vinilos.data.model.Cancion { *; }
#-keep class com.example.vinilos.data.model.Artista { *; }
#-keep class com.example.vinilos.data.model.Comentario { *; }
#-keep class com.example.vinilos.data.model.IntegranteArtista { *; }
#-keep class com.example.vinilos.data.model.AlbumArtista { *; }
#-keep class com.example.vinilos.data.model.PremioArtista { *; }
