package com.myapp.grpnutrisup.adapters

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.myapp.grpnutrisup.R
import com.myapp.grpnutrisup.models.Meal
import java.text.SimpleDateFormat
import java.util.*

class MealHistoryAdapter(
    private val context: Context,
    private var mealList: MutableList<Meal> // Made mutable for easier updating on deletion
) : RecyclerView.Adapter<MealHistoryAdapter.MealViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MealViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_meal_history, parent, false)
        return MealViewHolder(view)
    }

    override fun onBindViewHolder(holder: MealViewHolder, position: Int) {
        val meal = mealList[position]

        holder.apply {
            foodNameTextView.text = meal.food_name
            caloriesTextView.text = "Calories: ${meal.calories}"
            carbsTextView.text = "Carbohydrates: ${meal.carbohydrates}"
            proteinsTextView.text = "Proteins: ${meal.proteins}"
            fatsTextView.text = "Fats: ${meal.fat}"
            allergensTextView.text = "Allergens: ${meal.allergens.ifEmpty { "None" }}"
            timestampTextView.text = meal.timestamp?.let { formatTimestamp(it) } ?: "N/A"

            // Set up item click listener for delete confirmation dialog
            itemView.setOnClickListener {
                showDeleteConfirmationDialog(meal, position)
            }
        }
    }

    override fun getItemCount() = mealList.size

    // Format timestamp for display
    private fun formatTimestamp(timestamp: com.google.firebase.Timestamp): String {
        val date = timestamp.toDate()
        val sdf = SimpleDateFormat("MMM dd, yyyy - HH:mm", Locale.getDefault())
        return sdf.format(date)
    }

    // Show dialog to confirm deletion of a meal
    private fun showDeleteConfirmationDialog(meal: Meal, position: Int) {
        AlertDialog.Builder(context).apply {
            setTitle("Remove Meal")
            setMessage("Are you sure you want to remove ${meal.food_name} from your history?")
            setPositiveButton("Yes") { _, _ ->
                deleteMealFromHistory(meal, position)
            }
            setNegativeButton("No", null)
            show()
        }
    }

    // Delete meal and update intake values in Firestore
    private fun deleteMealFromHistory(meal: Meal, position: Int) {
        auth.currentUser?.let { user ->
            val userRef = db.collection("users").document(user.email ?: "")

            // Step 1: Retrieve current intake values
            userRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val currentCalories = (document.get("calorieIntake") as? Long ?: 0L).toInt()
                    val currentProteins = (document.get("proteinIntake") as? Long ?: 0L).toInt()
                    val currentFats = (document.get("fatIntake") as? Long ?: 0L).toInt()

                    // Step 2: Calculate updated intake after removing meal
                    val updatedCalories = (currentCalories - meal.calories).coerceAtLeast(0)
                    val updatedProteins = (currentProteins - meal.proteins).coerceAtLeast(0)
                    val updatedFats = (currentFats - meal.fat).coerceAtLeast(0)

                    // Step 3: Update intake values in Firestore
                    userRef.update(
                        mapOf(
                            "calorieIntake" to updatedCalories,
                            "proteinIntake" to updatedProteins,
                            "fatIntake" to updatedFats
                        )
                    ).addOnSuccessListener {
                        // Step 4: Delete the meal from the meal history collection
                        userRef.collection("eatenFoods")
                            .whereEqualTo("food_name", meal.food_name)
                            .get()
                            .addOnSuccessListener { querySnapshot ->
                                if (!querySnapshot.isEmpty) {
                                    // Iterate over the matched documents and delete them
                                    for (document in querySnapshot.documents) {
                                        document.reference.delete()
                                            .addOnSuccessListener {
                                                Toast.makeText(context, "${meal.food_name} removed from history.", Toast.LENGTH_SHORT).show()
                                                // Remove the item from the list and notify adapter
                                                mealList.removeAt(position)
                                                notifyItemRemoved(position)
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("MealHistoryAdapter", "Error deleting meal: ${e.message}")
                                                Toast.makeText(context, "Failed to delete meal.", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                } else {
                                    Toast.makeText(context, "Meal not found in history.", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("MealHistoryAdapter", "Error querying meals: ${e.message}")
                                Toast.makeText(context, "Failed to find meal.", Toast.LENGTH_SHORT).show()
                            }

                    }.addOnFailureListener {
                        Toast.makeText(context, "Failed to update intake values.", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to retrieve user intake data.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(context, "User not logged in.", Toast.LENGTH_SHORT).show()
        }
    }

    // ViewHolder class for meal history items
    class MealViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
        val caloriesTextView: TextView = itemView.findViewById(R.id.caloriesTextView)
        val carbsTextView: TextView = itemView.findViewById(R.id.carbsTextView)
        val proteinsTextView: TextView = itemView.findViewById(R.id.proteinsTextView)
        val fatsTextView: TextView = itemView.findViewById(R.id.fatsTextView)
        val allergensTextView: TextView = itemView.findViewById(R.id.allergensTextView)
        val timestampTextView: TextView = itemView.findViewById(R.id.timestampTextView)
    }
}
