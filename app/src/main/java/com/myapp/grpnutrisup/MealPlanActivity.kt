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

@Suppress("DEPRECATION")
class MealPlanActivity : AppCompatActivity() {

    // RecyclerViews for each meal
    private lateinit var breakfastRecyclerView: RecyclerView
    private lateinit var lunchRecyclerView: RecyclerView
    private lateinit var dinnerRecyclerView: RecyclerView

    // Adapters for each meal
    private lateinit var breakfastAdapter: FoodAdapter
    private lateinit var lunchAdapter: FoodAdapter
    private lateinit var dinnerAdapter: FoodAdapter

    // Firebase instances
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meal_plan)

        Log.d("MealPlanActivity", "Layout set, initializing views.")

        // Initialize RecyclerViews and Adapters
        setupRecyclerViews()

        // Setup bottom navigation after initializing views
        setupBottomNavigation()

        // Fetch meal plan data
        fetchMealPlan()
    }



    /**
     * Setup RecyclerViews and their adapters.
     */
    private fun setupRecyclerViews() {
        breakfastRecyclerView = findViewById(R.id.breakfastRecyclerView)
        lunchRecyclerView = findViewById(R.id.lunchRecyclerView)
        dinnerRecyclerView = findViewById(R.id.dinnerRecyclerView)

        // Initialize adapters with empty lists
        breakfastAdapter = FoodAdapter(this, emptyList())
        lunchAdapter = FoodAdapter(this, emptyList())
        dinnerAdapter = FoodAdapter(this, emptyList())

        // Set LayoutManager and adapters for RecyclerViews
        breakfastRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MealPlanActivity)
            adapter = breakfastAdapter
        }

        lunchRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MealPlanActivity)
            adapter = lunchAdapter
        }

        dinnerRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MealPlanActivity)
            adapter = dinnerAdapter
        }
    }

    /**
     * Setup bottom navigation and handle navigation events.
     */
    private fun setupBottomNavigation() {
        val bottomNav: BottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNav.setOnNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.navigation_home -> {
                    navigateTo(MainActivity::class.java)
                    true
                }
                R.id.navigation_search -> {
                    navigateTo(FoodSearchActivity::class.java)
                    true
                }
                R.id.navigation_meal -> {
                    // Current activity, no action needed
                    true
                }
                R.id.navigation_profile -> {
                    navigateTo(ProfileActivity::class.java)
                    true
                }
                else -> false
            }
        }
    }

    /**
     * Helper function to navigate between activities.
     */
    private fun navigateTo(destination: Class<*>) {
        val intent = Intent(this, destination)
        startActivity(intent)
        finish() // Close current activity to prevent stack buildup
    }

    /**
     * Fetch the user's meal plan from Firestore based on their goal and allergies.
     */
    private fun fetchMealPlan() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User is not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        // Get user data from Firestore
        val userEmail = currentUser.email ?: return
        db.collection("users").document(userEmail).get()
            .addOnSuccessListener { userDocument ->
                if (userDocument.exists()) {
                    val userGoal = userDocument.getString("goal") ?: ""
                    val userAllergies = userDocument.get("allergens") as? List<String> ?: emptyList()

                    // Fetch foods based on the user's goal
                    fetchFoodsBasedOnGoal(userGoal, userAllergies)
                } else {
                    Toast.makeText(this, "User data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching user data: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * Fetch foods from Firestore based on the user's goal and allergies.
     */
    private fun fetchFoodsBasedOnGoal(goal: String, allergies: List<String>) {
        db.collection("food_db")
            .whereEqualTo("goal_type", goal)
            .get()
            .addOnSuccessListener { result ->
                val breakfastList = mutableListOf<Food>()
                val lunchList = mutableListOf<Food>()
                val dinnerList = mutableListOf<Food>()

                for (document in result) {
                    val food = document.toObject(Food::class.java)

                    // Filter out foods based on user's allergies
                    if (!allergies.any { food.description.contains(it, ignoreCase = true) }) {
                        when (food.mealType) {
                            "Breakfast" -> breakfastList.add(food)
                            "Lunch" -> lunchList.add(food)
                            "Dinner" -> dinnerList.add(food)
                        }
                    }
                }

                // Update RecyclerViews with fetched food items
                breakfastAdapter.updateList(breakfastList)
                lunchAdapter.updateList(lunchList)
                dinnerAdapter.updateList(dinnerList)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching meal plan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
