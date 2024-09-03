package com.example.myplayer

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.navigation.fragment.NavHostFragment
import com.google.android.material.tabs.TabLayout


class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        //enableEdgeToEdge()
        super.onCreate(savedInstanceState)


        setContentView(R.layout.main_layout)
    }
}
