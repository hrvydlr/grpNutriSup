package com.myapp.grpnutrisup.adapters

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.myapp.grpnutrisup.R
import com.myapp.grpnutrisup.models.Food
import java.text.SimpleDateFormat
import java.util.*

class FoodAdapter(
    private val context: Context,
    private var foodList: List<Food>,
    private var favoriteFoodNames: MutableList<String> // MutableList for easier updates
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val todayDate: String by lazy { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.food_item, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foodList[position]

        holder.apply {
            foodNameTextView.text = food.food_name
            foodDescriptionTextView.text = food.food_desc
            foodCaloriesTextView.text = "Calories: ${food.calories}"
            foodCarbsTextView.text = "Carbohydrates: ${food.carbohydrates?.takeIf { it > 0 } ?: "N/A"}"
            foodProteinsTextView.text = "Proteins: ${food.proteins?.takeIf { it > 0 } ?: "N/A"}"
            foodFatsTextView.text = "Fats: ${food.fat?.takeIf { it > 0 } ?: "N/A"}"
            foodAllergenTextView.text = "Allergens: ${food.allergens.ifEmpty { "None" }}"
        }

        Log.d("FoodAdapter", "Fetching image for food: ${food.food_name}, Storage Path: ${food.image_url}")
        fetchImageUrl(food.image_url) { imageUrl ->
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.foodImageView)
        }

        holder.itemView.setOnClickListener {
            showFoodDetailsDialog(food)
        }
    }

    override fun getItemCount() = foodList.size

    fun updateList(newList: List<Food>) {
        foodList = newList
        notifyDataSetChanged()
    }

    private fun fetchImageUrl(storagePath: String, callback: (String) -> Unit) {
        if (storagePath.isNotEmpty()) {
            FirebaseStorage.getInstance().getReference(storagePath)
                .downloadUrl
                .addOnSuccessListener { uri -> callback(uri.toString()) }
                .addOnFailureListener {
                    Log.e("FoodAdapter", "Error fetching image URL: $it")
                    callback("")
                }
        } else {
            callback("")
        }
    }

    private fun showFoodDetailsDialog(food: Food) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_food_details, null)
        val dialog = AlertDialog.Builder(context).apply { setView(dialogView) }.create()

        val foodImageView = dialogView.findViewById<ImageView>(R.id.dialogFoodImageView)
        val foodNameTextView = dialogView.findViewById<TextView>(R.id.dialogFoodNameTextView)
        val foodDescriptionTextView = dialogView.findViewById<TextView>(R.id.dialogFoodDescriptionTextView)
        val foodCaloriesTextView = dialogView.findViewById<TextView>(R.id.dialogFoodCaloriesTextView)
        val foodCarbsTextView = dialogView.findViewById<TextView>(R.id.dialogFoodCarbsTextView)
        val foodProteinsTextView = dialogView.findViewById<TextView>(R.id.dialogFoodProteinsTextView)
        val foodFatsTextView = dialogView.findViewById<TextView>(R.id.dialogFoodFatsTextView)
        val foodAllergenTextView = dialogView.findViewById<TextView>(R.id.dialogFoodAllergenTextView)
        val foodServingSize = dialogView.findViewById<TextView>(R.id.dialogFoodServingSizeTextView)
        val eatenButton = dialogView.findViewById<Button>(R.id.dialogFoodEatenButton)

        foodNameTextView.text = food.food_name
        foodDescriptionTextView.text = food.food_desc
        foodCaloriesTextView.text = "Calories: ${food.calories}"
        foodCarbsTextView.text = "Carbohydrates: ${food.carbohydrates?.takeIf { it > 0 } ?: "N/A"}"
        foodProteinsTextView.text = "Proteins: ${food.proteins?.takeIf { it > 0 } ?: "N/A"}"
        foodFatsTextView.text = "Fats: ${food.fat?.takeIf { it > 0 } ?: "N/A"}"
        foodAllergenTextView.text = "Allergens: ${food.allergens.ifEmpty { "None" }}"
        foodServingSize.text = "Serving Size: ${food.serving_size.ifEmpty { "N/A" }}"

        fetchImageUrl(food.image_url) { imageUrl ->
            Glide.with(context).load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(foodImageView)
        }

        eatenButton.setOnClickListener {
            auth.currentUser?.email?.let { userEmail ->
                saveFoodEaten(userEmail, food)
                updateIntakes(userEmail, food.calories, food.proteins ?: 0, food.fat ?: 0, dialog)
            } ?: run {
                Toast.makeText(context, "Please log in to track your intake", Toast.LENGTH_SHORT).show()
            }
        }

        dialog.show()
    }

    private fun saveFoodEaten(userEmail: String, food: Food) {
        val userEatenFoodsRef = db.collection("users").document(userEmail).collection("eatenFoods")
        val eatenFood = mapOf(
            "food_name" to food.food_name,
            "food_desc" to food.food_desc,
            "calories" to food.calories,
            "carbohydrates" to food.carbohydrates,
            "proteins" to food.proteins,
            "fat" to food.fat,
            "allergens" to food.allergens,
            "timestamp" to com.google.firebase.Timestamp.now()
        )

        userEatenFoodsRef.add(eatenFood)
            .addOnSuccessListener {
                Toast.makeText(context, "${food.food_name} marked as eaten", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to save eaten food", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateIntakes(userEmail: String, calories: Int, proteins: Int, fats: Int, dialog: AlertDialog) {
        val userRef = db.collection("users").document(userEmail)
        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                resetDailyIntakeIfNeeded(userEmail, document)

                // Update the calorie intake with the newly consumed calories
                val updatedCalories = (document.get("calorieIntake") as? Long ?: 0) + calories
                val updatedProteins = (document.get("proteinIntake") as? Long ?: 0) + proteins
                val updatedFats = (document.get("fatIntake") as? Long ?: 0) + fats

                // Calculate the new remaining calories (calorieResult - calorieIntake)
                val calorieResult = document.getDouble("calorieResult")?.toInt() ?: 2000 // Default if not set
                val updatedRemainingCalories = (calorieResult - updatedCalories).coerceAtLeast(0)

                // Calculate calorieGoalForTomorrow using the formula: calorieResult - remainingCalories
                val updatedCalorieGoalForTomorrow = calorieResult + updatedRemainingCalories

                // Update the user's calorie intake, remaining calories, and calorie goal for tomorrow
                userRef.update(mapOf(
                    "calorieIntake" to updatedCalories,
                    "proteinIntake" to updatedProteins,
                    "fatIntake" to updatedFats,
                    "remainingCalories" to updatedRemainingCalories,
                    "calorieGoalForTomorrow" to updatedCalorieGoalForTomorrow
                )).addOnSuccessListener {
                    Toast.makeText(context, "Intake updated and tomorrow's goal recalculated!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }.addOnFailureListener {
                    Toast.makeText(context, "Failed to update intake", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to retrieve user data", Toast.LENGTH_SHORT).show()
        }
    }

    private fun resetDailyIntakeIfNeeded(userEmail: String, document: DocumentSnapshot, testDate: String? = null) {
        val today = testDate ?: todayDate // Use testDate for testing, or the actual date
        val lastUpdateDate = document.getString("lastUpdateDate") ?: today
        val calorieResult = document.getDouble("calorieResult")?.toInt() ?: 2000 // Default daily goal
        val calorieIntake = document.getLong("calorieIntake")?.toInt() ?: 0
        val calorieGoalForTomorrow = document.getLong("calorieGoalForTomorrow")?.toInt() ?: calorieResult

        if (lastUpdateDate != today) {
            // Calculate remaining calories for the day
            val remainingCalories = (calorieResult - calorieIntake).coerceAtLeast(0)

            // Calculate tomorrow's calorie goal based on the formula: calorieResult - remainingCalories
            val updatedCalorieGoalForTomorrow = calorieResult - remainingCalories

            val updates = mapOf(
                "calorieIntake" to 0,
                "proteinIntake" to 0,
                "fatIntake" to 0,
                "remainingCalories" to remainingCalories,
                "lastUpdateDate" to today,
                "calorieGoalForToday" to calorieGoalForTomorrow,
                "calorieGoalForTomorrow" to updatedCalorieGoalForTomorrow
            )

            db.collection("users").document(userEmail)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener {
                    Log.d("FoodAdapter", "Reset daily intake and updated calorie goals for new day.")
                }
                .addOnFailureListener { e ->
                    Log.e("FoodAdapter", "Error resetting daily intake: ", e)
                }
        }
    }


    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodImageView: ImageView = itemView.findViewById(R.id.foodImageView)
        val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
        val foodDescriptionTextView: TextView = itemView.findViewById(R.id.foodDescriptionTextView)
        val foodCaloriesTextView: TextView = itemView.findViewById(R.id.foodCaloriesTextView)
        val foodCarbsTextView: TextView = itemView.findViewById(R.id.foodCarbsTextView)
        val foodProteinsTextView: TextView = itemView.findViewById(R.id.foodProteinsTextView)
        val foodFatsTextView: TextView = itemView.findViewById(R.id.foodFatsTextView)
        val foodAllergenTextView: TextView = itemView.findViewById(R.id.foodAllergenTextView)
    }
}
