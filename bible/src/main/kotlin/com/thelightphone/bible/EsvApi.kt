package com.thelightphone.bible

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URLEncoder
import kotlin.text.Charsets.UTF_8

@Serializable
internal data class EsvPassageResponse(
    val passages: List<String> = emptyList(),
)

@Serializable
internal data class EsvSearchResponse(
    val results: List<EsvSearchResult> = emptyList(),
    @SerialName("total_results") val totalResults: Int = 0,
)

@Serializable
internal data class EsvSearchResult(
    val reference: String,
    val content: String,
)

data class SearchHit(val book: Book, val chapter: Int, val verse: Int, val text: String)

private val REFERENCE_PATTERN = Regex("""^(.+?)\s+(\d+):(\d+)$""")
private val VERSE_MARKER_PATTERN = Regex("""\[(\d+)]""")

internal class EsvApi {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    // Separate client that doesn't follow redirects: the audio endpoint responds with a
    // 302 to the actual mp3 file, and we only want the Location header, not the audio bytes.
    // Both the engine's own redirect handling and Ktor's HttpRedirect plugin need to be
    // disabled - the plugin follows redirects by default regardless of the engine setting.
    private val redirectClient = HttpClient(OkHttp) {
        followRedirects = false
        engine { config { followRedirects(false) } }
        expectSuccess = false
    }

    suspend fun fetchPassage(apiKey: String, book: String, chapter: Int): Result<List<Verse>> = runCatching {
        val query = URLEncoder.encode("$book $chapter", UTF_8.name())
        val response = client.get(
            "https://api.esv.org/v3/passage/text/" +
                "?q=$query&include-headings=false&include-footnotes=false" +
                "&include-verse-numbers=true&include-short-copyright=false&include-passage-references=false",
        ) {
            header("Authorization", "Token $apiKey")
        }

        if (!response.status.isSuccess()) {
            val body = response.bodyAsText().take(500)
            throw IllegalStateException("ESV passage HTTP ${response.status.value}: $body")
        }

        val passage: EsvPassageResponse = response.body()
        val text = passage.passages.joinToString("")
        parseVerseMarkers(text)
    }

    suspend fun fetchAudioUrl(apiKey: String, book: String, chapter: Int): Result<String> = runCatching {
        val query = URLEncoder.encode("$book $chapter", UTF_8.name())
        val response = redirectClient.get(
            "https://api.esv.org/v3/passage/audio/?q=$query",
        ) {
            header("Authorization", "Token $apiKey")
        }
        response.headers[HttpHeaders.Location]
            ?: throw IllegalStateException("ESV audio HTTP ${response.status.value}: no redirect location")
    }

    suspend fun search(apiKey: String, query: String): Result<List<SearchHit>> = runCatching {
        val encoded = URLEncoder.encode(query, UTF_8.name())
        val response = client.get(
            "https://api.esv.org/v3/passage/search/?q=$encoded&page-size=50",
        ) {
            header("Authorization", "Token $apiKey")
        }

        if (!response.status.isSuccess()) {
            val body = response.bodyAsText().take(500)
            throw IllegalStateException("ESV search HTTP ${response.status.value}: $body")
        }

        val search: EsvSearchResponse = response.body()
        search.results.mapNotNull { it.toSearchHit() }
    }

    fun close() {
        client.close()
        redirectClient.close()
    }
}

private fun EsvSearchResult.toSearchHit(): SearchHit? {
    val match = REFERENCE_PATTERN.matchEntire(reference.trim()) ?: return null
    val (bookName, chapterStr, verseStr) = match.destructured
    val book = findBook(bookName) ?: return null
    return SearchHit(book, chapterStr.toInt(), verseStr.toInt(), content.trim())
}

private fun parseVerseMarkers(text: String): List<Verse> {
    val markers = VERSE_MARKER_PATTERN.findAll(text).toList()
    return markers.mapIndexed { i, match ->
        val number = match.groupValues[1].toInt()
        val start = match.range.last + 1
        val end = markers.getOrNull(i + 1)?.range?.first ?: text.length
        Verse(number, text.substring(start, end).trim())
    }
}
