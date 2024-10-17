package com.myapp.grpnutrisup

import ImageSliderAdapter
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2

class ImageSliderActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var handler: Handler
    private var imageList = listOf(R.drawable.image_slider2, R.drawable.image_slider3, R.drawable.image_slider4) // Your images here

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home)  // Make sure this is the correct layout

        viewPager = findViewById(R.id.imageSlider)
        handler = Handler(Looper.getMainLooper())

        // Set up the ViewPager2 with an adapter
        viewPager.adapter = ImageSliderAdapter(imageList)

        // Optional: Auto-scroll every few seconds
        //autoScroll()
    }

    private fun autoScroll() {
        val delayMillis: Long = 3000  // Slide every 3 seconds
        handler.postDelayed(object : Runnable {
            override fun run() {
                viewPager.currentItem = (viewPager.currentItem + 1) % imageList.size
                handler.postDelayed(this, delayMillis)
            }
        }, delayMillis)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)  // Stop auto-scroll when activity is destroyed
    }
}
