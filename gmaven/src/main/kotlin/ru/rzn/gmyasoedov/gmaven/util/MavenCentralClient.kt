package ru.rzn.gmyasoedov.gmaven.util

import com.google.gson.Gson
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.HttpRequests.JSON_CONTENT_TYPE
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

object MavenCentralClient {
    private const val ROW_COUNT = 50
    private const val SEARCH_BY_GROUP_AND_ARTIFACT_URL =
        "https://search.maven.org/solrsearch/select?q=g:%s+AND+a:%s&core=gav&rows=${ROW_COUNT}&wt=json"
    private const val SEARCH_BY_ARTIFACT_URL =
        "https://search.maven.org/solrsearch/select?q=%s&rows=${ROW_COUNT}&wt=json"

    private val errorCount = AtomicLong()
    private val gson = Gson()
    private val connectTimeoutMillis: Int = Duration.ofSeconds(2).toMillis().toInt()
    private val readTimeoutMillis: Int = Duration.ofSeconds(3).toMillis().toInt()

    @JvmStatic
    fun find(group: String, artifact: String): List<MavenCentralArtifactInfo> {
        return if (group.length > 1 && artifact.length > 1)
            findByUrl(SEARCH_BY_GROUP_AND_ARTIFACT_URL.format(group, artifact))
        else emptyList()
    }

    @JvmStatic
    fun find(query: String): List<MavenCentralArtifactInfo> {
        return if (query.length > 2) findByUrl(SEARCH_BY_ARTIFACT_URL.format(query)) else emptyList()
    }

    private fun findByUrl(url: String): List<MavenCentralArtifactInfo> {
        if (errorCount.get() > 30) return emptyList()
        return try {
            val string = HttpRequests.request(url)
                .readTimeout(readTimeoutMillis)
                .connectTimeout(connectTimeoutMillis)
                .accept(JSON_CONTENT_TYPE)
                .readString()
            gson.fromJson(string, MavenCentralResponse::class.java)?.response?.docs?.asSequence()
                ?.filter { it.id != null && it.g != null && it.a != null }
                ?.map { MavenCentralArtifactInfo(it.id!!, it.g!!, it.a!!, it.v ?: it.latestVersion) }
                ?.toList() ?: emptyList()
        } catch (e: Exception) {
            errorCount.incrementAndGet()
            MavenLog.LOG.debug(e)
            emptyList()
        }
    }
}

data class MavenCentralArtifactInfo(val id: String, val g: String, val a: String, val v: String?)

private data class MavenCentralResponse(val response: ResponseBody?)

private data class ResponseBody(val docs: List<ArtifactResponse>)

private data class ArtifactResponse(
    val id: String?, val g: String?, val a: String?,
    val v: String?, val latestVersion: String?
)