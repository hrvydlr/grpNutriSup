package com.myapp.grpnutrisup

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.adapters.FoodAdapter
import com.myapp.grpnutrisup.models.Food

class MealPlanActivity : AppCompatActivity() {

    private lateinit var foodAdapter: FoodAdapter
    private lateinit var recyclerView: RecyclerView
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_plan)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Initialize the food adapter with an empty list
        foodAdapter = FoodAdapter(this, emptyList())
        recyclerView.adapter = foodAdapter

        // Fetch meal plan data from Firestore
        fetchMealPlan()
    }

    private fun fetchMealPlan() {
        val currentUserEmail = auth.currentUser?.email

        if (currentUserEmail != null) {
            // Fetch user data (e.g., goal and allergies)
            db.collection("users").document(currentUserEmail).get()
                .addOnSuccessListener { userDocument ->
                    if (userDocument != null) {
                        val userGoal = userDocument.getString("goal") ?: ""
                        val userAllergies = userDocument.get("allergies") as? List<String> ?: emptyList()

                        // Fetch food items based on user's goal
                        db.collection("foods")
                            .whereEqualTo("goal", userGoal) // Filter by goal
                            .get()
                            .addOnSuccessListener { result ->
                                val foodList = mutableListOf<Food>()
                                for (document in result) {
                                    val food = document.toObject(Food::class.java)

                                    // Check if the food contains any of the user's allergies
                                    if (!userAllergies.any { food.description.contains(it, ignoreCase = true) }) {
                                        foodList.add(food) // Add the food if no allergies match
                                    }
                                }
                                // Update the RecyclerView with the filtered meal plan
                                foodAdapter.updateList(foodList)
                            }
                            .addOnFailureListener { exception ->
                                Toast.makeText(this, "Error getting meal plan: ${exception.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "User data not found!", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(this, "Failed to retrieve user data: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            Toast.makeText(this, "User is not logged in", Toast.LENGTH_SHORT).show()
        }
    }
}
