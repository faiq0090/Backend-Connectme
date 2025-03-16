package com.faiq.i210759

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class bottom_navigation : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bottom_navigation)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation)


        // Set default fragment (HomeFragment)
        replaceFragment(HomeFragment())

        bottomNavigationView.setOnItemSelectedListener { item ->
            handleNavigation(item.itemId)
        }


    }

    private fun handleNavigation(itemId: Int): Boolean {
        val fragment: Fragment = when (itemId) {
            R.id.nav_home -> HomeFragment()
            R.id.nav_search -> Search()
            R.id.nav_profile -> Profile()
            R.id.nav_contacts -> Contact()
            R.id.nav_add->      Post()

            else -> return false
        }
        replaceFragment(fragment)
        return true
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

}