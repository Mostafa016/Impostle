package com.mostafa.impostle.domain.logic

import com.mostafa.impostle.core.util.GameConstants

object GameCodeGenerator {
    fun generate(): String =
        (1..GameConstants.CODE_LENGTH)
            .map { GameConstants.CODE_ALLOWED_CHARACTERS.random() }
            .joinToString("")
}
