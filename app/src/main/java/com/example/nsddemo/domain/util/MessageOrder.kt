package com.example.nsddemo.domain.util

import kotlinx.serialization.Serializable

@Serializable
enum class MessageOrder(private val isSentToServer: Boolean) {
    CLIENT_PLAYER_NAME(true),
    SERVER_PLAYER_COLOR(false),
    SERVER_IS_LAST_PLAYER(false),
    SERVER_PLAYER_LIST(false),
    SERVER_CATEGORY_AND_WORD(false),
    CLIENT_CATEGORY_AND_WORD_CONFIRMATION(true),
    SERVER_ASK_QUESTION(false),
    CLIENT_QUESTION_CONFIRMATION(true),
    SERVER_END_OF_QUESTIONS_ROUND(false),
    SERVER_IS_EXTRA_QUESTIONS_ROUND(false),
    SERVER_START_VOTE(false),
    CLIENT_VOTE(true),
    SERVER_VOTING_RESULTS(false),
    SERVER_IS_REPLAY_GAME(false);
}