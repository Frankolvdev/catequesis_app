package com.chayzay.catequesisapp.news

import com.chayzay.catequesisapp.BuildConfig
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import org.json.JSONObject

data class XPost(
    val id: String,
    val text: String,
    val createdAt: String,
    val likeCount: Int,
    val repostCount: Int,
    val replyCount: Int
)

object XTimelineRepository {
    private const val API = "https://api.x.com/2"

    fun isConfigured(): Boolean = BuildConfig.X_BEARER_TOKEN.isNotBlank()

    fun loadUserTimeline(username: String): List<XPost> {
        val encoded = URLEncoder.encode(username, Charsets.UTF_8.name())
        val userJson = request("$API/users/by/username/$encoded")
        val userId = userJson.optJSONObject("data")?.optString("id").orEmpty()
        if (userId.isBlank()) throw IllegalStateException("X no devolvió el usuario @$username")

        val timeline = request(
            "$API/users/$userId/tweets" +
                "?max_results=20&exclude=retweets,replies" +
                "&tweet.fields=created_at,public_metrics"
        )
        val data = timeline.optJSONArray("data") ?: return emptyList()
        return buildList {
            for (i in 0 until data.length()) {
                val item = data.getJSONObject(i)
                val metrics = item.optJSONObject("public_metrics")
                add(
                    XPost(
                        id = item.optString("id"),
                        text = item.optString("text"),
                        createdAt = item.optString("created_at"),
                        likeCount = metrics?.optInt("like_count") ?: 0,
                        repostCount = metrics?.optInt("retweet_count") ?: 0,
                        replyCount = metrics?.optInt("reply_count") ?: 0
                    )
                )
            }
        }
    }

    private fun request(url: String): JSONObject {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 12000
            readTimeout = 12000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("Authorization", "Bearer ${BuildConfig.X_BEARER_TOKEN}")
        }
        try {
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val detail = runCatching { JSONObject(body).optString("detail") }.getOrNull()
                throw IllegalStateException(detail?.takeIf { it.isNotBlank() } ?: "X API HTTP $code")
            }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }
}
