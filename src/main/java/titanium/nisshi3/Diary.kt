package titanium.nisshi3


// definition

class Diary(val items: List<DiaryItem>)

class Category(val name: String) {
}

sealed class DiaryItem(val day: Int, val category: Category) {
    init {
        require(day >= 1)
    }
}

class Work(day: Int, category: Category, val description: String, val timeRanges: HourMinuteRangeList, val isRest: Boolean) : DiaryItem(day, category)
class Comment(day: Int, category: Category, val label: String) : DiaryItem(day, category)


// Utility

val Diary.lastDay get() = items.maxOfOrNull { it.day } ?: 1

val Diary.works get(): List<Work> = items.filterIsInstance<Work>()
val Diary.comments get(): List<Comment> = items.filterIsInstance<Comment>()
fun <T : DiaryItem> Iterable<T>.filterDay(day: Int) = this.filter { it.day == day }
fun <T : DiaryItem> Iterable<T>.filterCategory(category: Category) = this.filter { it.category == category }
fun Iterable<Work>.filterNonRests() = this.filter { !it.isRest }
