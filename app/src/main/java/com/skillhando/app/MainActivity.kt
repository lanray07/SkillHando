package com.skillhando.app

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel

class MainActivity : AppCompatActivity() {
    private lateinit var voice: Voice
    private lateinit var media: PracticeMedia

    override fun attachBaseContext(newBase: android.content.Context) {
        super.attachBaseContext(Localisation.configuredContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Localisation.load(this)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Billing.open(this)
        voice = Voice(this)
        media = PracticeMedia(this)
        setContent {
            val model: LearningModel = viewModel()
            val dark = isSystemInDarkTheme()
            MaterialTheme(
                colorScheme = if (dark) darkColorScheme(
                    primary = Color(0xFFC3C6FF), onPrimary = Color(0xFF252F63),
                    background = Color(0xFF171923), surface = Color(0xFF1E2130),
                    surfaceContainer = Color(0xFF282D40), secondaryContainer = Color(0xFF214945),
                    onSecondaryContainer = Color(0xFFC3EFEB)
                ) else lightColorScheme(
                    primary = Color(0xFF303A72), onPrimary = Color.White,
                    background = Color(0xFFFAF8F2), surface = Color(0xFFFFFDF8),
                    surfaceContainer = Color(0xFFF0EEE8), secondaryContainer = Color(0xFFD5ECE7),
                    onSecondaryContainer = Color(0xFF1B4B46), outlineVariant = Color(0xFFD5D0DC)
                )
            ) { androidx.compose.runtime.CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides if (android.text.TextUtils.getLayoutDirectionFromLocale(Localisation.locale) == android.view.View.LAYOUT_DIRECTION_RTL) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr) {
                SkillHando(model, voice, media)
            } }
        }
    }

    override fun onStop() {
        voice.cancel()
        media.close()
        super.onStop()
    }

    override fun onDestroy() {
        voice.close()
        super.onDestroy()
    }
}
