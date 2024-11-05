package com.myapp.grpnutrisup.fragments

import android.os.Bundle
import android.util.Log
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

class BreakfastFragment : Fragment() {

    private lateinit var breakfastRecyclerView: RecyclerView
    private lateinit var breakfastAdapter: FoodAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_breakfast, container, false)

        // Initialize RecyclerView and set the adapter
        initializeRecyclerView(view)

        // Load user data and fetch optimized food items
        loadUserDataAndFetchFood()

        return view
    }

    // Initializes the RecyclerView with layout manager and adapter
    private fun initializeRecyclerView(view: View) {
        breakfastRecyclerView = view.findViewById(R.id.breakfastRecyclerView)
        breakfastAdapter = FoodAdapter(requireContext(), emptyList(), mutableListOf())
        breakfastRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = breakfastAdapter
        }
    }

    // Fetches user data and retrieves optimized food items
    private fun loadUserDataAndFetchFood() {
        fetchUserData { tdee, goal, allergens ->
            val dailyCalorieGoal = calculateCalorieGoal(tdee, goal)
            val breakfastCalorieGoal = calculateMealCalorieGoal(dailyCalorieGoal, "Breakfast")

            fetchOptimizedFoodForMeal("Breakfast", breakfastCalorieGoal, allergens, goal) { foods ->
                breakfastAdapter.updateList(foods)
                breakfastRecyclerView.visibility = View.VISIBLE // Ensure visibility after update
            }
        }
    }

    // Fetches user-specific data such as TDEE, goal, and allergens from Firebase
    private fun fetchUserData(callback: (Double, String, List<String>) -> Unit) {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            showToast("User not logged in.")
            return
        }

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val tdee = document.getDouble("TDEE") ?: 2000.0
                    val goal = document.getString("goal") ?: "maintain"
                    val allergens = document.get("allergens") as? List<String> ?: emptyList()
                    callback(tdee, goal, allergens)
                } else {
                    showToast("User data not found.")
                }
            }
            .addOnFailureListener { e ->
                showToast("Error fetching user data: ${e.message}")
            }
    }

    // Fetches and filters food items based on meal type, calorie goal, and user allergies
    private fun fetchOptimizedFoodForMeal(
        mealType: String,
        calorieGoal: Double,
        allergens: List<String>,
        userGoal: String,
        callback: (List<Food>) -> Unit
    ) {
        logDebug("Querying meal type: $mealType")

        db.collection("food_db")
            .whereEqualTo("meal_type", mealType)
            .get()
            .addOnSuccessListener { result ->
                logDebug("Query successful, result size: ${result.size()}")

                val foods = result.mapNotNull { document ->
                    document.toObject(Food::class.java).takeIf { food ->
                        allergens.none { food.allergens.contains(it, ignoreCase = true) }
                    }
                }

                logDebug("Available foods count after allergen filtering: ${foods.size}")

                val sortedFoods = foods.sortFoodsByGoal(userGoal)
                val optimizedFoods = selectFoodsToMeetCalorieGoal(sortedFoods, calorieGoal)

                logDebug("Optimized foods count: ${optimizedFoods.size}")
                callback(optimizedFoods)
            }
            .addOnFailureListener { e ->
                showToast("Error fetching $mealType foods: ${e.message}")
            }
    }

    // Sorts foods based on user's goal (gain, lose, or maintain)
    private fun List<Food>.sortFoodsByGoal(goal: String) = when (goal.lowercase()) {
        "gain" -> sortedByDescending { it.calories }
        "lose" -> sortedBy { it.calories }
        else -> this
    }

    // Selects foods to reach the calorie goal while staying under the limit
    private fun selectFoodsToMeetCalorieGoal(
        foods: List<Food>,
        calorieGoal: Double
    ): List<Food> {
        val selectedFoods = mutableListOf<Food>()
        var totalCalories = 0.0

        for (food in foods) {
            if (totalCalories + food.calories <= calorieGoal) {
                selectedFoods.add(food)
                totalCalories += food.calories
            }
        }
        return selectedFoods
    }

    // Displays a toast message to the user
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    // Calculates daily calorie goal based on user's TDEE and goal
    private fun calculateCalorieGoal(tdee: Double, goal: String): Double {
        return when (goal.lowercase()) {
            "gain" -> tdee * 1.1  // 10% increase for gain
            "lose" -> tdee * 0.9  // 10% decrease for lose
            else -> tdee           // maintain
        }
    }

    // Calculates calorie goal for a specific meal type
    private fun calculateMealCalorieGoal(dailyCalories: Double, mealType: String): Double {
        return when (mealType.lowercase()) {
            "breakfast" -> dailyCalories * 0.25
            "lunch" -> dailyCalories * 0.35
            "dinner" -> dailyCalories * 0.30
            "snack" -> dailyCalories * 0.10
            else -> dailyCalories * 0.25
        }
    }

    // Logs debug information for troubleshooting
    private fun logDebug(message: String) {
        Log.d("BreakfastFragment", message)
    }
}
