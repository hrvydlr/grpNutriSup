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

    private lateinit var breakfastRecyclerView: RecyclerView
    private lateinit var lunchRecyclerView: RecyclerView
    private lateinit var dinnerRecyclerView: RecyclerView
    private lateinit var breakfastAdapter: FoodAdapter
    private lateinit var lunchAdapter: FoodAdapter
    private lateinit var dinnerAdapter: FoodAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_plan)

        breakfastRecyclerView = findViewById(R.id.breakfastRecyclerView)
        lunchRecyclerView = findViewById(R.id.lunchRecyclerView)
        dinnerRecyclerView = findViewById(R.id.dinnerRecyclerView)

        // Set up each RecyclerView with its own adapter
        breakfastRecyclerView.layoutManager = LinearLayoutManager(this)
        lunchRecyclerView.layoutManager = LinearLayoutManager(this)
        dinnerRecyclerView.layoutManager = LinearLayoutManager(this)

        breakfastAdapter = FoodAdapter(this, emptyList())
        lunchAdapter = FoodAdapter(this, emptyList())
        dinnerAdapter = FoodAdapter(this, emptyList())

        breakfastRecyclerView.adapter = breakfastAdapter
        lunchRecyclerView.adapter = lunchAdapter
        dinnerRecyclerView.adapter = dinnerAdapter

        // Fetch meal plan data from Firestore
        fetchMealPlan()
    }

    private fun fetchMealPlan() {
        val currentUserEmail = auth.currentUser?.email

        if (currentUserEmail != null) {
            db.collection("users").document(currentUserEmail).get()
                .addOnSuccessListener { userDocument ->
                    if (userDocument != null) {
                        val userGoal = userDocument.getString("goal") ?: ""
                        val userAllergies = userDocument.get("allergies") as? List<String> ?: emptyList()

                        // Fetch food items based on user's goal
                        db.collection("foods")
                            .whereEqualTo("goal", userGoal)
                            .get()
                            .addOnSuccessListener { result ->
                                val breakfastList = mutableListOf<Food>()
                                val lunchList = mutableListOf<Food>()
                                val dinnerList = mutableListOf<Food>()

                                for (document in result) {
                                    val food = document.toObject(Food::class.java)

                                    // Check for allergies
                                    if (!userAllergies.any { food.description.contains(it, ignoreCase = true) }) {
                                        when (food.mealType) {
                                            "Breakfast" -> breakfastList.add(food)
                                            "Lunch" -> lunchList.add(food)
                                            "Dinner" -> dinnerList.add(food)
                                        }
                                    }
                                }

                                // Update the RecyclerViews with respective meal plans
                                breakfastAdapter.updateList(breakfastList)
                                lunchAdapter.updateList(lunchList)
                                dinnerAdapter.updateList(dinnerList)
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
