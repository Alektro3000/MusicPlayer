package com.example.myplayer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myplayer.ui.theme.MyPlayerTheme

@Composable
fun SongList(files: LazyPagingItems<Song>, modifier: Modifier = Modifier) {
    LazyColumn(modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
        ) {
        items(
            count = files.itemCount,
            key = files.itemKey { it.id }) { id ->
            files[id]?.let {
                SongRow(
                    song =  it,
                    onClickMore = { "To Do" },
                    onClickSong = { "To Do" },
                    modifier = Modifier.padding(4.dp)
                )


            }
        }
    }
}

@Composable
fun SongRow(song: Song, modifier: Modifier = Modifier, onClickMore: ()->Unit, onClickSong: ()->Unit) {
    Box(modifier = Modifier.clickable(onClick = onClickSong))
    {
        Row(modifier = modifier) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .crossfade(true)
                    .data(song.uri)
                    .fetcherFactory(AudioFetcher.Factory(LocalContext.current))
                    .build(),
                null,
                placeholder = painterResource(R.drawable.cover),
                modifier = Modifier
                    .padding(4.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .size(62.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(10.dp)
            )
            {
                Text(
                    text = song.name?:"N/A",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.paddingFromBaseline(top = 10.dp, bottom = 10.dp)

                )

                Row {
                    Text(
                        text = song.artist?:"N/A",
                        style = MaterialTheme.typography.titleSmall,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    val length = (song.length ?: 0) / 1000
                    Text(
                        text = "${length/60}:${(length % 60).toString().padStart(2,'0')}",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
            IconButton(
                onClick = onClickMore,
                modifier = Modifier.padding(top = 14.dp, end = 10.dp)
            ) {
                Icon(Icons.Default.MoreVert, stringResource(R.string.song_row_more))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SongRowPreview() {
    MyPlayerTheme {
        SongRow(Song(
            id = 1,
            name = "Ghosts",
            length = 193000,
            artist = "Artist - with a lot of text, so so much"), modifier = Modifier,
            {}, {}
        )
    }
}

/*
@Preview(showBackground = true)
@Composable
fun SongListPreview() {
    MyPlayerTheme {
        SongList(List(10
        ) {
            Song(
                id = 1,
                name = "Ghosts",
                length = 193,
                artist = "Play me"
            )
        })

    }
}
*/