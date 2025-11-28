package com.example.myapplication.battle

import java.io.Serializable
import android.content.Context
import android.util.Log
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.data.db.MoveEntity
import com.example.myapplication.data.FirebaseManager
import com.google.firebase.database.DataSnapshot
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class Monster(
    val id: String = "",  // Firebase auto-generated ID
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
            val move1Entity = species.move1Id?.let { dao.getMoveByIdNow(it) }
            val move2Entity = species.move2Id?.let { dao.getMoveByIdNow(it) }
            val move3Entity = species.move3Id?.let { dao.getMoveByIdNow(it) }
            val move4Entity = species.move4Id?.let { dao.getMoveByIdNow(it) }

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
                move1 = move1Name,
                move2 = move2Name,
                move3 = move3Name,
                move4 = move4Name,
                learnableMoves = listOfNotNull(move1Name, move2Name, move3Name, move4Name),
                isFainted = false,
                latitude = latitude,
                longitude = longitude
            )

            // Upload to Firebase Realtime Database
            monsterRef.setValue(monster.toMap())
            Log.d("Monster", "Uploaded monster ${monster.name} with ID: $monsterId at ($latitude, $longitude)")

            return monster
        }

        fun fromSnapshot(snapshot: DataSnapshot): Monster? {
            return try {
                //  Now it reads String move names from Firebase
                val move1Name = snapshot.child("move1").getValue(String::class.java)
                val move2Name = snapshot.child("move2").getValue(String::class.java)
                val move3Name = snapshot.child("move3").getValue(String::class.java)
                val move4Name = snapshot.child("move4").getValue(String::class.java)

                Monster(
                    id = snapshot.key ?: "",
                    name = snapshot.child("name").getValue(String::class.java) ?: return null,
                    level = snapshot.child("level").getValue(Int::class.java) ?: 1,
                    type1 = snapshot.child("type1").getValue(String::class.java),
                    type2 = snapshot.child("type2").getValue(String::class.java),
                    type3 = snapshot.child("type3").getValue(String::class.java),
                    spritePath = snapshot.child("spritePath").getValue(String::class.java) ?: "",
                    maxHp = snapshot.child("maxHp").getValue(Float::class.java) ?: 50f,
                    currentHp = snapshot.child("currentHp").getValue(Float::class.java) ?: 50f,
                    attack = snapshot.child("attack").getValue(Float::class.java) ?: 20f,
                    specialAttack = snapshot.child("specialAttack").getValue(Float::class.java) ?: 20f,
                    defense = snapshot.child("defense").getValue(Float::class.java) ?: 20f,
                    specialDefense = snapshot.child("specialDefense").getValue(Float::class.java) ?: 20f,
                    speed = snapshot.child("speed").getValue(Float::class.java) ?: 20f,
                    move1 = move1Name,
                    move2 = move2Name,
                    move3 = move3Name,
                    move4 = move4Name,
                    learnableMoves = listOfNotNull(move1Name, move2Name, move3Name, move4Name),
                    isFainted = snapshot.child("isFainted").getValue(Boolean::class.java) ?: false,
                    latitude = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0,
                    longitude = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0,
                    ownerId = snapshot.child("ownerId").getValue(String::class.java)
                )
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

        // prevents errors when a monster back ups improperly
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
