
package com.example.myapplication.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
//Initializes and load all the moves from using the json helper methods to appdatabase.
//This allows for easy accesss
object Seeder {
    suspend fun run(context: Context, repo: SpeciesRepository) = withContext(Dispatchers.IO) {


        if (repo.countSpecies() > 0) {
            return@withContext
        }
        val speciesEntities = loadSpeciesEntitiesFromJson(context)



        val moveEntities = loadMoveEntitiesFromJson(context)


        repo.upsertAll(speciesEntities)
        repo.upsertMoves(moveEntities)
    }
}