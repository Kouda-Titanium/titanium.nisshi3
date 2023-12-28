package titanium.nisshi3.github

import kotlinx.coroutines.runBlocking
import mirrg.kotlin.hydrogen.join
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val client = GitHubClient(GitHubClient.Cache(File("./build/GitHubClient/cache"), Duration.ofMinutes(60)), waitMs = 500)

object ShowCommentsMain {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val user = "Kouda-Titanium"

        val issues = client.searchIssues(user)
        issues
            .flatMap { issue ->
                val issue2 = client.getIssueView(issue.repository, issue.number)
                issue2.comments
                    .filter { it.author == user }
                    .map { comment ->
                        Pair(issue, comment)
                    }
            }
            .sortedBy { it.second.createdAt }
            .forEach { (issue, comment) ->
                println("[${comment.createdAt.render()}] ${issue.repository}/#${issue.number}(${issue.title}): <${comment.author}> ${comment.body.renderAsInline(200)}")
            }
    }
}

object ShowMonthlyCommitsMain {
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        val author = "Kouda-Titanium"
        val period = YearMonth.of(2023, 12)

        val commits = client.searchCommits(
            period.atDay(1),
            period.atDay(1).plusMonths(1).minusDays(1),
            author = author,
        )
        commits
            .sortedBy { it.time }
            .forEach { commit ->
                println("[${commit.time.render()}] ${commit.repository.name}: ${commit.message.renderAsInline(200)}")
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
