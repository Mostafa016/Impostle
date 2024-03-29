package com.example.nsddemo.ui

enum class PlayerColors(val argb: Long) {
    Red(0xFFD71E22),
    Blue(0xFF1D3CE9),
    DarkGreen(0xFF023020),
    Pink(0xFFFF63D4),
    Orange(0xFFFF8D1C),
    Yellow(0xFFFFFF67),
    Purple(0xFF783DD2),
    Brown(0xFF80582D),
    Cyan(0xFF44FFF7),
    Lime(0xFF5BFE4B),
    Maroon(0xFF6C2B3D),
    Rose(0xFFFFD6EC);
}

fun String.toPlayerColors(): PlayerColors {
    return when (this) {
        "0xFFD71E22" -> PlayerColors.Red
        "0xFF1D3CE9" -> PlayerColors.Blue
        "0xFF023020" -> PlayerColors.DarkGreen
        "0xFFFF63D4" -> PlayerColors.Pink
        "0xFFFF8D1C" -> PlayerColors.Orange
        "0xFFFFFF67" -> PlayerColors.Yellow
        "0xFF783DD2" -> PlayerColors.Purple
        "0xFF80582D" -> PlayerColors.Brown
        "0xFF44FFF7" -> PlayerColors.Cyan
        "0xFF5BFE4B" -> PlayerColors.Lime
        "0xFF6C2B3D" -> PlayerColors.Maroon
        "0xFFFFD6EC" -> PlayerColors.Rose
        else -> throw IllegalArgumentException("Invalid color")
    }
}