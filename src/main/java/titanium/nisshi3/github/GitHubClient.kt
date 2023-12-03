package titanium.nisshi3.github

import kotlinx.coroutines.delay
import mirrg.kotlin.gson.hydrogen.JsonWrapper
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
import java.time.LocalDate
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
            val process = ProcessBuilder(*toSafeBashCommand(bashScript).toTypedArray<String>()).start()
            val output = process.inputStream.use { it -> it.readBytes().decodeToString() }
            val returnCode = process.waitFor()
            delay(1000)
            check(returnCode == 0) { "Invalid result: $returnCode" }
            output
        }
    }

    private suspend fun getAsJsonWrapper(bashScript: String) = getAsString(bashScript).toJsonElement().toJsonWrapper()


    suspend fun getUserName() = getAsJsonWrapper("gh api user")["login"].asString()

    suspend fun getOrganizations() = getAsString("gh org list")

    suspend fun getRepositories(organization: String?): List<String> {
        val data = getAsJsonWrapper("gh repo list${if (organization != null) " $organization" else ""} --json name --limit 1000")
        return data.asList().map {
            it["name"].asString()
        }
    }

    suspend fun getIssues(organization: String, repository: String): List<Issue> {
        val data = getAsJsonWrapper("gh issue list -R $organization/$repository -s all --json createdAt,number,author,title,body,comments,repository")
        return data.asList().map { it.toIssue() }
    }

    suspend fun searchCommit(startDate: LocalDate, endInclusiveDate: LocalDate, author: String? = null): List<Commit> {
        val command = listOfNotNull(
            "gh search commits",
            author?.let { "--author $it" },
            "--committer-date $startDate..$endInclusiveDate",
            "--json author,commit,committer,id,parents,repository,sha,url",
            "--limit 1000",
        ).join(" ")
        val data = getAsJsonWrapper(command)
        return data.asList().map { it.toCommit() }
    }

    suspend fun searchIssue(author: String): List<Issue> {
        val items = listOf(
            run {
                val command = listOf(
                    "gh search issues",
                    "--author $author",
                    "--json assignees,author,authorAssociation,body,closedAt,commentsCount,createdAt,id,isLocked,isPullRequest,labels,number,repository,state,title,updatedAt,url",
                    "--limit 1000",
                ).join(" ")
                val data = getAsJsonWrapper(command)
                data.asList()
            },
            run {
                val command = listOf(
                    "gh search issues",
                    "--commenter $author",
                    "--json assignees,author,authorAssociation,body,closedAt,commentsCount,createdAt,id,isLocked,isPullRequest,labels,number,repository,state,title,updatedAt,url",
                    "--limit 1000",
                ).join(" ")
                val data = getAsJsonWrapper(command)
                data.asList()
            },
        ).flatten()
            .map { it.toIssue() }
            .distinctBy { "${it.repository!!}/${it.number}" }
        return items
    }

    suspend fun getIssue(repository: String, number: Int): Issue {
        val command = listOf(
            "gh issue view",
            "--repo $repository",
            "$number",
            "--json assignees,author,body,closed,closedAt,comments,createdAt,id,labels,milestone,number,projectCards,projectItems,reactionGroups,state,title,updatedAt,url",
        ).join(" ")
        val data = getAsJsonWrapper(command)
        return data.toIssue()
    }


    class Issue(
        val repository: String?,
        val createdAt: Instant,
        val number: Int,
        val author: String,
        val title: String,
        val body: String,
        val comments: List<Comment>?,
    ) {
        override fun toString() = "#$number $title"
    }

    private fun JsonWrapper.toIssue() = Issue(
        repository = this["repository"].orNull?.let { it["nameWithOwner"].asString() },
        createdAt = this["createdAt"].asString().toInstant(),
        number = this["number"].asInt(),
        author = this["author"]["login"].asString(),
        title = this["title"].asString(),
        body = this["body"].asString(),
        comments = this["comments"].orNull?.asList()?.map { it.toComment() },
    )

    class Comment(
        val createdAt: Instant,
        val author: String,
        val body: String,
    )

    private fun JsonWrapper.toComment() = Comment(
        createdAt = this["createdAt"].asString().toInstant(),
        author = this["author"]["login"].asString(),
        body = this["body"].asString(),
    )

    class Commit(
        val repository: String,
        val time: Instant,
        val author: String,
        val summary: String,
    )

    private fun JsonWrapper.toCommit() = Commit(
        repository = this["repository"]["fullName"].asString(),
        time = this["commit"]["committer"]["date"].asString().toInstant(),
        author = this["author"]["login"].asString(),
        summary = this["commit"]["message"].asString(),
    )

}
