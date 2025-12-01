package com.group14.geomon.data

import com.group14.geomon.data.db.MoveEntity
import com.group14.geomon.data.db.SpeciesDao
import com.group14.geomon.data.db.SpeciesEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import com.group14.geomon.data.db.ItemEntity

class SpeciesRepository(private val dao: SpeciesDao) {

    fun allSpecies(): Flow<List<SpeciesEntity>> = dao.getAll()
    fun speciesById(id: String): Flow<SpeciesEntity?> = dao.getById(id)

    fun clear() {
        CoroutineScope(IO).launch { dao.deleteAll() }
    }

    suspend fun countSpecies(): Int = dao.countSpecies()

    suspend fun upsertAll(species: List<SpeciesEntity>) = dao.upsertAll(species)

    suspend fun upsertMoves(moves: List<MoveEntity>) = dao.upsertMoves(moves)
    fun moveById(id: String): Flow<MoveEntity?> = dao.moveById(id)


    suspend fun upsertItems(items: List<ItemEntity>) = dao.upsertItems(items)

}