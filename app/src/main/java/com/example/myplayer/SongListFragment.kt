package com.example.myplayer

import android.app.Application
import android.content.ComponentName
import android.os.Bundle
import android.view.ContextMenu
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
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

class SongListFragment: Fragment(R.layout.songs_list) {
    private val viewModel: PlayerViewModel by viewModels()
    private val fetchViewModel: FetchViewModel by viewModels()
    private val dbViewModel: DataBaseViewModel by viewModels()
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)!!
            val sessionToken =
                SessionToken(requireContext(), ComponentName(requireContext(), PlayerService::class.java))
            viewModel.onStart(sessionToken)

        val activityLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { result ->
            lifecycleScope.launch(Dispatchers.IO) {
                fetchViewModel.fetchAudio(result ?: return@launch)
            }.start()
        }
        val adapter = SongAdapter(onMenuClick = { view, song, id ->
            showMenu(view, R.menu.song_menu, song)
        }, onSongClick =  { _, id ->
            lifecycleScope.launch(Dispatchers.IO) {
                viewModel.setSongs(id, dbViewModel.songList().single())
            }
        })

        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)

        recyclerView.setLayoutManager(LinearLayoutManager(context))

        recyclerView.adapter = adapter
        registerForContextMenu(recyclerView)
        
        lifecycleScope.launch {
            dbViewModel.songList().collect { data ->
                adapter.submitList(data)
            }
        }

        val swipeRefreshLayout: SwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            activityLauncher.launch(null) // TODO
            swipeRefreshLayout.isRefreshing = false
        }
        return view
    }

    private fun showMenu(view: View, songMenu: Int, song: Song) {
        val popup = PopupMenu(requireContext(), view)
        popup.menuInflater.inflate(songMenu, popup.menu)

        popup.setOnMenuItemClickListener { menuItem: MenuItem ->
            val bundle = bundleOf("id" to song.songId)
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

    override fun onCreateContextMenu(
        menu: ContextMenu,
        v: View,
        menuInfo: ContextMenu.ContextMenuInfo?
    ) {
        super.onCreateContextMenu(menu, v, menuInfo)
        val inflater = MenuInflater(this.context)
        inflater.inflate(R.menu.song_menu, menu)
    }
}