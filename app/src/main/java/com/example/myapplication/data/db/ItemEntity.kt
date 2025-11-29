package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
//Item Entity
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val healing: Int,
    val sideAffect: Int,
    val effect: String,
    val displayDescription: String,
    val iconRoot: String,
    val colorDisplay: String
)