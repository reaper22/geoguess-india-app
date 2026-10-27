package com.example.geoguessindia.firebase

import com.example.geoguessindia.model.ScoreEntry
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * SYLLABUS: Networking / third-party services (Cloud Firestore).
 * Like AuthHelper, Firestore calls are asynchronous and call us back on the main thread.
 */
object LeaderboardRepository {

    private val firestore get() = FirebaseFirestore.getInstance()

    /**
     * Save a finished game. The field names match the Firestore security rules
     * (uid must be the signed-in user, score must be an integer from 0 to 25000).
     *
     * Offline note: Firestore keeps the write on the phone and sends it when the internet
     * returns, but the success callback only fires once the server confirms it.
     */
    fun submitScore(
        score: Int,
        difficulty: String,
        rounds: Int,
        newPersonalBest: Boolean,
        onDone: (success: Boolean, error: String?) -> Unit
    ) {
        val uid = AuthHelper.currentUid()
        if (uid == null) {
            onDone(false, "Not signed in")
            return
        }
        val name = AuthHelper.currentName()
        val doc = hashMapOf<String, Any>(
            "uid" to uid,
            "playerName" to name,
            "score" to score,
            "difficulty" to difficulty,
            "rounds" to rounds,
            "timestamp" to FieldValue.serverTimestamp()
        )
        firestore.collection("scores").add(doc)
            .addOnSuccessListener {
                if (newPersonalBest) {
                    // merge = only touch these fields of users/{uid}
                    firestore.collection("users").document(uid)
                        .set(mapOf("displayName" to name, "bestScore" to score), SetOptions.merge())
                }
                onDone(true, null)
            }
            .addOnFailureListener { e -> onDone(false, e.localizedMessage ?: "Unknown error") }
    }


    fun getTop10(
        difficulty: String?,
        onResult: (entries: List<ScoreEntry>?, error: String?) -> Unit
    ) {
        firestore.collection("scores")
            .orderBy("score", Query.Direction.DESCENDING)
            .limit(100)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents
                    .mapNotNull { toEntry(it) }
                    .filter { difficulty == null || it.difficulty == difficulty }
                    .take(10)
                onResult(list, null)
            }
            .addOnFailureListener { e -> onResult(null, e.localizedMessage ?: "Unknown error") }
    }

    private fun toEntry(doc: DocumentSnapshot): ScoreEntry? {
        val score = doc.getLong("score")?.toInt() ?: return null
        val date = doc.getTimestamp("timestamp")?.toDate()
        return ScoreEntry(
            player = doc.getString("playerName") ?: "Player",
            score = score,
            difficulty = doc.getString("difficulty") ?: "",
            date = if (date != null)
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(date)
            else "just now"
        )
    }
}