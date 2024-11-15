package com.myapp.grpnutrisup.activities

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.R
import com.myapp.grpnutrisup.adapters.HistoryAdapter
import com.myapp.grpnutrisup.models.DailyCalories

class IntakeHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var loadMoreButton: Button
    private val db = FirebaseFirestore.getInstance()
    private val historyList = mutableListOf<DailyCalories>()
    private val batchSize = 10
    private var lastLoadedDate: String? = null
    private lateinit var historyAdapter: HistoryAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intake_history)

        recyclerView = findViewById(R.id.historyRecyclerView)
        loadMoreButton = findViewById(R.id.loadMoreButton)

        historyAdapter = HistoryAdapter(historyList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = historyAdapter

        loadIntakeHistory()

        loadMoreButton.setOnClickListener { loadIntakeHistory() }
    }

    private fun loadIntakeHistory() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: return
        val userDocRef = db.collection("users").document(userEmail)

        userDocRef.get().addOnSuccessListener { document ->
            val weeklyProgress = document.get("weeklyProgress") as? Map<String, Map<String, Any>>
            if (weeklyProgress != null) {
                val sortedDates = weeklyProgress.keys.sortedDescending()
                val startIndex = lastLoadedDate?.let { sortedDates.indexOf(it) + 1 } ?: 0
                val endIndex = (startIndex + batchSize).coerceAtMost(sortedDates.size)

                if (startIndex < endIndex) {
                    val batch = sortedDates.subList(startIndex, endIndex)
                    batch.forEach { date ->
                        val data = weeklyProgress[date]
                        val calories = (data?.get("calories") as? Long)?.toInt() ?: 0
                        historyList.add(DailyCalories(date, calories))
                    }
                    historyAdapter.notifyDataSetChanged()
                    lastLoadedDate = batch.last()
                } else {
                    loadMoreButton.isEnabled = false
                    loadMoreButton.text = "No More Data"
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load intake history", Toast.LENGTH_SHORT).show()
        }
    }
}
