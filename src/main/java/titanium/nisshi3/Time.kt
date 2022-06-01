package titanium.nisshi3

// definition

data class HourMinute(val hour: Int, val minute: Int)

operator fun HourMinute.compareTo(other: HourMinute): Int {
    this.hour.compareTo(other.hour).let { if (it != 0) return it }
    this.minute.compareTo(other.minute).let { if (it != 0) return it }
    return 0
}

data class HourMinuteRange(val start: HourMinute, val end: HourMinute) {
    init {
        require(start < end) { "異常な時間範囲: $start < $end" }
    }
}

operator fun HourMinute.rangeTo(to: HourMinute) = HourMinuteRange(this, to)

data class HourMinuteRangeList(val ranges: List<HourMinuteRange>) {
    init {
        if (ranges.size >= 2) { // 2個以上の要素がある場合（例：size = 5）
            repeat(ranges.size - 1) { i -> // 例：i = 0..3
                require(ranges[i].end <= ranges[i + 1].start) // 前の範囲の末尾は後の範囲の先頭より後ではならない
            }
        }
    }
}


// seconds

val HourMinute.seconds get() = 60 * (60 * hour + minute)
val HourMinuteRange.seconds get() = end.seconds - start.seconds


// concat

/** 2個の隣接した時間を結合します。 */
operator fun HourMinuteRange.plus(other: HourMinuteRange): HourMinuteRange {
    require(this.end == other.start) { "不連続な結合: $this + $other" }
    return this.start..other.end
}

/** すべての結合可能な隣接時間を結合したリストを返します。 */
fun HourMinuteRangeList.concat(): HourMinuteRangeList {
    fun concat(ranges2: List<HourMinuteRange>): List<HourMinuteRange> = when {
        ranges2.size <= 1 -> ranges2 // 長さが1以下ならそのまま通す
        // 先頭の2項目が結合可能な場合、先頭の2個を結合して全体を再び評価
        ranges2[0].end == ranges2[1].start -> concat(listOf(ranges2[0] + ranges2[1]) + ranges2.drop(2))
        // 先頭の2項目が結合不能な場合、先頭を除いて残りを再び評価
        else -> ranges2.take(1) + concat(ranges2.drop(1))
    }
    return HourMinuteRangeList(concat(ranges))
}


// others

/** 時間列全体の開始時刻から終了時刻までで、どの時間にも含まれない秒数を計算します。 */
val HourMinuteRangeList.excludingSeconds get() = if (ranges.size <= 1) 0 else (ranges.last().end.seconds - ranges.first().start.seconds) - ranges.sumOf { it.seconds }
