package com.example.nsddemo.data.repository

import com.example.nsddemo.domain.model.GameCategory
import com.example.nsddemo.domain.repository.WordRepository

class InMemoryWordRepository : WordRepository {
    override fun getWordsForCategory(category: GameCategory): List<String> {
        return when (category) {
            GameCategory.ANIMALS -> listOf(
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
            )

            GameCategory.FOOD -> listOf(
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
            )

            GameCategory.JOBS -> listOf(
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
            )

        }
    }
}