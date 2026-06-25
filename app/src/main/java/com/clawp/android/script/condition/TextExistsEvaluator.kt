package com.clawp.android.script.condition

import com.clawp.android.script.model.Condition

/**
 * Evaluates [text_exists] and [text_not_exists] conditions by searching
 * the screen-tree text for `text="...value..."` patterns.
 */
class TextExistsEvaluator(private val exists: Boolean) : ConditionEvaluator {

    override fun evaluate(
        condition: Condition,
        screenTreeText: String?,
        service: com.clawp.android.service.ClawAccessibilityService?
    ): Boolean {
        val text = condition.text ?: return false
        if (screenTreeText.isNullOrBlank()) return false

        val found = textMatchesInTree(screenTreeText, text, condition.match ?: "contains")
        return if (exists) found else !found
    }

    companion object {
        /**
         * Search for `text="...value..."` in the screen tree string.
         */
        fun textMatchesInTree(treeText: String, searchText: String, matchMode: String): Boolean {
            // We look for `text="` patterns and check each extracted text
            val pattern = Regex("text=\"([^\"]*)\"")
            for (m in pattern.findAll(treeText)) {
                val nodeText = m.groupValues[1]
                if (matches(nodeText, searchText, matchMode)) return true
            }
            return false
        }

        fun matches(nodeText: String, searchText: String, matchMode: String): Boolean {
            return when (matchMode) {
                "exact" -> nodeText == searchText
                "startsWith" -> nodeText.startsWith(searchText)
                "endsWith" -> nodeText.endsWith(searchText)
                "contains" -> nodeText.contains(searchText)
                else -> nodeText.contains(searchText) // default to contains
            }
        }
    }
}