package app.stil.launcher.data

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.random.Random
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
data class Photo(val id: String, val author: String, val url: String, val width: Int, val height: Int)
data class DownloadedPhoto(val photo: Photo, val bytes: ByteArray)
fun interface PhotoSource { fun download(previousId: String): DownloadedPhoto }

class PhotoClient(
    private val baseUrl: HttpUrl = "https://picsum.photos/".toHttpUrl(),
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build(),
) : PhotoSource {
    private val json = Json { ignoreUnknownKeys = true }

    override fun download(previousId: String): DownloadedPhoto {
        val page = Random.nextInt(1, 11)
        val listUrl = baseUrl.newBuilder().addPathSegments("v2/list")
            .addQueryParameter("page", page.toString()).addQueryParameter("limit", "30").build()
        val photos = client.newCall(Request.Builder().url(listUrl).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Photo list returned HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty photo list")
            val payload = readBounded(body.byteStream(), 512 * 1024)
            try {
                json.decodeFromString<List<Photo>>(payload.toString(Charsets.UTF_8))
            } catch (e: IllegalArgumentException) {
                throw IOException("Invalid photo list", e)
            }
        }
        val candidates = photos.filter {
            it.id != previousId && it.id.matches(Regex("[0-9]+")) &&
                it.width >= 1080 && it.height >= 720 && it.url.startsWith("https://unsplash.com/") && it.author.isNotBlank()
        }
        val photo = candidates.randomOrNull() ?: throw IOException("No suitable photos")
        val imageUrl = baseUrl.newBuilder().addPathSegments("id/${photo.id}/1080/2160.jpg").build()
        val bytes = client.newCall(Request.Builder().url(imageUrl).build()).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Photo returned HTTP ${response.code}")
            val body = response.body ?: throw IOException("Empty photo")
            if (body.contentType()?.type != "image") throw IOException("Response is not an image")
            readBounded(body.byteStream(), 10 * 1024 * 1024)
        }
        return DownloadedPhoto(photo, bytes)
    }

    private fun readBounded(input: java.io.InputStream, limit: Int): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        input.use {
            while (true) {
                val count = it.read(buffer)
                if (count == -1) break
                if (output.size() + count > limit) throw IOException("Response exceeds size limit")
                output.write(buffer, 0, count)
            }
        }
        return output.toByteArray()
    }
}
