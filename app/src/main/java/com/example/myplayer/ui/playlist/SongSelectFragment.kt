package com.example.myplayer.ui.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myplayer.R
import com.example.myplayer.data.*
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class SongSelectFragment: Fragment(R.layout.songs_select) {
    private val dbViewModel: DataBaseViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)!!
        val playlistId = arguments?.getInt("id")?:return view

        val adapter = SongSelectAdapter(onMenuClick = { songView, song, _ ->
            showMenu(songView, R.menu.song_menu, song)
        }, onSongClick =  { song, _ ->
            if (song.included) {
                dbViewModel.deletePlaylistSong(PlaylistSongCrossRef(playlistId, song.song.songId))
            } else
                dbViewModel.insert(PlaylistSongCrossRef(playlistId, song.song.songId))
        })
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)
        recyclerView.setLayoutManager(LinearLayoutManager(context))
        recyclerView.adapter = adapter

        lifecycleScope.launch() {
            dbViewModel.getSongsSelect(playlistId).collect{
                adapter.submitList(it)
            }
        }

        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().popBackStack()
    }

        val addButton: FloatingActionButton = view.findViewById(R.id.fab)
        addButton.setOnClickListener {
            findNavController().popBackStack()
        }

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

}