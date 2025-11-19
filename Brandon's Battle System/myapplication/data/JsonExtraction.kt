package geomon.myapplication.data

import android.content.Context
import com.google.gson.Gson
import geomon.myapplication.R

import geomon.myapplication.data.db.MoveEntity
import geomon.myapplication.data.db.SpeciesEntity
import geomon.myapplication.data.db.SpeciesMoveCrossRef

data class BaseStats(
    val hp: Int, val atk: Int, val def: Int,
    val spa: Int, val spd: Int, val spe: Int
)
data class Evolution(
    val to: String? = null,
    val from: String? = null,
    val method: String? = null
)
data class CreatureBase(
    val id: String,
    val name: String,
    val types: List<String>,
    val baseStats: BaseStats,
    val growthRate: String,
    val baseExp: Int,
    val catchRate: Int,
    val evolution: Evolution?,
    val moves: List<String>,
    val sprite: String
)
data class Move(
    val id: String,
    val name: String,
    val type: String,
    val accuracy: String
)

fun loadMovesFromJson(context: Context): List<Move> {
    val json = context.resources.openRawResource(R.raw.moves)
        .bufferedReader().use { it.readText() }
    return Gson().fromJson(json, Array<Move>::class.java).toList()
}

fun mapMovesToEntities(moves: List<Move>): List<MoveEntity> =
    moves.map { m ->
        MoveEntity(
            id = m.id,
            name = m.name,
            type = m.type,
            category = "Unknown",
            power = null,
            accuracy = m.accuracy.toIntOrNull(),
            priority = 0,
            ppMax = 20
        )
    }


fun loadCreaturesRaw(context: Context): List<CreatureBase> {
    val json = context.resources.openRawResource(R.raw.stats)
        .bufferedReader().use { it.readText() }
    return Gson().fromJson(json, Array<CreatureBase>::class.java).toList()
}

fun loadSpeciesEntitiesFromJson(context: Context): List<SpeciesEntity> =
    loadCreaturesRaw(context).map { b ->
        SpeciesEntity(
            id = b.id,
            name = b.name,
            type1 = b.types.getOrNull(0) ?: "Unknown",
            type2 = b.types.getOrNull(1),
            hp  = b.baseStats.hp,
            atk = b.baseStats.atk,
            def = b.baseStats.def,
            spa = b.baseStats.spa,
            spd = b.baseStats.spd,
            spe = b.baseStats.spe
        )
    }
