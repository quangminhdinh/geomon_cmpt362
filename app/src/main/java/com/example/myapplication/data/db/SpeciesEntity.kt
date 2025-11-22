package com.example.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
//Monsters stats
@Entity(tableName = "species")
data class SpeciesEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type1: String,
    val type2: String?,
    val hp: Int,
    val atk: Int,
    val def: Int,
    val spa: Int,
    val spd: Int,
    val spe: Int
)
