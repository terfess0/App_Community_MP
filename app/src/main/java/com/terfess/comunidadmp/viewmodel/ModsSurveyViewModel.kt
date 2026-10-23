package com.terfess.comunidadmp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

class ModsSurveyViewModel : ViewModel() {
    private val db = FirebaseFirestore.getInstance()

    val hasUserVoted = MutableLiveData<Boolean>()
    val voteSubmitted = MutableLiveData<Boolean>()
    val surveyStats = MutableLiveData<Pair<Int, Int>>() // (votosSi, votosTotal)
    val errorMessage = MutableLiveData<String>()

    fun checkUserVote(userEmail: String) {
        if (userEmail.isBlank()) {
            hasUserVoted.postValue(false)
            return
        }

        db.collection("mods_survey_votes")
            .whereEqualTo("userEmail", userEmail)
            .get()
            .addOnSuccessListener { query ->
                hasUserVoted.postValue(!query.isEmpty)
                loadSurveyStats()
            }
            .addOnFailureListener { e ->
                errorMessage.postValue(e.message ?: "Error al verificar voto")
            }
    }

    fun submitVote(voteData: HashMap<String, Any?>) {
        db.collection("mods_survey_votes")
            .add(voteData)
            .addOnSuccessListener {
                voteSubmitted.postValue(true)
                loadSurveyStats()
            }
            .addOnFailureListener { e ->
                errorMessage.postValue(e.message ?: "Error al registrar el voto")
            }
    }

    fun loadSurveyStats() {
        db.collection("mods_survey_votes")
            .get()
            .addOnSuccessListener { querySnapshot ->
                val total = querySnapshot.size()
                var yesCount = 0
                for (doc in querySnapshot.documents) {
                    if (doc.getBoolean("wantsMods") == true) {
                        yesCount++
                    }
                }
                surveyStats.postValue(Pair(yesCount, total))
            }
            .addOnFailureListener {
                // Silently fallback if unable to fetch counts
            }
    }
}
