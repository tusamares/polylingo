package com.polylingo.data

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.JsonObject
import org.json.JSONObject
import com.polylingo.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Response
import com.polylingo.MainActivity
import android.content.Intent
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.AdapterView
import android.widget.ScrollView
import android.widget.Spinner
import android.speech.tts.TextToSpeech
import android.widget.Toast
import java.util.Locale

class ChatActivity : AppCompatActivity(), TextToSpeech.OnInitListener {
    // Define the Retrofit object
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl("https://api.openai.com/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // Create the OpenAI API service
    private val service = retrofit.create(OpenAIApi::class.java)
    private var selectedLanguage = "yue"
    private lateinit var language: String
    private var isTtsEnabled = true
    private var lastAssistantMessage: String? = null

    // Initialize the conversation history with a system message
    private val conversationHistory = mutableListOf<MessageTurbo>()

    private var selectedSpeed: Float = 0.8f // Default speed
    private lateinit var textToSpeech: TextToSpeech

    private fun enableTextToSpeech() {
        isTtsEnabled = true
    }
    private fun disableTextToSpeech() {
        isTtsEnabled = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this, this)

        val languageSpinner: Spinner = findViewById(R.id.spinner_languages)

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

        languageSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                language = parent.getItemAtPosition(pos) as String
                selectedLanguage = when (language) {
                    "Cantonese" -> "yue"
                    "Mandarin" -> "cmn"
                    "Dutch" -> "nld"
                    "Spanish" -> "spa"
                    "French" -> "fra"
                    "Greek" -> "ell"
                    // add more mappings as needed
                    else -> "yue"
                }

                // Stop the TextToSpeech instance if it's speaking
                if (::textToSpeech.isInitialized && textToSpeech.isSpeaking) {
                    textToSpeech.stop()
                }

                // Update the locale of the TextToSpeech instance
                if (::textToSpeech.isInitialized) {
                    val locale = Locale(selectedLanguage)
                    val result = textToSpeech.setLanguage(locale)
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Toast.makeText(this@ChatActivity, "Text-to-Speech language not supported", Toast.LENGTH_SHORT).show()
                    }
                }

                clearChatLog()
                // Handle the selected language here
                val systemMessage =
                    "Act as a language tutor and teach your conversation partner how to speak colloquial $language. Engage in casual conversation but every time you get a message do the following:\n\n1) Review the message. If there are no language mistakes (grammar, spelling, or word order), continue the conversation by asking a question and ignore the next steps.\n\n2) Use English for this step: Review the message, identify any mistakes, explain the mistakes in English, and suggest improvements in English.\n\n3) Use $language for this step: Act as a conversation partner and continue the conversation by asking a question."
                conversationHistory.clear()
                conversationHistory.add(
                    MessageTurbo(
                        content = systemMessage,
                        role = TurboRole.system
                    )
                )
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Handle the case where no language is selected if necessary
            }
        }

        // Set click listener for the back button
        val backButton = findViewById<Button>(R.id.backButton)
        backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
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

                // Check if textToSpeech has been initialized before using it
                if (::textToSpeech.isInitialized) {
                    // Apply the new speed to the Text-to-Speech engine
                    textToSpeech.setSpeechRate(selectedSpeed)
                }

                // Stop the TextToSpeech instance if it's speaking
                if (::textToSpeech.isInitialized && textToSpeech.isSpeaking) {
                    textToSpeech.stop()
                }

                // Speak the current sentence with the updated speed
                speakSentence(lastAssistantMessage)
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // Another interface callback
            }
        }

        // Set the default speed spinner position to 3 (Turn Off option)
        speedSpinner.setSelection(3)
    } // Closing brace for onCreate method

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
        super.onDestroy()
        // Shutdown the TextToSpeech object
        textToSpeech.shutdown()
    }

    fun sendMessage(view: View) {
        val inputField = findViewById<EditText>(R.id.input_field)
        val message = inputField.text.toString()
        // Add the user's message to the conversation history
        conversationHistory.add(MessageTurbo(content = message, role = TurboRole.user))

        val textCompletionsParam = TextCompletionsParam(messagesTurbo = conversationHistory)

        val requestBody =
            textCompletionsParam.toJson().toString().toRequestBody("application/json".toMediaType())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response: Response<ResponseBody> =
                    service.textCompletionsWithStream(requestBody).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        val chatLog = findViewById<TextView>(R.id.chat_log)
                        val assistantMessage =
                            JSONObject(responseBody?.string()).getJSONArray("choices")
                                .getJSONObject(0).getJSONObject("message").getString("content")

                        // Add the assistant's message to the conversation history
                        conversationHistory.add(
                            MessageTurbo(
                                content = assistantMessage,
                                role = TurboRole.assistant
                            )
                        )

                        // Store the assistant's message
                        lastAssistantMessage = assistantMessage

                        // Append the user's and assistant's messages to the chat log
                        chatLog.append("\nUser: $message")
                        chatLog.append("\nAI tutor: $assistantMessage")
                        // Speak the assistant's message
                        speakSentence(assistantMessage)
                        // Request focus and set cursor in the input field
                        inputField.requestFocus()
                    } else {
                        // TODO: Handle the error case
                    }
                }
            } catch (e: Exception) {
                // TODO: Handle network exception
            }
        }

        // After updating the chat log, scroll to the bottom
        val scrollView = findViewById<ScrollView>(R.id.scrollView)
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
        inputField.text.clear()
    }

    private fun clearChatLog() {
        val chatLog = findViewById<TextView>(R.id.chat_log)
        chatLog.text = ""
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
} // Closing brace for ChatActivity class
