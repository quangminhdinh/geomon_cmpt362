package com.example.myapplication.data

import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object FirebaseManager {
    private val database: FirebaseDatabase by lazy {
        FirebaseDatabase.getInstance()
    }

    val monstersRef: DatabaseReference
        get() = database.getReference("monsters")

    val battlesRef: DatabaseReference
        get() = database.getReference("battles")

    val usersRef: DatabaseReference
        get() = database.getReference("users")

    fun getReference(path: String): DatabaseReference {
        return database.getReference(path)
    }
}
