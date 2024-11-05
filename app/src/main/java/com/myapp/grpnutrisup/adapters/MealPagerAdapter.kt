// MealPagerAdapter.kt
package com.myapp.grpnutrisup.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.myapp.grpnutrisup.fragments.BreakfastFragment
import com.myapp.grpnutrisup.fragments.LunchFragment
import com.myapp.grpnutrisup.fragments.DinnerFragment

class MealPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 3  // Three tabs: Breakfast, Lunch, Dinner

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> BreakfastFragment()  // Breakfast tab
            1 -> LunchFragment()      // Lunch tab
            2 -> DinnerFragment()     // Dinner tab
            else -> throw IllegalStateException("Invalid position")
        }
    }
}
