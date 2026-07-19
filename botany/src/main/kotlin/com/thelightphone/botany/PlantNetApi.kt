package com.thelightphone.botany

import android.graphics.Bitmap
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import java.io.ByteArrayOutputStream
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private const val PLANTNET_IDENTIFY_URL = "https://my-api.plantnet.org/v2/identify/all"

/** What we actually know about an identification - deliberately does not include native-range
 *  or toxicity, since Pl@ntNet's API doesn't provide either and guessing would be unsafe. */
data class PlantIdentification(
    val commonName: String,
    val latinName: String,
    val family: String,
    val confidencePercent: Int,
    val description: String,
)

@Serializable
internal data class PlantNetResponse(val results: List<PlantNetResult> = emptyList())

@Serializable
internal data class PlantNetResult(
    val score: Double = 0.0,
    val species: PlantNetSpecies,
)

@Serializable
internal data class PlantNetSpecies(
    @SerialName("scientificNameWithoutAuthor") val scientificName: String,
    val commonNames: List<String> = emptyList(),
    val family: PlantNetTaxon? = null,
    val genus: PlantNetTaxon? = null,
)

@Serializable
internal data class PlantNetTaxon(
    @SerialName("scientificNameWithoutAuthor") val scientificName: String,
)

internal class PlantNetApi {
    private val client = HttpClient(OkHttp) {
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    suspend fun identify(photo: Bitmap, apiKey: String): Result<PlantIdentification> = runCatching {
        check(apiKey.isNotBlank()) { "Missing Pl@ntNet API key - set plantnet.apiKey in local.properties" }

        val jpegBytes = ByteArrayOutputStream().use { stream ->
            photo.compress(Bitmap.CompressFormat.JPEG, 90, stream)
            stream.toByteArray()
        }

        val response = client.post("$PLANTNET_IDENTIFY_URL?api-key=$apiKey&nb-results=1") {
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("organs", "auto")
                        append(
                            "images",
                            jpegBytes,
                            Headers.build {
                                append(HttpHeaders.ContentType, "image/jpeg")
                                append(HttpHeaders.ContentDisposition, "filename=\"plant.jpg\"")
                            },
                        )
                    },
                ),
            )
        }

        if (!response.status.isSuccess()) {
            val body = response.bodyAsText().take(500)
            throw IllegalStateException("Pl@ntNet HTTP ${response.status.value}: $body")
        }

        val parsed: PlantNetResponse = response.body()
        val top = parsed.results.firstOrNull() ?: throw IllegalStateException("No match found for this photo")
        top.toIdentification()
    }

    fun close() = client.close()
}

private fun PlantNetResult.toIdentification(): PlantIdentification {
    val family = species.family?.scientificName ?: species.genus?.scientificName ?: "Unknown family"
    val commonName = species.commonNames.firstOrNull() ?: species.scientificName
    return PlantIdentification(
        commonName = commonName,
        latinName = species.scientificName,
        family = family,
        confidencePercent = (score * 100).toInt().coerceIn(0, 100),
        description = "Identified as ${species.scientificName}, a member of the $family family.",
    )
}
