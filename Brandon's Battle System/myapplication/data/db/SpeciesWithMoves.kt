package geomon.myapplication.data.db

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class SpeciesWithMoves(
    @Embedded val species: SpeciesEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SpeciesMoveCrossRef::class,
            parentColumn = "speciesId",
            entityColumn = "moveId"
        )
    )
    val moves: List<MoveEntity>
)
