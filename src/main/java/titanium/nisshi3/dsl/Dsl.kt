@file:Suppress("FunctionName")

package titanium.nisshi3.dsl

import titanium.nisshi3.Category
import titanium.nisshi3.Comment
import titanium.nisshi3.Diary
import titanium.nisshi3.DiaryItem
import titanium.nisshi3.HourMinute
import titanium.nisshi3.HourMinuteRange
import titanium.nisshi3.HourMinuteRangeList
import titanium.nisshi3.Work
import titanium.nisshi3.rangeTo
import kotlin.math.roundToInt


private fun Int.toHourMinute() = HourMinute(this, 0)
private fun Double.toHourMinute() = (this * 100.0).roundToInt().let { minutes -> HourMinute(minutes / 100, minutes % 100) }

infix fun Int.To(to: Int) = this.toHourMinute()..to.toHourMinute()
infix fun Int.To(to: Double) = this.toHourMinute()..to.toHourMinute()
infix fun Double.To(to: Int) = this.toHourMinute()..to.toHourMinute()
infix fun Double.To(to: Double) = this.toHourMinute()..to.toHourMinute()


interface DiaryBuilder {
    operator fun Int.invoke(block: DayScope.() -> Unit)
}

interface DayScope {
    /** @receiver 句点を含まない日本語の文 */
    operator fun String.invoke(head: HourMinuteRange, vararg tail: HourMinuteRange): Work

    /** @receiver 任意のラベル */
    operator fun String.invoke(): Comment
}

fun diary(defaultCategory: Category, block: DiaryBuilder.() -> Unit): Diary {
    val diaryBuilder = object : DiaryBuilder {
        private val diaryItems = mutableListOf<DiaryItem>()

        override operator fun Int.invoke(block: DayScope.() -> Unit) {
            val day = this
            val dayScope = object : DayScope {
                override operator fun String.invoke(head: HourMinuteRange, vararg tail: HourMinuteRange): Work {
                    val work = Work(day, defaultCategory, this, HourMinuteRangeList(listOf<HourMinuteRange>() + head + tail))
                    diaryItems += work
                    return work
                }

                override operator fun String.invoke(): Comment {
                    val comment = Comment(day, defaultCategory, this)
                    diaryItems += comment
                    return comment
                }
            }
            dayScope.block()
        }

        fun create() = Diary(diaryItems)
    }
    diaryBuilder.block()
    return diaryBuilder.create()
}
