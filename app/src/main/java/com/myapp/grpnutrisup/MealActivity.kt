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
import java.text.SimpleDateFormat
import java.util.*

class MealActivity : AppCompatActivity() {

    private lateinit var breakfastRecyclerView: RecyclerView
    private lateinit var lunchRecyclerView: RecyclerView
    private lateinit var dinnerRecyclerView: RecyclerView

    private lateinit var breakfastAdapter: FoodAdapter
    private lateinit var lunchAdapter: FoodAdapter
    private lateinit var dinnerAdapter: FoodAdapter

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_plan)

        // Initialize RecyclerViews and Adapters
        setupRecyclerViews()

        // Set up Bottom Navigation View
        setupBottomNavigation()

        // Fetch meal plan data from Firestore
        fetchMealPlan()
    }

    private fun setupRecyclerViews() {
        breakfastRecyclerView = findViewById(R.id.breakfastRecyclerView)
        breakfastAdapter = FoodAdapter(this, emptyList())
        breakfastRecyclerView.layoutManager = LinearLayoutManager(this)
        breakfastRecyclerView.adapter = breakfastAdapter

        lunchRecyclerView = findViewById(R.id.lunchRecyclerView)
        lunchAdapter = FoodAdapter(this, emptyList())
        lunchRecyclerView.layoutManager = LinearLayoutManager(this)
        lunchRecyclerView.adapter = lunchAdapter

        dinnerRecyclerView = findViewById(R.id.dinnerRecyclerView)
        dinnerAdapter = FoodAdapter(this, emptyList())
        dinnerRecyclerView.layoutManager = LinearLayoutManager(this)
        dinnerRecyclerView.adapter = dinnerAdapter
    }

    private fun setupBottomNavigation() {
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    navigateTo(HomeActivity::class.java)
                    true
                }
                R.id.navigation_search -> {
                    navigateTo(FoodSearchActivity::class.java)
                    true
                }
                R.id.navigation_meal -> true // Current activity
                R.id.navigation_profile -> {
                    navigateTo(ProfileActivity::class.java)
                    true
                }
                else -> false
            }
        }
    }

    private fun navigateTo(destination: Class<*>) {
        val intent = Intent(this, destination)
        startActivity(intent)
        finish() // Close current activity to prevent stack buildup
    }

    private fun fetchMealPlan() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User is not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        val userEmail = currentUser.email ?: return
        db.collection("users").document(userEmail).get()
            .addOnSuccessListener { userDocument ->
                if (userDocument.exists()) {
                    val userAllergies = userDocument.get("allergens") as? List<String> ?: emptyList()
                    Log.d("MealActivity", "Allergies: $userAllergies")
                    // Generate daily meal plan based on user data
                    generateDailyMealPlan(userAllergies)
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                    Log.d("MealActivity", "User document does not exist.")
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("MealActivity", "Error fetching user data", e)
            }
    }

    private fun generateDailyMealPlan(allergies: List<String>) {
        fetchRandomFoodForMeal("Breakfast", allergies, breakfastAdapter)
        fetchRandomFoodForMeal("Lunch", allergies, lunchAdapter)
        fetchRandomFoodForMeal("Dinner", allergies, dinnerAdapter)
    }

    private fun fetchRandomFoodForMeal(mealType: String, allergies: List<String>, adapter: FoodAdapter) {
        Log.d("MealActivity", "Fetching random food for meal: $mealType")

        db.collection("food_db")
            .whereEqualTo("meal_type", mealType)
            .get()
            .addOnSuccessListener { result ->
                val foodList = mutableListOf<Food>()
                Log.d("MealActivity", "Query success for $mealType, result count: ${result.size()}")

                if (result.isEmpty) {
                    Log.d("MealActivity", "No foods found for meal: $mealType")
                } else {
                    for (document in result) {
                        val food = document.toObject(Food::class.java)

                        // Check for allergens
                        if (!allergies.any { allergen -> food.allergens.contains(allergen, ignoreCase = true) }) {
                            foodList.add(food)
                        }
                    }

                    // Randomly select one food item based on the current day
                    if (foodList.isNotEmpty()) {
                        val randomFood = selectFoodForTheDay(foodList) // Select one food for the day
                        adapter.updateList(listOf(randomFood)) // Update adapter with a single item
                        Log.d("MealActivity", "Random food selected for $mealType: ${randomFood.food_desc}")
                    } else {
                        Log.d("MealActivity", "No suitable food options found for $mealType")
                        Toast.makeText(this, "No suitable food options found for $mealType", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching $mealType foods: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("MealActivity", "Error fetching $mealType foods", e)
            }
    }

    // Select food for the day using the current date as a seed
    private fun selectFoodForTheDay(foodList: List<Food>): Food {
        val calendar = Calendar.getInstance()
        val dayOfYear = calendar.get(Calendar.DAY_OF_YEAR) // Get the day of the year

        // Use the day of the year as the seed for random selection
        val random = Random(dayOfYear.toLong())
        return foodList[random.nextInt(foodList.size)] // Select a random food
    }
}
