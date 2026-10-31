package com.ares.ewe.domain.cart

import com.ares.ewe.domain.model.ProductOptionGroup
import org.json.JSONArray
import org.json.JSONObject

data class ComboChoice(
    val groupName: String,
    val optionName: String,
)

object ComboCart {
    const val PREFIX = "combo:"

    fun isComboLine(variantId: String): Boolean = variantId.startsWith(PREFIX)

    fun encode(choices: List<ComboChoice>): String {
        val array = JSONArray()
        choices.forEach { choice ->
            array.put(
                JSONObject()
                    .put("groupName", choice.groupName)
                    .put("optionName", choice.optionName)
            )
        }
        return PREFIX + array.toString()
    }

    fun decode(variantId: String): List<ComboChoice> {
        if (!isComboLine(variantId)) return emptyList()
        val raw = variantId.removePrefix(PREFIX)
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                val group = item.optString("groupName")
                val option = item.optString("optionName")
                if (group.isNotBlank() && option.isNotBlank()) {
                    add(ComboChoice(group, option))
                }
            }
        }
    }

    fun summary(choices: List<ComboChoice>): String {
        if (choices.isEmpty()) return ""
        val grouped = linkedMapOf<String, MutableList<String>>()
        choices.forEach { choice ->
            grouped.getOrPut(choice.groupName) { mutableListOf() }.add(choice.optionName)
        }
        return grouped.entries.joinToString(" · ") { (group, options) ->
            "${group}: ${options.joinToString(", ")}"
        }
    }

    fun choicesFor(
        groups: List<ProductOptionGroup>,
        selectedOptionIds: Set<String>,
    ): List<ComboChoice> {
        val choices = mutableListOf<ComboChoice>()
        for (group in groups) {
            for (option in group.options) {
                if (option.id in selectedOptionIds) {
                    choices.add(ComboChoice(group.name, option.name))
                }
            }
        }
        return choices
    }

    fun selectionIsComplete(groups: List<ProductOptionGroup>, selectedOptionIds: Set<String>): Boolean {
        if (groups.isEmpty()) return true
        return groups.all { group ->
            val count = group.options.count { it.id in selectedOptionIds }
            count in group.minSelect..group.maxSelect
        }
    }
}
