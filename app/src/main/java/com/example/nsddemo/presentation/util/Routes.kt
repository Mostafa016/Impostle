package com.example.nsddemo.presentation.util

enum class Routes(
    val route: String,
) {
    MainMenu("main_menu_screen"),
    Settings("settings_screen"),
    CreateGameLoading("create_game_screen"),
    JoinGame("join_game_screen"),
    JoinGameLoading("join_game_loading_screen"),
    Lobby("lobby_screen"),
    ChooseCategory("choose_category_screen"),
    RoleReveal("category_and_word_screen"),
    Question("question_screen"),
    ReplayRoundChoice("choose_extra_questions_screen"),
    Voting("voting_screen"),
    ImposterGuess("imposter_guess_screen"),
    VotingResults("voting_results_screen"),
    Scoreboard("scoreboard_screen"),
    EndGame("end_game_screen"),
    Paused("pause_screen"),
    Disconnected("disconnected_screen"),

    JoinGameGraph("join_game_graph"),
    GameSessionGraph("game_session_graph"),
    RootGraph("root_graph"),
}
