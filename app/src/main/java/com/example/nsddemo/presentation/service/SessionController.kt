package com.example.nsddemo.presentation.service

interface SessionController {
    fun startHost(gameCode: String, playerId: String)
    fun startJoin(gameCode: String, playerId: String)
    fun stopSession()
}