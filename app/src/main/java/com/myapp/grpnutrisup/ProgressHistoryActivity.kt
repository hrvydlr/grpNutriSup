package com.myapp.grpnutrisup

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.models.ProgressItem

class ProgressHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressAdapter: ProgressHistoryAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var backButton: ImageButton
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_progress_history)
        backButton = findViewById(R.id.backButton)

        backButton.setOnClickListener {
            onBackPressed() // Navigate back
        }

        recyclerView = findViewById(R.id.progress_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)

        progressAdapter = ProgressHistoryAdapter(emptyList())
        recyclerView.adapter = progressAdapter

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        fetchProgressHistory()
    }

    private fun fetchProgressHistory() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid

            firestore.collection("progressHistory").document(userId).collection("days")
                .get()
                .addOnSuccessListener { documents ->
                    val progressMap = mutableMapOf<String, ProgressItem>()

                    for (document in documents) {
                        val day = document.getString("day") ?: "Day Unknown"
                        val calorieIntake = document.getLong("calorieIntake")?.toInt() ?: 0
                        val calorieGoal = document.getLong("calorieGoal")?.toInt() ?: 2000
                        val fats = document.getLong("fats")?.toInt() ?: 0
                        val protein = document.getLong("protein")?.toInt() ?: 0

                        // Aggregate data by day
                        val existingProgress = progressMap[day]
                        if (existingProgress != null) {
                            progressMap[day] = ProgressItem(
                                day = day,
                                calorieIntake = existingProgress.calorieIntake + calorieIntake,
                                calorieGoal = calorieGoal, // Goal remains the same
                                fats = existingProgress.fats + fats,
                                protein = existingProgress.protein + protein
                            )
                        } else {
                            progressMap[day] = ProgressItem(
                                day = day,
                                calorieIntake = calorieIntake,
                                calorieGoal = calorieGoal,
                                fats = fats,
                                protein = protein
                            )
                        }
                    }

                    val progressList = progressMap.values.toList()
                    progressAdapter.updateProgressList(progressList)
                }
                .addOnFailureListener { exception ->
                    Log.e("ProgressHistory", "Error fetching progress history", exception)
                }
        }
    }
}
