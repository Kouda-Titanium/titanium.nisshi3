@file:Suppress("FunctionName")

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
private fun Double.toHourMinute() = (this * 60.0).roundToInt().let { minutes -> HourMinute(minutes / 60, minutes % 60) }

infix fun Int.To(to: Int) = this.toHourMinute()..to.toHourMinute()
infix fun Int.To(to: Double) = this.toHourMinute()..to.toHourMinute()
infix fun Double.To(to: Int) = this.toHourMinute()..to.toHourMinute()
infix fun Double.To(to: Double) = this.toHourMinute()..to.toHourMinute()


interface DiaryBuilder {
    operator fun Int.invoke(block: DayScope.() -> Unit)
}

interface DayScope {
    operator fun Category.invoke(block: DayCategoryScope.() -> Unit)
}

interface DayCategoryScope {
    /** @receiver 句点を含まない日本語の文 */
    operator fun String.invoke(head: HourMinuteRange, vararg tail: HourMinuteRange, isRest: Boolean = false)

    /** @receiver 任意のラベル */
    operator fun String.invoke()
}

fun diary(block: DiaryBuilder.() -> Unit): Diary {
    val diaryBuilder = object : DiaryBuilder {
        private val diaryItems = mutableListOf<DiaryItem>()

        override operator fun Int.invoke(block: DayScope.() -> Unit) {
            val day = this
            val dayScope = object : DayScope {
                override operator fun Category.invoke(block: DayCategoryScope.() -> Unit) {
                    val category = this
                    val dayCategoryScope = object : DayCategoryScope {
                        override operator fun String.invoke(head: HourMinuteRange, vararg tail: HourMinuteRange, isRest: Boolean) {
                            diaryItems += Work(day, category, this, HourMinuteRangeList(listOf<HourMinuteRange>() + head + tail), isRest)
                        }

                        override operator fun String.invoke() {
                            diaryItems += Comment(day, category, this)
                        }
                    }
                    dayCategoryScope.block()
                }
            }
            dayScope.block()
        }

        fun create() = Diary(diaryItems)
    }
    diaryBuilder.block()
    return diaryBuilder.create()
}
