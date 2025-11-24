package com.example.myapplication.data

import android.util.Log
import com.google.firebase.database.DataSnapshot
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class User(
    val id: String = "",
    val displayName: String = "",
    val playerMonsterId: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastActive: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any?> {
        return mapOf(
            "displayName" to displayName,
            "playerMonsterId" to playerMonsterId,
            "latitude" to latitude,
            "longitude" to longitude,
            "lastActive" to lastActive
        )
    }

    companion object {
        fun fromSnapshot(snapshot: DataSnapshot): User? {
            return try {
                User(
                    id = snapshot.key ?: "",
                    displayName = snapshot.child("displayName").getValue(String::class.java) ?: "",
                    playerMonsterId = snapshot.child("playerMonsterId").getValue(String::class.java) ?: "",
                    latitude = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0,
                    longitude = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0,
                    lastActive = snapshot.child("lastActive").getValue(Long::class.java) ?: 0L
                )
            } catch (e: Exception) {
                Log.e("User", "Error parsing user from snapshot: ${e.message}")
                null
            }
        }

        suspend fun fetchById(userId: String): User? = suspendCoroutine { continuation ->
            FirebaseManager.usersRef.child(userId).get()
                .addOnSuccessListener { snapshot ->
                    val user = fromSnapshot(snapshot)
                    continuation.resume(user)
                }
                .addOnFailureListener { e ->
                    Log.e("User", "Error fetching user: ${e.message}")
                    continuation.resume(null)
                }
        }

        suspend fun createOrUpdate(
            userId: String,
            displayName: String = "Player",
            playerMonsterId: String = "",
            latitude: Double = 0.0,
            longitude: Double = 0.0
        ): User = suspendCoroutine { continuation ->
            val user = User(
                id = userId,
                displayName = displayName,
                playerMonsterId = playerMonsterId,
                latitude = latitude,
                longitude = longitude,
                lastActive = System.currentTimeMillis()
            )

            FirebaseManager.usersRef.child(userId).setValue(user.toMap())
                .addOnSuccessListener {
                    Log.d("User", "User saved: $userId")
                    continuation.resume(user)
                }
                .addOnFailureListener { e ->
                    Log.e("User", "Failed to save user: ${e.message}")
                    continuation.resume(user)
                }
        }

        fun updateLocation(userId: String, latitude: Double, longitude: Double) {
            FirebaseManager.usersRef.child(userId).updateChildren(
                mapOf(
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "lastActive" to System.currentTimeMillis()
                )
            )
        }

        fun updatePlayerMonsterId(userId: String, monsterId: String) {
            FirebaseManager.usersRef.child(userId).updateChildren(
                mapOf(
                    "playerMonsterId" to monsterId,
                    "lastActive" to System.currentTimeMillis()
                )
            )
        }
    }
}
