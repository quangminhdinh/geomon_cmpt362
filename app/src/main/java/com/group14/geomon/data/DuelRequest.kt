package com.group14.geomon.data

import android.util.Log
import com.google.firebase.database.DataSnapshot
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class DuelRequest(
    val id: String = "",
    val challengerId: String = "",
    val challengerName: String = "",
    val targetId: String = "",
    val targetName: String = "",
    val status: String = "pending", // pending, accepted, rejected, expired, battle_ready
    val battleId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "challengerId" to challengerId,
            "challengerName" to challengerName,
            "targetId" to targetId,
            "targetName" to targetName,
            "status" to status,
            "battleId" to battleId,
            "timestamp" to timestamp
        )
    }

    companion object {
        fun fromSnapshot(snapshot: DataSnapshot): DuelRequest? {
            return try {
                DuelRequest(
                    id = snapshot.key ?: "",
                    challengerId = snapshot.child("challengerId").getValue(String::class.java) ?: "",
                    challengerName = snapshot.child("challengerName").getValue(String::class.java) ?: "",
                    targetId = snapshot.child("targetId").getValue(String::class.java) ?: "",
                    targetName = snapshot.child("targetName").getValue(String::class.java) ?: "",
                    status = snapshot.child("status").getValue(String::class.java) ?: "pending",
                    battleId = snapshot.child("battleId").getValue(String::class.java),
                    timestamp = snapshot.child("timestamp").getValue(Long::class.java) ?: 0L
                )
            } catch (e: Exception) {
                Log.e("DuelRequest", "Error parsing duel request: ${e.message}")
                null
            }
        }

        suspend fun create(
            challengerId: String,
            challengerName: String,
            targetId: String,
            targetName: String
        ): DuelRequest? = suspendCoroutine { continuation ->
            val requestRef = FirebaseManager.duelRequestsRef.push()
            val requestId = requestRef.key ?: run {
                Log.e("DuelRequest", "Failed to generate request ID")
                continuation.resume(null)
                return@suspendCoroutine
            }

            val duelRequest = DuelRequest(
                id = requestId,
                challengerId = challengerId,
                challengerName = challengerName,
                targetId = targetId,
                targetName = targetName,
                status = "pending",
                timestamp = System.currentTimeMillis()
            )

            requestRef.setValue(duelRequest.toMap())
                .addOnSuccessListener {
                    Log.d("DuelRequest", "Duel request created: $requestId")
                    continuation.resume(duelRequest)
                }
                .addOnFailureListener { e ->
                    Log.e("DuelRequest", "Failed to create duel request: ${e.message}")
                    continuation.resume(null)
                }
        }

        fun updateStatus(requestId: String, newStatus: String) {
            FirebaseManager.duelRequestsRef
                .child(requestId)
                .updateChildren(mapOf("status" to newStatus))
                .addOnSuccessListener {
                    Log.d("DuelRequest", "Duel request $requestId updated to $newStatus")
                }
                .addOnFailureListener { e ->
                    Log.e("DuelRequest", "Failed to update duel request: ${e.message}")
                }
        }

        fun setBattleReady(requestId: String, battleId: String) {
            FirebaseManager.duelRequestsRef
                .child(requestId)
                .updateChildren(mapOf(
                    "status" to "battle_ready",
                    "battleId" to battleId
                ))
                .addOnSuccessListener {
                    Log.d("DuelRequest", "Duel request $requestId set to battle_ready with battle $battleId")
                }
                .addOnFailureListener { e ->
                    Log.e("DuelRequest", "Failed to set battle ready: ${e.message}")
                }
        }

        fun delete(requestId: String) {
            FirebaseManager.duelRequestsRef
                .child(requestId)
                .removeValue()
                .addOnSuccessListener {
                    Log.d("DuelRequest", "Duel request $requestId deleted")
                }
                .addOnFailureListener { e ->
                    Log.e("DuelRequest", "Failed to delete duel request: ${e.message}")
                }
        }
    }
}
