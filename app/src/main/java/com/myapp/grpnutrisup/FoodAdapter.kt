package com.myapp.grpnutrisup.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
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
        holder.foodNameTextView.text = food.foodName
        holder.foodDescriptionTextView.text = food.description

        // Toggle favourite button
        holder.favouriteButton.setOnClickListener {
            addToFavourites(food, holder) // Pass the food object correctly
        }
    }

    override fun getItemCount(): Int {
        return foodList.size
    }

    // Method to update the food list
    fun updateList(newList: List<Food>) {
        foodList = newList
        notifyDataSetChanged() // Notify the adapter that the data has changed
    }

    private fun addToFavourites(food: Food, holder: FoodViewHolder) {
        val userEmail = auth.currentUser?.email
        if (userEmail != null) {
            val favouriteData = mapOf(
                "food_name" to food.foodName,
                "description" to food.description
            )
            db.collection("users").document(userEmail).collection("favourites").add(favouriteData)
                .addOnSuccessListener {
                    Toast.makeText(context, "${food.foodName} added to favourites!", Toast.LENGTH_SHORT).show()
                    // Change star icon to 'on' state
                    holder.favouriteButton.setImageResource(android.R.drawable.star_big_on)
                }
                .addOnFailureListener { e ->
                    Toast.makeText(context, "Failed to add favourite: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    class FoodViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
        val foodDescriptionTextView: TextView = itemView.findViewById(R.id.foodDescriptionTextView)
        val favouriteButton: ImageButton = itemView.findViewById(R.id.favouriteButton)
    }
}
