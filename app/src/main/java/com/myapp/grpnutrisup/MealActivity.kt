package com.myapp.grpnutrisup

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.adapters.FoodAdapter
import com.myapp.grpnutrisup.models.Food
import java.util.*
import java.util.concurrent.TimeUnit

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

        checkHealthComplicationAndProceed()
    }

    private fun checkHealthComplicationAndProceed() {
        val currentUser = auth.currentUser ?: run {
            showToast("User is not logged in")
            return
        }

        val userEmail = currentUser.email ?: return
        db.collection("users").document(userEmail).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val healthComp = document.getString("healthComp") ?: "no"
                    if (healthComp.equals("yes", ignoreCase = true)) {
                        showToast("Meal plans are disabled due to health complications.")
                        finish()
                    } else {
                        val favoriteFoods = document.get("favoriteFoods") as? List<String> ?: emptyList()
                        setupRecyclerViews(favoriteFoods)
                        setupBottomNavigation()
                        fetchMealPlan()
                        setupDailyMealPlanUpdate()
                    }
                } else {
                    showToast("User data not found")
                }
            }
            .addOnFailureListener { e ->
                showToast("Error fetching user data: ${e.message}")
            }
    }

    private fun setupRecyclerViews(favoriteFoods: List<String>) {
        breakfastRecyclerView = findViewById(R.id.breakfastRecyclerView)
        breakfastAdapter = FoodAdapter(this, emptyList(), favoriteFoods.toMutableList()) // Convert to MutableList
        breakfastRecyclerView.layoutManager = LinearLayoutManager(this)
        breakfastRecyclerView.adapter = breakfastAdapter

        lunchRecyclerView = findViewById(R.id.lunchRecyclerView)
        lunchAdapter = FoodAdapter(this, emptyList(), favoriteFoods.toMutableList()) // Convert to MutableList
        lunchRecyclerView.layoutManager = LinearLayoutManager(this)
        lunchRecyclerView.adapter = lunchAdapter

        dinnerRecyclerView = findViewById(R.id.dinnerRecyclerView)
        dinnerAdapter = FoodAdapter(this, emptyList(), favoriteFoods.toMutableList()) // Convert to MutableList
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
        finish()
    }

    private fun fetchMealPlan() {
        val currentUser = auth.currentUser ?: run {
            showToast("User is not logged in")
            return
        }

        val userEmail = currentUser.email ?: return
        db.collection("users").document(userEmail).get()
            .addOnSuccessListener { userDocument ->
                if (userDocument.exists()) {
                    val userAllergies = userDocument.get("allergens") as? List<String> ?: emptyList()
                    val favoriteFoods = userDocument.get("favoriteFoods") as? List<String> ?: emptyList()

                    val calorieResult = userDocument.getDouble("calorieResult") ?: 2000.0
                    val currentCalorieIntake = userDocument.getDouble("calorieIntake") ?: 0.0
                    val proteinIntake = userDocument.getDouble("proteinIntake") ?: 0.0
                    val fatIntake = userDocument.getDouble("fatIntake") ?: 0.0

                    val remainingCalories = calculateRemainingIntake(calorieResult, currentCalorieIntake)
                    val remainingProtein = calculateRemainingIntake(50.0, proteinIntake) // Protein goal
                    val remainingFat = calculateRemainingIntake(70.0, fatIntake) // Fat goal

                    generatePrescriptiveMealPlan(userAllergies, favoriteFoods, remainingCalories, remainingProtein, remainingFat)
                } else {
                    showToast("User data not found")
                }
            }
            .addOnFailureListener { e ->
                showToast("Error fetching user data: ${e.message}")
            }
    }

    private fun calculateRemainingIntake(goal: Double, currentIntake: Double): Double {
        return goal - currentIntake
    }

    private fun generatePrescriptiveMealPlan(
        allergies: List<String>,
        favoriteFoods: List<String>,
        remainingCalories: Double,
        remainingProtein: Double,
        remainingFat: Double
    ) {
        fetchOptimizedFoodForMeal("Breakfast", allergies, favoriteFoods, remainingCalories * 0.3, remainingProtein * 0.3, remainingFat * 0.3) { breakfastList ->
            breakfastAdapter.updateList(breakfastList)
        }

        fetchOptimizedFoodForMeal("Lunch", allergies, favoriteFoods, remainingCalories * 0.4, remainingProtein * 0.4, remainingFat * 0.4) { lunchList ->
            lunchAdapter.updateList(lunchList)
        }

        fetchOptimizedFoodForMeal("Dinner", allergies, favoriteFoods, remainingCalories * 0.3, remainingProtein * 0.3, remainingFat * 0.3) { dinnerList ->
            dinnerAdapter.updateList(dinnerList)
        }
    }

    private fun fetchOptimizedFoodForMeal(
        mealType: String,
        allergies: List<String>,
        favoriteFoods: List<String>,
        calorieGoal: Double,
        proteinGoal: Double,
        fatGoal: Double,
        callback: (List<Food>) -> Unit
    ) {
        db.collection("food_db")
            .whereEqualTo("meal_type", mealType)
            .get()
            .addOnSuccessListener { result ->
                val availableFoods = mutableListOf<Food>()
                for (document in result) {
                    val food = document.toObject(Food::class.java)

                    // Exclude foods with allergens
                    if (!allergies.any { allergen -> food.allergens.contains(allergen, ignoreCase = true) }) {
                        availableFoods.add(food)
                    }
                }

                // Prioritize favorite foods in the selection
                val prioritizedFoods = availableFoods.filter { food -> favoriteFoods.contains(food.food_name) }.ifEmpty { availableFoods }
                val selectedFoods = optimizeFoodSelection(prioritizedFoods, calorieGoal, proteinGoal, fatGoal)
                callback(selectedFoods)
            }
            .addOnFailureListener { e ->
                showToast("Error fetching $mealType foods: ${e.message}")
            }
    }

    private fun optimizeFoodSelection(
        availableFoods: List<Food>,
        calorieGoal: Double,
        proteinGoal: Double,
        fatGoal: Double
    ): List<Food> {
        val selectedFoods = mutableListOf<Food>()
        var totalCalories = 0.0
        var totalProteins = 0.0
        var totalFats = 0.0

        // Greedy algorithm to select foods until matching daily goals
        for (food in availableFoods) {
            if (totalCalories < calorieGoal && totalProteins < proteinGoal && totalFats < fatGoal) {
                selectedFoods.add(food)
                totalCalories += food.calories
                totalProteins += food.proteins
                totalFats += food.fat
            }
        }

        return selectedFoods
    }

    private fun setupDailyMealPlanUpdate() {
        val workRequest = PeriodicWorkRequestBuilder<MealPlanUpdateWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "dailyMealPlanUpdate",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun calculateInitialDelay(): Long {
        val currentTime = Calendar.getInstance()
        val targetTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 8) // 8 AM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
        }

        if (currentTime.after(targetTime)) {
            targetTime.add(Calendar.DAY_OF_MONTH, 1) // Move to next day
        }

        return targetTime.timeInMillis - currentTime.timeInMillis
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

// Worker class to update the meal plan in the background
class MealPlanUpdateWorker(appContext: android.content.Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("MealPlanUpdateWorker", "Updating meal plan in the background")
        // Implement your meal plan update logic here
        return Result.success()
    }
}
