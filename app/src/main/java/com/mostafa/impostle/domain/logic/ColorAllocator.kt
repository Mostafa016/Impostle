package com.mostafa.impostle.domain.logic

import com.mostafa.impostle.domain.model.NewPlayerColors

object ColorAllocator {
    fun assignColor(usedColors: Set<String>): NewPlayerColors {
        val usedColorsEnumValue =
            usedColors
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
