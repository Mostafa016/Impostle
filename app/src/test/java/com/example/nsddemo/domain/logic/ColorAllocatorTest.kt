package com.example.nsddemo.domain.logic

import com.example.nsddemo.domain.model.NewPlayerColors
import com.example.nsddemo.domain.model.Player
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ColorAllocatorTest {

    // Helper to create a dummy player with a specific color
    private fun createPlayer(colorHex: String) = Player(
        id = "id", name = "name", color = colorHex, isConnected = true
    )

    @Test
    fun `GIVEN empty list WHEN assignColor THEN returns valid color`() {
        val result = ColorAllocator.assignColor(emptySet())

        assertTrue(
            "Result should be a valid hex code",
            result in NewPlayerColors.entries
        )
        assertNotEquals(
            "Should not return Fallback color when empty",
            NewPlayerColors.Fallback,
            result
        )
    }

    @Test
    fun `GIVEN partially used palette WHEN assignColor THEN returns unused color`() {
        // Arrange: Use up all colors except one (e.g., Red)
        val targetColor = NewPlayerColors.Red
        val usedColors = NewPlayerColors.entries
            .filter { it != targetColor && it != NewPlayerColors.Fallback }
            .map { it.hexCode }
            .toSet()

        // Act
        val result = ColorAllocator.assignColor(usedColors)

        // Assert
        assertEquals(
            "Should pick the only remaining color",
            targetColor,
            result
        )
    }

    @Test
    fun `GIVEN full palette WHEN assignColor THEN returns any valid color (Reuse)`() {
        // Arrange: Use every single valid color
        val allColorsUsed = NewPlayerColors.entries
            .filter { it != NewPlayerColors.Fallback }
            .map { it.hexCode }
            .toSet()

        // Act
        val result = ColorAllocator.assignColor(allColorsUsed)

        // Assert
        assertTrue(
            "Should return a valid color even if full",
            result in NewPlayerColors.entries
        )
        assertNotEquals(
            "Should still avoid Fallback if possible",
            NewPlayerColors.Fallback,
            result
        )
    }

    @Test
    fun `GIVEN players with unknown custom colors WHEN assignColor THEN ignores them and picks standard`() {
        // Arrange
        val unknownColor = "0xFF0099"

        // Act
        val result = ColorAllocator.assignColor(setOf(unknownColor))

        // Assert
        assertTrue(
            "Should return a standard palette color",
            result in NewPlayerColors.entries && result != NewPlayerColors.Fallback
        )
    }
}