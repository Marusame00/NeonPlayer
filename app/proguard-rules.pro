-keep class com.neonplayer.playback.PlaybackService

# Play Console's edge-to-edge check scans the dex for a direct call to
# enableEdgeToEdge(). R8 inlines it into MainActivity.onCreate, which makes the
# call invisible to that scanner and raises "Edge-to-edge may not display for all
# users". Keeping these classes prevents the inlining and the renaming.
-keep class androidx.activity.EdgeToEdge { *; }
-keep class androidx.activity.EdgeToEdgeKt { *; }
-keep class androidx.activity.EdgeToEdgeApi* { *; }
-keepclassmembers class androidx.activity.ComponentActivity {
    public void enableEdgeToEdge(...);
}
