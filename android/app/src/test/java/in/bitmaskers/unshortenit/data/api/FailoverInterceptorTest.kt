package `in`.bitmaskers.unshortenit.data.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.io.IOException

class FailoverInterceptorTest {

    private val primaryUrl = "https://unshorten-backend.bitmaskers.in"
    private val fallbackUrl = "https://unshorten-backend2.bitmaskers.in"

    @Before
    fun setup() {
        mockkStatic(android.util.Log::class)
        every { android.util.Log.d(any(), any()) } returns 0
        every { android.util.Log.i(any(), any()) } returns 0
        every { android.util.Log.w(any(), any<String>()) } returns 0
        every { android.util.Log.e(any(), any<String>()) } returns 0
    }

    @After
    fun tearDown() {
        unmockkStatic(android.util.Log::class)
    }

    private fun createDummyResponse(request: Request, code: Int, body: String = "{}"): Response {
        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Error")
            .body(body.toResponseBody("application/json".toMediaType()))
            .build()
    }

    @Test
    fun `primary 200 OK succeeds without calling fallback`() {
        val interceptor = FailoverInterceptor(primaryUrl, fallbackUrl)
        val request = Request.Builder().url("$primaryUrl/api/v1/unshorten").build()

        val chain = mockk<Interceptor.Chain>()
        val requestedHosts = mutableListOf<String>()

        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val req = firstArg<Request>()
            requestedHosts.add(req.url.host)
            createDummyResponse(req, 200, "{\"status\":\"ok\"}")
        }

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(listOf("unshorten-backend.bitmaskers.in"), requestedHosts)
    }

    @Test
    fun `primary 400 Bad Request is returned without calling fallback`() {
        val interceptor = FailoverInterceptor(primaryUrl, fallbackUrl)
        val request = Request.Builder().url("$primaryUrl/api/v1/unshorten").build()

        val chain = mockk<Interceptor.Chain>()
        val requestedHosts = mutableListOf<String>()

        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val req = firstArg<Request>()
            requestedHosts.add(req.url.host)
            createDummyResponse(req, 400, "{\"error\":\"bad url\"}")
        }

        val response = interceptor.intercept(chain)

        assertEquals(400, response.code)
        assertEquals(listOf("unshorten-backend.bitmaskers.in"), requestedHosts)
    }

    @Test
    fun `primary 502 Bad Gateway triggers fallback to secondary URL`() {
        val interceptor = FailoverInterceptor(primaryUrl, fallbackUrl)
        val request = Request.Builder().url("$primaryUrl/api/v1/unshorten").build()

        val chain = mockk<Interceptor.Chain>()
        val requestedHosts = mutableListOf<String>()

        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val req = firstArg<Request>()
            requestedHosts.add(req.url.host)
            if (req.url.host == "unshorten-backend.bitmaskers.in") {
                createDummyResponse(req, 502, "Bad Gateway")
            } else {
                createDummyResponse(req, 200, "{\"status\":\"fallback_ok\"}")
            }
        }

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(
            listOf("unshorten-backend.bitmaskers.in", "unshorten-backend2.bitmaskers.in"),
            requestedHosts
        )
    }

    @Test
    fun `primary IOException triggers fallback to secondary URL`() {
        val interceptor = FailoverInterceptor(primaryUrl, fallbackUrl)
        val request = Request.Builder().url("$primaryUrl/api/v1/unshorten").build()

        val chain = mockk<Interceptor.Chain>()
        val requestedHosts = mutableListOf<String>()

        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val req = firstArg<Request>()
            requestedHosts.add(req.url.host)
            if (req.url.host == "unshorten-backend.bitmaskers.in") {
                throw IOException("Connection refused")
            } else {
                createDummyResponse(req, 200, "{\"status\":\"fallback_ok\"}")
            }
        }

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(
            listOf("unshorten-backend.bitmaskers.in", "unshorten-backend2.bitmaskers.in"),
            requestedHosts
        )
    }

    @Test
    fun `empty fallback URL proceeds normally without fallback`() {
        val interceptor = FailoverInterceptor(primaryUrl, "")
        val request = Request.Builder().url("$primaryUrl/api/v1/unshorten").build()

        val chain = mockk<Interceptor.Chain>()
        val requestedHosts = mutableListOf<String>()

        every { chain.request() } returns request
        every { chain.proceed(any()) } answers {
            val req = firstArg<Request>()
            requestedHosts.add(req.url.host)
            createDummyResponse(req, 200)
        }

        val response = interceptor.intercept(chain)

        assertEquals(200, response.code)
        assertEquals(listOf("unshorten-backend.bitmaskers.in"), requestedHosts)
    }
}
