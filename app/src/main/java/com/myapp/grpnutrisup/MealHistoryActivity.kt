package com.myapp.grpnutrisup

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.R
import com.myapp.grpnutrisup.adapters.MealHistoryAdapter
import com.myapp.grpnutrisup.models.Meal

class MealHistoryActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var mealHistoryAdapter: MealHistoryAdapter
    private val mealsList = mutableListOf<Meal>()
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_history)

        recyclerView = findViewById(R.id.recyclerViewMealHistory)
        mealHistoryAdapter = MealHistoryAdapter(this, mealsList)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MealHistoryActivity)
            adapter = mealHistoryAdapter
        }

        loadMealHistory()
    }

    private fun loadMealHistory() {
        val userEmail = auth.currentUser?.email
        if (userEmail != null) {
            db.collection("users")
                .document(userEmail)
                .collection("eatenFoods")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener { querySnapshot ->
                    mealsList.clear()
                    for (document in querySnapshot.documents) {
                        document.toObject(Meal::class.java)?.let { mealsList.add(it) }
                    }
                    mealHistoryAdapter.notifyDataSetChanged()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Failed to load meal history", Toast.LENGTH_SHORT).show()
                    Log.e("MealHistoryActivity", "Error loading meal history", e)
                }
        } else {
            Toast.makeText(this, "Please log in to view meal history", Toast.LENGTH_SHORT).show()
        }
    }
}
