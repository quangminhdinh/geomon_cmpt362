package com.example.myapplication.data

import android.content.Context
import com.example.myapplication.data.db.SpeciesMoveCrossRef
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object Seeder {
    suspend fun run(context: Context, repo: SpeciesRepository) = withContext(Dispatchers.IO) {

        if (repo.countSpecies() > 0) return@withContext


        val speciesEntities = loadSpeciesEntitiesFromJson(context)

        val moveDtos = loadMovesFromJson(context)
        val moveEntities = mapMovesToEntities(moveDtos)

        val bases = loadCreaturesRaw(context)
        val links = bases.flatMap { base ->
            base.moves.map { moveId ->
                SpeciesMoveCrossRef(speciesId = base.id, moveId = moveId)
            }
        }

        repo.upsertAll(speciesEntities)
        repo.upsertMoves(moveEntities)
        repo.insertLinks(links)
    }
}
