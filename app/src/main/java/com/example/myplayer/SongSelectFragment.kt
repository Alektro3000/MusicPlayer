package com.example.myplayer

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.addCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myplayer.data.*
import com.example.myplayer.player.PlayerService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.single
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

        val adapter = SongSelectAdapter { song, _ ->
            if (song.included) {
                dbViewModel.deletePlaylistSong(PlaylistSongCrossRef(playlistId, song.song.songId))
            } else
                dbViewModel.insert(PlaylistSongCrossRef(playlistId, song.song.songId))
        }
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

        return view
    }
}