package com.myapp.grpnutrisup

import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.R
import com.myapp.grpnutrisup.adapters.IntakeHistoryAdapter
import com.myapp.grpnutrisup.models.DailyCalories
import java.text.SimpleDateFormat
import java.util.*

class IntakeHistoryActivity : AppCompatActivity() {

    private lateinit var historyRecyclerView: RecyclerView
    private lateinit var backButton: ImageButton
    private lateinit var adapter: IntakeHistoryAdapter
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var dailyCaloriesList: MutableList<DailyCalories> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intake_history)

        // Initialize Firebase and views
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        historyRecyclerView = findViewById(R.id.historyRecyclerView)
        backButton = findViewById(R.id.backButton)

        setupRecyclerView()
        setupBackButton()
        fetchDailyCalories()
    }

    private fun setupRecyclerView() {
        adapter = IntakeHistoryAdapter(dailyCaloriesList)
        historyRecyclerView.layoutManager = LinearLayoutManager(this)
        historyRecyclerView.adapter = adapter
    }

    private fun setupBackButton() {
        backButton.setOnClickListener {
            finish() // Close the activity and go back to the previous screen
        }
    }

    private fun fetchDailyCalories() {
        val currentUser = auth.currentUser
        currentUser?.let { user ->
            val userId = user.uid
            firestore.collection("intake_history")
                .whereEqualTo("user_id", userId) // Filter by user_id
                .get()
                .addOnSuccessListener { querySnapshot ->
                    val dailyCaloriesMap = mutableMapOf<String, Int>() // Map for daily calories

                    for (document in querySnapshot.documents) {
                        val calories = document.getLong("calories")?.toInt() ?: 0
                        val timestamp = document.getTimestamp("timestamp")?.toDate() ?: continue

                        val day = formatDate(timestamp) // Format date to "MMM dd, yyyy"

                        dailyCaloriesMap[day] = dailyCaloriesMap.getOrDefault(day, 0) + calories
                    }

                    dailyCaloriesList.clear()
                    for ((day, totalCalories) in dailyCaloriesMap) {
                        dailyCaloriesList.add(DailyCalories(day, totalCalories))
                    }

                    dailyCaloriesList.sortBy { it.date } // Sort by date
                    adapter.notifyDataSetChanged() // Notify adapter of data changes
                }
                .addOnFailureListener { exception ->
                    Log.e("IntakeHistoryActivity", "Error fetching intake history", exception)
                    Toast.makeText(this, "Failed to load history", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun formatDate(date: Date): String {
        val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        return sdf.format(date)
    }
}
