package com.example.nsddemo.domain.logic

import com.example.nsddemo.domain.model.NewPlayerColors

object ColorAllocator {
    fun assignColor(usedColors: Set<String>): NewPlayerColors {
        val usedColorsEnumValue = usedColors
            .map { NewPlayerColors.fromHex(it) }
            .toSet()

        val available =
            NewPlayerColors.entries.toSet() - (usedColorsEnumValue + setOf(NewPlayerColors.Fallback))

        return if (available.isNotEmpty()) {
            available.random()
        } else {
            NewPlayerColors.entries.filter { it != NewPlayerColors.Fallback }.random()
        }
    }
}