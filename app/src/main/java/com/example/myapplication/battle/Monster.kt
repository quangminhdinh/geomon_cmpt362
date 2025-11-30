package com.example.myapplication.battle

import java.io.Serializable
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.data.db.MoveEntity
import com.example.myapplication.data.FirebaseManager
import com.google.firebase.database.DataSnapshot
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.math.floor

class Monster(
    val id: String = "",  // Firebase auto-generated ID
    val name: String,
    var level: Int = 1,

    val type1: String?,
    val type2: String? = null,
    val type3: String? = null,

    val spritePath: String,


    var maxHp: Float,
    var currentHp: Float,
    var attack: Float,
    var specialAttack: Float,
    var defense: Float,
    var specialDefense: Float,
    var speed: Float,
    //Base state which are final
    val baseMaxHp: Float = maxHp,
    val baseAttack: Float = attack,
    val baseSpecialAttack: Float = specialAttack,
    val baseDefense: Float = defense,
    val baseSpecialDefense: Float = specialDefense,
    val baseSpeed: Float = speed,

    // four battle moves - strings
    var move1: String? = null,
    var move2: String? = null,
    var move3: String? = null,
    var move4: String? = null,

    // learnable moves
    val learnableMoves: List<String> = emptyList(),

    var isFainted: Boolean = false,

    // Location fields for map spawning
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,

    // Owner ID - null/empty means wild monster
    val ownerId: String? = null
) : Serializable {

    val isWild: Boolean
        get() = ownerId.isNullOrEmpty()

    companion object {

        /**
         * Level scaling inspired by Pokemon: Stat = floor(floor((2 * B) * (L / 100) + 5))

         */
        fun levelScaling(monster: Monster): Monster {

            fun scaled(base: Float, level: Int): Float {
                val I = 31
                val E = 252
                val value = (2f * (base)+ I + E) * (level.toFloat() / 100f) + 5f
                return floor(floor(value))
            }

            val L = monster.level

            val oldMax = if (monster.maxHp <= 0f) 1f else monster.maxHp
            val hpRatio = monster.currentHp / oldMax

            val newMaxHp = scaled(monster.baseMaxHp, L)
            monster.maxHp = newMaxHp
            monster.currentHp = (hpRatio * newMaxHp).coerceIn(0f, newMaxHp)

            monster.attack         = scaled(monster.baseAttack,         L)
            monster.specialAttack  = scaled(monster.baseSpecialAttack,  L)
            monster.defense        = scaled(monster.baseDefense,        L)
            monster.specialDefense = scaled(monster.baseSpecialDefense, L)
            monster.speed          = scaled(monster.baseSpeed,          L)

            return monster
        }

        // initializer a monster with the attributes from the appdatabase
        suspend fun initializeByName(
            context: Context,
            name: String,
            level: Int = 50,
            latitude: Double = 0.0,
            longitude: Double = 0.0
        ): Monster {
            val db = AppDatabase.get(context)
            val dao = db.speciesDao()

            val tempSpecies = dao.getByIdNow(name)

            if (tempSpecies == null) {
                Log.e("Monster", "Species not found in database: ${name.lowercase()}")
                // Still upload to Firebase even for default monster
                val monsterRef = FirebaseManager.monstersRef.push()
                val monsterId = monsterRef.key ?: ""
                val defaultMonster = createDefault(name, monsterId, latitude, longitude)
                monsterRef.setValue(defaultMonster.toMap())
                Log.d("Monster", "Uploaded default monster with ID: $monsterId")
                return defaultMonster
            }

            val species = tempSpecies

            // Get MoveEntities with names
            val move1Entity: MoveEntity? = species.move1Id?.let { dao.getMoveByIdNow(it) }
            val move2Entity: MoveEntity? = species.move2Id?.let { dao.getMoveByIdNow(it) }
            val move3Entity: MoveEntity? = species.move3Id?.let { dao.getMoveByIdNow(it) }
            val move4Entity: MoveEntity? = species.move4Id?.let { dao.getMoveByIdNow(it) }

            val move1Name = move1Entity?.name
            val move2Name = move2Entity?.name
            val move3Name = move3Entity?.name
            val move4Name = move4Entity?.name

            val monsterRef = FirebaseManager.monstersRef.push()
            val monsterId = monsterRef.key ?: ""

            val monster = Monster(
                id = monsterId,
                name = species.name,
                level = level,
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
                // base* default to the values above
                move1 = move1Name,
                move2 = move2Name,
                move3 = move3Name,
                move4 = move4Name,
                learnableMoves = listOfNotNull(move1Name, move2Name, move3Name, move4Name),
                isFainted = false,
                latitude = latitude,
                longitude = longitude
            )


            levelScaling(monster)

            monsterRef.setValue(monster.toMap())
            Log.d(
                "Monster",
                "Uploaded monster ${monster.name} with ID: $monsterId at ($latitude, $longitude)"
            )

            return monster
        }

        fun fromSnapshot(snapshot: DataSnapshot): Monster? {
            return try {
                //  Now it reads String move names from Firebase
                val move1Name = snapshot.child("move1").getValue(String::class.java)
                val move2Name = snapshot.child("move2").getValue(String::class.java)
                val move3Name = snapshot.child("move3").getValue(String::class.java)
                val move4Name = snapshot.child("move4").getValue(String::class.java)


                val maxHpSaved =
                    snapshot.child("maxHp").getValue(Float::class.java) ?: 50f
                val currentHpSaved =
                    snapshot.child("currentHp").getValue(Float::class.java) ?: maxHpSaved
                val attackSaved =
                    snapshot.child("attack").getValue(Float::class.java) ?: 20f
                val specialAttackSaved =
                    snapshot.child("specialAttack").getValue(Float::class.java) ?: 20f
                val defenseSaved =
                    snapshot.child("defense").getValue(Float::class.java) ?: 20f
                val specialDefenseSaved =
                    snapshot.child("specialDefense").getValue(Float::class.java) ?: 20f
                val speedSaved =
                    snapshot.child("speed").getValue(Float::class.java) ?: 20f


                val baseMaxHp =
                    snapshot.child("baseMaxHp").getValue(Float::class.java) ?: maxHpSaved
                val baseAttack =
                    snapshot.child("baseAttack").getValue(Float::class.java) ?: attackSaved
                val baseSpecialAttack =
                    snapshot.child("baseSpecialAttack").getValue(Float::class.java)
                        ?: specialAttackSaved
                val baseDefense =
                    snapshot.child("baseDefense").getValue(Float::class.java) ?: defenseSaved
                val baseSpecialDefense =
                    snapshot.child("baseSpecialDefense").getValue(Float::class.java)
                        ?: specialDefenseSaved
                val baseSpeed =
                    snapshot.child("baseSpeed").getValue(Float::class.java) ?: speedSaved

                val monster = Monster(
                    id = snapshot.key ?: "",
                    name = snapshot.child("name").getValue(String::class.java) ?: return null,
                    level = snapshot.child("level").getValue(Int::class.java) ?: 1,
                    type1 = snapshot.child("type1").getValue(String::class.java),
                    type2 = snapshot.child("type2").getValue(String::class.java),
                    type3 = snapshot.child("type3").getValue(String::class.java),
                    spritePath = snapshot.child("spritePath").getValue(String::class.java) ?: "",
                    maxHp = maxHpSaved,
                    currentHp = currentHpSaved,
                    attack = attackSaved,
                    specialAttack = specialAttackSaved,
                    defense = defenseSaved,
                    specialDefense = specialDefenseSaved,
                    speed = speedSaved,
                    baseMaxHp = baseMaxHp,
                    baseAttack = baseAttack,
                    baseSpecialAttack = baseSpecialAttack,
                    baseDefense = baseDefense,
                    baseSpecialDefense = baseSpecialDefense,
                    baseSpeed = baseSpeed,
                    move1 = move1Name,
                    move2 = move2Name,
                    move3 = move3Name,
                    move4 = move4Name,
                    learnableMoves = listOfNotNull(
                        move1Name,
                        move2Name,
                        move3Name,
                        move4Name
                    ),
                    isFainted = snapshot.child("isFainted")
                        .getValue(Boolean::class.java) ?: false,
                    latitude = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0,
                    longitude = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0,
                    ownerId = snapshot.child("ownerId").getValue(String::class.java)
                )

                levelScaling(monster)
            } catch (e: Exception) {
                Log.e("Monster", "Error parsing monster from snapshot: ${e.message}")
                null
            }
        }

        suspend fun fetchById(id: String): Monster? = suspendCoroutine { continuation ->
            FirebaseManager.monstersRef.child(id).get()
                .addOnSuccessListener { snapshot ->
                    val monster = fromSnapshot(snapshot)
                    continuation.resume(monster)
                }
                .addOnFailureListener { e ->
                    Log.e("Monster", "Error fetching monster by ID: ${e.message}")
                    continuation.resume(null)
                }
        }

        fun setOwner(monsterId: String, ownerId: String) {
            FirebaseManager.monstersRef.child(monsterId).child("ownerId").setValue(ownerId)
                .addOnSuccessListener {
                    Log.d("Monster", "Set owner $ownerId for monster $monsterId")
                }
        }

        // prevents errors when a monster backs up improperly
        private fun createDefault(
            name: String = "DefaultMon",
            id: String = "",
            latitude: Double = 0.0,
            longitude: Double = 0.0
        ): Monster {
            val tackleName = "Tackle"

            return Monster(
                id = id,
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

                move1 = tackleName,
                move2 = null,
                move3 = null,
                move4 = null,
                learnableMoves = listOf(tackleName),
                isFainted = false,
                latitude = latitude,
                longitude = longitude
            )
        }
    }


    fun takeDamage(damage: Float) {
        currentHp -= damage
        if (currentHp <= 0f) {
            currentHp = 0f
            isFainted = true
        }
        syncHpToFirebase()
    }
    //public method for leveling up a monster by scaling
     fun levelUp(amount: Int) {
        level += amount
        levelScaling(this)
        syncHpToFirebase()
    }

    private fun syncHpToFirebase() {
        if (id.isEmpty()) {
            Log.e("Monster", "Cannot sync HP: Monster has no ID")
            return
        }

        FirebaseManager.monstersRef.child(id).child("currentHp").setValue(currentHp)
            .addOnSuccessListener {
                Log.d("Monster", "Synced HP for monster $id: $currentHp")
            }
            .addOnFailureListener { e ->
                Log.e("Monster", "Failed to sync HP for monster $id: ${e.message}")
            }
    }
    // heal health, called by healing moves and bag items
    fun healDamage(amount: Float) {
        if (isFainted) {
            return
        }
        currentHp += amount
        if (currentHp > maxHp) {
            currentHp = maxHp
        }


        syncHpToFirebase()
    }
    fun isAlive(): Boolean = !isFainted
    // damage/heal/toMap unchanged except for moves part:
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "name" to name,
            "level" to level,
            "type1" to type1,
            "type2" to type2,
            "type3" to type3,
            "spritePath" to spritePath,
            "maxHp" to maxHp,
            "currentHp" to currentHp,
            "attack" to attack,
            "specialAttack" to specialAttack,
            "defense" to defense,
            "specialDefense" to specialDefense,
            "speed" to speed,

            // store base stats so we can rescale later
            "baseMaxHp" to baseMaxHp,
            "baseAttack" to baseAttack,
            "baseSpecialAttack" to baseSpecialAttack,
            "baseDefense" to baseDefense,
            "baseSpecialDefense" to baseSpecialDefense,
            "baseSpeed" to baseSpeed,

            "move1" to move1,
            "move2" to move2,
            "move3" to move3,
            "move4" to move4,
            "isFainted" to isFainted,
            "latitude" to latitude,
            "longitude" to longitude,
            "ownerId" to ownerId
        )
    }
}
