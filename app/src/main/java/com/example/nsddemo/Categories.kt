package com.example.nsddemo

import androidx.compose.ui.graphics.Color
import com.example.nsddemo.R

enum class Categories(val words: List<String>, val color: Color, val imageDrawableID: Int) {
    Animals(
        words = listOf(
            "Lion",
            "Tiger",
            "Cheetah",
            "Bear",
            "Panda",
            "Wolf",
            "Dog",
            "Cat",
            "Fox",
            "Chicken",
            "Sheep",
            "Cow",
            "Pig",
            "Horse",
            "Zebra",
            "Donkey",
            "Giraffe",
            "Monkey",
            "Elephant",
            "Fish",
            "Shark",
            "Octopus",
            "Squid",
            "Whale",
            "Walrus",
            "Polar Bear"
        ),
        color = Color.Green,
        imageDrawableID = R.drawable.animals_silhouette
    ),
    Food(
        listOf(
            "Pizza",
            "Burger",
            "Fries",
            "Ice Cream",
            "Cookie",
            "Bread",
            "Milk",
            "Juice",
            "Pasta",
            "Rice",
            "Popcorn",
            "Eggs",
            "Cheese",
            "Cake",
            "Beef",
            "Chicken",
            "Fish"
        ),
        color = Color.Red,
        imageDrawableID = R.drawable.food_silhouette
    ),
    Jobs(
        listOf(
            "Teacher",
            "Doctor",
            "Waiter",
            "Actor",
            "Athlete",
            "Singer",
            "Police Officer",
            "Firefighter",
            "Construction Worker",
            "Nurse",
            "Programmer",
            "Engineer",
            "Accountant",
            "Farmer",
            "Driver",
            "Chef"
        ),
        color= Color.Blue,
        imageDrawableID = R.drawable.jobs_silhouette
    )
}