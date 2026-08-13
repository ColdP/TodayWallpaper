package btm.m.todaywallpaper

import btm.m.todaywallpaper.data.model.PixabayResponse
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PixabayResponseParsingTest {

    @Test
    fun parsesStandardResponseWithoutFullAccessUrls() {
        val json = """
            {
              "total": 4692,
              "totalHits": 500,
              "hits": [
                {
                  "id": 195893,
                  "pageURL": "https://pixabay.com/photos/flower-195893/",
                  "tags": "blossom, bloom, flower",
                  "previewURL": "https://cdn.pixabay.com/preview.jpg",
                  "webformatURL": "https://pixabay.com/get/flower_640.jpg",
                  "largeImageURL": "https://pixabay.com/get/flower_1280.jpg",
                  "user_id": 48777,
                  "user": "Contributor"
                }
              ]
            }
        """.trimIndent()
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()

        val response = moshi.adapter(PixabayResponse::class.java).fromJson(json)!!

        assertEquals(500, response.totalHits)
        assertEquals(195893L, response.hits.single().id)
        assertNull(response.hits.single().fullHdUrl)
        assertNull(response.hits.single().imageUrl)
    }
}