package com.example.nsddemo.presentation.util

enum class Routes(val route: String) {
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
    EndGame("end_game_screen"),
    GameSession("game_session_graph"),
    JoinGameSession("join_game_session_graph"),
}