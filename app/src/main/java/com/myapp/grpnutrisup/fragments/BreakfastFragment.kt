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

class BreakfastFragment : Fragment() {

    private lateinit var breakfastRecyclerView: RecyclerView
    private lateinit var breakfastAdapter: FoodAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // List of food names to exclude (no longer used)
    private val excludedFoodNames = listOf("Rice", "Half Rice", "Half Fried Rice", "Fried Rice", "Sinangag")

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
        fetchUserData { calorieResult, goal, allergens ->
            Log.d("BreakfastFragment", "User Calorie Result: $calorieResult, Goal: $goal, Allergens: $allergens")

            // Fetch fresh data for the current meal without caching
            fetchFreshFoods("Breakfast", allergens, goal) { foods ->
                breakfastAdapter.updateList(foods)
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
                    val calorieResult = document.getDouble("calorieResult") ?: 2000.0
                    val goal = document.getString("goal") ?: "Maintain"
                    val allergens = document.get("allergens") as? List<String> ?: emptyList()

                    // Log the user's details
                    Log.d("BreakfastFragment", "User Data: TDEE=$calorieResult, Goal=$goal, Allergens=$allergens")
                    callback(calorieResult, goal, allergens)
                } else {
                    showToast("User data not found.")
                }
            }
            .addOnFailureListener { e ->
                showToast("Error fetching user data: ${e.message}")
            }
    }

    private fun fetchFreshFoods(
        mealType: String,
        allergens: List<String>,
        userGoal: String,
        callback: (List<Food>) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: return
        val userFoodSelectionsRef = db.collection("daily_food_selections").document(userId)

        // Fetch all food items for the given meal type and goal
        db.collection("food_db")
            .whereEqualTo("meal_type", mealType)  // Get all foods for breakfast
            .whereEqualTo("goal_type", userGoal)  // Filter by goal type
            .get()
            .addOnSuccessListener { result ->

                // Filter foods based on allergens (and any other criteria if needed)
                val foods = result.mapNotNull { document ->
                    document.toObject(Food::class.java).takeIf { food ->
                        allergens.none { allergen ->
                            food.allergens.contains(allergen, ignoreCase = true)
                        }
                    }
                }

                // Ensure we always get a random selection each day
                val currentDate = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
                val selectedFoods = foods.shuffled().take(4)  // Take 4 random foods

                // Save the selected foods in Firestore with today's date
                val foodData = selectedFoods.map { it.toHashMap() }
                val updateData = mapOf("date" to currentDate, mealType to foodData)

                // Save the food selection for today
                userFoodSelectionsRef.set(updateData, SetOptions.merge())

                // Call the callback to update the UI
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