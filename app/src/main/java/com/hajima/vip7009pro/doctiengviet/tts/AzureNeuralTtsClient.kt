package com.hajima.vip7009pro.doctiengviet.tts

import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

class AzureNeuralTtsClient {

    companion object {
        private const val TOKEN_URL_TEMPLATE =
            "https://%s.api.cognitive.microsoft.com/sts/v1.0/issueToken"
        private const val SYNTH_URL_TEMPLATE =
            "https://%s.tts.speech.microsoft.com/cognitiveservices/v1"
        private const val OUTPUT_FORMAT = "audio-24khz-96kbitrate-mono-mp3"
        private val SSML_MEDIA_TYPE = "application/ssml+xml".toMediaType()
    }

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(40, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .build()

    @Throws(IOException::class)
    fun synthesizeMp3(
        text: String,
        subscriptionKey: String,
        region: String,
        voiceName: String,
        pitchMultiplier: Float,
        speedMultiplier: Float
    ): ByteArray {
        val token = fetchAccessToken(subscriptionKey, region)
        val ssml = buildSsml(text, voiceName, pitchMultiplier, speedMultiplier)

        val request = Request.Builder()
            .url(String.format(Locale.US, SYNTH_URL_TEMPLATE, region))
            .addHeader("Authorization", "Bearer $token")
            .addHeader("Content-Type", "application/ssml+xml")
            .addHeader("X-Microsoft-OutputFormat", OUTPUT_FORMAT)
            .addHeader("User-Agent", "DocTiengViet2")
            .post(ssml.toRequestBody(SSML_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Azure TTS failed: ${response.code} ${response.message}")
            }
            val body = response.body ?: throw IOException("Azure TTS returned empty audio data")
            return body.bytes()
        }
    }

    @Throws(IOException::class)
    private fun fetchAccessToken(subscriptionKey: String, region: String): String {
        val request = Request.Builder()
            .url(String.format(Locale.US, TOKEN_URL_TEMPLATE, region))
            .addHeader("Ocp-Apim-Subscription-Key", subscriptionKey)
            .addHeader("Content-Length", "0")
            .post(ByteArray(0).toRequestBody())
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("Cannot get Azure token: ${response.code} ${response.message}")
            }
            val body = response.body ?: throw IOException("Azure token response is empty")
            val token = body.string()
            if (token.trim().isEmpty()) {
                throw IOException("Azure token is empty")
            }
            return token
        }
    }

    private fun buildSsml(
        text: String,
        voiceName: String,
        pitchMultiplier: Float,
        speedMultiplier: Float
    ): String {
        val escapedText = escapeXml(text)
        val rate = toAzureRate(speedMultiplier)
        val pitch = toAzurePitch(pitchMultiplier)

        return "<speak version='1.0' xml:lang='vi-VN'>" +
            "<voice name='$voiceName'>" +
            "<prosody rate='$rate' pitch='$pitch'>" +
            escapedText +
            "</prosody>" +
            "</voice>" +
            "</speak>"
    }

    private fun toAzureRate(speedMultiplier: Float): String {
        var value = Math.round((speedMultiplier - 1f) * 100f)
        value = clamp(value, -50, 100)
        return (if (value >= 0) "+" else "") + value + "%"
    }

    private fun toAzurePitch(pitchMultiplier: Float): String {
        var value = Math.round((pitchMultiplier - 1f) * 8f)
        value = clamp(value, -12, 12)
        return (if (value >= 0) "+" else "") + value + "st"
    }

    private fun clamp(value: Int, min: Int, max: Int): Int {
        if (value < min) return min
        if (value > max) return max
        return value
    }

    private fun escapeXml(value: String): String {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
