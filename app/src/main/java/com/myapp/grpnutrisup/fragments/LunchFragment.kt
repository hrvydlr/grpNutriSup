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

class LunchFragment : Fragment() {

    private lateinit var lunchRecyclerView: RecyclerView
    private lateinit var lunchAdapter: FoodAdapter
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // List of food names to exclude
    private val excludedFoodNames = listOf("Rice", "Half Rice", "Half Fried Rice", "Fried Rice", "Sinangag")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_lunch, container, false)
        initializeRecyclerView(view)
        loadUserDataAndFetchFood()
        return view
    }

    private fun initializeRecyclerView(view: View) {
        lunchRecyclerView = view.findViewById(R.id.lunchRecyclerView)
        lunchAdapter = FoodAdapter(requireContext(), emptyList(), mutableListOf())
        lunchRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = lunchAdapter
        }
    }

    private fun loadUserDataAndFetchFood() {
        fetchUserData { calorieResult, goal, allergens ->
            Log.d("LunchFragment", "User Calorie Result: $calorieResult, Goal: $goal, Allergens: $allergens")

            // Fetch fresh data for the current meal without caching
            fetchFreshFoods("Lunch", allergens, goal) { foods ->
                lunchAdapter.updateList(foods)
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
                    Log.d("LunchFragment", "User Data: TDEE=$calorieResult, Goal=$goal, Allergens=$allergens")
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

        db.collection("food_db")
            .whereEqualTo("meal_type", mealType)
            .whereEqualTo("goal_type", userGoal)
            .get()
            .addOnSuccessListener { result ->
                val foods = result.mapNotNull { document ->
                    document.toObject(Food::class.java).takeIf { food ->
                        allergens.none { allergen ->
                            food.allergens.contains(allergen, ignoreCase = true)
                        } && food.food_name !in excludedFoodNames
                    }
                }

                // Select foods for lunch
                val selectedFoods = foods

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
