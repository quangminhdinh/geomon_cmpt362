package com.example.myapplication.data

import android.util.Log
import com.google.firebase.database.DataSnapshot
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class BattleState(
    val id: String = "",
    val player1Id: String = "",
    val player2Id: String = "",
    val player1MonsterId: String = "",
    val player2MonsterId: String = "",
    val currentTurn: String = "", // player1Id or player2Id
    val player1Move: String? = null,
    val player2Move: String? = null,
    val player1Hp: Float = 0f,
    val player2Hp: Float = 0f,
    val lastMove: String? = null,
    val lastMoveUser: String? = null,
    val status: String = "waiting", // waiting, in_progress, finished
    val winnerId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "player1Id" to player1Id,
            "player2Id" to player2Id,
            "player1MonsterId" to player1MonsterId,
            "player2MonsterId" to player2MonsterId,
            "currentTurn" to currentTurn,
            "player1Move" to player1Move,
            "player2Move" to player2Move,
            "player1Hp" to player1Hp,
            "player2Hp" to player2Hp,
            "lastMove" to lastMove,
            "lastMoveUser" to lastMoveUser,
            "status" to status,
            "winnerId" to winnerId,
            "timestamp" to timestamp
        )
    }

    companion object {
        fun fromSnapshot(snapshot: DataSnapshot): BattleState? {
            return try {
                BattleState(
                    id = snapshot.key ?: "",
                    player1Id = snapshot.child("player1Id").getValue(String::class.java) ?: "",
                    player2Id = snapshot.child("player2Id").getValue(String::class.java) ?: "",
                    player1MonsterId = snapshot.child("player1MonsterId").getValue(String::class.java) ?: "",
                    player2MonsterId = snapshot.child("player2MonsterId").getValue(String::class.java) ?: "",
                    currentTurn = snapshot.child("currentTurn").getValue(String::class.java) ?: "",
                    player1Move = snapshot.child("player1Move").getValue(String::class.java),
                    player2Move = snapshot.child("player2Move").getValue(String::class.java),
                    player1Hp = snapshot.child("player1Hp").getValue(Float::class.java) ?: 0f,
                    player2Hp = snapshot.child("player2Hp").getValue(Float::class.java) ?: 0f,
                    lastMove = snapshot.child("lastMove").getValue(String::class.java),
                    lastMoveUser = snapshot.child("lastMoveUser").getValue(String::class.java),
                    status = snapshot.child("status").getValue(String::class.java) ?: "waiting",
                    winnerId = snapshot.child("winnerId").getValue(String::class.java),
                    timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                )
            } catch (e: Exception) {
                Log.e("BattleState", "Error parsing battle state: ${e.message}")
                null
            }
        }

        suspend fun create(
            player1Id: String,
            player2Id: String,
            player1MonsterId: String,
            player2MonsterId: String,
            player1Hp: Float,
            player2Hp: Float
        ): BattleState? = suspendCoroutine { continuation ->
            val battleRef = FirebaseManager.battleStatesRef.push()
            val battleId = battleRef.key ?: run {
                Log.e("BattleState", "Failed to generate battle ID")
                continuation.resume(null)
                return@suspendCoroutine
            }

            val battleState = BattleState(
                id = battleId,
                player1Id = player1Id,
                player2Id = player2Id,
                player1MonsterId = player1MonsterId,
                player2MonsterId = player2MonsterId,
                currentTurn = player2Id,
                player1Hp = player1Hp,
                player2Hp = player2Hp,
                status = "in_progress"
            )

            battleRef.setValue(battleState.toMap())
                .addOnSuccessListener {
                    Log.d("BattleState", "Battle state created: $battleId")
                    continuation.resume(battleState)
                }
                .addOnFailureListener { e ->
                    Log.e("BattleState", "Failed to create battle state: ${e.message}")
                    continuation.resume(null)
                }
        }

        fun updateMove(battleId: String, playerId: String, moveName: String) {
            FirebaseManager.battleStatesRef.child(battleId).get()
                .addOnSuccessListener { snapshot ->
                    val state = fromSnapshot(snapshot) ?: return@addOnSuccessListener

                    val updates = mutableMapOf<String, Any>()
                    if (playerId == state.player1Id) {
                        updates["player1Move"] = moveName
                    } else {
                        updates["player2Move"] = moveName
                    }

                    FirebaseManager.battleStatesRef.child(battleId)
                        .updateChildren(updates)
                        .addOnSuccessListener {
                            Log.d("BattleState", "Move updated for player $playerId")
                        }
                }
        }

        fun updateHpAndNextTurn(battleId: String, player1Hp: Float, player2Hp: Float, nextPlayerId: String, lastMove: String, lastMoveUser: String) {
            FirebaseManager.battleStatesRef.child(battleId)
                .updateChildren(
                    mapOf(
                        "player1Hp" to player1Hp,
                        "player2Hp" to player2Hp,
                        "player1Move" to null,
                        "player2Move" to null,
                        "currentTurn" to nextPlayerId,
                        "lastMove" to lastMove,
                        "lastMoveUser" to lastMoveUser,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
                .addOnSuccessListener {
                    Log.d("BattleState", "HP updated and turn switched to $nextPlayerId")
                }
        }

        fun finishBattle(battleId: String, winnerId: String?, player1Hp: Float, player2Hp: Float, lastMove: String?, lastMoveUser: String?) {
            FirebaseManager.battleStatesRef.child(battleId)
                .updateChildren(
                    mapOf(
                        "status" to "finished",
                        "winnerId" to winnerId,
                        "player1Hp" to player1Hp,
                        "player2Hp" to player2Hp,
                        "lastMove" to lastMove,
                        "lastMoveUser" to lastMoveUser,
                        "timestamp" to System.currentTimeMillis()
                    )
                )
                .addOnSuccessListener {
                    Log.d("BattleState", "Battle finished, winner: $winnerId")
                }
        }

        fun delete(battleId: String) {
            FirebaseManager.battleStatesRef.child(battleId).removeValue()
                .addOnSuccessListener {
                    Log.d("BattleState", "Battle state deleted: $battleId")
                }
        }
    }
}
