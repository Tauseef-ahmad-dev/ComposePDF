package io.github.tauseefahmad.composepdf.core

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/** Downloads remote PDF sources consistently before handing bytes to a platform renderer. */
internal suspend fun PdfSource.downloadIfRemote(): PdfSource = when (this) {
    is PdfSource.Url -> {
        require(url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)) {
            "PDF URL must use HTTP or HTTPS."
        }
        val requestHeaders = headers
        val client = HttpClient(CIO)
        try {
            val response = client.get(url) {
                headers { requestHeaders.forEach { (name, value) -> append(name, value) } }
            }
            if (!response.status.isSuccess()) {
                throw PdfDownloadException(response.status.value, "PDF download failed with HTTP ${response.status.value}.")
            }
            val bytes: ByteArray = response.body()
            require(bytes.isNotEmpty()) { "The remote PDF is empty." }
            PdfSource.Bytes(bytes)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } finally {
            client.close()
        }
    }
    else -> this
}

/** HTTP response failure while loading a remote PDF. */
class PdfDownloadException(val statusCode: Int, message: String) : IllegalStateException(message)
