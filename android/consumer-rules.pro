# Consumer ProGuard rules for @capgo/capacitor-native-biometric
# Merged into app release minification when the app depends on this library.

# Capacitor discovers plugins and invokes API methods via reflection on annotations.
# Keep NativeBiometric and its @PluginMethod and @ActivityCallback entry points so R8 does not strip or rename them.
# Keep annotation metadata Capacitor and WebView read through reflection.
-keepattributes RuntimeVisibleAnnotations

-keep @com.getcapacitor.annotation.CapacitorPlugin class ee.forgr.biometric.NativeBiometric {
    <init>(...);
}
-keepclassmembers class ee.forgr.biometric.NativeBiometric {
    @com.getcapacitor.PluginMethod <methods>;
    @com.getcapacitor.annotation.ActivityCallback <methods>;
}
