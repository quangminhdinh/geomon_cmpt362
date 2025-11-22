// JsonExtraction.kt
package geomon.myapplication.data

import android.content.Context
import com.google.gson.Gson
import geomon.myapplication.R
import geomon.myapplication.data.db.MoveEntity
import geomon.myapplication.data.db.SpeciesEntity
//Stats representative for the Json file
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

    val move1: String? = null,
    val move2: String? = null,
    val move3: String? = null,
    val move4: String? = null,
    val sprite: String
)

data class Move(
    val id: String,
    val name: String,
    val type1: String,
    val type2: String,
    val type3: String,
    val baseDamage: Float,
    val category: String,
    val accuracy: Float,
    val priority: Int = 0,
    val ppMax: Int = 20,
    val healing: Float,
    val otherEffect: String? = null
)

// Load Moves from JSON to list
fun loadMovesRaw(context: Context): List<Move> {
    val json = context.resources.openRawResource(R.raw.moves)
        .bufferedReader().use { it.readText() }
    return Gson().fromJson(json, Array<Move>::class.java).toList()
}

// Utilizes loadMovesRaw to move to the appDatabase
fun loadMoveEntitiesFromJson(context: Context): List<MoveEntity> =
    loadMovesRaw(context).map { m ->
        MoveEntity(
            id = m.id,
            name = m.name,
            type1 = m.type1,
            type2 = m.type2?.ifEmpty { null },
            type3 = m.type3?.ifEmpty { null },
            baseDamage = m.baseDamage,
            category = m.category,
            accuracy = m.accuracy,
            priority = m.priority,
            ppMax = m.ppMax,
            healing = m.healing,
            otherEffect = m.otherEffect
        )
    }

// Load Monster from JSON to list
fun loadCreaturesRaw(context: Context): List<CreatureBase> {
    val json = context.resources.openRawResource(R.raw.stats)
        .bufferedReader().use { it.readText() }
    return Gson().fromJson(json, Array<CreatureBase>::class.java).toList()
}

// Load Monster from JSON to list
fun loadSpeciesEntitiesFromJson(context: Context): List<SpeciesEntity> =
    loadCreaturesRaw(context).map { b ->
        SpeciesEntity(
            id = b.id,
            name = b.name,
            type1 = b.types.getOrNull(0),
            type2 = b.types.getOrNull(1),
            type3 = b.types.getOrNull(1),
            hp  = b.baseStats.hp,
            atk = b.baseStats.atk,
            def = b.baseStats.def,
            spa = b.baseStats.spa,
            spd = b.baseStats.spd,
            spe = b.baseStats.spe,
            move1Id = b.move1,
            move2Id = b.move2,
            move3Id = b.move3,
            move4Id = b.move4
        )
    }

