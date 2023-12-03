package titanium.nisshi3.github

import mirrg.kotlin.hydrogen.join
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val client = GitHubClient(GitHubClient.Cache(File("./build/GitHubClient/cache"), Duration.ofMinutes(60)))

private suspend fun main() {
    val issues = client.searchIssue("Kouda-Titanium")
    issues
        .flatMap { issue ->
            val issue2 = client.getIssue(issue.repository!!, issue.number)
            issue2.comments!!
                .filter { it.author == "Kouda-Titanium" }
                .map { comment ->
                    Pair(issue, comment)
                }
        }
        .sortedBy { it.second.createdAt }
        .forEach { (issue, comment) ->
            println("[${comment.createdAt.render()}] ${issue.repository}/#${issue.number}(${issue.title}): <${comment.author}> ${comment.body.renderAsInline(chars = 200)}")
        }
}

private suspend fun main2() {
    val commits = client.searchCommit(LocalDate.of(2023, 11, 1), LocalDate.of(2023, 12, 1).minusDays(1), author = "Kouda-Titanium")
    commits
        .sortedBy { it.time }
        .forEach { commit ->
            println("[${commit.time.render()}] ${commit.repository}: (${commit.author}) ${commit.summary}")
        }
}

private suspend fun main3() {
    val organizations = listOf(client.getUserName()) + client.getOrganizations()
    organizations.forEach { organization ->
        val repositories = client.getRepositories(organization)
        repositories.forEach nextRepository@{ repository ->
            val issues = try {
                client.getIssues(organization, repository)
            } catch (e: RuntimeException) {
                return@nextRepository
            }
            issues.forEach { issue ->

                var printedIssueLine = false
                fun printIssue() {
                    if (!printedIssueLine) {
                        printedIssueLine = true
                        println("Issue: $organization/$repository/#${issue.number} ${issue.title}")
                    }
                }

                if (issue.author == "Kouda-Titanium") {
                    printIssue()
                    println("[${issue.createdAt.render()}] ${issue.author}: ${issue.body.renderAsInline()}")
                }

                issue.comments?.forEach { comment ->

                    if (comment.author == "Kouda-Titanium") {
                        printIssue()
                        println("[${comment.createdAt.render()}] ${comment.author}: ${comment.body.renderAsInline()}")
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
