package ru.rzn.gmyasoedov.gmaven.util

import com.google.gson.Gson
import com.intellij.util.io.HttpRequests
import com.intellij.util.io.HttpRequests.JSON_CONTENT_TYPE
import ru.rzn.gmyasoedov.gmaven.utils.MavenLog
import java.time.Duration
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

object MavenCentralClient {
    private const val ROW_COUNT = 50
    private const val SEARCH_BY_GROUP_AND_ARTIFACT_URL =
        "https://search.maven.org/solrsearch/select?q=g:%s+AND+a:%s&core=gav&rows=${ROW_COUNT}&wt=json"
    private const val SEARCH_BY_ARTIFACT_URL =
        "https://search.maven.org/solrsearch/select?q=%s&rows=${ROW_COUNT}&wt=json"
    private const val SEARCH_ARTIFACT_BY_ARTIFACT_AND_GROUP_URL =
        "https://search.maven.org/solrsearch/select?q=%s+AND+g:%s&rows=${ROW_COUNT}&wt=json"

    private val errorCount = AtomicLong()
    private val gson = Gson()
    private val connectTimeoutMillis: Int = Duration.ofSeconds(4).toMillis().toInt()
    private val readTimeoutMillis: Int = Duration.ofSeconds(4).toMillis().toInt()
    private val isPerform = AtomicBoolean(false)

    @JvmStatic
    fun find(group: String, artifact: String): List<MavenArtifactInfo> {
        return if (group.length > 1 && artifact.length > 1)
            findByUrl(SEARCH_BY_GROUP_AND_ARTIFACT_URL.format(group, artifact))
        else emptyList()
    }

    @JvmStatic
    fun findArtifact(query: String, groupId: String?): List<MavenArtifactInfo> {
        if (query.length < 3) return emptyList()
        if (groupId != null && groupId.length > 4) {
            return findByUrl(SEARCH_ARTIFACT_BY_ARTIFACT_AND_GROUP_URL.format(query, groupId))
        }
        return findByUrl(SEARCH_BY_ARTIFACT_URL.format(query))
    }

    private fun findByUrl(url: String): List<MavenArtifactInfo> {
        if (errorCount.get() > 30) return emptyList()
        return try {
            if (isPerform.get()) return emptyList()
            isPerform.set(true)
            val string = HttpRequests.request(url)
                .readTimeout(readTimeoutMillis)
                .connectTimeout(connectTimeoutMillis)
                .accept(JSON_CONTENT_TYPE)
                .readString()
            gson.fromJson(string, MavenCentralResponse::class.java)?.response?.docs?.asSequence()
                ?.filter { it.id != null && it.g != null && it.a != null }
                ?.map { MavenArtifactInfo(it.id!!, it.g!!, it.a!!, it.v ?: it.latestVersion) }
                ?.toList() ?: emptyList()
        } catch (e: Exception) {
            errorCount.incrementAndGet()
            MavenLog.LOG.debug(e)
            emptyList()
        } finally {
            isPerform.set(false)
        }
    }
}

data class MavenArtifactInfo(val id: String, val g: String, val a: String, val v: String?)

private data class MavenCentralResponse(val response: ResponseBody?)

private data class ResponseBody(val docs: List<ArtifactResponse>)

private data class ArtifactResponse(
    val id: String?, val g: String?, val a: String?,
    val v: String?, val latestVersion: String?
)