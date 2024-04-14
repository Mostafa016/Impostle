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
    return when (this.toLong().toString(16).uppercase()) {
        "FFD71E22" -> PlayerColors.Red
        "FF1D3CE9" -> PlayerColors.Blue
        "FF023020" -> PlayerColors.DarkGreen
        "FFFF63D4" -> PlayerColors.Pink
        "FFFF8D1C" -> PlayerColors.Orange
        "FFFFFF67" -> PlayerColors.Yellow
        "FF783DD2" -> PlayerColors.Purple
        "FF80582D" -> PlayerColors.Brown
        "FF44FFF7" -> PlayerColors.Cyan
        "FF5BFE4B" -> PlayerColors.Lime
        "FF6C2B3D" -> PlayerColors.Maroon
        "FFFFD6EC" -> PlayerColors.Rose
        else -> throw IllegalArgumentException("Invalid color")
    }
}