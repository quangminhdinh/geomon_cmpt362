// Monster.kt
package com.example.myapplication.battle
import java.io.Serializable
import android.content.Context
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.data.db.MoveEntity
//Monster Class Object
class Monster(
    val name: String,
    var level: Int = 1,

    val type1: String?,
    val type2: String? = null,
    val type3: String? = null,

    val spritePath: String,

    val maxHp: Float,
    var currentHp: Float,
    val attack: Float,
    val specialAttack: Float,
    val defense: Float,
    val specialDefense: Float,
    val speed: Float,

    // four battle moves
    var move1: Move? = null,
    var move2: Move? = null,
    var move3: Move? = null,
    var move4: Move? = null,

    // learnable moves
    val learnableMoves: List<Move> = emptyList(),

    var isFainted: Boolean = false
) : Serializable {

    companion object {
        // initializer a monster with the attributes from the appdatabase
        suspend fun initializeByName(context: Context, name: String): Monster {
            val db = AppDatabase.get(context)
            val dao = db.speciesDao()


            val species =
                dao.getByIdNow(name.lowercase())
                    ?: dao.getByIdNow(name)
                    ?: return createDefault(name)

            val move1Entity = species.move1Id?.let { dao.getMoveByIdNow(it) }
            val move2Entity = species.move2Id?.let { dao.getMoveByIdNow(it) }
            val move3Entity = species.move3Id?.let { dao.getMoveByIdNow(it) }
            val move4Entity = species.move4Id?.let { dao.getMoveByIdNow(it) }


            val move1 = move1Entity?.let { e: MoveEntity -> Move.fromEntity(e) }
            val move2 = move2Entity?.let { e: MoveEntity -> Move.fromEntity(e) }
            val move3 = move3Entity?.let { e: MoveEntity -> Move.fromEntity(e) }
            val move4 = move4Entity?.let { e: MoveEntity -> Move.fromEntity(e) }


            return Monster(
                name = species.name,
                level = 50,
                type1 = species.type1,
                type2 = species.type2,
                type3 = species.type3,
                spritePath = "sprites/${species.id}.png",
                maxHp = species.hp.toFloat(),
                currentHp = species.hp.toFloat(),
                attack = species.atk.toFloat(),
                specialAttack = species.spa.toFloat(),
                defense = species.def.toFloat(),
                specialDefense = species.spd.toFloat(),
                speed = species.spe.toFloat(),
                move1 = move1,
                move2 = move2,
                move3 = move3,
                move4 = move4,
                learnableMoves = listOfNotNull(move1, move2, move3, move4),
                isFainted = false
            )
        }
        //prevents errors when a monster back ups improperly
        private fun createDefault(name: String = "DefaultMon"): Monster {
            val tackle = Move.initializeByName("Tackle")

            return Monster(
                name = name,
                level = 1,
                type1 = "Normal",
                type2 = null,
                type3 = null,
                spritePath = "sprites/default.png",
                maxHp = 50f,
                currentHp = 50f,
                attack = 20f,
                specialAttack = 20f,
                defense = 20f,
                specialDefense = 20f,
                speed = 20f,
                move1 = tackle,
                move2 = null,
                move3 = null,
                move4 = null,
                learnableMoves = listOf(tackle),
                isFainted = false
            )
        }
    }

    fun takeDamage(damage: Float) {
        currentHp -= damage
        if (currentHp <= 0f) {
            currentHp = 0f
            isFainted = true
        }
    }
    // heal health, called by healing moves
    fun healDamage(amount: Float) {
        if (isFainted) return
        currentHp += amount
        if (currentHp > maxHp) {
            currentHp = maxHp
        }
    }

    fun isAlive(): Boolean = !isFainted
    // heal health, called by healing moves

}
