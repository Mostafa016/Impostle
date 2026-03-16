package com.example.nsddemo.presentation.util

import androidx.compose.foundation.IndicationNodeFactory
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DelegatableNode
import androidx.compose.ui.node.DrawModifierNode

/**
 * An IndicationNodeFactory that creates indication nodes without any visual feedback.
 * Useful for clickable elements that should not show ripple effects or other indications.
 *
 * This factory creates nodes that simply draw content without any additional visual effects,
 * effectively disabling all click animations and feedback while preserving functionality.
 */
class NoFeedbackIndication : IndicationNodeFactory {
    override fun create(interactionSource: InteractionSource): DelegatableNode = NoFeedbackIndicationNode(interactionSource)

    override fun hashCode(): Int = -1

    override fun equals(other: Any?): Boolean = other is NoFeedbackIndication
}

/**
 * A modifier node that implements no-feedback indication behavior.
 * This node draws content without any visual indication effects.
 *
 * Unlike default indication nodes that add ripples, highlights, or other visual feedback,
 * this node simply passes through the content drawing without any modifications.
 */
private class NoFeedbackIndicationNode(
    private val interactionSource: InteractionSource,
) : Modifier.Node(),
    DrawModifierNode {
    override fun ContentDrawScope.draw() {
        // Simply draw the content without any indication effects
        // This is the key: we don't add any visual layers or effects
        drawContent()
    }

    // No additional setup needed for this indication type
    override fun onAttach() {
        super.onAttach()
        // Intentionally empty - no indication effects to set up
        // We don't need to listen to interaction source events
        // since we're not providing any visual feedback
    }

    override fun onDetach() {
        super.onDetach()
        // Intentionally empty - no indication effects to clean up
    }
}
