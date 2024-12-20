package com.example.myplayer.ui.base

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.example.myplayer.R
import com.example.myplayer.data.SongIncluded

open class ListFragment<T, ViewHold : ViewHolder>(@LayoutRes contentLayoutId: Int): Fragment(contentLayoutId) {
        override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
        ): View {
            val view = super.onCreateView(inflater, container, savedInstanceState)!!

            val adapter = getAdapter()?:return view
            val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
            recyclerView.layoutManager = LinearLayoutManager(context)
            recyclerView.adapter = adapter

            addVals(view)

            return view
        }

        private fun showMenu(view: View, songMenu: Int, song: SongIncluded) {
            val popup = PopupMenu(requireContext(), view)
            popup.menuInflater.inflate(songMenu, popup.menu)

            popup.setOnMenuItemClickListener { menuItem: MenuItem ->
                val bundle = bundleOf("id" to song.song.songId)
                if(menuItem.itemId == R.id.edit)
                {
                    findNavController().navigate(R.id.action_mainMenuFragment_to_songEditFragment,bundle)
                }
                true
            }
            popup.setOnDismissListener {
                // Respond to popup being dismissed.
            }
            // Show the popup menu.
            popup.show()
        }

        open fun getAdapter(): ListAdapter<T, ViewHold>?
        {
            return null
        }
        open fun addVals(view: View)
        {
        }
    }