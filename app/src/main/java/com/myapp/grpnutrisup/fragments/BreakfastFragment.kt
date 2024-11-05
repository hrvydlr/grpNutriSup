package com.myapp.grpnutrisup.fragments

import android.content.Context
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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.myapp.grpnutrisup.R
import com.myapp.grpnutrisup.adapters.FoodAdapter
import com.myapp.grpnutrisup.models.Food
import java.text.SimpleDateFormat
import java.util.*

class BreakfastFragment : Fragment() {

    private lateinit var breakfastRecyclerView: RecyclerView
    private lateinit var breakfastAdapter: FoodAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val dateFormat = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    private var lastFetchDate: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_breakfast, container, false)
        initializeRecyclerView(view)
        loadUserDataAndFetchFood()
        return view
    }

    private fun initializeRecyclerView(view: View) {
        breakfastRecyclerView = view.findViewById(R.id.breakfastRecyclerView)
        breakfastAdapter = FoodAdapter(requireContext(), emptyList(), mutableListOf())
        breakfastRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = breakfastAdapter
        }
    }

    private fun loadUserDataAndFetchFood() {
        fetchUserData { tdee, goal, allergens ->
            val dailyCalorieGoal = calculateCalorieGoal(tdee, goal)
            val breakfastCalorieGoal = calculateMealCalorieGoal(dailyCalorieGoal, "Breakfast")

            // Check if we need to fetch new food items based on the current date
            val currentDate = dateFormat.format(Date())
            if (currentDate != lastFetchDate) {
                lastFetchDate = currentDate // Update the last fetch date
                fetchOptimizedFoodForMeal("Breakfast", breakfastCalorieGoal, allergens, goal) { foods ->
                    breakfastAdapter.updateList(foods)
                    // Optionally, save lastFetchDate to shared preferences
                    saveLastFetchDate(currentDate)
                }
            } else {
                // If it's the same day, we can use cached items
                val cachedFoods = loadCachedFoodItems()
                if (cachedFoods.isNotEmpty()) {
                    breakfastAdapter.updateList(cachedFoods)
                } else {
                    // If there are no cached items, still fetch to ensure the list is populated
                    fetchOptimizedFoodForMeal("Breakfast", breakfastCalorieGoal, allergens, goal) { foods ->
                        breakfastAdapter.updateList(foods)
                    }
                }
            }
        }
    }

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

    private fun List<Food>.sortFoodsByGoal(goal: String) = when (goal.lowercase()) {
        "gain" -> sortedByDescending { it.calories }
        "lose" -> sortedBy { it.calories }
        else -> this
    }

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

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    private fun calculateCalorieGoal(tdee: Double, goal: String): Double {
        return when (goal.lowercase()) {
            "gain" -> tdee * 1.1  // 10% increase for gain
            "lose" -> tdee * 0.9  // 10% decrease for lose
            else -> tdee           // maintain
        }
    }

    private fun calculateMealCalorieGoal(dailyCalories: Double, mealType: String): Double {
        return when (mealType.lowercase()) {
            "breakfast" -> dailyCalories * 0.25
            "lunch" -> dailyCalories * 0.35
            "dinner" -> dailyCalories * 0.30
            "snack" -> dailyCalories * 0.10
            else -> dailyCalories * 0.25
        }
    }

    private fun logDebug(message: String) {
        Log.d("BreakfastFragment", message)
    }

    private fun saveLastFetchDate(date: String) {
        val sharedPreferences = requireContext().getSharedPreferences("UserFoodPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("lastFetchDate", date).apply()
    }

    private fun loadCachedFoodItems(): List<Food> {
        val sharedPreferences = requireContext().getSharedPreferences("UserFoodPrefs", Context.MODE_PRIVATE)
        val jsonString = sharedPreferences.getString("cachedFoodItems", null)
        return if (jsonString != null) {
            val gson = Gson()
            val foodType = object : TypeToken<List<Food>>() {}.type
            gson.fromJson(jsonString, foodType) ?: emptyList()
        } else {
            emptyList()
        }
    }

    private fun saveFoodItemsToPreferences(foods: List<Food>) {
        val gson = Gson()
        val jsonString = gson.toJson(foods)
        val sharedPreferences = requireContext().getSharedPreferences("UserFoodPrefs", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString("cachedFoodItems", jsonString).apply()
    }
}
