package titanium.nisshi3.github

import com.github.pgreze.process.Redirect
import com.github.pgreze.process.process
import com.github.pgreze.process.unwrap
import kotlinx.coroutines.delay
import mirrg.kotlin.gson.hydrogen.jsonElement
import mirrg.kotlin.gson.hydrogen.jsonObject
import mirrg.kotlin.gson.hydrogen.toJson
import mirrg.kotlin.gson.hydrogen.toJsonElement
import mirrg.kotlin.gson.hydrogen.toJsonWrapper
import mirrg.kotlin.hydrogen.join
import mirrg.kotlin.java.hydrogen.createParentDirectories
import mirrg.kotlin.java.hydrogen.encodePercent
import mirrg.kotlin.java.hydrogen.toInstant
import titanium.util.toSafeBashCommand
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class GitHubClient(val cache: Cache) {

    class Cache(val baseDir: File, val timeout: Duration) {
        suspend fun getOrCreate(key: String, creator: suspend () -> String): String {
            val fileName = "_" + key.encodePercent {
                when (it) {
                    ' ', '.', ',', '-', '_',
                    in 'A'..'Z',
                    in 'a'..'z',
                    in '0'..'9' -> false

                    else -> true
                }
            } + ".json"
            val file = baseDir.resolve(fileName)

            if (file.exists()) run { // ファイルが存在して、
                val json = file.readText()

                val time: Instant
                val success: Boolean
                val string: String
                try {
                    val data = json.toJsonElement().toJsonWrapper()
                    time = data["time"].asString().toInstant()
                    success = data["success"].asBoolean()
                    string = data["body"].asString()
                } catch (_: Exception) {
                    return@run // キャッシュファイルが壊れているので無視
                }

                if (time > Instant.now().minus(timeout)) { // タイムアウトしていないならば、
                    if (success) { // そのコンテンツを返す
                        return string
                    } else {
                        throw RuntimeException(string)
                    }
                }
            }

            // そうでない場合、
            val string = try {
                creator() // データを生成して
            } catch (e: Throwable) {
                val time = Instant.now()
                file.createParentDirectories()
                val jsonElement = jsonObject(
                    "time" to (DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(time.atOffset(ZoneOffset.UTC).toLocalDateTime()) + "Z").jsonElement,
                    "success" to false.jsonElement,
                    "body" to (e.message ?: "").jsonElement,
                )
                file.writeText(jsonElement.toJson { setPrettyPrinting() } + "\n") // 書き込んでから
                throw e // 返す
            }
            val time = Instant.now()
            file.createParentDirectories()
            val jsonElement = jsonObject(
                "time" to (DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(time.atOffset(ZoneOffset.UTC).toLocalDateTime()) + "Z").jsonElement,
                "success" to true.jsonElement,
                "body" to string.jsonElement,
            )
            file.writeText(jsonElement.toJson { setPrettyPrinting() } + "\n") // 書き込んでから
            return string // 返す
        }
    }

    private suspend fun getAsString(bashScript: String): String {
        return cache.getOrCreate(bashScript) {
            println("< $bashScript")
            val result = process(
                *toSafeBashCommand(bashScript).toTypedArray(),
                stdout = Redirect.CAPTURE,
                stderr = Redirect.PRINT,
            )
            delay(1000)
            result.unwrap().join("\n")
        }
    }

    private suspend fun getAsJsonWrapper(bashScript: String) = getAsString(bashScript).toJsonElement().toJsonWrapper()

    suspend fun getUserName() = getAsJsonWrapper("gh api user")["login"].asString()

    suspend fun getOrganizations() = getAsString("gh org list")

    suspend fun getRepositories(organization: String?): List<String> {
        val data = getAsJsonWrapper("gh repo list${if (organization != null) " $organization" else ""} --json name")
        return data.asList().map {
            it["name"].asString()
        }
    }

    class Issue(
        val createdAt: Instant,
        val number: Int,
        val author: String,
        val title: String,
        val body: String,
        val comments: List<Comment>,
    ) {
        override fun toString() = "#$number $title"
    }

    class Comment(
        val createdAt: Instant,
        val author: String,
        val body: String,
    )

    suspend fun getIssues(organization: String, repository: String): List<Issue> {
        val data = getAsJsonWrapper("gh issue list -R $organization/$repository -s all --json createdAt,number,author,title,body,comments")
        return data.asList().map {
            Issue(
                createdAt = it["createdAt"].asString().toInstant(),
                number = it["number"].asInt(),
                author = it["author"]["login"].asString(),
                title = it["title"].asString(),
                body = it["body"].asString(),
                comments = it["comments"].asList().map { comment ->
                    Comment(
                        createdAt = comment["createdAt"].asString().toInstant(),
                        author = comment["author"]["login"].asString(),
                        body = comment["body"].asString(),
                    )
                },
            )
        }
    }

}
