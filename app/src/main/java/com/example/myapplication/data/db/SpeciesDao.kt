package com.example.myapplication.data.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SpeciesDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMoves(items: List<MoveEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(list: List<SpeciesEntity>)

    @Query("SELECT * FROM species WHERE id = :id LIMIT 1")
    fun getById(id: String): Flow<SpeciesEntity?>

    @Query("SELECT * FROM species ORDER BY id")
    fun getAll(): Flow<List<SpeciesEntity>>

    @Query("DELETE FROM species")
    suspend fun deleteAll()

    @Query("SELECT * FROM moves WHERE id = :moveId LIMIT 1")
    fun moveById(moveId: String): Flow<MoveEntity?>

    @Query("SELECT COUNT(*) FROM species")
    suspend fun countSpecies(): Int
    @Query("SELECT * FROM species WHERE id = :id LIMIT 1")
    suspend fun getByIdNow(id: String): SpeciesEntity?

    @Query("SELECT * FROM moves WHERE id = :id LIMIT 1")
    suspend fun getMoveByIdNow(id: String): MoveEntity?
}

