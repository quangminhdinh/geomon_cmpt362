package com.example.myapplication.data

import com.example.myapplication.data.db.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class SpeciesRepository(private val dao: SpeciesDao) {


    fun allSpecies(): Flow<List<SpeciesEntity>> = dao.getAll()
    fun speciesById(id: String): Flow<SpeciesEntity?> = dao.getById(id)
    fun movesFor(speciesId: String): Flow<List<MoveEntity>> = dao.movesForSpecies(speciesId)


    fun clear() {
        CoroutineScope(IO).launch { dao.deleteAll() }
    }

    suspend fun seedMovesAndLinks(moves: List<MoveEntity>, links: List<SpeciesMoveCrossRef>) {
        dao.upsertMoves(moves)
        dao.insertLinks(links)
    }

    suspend fun countSpecies(): Int = dao.countSpecies()

    suspend fun upsertAll(species: List<SpeciesEntity>) = dao.upsertAll(species)


    suspend fun upsertMoves(moves: List<MoveEntity>) = dao.upsertMoves(moves)
    suspend fun insertLinks(links: List<SpeciesMoveCrossRef>) = dao.insertLinks(links)
}
