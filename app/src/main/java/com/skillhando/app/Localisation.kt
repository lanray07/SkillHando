package com.skillhando.app

import android.content.Context
import android.content.res.Configuration
import org.json.JSONObject
import java.util.Locale

object Localisation {
    private var fallback = JSONObject()
    private var translated = JSONObject()
    var locale: Locale = Locale.UK
        private set
    private val targets = listOf("en-GB", "en-US", "en-AU", "en-IN", "es-ES", "es-419", "es-MX", "fr-FR", "fr-CA", "de-DE",
        "it-IT", "pt-BR", "pt-PT", "nl-NL", "pl-PL", "sv-SE", "ja-JP", "hi-IN", "ar", "tr-TR", "id-ID")

    var supported = listOf("en-GB")
        private set

    fun contentTag(tag: String) = when (tag) {
        "en-IN" -> "en-GB"
        "es-MX" -> "es-419"
        else -> tag
    }

    fun configuredContext(context: Context): Context {
        val bundled = context.assets.list("i18n").orEmpty().toSet()
        supported = targets.filter { it == "en-GB" || "${contentTag(it)}.json" in bundled }
        val selected = context.getSharedPreferences("settings", 0).getString("language", null)
        val system = context.resources.configuration.locales[0]
        val tag = selected?.takeIf { it in supported }
            ?: supported.find { it.equals(system.toLanguageTag(), ignoreCase = true) }
            ?: supported.find { Locale.forLanguageTag(it).language == system.language } ?: "en-GB"
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
        return context.createConfigurationContext(config)
    }

    fun load(context: Context) {
        val config = configuredContext(context)
        locale = config.resources.configuration.locales[0]
        fallback = JSONObject(config.assets.open("ui-en.json").bufferedReader().use { it.readText() })
        translated = runCatching { JSONObject(config.assets.open("i18n/${contentTag(locale.toLanguageTag())}.json").bufferedReader().use { it.readText() }) }
            .getOrDefault(JSONObject())
    }

    fun string(key: String) = translated.optString(key).ifEmpty { fallback.optString(key) }
}

fun tr(key: String) = Localisation.string(key)
fun trf(key: String, vararg values: Any) = String.format(Localisation.locale, tr(key), *values)
