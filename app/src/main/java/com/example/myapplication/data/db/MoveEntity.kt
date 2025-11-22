package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "moves")
data class MoveEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,
    val category: String,
    val power: Int?,
    val accuracy: Int?,
    val priority: Int = 0,
    val ppMax: Int = 20
)
