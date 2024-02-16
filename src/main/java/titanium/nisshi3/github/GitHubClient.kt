package titanium.nisshi3.github

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
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class GitHubClient(val cache: Cache, val waitMs: Int = 1000) {

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
            delay(waitMs.toLong())
            check(returnCode == 0) { "Invalid result: $returnCode" }
            output
        }
    }

    private suspend fun getAsJsonWrapper(bashScript: String) = getAsString(bashScript).toJsonElement().toJsonWrapper()


    suspend fun getUser() = getAsJsonWrapper("gh api user")["login"].asString()

    suspend fun getOrganizationList() = getAsString("gh org list")

    suspend fun getRepositoryList(organization: String?, limit: Int = 1000): List<ViewRepository> {
        val command = listOfNotNull(
            "gh repo list",
            organization,
            "--json name",
            "--limit $limit",
        ).join(" ")
        val data = getAsJsonWrapper(command)
        // assignableUsers,codeOfConduct,contactLinks,createdAt,defaultBranchRef,deleteBranchOnMerge,description,diskUsage,forkCount
        // fundingLinks,hasDiscussionsEnabled,hasIssuesEnabled,hasProjectsEnabled,hasWikiEnabled,homepageUrl,id,isArchived
        // isBlankIssuesEnabled,isEmpty,isFork,isInOrganization,isMirror,isPrivate,isSecurityPolicyEnabled,isTemplate
        // isUserConfigurationRepository,issueTemplates,issues,labels,languages,latestRelease,licenseInfo,mentionableUsers
        // mergeCommitAllowed,milestones,mirrorUrl,name,nameWithOwner,openGraphImageUrl,owner,parent,primaryLanguage,projects
        // pullRequestTemplates,pullRequests,pushedAt,rebaseMergeAllowed,repositoryTopics,securityPolicyUrl,squashMergeAllowed
        // sshUrl,stargazerCount,templateRepository,updatedAt,url,usesCustomOpenGraphImage,viewerCanAdminister
        // viewerDefaultCommitEmail,viewerDefaultMergeMethod,viewerHasStarred,viewerPermission,viewerPossibleCommitEmails
        // viewerSubscription,visibility,watchers
        return data.asList().map { ViewRepository(it) }
    }

    suspend fun getIssueList(organization: String, repository: String): List<ViewIssue> {
        val command = listOfNotNull(
            "gh issue list",
            "-R $organization/$repository",
            "-s all",
            "--json author,body,comments,createdAt,number,title",
        ).join(" ")
        val data = getAsJsonWrapper(command)
        // assignees,author,body,closed,closedAt,comments,createdAt,id,labels,milestone,number
        // projectCards,projectItems,reactionGroups,state,title,updatedAt,url
        return data.asList().map { ViewIssue(it) }
    }

    suspend fun searchCommits(startTime: OffsetDateTime, endInclusiveTime: OffsetDateTime, author: String? = null, limit: Int = 1000): List<SearchedCommit> {
        val command = listOfNotNull(
            "gh search commits",
            author?.let { "--author $it" },
            "--committer-date ${startTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}..${endInclusiveTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)}",
            "--json author,commit,repository",
            "--limit $limit",
        ).join(" ")
        val data = getAsJsonWrapper(command)
        // author,commit,committer,id,parents,repository,sha,url
        return data.asList().map { SearchedCommit(it) }
    }

    /** GitHub全体から特定のユーザーが関与しているIssueを検索します。 */
    suspend fun searchIssues(user: String): List<SearchedIssue> {
        val items = listOf(
            run {
                val command = listOf(
                    "gh search issues",
                    "--author $user",
                    "--json author,body,createdAt,number,repository,title",
                    "--limit 1000",
                ).join(" ")
                val data = getAsJsonWrapper(command)
                // assignees,author,authorAssociation,body,closedAt,commentsCount,createdAt,id,isLocked
                // isPullRequest,labels,number,repository,state,title,updatedAt,url
                data.asList()
            },
            run {
                val command = listOf(
                    "gh search issues",
                    "--commenter $user",
                    "--json author,body,createdAt,number,repository,title",
                    "--limit 1000",
                ).join(" ")
                val data = getAsJsonWrapper(command)
                // assignees,author,authorAssociation,body,closedAt,commentsCount,createdAt,id,isLocked
                // isPullRequest,labels,number,repository,state,title,updatedAt,url
                data.asList()
            },
        ).flatten()
            .map { SearchedIssue(it) }
            .distinctBy { "${it.repository}/${it.number}" }
        return items
    }

    suspend fun getIssueView(repository: String, number: Int): ViewIssue {
        val command = listOf(
            "gh issue view",
            "--repo $repository",
            "$number",
            "--json author,body,comments,createdAt,number,title",
        ).join(" ")
        val data = getAsJsonWrapper(command)
        // assignees,author,body,closed,closedAt,comments,createdAt,id,labels,milestone,number
        // projectCards,projectItems,reactionGroups,state,title,updatedAt,url
        return ViewIssue(data)
    }

}
