package com.myapp.grpnutrisup.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.myapp.grpnutrisup.R
import com.myapp.grpnutrisup.models.Food

class FavoritesAdapter(private var favoriteFoodItems: List<Food>) :
    RecyclerView.Adapter<FavoritesAdapter.FavoritesViewHolder>() {

    class FavoritesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val foodNameTextView: TextView = itemView.findViewById(R.id.foodNameTextView)
        val foodDescriptionTextView: TextView = itemView.findViewById(R.id.foodDescriptionTextView)
        val foodCaloriesTextView: TextView = itemView.findViewById(R.id.foodCaloriesTextView)
        val foodCarbsTextView: TextView = itemView.findViewById(R.id.foodCarbsTextView)
        val foodProteinsTextView: TextView = itemView.findViewById(R.id.foodProteinsTextView)
        val foodFatsTextView: TextView = itemView.findViewById(R.id.foodFatsTextView)
        val foodAllergenTextView: TextView = itemView.findViewById(R.id.foodAllergenTextView)
        val foodImageView: ImageView = itemView.findViewById(R.id.foodImageView)
        val favouriteButton: ImageButton = itemView.findViewById(R.id.favouriteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavoritesViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.food_item, parent, false)
        return FavoritesViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: FavoritesViewHolder, position: Int) {
        val foodItem = favoriteFoodItems[position]

        holder.foodNameTextView.text = foodItem.food_name
        holder.foodDescriptionTextView.text = foodItem.food_desc
        holder.foodCaloriesTextView.text = "Calories: ${foodItem.calories}"
        holder.foodCarbsTextView.text = "Carbohydrates: ${foodItem.carbohydrate}g"
        holder.foodProteinsTextView.text = "Proteins: ${foodItem.proteins}g"
        holder.foodFatsTextView.text = "Fats: ${foodItem.fat}g"
        holder.foodAllergenTextView.text = "Allergens: ${foodItem.allergens}"

        // Load food image
        Glide.with(holder.itemView.context)
            .load(foodItem.imageUrl)
            .placeholder(R.drawable.placeholder_image)
            .into(holder.foodImageView)

        // Handle favorite button click (can be extended to remove favorites)
        holder.favouriteButton.setOnClickListener {
            // Handle favorite button logic here
        }
    }

    override fun getItemCount(): Int {
        return favoriteFoodItems.size
    }

    fun updateData(newFavorites: List<Food>) {
        favoriteFoodItems = newFavorites
        notifyDataSetChanged()
    }
}
