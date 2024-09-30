package com.example.myplayer

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.addCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import coil.dispose
import coil.imageLoader
import coil.memory.MemoryCache
import coil.request.ImageRequest
import com.example.myplayer.data.DataBaseViewModel
import com.example.myplayer.data.FetchViewModel
import com.example.myplayer.data.PlayerViewModel
import com.example.myplayer.data.Song
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText
import com.yalantis.ucrop.UCrop
import com.yalantis.ucrop.UCropActivity.ALL
import kotlinx.coroutines.launch
import java.io.File


class SongEditFragment : Fragment(R.layout.song_edit) {
    private val viewModel: PlayerViewModel by viewModels()
    private val fetchViewModel: FetchViewModel by viewModels()
    private val dbViewModel: DataBaseViewModel by viewModels()
    private lateinit var song: Song
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = super.onCreateView(inflater, container, savedInstanceState)!!
        val titleView: TextInputEditText = view.findViewById(R.id.title_text)
        val artistView: TextInputEditText = view.findViewById(R.id.artist_text)
        val cover: ImageView = view.findViewById(R.id.cover)
        val buttonCover: MaterialButton = view.findViewById(R.id.button_cover)
        var newCover: Uri? = null

        val id = arguments?.getInt("id")?:return view
        viewLifecycleOwner.lifecycleScope.launch {
                dbViewModel.getSong(id).collect(){
                    song = it
                    titleView.text?.clear()
                    titleView.text?.insert(0,it.name)
                    artistView.text?.clear()
                    artistView.text?.insert(0,it.displayArtist)

                    val request = ImageRequest.Builder(view.context)
                        .data(it.uri)
                        .target(cover)
                        .error(R.drawable.cover)
                        .build()


                    cover.context.imageLoader.enqueue(request)
                }

        }


        val pictureCrop =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
                val resultCode = result.resultCode
                val data = result.data

                when (resultCode) {
                    Activity.RESULT_OK -> {
                        //Image Uri will not be null for RESULT_OK
                        val fileUri = UCrop.getOutput(data!!)

                        newCover = fileUri
                        cover.dispose()

                        val request = ImageRequest.Builder(view.context)
                            .data(newCover)
                            .target(cover)
                            .error(R.drawable.cover)
                            .build()
                        cover.context.imageLoader.enqueue(request)
                    }
                    UCrop.RESULT_ERROR -> {
                        Toast.makeText(context, UCrop.getError(data!!).toString(), Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        Toast.makeText(context, "Task Cancelled", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        val picturePick = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { intent ->
            val uri = intent.data?.toUri(0)?.toUri()
            if (uri != null) {
                val destination: Uri = File(requireContext().cacheDir, "crop.out").toUri()
                val a = UCrop.Options()
                a.setAllowedGestures(ALL,ALL,ALL)

                pictureCrop.launch(
                    UCrop.of(uri, destination)
                        .withAspectRatio(1f, 1f)
                        .withOptions(a)
                        .getIntent(requireContext())
                )


            }
        }

        val cancelButton: FloatingActionButton = view.findViewById(R.id.fab)

        cancelButton.setOnClickListener {
            if(titleView.text.toString() != "")
                song = song.copy(name = titleView.text.toString())
            if(artistView.text.toString() != "")
                song = song.copy(displayArtist = artistView.text.toString())
            fetchViewModel.updateSong(song)
            if(newCover != null && song.uri != null) {
                fetchViewModel.updateCover(newCover!!, song.uri!!)
                cover.context.imageLoader.memoryCache?.remove(MemoryCache.Key(song.uri!!.toString()))
            }
            dbViewModel.insert(song)
            findNavController().popBackStack()
        }


        requireActivity().onBackPressedDispatcher.addCallback(this) {
            findNavController().popBackStack()
        }
        buttonCover.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.setDataAndType( android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,"image/*")

            picturePick.launch(intent)
            //picturePick.launch(PickVisualMediaRequest(mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly))
        }
        return view
    }
}