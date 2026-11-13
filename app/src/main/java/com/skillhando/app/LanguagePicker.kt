package com.skillhando.app

import android.app.Activity
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

@Composable
fun LanguagePicker() {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { open = true }) { Text(tr("language") + " · " + Localisation.locale.getDisplayName(Localisation.locale)) }
        DropdownMenu(open, { open = false }) {
            Localisation.supported.forEach { tag ->
                val locale = Locale.forLanguageTag(tag)
                DropdownMenuItem(text = { Text(locale.getDisplayName(locale)) }, onClick = {
                    context.getSharedPreferences("settings", 0).edit().putString("language", tag).apply()
                    open = false
                    generateSequence(context) { (it as? ContextWrapper)?.baseContext }
                        .filterIsInstance<Activity>().firstOrNull()?.recreate()
                })
            }
        }
    }
}
