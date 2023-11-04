import org.junit.Test
import titanium.nisshi3.Category
import titanium.nisshi3.dsl.To
import titanium.nisshi3.dsl.diary
import titanium.nisshi3.renderer.toJapanese
import titanium.nisshi3.renderer.toTimeTableString

class TestRoot {
    @Test
    fun test() {
        val c1 = Category("Category 1")
        val c2 = Category("Category 2")

        val diary = diary {
            2 {
                c1 { "テスト"(1 To 1.15, 2 To 3, 4.45 To 5, 5.10 To 6.59) }
                c1 { "コメント。"() }
            }
        }

        println("=== Japanese ===")
        println(diary.toJapanese(c1, c2))
        println("=== Time Table ===")
        println(diary.toTimeTableString(c1, c2))
    }
}
