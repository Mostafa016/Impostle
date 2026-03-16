package com.example.nsddemo.presentation.util

import com.example.nsddemo.R

object WordResourceMapper {
    private val AnimalWords =
        mapOf(
            "lion" to R.string.lion,
            "tiger" to R.string.tiger,
            "cheetah" to R.string.cheetah,
            "bear" to R.string.bear,
            "panda" to R.string.panda,
            "wolf" to R.string.wolf,
            "dog" to R.string.dog,
            "cat" to R.string.cat,
            "fox" to R.string.fox,
            "chicken" to R.string.chicken,
            "sheep" to R.string.sheep,
            "cow" to R.string.cow,
            "pig" to R.string.pig,
            "horse" to R.string.horse,
            "zebra" to R.string.zebra,
            "donkey" to R.string.donkey,
            "giraffe" to R.string.giraffe,
            "monkey" to R.string.monkey,
            "elephant" to R.string.elephant,
            "fish" to R.string.fish,
            "shark" to R.string.shark,
            "octopus" to R.string.octopus,
            "squid" to R.string.squid,
            "whale" to R.string.whale,
            "walrus" to R.string.walrus,
            "polar bear" to R.string.polar_bear,
        )

    private val FoodWords =
        mapOf(
            "pizza" to R.string.pizza,
            "burger" to R.string.burger,
            "fries" to R.string.fries,
            "ice cream" to R.string.ice_cream,
            "cookie" to R.string.cookie,
            "bread" to R.string.bread,
            "milk" to R.string.milk,
            "juice" to R.string.juice,
            "pasta" to R.string.pasta,
            "rice" to R.string.rice,
            "popcorn" to R.string.popcorn,
            "eggs" to R.string.eggs,
            "cheese" to R.string.cheese,
            "cake" to R.string.cake,
            "beef" to R.string.beef,
            "chicken" to R.string.chicken,
            "fish" to R.string.fish,
        )

    private val JobWords =
        mapOf(
            "teacher" to R.string.teacher,
            "doctor" to R.string.doctor,
            "waiter" to R.string.waiter,
            "actor" to R.string.actor,
            "athlete" to R.string.athlete,
            "singer" to R.string.singer,
            "police officer" to R.string.police_officer,
            "firefighter" to R.string.firefighter,
            "construction worker" to R.string.construction_worker,
            "nurse" to R.string.nurse,
            "programmer" to R.string.programmer,
            "engineer" to R.string.engineer,
            "accountant" to R.string.accountant,
            "farmer" to R.string.farmer,
            "driver" to R.string.driver,
            "chef" to R.string.chef,
        )

    fun getResId(key: String): Int {
        val normalizedKey = key.lowercase()
        return AnimalWords[normalizedKey]
            ?: FoodWords[normalizedKey]
            ?: JobWords[normalizedKey]!!
    }
}
