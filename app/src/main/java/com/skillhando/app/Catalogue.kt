package com.skillhando.app

import android.content.Context
import org.json.JSONArray

class Catalogue(context: Context) {
    val skills = (runCatching { context.assets.open("i18n/catalogue-" + Localisation.locale.toLanguageTag() + ".json") }.getOrNull() ?: context.assets.open("catalogue.json")).bufferedReader().use { reader ->
        val data = JSONArray(reader.readText())
        List(data.length()) { index ->
            val item = data.getJSONObject(index)
            val id = item.getString("id")
            val content = item.getJSONArray("challenges")
            Skill(id, item.getString("title"), item.getString("description"), strings(item.getJSONArray("rubric")),
                List(content.length()) { order ->
                    val entry = content.getJSONObject(order)
                    Challenge(id + "-" + entry.getString("id"), entry.getString("title"), entry.getString("objective"),
                        entry.getString("materials"), strings(entry.getJSONArray("steps")), entry.getString("criterion"),
                        entry.getString("prompt"), entry.getString("experiment"), entry.getInt("minutes"), order)
                })
        }
    }
    private fun strings(array: JSONArray) = List(array.length(), array::getString)
}
