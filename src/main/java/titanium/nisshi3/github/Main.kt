package titanium.nisshi3.github

import kotlinx.coroutines.runBlocking
import mirrg.kotlin.hydrogen.join
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val zoneOffset = ZoneOffset.ofHours(9)
private val user = "Kouda-Titanium"
private val yearMonth = YearMonth.of(2024, 1)

private val client = GitHubClient(GitHubClient.Cache(File("./build/GitHubClient/cache"), Duration.ofMinutes(60)), waitMs = 500)

class Action(val time: Instant, val metadata: String, val body: String)

object ShowMonthlyMain {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {

        // Issue主もしくはコメントしたIssueの検索
        val issueEntries = client.searchIssues(user)

            // Issueのオーナーとコメントのデータを集める
            .flatMap { searchedIssue ->
                buildList {
                    val viewIssue = client.getIssueView(searchedIssue.repository, searchedIssue.number)
                    if (viewIssue.author == user) {
                        add(Action(viewIssue.createdAt, "Issue ${searchedIssue.repository}/#${searchedIssue.number}(${searchedIssue.title}): <${viewIssue.author}>", viewIssue.body))
                    }
                    viewIssue.comments.forEach { comment ->
                        if (comment.author == user) {
                            add(Action(comment.createdAt, "IsCmt ${searchedIssue.repository}/#${searchedIssue.number}(${searchedIssue.title}): <${comment.author}>", comment.body))
                        }
                    }
                }
            }

            // 範囲絞り込み
            .filter { it.time >= yearMonth.atDay(1).atStartOfDay(zoneOffset).toInstant() }
            .filter { it.time < yearMonth.atDay(1).plusMonths(1).minusDays(1).atStartOfDay(zoneOffset).toInstant() }

        // コミットを検索
        val commitEntries = client.searchCommits(
            yearMonth.atDay(1).atStartOfDay(zoneOffset).toOffsetDateTime(),
            yearMonth.atDay(1).plusMonths(1).minusDays(1).atStartOfDay(zoneOffset).toOffsetDateTime(),
            author = user,
        ).map { Action(it.time, "Comit ${it.repository.name}:", it.message) }

        // 表示
        listOf(issueEntries, commitEntries).flatten()
            .sortedBy { it.time }
            .forEach {
                println("[${it.time.render()}] ${it.metadata} ${it.body.renderAsInline(200)}")
            }
    }
}

object Main1 {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val organizations = listOf(client.getUser()) + client.getOrganizationList()
        organizations.forEach { organization ->
            val repositories = client.getRepositoryList(organization)
            repositories.forEach nextRepository@{ repository ->
                val issues = try {
                    client.getIssueList(organization, repository.name)
                } catch (e: RuntimeException) {
                    return@nextRepository
                }
                issues.forEach { issue ->

                    var printedIssueLine = false
                    fun printIssue() {
                        if (!printedIssueLine) {
                            printedIssueLine = true
                            println("Issue: $organization/${repository.name}/#${issue.number} ${issue.title}")
                        }
                    }

                    if (issue.author == "Kouda-Titanium") {
                        printIssue()
                        println("[${issue.createdAt.render()}] ${issue.author}: ${issue.body.renderAsInline()}")
                    }

                    issue.comments.forEach { comment ->

                        if (comment.author == "Kouda-Titanium") {
                            printIssue()
                            println("[${comment.createdAt.render()}] ${comment.author}: ${comment.body.renderAsInline()}")
                        }

                    }
                }
            }
        }
    }
}

private fun Instant.render(): String = this.atOffset(ZoneOffset.ofHours(9)).toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

private fun String.renderAsInline(chars: Int = 50): String {
    val combinedLine = this.lines().join("\\n")
    return when {
        combinedLine.isEmpty() -> "<no comments>"
        combinedLine.length > chars -> combinedLine.take(chars) + "..."
        else -> combinedLine
    }
}
