package com.myapp.grpnutrisup.adapters

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.R
import com.myapp.grpnutrisup.models.Meal
import com.myapp.grpnutrisup.models.MealGroup

class MealHistoryAdapter(
    private val context: Context,
    private var mealDateGroupList: MutableList<MealGroup> // List of grouped meals
) : RecyclerView.Adapter<MealHistoryAdapter.MealViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_meal_group, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val group = mealDateGroupList[position]
        holder.bind(group)

        // Set up click listener for deletion
        holder.itemView.setOnClickListener {
            showDeleteConfirmationDialog(group, position)
        }
    }

    override fun getItemCount(): Int = mealDateGroupList.size

    // Show a dialog to confirm the deletion
    private fun showDeleteConfirmationDialog(group: MealGroup, position: Int) {
        AlertDialog.Builder(context).apply {
            setTitle("Remove Meals")
            setMessage("Are you sure you want to remove all meals on ${group.date} from your history?")
            setPositiveButton("Yes") { _, _ -> deleteMealsFromHistory(group, position) }
            setNegativeButton("No", null)
            show()
        }
    }

    // Deletes all meals for the given date and updates UI
    private fun deleteMealsFromHistory(group: MealGroup, position: Int) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
            return
        }

        val userEmail = currentUser.email ?: run {
            Toast.makeText(context, "User email not found.", Toast.LENGTH_SHORT).show()
            return
        }

        // Step 1: Retrieve current intake values
        db.collection("users").document(userEmail).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentCalories = (document.getLong("calorieIntake") ?: 0L).toInt()
                    val currentProteins = (document.getLong("proteinIntake") ?: 0L).toInt()
                    val currentFats = (document.getLong("fatIntake") ?: 0L).toInt()
                    val calorieResult = (document.getLong("calorieResult") ?: 2000L).toInt()

                    // Calculate updated values after removing meals
                    var updatedCalories = currentCalories
                    var updatedProteins = currentProteins
                    var updatedFats = currentFats

                    group.meals.forEach { meal ->
                        updatedCalories -= meal.calories
                        updatedProteins -= meal.proteins
                        updatedFats -= meal.fat
                    }

                    updatedCalories = updatedCalories.coerceAtLeast(0)
                    updatedProteins = updatedProteins.coerceAtLeast(0)
                    updatedFats = updatedFats.coerceAtLeast(0)

                    val remainingCalories = (calorieResult - updatedCalories).coerceAtLeast(0)
                    val updatedCalorieGoalForTomorrow = calorieResult + remainingCalories

                    // Update intake and calorie goal in Firestore
                    db.collection("users").document(userEmail).update(
                        mapOf(
                            "calorieIntake" to updatedCalories,
                            "proteinIntake" to updatedProteins,
                            "fatIntake" to updatedFats,
                            "remainingCalories" to remainingCalories,
                            "calorieGoalForTomorrow" to updatedCalorieGoalForTomorrow
                        )
                    ).addOnSuccessListener {
                        deleteMealsFromFirestore(userEmail, group, position)
                    }.addOnFailureListener { e ->
                        Log.e("MealHistoryAdapter", "Failed to update intake values: ${e.localizedMessage}")
                        Toast.makeText(context, "Failed to update intake values.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(context, "User data not found.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("MealHistoryAdapter", "Failed to retrieve user data: ${e.localizedMessage}")
                Toast.makeText(context, "Failed to retrieve user intake data.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun deleteMealsFromFirestore(userEmail: String, group: MealGroup, position: Int) {
        db.collection("users").document(userEmail)
            .collection("eatenFoods")
            .whereIn("food_name", group.meals.map { it.food_name })
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val batch = db.batch()
                    querySnapshot.documents.forEach { document ->
                        batch.delete(document.reference)
                    }
                    batch.commit()
                        .addOnSuccessListener {
                            // Notify UI about successful deletion
                            Toast.makeText(context, "Meals from ${group.date} removed from history.", Toast.LENGTH_SHORT).show()
                            mealDateGroupList.removeAt(position)
                            notifyItemRemoved(position)
                        }
                        .addOnFailureListener { e ->
                            Log.e("MealHistoryAdapter", "Error deleting meals: ${e.localizedMessage}")
                            Toast.makeText(context, "Failed to delete meals.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Meals not found for this date.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("MealHistoryAdapter", "Error querying meals: ${e.localizedMessage}")
                Toast.makeText(context, "Failed to query meals.", Toast.LENGTH_SHORT).show()
            }
    }

    // ViewHolder class for grouped meals
    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val totalCaloriesTextView: TextView = itemView.findViewById(R.id.totalCaloriesTextView)
        private val mealsRecyclerView: RecyclerView = itemView.findViewById(R.id.recyclerViewMeals)

        fun bind(group: MealGroup) {
            dateTextView.text = group.date
            totalCaloriesTextView.text = "Total Calories: ${group.totalCalories}"

            // Set up the inner RecyclerView for meals on that day
            val mealAdapter = InnerMealAdapter(group.meals)
            mealsRecyclerView.apply {
                layoutManager = LinearLayoutManager(itemView.context)
                adapter = mealAdapter
            }
        }
    }

    // Inner Adapter for displaying meals for each day
    class InnerMealAdapter(private val meals: List<Meal>) : RecyclerView.Adapter<InnerMealAdapter.InnerMealViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InnerMealViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_meal_history, parent, false)
            return InnerMealViewHolder(view)
        }

        override fun onBindViewHolder(holder: InnerMealViewHolder, position: Int) {
            val meal = meals[position]
            holder.bind(meal)
        }

        override fun getItemCount(): Int = meals.size

        class InnerMealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            private val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
            private val caloriesTextView: TextView = itemView.findViewById(R.id.caloriesTextView)
            private val carbsTextView: TextView = itemView.findViewById(R.id.carbsTextView)
            private val proteinsTextView: TextView = itemView.findViewById(R.id.proteinsTextView)
            private val fatsTextView: TextView = itemView.findViewById(R.id.fatsTextView)
            private val allergenTextView: TextView = itemView.findViewById(R.id.allergensTextView)

            fun bind(meal: Meal) {
                foodNameTextView.text = meal.food_name
                caloriesTextView.text = "Calories: ${meal.calories}"
                carbsTextView.text = "Carbohydrates: ${meal.carbohydrates}"
                proteinsTextView.text = "Proteins: ${meal.proteins}"
                fatsTextView.text = "Fats: ${meal.fat}"
                allergenTextView.text = "Allergens: ${meal.allergens}"
            }
        }
    }
}
