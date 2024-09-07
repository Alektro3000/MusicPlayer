package com.example.myplayer.ui.playlist

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.myplayer.R
import com.example.myplayer.data.DataBaseViewModel
import com.example.myplayer.data.Playlist
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.launch

class PlaylistListFragment: Fragment(R.layout.playlists_list) {

    private val dbViewModel: DataBaseViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)!!

        val adapter = PlaylistAdapter(PlaylistComparator) { playlist, _ ->
                val bundle = bundleOf("id" to playlist.playlistId)
                findNavController().navigate(R.id.menuToPlaylistFull,bundle)
        }
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerView)

        recyclerView.setLayoutManager(LinearLayoutManager(context))

        recyclerView.adapter = adapter
        lifecycleScope.launch {
            dbViewModel.playlistListsPaged.collect { pagingData ->
                adapter.submitData(pagingData)
            }
        }

        val swipeRefreshLayout: SwipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        swipeRefreshLayout.setOnRefreshListener {
            // TODO
            swipeRefreshLayout.isRefreshing = false
        }

        val fab: FloatingActionButton = view.findViewById(R.id.fab)

        fab.setOnClickListener {
            dbViewModel.insert(Playlist(0,"New Playlist",0,0))
        }

        return view
    }
}

class PlaylistAdapter(diffCallback: DiffUtil.ItemCallback<Playlist>, private val onClick: (Playlist, Int) -> Unit) :
    PagingDataAdapter<Playlist, PlaylistViewHolder>(diffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder =
        PlaylistViewHolder.create(parent, onClick)

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val item = getItem(position)?:return
        holder.bind(item, position)
    }
}

class PlaylistViewHolder(private val view: View, val onClick: (Playlist, Int) -> Unit) : RecyclerView.ViewHolder(view) {
    private val name: TextView = view.findViewById(R.id.title)
    private val content: TextView = view.findViewById(R.id.content)
    private val cover: ImageView = view.findViewById(R.id.cover)
    private val card: View = view.findViewById(R.id.card)
    fun bind(playlist: Playlist, id: Int) {

        name.text = playlist.title
        content.text = "1"
        card.setOnClickListener {
            onClick(playlist, id)
        }

    }

    companion object {
        fun create(parent: ViewGroup, onClick: (Playlist, Int) -> Unit): PlaylistViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.playlist_layout, parent, false)
            return PlaylistViewHolder(view, onClick)
        }
    }
}


object PlaylistComparator : DiffUtil.ItemCallback<Playlist>() {
    override fun areItemsTheSame(oldItem: Playlist, newItem: Playlist) =
        oldItem.playlistId == newItem.playlistId

    override fun areContentsTheSame(oldItem: Playlist, newItem: Playlist) =
        oldItem == newItem
}