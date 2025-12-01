
package com.example.myapplication.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


object Seeder {
    suspend fun run(context: Context, repo: SpeciesRepository) = withContext(Dispatchers.IO) {


//        if (repo.countSpecies() > 0) {
//            return@withContext
//        }
        val speciesEntities = loadSpeciesEntitiesFromJson(context)
        val moveEntities = loadMoveEntitiesFromJson(context)
        val itemEntities = loadItemEntitiesFromJson(context)


        repo.upsertAll(speciesEntities)
        repo.upsertMoves(moveEntities)
        repo.upsertItems(itemEntities)
    }

}