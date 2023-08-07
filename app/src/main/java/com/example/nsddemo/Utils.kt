package com.example.nsddemo

import kotlin.random.Random

object Debugging {
    const val TAG = "GIGACHAD"
}

enum class Screen(val route: String) {
    MainMenu("main_menu_screen"),
    CreateGame("create_game_screen"),
    JoinGame("join_game_screen"),
    ClientGameStartLoading("client_game_start_loading_screen"),
    Lobby("lobby_screen"),
    ChooseCategory("choose_category_screen"),
    CategoryAndWord("category_and_word_screen"),
    Question("question_screen"),
    ExtraQuestions("extra_questions_screen"),
    Voting("voting_screen"),
    VotingResults("voting_results_screen"),
    Scoreboard("scoreboard_screen")
}

object NSDConstants {
    const val BASE_SERVICE_NAME = "NsdChat"
    const val SERVICE_TYPE = "_nsdchat._tcp."
}


