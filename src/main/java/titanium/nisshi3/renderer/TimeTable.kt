package titanium.nisshi3.renderer

import mirrg.kotlin.hydrogen.formatAs
import titanium.nisshi3.Category
import titanium.nisshi3.Diary
import titanium.nisshi3.HourMinute
import titanium.nisshi3.HourMinuteRange
import titanium.nisshi3.HourMinuteRangeList
import titanium.nisshi3.Work
import titanium.nisshi3.concat
import titanium.nisshi3.envelope
import titanium.nisshi3.excludingSeconds
import titanium.nisshi3.filterCategory
import titanium.nisshi3.filterDay
import titanium.nisshi3.filterNonRests
import titanium.nisshi3.lastDay
import titanium.nisshi3.rangeTo
import titanium.nisshi3.seconds
import titanium.nisshi3.works

private class SingleRangedRecord(val timeRange1: HourMinuteRange?, val excludingSeconds: Int)

private val List<WorkSection>.singleRangedRecord: SingleRangedRecord
    get() {
        val timeRanges = HourMinuteRangeList(this.map { it.timeRange }).concat()
        return when (timeRanges.ranges.size) {
            0 -> SingleRangedRecord(null, 0)
            else -> SingleRangedRecord(timeRanges.ranges[0].start..timeRanges.ranges.last().end, timeRanges.excludingSeconds)
        }
    }


private class DoubleRangedRecord(val timeRange1: HourMinuteRange?, val timeRange2: HourMinuteRange?, val excludingSeconds: Int)

private val List<WorkSection>.doubleRangedRecord: DoubleRangedRecord
    get() {
        val timeRanges = HourMinuteRangeList(this.map { it.timeRange }).concat()
        return when (timeRanges.ranges.size) {
            0 -> DoubleRangedRecord(null, null, 0)
            1 -> DoubleRangedRecord(timeRanges.ranges[0], null, 0)
            2 -> DoubleRangedRecord(timeRanges.ranges[0], timeRanges.ranges[1], 0)

            else -> {
                class Tuple(val head: HourMinuteRangeList, val tail: HourMinuteRangeList, val separation: Int)

                val tuple = (1 until timeRanges.ranges.size).map { i ->
                    Tuple(
                        HourMinuteRangeList(timeRanges.ranges.take(i)),
                        HourMinuteRangeList(timeRanges.ranges.drop(i)),
                        timeRanges.ranges[i].start.seconds - timeRanges.ranges[i - 1].end.seconds
                    )
                }.maxByOrNull { it.separation }!!

                DoubleRangedRecord(
                    tuple.head.ranges.first() envelope tuple.head.ranges.last(),
                    tuple.tail.ranges.first() envelope tuple.tail.ranges.last(),
                    tuple.head.excludingSeconds + tuple.tail.excludingSeconds
                )
            }
        }
    }


private class WorkSection(val work: Work, val timeRange: HourMinuteRange)

private fun Iterable<Work>.sections() = this.flatMap { work -> work.timeRanges.ranges.map { WorkSection(work, it) } }.sortedBy { it.timeRange.start.seconds }


// rendering

fun Diary.toTimeTableString(vararg categories: Category?): String {
    return (1..lastDay).joinToString("\n") { day ->
        categories.flatMapIndexed { i, category ->
            when {
                category == null -> listOf("", "", "", "", "")
                i == 0 -> this.works.filterNonRests().filterDay(day).sections().singleRangedRecord.timeTableRecord
                else -> this.works.filterNonRests().filterDay(day).filterCategory(category).sections().doubleRangedRecord.timeTableRecord
            }
        }.joinToString("\t")
    }
}

private val HourMinute.timeTableValue get() = "$hour:${minute formatAs "%02d"}"

private val SingleRangedRecord.timeTableRecord
    get() = listOf(
        if (timeRange1 != null) listOf(timeRange1.start.timeTableValue, timeRange1.end.timeTableValue) else listOf("", ""),
        if (timeRange1 != null) listOf(HourMinute(excludingSeconds / 60 / 60 % 24, excludingSeconds / 60 % 60).timeTableValue) else listOf("")
    ).flatten()

private val DoubleRangedRecord.timeTableRecord
    get() = listOf(
        if (timeRange1 != null) listOf(timeRange1.start.timeTableValue, timeRange1.end.timeTableValue) else listOf("", ""),
        if (timeRange2 != null) listOf(timeRange2.start.timeTableValue, timeRange2.end.timeTableValue) else listOf("", ""),
        if (timeRange1 != null) listOf(HourMinute(excludingSeconds / 60 / 60 % 24, excludingSeconds / 60 % 60).timeTableValue) else listOf("")
    ).flatten()
