package titanium.nisshi3.github

import mirrg.kotlin.hydrogen.join
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private suspend fun main() {
    val client = GitHubClient(GitHubClient.Cache(File("./build/GitHubClient/cache"), Duration.ofMinutes(60)))

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

private fun Instant.render(): String = this.atOffset(ZoneOffset.ofHours(9)).toLocalDateTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

private fun String.renderAsInline(): String {
    val combinedLine = this.lines().join("\\n")
    return when {
        combinedLine.isEmpty() -> "<no comments>"
        combinedLine.length > 50 -> combinedLine.take(50) + "..."
        else -> combinedLine
    }
}
