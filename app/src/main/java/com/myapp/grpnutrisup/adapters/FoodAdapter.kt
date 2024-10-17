package com.myapp.grpnutrisup.adapters

import android.app.AlertDialog
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
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

class FoodAdapter(private val context: Context, private var foodList: List<Food>) :
    RecyclerView.Adapter<FoodAdapter.FoodViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.food_item, parent, false)
        return FoodViewHolder(view)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        val food = foodList[position]

        // Populate food details
        holder.foodNameTextView.text = food.food_name
        holder.foodDescriptionTextView.text = food.food_desc
        holder.foodCaloriesTextView.text = "Calories: ${food.calories}"
        holder.foodCarbsTextView.text = "Carbohydrates: ${food.carbohydrate?.takeIf { it > 0 } ?: "N/A"}"
        holder.foodProteinsTextView.text = "Proteins: ${food.proteins?.takeIf { it > 0 } ?: "N/A"}"
        holder.foodFatsTextView.text = "Fats: ${food.fat?.takeIf { it > 0 } ?: "N/A"}"
        holder.foodAllergenTextView.text = "Allergens: ${food.allergens.ifEmpty { "None" }}"

        // Log the storage path for debugging
        Log.d("FoodAdapter", "Fetching image for food: ${food.food_name}, Storage Path: ${food.imageUrl}")

        // Fetch and display the food image from Firebase Storage
        fetchImageUrl(food.imageUrl) { imageUrl ->
            if (imageUrl.isNotEmpty()) {
                Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image) // Placeholder image during loading
                    .error(R.drawable.error_image) // Error image if fetching fails
                    .into(holder.foodImageView)
            } else {
                Log.e("FoodAdapter", "Image URL is empty or invalid for food: ${food.food_name}")
                holder.foodImageView.setImageResource(R.drawable.error_image)
            }
        }

        // Handle item click to show details dialog
        holder.itemView.setOnClickListener {
            showFoodDetailsDialog(food)
        }

        // Handle add to favorites functionality
        holder.favoriteButton.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                addToFavorites(currentUser.email!!, food)
            } else {
                Toast.makeText(context, "Please log in to add favorites", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int {
        return foodList.size
    }

    fun updateList(newList: List<Food>) {
        foodList = newList
        notifyDataSetChanged()
    }

    private fun fetchImageUrl(storagePath: String, callback: (String) -> Unit) {
        if (storagePath.isNotEmpty()) {
            val storageReference = FirebaseStorage.getInstance().getReference(storagePath)
            storageReference.downloadUrl.addOnSuccessListener { uri ->
                callback(uri.toString())
            }.addOnFailureListener { exception ->
                Log.e("FoodAdapter", "Error fetching image URL: $exception")
                callback("")
            }
        } else {
            callback("")
        }
    }

    private fun addToFavorites(userEmail: String, food: Food) {
        val userFavoritesRef = db.collection("users").document(userEmail)
        userFavoritesRef.get().addOnSuccessListener { documentSnapshot ->
            val favoriteFoods = documentSnapshot.get("favoriteFoods") as? MutableList<String> ?: mutableListOf()

            if (favoriteFoods.contains(food.food_name)) {
                Toast.makeText(context, "${food.food_name} is already in your favorites", Toast.LENGTH_SHORT).show()
            } else {
                // Add the food to favorites and update in Firestore
                favoriteFoods.add(food.food_name)
                userFavoritesRef.update("favoriteFoods", favoriteFoods)
                    .addOnSuccessListener {
                        Toast.makeText(context, "${food.food_name} added to your favorites", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to add favorite", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to retrieve favorites", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showFoodDetailsDialog(food: Food) {
        // Inflate the custom layout for dialog
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_food_details, null)

        // Initialize dialog components
        val foodImageView = dialogView.findViewById<ImageView>(R.id.dialogFoodImageView)
        val foodNameTextView = dialogView.findViewById<TextView>(R.id.dialogFoodNameTextView)
        val foodDescriptionTextView = dialogView.findViewById<TextView>(R.id.dialogFoodDescriptionTextView)
        val foodCaloriesTextView = dialogView.findViewById<TextView>(R.id.dialogFoodCaloriesTextView)
        val foodCarbsTextView = dialogView.findViewById<TextView>(R.id.dialogFoodCarbsTextView)
        val foodProteinsTextView = dialogView.findViewById<TextView>(R.id.dialogFoodProteinsTextView)
        val foodFatsTextView = dialogView.findViewById<TextView>(R.id.dialogFoodFatsTextView)
        val foodAllergenTextView = dialogView.findViewById<TextView>(R.id.dialogFoodAllergenTextView)
        val eatenButton = dialogView.findViewById<Button>(R.id.dialogFoodEatenButton)

        // Set data into dialog
        foodNameTextView.text = food.food_name
        foodDescriptionTextView.text = food.food_desc
        foodCaloriesTextView.text = "Calories: ${food.calories}"
        foodCarbsTextView.text = "Carbohydrates: ${food.carbohydrate?.takeIf { it > 0 } ?: "N/A"}"
        foodProteinsTextView.text = "Proteins: ${food.proteins?.takeIf { it > 0 } ?: "N/A"}"
        foodFatsTextView.text = "Fats: ${food.fat?.takeIf { it > 0 } ?: "N/A"}"
        foodAllergenTextView.text = "Allergens: ${food.allergens.ifEmpty { "None" }}"

        // Fetch image for the dialog
        fetchImageUrl(food.imageUrl) { imageUrl ->
            if (imageUrl.isNotEmpty()) {
                Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.error_image)
                    .into(foodImageView)
            } else {
                foodImageView.setImageResource(R.drawable.error_image)
            }
        }

        // Handle 'Eaten' button click to update calorie, protein, and fat intake
        eatenButton.setOnClickListener {
            val currentUser = auth.currentUser
            if (currentUser != null) {
                updateIntakes(currentUser.email!!, food.calories, food.proteins ?: 0, food.fat ?: 0)
            } else {
                Toast.makeText(context, "Please log in to track your intake", Toast.LENGTH_SHORT).show()
            }
        }

        // Build and show the dialog
        AlertDialog.Builder(context)
            .setView(dialogView)
            .setPositiveButton("Close", null)
            .create()
            .show()
    }

    private fun updateIntakes(userEmail: String, foodCalories: Int, foodProteins: Int, foodFats: Int) {
        val userDocRef = db.collection("users").document(userEmail)

        // Fetch the current intakes (calorie, protein, fat) from Firestore
        userDocRef.get().addOnSuccessListener { documentSnapshot ->
            val currentCalorieIntake = documentSnapshot.getLong("calorieIntake")?.toInt() ?: 0
            val currentProteinIntake = documentSnapshot.getLong("proteinIntake")?.toInt() ?: 0
            val currentFatIntake = documentSnapshot.getLong("fatIntake")?.toInt() ?: 0

            val updatedCalorieIntake = currentCalorieIntake + foodCalories
            val updatedProteinIntake = currentProteinIntake + foodProteins
            val updatedFatIntake = currentFatIntake + foodFats

            // Update the calorie, protein, and fat intake in Firestore
            userDocRef.update(
                mapOf(
                    "calorieIntake" to updatedCalorieIntake,
                    "proteinIntake" to updatedProteinIntake,
                    "fatIntake" to updatedFatIntake
                )
            ).addOnSuccessListener {
                Toast.makeText(
                    context,
                    "Intakes updated: Calories +$foodCalories, Proteins +$foodProteins, Fat +$foodFats",
                    Toast.LENGTH_SHORT
                ).show()
            }.addOnFailureListener {
                Toast.makeText(context, "Failed to update intakes", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Failed to fetch current intakes", Toast.LENGTH_SHORT).show()
        }
    }


    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
        val foodDescriptionTextView: TextView = itemView.findViewById(R.id.foodDescriptionTextView)
        val foodCaloriesTextView: TextView = itemView.findViewById(R.id.foodCaloriesTextView)
        val foodCarbsTextView: TextView = itemView.findViewById(R.id.foodCarbsTextView)
        val foodProteinsTextView: TextView = itemView.findViewById(R.id.foodProteinsTextView)
        val foodFatsTextView: TextView = itemView.findViewById(R.id.foodFatsTextView)
        val foodAllergenTextView: TextView = itemView.findViewById(R.id.foodAllergenTextView)
        val foodImageView: ImageView = itemView.findViewById(R.id.foodImageView)
        val favoriteButton: ImageButton = itemView.findViewById(R.id.favouriteButton)
    }
}
