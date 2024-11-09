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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.myapp.grpnutrisup.R
import com.myapp.grpnutrisup.models.Food

class FoodAdapter(
    private val context: Context,
    private var foodList: List<Food>,
    private var favoriteFoodNames: MutableList<String> // MutableList for easier updates
) : RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    // Initializing Firestore and FirebaseAuth once, reducing repeated initializations
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.food_item, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foodList[position]

        // Populate food details efficiently using string templates
        holder.apply {
            foodNameTextView.text = food.food_name
            foodDescriptionTextView.text = food.food_desc
            foodCaloriesTextView.text = "Calories: ${food.calories}"
            foodCarbsTextView.text = "Carbohydrates: ${food.carbohydrates?.takeIf { it > 0 } ?: "N/A"}"
            foodProteinsTextView.text = "Proteins: ${food.proteins?.takeIf { it > 0 } ?: "N/A"}"
            foodFatsTextView.text = "Fats: ${food.fat?.takeIf { it > 0 } ?: "N/A"}"
            foodAllergenTextView.text = "Allergens: ${food.allergens.ifEmpty { "None" }}"
        }

        // Log the image path for debugging
        Log.d("FoodAdapter", "Fetching image for food: ${food.food_name}, Storage Path: ${food.image_url}")

        // Efficiently fetch and load the image from Firebase Storage
        fetchImageUrl(food.image_url) { imageUrl ->
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(holder.foodImageView)
        }

        // Item click listener for showing food details in a dialog
        holder.itemView.setOnClickListener {
            showFoodDetailsDialog(food)
        }
    }

    override fun getItemCount() = foodList.size

    // Update food list efficiently
    fun updateList(newList: List<Food>) {
        foodList = newList
        notifyDataSetChanged()
    }

    // Optimized image fetching method using Firebase Storage
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

    // Show food details in a dialog and handle marking food as eaten
    private fun showFoodDetailsDialog(food: Food) {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_food_details, null)

        // Build the AlertDialog and store it in a variable
        val dialog = AlertDialog.Builder(context).apply {
            setView(dialogView)
        }.create()

        // Initialize dialog components
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

        // Set dialog values
        foodNameTextView.text = food.food_name
        foodDescriptionTextView.text = food.food_desc
        foodCaloriesTextView.text = "Calories: ${food.calories}"
        foodCarbsTextView.text = "Carbohydrates: ${food.carbohydrates?.takeIf { it > 0 } ?: "N/A"}"
        foodProteinsTextView.text = "Proteins: ${food.proteins?.takeIf { it > 0 } ?: "N/A"}"
        foodFatsTextView.text = "Fats: ${food.fat?.takeIf { it > 0 } ?: "N/A"}"
        foodAllergenTextView.text = "Allergens: ${food.allergens.ifEmpty { "None" }}"
        foodServingSize.text = "Serving Size: ${food.serving_size.ifEmpty { "N/A" }}"

        // Load image
        fetchImageUrl(food.image_url) { imageUrl ->
            Glide.with(context).load(imageUrl)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.error_image)
                .into(foodImageView)
        }

        // Handle 'Eaten' button click
        eatenButton.setOnClickListener {
            auth.currentUser?.email?.let { userEmail ->
                saveFoodEaten(userEmail, food) // Save eaten food to Firestore
                updateIntakes(userEmail, food.calories, food.proteins ?: 0, food.fat ?: 0, dialog)
            } ?: run {
                Toast.makeText(context, "Please log in to track your intake", Toast.LENGTH_SHORT).show()
            }
        }

        // Show the dialog
        dialog.show()
    }

    // Save food eaten details to Firestore
    private fun saveFoodEaten(userEmail: String, food: Food) {
        val userEatenFoodsRef = db.collection("users").document(userEmail).collection("eatenFoods")

        // Create a map of the food details with a timestamp
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

        // Add the eaten food entry to the user's "eatenFoods" collection
        userEatenFoodsRef.add(eatenFood)
            .addOnSuccessListener {
                Toast.makeText(context, "${food.food_name} marked as eaten", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(context, "Failed to save eaten food", Toast.LENGTH_SHORT).show()
            }
    }

    // Update user's calorie, protein, and fat intake and close dialog upon completion
    private fun updateIntakes(userEmail: String, calories: Int, proteins: Int, fats: Int, dialog: AlertDialog) {
        val userRef = db.collection("users").document(userEmail)

        userRef.get().addOnSuccessListener { document ->
            if (document.exists()) {
                val updatedCalories = (document.get("calorieIntake") as? Long ?: 0) + calories
                val updatedProteins = (document.get("proteinIntake") as? Long ?: 0) + proteins
                val updatedFats = (document.get("fatIntake") as? Long ?: 0) + fats

                // Batch updating fields for consistency
                userRef.update(mapOf(
                    "calorieIntake" to updatedCalories,
                    "proteinIntake" to updatedProteins,
                    "fatIntake" to updatedFats
                )).addOnSuccessListener {
                    Toast.makeText(context, "Intake updated!", Toast.LENGTH_SHORT).show()

                    // Close the dialog after updating
                    dialog.dismiss()

                }.addOnFailureListener {
                    Toast.makeText(context, "Failed to update intake", Toast.LENGTH_SHORT).show()
                }
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to retrieve user data", Toast.LENGTH_SHORT).show()
        }
    }

    // ViewHolder class to represent each food item
    inner class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
        val foodDescriptionTextView: TextView = itemView.findViewById(R.id.foodDescriptionTextView)
        val foodCaloriesTextView: TextView = itemView.findViewById(R.id.foodCaloriesTextView)
        val foodCarbsTextView: TextView = itemView.findViewById(R.id.foodCarbsTextView)
        val foodProteinsTextView: TextView = itemView.findViewById(R.id.foodProteinsTextView)
        val foodFatsTextView: TextView = itemView.findViewById(R.id.foodFatsTextView)
        val foodAllergenTextView: TextView = itemView.findViewById(R.id.foodAllergenTextView)
        val foodImageView: ImageView = itemView.findViewById(R.id.foodImageView)
    }
}
