import titanium.nisshi3.Category
import titanium.nisshi3.Comment
import titanium.nisshi3.Diary
import titanium.nisshi3.DiaryItem
import titanium.nisshi3.HourMinute
import titanium.nisshi3.HourMinuteRange
import titanium.nisshi3.HourMinuteRangeList
import titanium.nisshi3.Work
import titanium.nisshi3.rangeTo as rangeTo2


operator fun HourMinute.rangeTo(to: HourMinute) = this.rangeTo2(to) // TODO conflict

fun t(hour: Int, minute: Int = 0) = HourMinute(hour, minute) // TODO もっといいインターフェースに

fun Category() = Category() // TODO conflict


interface DiaryBuilder {
    operator fun Int.invoke(block: DayScope.() -> Unit)
}

interface DayScope {
    operator fun Category.invoke(block: DayCategoryScope.() -> Unit)
}

interface DayCategoryScope {
    /** @receiver 句点を含まない日本語の文 */
    operator fun String.invoke(head: HourMinuteRange, vararg tail: HourMinuteRange, isRest: Boolean = false)

    /** @receiver 任意の日本語の言葉 */
    operator fun String.invoke()
}

fun nisshi(block: DiaryBuilder.() -> Unit): Diary { // TODO rename
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
