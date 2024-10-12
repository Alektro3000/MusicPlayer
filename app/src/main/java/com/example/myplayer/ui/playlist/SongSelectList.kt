package com.example.myplayer.ui.playlist

import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.imageLoader
import coil.request.ImageRequest
import com.example.myplayer.R
import com.example.myplayer.SongViewHolder
import com.example.myplayer.data.Song
import com.example.myplayer.data.SongIncluded
import com.google.android.material.card.MaterialCardView


class SongSelectAdapter(private val onSongClick: (SongIncluded, Int) -> Unit,
                        private val onMenuClick: (View, SongIncluded, Int) -> Unit) :
    ListAdapter<SongIncluded, SongSelectViewHolder>(SongIncludedComparator) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongSelectViewHolder =
        SongSelectViewHolder.create(parent, onSongClick, onMenuClick)

    override fun onBindViewHolder(holder: SongSelectViewHolder, position: Int) {
        val item = getItem(position)?:return
        holder.bind(item, position)
    }
}

class SongSelectViewHolder(private val view: View,
                           val onSongSelectClick: (SongIncluded, Int) -> Unit,
                            val onSongMenu: (View, SongIncluded, Int) -> Unit) :
    SongViewHolder(view, {song, id ->
        val card: MaterialCardView = view.findViewById(R.id.card)
        onSongSelectClick(SongIncluded(song,card.isChecked),id)
        card.isChecked = !card.isChecked },
        {menuView, song, id ->
            val card: MaterialCardView = view.findViewById(R.id.card)
            onSongMenu(menuView,SongIncluded(song,card.isChecked),id) }) {


    fun bind(songIncluded: SongIncluded, id: Int) {
        bind(songIncluded.song, id);
        card.isChecked = songIncluded.included
    }

    companion object {
        fun create(parent: ViewGroup, onSongClick: (SongIncluded, Int) -> Unit, onMenuClick: (View, SongIncluded, Int) -> Unit): SongSelectViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.song_layout, parent, false)
            return SongSelectViewHolder(view, onSongClick, onMenuClick)
        }
    }
}

object SongIncludedComparator : DiffUtil.ItemCallback<SongIncluded>() {
    override fun areItemsTheSame(oldItem: SongIncluded, newItem: SongIncluded): Boolean =
        oldItem.song.songId == newItem.song.songId

    override fun areContentsTheSame(oldItem: SongIncluded, newItem: SongIncluded): Boolean =
        oldItem.song == newItem.song
}