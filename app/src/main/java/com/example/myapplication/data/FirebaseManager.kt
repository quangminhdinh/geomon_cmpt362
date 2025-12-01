package com.example.myapplication.data

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseManager {
    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    val monstersRef: DatabaseReference
        get() = database.getReference("monsters")

    val usersRef: DatabaseReference
        get() = database.getReference("users")

    val duelRequestsRef: DatabaseReference
        get() = database.getReference("duelRequests")

    val battleStatesRef: DatabaseReference
        get() = database.getReference("battleStates")

    fun getReference(path: String): DatabaseReference {
        return database.getReference(path)
    }
}
