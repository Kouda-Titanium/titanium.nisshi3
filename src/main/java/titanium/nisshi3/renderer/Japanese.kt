package titanium.nisshi3.renderer

import titanium.nisshi3.Category
import titanium.nisshi3.Comment
import titanium.nisshi3.Diary
import titanium.nisshi3.Work
import titanium.nisshi3.comments
import titanium.nisshi3.filterCategory
import titanium.nisshi3.filterDay
import titanium.nisshi3.lastDay
import titanium.nisshi3.HourMinute
import titanium.nisshi3.HourMinuteRange
import titanium.nisshi3.works

fun Diary.toJapanese(categories: List<Category?>) = (1..lastDay).joinToString("\n") { day ->
    categories.joinToString("\t".repeat(15)) cell@{ category ->
        if (category == null) return@cell ""
        val stringsComment = comments.filterDay(day).filterCategory(category).map { it.japanese }
        val stringWorks = works.filterDay(day).filterCategory(category).joinToString("") { it.japanese }
        "\"${(if (stringWorks.isEmpty()) stringsComment else stringsComment + stringWorks).joinToString("\n")}\""
    }
}

private val Work.japanese get() = "${description}（${timeRanges.ranges.joinToString("、") { it.japanese }}）。"
private val Comment.japanese get() = label
private val HourMinuteRange.japanese get() = "${start.japanese}～${end.japanese}"
private val HourMinute.japanese get() = "${hour}時${minute}分"
