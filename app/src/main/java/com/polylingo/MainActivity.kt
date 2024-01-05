package com.polylingo

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.text.HtmlCompat
import com.polylingo.app.data.*
import com.polylingo.app.data.TatoebaAPI.SearchResponse
import com.polylingo.app.data.TatoebaAPI.Sentence
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.util.*
import com.google.android.material.snackbar.Snackbar
import com.polylingo.data.ChatActivity
import com.polylingo.data.ChatGPTCallback
import com.polylingo.data.callChatGPT
import android.speech.tts.TextToSpeech
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import okhttp3.Cache

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    private lateinit var translationAPI: TranslationAPI
    private lateinit var api: TatoebaAPI
    private lateinit var buttonGetSentence: Button
    private lateinit var textViewSentence: TextView
    private lateinit var editTextAnswer: EditText
    private lateinit var editAIresponse: EditText
    private var missingWord = ""
    private var previousWord = ""
    private var nextWord = ""
    private var missingjyutWord = ""
    private var previousjyutWord = ""
    private var nextjyutWord = ""
    private var missingtranscriptionWord = ""
    private lateinit var textViewEnglishTranslation: TextView
    private lateinit var textViewLatinTranscription: TextView
    private lateinit var languageSpinner: Spinner
    private var languagechoice = "Cantonese"  // default language
    private var selectedLanguage = "yue"  // default language
    private var baselanguagechoice = "English"  // default language
    private var selectedbaseLanguage = "eng"  // default language
    private lateinit var textToSpeech: TextToSpeech
    private var selectedSpeed: Float = 0.8f // Default speed
    private var isTtsEnabled = true
    private var speechText = ""
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView


    private fun enableTextToSpeech() {
        isTtsEnabled = true
    }

    private fun disableTextToSpeech() {
        isTtsEnabled = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ProgressBar
        progressBar = findViewById(R.id.progressBar)
        progressBar.max = 100  // Set the maximum value to 100 (for 100%)
        progressBar.progress = 0  // Set the initial progress to 0

        // Get a reference to the TextView
        progressText = findViewById(R.id.progressText)

        // Initialize APIs
        api = createTatoebaAPI()
        translationAPI = createTranslationAPI()

        // Initialize UI elements
        buttonGetSentence = findViewById(R.id.buttonGetSentence)
        textViewSentence = findViewById(R.id.textViewSentence)
        editTextAnswer = findViewById(R.id.editTextAnswer)
        editAIresponse = findViewById(R.id.editAIresponse)
        textViewEnglishTranslation = findViewById(R.id.textViewEnglishTranslation)
        textViewLatinTranscription = findViewById(R.id.textViewLatinTranscription)
        val buttonGetExplanation: Button = findViewById(R.id.buttonGetExplanation)
        val buttonGetTranslation: Button = findViewById(R.id.buttonGetTranslation)
        val rootView: View = findViewById(android.R.id.content)
        val chatButton: Button = findViewById(R.id.chatButton)
        chatButton.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }

        // Initialize the Speed Spinner
        val speedSpinner: Spinner = findViewById(R.id.spinner_speed)

// Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.tts_speed_options,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            speedSpinner.adapter = adapter
        }

