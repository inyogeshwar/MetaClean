package com.inyogeshwar.metaclean

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.inyogeshwar.metaclean.core.ui.theme.MetaCleanTheme
import com.inyogeshwar.metaclean.domain.repository.AppThemeMode
import com.inyogeshwar.metaclean.domain.repository.MetadataRepository
import com.inyogeshwar.metaclean.domain.repository.SettingsRepository
import com.inyogeshwar.metaclean.presentation.home.HomeScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var metadataRepository: MetadataRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle images shared from other apps
        handleInboundIntent(intent)

        setContent {
            val settings by settingsRepository.settingsState.collectAsState()
            val mediaQueue by metadataRepository.mediaQueue.collectAsState()

            val isDark = when (settings.themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
            }

            MetaCleanTheme(darkTheme = isDark) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = androidx.compose.material3.MaterialTheme.colorScheme.background
                ) {
                    HomeScreen()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleInboundIntent(intent)
    }

    private fun handleInboundIntent(intent: Intent?) {
        if (intent == null) return
        val action = intent.action
        val type = intent.type

        if (type?.startsWith("image/") == true) {
            if (Intent.ACTION_SEND == action) {
                val imageUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
                }
                imageUri?.let { uri ->
                    CoroutineScope(Dispatchers.IO).launch {
                        metadataRepository.addMediaUris(listOf(uri))
                    }
                }
            } else if (Intent.ACTION_SEND_MULTIPLE == action) {
                val imageUris = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
                }
                imageUris?.let { uris ->
                    CoroutineScope(Dispatchers.IO).launch {
                        metadataRepository.addMediaUris(uris)
                    }
                }
            }
        }
    }
}
