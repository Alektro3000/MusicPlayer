package com.example.myplayer.ui.songs

import android.content.ComponentName
import android.view.ContextMenu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.PopupMenu
import androidx.core.os.bundleOf
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.media3.session.SessionToken
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ListAdapter
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myplayer.R
import com.example.myplayer.data.DataBaseViewModel
import com.example.myplayer.data.FetchViewModel
import com.example.myplayer.data.PlayerViewModel
import com.example.myplayer.data.Song
import com.example.myplayer.player.PlayerService
import com.example.myplayer.ui.base.ListFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.launch

class SongListFragment: ListFragment<Song,SongViewHolder>(R.layout.songs_list) {
    private val dbViewModel: DataBaseViewModel by viewModels()
    private val playerViewModel: PlayerViewModel by viewModels()
    private val fetchViewModel: FetchViewModel by viewModels()

    override fun getAdapter(): ListAdapter<Song, SongViewHolder> {
        val adapter =  SongAdapter(onMenuClick = { songView, song, _ ->
            showMenu(songView, R.menu.song_menu, song)
        }, onSongClick =  { _, id ->
            lifecycleScope.launch(Dispatchers.IO) {
                playerViewModel.setSongs(id, dbViewModel.songList().single())
            }
        })

        lifecycleScope.launch {
            dbViewModel.songList().collect { data ->
                adapter.submitList(data)
            }
        }
        return adapter
    }
    override fun addVals(view: View)
    {
        val sessionToken =
            SessionToken(requireContext(), ComponentName(requireContext(), PlayerService::class.java))
        playerViewModel.onStart(sessionToken)

        //Refresh on swipe
        val activityLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { result ->
            lifecycleScope.launch(Dispatchers.IO) {
                fetchViewModel.fetchAudio(result ?: return@launch)
            }.start()
        }

        val swipeRefreshLayout: SwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            activityLauncher.launch(null) // TODO
            swipeRefreshLayout.isRefreshing = false
        }
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