package com.example.nsddemo

object Debugging {
    const val TAG = "GIGACHAD"
}

enum class ScreenRoutes(val route: String) {
    MainMenu("main_menu_screen"),
    Settings("settings_screen"),
    CreateGame("create_game_screen"),
    JoinGame("join_game_screen"),
    JoinGameLoading("join_game_loading_screen"),
    Lobby("lobby_screen"),
    ChooseCategory("choose_category_screen"),
    CategoryAndWord("category_and_word_screen"),
    Question("question_screen"),
    ChooseExtraQuestions("choose_extra_questions_screen"),
    Voting("voting_screen"),
    VotingResults("voting_results_screen"),
    Scoreboard("scoreboard_screen"),
    EndGame("end_game_screen")
}

object NSDConstants {
    const val BASE_SERVICE_NAME = "NsdChat"
    const val SERVICE_TYPE = "_nsdchat._tcp."
}

object GameConstants {
    const val DEFAULT_PLAYER_COLOR = "FF000000"
    const val CODE_LENGTH = 4
}


