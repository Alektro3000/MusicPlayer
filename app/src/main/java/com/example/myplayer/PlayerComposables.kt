package com.example.myplayer

import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.itemKey
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.myplayer.ui.theme.MyPlayerTheme
import kotlinx.coroutines.withContext

@Composable
fun SongList(files: LazyPagingItems<Song>, onClickSong: (id: Int) -> Unit, modifier: Modifier = Modifier) {

    LazyColumn(modifier = modifier,
        contentPadding = PaddingValues(vertical = 8.dp)
        ) {
        items(
            count = files.itemCount,
            key = files.itemKey { it.songId }) { id ->
            files[id]?.let {
                SongRow(
                    song =  it,
                    onClickMore = { "To Do" },
                    modifier = Modifier.clickable {
                        onClickSong(id)
                    }.padding(4.dp)
                )


            }
        }
    }
}

@Composable
fun SongRow(song: Song, modifier: Modifier = Modifier, onClickMore: ()->Unit) {
    ConstraintLayout(modifier = modifier
        .fillMaxWidth(1f)
        .height(70.dp)) {

        val (image, title, artist, menu) = createRefs()

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .crossfade(true)
                .data(song.uri)
                .fetcherFactory(AudioFetcher.Factory(LocalContext.current))
                .build(),
            null,
            placeholder = painterResource(R.drawable.cover),
            error = painterResource(R.drawable.cover),
            modifier = Modifier
                .padding(4.dp)
                .clip(MaterialTheme.shapes.medium)
                .size(62.dp)
                .constrainAs(image) {
                    start.linkTo(parent.start)
                }
        )

        IconButton(
            onClick = onClickMore,
            modifier = Modifier.padding(top = 10.dp, end = 10.dp)
                .constrainAs(menu)
                {
                    end.linkTo(parent.end)
                }
        ) {
            Icon(Icons.Default.MoreVert, stringResource(R.string.song_row_more))
        }

        Text(
            text = song.name?:"N/A",
            style = MaterialTheme.typography.titleMedium,
            overflow = TextOverflow.Ellipsis,
            softWrap = false,
            modifier = Modifier.paddingFromBaseline(top = 26.dp, bottom = 10.dp)
                .constrainAs(title) {
                    linkTo(image.end, menu.start, 5.dp)
                    width = Dimension.fillToConstraints
                }

        )


        Row(modifier = Modifier.constrainAs(artist)
        {
            top.linkTo(title.bottom)
            linkTo(image.end, menu.start)
            width = Dimension.fillToConstraints
        })
        {
            Text(
                text = song.displayArtist ?: "N/A",
                style = MaterialTheme.typography.titleSmall,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                modifier =  Modifier.weight(1f,false).padding(start = 5.dp)
            )

            val length = (song.length ?: 0) / 1000
            Text(
                text = "${length / 60}:${(length % 60).toString().padStart(2, '0')}",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SongRowPreview() {
    MyPlayerTheme {
        SongRow(Song(
            songId = 1,
            name = "Ghosts",
            length = 193000,
            displayArtist = "Artist - with a lot of text, so sooo much"), modifier = Modifier,
            {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SongRowPreviewL() {
    MyPlayerTheme {
        SongRow(Song(
            songId = 1,
            name = "Title",
            length = 193000,
            displayArtist = "Artist"), modifier = Modifier,
            {}
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