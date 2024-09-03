package com.example.myplayer

import android.content.ComponentName
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.addCallback
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.findNavController
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myplayer.data.DataBaseViewModel
import com.example.myplayer.data.PlayerViewModel
import com.example.myplayer.data.PlaylistSongCrossRef
import com.example.myplayer.player.PlayerService
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch
import okhttp3.Dispatcher

class PlaylistFullFragment: Fragment(R.layout.playlist_full) {
    val dbViewModel: DataBaseViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        val id = arguments?.getInt("id")?:return view

        val sessionToken =
            SessionToken(requireContext(), ComponentName(requireContext(), PlayerService::class.java))
        playerViewModel.onStart(sessionToken)

        val textView = view?.findViewById<TextView>(R.id.title_text) ?: return view

        val adapter = SongAdapter(onMenuClick = {_,_,_ -> }, onSongClick = {_,songId ->
            lifecycleScope.launch(Dispatchers.IO) {
                playerViewModel.setSongs(songId, dbViewModel.getPlaylist(id).first().songs)
            }
        })
        val recyclerView: RecyclerView = view.findViewById(R.id.recycler_view)
        recyclerView.adapter = adapter
        recyclerView.setLayoutManager(LinearLayoutManager(context))

        lifecycleScope.launch(Dispatchers.IO){
            dbViewModel.getPlaylist(id).collect{
                adapter.submitList (it.songs)
            }
        }

        val addButton: FloatingActionButton = view.findViewById(R.id.fab)

        addButton.setOnClickListener {
            val bundle = bundleOf("id" to id)
            findNavController().navigate(R.id.playlistFullToSongsSelect,bundle)
        }



        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().popBackStack()
        }

        return view
    }
}