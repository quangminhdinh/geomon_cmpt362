package com.example.myapplication.battle

import com.example.myapplication.data.db.MoveEntity
import kotlin.random.Random

// Battle Move object
data class Move(
    val id: String,
    val name: String,
    val type1: String?,
    val type2: String? = null,
    val type3: String? = null,
    val baseDamage: Float,
    val category: String,
    val accuracy: Float,
    val priority: Int = 0,
    val ppMax: Int = 20,
    val healing: Float,
    val otherEffect: String? = null
) {

    companion object {

        fun fromEntity(e: MoveEntity): Move =
            Move(
                id = e.id,
                name = e.name,
                type1 = e.type1,
                type2 = e.type2,
                type3 = e.type3,
                baseDamage = e.baseDamage,
                category = e.category,
                accuracy = e.accuracy,
                priority = e.priority,
                ppMax = e.ppMax,
                healing = e.healing,
                otherEffect = e.otherEffect
            )

        fun initializeByName(name: String): Move {
            return when (name) {
                "Thunderbolt" -> Move(
                    id = "Thunderbolt",
                    name = "Thunderbolt",
                    type1 = "Electric",
                    baseDamage = 90f,
                    category = "special",
                    accuracy = 1.0f,
                    priority = 0,
                    ppMax = 15,
                    healing = 0f,
                    otherEffect = "MayParalyze"
                )

                "Tackle" -> Move(
                    id = "Tackle",
                    name = "Tackle",
                    type1 = "Normal",
                    baseDamage = 40f,
                    category = "physical",
                    accuracy = 0.95f,
                    priority = 0,
                    ppMax = 35,
                    healing = 0f,
                    otherEffect = null
                )

                else -> Move(
                    id = name,
                    name = name,
                    type1 = "Normal",
                    baseDamage = 20f,
                    category = "physical",
                    accuracy = 1.0f,
                    priority = 0,
                    ppMax = 20,
                    healing = 0f,
                    otherEffect = "DefaultMove"
                )
            }
        }
    }


// Accuracy Helper checker
    fun doesHit(): Boolean {

        val roll = Random.nextFloat()
        return roll <= accuracy
    }
}
