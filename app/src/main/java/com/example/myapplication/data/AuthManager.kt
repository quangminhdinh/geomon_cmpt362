package com.example.myapplication.data

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

object AuthManager {
    private val auth: FirebaseAuth by lazy {
        FirebaseAuth.getInstance()
    }

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val userId: String?
        get() = auth.currentUser?.uid

    val isSignedIn: Boolean
        get() = auth.currentUser != null

    /**
     * Sign in anonymously - creates a new anonymous user if not already signed in
     */
    suspend fun signInAnonymously(): FirebaseUser? = suspendCoroutine { continuation ->
        // If already signed in, return current user
        if (auth.currentUser != null) {
            Log.d("AuthManager", "Already signed in: ${auth.currentUser?.uid}")
            continuation.resume(auth.currentUser)
            return@suspendCoroutine
        }

        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                Log.d("AuthManager", "Signed in anonymously: ${result.user?.uid}")
                continuation.resume(result.user)
            }
            .addOnFailureListener { e ->
                Log.e("AuthManager", "Anonymous sign in failed: ${e.message}")
                continuation.resume(null)
            }
    }

    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
        Log.d("AuthManager", "Signed out")
    }

    /**
     * Add auth state listener
     */
    fun addAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.addAuthStateListener(listener)
    }

    /**
     * Remove auth state listener
     */
    fun removeAuthStateListener(listener: FirebaseAuth.AuthStateListener) {
        auth.removeAuthStateListener(listener)
    }
}
