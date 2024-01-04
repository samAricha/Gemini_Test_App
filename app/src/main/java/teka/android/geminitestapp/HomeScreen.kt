package teka.android.geminitestapp

import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.launch

@Composable
fun AppContent(
    homeViewModel: HomeViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {

    val appUiState = homeViewModel.uiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val imageRequestBuilder = ImageRequest.Builder(LocalContext.current)
    val imageLoader = ImageLoader.Builder(LocalContext.current).build()

    HomeScreen(uiState = appUiState.value){ inputText, selectedItems ->

        coroutineScope.launch {
            val bitmaps = selectedItems.mapNotNull {
                val imageRequest = imageRequestBuilder
                    .data(it)
                    .size(size = 768)
                    .build()

                val imageResult = imageLoader.execute(imageRequest)
                if (imageResult is SuccessResult){
                    return@mapNotNull(imageResult.drawable as BitmapDrawable).bitmap
                } else {
                    return@mapNotNull null
                }
            }

            homeViewModel.questioning(userInput = inputText, selectedImages = bitmaps)
        }

    }


}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState= HomeUiState.Loading,
    onSendClicked: (String, List<Uri>) -> Unit
){

    var userQuestion by rememberSaveable() {
        mutableStateOf("")
    }

    val imageUris = rememberSaveable(saver = UriCustomSaver()) {
        mutableStateListOf()
    }

    val pickMediaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ){imageUri ->
        imageUri?.let {
            imageUris.add(it)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Gemini ChatBot")},
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary
                )

            )
        },
        bottomBar = {
            Column {
                Row(
                    modifier = Modifier.padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {

                    //Add Image Icon
                    IconButton(
                        modifier = Modifier.padding(4.dp ),
                        onClick = { pickMediaLauncher.launch(PickVisualMediaRequest(
                            ActivityResultContracts.PickVisualMedia.ImageOnly
                        )) }
                    ) {
                        Icon(imageVector = Icons.Default.AddCircle, contentDescription = "Add Image")
                    }
                    
                    //Input Field
                    OutlinedTextField(
                        value = userQuestion,
                        placeholder = { Text(text = "Upload Image and ask question")},
                        onValueChange = {
                            userQuestion = it
                        },
                        label = { Text(text = "User Input")},
                        modifier = Modifier.fillMaxWidth(0.82f)

                    )

                    //Send Button
                    IconButton(
                        modifier = Modifier.padding(4.dp ),
                        onClick = {
                            if (userQuestion.isNotBlank()){
                                onSendClicked(userQuestion, imageUris)
                            }

                        }) {
                        Icon(
                            imageVector = Icons.Default.Send,
                            contentDescription = "Send"
                        )
                    }
                }
                AnimatedVisibility(visible = imageUris.size > 0) {
                    Card(modifier = Modifier.padding(8.dp).fillMaxWidth()) {
                        LazyRow(
                            modifier = Modifier.padding(8.dp)
                        ){
                            items(imageUris){ imageUri ->
                                Column (
                                    verticalArrangement = Arrangement.Center,
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ){
                                    AsyncImage(
                                        model = imageUri,
                                        contentDescription = "",
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .requiredSize(50.dp)
                                    )
                                    TextButton(onClick = { imageUris.remove(imageUri) }) {
                                        Text(text = "Remove")
                                    }
                                }
                            }
                        }
                    }

                }

            }
        }
    ) {

        Column(
            modifier = Modifier
                .padding(it)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            when(uiState){
                is HomeUiState.Initial ->{}
                is HomeUiState.Loading -> {
                    Box(contentAlignment = Alignment.Center){
                        CircularProgressIndicator()
                    }
                }
                is HomeUiState.Success -> {
                    Card(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.large
                    ){
                        Text(text = uiState.outputText,
                            modifier = Modifier.padding(10.dp))
                    }
                }
                is HomeUiState.Error -> {
                    Card(
                        modifier = Modifier
                            .padding(vertical = 16.dp)
                            .fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ){
                        Text(text = uiState.error)
                    }
                }
            }

        }

    }
}