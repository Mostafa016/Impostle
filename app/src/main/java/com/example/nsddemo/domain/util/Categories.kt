package com.example.nsddemo.domain.util

import androidx.compose.ui.graphics.Color
import com.example.nsddemo.R

enum class Categories(
    val wordResourceIds: List<Int>,
    val color: Color,
    val imageDrawableID: Int,
    val nameResourceId: Int
) {
    Animals(
        wordResourceIds = listOf(
            R.string.lion,
            R.string.tiger,
            R.string.cheetah,
            R.string.bear,
            R.string.panda,
            R.string.wolf,
            R.string.dog,
            R.string.cat,
            R.string.fox,
            R.string.chicken,
            R.string.sheep,
            R.string.cow,
            R.string.pig,
            R.string.horse,
            R.string.zebra,
            R.string.donkey,
            R.string.giraffe,
            R.string.monkey,
            R.string.elephant,
            R.string.fish,
            R.string.shark,
            R.string.octopus,
            R.string.squid,
            R.string.whale,
            R.string.walrus,
            R.string.polar_bear
        ),
        color = Color.Green,
        imageDrawableID = R.drawable.animals_silhouette,
        nameResourceId = R.string.animals
    ),
    Food(
        listOf(
            R.string.pizza,
            R.string.burger,
            R.string.fries,
            R.string.ice_cream,
            R.string.cookie,
            R.string.bread,
            R.string.milk,
            R.string.juice,
            R.string.pasta,
            R.string.rice,
            R.string.popcorn,
            R.string.eggs,
            R.string.cheese,
            R.string.cake,
            R.string.beef,
            R.string.chicken,
            R.string.fish
        ),
        color = Color.Red,
        imageDrawableID = R.drawable.food_silhouette,
        nameResourceId = R.string.food
    ),
    Jobs(
        listOf(
            R.string.teacher,
            R.string.doctor,
            R.string.waiter,
            R.string.actor,
            R.string.athlete,
            R.string.singer,
            R.string.police_officer,
            R.string.firefighter,
            R.string.construction_worker,
            R.string.nurse,
            R.string.programmer,
            R.string.engineer,
            R.string.accountant,
            R.string.farmer,
            R.string.driver,
            R.string.chef
        ),
        color = Color.Blue,
        imageDrawableID = R.drawable.jobs_silhouette,
        nameResourceId = R.string.jobs
    )
}