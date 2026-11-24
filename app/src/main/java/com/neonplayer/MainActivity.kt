package com.neonplayer

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.neonplayer.data.settings.AppLocale
import com.neonplayer.domain.model.OrientationLock
import com.neonplayer.ui.main.MainViewModel
import com.neonplayer.ui.main.NeonPlayerApp
import com.neonplayer.ui.theme.NeonTheme
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private var audioPermissionGranted by mutableStateOf(false)

    private val audioPermission: String
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_AUDIO
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { result ->
        audioPermissionGranted = result[audioPermission] ?: hasAudioPermission()
        if (audioPermissionGranted) viewModel.rescan()
    }

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLocale.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        applyCutoutMode()
        applyImmersive()
        audioPermissionGranted = hasAudioPermission()
        if (audioPermissionGranted) {
            viewModel.rescan()
        } else {
            requestPermissions()
        }
        observeOrientation()
        observeKeepScreenOn()
        observeLanguage()
        observeEvents()
        setContent {
            val settings by viewModel.settings.collectAsState()
            NeonTheme(
                accent = settings.accent,
                glassIcons = settings.glassIcons,
                reduceMotion = settings.reduceMotion,
            ) {
                NeonPlayerApp(
                    vm = viewModel,
                    windowSizeClass = calculateWindowSizeClass(this),
                    audioPermissionGranted = audioPermissionGranted,
                    onGrantAccess = { requestAudioAccess() },
                )
            }
        }
    }

    override fun onStart() {
        super.onStart()
        (application as NeonApp).container.playerConnection.connect()
    }

    override fun onResume() {
        super.onResume()
        applyImmersive()
        audioPermissionGranted = hasAudioPermission()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) applyImmersive()
    }

    override fun dispatchTouchEvent(ev: android.view.MotionEvent?): Boolean {
        viewModel.onUserInteraction()
        return super.dispatchTouchEvent(ev)
    }

    private fun hasAudioPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, audioPermission) == PackageManager.PERMISSION_GRANTED

    private fun requestPermissions() {
        val permissions = ArrayList<String>(2)
        permissions.add(audioPermission)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        permissionLauncher.launch(permissions.toTypedArray())
    }

    private fun requestAudioAccess() {
        val permanentlyDenied = !shouldShowRequestPermissionRationale(audioPermission)
        if (permanentlyDenied) {
            openAppSettings()
        } else {
            requestPermissions()
        }
    }

    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching { startActivity(intent) }
    }

    private fun applyCutoutMode() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        val attributes = window.attributes
        attributes.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        window.attributes = attributes
    }

    private fun applyImmersive() {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun observeOrientation() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings
                    .map { it.orientation }
                    .distinctUntilChanged()
                    .collect { lock ->
                        requestedOrientation = when (lock) {
                            OrientationLock.LANDSCAPE -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                            OrientationLock.AUTO -> ActivityInfo.SCREEN_ORIENTATION_FULL_USER
                            OrientationLock.PORTRAIT -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                        }
                    }
            }
        }
    }

    private fun observeKeepScreenOn() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                combine(
                    viewModel.stageMode,
                    viewModel.settings,
                    viewModel.player,
                ) { stage, settings, player ->
                    stage || (settings.keepScreenOnWhilePlaying && player.isPlaying)
                }
                    .distinctUntilChanged()
                    .collect { keepOn ->
                        if (keepOn) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        }
                    }
            }
        }
    }

    private fun observeLanguage() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.settings
                    .map { it.language }
                    .distinctUntilChanged()
                    .collect { tag ->
                        if (AppLocale.store(this@MainActivity, tag)) recreate()
                    }
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.events.collect { message ->
                    Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
