package com.example.myapplication.battle

import com.example.myapplication.data.db.MoveEntity

fun MoveEntity.toEngine(): Move =
    Move(
        id = id,
        name = name,
        power = power,
        accuracy = accuracy,
        type = Element.valueOf(type.uppercase()),
        category = Category.valueOf(category.uppercase()),
        priority = priority,
        ppMax = ppMax
    )
