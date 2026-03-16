package com.example.nsddemo.data.repository

import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.domain.repository.WordRepository
import javax.inject.Inject

class InMemoryWordRepository
    @Inject
    constructor() : WordRepository {
        override fun getWordsForCategory(category: GameCategory): List<String> =
            when (category) {
                GameCategory.ANIMALS ->
                    listOf(
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
                        "Polar Bear",
                    )

                GameCategory.FOOD ->
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
                        "Fish",
                    )

                GameCategory.JOBS ->
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
                        "Chef",
                    )
            }

        private val semanticGroups =
            mapOf(
                // Animals
                "Lion" to listOf("Tiger", "Cheetah", "Bear", "Wolf", "Fox"),
                "Tiger" to listOf("Lion", "Cheetah", "Bear", "Wolf", "Fox"),
                "Cheetah" to listOf("Lion", "Tiger", "Bear", "Wolf", "Fox"),
                "Bear" to listOf("Polar Bear", "Wolf", "Lion", "Tiger", "Fox"),
                "Panda" to listOf("Bear", "Monkey", "Elephant", "Giraffe", "Zebra"),
                "Wolf" to listOf("Fox", "Dog", "Bear", "Lion", "Tiger"),
                "Dog" to listOf("Cat", "Wolf", "Fox", "Sheep", "Cow"),
                "Cat" to listOf("Dog", "Fox", "Chicken", "Pig", "Cow"),
                "Fox" to listOf("Wolf", "Dog", "Cat", "Lion", "Tiger"),
                "Chicken" to listOf("Pig", "Cow", "Sheep", "Horse", "Donkey"),
                "Sheep" to listOf("Cow", "Pig", "Chicken", "Horse", "Donkey"),
                "Cow" to listOf("Pig", "Chicken", "Sheep", "Horse", "Donkey"),
                "Pig" to listOf("Cow", "Chicken", "Sheep", "Horse", "Donkey"),
                "Horse" to listOf("Zebra", "Donkey", "Cow", "Sheep", "Pig"),
                "Zebra" to listOf("Horse", "Donkey", "Giraffe", "Elephant", "Lion"),
                "Donkey" to listOf("Horse", "Zebra", "Cow", "Sheep", "Pig"),
                "Giraffe" to listOf("Elephant", "Zebra", "Monkey", "Lion", "Tiger"),
                "Monkey" to listOf("Giraffe", "Elephant", "Panda", "Lion", "Tiger"),
                "Elephant" to listOf("Giraffe", "Zebra", "Lion", "Tiger", "Monkey"),
                "Fish" to listOf("Shark", "Whale", "Octopus", "Squid", "Walrus"),
                "Shark" to listOf("Whale", "Fish", "Octopus", "Squid", "Walrus"),
                "Octopus" to listOf("Squid", "Fish", "Shark", "Whale", "Walrus"),
                "Squid" to listOf("Octopus", "Fish", "Shark", "Whale", "Walrus"),
                "Whale" to listOf("Shark", "Fish", "Octopus", "Squid", "Walrus"),
                "Walrus" to listOf("Polar Bear", "Whale", "Shark", "Fish", "Octopus"),
                "Polar Bear" to listOf("Bear", "Walrus", "Wolf", "Lion", "Tiger"),
                // Food
                "Pizza" to listOf("Burger", "Pasta", "Bread", "Cheese", "Fries"),
                "Burger" to listOf("Pizza", "Fries", "Beef", "Cheese", "Chicken"),
                "Fries" to listOf("Burger", "Pizza", "Popcorn", "Rice", "Pasta"),
                "Ice Cream" to listOf("Cookie", "Cake", "Milk", "Juice", "Cheese"),
                "Cookie" to listOf("Cake", "Ice Cream", "Bread", "Milk", "Cheese"),
                "Bread" to listOf("Pizza", "Cookie", "Cake", "Rice", "Pasta"),
                "Milk" to listOf("Juice", "Ice Cream", "Eggs", "Cheese", "Cookie"),
                "Juice" to listOf("Milk", "Ice Cream", "Cookie", "Cake", "Bread"),
                "Pasta" to listOf("Pizza", "Rice", "Bread", "Cheese", "Fries"),
                "Rice" to listOf("Pasta", "Bread", "Fries", "Popcorn", "Chicken"),
                "Popcorn" to listOf("Fries", "Rice", "Cookie", "Cake", "Burger"),
                "Eggs" to listOf("Chicken", "Beef", "Fish", "Milk", "Cheese"),
                "Cheese" to listOf("Milk", "Pizza", "Pasta", "Burger", "Bread"),
                "Cake" to listOf("Cookie", "Ice Cream", "Bread", "Milk", "Cheese"),
                "Beef" to listOf("Chicken", "Fish", "Burger", "Pizza", "Fries"),
                "Chicken" to listOf("Beef", "Fish", "Eggs", "Burger", "Pizza"),
                "Fish" to listOf("Chicken", "Beef", "Shark", "Whale", "Squid"),
                // Jobs
                "Teacher" to listOf("Doctor", "Actor", "Singer", "Programmer", "Nurse"),
                "Doctor" to listOf("Nurse", "Teacher", "Programmer", "Engineer", "Accountant"),
                "Waiter" to listOf("Chef", "Actor", "Singer", "Driver", "Farmer"),
                "Actor" to listOf("Singer", "Waiter", "Chef", "Teacher", "Athlete"),
                "Athlete" to
                    listOf(
                        "Driver",
                        "Farmer",
                        "Police Officer",
                        "Firefighter",
                        "Construction Worker",
                    ),
                "Singer" to listOf("Actor", "Waiter", "Chef", "Teacher", "Athlete"),
                "Police Officer" to
                    listOf(
                        "Firefighter",
                        "Construction Worker",
                        "Driver",
                        "Athlete",
                        "Doctor",
                    ),
                "Firefighter" to
                    listOf(
                        "Police Officer",
                        "Construction Worker",
                        "Driver",
                        "Athlete",
                        "Nurse",
                    ),
                "Construction Worker" to
                    listOf(
                        "Engineer",
                        "Programmer",
                        "Driver",
                        "Farmer",
                        "Firefighter",
                    ),
                "Nurse" to listOf("Doctor", "Teacher", "Programmer", "Engineer", "Accountant"),
                "Programmer" to
                    listOf(
                        "Engineer",
                        "Accountant",
                        "Construction Worker",
                        "Teacher",
                        "Doctor",
                    ),
                "Engineer" to
                    listOf(
                        "Programmer",
                        "Construction Worker",
                        "Accountant",
                        "Teacher",
                        "Doctor",
                    ),
                "Accountant" to listOf("Programmer", "Engineer", "Teacher", "Doctor", "Nurse"),
                "Farmer" to
                    listOf(
                        "Driver",
                        "Chef",
                        "Construction Worker",
                        "Firefighter",
                        "Police Officer",
                    ),
                "Driver" to
                    listOf(
                        "Farmer",
                        "Construction Worker",
                        "Firefighter",
                        "Police Officer",
                        "Athlete",
                    ),
                "Chef" to listOf("Waiter", "Farmer", "Driver", "Actor", "Singer"),
            )

        override fun getSemanticWords(word: String): List<String> = semanticGroups[word] ?: emptyList()
    }
