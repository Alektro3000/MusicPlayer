package com.example.myplayer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.example.myplayer.ui.playlist.PlaylistListFragment
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class MainMenuFragment: Fragment() {

    private lateinit var mainMenuPagerAdapter: MainMenuPagerAdapter
    private lateinit var viewPager: ViewPager2

    override fun onCreateView(inflater: LayoutInflater,
                              container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.main_menu_layout, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mainMenuPagerAdapter = MainMenuPagerAdapter(this)
        viewPager = view.findViewById(R.id.pager)
        val tabLayout = view.findViewById<TabLayout>(R.id.tab_layout)
        viewPager.adapter = mainMenuPagerAdapter

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when(position){0-> "songs"
                1->"playlists"
                else -> "error $position"
            }
        }.attach()
    }
}
class MainMenuPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2
    override fun createFragment(position: Int): Fragment {
        return when(position){
            0-> SongListFragment()
            1-> PlaylistListFragment()
            else -> {throw Exception("Invalid tab")}
        }
    }
}