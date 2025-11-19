package geomon.myapplication.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeciesDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSpeciesMoves(cross: List<SpeciesMoveCrossRef>)
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMoves(items: List<MoveEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLinks(items: List<SpeciesMoveCrossRef>)


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<SpeciesEntity>)


    @Query("SELECT * FROM species WHERE id = :id LIMIT 1")
    fun getById(id: String): Flow<SpeciesEntity?>

    @Query("SELECT * FROM species ORDER BY id")
    fun getAll(): Flow<List<SpeciesEntity>>

    @Query("DELETE FROM species")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT * FROM species WHERE id = :speciesId LIMIT 1")
    fun speciesWithMoves(speciesId: String): Flow<SpeciesWithMoves?>

    @Query("""
        SELECT m.* 
        FROM moves m 
        INNER JOIN species_moves sm ON sm.moveId = m.id 
        WHERE sm.speciesId = :speciesId 
        ORDER BY m.name
    """)
    fun movesForSpecies(speciesId: String): Flow<List<MoveEntity>>

    @Query("SELECT * FROM moves WHERE id = :moveId LIMIT 1")
    fun moveById(moveId: String): Flow<MoveEntity?>


    @Query("SELECT COUNT(*) FROM species")
    suspend fun countSpecies(): Int
}
