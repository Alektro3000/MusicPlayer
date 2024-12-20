package com.example.myplayer.ui.songs

import android.view.MenuItem
import android.view.View
import androidx.activity.addCallback
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import com.example.myplayer.R
import com.example.myplayer.data.DataBaseViewModel
import com.example.myplayer.data.PlaylistSongCrossRef
import com.example.myplayer.data.SongIncluded
import com.example.myplayer.ui.base.ListFragment
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch


class SongSelectFragment: ListFragment<SongIncluded, SongSelectViewHolder>(R.layout.songs_select) {
    private val dbViewModel: DataBaseViewModel by viewModels()

    override fun getAdapter(): ListAdapter<SongIncluded, SongSelectViewHolder>? {
        val playlistId = arguments?.getInt("id")?: return null
        val adapter = SongSelectAdapter(onMenuClick = { songView, song, _ ->
            showMenu(songView, R.menu.song_menu, song)
        }, onSongClick =  { song, _ ->
            if (song.included) {
                dbViewModel.deletePlaylistSong(PlaylistSongCrossRef(playlistId, song.song.songId))
            } else
                dbViewModel.insert(PlaylistSongCrossRef(playlistId, song.song.songId))
        })

        lifecycleScope.launch {
            dbViewModel.getSongsSelect(playlistId).collect{
                adapter.submitList(it)
            }
        }
        return adapter
    }
    override fun addVals(view: View)
    {
        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().popBackStack()
        }

        val addButton: FloatingActionButton = view.findViewById(R.id.fab)
        addButton.setOnClickListener {
            findNavController().popBackStack()
        }
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