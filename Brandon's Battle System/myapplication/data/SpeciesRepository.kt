package geomon.myapplication.data

import geomon.myapplication.data.db.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

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
}
