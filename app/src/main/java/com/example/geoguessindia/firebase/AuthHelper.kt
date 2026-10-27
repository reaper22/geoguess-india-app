package com.example.geoguessindia.firebase

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore


object AuthHelper {

    private val auth: FirebaseAuth get() = FirebaseAuth.getInstance()

    fun isLoggedIn(): Boolean = auth.currentUser != null

    fun currentUid(): String? = auth.currentUser?.uid

    fun currentName(): String {
        val user = auth.currentUser
        return user?.displayName?.takeIf { it.isNotBlank() } ?: user?.email ?: "Player"
    }

    fun logout() = auth.signOut()


    fun signUp(
        name: String,
        email: String,
        password: String,
        onDone: (success: Boolean, error: String?) -> Unit
    ) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnSuccessListener { result ->
                val user = result.user
                if (user == null) {
                    onDone(false, "Could not create the account. Try again.")
                    return@addOnSuccessListener
                }
                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build()
                user.updateProfile(profile).addOnCompleteListener {
                    // Profile document for the online leaderboard (failure here is not fatal)
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.uid)
                        .set(mapOf("displayName" to name, "bestScore" to 0))
                    onDone(true, null)
                }
            }
            .addOnFailureListener { e -> onDone(false, friendlyError(e)) }
    }

    fun login(
        email: String,
        password: String,
        onDone: (success: Boolean, error: String?) -> Unit
    ) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnSuccessListener { onDone(true, null) }
            .addOnFailureListener { e -> onDone(false, friendlyError(e)) }
    }
    fun sendPasswordReset(email: String, onDone: (success: Boolean, error: String?) -> Unit) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener { onDone(true, null) }
            .addOnFailureListener { e -> onDone(false, friendlyError(e)) }
    }

       private fun friendlyError(e: Exception): String = when (e) {
        is FirebaseAuthWeakPasswordException -> e.reason ?: "Password does not meet the requirements."
        is FirebaseAuthUserCollisionException -> "An account with this email already exists."
        is FirebaseAuthInvalidUserException -> "No account found for this email."
        is FirebaseAuthInvalidCredentialsException -> "Wrong email or password."
        is FirebaseNetworkException -> "No internet connection."

        else -> e.localizedMessage ?: "Something went wrong."
    }
}