package com.example.myplayer.ui.playlist

import android.view.LayoutInflater
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
import com.example.myplayer.data.SongIncluded
import com.google.android.material.card.MaterialCardView


class SongSelectAdapter(private val onSongClick: (SongIncluded, Int) -> Unit) :
    ListAdapter<SongIncluded, SongSelectViewHolder>(SongIncludedComparator) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SongSelectViewHolder =
        SongSelectViewHolder.create(parent, onSongClick)

    override fun onBindViewHolder(holder: SongSelectViewHolder, position: Int) {
        val item = getItem(position)?:return
        holder.bind(item, position)
    }
}

class SongSelectViewHolder(private val view: View, val onSongClick: (SongIncluded, Int) -> Unit) : RecyclerView.ViewHolder(view) {
    private val name: TextView = view.findViewById(R.id.title)
    private val artist: TextView = view.findViewById(R.id.artist)
    private val time: TextView = view.findViewById(R.id.time)
    private val cover: ImageView = view.findViewById(R.id.cover)
    private val card: MaterialCardView = view.findViewById(R.id.card)

    fun bind(songIncluded: SongIncluded, id: Int) {
        val song = songIncluded.song
        name.text = song.name
        artist.text = song.displayArtist
        card.isChecked = songIncluded.included
        //card.setCardBackgroundColor()
        //name.setTextAppearance(songIncluded.included ? 1 : 0)


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

            onSongClick(songIncluded.copy(included = card.isChecked), id)
            card.isChecked = !card.isChecked
        }

    }

    companion object {
        fun create(parent: ViewGroup, onSongClick: (SongIncluded, Int) -> Unit): SongSelectViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.song_layout, parent, false)
            return SongSelectViewHolder(view, onSongClick)
        }
    }
}

object SongIncludedComparator : DiffUtil.ItemCallback<SongIncluded>() {
    override fun areItemsTheSame(oldItem: SongIncluded, newItem: SongIncluded): Boolean =
        oldItem.song.songId == newItem.song.songId

    override fun areContentsTheSame(oldItem: SongIncluded, newItem: SongIncluded): Boolean =
        oldItem.song == newItem.song
}