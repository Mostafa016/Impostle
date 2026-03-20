package com.mostafa.impostle.domain.model

enum class NewPlayerColors(
    val hexCode: String,
) {
    Red("FFD71E22"),
    Blue("FF1D3CE9"),
    DarkGreen("FF023020"),
    Pink("FFFF63D4"),
    Orange("FFFF8D1C"),
    Yellow("FFFFFF67"),
    Purple("FF783DD2"),
    Brown("FF80582D"),
    Cyan("FF44FFF7"),
    Lime("FF5BFE4B"),
    Maroon("FF6C2B3D"),
    Rose("FFFFD6EC"),
    Teal("FF008080"),
    Navy("FF0B1C2D"),
    Gold("FFFFC107"),
    Gray("FF8E8E93"),
    Fallback("FF000000"),
    ;

    override fun toString(): String = hexCode

    companion object {
        fun fromHex(hex: String): NewPlayerColors = entries.find { it.hexCode.equals(hex, ignoreCase = true) } ?: Fallback
    }
}
