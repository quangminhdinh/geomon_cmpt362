package com.example.myapplication.data

import android.util.Log
import com.google.firebase.database.DataSnapshot
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class User(
    val id: String = "",
    val displayName: String = "",
    val monsterIds: List<String> = emptyList(), // List of owned monster IDs
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val lastActive: Long = System.currentTimeMillis(),
    val activeMonsterIndex: Int = 0,
    val bag: Map<String, Int> = emptyMap()// bag containing items
) {
    // Get the first monster ID (used for battles)
    val firstMonsterId: String?
        get() = monsterIds.getOrNull(activeMonsterIndex)

    fun toMap(): Map<String, Any?> {
        return mapOf(
            "displayName" to displayName,
            "monsterIds" to monsterIds,
            "latitude" to latitude,
            "longitude" to longitude,
            "lastActive" to lastActive,
            "activeMonsterIndex" to activeMonsterIndex,
            "bag" to bag
        )
    }

    companion object {
        fun fromSnapshot(snapshot: DataSnapshot): User? {
            return try {
                // Parse monsterIds list
                val monsterIdsList = mutableListOf<String>()
                snapshot.child("monsterIds").children.forEach { child ->
                    child.getValue(String::class.java)?.let { monsterIdsList.add(it) }
                }

                val bagMap = mutableMapOf<String, Int>()
                snapshot.child("bag").children.forEach { child ->
                    val itemName = child.key ?: return@forEach
                    val count = child.getValue(Int::class.java) ?: 0
                    if (count > 0) bagMap[itemName] = count
                }

                val indexMonster =
                    snapshot.child("activeMonsterIndex").getValue(Int::class.java) ?: 0
                Log.d("GeoMon", "activeMonsterIndex from Firebase = $indexMonster")

                User(
                    id = snapshot.key ?: "",
                    displayName = snapshot.child("displayName").getValue(String::class.java) ?: "",
                    monsterIds = monsterIdsList,
                    latitude = snapshot.child("latitude").getValue(Double::class.java) ?: 0.0,
                    longitude = snapshot.child("longitude").getValue(Double::class.java) ?: 0.0,
                    lastActive = snapshot.child("lastActive").getValue(Long::class.java) ?: 0L,
                    activeMonsterIndex = indexMonster,
                    bag = bagMap                         // <-- NEW
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
            monsterIds: List<String> = emptyList(),
            latitude: Double = 0.0,
            longitude: Double = 0.0
        ): User = suspendCoroutine { continuation ->
            val user = User(
                id = userId,
                displayName = displayName,
                monsterIds = monsterIds,
                latitude = latitude,
                longitude = longitude,
                lastActive = System.currentTimeMillis(),
                activeMonsterIndex = 0
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

        fun updateLastActive(userId: String) {
            FirebaseManager.usersRef.child(userId).updateChildren(
                mapOf("lastActive" to System.currentTimeMillis())
            )
        }

        fun addMonster(userId: String, monsterId: String) {
            FirebaseManager.usersRef.child(userId).child("monsterIds").get()
                .addOnSuccessListener { snapshot ->
                    val currentIds = mutableListOf<String>()
                    snapshot.children.forEach { child ->
                        child.getValue(String::class.java)?.let { currentIds.add(it) }
                    }
                    currentIds.add(monsterId)
                    FirebaseManager.usersRef.child(userId).updateChildren(
                        mapOf(
                            "monsterIds" to currentIds,
                            "lastActive" to System.currentTimeMillis()
                        )
                    )
                    Log.d("User", "Added monster $monsterId to user $userId")
                }
        }

        fun setUserActiveMonster(userId: String, index: Int) {
            val updates = mapOf(
                "activeMonsterIndex" to index,
                "lastActive" to System.currentTimeMillis()
            )

            FirebaseManager.usersRef.child(userId)
                .updateChildren(updates)
                .addOnSuccessListener {
                    Log.d("User", "activeMonsterIndex updated: $index, user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e("User", "activeMonsterIndex failed updated: ${e.message}")
                }
        }

        fun removeMonster(userId: String, monsterId: String) {
            FirebaseManager.usersRef.child(userId).child("monsterIds").get()
                .addOnSuccessListener { snapshot ->
                    val currentIds = mutableListOf<String>()
                    snapshot.children.forEach { child ->
                        child.getValue(String::class.java)?.let { currentIds.add(it) }
                    }
                    currentIds.remove(monsterId)
                    FirebaseManager.usersRef.child(userId).updateChildren(
                        mapOf(
                            "monsterIds" to currentIds,
                            "lastActive" to System.currentTimeMillis()
                        )
                    )
                    Log.d("User", "Removed monster $monsterId from user $userId")
                }
        }

        //refreshes from firebase so that the bag displays the correct item numbers
        fun updateBag(userId: String, bag: Map<String, Int>) {
            val updates = mapOf(
                "bag" to bag,
                "lastActive" to System.currentTimeMillis()
            )

            FirebaseManager.usersRef.child(userId)
                .updateChildren(updates)
                .addOnSuccessListener {
                    Log.d("User", "Bag updated for user: $userId")
                }
                .addOnFailureListener { e ->
                    Log.e("User", "Failed to update bag: ${e.message}")
                }
        }

        // helper method to add items to the bag mapping
        fun addItem(userId: String, itemName: String, amount: Int) {
            if (amount <= 0) {
                return
            }
            FirebaseManager.usersRef.child(userId).child("bag").get()
                .addOnSuccessListener { snapshot ->
                    val bagMap = mutableMapOf<String, Int>()
                    snapshot.children.forEach { child ->
                        val name = child.key ?: return@forEach
                        val count = child.getValue(Int::class.java) ?: 0
                        if (count > 0) bagMap[name] = count
                    }
                    val current = bagMap[itemName] ?: 0
                    bagMap[itemName] = current + amount
                    updateBag(userId, bagMap)
                }
                .addOnFailureListener { e ->
                    Log.e("User", "Error when reading bag for addItem: ${e.message}")
                }
        }

        //Remove items from the bag
        fun removeByItemName(userId: String, itemName: String, amount: Int) {
            if (amount <= 0){
                return
            }
            FirebaseManager.usersRef.child(userId).child("bag").get()
                .addOnSuccessListener { snapshot ->
                    val bagMap = mutableMapOf<String, Int>()
                    snapshot.children.forEach { child ->
                        val name = child.key ?: return@forEach
                        val count = child.getValue(Int::class.java) ?: 0
                        if (count > 0) bagMap[name] = count
                    }

                    val current = bagMap[itemName] ?: 0
                    val newCount = (current - amount).coerceAtLeast(0)
                    if (newCount <= 0) {
                        bagMap.remove(itemName)
                    } else {
                        bagMap[itemName] = newCount
                    }

                    updateBag(userId, bagMap)
                }
                .addOnFailureListener { e ->
                    Log.e("User", "Error when reading bag for removeByItemName: ${e.message}")
                }
        }


    }
}
