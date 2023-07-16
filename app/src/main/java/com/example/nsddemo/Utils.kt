package com.example.nsddemo

import kotlin.random.Random

object Debugging{
    const val TAG = "GIGACHAD"
}

fun <T,U> Map<T,U>.random(): Map.Entry<T,U> = entries.elementAt(Random.nextInt(size))
