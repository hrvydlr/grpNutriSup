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
import com.google.firebase.firestore.SetOptions
import com.myapp.grpnutrisup.R
import com.myapp.grpnutrisup.adapters.FoodAdapter
import com.myapp.grpnutrisup.models.Food
import java.util.*

class DinnerFragment : Fragment() {

    private lateinit var dinnerRecyclerView: RecyclerView
    private lateinit var dinnerAdapter: FoodAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // List of food names to exclude
    private val excludedFoodNames = listOf("Rice", "Half Rice", "Half Fried Rice", "Fried Rice", "Sinangag")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_dinner, container, false)
        initializeRecyclerView(view)
        loadUserDataAndFetchFood()
        return view
    }

    private fun initializeRecyclerView(view: View) {
        dinnerRecyclerView = view.findViewById(R.id.dinnerRecyclerView)
        dinnerAdapter = FoodAdapter(requireContext(), emptyList(), mutableListOf())
        dinnerRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = dinnerAdapter
        }
    }

    private fun loadUserDataAndFetchFood() {
        fetchUserData { calorieResult, goal, allergens ->
            Log.d("DinnerFragment", "User Calorie Result: $calorieResult, Goal: $goal, Allergens: $allergens")
            // Fetch fresh data for the current meal without caching
            fetchFilteredFoods("Dinner", allergens, goal) { foods ->
                if (foods.isNotEmpty()) {
                    dinnerAdapter.updateList(foods)
                } else {
                    showToast("No foods found for dinner")
                }
            }
        }
    }

    private fun fetchUserData(callback: (Double, String, List<String>) -> Unit) {
        val userEmail = auth.currentUser?.email ?: return
        if (userEmail == null) {
            showToast("User not logged in.")
            return
        }

        db.collection("users").document(userEmail)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val calorieResult = document.getDouble("calorieGoalForToday") ?: 2000.0
                    val goal = document.getString("goal") ?: "Maintain"
                    val allergens = document.get("allergens") as? List<String> ?: emptyList()

                    // Log the user's details
                    Log.d("DinnerFragment", "User Data: Calorie Result=$calorieResult, Goal=$goal, Allergens=$allergens")
                    callback(calorieResult, goal, allergens)
                } else {
                    showToast("User data not found.")
                }
            }
            .addOnFailureListener { e ->
                showToast("Error fetching user data: ${e.message}")
            }
    }

    private fun fetchFilteredFoods(
        mealType: String,
        allergens: List<String>,
        userGoal: String,
        callback: (List<Food>) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        val userFoodSelectionsRef = db.collection("daily_food_selections").document(userId) // Define reference here

        userFoodSelectionsRef.get().addOnSuccessListener { document ->
            val currentDate = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
            val lastUpdateDate = document.getLong("date")?.toInt()

            // Log date and current selection
            Log.d("DinnerFragment", "Last update date: $lastUpdateDate, current date: $currentDate")

            if (lastUpdateDate == currentDate && document.contains(mealType)) {
                // Load stored foods if they're from today
                val storedFoods = document.get(mealType) as? List<HashMap<String, Any>> ?: emptyList()
                val selectedFoods = storedFoods.mapNotNull { Food.fromHashMap(it) }

                Log.d("DinnerFragment", "Loaded stored foods: ${selectedFoods.size}")
                if (selectedFoods.isNotEmpty()) {
                    callback(selectedFoods)
                } else {
                    showToast("No foods found for dinner, fetching fresh foods...")
                    fetchFreshFoods(mealType, allergens, userGoal, callback)
                }
            } else {
                // If data is outdated or doesn't exist, fetch fresh data
                fetchFreshFoods(mealType, allergens, userGoal, callback)
            }
        }.addOnFailureListener { e ->
            Log.e("FetchFoods", "Error fetching stored selections: ${e.message}")
            showToast("Error fetching stored selections: ${e.message}")
        }
    }

    private fun fetchFreshFoods(
        mealType: String,
        allergens: List<String>,
        userGoal: String,
        callback: (List<Food>) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        val userFoodSelectionsRef = db.collection("daily_food_selections").document(userId) // Define reference here too

        db.collection("food_db")
            .whereEqualTo("meal_type", mealType)
            .whereEqualTo("goal_type", userGoal)
            .get()
            .addOnSuccessListener { result ->
                Log.d("DinnerFragment", "Fetched foods: ${result.size()}")
                val foods = result.mapNotNull { document ->
                    document.toObject(Food::class.java).takeIf { food ->
                        allergens.none { allergen ->
                            food.allergens.contains(allergen, ignoreCase = true)
                        } && food.food_name !in excludedFoodNames
                    }
                }

                // Select foods for Dinner
                val selectedFoods = foods
                Log.d("DinnerFragment", "Selected foods: ${selectedFoods.size}")

                // Save selection with today's date
                val currentDate = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                val foodData = selectedFoods.map { it.toHashMap() }
                val updateData = mapOf("date" to currentDate, mealType to foodData)

                userFoodSelectionsRef.set(updateData, SetOptions.merge())
                callback(selectedFoods)
            }
            .addOnFailureListener { e ->
                Log.e("FetchFoods", "Error fetching $mealType foods: ${e.message}")
                showToast("Error fetching $mealType foods: ${e.message}")
            }
    }


    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}
