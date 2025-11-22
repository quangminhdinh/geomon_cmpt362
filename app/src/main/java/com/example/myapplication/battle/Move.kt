package com.example.myapplication.battle

enum class Category { PHYSICAL, SPECIAL, STATUS }

data class Move(
    val id: String,
    val name: String,
    val power: Int?,
    val accuracy: Int?,
    val type: Element,
    val category: Category,
    val priority: Int = 0,
    val ppMax: Int = 20
)
