package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.adapters.FoodAdapter
import com.myapp.grpnutrisup.models.Food

class MealActivity : AppCompatActivity() {

    private lateinit var breakfastRecyclerView: RecyclerView
    private lateinit var lunchRecyclerView: RecyclerView
    private lateinit var dinnerRecyclerView: RecyclerView

    private lateinit var breakfastAdapter: FoodAdapter
    private lateinit var lunchAdapter: FoodAdapter
    private lateinit var dinnerAdapter: FoodAdapter

    private lateinit var bottomNavigationView: BottomNavigationView

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_plan)

        setupRecyclerViews(emptyList())
        fetchAndGenerateMealPlan()

        setupBottomNavigationBar()
    }

    private fun setupRecyclerViews(favoriteFoods: List<String>) {
        breakfastRecyclerView = findViewById(R.id.breakfastRecyclerView)
        breakfastAdapter = FoodAdapter(this, emptyList(), favoriteFoods.toMutableList())
        breakfastRecyclerView.layoutManager = LinearLayoutManager(this)
        breakfastRecyclerView.adapter = breakfastAdapter

        lunchRecyclerView = findViewById(R.id.lunchRecyclerView)
        lunchAdapter = FoodAdapter(this, emptyList(), favoriteFoods.toMutableList())
        lunchRecyclerView.layoutManager = LinearLayoutManager(this)
        lunchRecyclerView.adapter = lunchAdapter

        dinnerRecyclerView = findViewById(R.id.dinnerRecyclerView)
        dinnerAdapter = FoodAdapter(this, emptyList(), favoriteFoods.toMutableList())
        dinnerRecyclerView.layoutManager = LinearLayoutManager(this)
        dinnerRecyclerView.adapter = dinnerAdapter
    }

    private fun setupBottomNavigationBar() {
        bottomNavigationView = findViewById(R.id.bottom_navigation)

        bottomNavigationView.selectedItemId = R.id.navigation_meal

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_home -> {
                    startActivity(Intent(this, HomeActivity::class.java))
                    true
                }
                R.id.navigation_search -> {
                    startActivity(Intent(this, FoodSearchActivity::class.java))
                    true
                }
                R.id.navigation_meal -> {
                    true
                }
                R.id.navigation_profile -> {
                    startActivity(Intent(this, ProfileActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun fetchAndGenerateMealPlan() {
        val currentUser = auth.currentUser ?: run {
            showToast("User not logged in.")
            return
        }

        val userEmail = currentUser.email ?: return
        db.collection("users").document(userEmail).get()
            .addOnSuccessListener { userDocument ->
                if (userDocument.exists()) {
                    // Fetch user-specific data (preferences, goals, etc.)
                    val calorieGoal = userDocument.getDouble("calorieGoal") ?: 2000.0
                    val allergies = userDocument.get("allergies") as? List<String> ?: emptyList()
                    val preferences = userDocument.get("favoriteFoods") as? List<String> ?: emptyList()

                    // Track user's goal (e.g., "gain_weight" or "lose_weight")
                    val userGoal = userDocument.getString("goal") ?: "maintain"

                    // Fetch current intake from the user document
                    val currentCalories = userDocument.getDouble("calorieIntake") ?: 0.0

                    // Calculate remaining calorie intake
                    val remainingCalories = calculateRemaining(calorieGoal, currentCalories, userGoal)

                    // Setup recycler views and generate meal plan
                    setupRecyclerViews(preferences)

                    // Pass the user's goal to generatePrescriptiveMealPlan
                    generatePrescriptiveMealPlan(
                        remainingCalories, allergies, preferences, userGoal
                    )
                } else {
                    showToast("User data not found.")
                }
            }
            .addOnFailureListener { e ->
                showToast("Error fetching user data: ${e.message}")
            }
    }

    private fun calculateRemaining(goal: Double, currentIntake: Double, userGoal: String): Double {
        // Adjust calorie goals based on user weight goals
        return when (userGoal) {
            "gain" -> goal * 1.15 - currentIntake  // Gain weight: +15% calories
            "lose" -> goal * 0.85 - currentIntake  // Lose weight: -15% calories
            else -> goal - currentIntake           // Maintain weight: no adjustment
        }
    }

    private fun generatePrescriptiveMealPlan(
        remainingCalories: Double,
        allergies: List<String>,
        preferences: List<String>,
        userGoal: String
    ) {
        // Fetch multiple options for breakfast, lunch, and dinner
        fetchOptimizedFoodForMeal(
            "Breakfast", remainingCalories * 0.3, allergies, userGoal
        ) { breakfastFoods ->
            breakfastAdapter.updateList(breakfastFoods)
        }

        fetchOptimizedFoodForMeal(
            "Lunch", remainingCalories * 0.4, allergies,  userGoal
        ) { lunchFoods ->
            lunchAdapter.updateList(lunchFoods)
        }

        fetchOptimizedFoodForMeal(
            "Dinner", remainingCalories * 0.3, allergies, userGoal
        ) { dinnerFoods ->
            dinnerAdapter.updateList(dinnerFoods)
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
                val availableFoods = mutableListOf<Food>()

                for (document in result) {
                    val food = document.toObject(Food::class.java)

                    // Check if the food item has allergens
                    val hasAllergen = allergies.any { food.allergens.contains(it, ignoreCase = true) }
                    if (!hasAllergen) {
                        availableFoods.add(food)
                    }
                }

                // Log the number of available foods after filtering for allergens
                Log.d("MealActivity", "Available foods for $mealType after filtering allergens: ${availableFoods.size}")

                // Sort by user goal: gain, lose, or maintain weight
                val sortedFoods = when (userGoal) {
                    "Gain" -> availableFoods.sortedByDescending { it.calories }
                    "Lose" -> availableFoods.sortedBy { it.calories }
                    else -> availableFoods // For maintenance or other goals
                }

                // Log the number of foods sorted by user goal
                Log.d("MealActivity", "Sorted foods for $mealType based on user goal: ${sortedFoods.size}")

                // Call the optimized food selection function to find foods around the calorie goal
                val optimizedFoods = getMultipleFoodOptions(sortedFoods, calorieGoal)
                callback(optimizedFoods)  // Return optimized food list for the meal type
            }
            .addOnFailureListener { e ->
                showToast("Error fetching $mealType foods: ${e.message}")
            }
    }



    // This method returns multiple food options around the calorie target
    private fun getMultipleFoodOptions(
        availableFoods: List<Food>,
        calorieGoal: Double
    ): List<Food> {
        val selectedFoods = mutableListOf<List<Food>>()
        val tempFoods = mutableListOf<Food>()

        var totalCalories = 0.0

        for (food in availableFoods) {
            if (totalCalories + food.calories <= calorieGoal + 50) {  // Small buffer for flexibility
                tempFoods.add(food)
                totalCalories += food.calories
            }

            // Once a combination is close to the goal, store it and reset
            if (totalCalories >= calorieGoal * 0.9) {
                selectedFoods.add(tempFoods.toList())
                tempFoods.clear()
                totalCalories = 0.0
            }
        }

        // Flatten multiple options into one list to display more meal choices
        return selectedFoods.flatten()
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
