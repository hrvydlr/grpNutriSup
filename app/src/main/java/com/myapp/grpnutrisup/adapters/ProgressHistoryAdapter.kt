package com.myapp.grpnutrisup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.myapp.grpnutrisup.models.ProgressItem

class ProgressHistoryAdapter(
    private var progressList: List<ProgressItem>
) : RecyclerView.Adapter<ProgressHistoryAdapter.ViewHolder>() {

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dayTextView: TextView = itemView.findViewById(R.id.dayTextView)
        val calorieTextView: TextView = itemView.findViewById(R.id.calorieTextView)
        val fatsTextView: TextView = itemView.findViewById(R.id.fatsTextView)
        val proteinTextView: TextView = itemView.findViewById(R.id.proteinTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_progress_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = progressList[position]
        holder.dayTextView.text = item.day
        holder.calorieTextView.text = "${item.calorieIntake}/${item.calorieGoal}"
        holder.fatsTextView.text = item.fats.toString()
        holder.proteinTextView.text = item.protein.toString()
    }

    override fun getItemCount(): Int = progressList.size

    fun updateProgressList(newProgressList: List<ProgressItem>) {
        progressList = newProgressList
        notifyDataSetChanged()
    }
}
