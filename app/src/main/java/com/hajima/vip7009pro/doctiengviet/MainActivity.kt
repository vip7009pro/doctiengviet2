package com.hajima.vip7009pro.doctiengviet

import android.Manifest
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.speech.tts.TextToSpeech
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.core.content.ContextCompat
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.hajima.vip7009pro.doctiengviet.tts.AzureNeuralTtsClient
import com.hajima.vip7009pro.doctiengviet.ui.theme.DocTiengVietTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val MAX_TEXT_LENGTH = 4000

        private const val PREFS_NAME = "DocTiengVietPrefs"
        private const val PREF_PROVIDER = "pref_provider"
        private const val PREF_AZURE_KEY = "pref_azure_key"
        private const val PREF_AZURE_REGION = "pref_azure_region"
        private const val PREF_AZURE_VOICE = "pref_azure_voice"

        private const val PROVIDER_LOCAL = "local"
        private const val PROVIDER_AZURE = "azure"
        private const val DEFAULT_AZURE_REGION = "southeastasia"
        private const val DEFAULT_AZURE_VOICE = "vi-VN-HoaiMyNeural"
    }

    private val voiceOptions = listOf(
        VoiceOption(R.string.azure_voice_hoai_my, "vi-VN-HoaiMyNeural"),
        VoiceOption(R.string.azure_voice_nam_minh, "vi-VN-NamMinhNeural")
    )

    private var inputText by mutableStateOf("")
    private var azureKey by mutableStateOf("")
    private var azureRegion by mutableStateOf(DEFAULT_AZURE_REGION)
    private var selectedVoiceCode by mutableStateOf(DEFAULT_AZURE_VOICE)
    private var provider by mutableStateOf(PROVIDER_AZURE)
    private var pitchProgress by mutableFloatStateOf(33f)
    private var speedProgress by mutableFloatStateOf(33f)
    private var isBusy by mutableStateOf(false)

    private var pendingSaveRequest = false

    private var localTts: TextToSpeech? = null
    private var localTtsReady = false
    private var mediaPlayer: MediaPlayer? = null

    private val worker: ExecutorService = Executors.newSingleThreadExecutor()
    private val azureNeuralTtsClient = AzureNeuralTtsClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        initLocalTts()
        loadPreferences()

        setContent {
            DocTiengVietTheme {
                val activity = this@MainActivity
                val context = LocalContext.current
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    if (isGranted && pendingSaveRequest) {
                        pendingSaveRequest = false
                        startCloudSave()
                        return@rememberLauncherForActivityResult
                    }
                    if (pendingSaveRequest) {
                        pendingSaveRequest = false
                        Toast.makeText(
                            context,
                            context.getString(R.string.toast_storage_permission_denied),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                MainScreen(
                    inputText = inputText,
                    maxTextLength = MAX_TEXT_LENGTH,
                    onInputChange = { newValue ->
                        if (newValue.length <= MAX_TEXT_LENGTH) {
                            inputText = newValue
                        }
                    },
                    isAzureSelected = isAzureSelected(),
                    onProviderChange = { useAzure ->
                        provider = if (useAzure) PROVIDER_AZURE else PROVIDER_LOCAL
                        savePreferences()
                    },
                    azureKey = azureKey,
                    onAzureKeyChange = { azureKey = it },
                    azureRegion = azureRegion,
                    onAzureRegionChange = { azureRegion = it },
                    voiceOptions = voiceOptions,
                    selectedVoiceCode = selectedVoiceCode,
                    onVoiceSelected = { code ->
                        selectedVoiceCode = code
                        savePreferences()
                    },
                    pitchProgress = pitchProgress,
                    onPitchChange = { pitchProgress = it },
                    speedProgress = speedProgress,
                    onSpeedChange = { speedProgress = it },
                    isBusy = isBusy,
                    onSpeakClick = { speakText() },
                    onSaveClick = {
                        if (!isAzureSelected()) {
                            Toast.makeText(
                                activity,
                                activity.getString(R.string.toast_select_azure_for_mp3),
                                Toast.LENGTH_LONG
                            ).show()
                            return@MainScreen
                        }
                        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                            ContextCompat.checkSelfPermission(
                                activity,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            pendingSaveRequest = true
                            permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                            return@MainScreen
                        }
                        startCloudSave()
                    }
                )
            }
        }
    }

    private fun initLocalTts() {
        localTts = TextToSpeech(this) { status ->
            if (status != TextToSpeech.SUCCESS) {
                Log.e(TAG, "Local TTS init failed")
                return@TextToSpeech
            }
            val result = localTts?.setLanguage(Locale.forLanguageTag("vi-VN")) ?: TextToSpeech.ERROR
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Local TTS does not support vi-VN")
                return@TextToSpeech
            }
            localTtsReady = true
        }
    }

    private fun loadPreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        provider = prefs.getString(PREF_PROVIDER, PROVIDER_AZURE) ?: PROVIDER_AZURE
        azureKey = prefs.getString(PREF_AZURE_KEY, "") ?: ""
        azureRegion = prefs.getString(PREF_AZURE_REGION, DEFAULT_AZURE_REGION) ?: DEFAULT_AZURE_REGION
        selectedVoiceCode = prefs.getString(PREF_AZURE_VOICE, DEFAULT_AZURE_VOICE)
            ?: DEFAULT_AZURE_VOICE
    }

    private fun savePreferences() {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(PREF_PROVIDER, if (isAzureSelected()) PROVIDER_AZURE else PROVIDER_LOCAL)
            .putString(PREF_AZURE_KEY, azureKey)
            .putString(PREF_AZURE_REGION, azureRegion.trim())
            .putString(PREF_AZURE_VOICE, selectedVoiceCode)
            .apply()
    }

    private fun isAzureSelected(): Boolean {
        return provider == PROVIDER_AZURE
    }

    private fun speakText() {
        val text = getInputTextOrNull() ?: return

        if (!isAzureSelected()) {
            speakWithLocalTts(text)
            return
        }

        val config = readAzureConfigOrNull() ?: return
        val pitch = sliderToMultiplier(pitchProgress)
        val speed = sliderToMultiplier(speedProgress)

        savePreferences()
        setBusyState(true)
        worker.execute {
            try {
                val mp3 = azureNeuralTtsClient.synthesizeMp3(
                    text,
                    config.apiKey,
                    config.region,
                    config.voice,
                    pitch,
                    speed
                )

                val cacheFile = File(cacheDir, "${buildBaseFileName(text)}.mp3")
                writeToFile(cacheFile, mp3)

                runOnUiThread {
                    setBusyState(false)
                    playAudio(cacheFile)
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Speak with Azure failed", ex)
                runOnUiThread {
                    setBusyState(false)
                    Toast.makeText(
                        this,
                        getString(R.string.toast_azure_error, ex.message ?: ""),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun speakWithLocalTts(text: String) {
        if (!localTtsReady) {
            Toast.makeText(this, getString(R.string.toast_local_tts_not_ready), Toast.LENGTH_SHORT)
                .show()
            return
        }

        val pitch = sliderToMultiplier(pitchProgress)
        val speed = sliderToMultiplier(speedProgress)

        localTts?.setPitch(pitch)
        localTts?.setSpeechRate(speed)
        localTts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "local_preview")
    }

    private fun startCloudSave() {
        val text = getInputTextOrNull() ?: return
        val config = readAzureConfigOrNull() ?: return
        val pitch = sliderToMultiplier(pitchProgress)
        val speed = sliderToMultiplier(speedProgress)

        savePreferences()
        setBusyState(true)
        worker.execute {
            try {
                val mp3 = azureNeuralTtsClient.synthesizeMp3(
                    text,
                    config.apiKey,
                    config.region,
                    config.voice,
                    pitch,
                    speed
                )

                val savedUri = saveMp3ToDownloads(mp3, buildBaseFileName(text))
                runOnUiThread {
                    setBusyState(false)
                    Toast.makeText(
                        this,
                        getString(R.string.toast_save_success, savedUri.toString()),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (ex: Exception) {
                Log.e(TAG, "Save cloud MP3 failed", ex)
                runOnUiThread {
                    setBusyState(false)
                    Toast.makeText(
                        this,
                        getString(R.string.toast_save_error, ex.message ?: ""),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun getInputTextOrNull(): String? {
        val text = inputText.trim()
        if (text.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_input_empty), Toast.LENGTH_SHORT).show()
            return null
        }
        if (text.length > MAX_TEXT_LENGTH) {
            Toast.makeText(this, getString(R.string.toast_input_too_long), Toast.LENGTH_SHORT).show()
            return null
        }
        return text
    }

    private fun readAzureConfigOrNull(): AzureConfig? {
        val apiKey = azureKey.trim()
        val region = azureRegion.trim()

        if (apiKey.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_missing_azure_key), Toast.LENGTH_SHORT)
                .show()
            return null
        }
        if (region.isEmpty()) {
            Toast.makeText(this, getString(R.string.toast_missing_azure_region), Toast.LENGTH_SHORT)
                .show()
            return null
        }
        return AzureConfig(apiKey, region, selectedVoiceCode)
    }

    private fun playAudio(audioFile: File) {
        releaseMediaPlayer()
        mediaPlayer = MediaPlayer()
        try {
            mediaPlayer?.apply {
                setDataSource(audioFile.absolutePath)
                setOnCompletionListener { releaseMediaPlayer() }
                prepare()
                start()
            }
        } catch (ex: IOException) {
            Log.e(TAG, "Cannot play audio", ex)
            Toast.makeText(this, getString(R.string.toast_cannot_play_audio), Toast.LENGTH_SHORT)
                .show()
            releaseMediaPlayer()
        }
    }

    @Throws(IOException::class)
    private fun saveMp3ToDownloads(data: ByteArray, baseName: String): Uri {
        val fileName = "$baseName.mp3"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver: ContentResolver = contentResolver
            val values = ContentValues()
            values.put(MediaStore.Downloads.DISPLAY_NAME, fileName)
            values.put(MediaStore.Downloads.MIME_TYPE, "audio/mpeg")
            values.put(
                MediaStore.Downloads.RELATIVE_PATH,
                Environment.DIRECTORY_DOWNLOADS + "/DocTiengViet"
            )
            values.put(MediaStore.Downloads.IS_PENDING, 1)

            val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
            val itemUri = resolver.insert(collection, values)
                ?: throw IOException("Cannot create file in Downloads")

            resolver.openOutputStream(itemUri)?.use { output ->
                output.write(data)
            } ?: throw IOException("Cannot open output stream")

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(itemUri, values, null, null)
            return itemUri
        }

        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val folder = File(downloads, "DocTiengViet")
        if (!folder.exists() && !folder.mkdirs()) {
            throw IOException("Cannot create output folder")
        }

        val outputFile = File(folder, fileName)
        writeToFile(outputFile, data)
        MediaScannerConnection.scanFile(
            this,
            arrayOf(outputFile.absolutePath),
            arrayOf("audio/mpeg"),
            null
        )
        return Uri.fromFile(outputFile)
    }

    @Throws(IOException::class)
    private fun writeToFile(file: File, data: ByteArray) {
        FileOutputStream(file).use { outputStream ->
            outputStream.write(data)
        }
    }

    private fun buildBaseFileName(sourceText: String): String {
        var candidate = sourceText.trim()
        if (candidate.length > 20) {
            candidate = candidate.substring(0, 20)
        }
        candidate = Normalizer.normalize(candidate, Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "")
            .replace("[^A-Za-z0-9]+".toRegex(), "_")
            .replace("_+".toRegex(), "_")
            .replace("^_|_$".toRegex(), "")

        if (candidate.isEmpty()) {
            candidate = "doc_tieng_viet"
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        return "${candidate}_$timestamp"
    }

    private fun setBusyState(busy: Boolean) {
        isBusy = busy
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onPause() {
        super.onPause()
        savePreferences()
    }

    override fun onDestroy() {
        super.onDestroy()
        localTts?.stop()
        localTts?.shutdown()
        releaseMediaPlayer()
        worker.shutdownNow()
    }

    private data class AzureConfig(
        val apiKey: String,
        val region: String,
        val voice: String
    )

}

private data class VoiceOption(
    val labelRes: Int,
    val code: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    inputText: String,
    maxTextLength: Int,
    onInputChange: (String) -> Unit,
    isAzureSelected: Boolean,
    onProviderChange: (Boolean) -> Unit,
    azureKey: String,
    onAzureKeyChange: (String) -> Unit,
    azureRegion: String,
    onAzureRegionChange: (String) -> Unit,
    voiceOptions: List<VoiceOption>,
    selectedVoiceCode: String,
    onVoiceSelected: (String) -> Unit,
    pitchProgress: Float,
    onPitchChange: (Float) -> Unit,
    speedProgress: Float,
    onSpeedChange: (Float) -> Unit,
    isBusy: Boolean,
    onSpeakClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val scrollState = rememberScrollState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val backgroundBrush = Brush.linearGradient(
            colors = listOf(
                Color(0xFF0A6AA6),
                Color(0xFF1FB9A2),
                Color(0xFFF58A3D)
            ),
            start = Offset.Zero,
            end = Offset(widthPx, heightPx)
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundBrush)
        )

        Box(
            modifier = Modifier
                .offset(x = (-80).dp, y = (-140).dp)
                .size(260.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0x4DFFFFFF), Color(0x00FFFFFF))
                    )
                )
        )

        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = 120.dp, y = 180.dp)
                .size(320.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(Color(0x66FFE29E), Color(0x00FFF5D6))
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            if (isBusy) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth(),
                    color = colorResource(R.color.brand_secondary),
                    trackColor = colorResource(R.color.surface_glass)
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                Text(
                    text = stringResource(R.string.hero_title),
                    color = colorResource(R.color.white),
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = stringResource(R.string.hero_subtitle),
                    color = colorResource(R.color.white),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.surface_glass)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, colorResource(R.color.stroke_soft))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.section_input),
                            color = colorResource(R.color.text_primary),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = inputText,
                            onValueChange = onInputChange,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                                .heightIn(min = 140.dp),
                            enabled = !isBusy,
                            placeholder = {
                                Text(stringResource(R.string.input_hint))
                            },
                            maxLines = 10,
                            minLines = 5,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = colorResource(R.color.brand_primary),
                                unfocusedBorderColor = colorResource(R.color.brand_primary),
                                focusedContainerColor = colorResource(R.color.white),
                                unfocusedContainerColor = colorResource(R.color.white),
                                cursorColor = colorResource(R.color.brand_primary)
                            )
                        )
                        Text(
                            text = stringResource(
                                R.string.length_template,
                                inputText.length,
                                maxTextLength
                            ),
                            color = colorResource(R.color.text_secondary),
                            fontSize = 13.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp)
                                .align(Alignment.End)
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.surface_glass)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, colorResource(R.color.stroke_soft))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.section_voice),
                            color = colorResource(R.color.text_primary),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            SegmentedButton(
                                selected = !isAzureSelected,
                                onClick = { onProviderChange(false) },
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(text = stringResource(R.string.provider_local))
                            }
                            SegmentedButton(
                                selected = isAzureSelected,
                                onClick = { onProviderChange(true) },
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Text(text = stringResource(R.string.provider_azure))
                            }
                        }
                        Text(
                            text = stringResource(
                                if (isAzureSelected) {
                                    R.string.provider_status_azure
                                } else {
                                    R.string.provider_status_local
                                }
                            ),
                            color = colorResource(R.color.text_secondary),
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 10.dp)
                        )

                        if (isAzureSelected) {
                            Column(modifier = Modifier.padding(top = 10.dp)) {
                                OutlinedTextField(
                                    value = azureKey,
                                    onValueChange = onAzureKeyChange,
                                    modifier = Modifier.fillMaxWidth(),
                                    enabled = !isBusy,
                                    label = { Text(stringResource(R.string.azure_api_key_hint)) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        capitalization = KeyboardCapitalization.None
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colorResource(R.color.brand_primary),
                                        unfocusedBorderColor = colorResource(R.color.brand_primary),
                                        focusedContainerColor = colorResource(R.color.white),
                                        unfocusedContainerColor = colorResource(R.color.white),
                                        cursorColor = colorResource(R.color.brand_primary)
                                    )
                                )

                                OutlinedTextField(
                                    value = azureRegion,
                                    onValueChange = onAzureRegionChange,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp),
                                    enabled = !isBusy,
                                    label = { Text(stringResource(R.string.azure_region_hint)) },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Text,
                                        capitalization = KeyboardCapitalization.None
                                    ),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = colorResource(R.color.brand_primary),
                                        unfocusedBorderColor = colorResource(R.color.brand_primary),
                                        focusedContainerColor = colorResource(R.color.white),
                                        unfocusedContainerColor = colorResource(R.color.white),
                                        cursorColor = colorResource(R.color.brand_primary)
                                    )
                                )

                                var expanded by remember { mutableStateOf(false) }
                                val selectedLabel = voiceOptions
                                    .firstOrNull { it.code == selectedVoiceCode }
                                    ?.let { stringResource(it.labelRes) }
                                    ?: stringResource(voiceOptions.first().labelRes)

                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { if (!isBusy) expanded = !expanded },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = selectedLabel,
                                        onValueChange = {},
                                        readOnly = true,
                                        label = { Text(stringResource(R.string.azure_voice_hint)) },
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded)
                                        },
                                        modifier = Modifier
                                            .menuAnchor()
                                            .fillMaxWidth(),
                                        enabled = !isBusy,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = colorResource(R.color.brand_primary),
                                            unfocusedBorderColor = colorResource(R.color.brand_primary),
                                            focusedContainerColor = colorResource(R.color.white),
                                            unfocusedContainerColor = colorResource(R.color.white),
                                            cursorColor = colorResource(R.color.brand_primary)
                                        )
                                    )

                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        voiceOptions.forEach { option ->
                                            DropdownMenuItem(
                                                text = { Text(stringResource(option.labelRes)) },
                                                onClick = {
                                                    onVoiceSelected(option.code)
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = colorResource(R.color.surface_glass)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = BorderStroke(1.dp, colorResource(R.color.stroke_soft))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = stringResource(R.string.section_tuning),
                            color = colorResource(R.color.text_primary),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.pitch_label),
                                color = colorResource(R.color.text_primary),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = stringResource(
                                    R.string.multiplier_template,
                                    sliderToMultiplier(pitchProgress)
                                ),
                                color = colorResource(R.color.text_secondary),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = pitchProgress,
                            onValueChange = onPitchChange,
                            valueRange = 0f..100f,
                            enabled = !isBusy
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.speed_label),
                                color = colorResource(R.color.text_primary),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = stringResource(
                                    R.string.multiplier_template,
                                    sliderToMultiplier(speedProgress)
                                ),
                                color = colorResource(R.color.text_secondary),
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Slider(
                            value = speedProgress,
                            onValueChange = onSpeedChange,
                            valueRange = 0f..100f,
                            enabled = !isBusy
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = onSpeakClick,
                        modifier = Modifier.weight(1f),
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors()
                    ) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_media_play),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = stringResource(R.string.action_speak))
                    }

                    Button(
                        onClick = onSaveClick,
                        modifier = Modifier.weight(1f),
                        enabled = !isBusy,
                        colors = ButtonDefaults.buttonColors()
                    ) {
                        Icon(
                            painter = painterResource(android.R.drawable.ic_menu_save),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(text = stringResource(R.string.action_save_mp3))
                    }
                }
            }

            AdBanner(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                adUnitId = stringResource(R.string.ad_unit_banner)
            )
        }
    }
}

private fun sliderToMultiplier(value: Float): Float {
    return 0.5f + (value / 100f) * 1.5f
}

@Composable
private fun AdBanner(modifier: Modifier, adUnitId: String) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                this.adUnitId = adUnitId
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
