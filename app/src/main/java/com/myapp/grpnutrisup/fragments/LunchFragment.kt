package com.myapp.grpnutrisup.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.R
import com.myapp.grpnutrisup.adapters.FoodAdapter
import com.myapp.grpnutrisup.models.Food

class LunchFragment : Fragment() {

    private lateinit var lunchRecyclerView: RecyclerView
    private lateinit var lunchAdapter: FoodAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_lunch, container, false)
        lunchRecyclerView = view.findViewById(R.id.lunchRecyclerView)
        lunchAdapter = FoodAdapter(requireContext(), emptyList(), mutableListOf())
        lunchRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        lunchRecyclerView.adapter = lunchAdapter

        // Fetch user data and update lunch meal plan
        fetchUserData { userTdee, userGoal, userAllergens ->
            val dailyCalorieGoal = calculateAdjustedCalorieGoal(userTdee, userGoal)
            val lunchCalorieGoal = getCalorieGoalForMeal(dailyCalorieGoal, "Lunch")
            fetchOptimizedFoodForMeal("Lunch", lunchCalorieGoal, userAllergens, userGoal) { foods ->
                lunchAdapter.updateList(foods)
            }
        }

        return view
    }

    private fun fetchUserData(callback: (Double, String, List<String>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId != null) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null) {
                        val tdee = document.getDouble("TDEE") ?: 2000.0  // Default TDEE if not found
                        val goal = document.getString("goal") ?: "maintain"
                        val allergens = document.get("allergens") as? List<String> ?: listOf("None")
                        callback(tdee, goal, allergens)
                    } else {
                        showToast("User data not found.")
                    }
                }
                .addOnFailureListener { e ->
                    showToast("Error fetching user data: ${e.message}")
                }
        } else {
            showToast("User not logged in.")
        }
    }

    private fun fetchOptimizedFoodForMeal(
        mealType: String,
        calorieGoal: Double,
        allergies: List<String>,
        userGoal: String,
        callback: (List<Food>) -> Unit
    ) {
        db.collection("food_db")
            .whereEqualTo("meal_type", mealType)
            .get()
            .addOnSuccessListener { result ->
                val availableFoods = result.mapNotNull { document ->
                    document.toObject(Food::class.java).takeIf { food ->
                        allergies.none { food.allergens.contains(it, ignoreCase = true) }
                    }
                }

                val sortedFoods = when (userGoal.lowercase()) {
                    "gain" -> availableFoods.sortedByDescending { it.calories }
                    "lose" -> availableFoods.sortedBy { it.calories }
                    else -> availableFoods
                }

                val optimizedFoods = getMultipleFoodOptions(sortedFoods, calorieGoal)
                callback(optimizedFoods)
            }
            .addOnFailureListener { e ->
                showToast("Error fetching $mealType foods: ${e.message}")
            }
    }

    private fun getMultipleFoodOptions(
        availableFoods: List<Food>,
        calorieGoal: Double
    ): List<Food> {
        val selectedFoods = mutableListOf<Food>()
        var totalCalories = 0.0

        for (food in availableFoods) {
            if (totalCalories + food.calories <= calorieGoal) {
                selectedFoods.add(food)
                totalCalories += food.calories
            }
        }
        return selectedFoods
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // Utility functions
    private fun calculateAdjustedCalorieGoal(tdee: Double, goal: String): Double {
        return when (goal.lowercase()) {
            "gain" -> tdee * 1.1  // 10% increase for gaining weight
            "lose" -> tdee * 0.9  // 10% decrease for losing weight
            else -> tdee          // maintain
        }
    }

    private fun getCalorieGoalForMeal(dailyCalories: Double, mealType: String): Double {
        return when (mealType.lowercase()) {
            "breakfast" -> dailyCalories * 0.25
            "lunch" -> dailyCalories * 0.35     // 35% for lunch
            "dinner" -> dailyCalories * 0.30
            "snack" -> dailyCalories * 0.10
            else -> dailyCalories * 0.25
        }
    }
}
