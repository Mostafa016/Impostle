package com.example.nsddemo.domain.logic

import com.example.nsddemo.core.util.GameConstants

object GameCodeGenerator {
    fun generate(): String =
        (1..GameConstants.CODE_LENGTH)
            .map { GameConstants.CODE_ALLOWED_CHARACTERS.random() }
            .joinToString("")
}
