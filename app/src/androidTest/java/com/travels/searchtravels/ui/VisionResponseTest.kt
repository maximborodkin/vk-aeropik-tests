package com.travels.searchtravels.ui

import android.graphics.BitmapFactory
import android.util.Log
import androidx.annotation.IdRes
import androidx.test.rule.ActivityTestRule
import com.google.api.client.extensions.android.http.AndroidHttp
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.json.JsonFactory
import com.google.api.client.json.gson.GsonFactory
import com.google.api.client.util.ArrayMap
import com.google.api.services.vision.v1.Vision
import com.google.api.services.vision.v1.VisionRequestInitializer
import com.google.api.services.vision.v1.model.*
import com.preview.planner.Define.API_KEY
import com.travels.searchtravels.R
import com.travels.searchtravels.activity.MainActivity
import com.travels.searchtravels.utils.ImageHelper
import org.junit.Rule
import org.junit.Test

/**
 * Shows vision api reaction for different images from [com.travels.searchtravels.R.drawable]
 * (Пишу тесты второй раз в жизни. Поэтому сделал, чтобы работало)
 */

class VisionResponseTest {

    @get:Rule
    var activityTestRule = ActivityTestRule(MainActivity::class.java, false, true)

    @Test
    fun detectImages() {
        val categories = ArrayList<Pair<String, Int>>().apply {
            add(Pair("sea", R.drawable.sea))
            add(Pair("ocean", R.drawable.ocean))
            add(Pair("beach", R.drawable.beach))
            add(Pair("mountains", R.drawable.mountains))
            add(Pair("snow", R.drawable.snow))
        }

        activityTestRule.run {
            categories.forEach {
                Log.d("DETECTION_TEST", "Start test with ${it.first} image")
                val result = detectImage(it.second)
                val hasMatch = findMatches(it.first, result)
                assert(hasMatch)
                Log.d("DETECTION_TEST", "${it.first} was recognized\n")
            }
        }
    }



    private fun detectImage(@IdRes imageResourceId: Int): BatchAnnotateImagesResponse? {

        val credential = GoogleCredential().setAccessToken("")
        val httpTransport = AndroidHttp.newCompatibleTransport()
        val jsonFactory: JsonFactory = GsonFactory.getDefaultInstance()

        val vision = Vision.Builder(httpTransport, jsonFactory, credential)
            .setVisionRequestInitializer(VisionRequestInitializer(API_KEY))
            .build()

        val featureList = ArrayList<Feature>().apply {
            add(Feature().apply {
                type = "WEB_DETECTION"
                maxResults = 10
            })
            add(Feature().apply {
                type = "LANDMARK_DETECTION"
                maxResults = 10
            })
        }

        val imageList = ArrayList<AnnotateImageRequest>().apply {
            add(AnnotateImageRequest().apply {
                val bitmap = BitmapFactory.decodeResource(activityTestRule.activity.resources, R.drawable.sea)
                image = ImageHelper.getBase64EncodedJpeg(bitmap)
                features = featureList
            })
        }

        val batchAnnotateImagesRequest =
            BatchAnnotateImagesRequest().apply { requests = imageList }

        return vision
            .images()
            .annotate(batchAnnotateImagesRequest).apply { disableGZipContent = true }
            .execute()
    }

    private fun findMatches(target: String, result: BatchAnnotateImagesResponse?): Boolean {
        if (result == null) return false
        result.responses.forEach { annotateImageResponse: AnnotateImageResponse ->
            ((annotateImageResponse["webDetection"] as ArrayMap<*, *>)["webEntities"] as ArrayList<*>).forEach {
                if((it as ArrayMap<*, *>)["description"].toString().toLowerCase().contains(target)) return true
            }
        }
        return false
    }
}
