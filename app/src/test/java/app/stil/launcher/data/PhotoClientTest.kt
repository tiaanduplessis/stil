package app.stil.launcher.data

import java.io.IOException
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class PhotoClientTest {
    private lateinit var server: MockWebServer
    @Before fun start() { server = MockWebServer(); server.start() }
    @After fun stop() { server.shutdown() }
    private val photo = """{"id":"42","author":"Photographer","url":"https://unsplash.com/photos/example","width":2000,"height":1500}"""
    private fun client() = PhotoClient(server.url("/"))

    @Test fun `downloads bounded photo with attribution and portrait sizing`() {
        server.enqueue(MockResponse().setBody("[$photo]"))
        server.enqueue(MockResponse().setHeader("Content-Type", "image/jpeg").setBody("photo bytes"))
        val result = client().download("old")
        assertEquals("42", result.photo.id)
        assertEquals("Photographer", result.photo.author)
        assertEquals("photo bytes", result.bytes.toString(Charsets.UTF_8))
        assertTrue(server.takeRequest().path!!.startsWith("/v2/list?page="))
        assertEquals("/id/42/1080/2160.jpg", server.takeRequest().path)
    }

    @Test fun `previous image cannot be immediately repeated`() {
        server.enqueue(MockResponse().setBody("[$photo]"))
        assertThrows(IOException::class.java) { client().download("42") }
        assertEquals(1, server.requestCount)
    }

    @Test fun `server failure is observable instead of replacing current photo`() {
        server.enqueue(MockResponse().setResponseCode(503))
        assertThrows(IOException::class.java) { client().download("") }
    }

    @Test fun `invalid JSON is a recoverable provider failure`() {
        server.enqueue(MockResponse().setBody("not json"))
        assertThrows(IOException::class.java) { client().download("") }
    }

    @Test fun `untrusted source and path traversal identifiers are rejected`() {
        server.enqueue(MockResponse().setBody("[${photo.replace("42", "../secret")}]"))
        assertThrows(IOException::class.java) { client().download("") }
        server.enqueue(MockResponse().setBody("[${photo.replace("https://unsplash.com/", "https://untrusted.test/")}]"))
        assertThrows(IOException::class.java) { client().download("") }
        assertEquals(2, server.requestCount)
    }

    @Test fun `HTML response cannot be treated as a wallpaper`() {
        server.enqueue(MockResponse().setBody("[$photo]"))
        server.enqueue(MockResponse().setHeader("Content-Type", "text/html").setBody("<html>Error</html>"))
        assertThrows(IOException::class.java) { client().download("") }
    }

    @Test fun `oversized metadata is rejected before image download`() {
        server.enqueue(MockResponse().setBody("x".repeat(512 * 1024 + 1)))
        assertThrows(IOException::class.java) { client().download("") }
        assertEquals(1, server.requestCount)
    }
}
