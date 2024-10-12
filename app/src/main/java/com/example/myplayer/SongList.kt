package com.example.myplayer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import coil.request.ImageRequest

import coil.imageLoader
import com.example.myplayer.data.Song
import com.example.myplayer.data.SongIncluded
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

class SongAdapter( private val onSongClick: (Song, Int) -> Unit, private val onMenuClick: (View, Song, Int) -> Unit) :
    ListAdapter<Song, SongViewHolder>(SongComparator) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongViewHolder =
        SongViewHolder.create(parent, onSongClick, onMenuClick)

    override fun onBindViewHolder(holder: SongViewHolder, position: Int) {
        val item = getItem(position)?:return
        holder.bind(item, position)
    }
}

open class SongViewHolder(
    private val view: View,
    val onSongClick: (Song, Int) -> Unit,
    val onMenuClick: (View, Song, Int) -> Unit)
    : RecyclerView.ViewHolder(view)
{
    private val name: TextView = view.findViewById(R.id.title)
    private val artist: TextView = view.findViewById(R.id.artist)
    private val time: TextView = view.findViewById(R.id.time)
    private val cover: ImageView = view.findViewById(R.id.cover)
    val card: MaterialCardView = view.findViewById(R.id.card)
    private val menu: MaterialButton = view.findViewById(R.id.menu)

    fun bind(song: Song, id: Int) {
        name.text = song.name?: view.context.getString(R.string.song_title_none)
        artist.text = song.displayArtist?:view.context.getString(R.string.song_artist_none)

        val length = (song.length ?: 0) / 1000
        val seconds = (length % 60).toString().padStart(2, '0')
        time.text = view.context.getString(R.string.time, length / 60, seconds)

        val request = ImageRequest.Builder(view.context)
            .data(song.uri)
            .target(cover)
            .error(R.drawable.cover)
            .build()
        cover.context.imageLoader.enqueue(request)

        artist.requestLayout()
        card.setOnClickListener {
            onSongClick(song, id)
        }
        menu.setOnClickListener {
            onMenuClick(menu, song, id)
        }

    }

    companion object {
        fun create(parent: ViewGroup, onSongClick: (Song, Int) -> Unit, onMenuClick: (View, Song, Int) -> Unit): SongViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.song_layout, parent, false)
            return SongViewHolder(view, onSongClick, onMenuClick)
        }
    }
}

object SongComparator : DiffUtil.ItemCallback<Song>() {
    override fun areItemsTheSame(oldItem: Song, newItem: Song): Boolean =
        oldItem.songId == newItem.songId

    override fun areContentsTheSame(oldItem: Song, newItem: Song): Boolean =
        oldItem == newItem
}