package com.example.myapplication.data.db

import androidx.room.Entity

@Entity(
    tableName = "species_moves",
    primaryKeys = ["speciesId", "moveId"]
)
data class SpeciesMoveCrossRef(
    val speciesId: String,
    val moveId: String
)