// Set the listener for the speed spinner
        speedSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                // Check if "Turn Off" option is selected
                if (pos == 3) {
                    disableTextToSpeech() // Disable the Text-to-Speech feature
                    // Stop the TextToSpeech instance if it's speaking
                    if (::textToSpeech.isInitialized && textToSpeech.isSpeaking) {
                        textToSpeech.stop()
                    }
                    return // Exit the listener callback
                }

                // Map the selected item to the corresponding speed value
                selectedSpeed = when (pos) {
                    0 -> 0.8f // Fast speed
                    1 -> 0.5f // Medium speed
                    2 -> 0.3f // Slow speed
                    else -> 0.8f // Default to medium speed
                }

                enableTextToSpeech() // Enable the Text-to-Speech feature
                // Apply the new speed to the Text-to-Speech engine
                textToSpeech.setSpeechRate(selectedSpeed)

                // Speak the current sentence with the updated speed
                speakSentence(speechText)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }

        }
        // Set the default speed spinner position to 3 (Turn Off option)
        speedSpinner.setSelection(3)

        // Initialize the Spinner
        languageSpinner = findViewById(R.id.spinner_languages)

        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter.createFromResource(
            this,
            R.array.languages,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            // Specify the layout to use when the list of choices appears
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            // Apply the adapter to the spinner
            languageSpinner.adapter = adapter
        }

        // Set the listener for the spinner
        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, pos: Int, id: Long) {
                // map the selected item to its language code
                val selectedItem = parent.getItemAtPosition(pos).toString()
                languagechoice = selectedItem
                selectedLanguage = when (selectedItem) {
                    "Cantonese" -> "yue"
                    "Mandarin" -> "cmn"
                    "Dutch" -> "nld"
                    "Spanish" -> "spa"
                    "French" -> "fra"
                    "Greek" -> "ell"
                    "Russian" -> "rus"
                    "English from Spanish" -> "eng"
                    // add more mappings as needed
                    else -> "yue"
                }

                // set the visibility of textViewLatinTranscription
                if (selectedLanguage == "yue" || selectedLanguage == "cmn") {
                    textViewLatinTranscription.visibility = View.VISIBLE
                } else {
                    textViewLatinTranscription.visibility = View.GONE
                }

                if (selectedLanguage == "eng") {
                    baselanguagechoice = "Spanish"
                    selectedbaseLanguage = "spa"
                } else {
                    baselanguagechoice = "English"
                    selectedbaseLanguage = "eng"
                }

                // Reset the progress bar
                progressBar.progress = 0
                // Reinitialize the Text-to-Speech engine with the updated language
                textToSpeech.language = Locale(selectedLanguage)
                // delete transcription word
                missingtranscriptionWord=""
                // Speak the current sentence with the updated language
                fetchRandomSentence()
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }
        }


        // Set listeners for buttons
        val buttonShowGuide: Button = findViewById(R.id.buttonShowGuide)
        buttonShowGuide.setOnClickListener {
            showGuide()
        }

        buttonGetTranslation.setOnClickListener {
            getTranslationFromApi(this, rootView, translationAPI, missingWord, previousWord, nextWord, missingjyutWord, previousjyutWord, nextjyutWord)
        }

        buttonGetSentence.setOnClickListener {
            editTextAnswer.text.clear()
            editAIresponse.text.clear()
            missingtranscriptionWord = ""
            textViewLatinTranscription.text = ""
            fetchRandomSentence()
        }

        buttonGetExplanation.setOnClickListener {
            val AIquestion = editAIresponse.text
            val originalsentence = textViewSentence.text.toString().replace("_", missingWord)
            if (originalsentence.isNotEmpty()) {
                val apiKeyResult = getApiKey(this)
                val apiKey = apiKeyResult.first
                val statusCode = apiKeyResult.second
                if (apiKey == null) {
                    // Handle authentication error
                    Toast.makeText(this, "API key not found or invalid (HTTP status code $statusCode)", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                val prompt = "$AIquestion.  Write your response in $baselanguagechoice. Your role is to serve as a $languagechoice language tutor and teach your conversation partner how to speak $languagechoice at intermediate level. The student speaks $baselanguagechoice. Your task is to review the sentence: \"$originalsentence\" and provide a translation of each word in $baselanguagechoice, as well as a detailed grammar explanation for the sentence. Your grammar explanation should be comprehensive and clear, addressing the sentence structure, verb tense, and any other relevant grammar concepts. Note: If the language is Cantonese, please use jyutping where necessary. If the language is not Cantonese, you can ignore the jyutping instruction."
                callChatGPT(this, prompt, object : ChatGPTCallback {
                    override fun onSuccess(responseText: String) {
                        runOnUiThread {
                            val responseTextView = findViewById<TextView>(R.id.response_textview)
                            responseTextView.text = responseText
                        }
                    }

                    override fun onFailure(errorMessage: String) {
                        runOnUiThread {
                            Toast.makeText(this@MainActivity, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
                        }
                    }
                })
            } else {
                Snackbar.make(rootView, "Please fetch a sentence first", Snackbar.LENGTH_LONG).show()
            }
        }

        findViewById<Button>(R.id.buttonSubmitAnswer).setOnClickListener {
            val userAnswer = editTextAnswer.text.toString().replace("1", "")
                .replace("2", "")
                .replace("3", "")
                .replace("4", "")
                .replace("5", "")
                .replace("6", "")
                .replace(" ", "")
            if (missingWord.isNotEmpty() && (missingWord.replace("[^\\p{L}]".toRegex(), "").lowercase(Locale.getDefault())
                        == userAnswer
                    .replace("[^\\p{L}\\s]", "").lowercase(Locale.getDefault())
                        ) || (missingtranscriptionWord.isNotEmpty() && missingtranscriptionWord.replace("[^\\p{L}]".toRegex(), "").lowercase(Locale.getDefault()) == userAnswer.lowercase(
                    Locale.getDefault()
                ))) {
                val toastMessage = if (missingtranscriptionWord.isNotEmpty()) {
                    "Correct! The correct answer is \"$missingWord\" or \"$missingtranscriptionWord\""
                } else {
                    "Correct! The correct answer is \"$missingWord\""
                }
                Snackbar.make(rootView, toastMessage, Snackbar.LENGTH_LONG).show()
                //Toast.makeText(this, toastMessage, Toast.LENGTH_LONG).show()
            } else {
                val errorMessage = if (missingtranscriptionWord.isNotEmpty()) {
                    "Incorrect. The correct answer is \"$missingWord\" or \"$missingtranscriptionWord\""
                } else {
                    "Incorrect. The correct answer is \"$missingWord\""
                }
                Snackbar.make(rootView, errorMessage, Snackbar.LENGTH_LONG).show()
                //Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
            }
        }
        // Initialize the Text-to-Speech engine
        textToSpeech = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = Locale(selectedLanguage) // Specify the desired language locale, e.g., Cantonese (yue)
            val result = textToSpeech.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                // Language not supported, handle the error
                Toast.makeText(this, "Text-to-Speech language not supported", Toast.LENGTH_SHORT).show()
            } else {
                // Set the speech rate to a slower value
                textToSpeech.setSpeechRate(selectedSpeed)
            }
        } else {
            // Text-to-Speech initialization failed, handle the error
            Toast.makeText(this, "Failed to initialize Text-to-Speech", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        // Shutdown the Text-to-Speech engine when the activity is destroyed
        if (textToSpeech != null) {
            textToSpeech.stop()
            textToSpeech.shutdown()
        }
        super.onDestroy()
    }

    private fun createTatoebaAPI(): TatoebaAPI {
        val client = createOkHttpClient()
        return Retrofit.Builder()
            .baseUrl("https://tatoeba.org/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TatoebaAPI::class.java)
    }

    private fun createTranslationAPI(): TranslationAPI {
        val client = createOkHttpClient()
        return Retrofit.Builder()
            .baseUrl("https://api.mymemory.translated.net/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TranslationAPI::class.java)
    }

    private fun showGuide() {
        val dialogMessage = getString(R.string.dialog_guide_message)

        val textView = TextView(this)
        textView.text = HtmlCompat.fromHtml(dialogMessage, HtmlCompat.FROM_HTML_MODE_LEGACY)

        val dialog = AlertDialog.Builder(this)
            .setTitle("User Guide")
            .setView(textView)
            .setPositiveButton("Close") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.show()
    }

    private fun showErrorMessage(message: String) {
        textViewSentence.text = message
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun getApiKey(context: Context): Pair<String?, Int> {
        try {
            val appInfo = context.packageManager.getApplicationInfo(context.packageName, PackageManager.GET_META_DATA)
            val apiKey = appInfo.metaData.getString("com.polylingo.api_key")
            if (apiKey.isNullOrEmpty()) {
                return Pair(null, 401) // Return null and error code 401 for authentication error
            }
            return Pair(apiKey, 200) // Return API key and success code 200
        } catch (e: Exception) {
            return Pair(null, 500) // Return null and error code 500 for server error
        }
    }

    private fun getTranslationFromApi(
        context: Context,
        view: View,
        translationAPI: TranslationAPI,
        word1: String,
        word2: String,
        word3: String,
        wordjyut1: String,
        wordjyut2: String,
        wordjyut3: String,
    ) {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val languageCode = when (selectedLanguage) {
                    "yue", "cmn", "nld" -> selectedLanguage
                    "spa" -> "es" // Spanish language code
                    "fra" -> "fr" // French language code
                    "ell" -> "el" // Greek language code
                    "eng" -> "en" // English language code
                    else -> selectedLanguage
                }
                val apiResponse1 = translationAPI.getTranslation(word1, "$languageCode|$selectedbaseLanguage").await()
                val apiResponse2 = translationAPI.getTranslation("$word2$word1", "yue|$selectedbaseLanguage").await()
                val apiResponse3 = translationAPI.getTranslation("$word1$word3", "yue|$selectedbaseLanguage").await()
                val translation1 = apiResponse1.responseData.translatedText
                val translation2 = apiResponse2.responseData.translatedText
                val translation3 = apiResponse3.responseData.translatedText
                val snackbar = if (selectedLanguage == "yue" || selectedLanguage == "cmn") {
                    val message =
                        "$word1: $wordjyut1: $translation1\n" +
                                "$word2$word1: $wordjyut2 $wordjyut1: $translation2\n" +
                                "$word1$word3: $wordjyut1 $wordjyut3: $translation3"
                    Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                } else {
                    val message = "$word1: $translation1"
                    Snackbar.make(view, message, Snackbar.LENGTH_LONG)
                }

                snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)?.apply {
                    maxLines = 5  // Set the maximum number of lines to accommodate the multiline message
                }

                snackbar.show()

            } catch (e: Exception) {
                val errorMessage = "Could not fetch translation: ${e.message}"
                Snackbar.make(view, errorMessage, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    private fun fetchRandomSentence() {
        val query = if (selectedLanguage == "yue") "@transcription ${getRandomCantoneseWord()}" else null
        try {
            api.searchSentences(selectedLanguage,translationsTo = "$selectedbaseLanguage", query = query).enqueue(object : Callback<SearchResponse> {
                override fun onResponse(call: Call<SearchResponse>, response: Response<SearchResponse>) {
                    if (response.isSuccessful) {
                        val searchResponse = response.body()
                        val sentenceIds = searchResponse?.results?.map { it.id } // Extract the list of sentence IDs
                        if (sentenceIds.isNullOrEmpty()) {
                            fetchRandomSentence()
                            return
                        }

                        Log.d("API_LOG", "Number of sentences: ${sentenceIds.size}")
                        val sentenceId = sentenceIds.random() // Choose a random sentence ID

                        api.getRandomSentence(sentenceId).enqueue(object : Callback<Sentence> {
                            @SuppressLint("SuspiciousIndentation", "SetTextI18n")
                            override fun onResponse(
                                call: Call<Sentence>,
                                response: Response<Sentence>
                            ) {
                                if (response.isSuccessful) {
                                    val sentence = response.body()
                                    // Extract the sentence com.polylingo.app.data.getText, English translation, and Latin transcription
                                    speechText= sentence?.text.toString()
                                    val sentenceText = sentence?.text
                                        ?.let {
                                            if (selectedLanguage == "cmn" || selectedLanguage == "yue") {
                                                if (it.contains(Regex("[a-zA-Z]")) || it.length > 35) {
                                                    fetchRandomSentence() // Fetch a new random sentence
                                                    return
                                                } else {
                                                    it
                                                }
                                            } else {
                                                it
                                            }
                                        }.toString()
                                    val translations = sentence?.translations?.getOrNull(0)
                                    val englishTranslation = translations?.find { it.lang == "$selectedbaseLanguage" }?.text ?: "No translation available"
                                    val latinTranscription = sentence?.transcriptions?.find { it.script == "Latn" }?.text ?: "No transcription available"

                                    // Replace a random word or character with an underscore
                                    if (sentenceText?.contains(" ") == true) {
                                        val words = sentenceText.split(" ").toMutableList()
                                        val index = (2 until words.size).random()
                                        missingWord = words[index]
                                        try {
                                            words[index] = "_"
                                        } catch (e: IndexOutOfBoundsException) {
                                            println("Error: Index out of bounds")
                                        }
                                        val sentenceWithBlank = words.joinToString(" ")
                                        textViewSentence.text = sentenceWithBlank
                                    } else {
                                        val index = sentenceText!!.indices.filter { sentenceText[it].isLetterOrDigit() }.randomOrNull()
                                        index?.let {
                                            try {
                                                previousWord = sentenceText[index-1].toString()
                                                missingWord = sentenceText[index].toString()
                                                nextWord = sentenceText[index+1].toString()
                                            } catch (e: Exception) {
                                                Log.e("API_ERROR", "Error previous/next/missing", e)
                                                // Handle the exception here, e.g. set default values for previousWord, missingWord, and nextWord
                                            }
                                            val sentenceWithBlank = sentenceText.replaceRange(index, index + 1, "_")
                                            textViewSentence.text = sentenceWithBlank
                                            if (latinTranscription.contains(" ")) {
                                                val delimiters = arrayOf(" ", "\n", "\t", ",", ".", "!", "?", ";", ":", "(", ")", "[", "]", "{", "}")
                                                val transcriptionWords = latinTranscription
                                                    .replace("¹", "¹ ")
                                                    .replace("²", "² ")
                                                    .replace("³", "³ ")
                                                    .replace("⁴", "⁴ ")
                                                    .replace("⁵", "⁵ ")
                                                    .replace("⁶", "⁶ ")
                                                    .replace("⁷", "⁷ ")
                                                    .replace("⁸", "⁸ ")
                                                    .replace("⁹", "⁹ ")
                                                    .replace("1", "¹ ")
                                                    .replace("2", "² ")
                                                    .replace("3", "³ ")
                                                    .replace("4", "⁴ ")
                                                    .replace("5", "⁵ ")
                                                    .replace("6", "⁶ ")
                                                    .replace("  ", " ")
                                                    .replace(", ", ",")
                                                    .split(*delimiters)
                                                    .toMutableList()
                                                if (index >= 2) {
                                                    previousjyutWord = transcriptionWords[index - 1].toString()
                                                } else {
                                                    previousjyutWord = ""
                                                }
                                                missingjyutWord = transcriptionWords[index].toString()
                                                nextjyutWord = transcriptionWords[index+1].toString()
                                                if (index < transcriptionWords.size) {
                                                    missingtranscriptionWord = transcriptionWords[index]
                                                    transcriptionWords[index] = "_"
                                                }
                                                val transcriptionWithBlank = transcriptionWords.joinToString(" ")
                                                textViewLatinTranscription.text = transcriptionWithBlank
                                            } else {
                                                textViewLatinTranscription.text = "No transcription available"
                                            }
                                        }
                                    }
                                    // Display the English translation
                                    textViewEnglishTranslation.text = englishTranslation

                                    // Speak the sentence using Text-to-Speech
                                    speakSentence(sentenceText)
                                    progressBar.incrementProgressBy(1)  // Increase progress by 1
                                    val currentProgress = progressBar.progress
                                    val maxProgress = progressBar.max
                                    progressText.text = "$currentProgress/$maxProgress  "

                                } else {
                                    showErrorMessage("Failed to fetch sentence")
                                }
                            }

                            override fun onFailure(call: Call<Sentence>, t: Throwable) {
                                showErrorMessage("Failed to fetch sentence")
                                Log.e("API_ERROR", "Failed to fetch sentence", t)
                            }
                        })
                    } else {
                        showErrorMessage("Failed to fetch sentences")
                    }
                }

                override fun onFailure(call: Call<SearchResponse>, t: Throwable) {
                    showErrorMessage("Failed to fetch sentences")
                    Log.e("API_ERROR", "Failed to fetch sentences2", t)
                }
            })
        } catch (e: Exception) {
            showErrorMessage("Exception occurred while fetching sentences")
            Log.e("API_ERROR", "Exception occurred while fetching sentences", e)
        }
    }

    private fun createOkHttpClient(): OkHttpClient {
        val cacheSize = (5 * 1024 * 1024).toLong()
        val cache = Cache(cacheDir, cacheSize)

        return OkHttpClient.Builder()
            .cache(cache)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .header("Cache-Control", "public, max-age=" + 5).build()
                chain.proceed(request)
            }
            .build()
    }
    private fun speakSentence(sentence: String?) {
        sentence?.let {
            if (isTtsEnabled && textToSpeech.isSpeaking) {
                textToSpeech.stop()
            }

            if (isTtsEnabled) {
                textToSpeech.speak(sentence, TextToSpeech.QUEUE_FLUSH, null, null)
            }
        }
    }


    private fun getRandomCantoneseWord(): String {
        val jyutwordList = listOf(
            "ngo", "nei", "keoi", "ge", "aa", "gam", "dou", "zau", "hai", "hou", "heoi", "ng", "go", "zo", "jau", "tung", "laa", "gong", "jiu", "di", "waa", "ho", "jat", "wui", "zan", "zi", "me", "ji", "soeng", "mou", "zou", "dak", "je", "sin", "gwo"
        )
        return jyutwordList.shuffled().first()
    }
}