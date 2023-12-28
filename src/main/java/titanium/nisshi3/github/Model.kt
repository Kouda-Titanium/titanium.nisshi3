package titanium.nisshi3.github

import mirrg.kotlin.gson.hydrogen.JsonWrapper
import mirrg.kotlin.java.hydrogen.toInstant
import java.time.Instant

private inline fun JsonWrapper.ifNull(block: () -> Nothing) = this.orNull ?: block()


open class Repository(val data: JsonWrapper) {
    open val name: String? get() = data["name"].ifNull { return null }.asString()
    open val fullName: String? get() = data["fullName"].ifNull { return null }.asString()
}

class ViewRepository(data: JsonWrapper) : Repository(data) {
    override val name = super.name!!
    override val fullName = super.fullName!!
}

class SimpleRepository(data: JsonWrapper) : Repository(data) {
    override val name = super.name!!
    override val fullName = super.fullName!!
}


open class Issue(val data: JsonWrapper) {
    open val createdAt: Instant? get() = data["createdAt"].asString().toInstant()
    open val repository: String? get() = data["repository"]["nameWithOwner"].asString()
    open val author: String? get() = data["author"]["login"].asString()
    open val number: Int? get() = data["number"].asInt()
    open val title: String? get() = data["title"].asString()
    open val body: String? get() = data["body"].asString()
    open val comments: List<SimpleComment>? get() = data["comments"].asList().map { SimpleComment(it) }
    override fun toString() = "#$number $title"
}

class ViewIssue(data: JsonWrapper) : Issue(data) {
    override val createdAt = super.createdAt!!
    override val author = super.author!!
    override val number = super.number!!
    override val title = super.title!!
    override val body = super.body!!
    override val comments = super.comments!!
}

class SearchedIssue(data: JsonWrapper) : Issue(data) {
    override val createdAt = super.createdAt!!
    override val repository = super.repository!!
    override val author = super.author!!
    override val number = super.number!!
    override val title = super.title!!
    override val body = super.body!!
}


open class Comment(val data: JsonWrapper) {
    open val createdAt: Instant? get() = data["createdAt"].ifNull { return null }.asString().toInstant()
    open val author: String? get() = data["author"].ifNull { return null }["login"].asString()
    open val body: String? get() = data["body"].ifNull { return null }.asString()
}

class SimpleComment(data: JsonWrapper) : Comment(data) {
    override val createdAt = super.createdAt!!
    override val author = super.author!!
    override val body = super.body!!
}


open class Commit(val data: JsonWrapper) {
    open val repository: SimpleRepository? get() = SimpleRepository(data["repository"].ifNull { return null })
    open val time: Instant? get() = data["commit"].ifNull { return null }["committer"]["date"].asString().toInstant()
    open val author: String? get() = data["author"].ifNull { return null }["login"].asString()
    open val message: String? get() = data["commit"].ifNull { return null }["message"].asString()
}

class SearchedCommit(data: JsonWrapper) : Commit(data) {
    override val repository = super.repository!!
    override val time = super.time!!
    override val author = super.author!!
    override val message = super.message!!
}
