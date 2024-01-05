package com.polylingo.data

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader

data class excelSentence(
    val cantonese: String,
    val english: String,
    val jyutping: String
)

fun readSentencesFromCSV(context: Context, resourceId: Int): List<excelSentence> {
    val sentences = mutableListOf<excelSentence>()
    val inputStream = context.resources.openRawResource(resourceId)
    val reader = BufferedReader(InputStreamReader(inputStream))
    reader.use { r ->
        r.readLine() // Skip header line if exists
        while (true) {
            val line = r.readLine() ?: break
            val tokens = line.split(",")
            if (tokens.size >= 5) {
                val sentence = excelSentence(
                    cantonese = tokens[1],
                    english = tokens[3],  // English translations are in the fourth column (index 3).
                    jyutping = tokens[2]
                )
                sentences.add(sentence)
            }
        }
    }
    return sentences
}
