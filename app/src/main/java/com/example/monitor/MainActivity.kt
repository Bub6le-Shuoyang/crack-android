package com.example.monitor

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        val fabUpload = findViewById<FloatingActionButton>(R.id.fabUpload)

        bottomNavigationView.background = null
        bottomNavigationView.menu.getItem(2).isEnabled = false

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_images -> loadFragment(ImagesFragment())
                R.id.nav_videos -> loadFragment(VideosFragment())
                R.id.nav_statistics -> loadFragment(StatisticsFragment())
                R.id.nav_profile -> loadFragment(ProfileFragment())
            }
            true
        }

        // Default fragment
        if (savedInstanceState == null) {
            bottomNavigationView.selectedItemId = R.id.nav_images
        }

        fabUpload.setOnClickListener {
            val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)
            if (currentFragment is ImagesFragment) {
                currentFragment.triggerImageUpload()
            } else if (currentFragment is VideosFragment) {
                currentFragment.triggerVideoUpload()
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }
}