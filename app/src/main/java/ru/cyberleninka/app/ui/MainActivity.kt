package ru.cyberleninka.app.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import ru.cyberleninka.app.R
import ru.cyberleninka.app.ads.Ads

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        applySystemBarInsets()
        Ads.init(this)
        val nav = findViewById<BottomNavigationView>(R.id.nav)
        nav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_browse -> BrowseFragment()
                R.id.nav_library -> LibraryFragment()
                else -> SearchFragment()
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.container, fragment)
                .commit()
            true
        }
        if (savedInstanceState == null) nav.selectedItemId = R.id.nav_search
    }

    override fun onResume() {
        super.onResume()
        Ads.showBanner(this)
    }
}
