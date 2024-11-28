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
import java.text.SimpleDateFormat
import java.util.*

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

        saveOrUpdateTodayProgress() // Automatically save/update progress for today
    }

    private fun saveOrUpdateTodayProgress() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            val currentDay = getCurrentDay()

            // Reference for today's progress in Firestore
            val todayRef = firestore.collection("progressHistory").document(userId).collection("days").document(currentDay)

            // Fetch user's current progress (calorieIntake, fats, proteins, etc.)
            firestore.collection("users").document(user.email!!)
                .get()
                .addOnSuccessListener { userDocument ->
                    if (userDocument.exists()) {
                        val calorieIntake = userDocument.getLong("calorieIntake")?.toInt() ?: 0
                        val calorieGoal = userDocument.getLong("calorieResult")?.toInt() ?: 2000
                        val fats = userDocument.getLong("fatIntake")?.toInt() ?: 0
                        val protein = userDocument.getLong("proteinIntake")?.toInt() ?: 0

                        // Check if today's progress already exists
                        todayRef.get()
                            .addOnSuccessListener { todayDocument ->
                                if (todayDocument.exists()) {
                                    // Update today's progress
                                    todayRef.update(
                                        mapOf(
                                            "calorieIntake" to calorieIntake,
                                            "calorieGoal" to calorieGoal,
                                            "fats" to fats,
                                            "protein" to protein
                                        )
                                    ).addOnSuccessListener {
                                        Log.d("ProgressHistory", "Today's progress updated successfully")
                                        fetchProgressHistory()
                                    }
                                } else {
                                    // Create a new entry for today's progress
                                    todayRef.set(
                                        mapOf(
                                            "day" to currentDay,
                                            "calorieIntake" to calorieIntake,
                                            "calorieGoal" to calorieGoal,
                                            "fats" to fats,
                                            "protein" to protein
                                        )
                                    ).addOnSuccessListener {
                                        Log.d("ProgressHistory", "Today's progress saved successfully")
                                        fetchProgressHistory()
                                    }
                                }
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("ProgressHistory", "Error fetching user data", exception)
                }
        }
    }

    private fun fetchProgressHistory() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid

            // Fetch all progress history for the user
            firestore.collection("progressHistory").document(userId).collection("days")
                .get()
                .addOnSuccessListener { documents ->
                    val progressList = mutableListOf<ProgressItem>()

                    for (document in documents) {
                        val day = document.getString("day") ?: "Day Unknown"
                        val calorieIntake = document.getLong("calorieIntake")?.toInt() ?: 0
                        val calorieGoal = document.getLong("calorieGoal")?.toInt() ?: 2000
                        val fats = document.getLong("fats")?.toInt() ?: 0
                        val protein = document.getLong("protein")?.toInt() ?: 0

                        progressList.add(
                            ProgressItem(
                                day = day,
                                calorieIntake = calorieIntake,
                                calorieGoal = calorieGoal,
                                fats = fats,
                                protein = protein
                            )
                        )
                    }

                    // Update the adapter with the new progress list
                    progressAdapter.updateProgressList(progressList)
                }
                .addOnFailureListener { exception ->
                    Log.e("ProgressHistory", "Error fetching progress history", exception)
                }
        }
    }

    private fun getCurrentDay(): String {
        val dateFormat = SimpleDateFormat("MM-dd-yy", Locale.getDefault())
        return dateFormat.format(Date()) // Returns current date in "MM-dd-yy" format
    }
}
