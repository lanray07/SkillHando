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
        "it-IT", "pt-BR", "pt-PT", "nl-NL", "pl-PL", "sv-SE", "ja-JP", "hi-IN", "ar", "tr-TR", "id-ID",
        "ko-KR", "zh-CN", "zh-TW")

    var supported = listOf("en-GB")
        private set

    fun contentTag(tag: String) = when (tag) {
        "en-IN" -> "en-GB"
        "es-MX" -> "es-419"
        else -> tag
    }

    private fun availableLocales(context: Context): List<String> {
        val files = context.assets.list("i18n").orEmpty().toSet()
        return targets.filter { tag ->
            val content = contentTag(tag)
            content == "en-GB" || ("$content.json" in files && "catalogue-$content.json" in files)
        }
    }

    fun configuredContext(context: Context): Context = configuredContext(context, availableLocales(context))

    private fun configuredContext(context: Context, available: List<String>): Context {
        val selected = context.getSharedPreferences("settings", 0).getString("language", null)
        val system = context.resources.configuration.locales[0]
        val tag = resolveTag(selected, system, available)
        val config = Configuration(context.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
        return context.createConfigurationContext(config)
    }

    internal fun resolveTag(selected: String?, system: Locale, available: List<String>): String =
        selected?.takeIf { it in available }
            ?: chineseTag(system)?.takeIf { it in available }
            ?: available.find { it.equals(system.toLanguageTag(), ignoreCase = true) }
            ?: available.find { Locale.forLanguageTag(it).language == system.language }
            ?: "en-GB"

    internal fun chineseTag(locale: Locale): String? = when {
        locale.language != "zh" -> null
        locale.script == "Hant" -> "zh-TW"
        locale.script == "Hans" -> "zh-CN"
        locale.country in setOf("TW", "HK", "MO") -> "zh-TW"
        else -> "zh-CN"
    }

    fun load(context: Context) {
        supported = availableLocales(context)
        val config = configuredContext(context, supported)
        locale = config.resources.configuration.locales[0]
        fallback = JSONObject(config.assets.open("ui-en.json").bufferedReader().use { it.readText() })
        translated = runCatching { JSONObject(config.assets.open("i18n/${contentTag(locale.toLanguageTag())}.json").bufferedReader().use { it.readText() }) }
            .getOrDefault(JSONObject())
    }

    fun string(key: String) = translated.optString(key).ifEmpty { fallback.optString(key) }
}

fun tr(key: String) = Localisation.string(key)
fun trf(key: String, vararg values: Any) = String.format(Localisation.locale, tr(key), *values)
