package titanium.util

import mirrg.kotlin.hydrogen.formatAs
import mirrg.kotlin.hydrogen.join

fun toSafeBashCommand(bashScript: String): List<String> {

    // WSL版bash.exeは引数に渡さた1個の文字列の先頭に "bash " を付けて実行する
    // これにより空白分割が行われるが、余計なことに変数展開も同時に行ってしまう
    // 両者は異なる結果を引き起こす
    // bash     -c "echo '\$TERM'"
    // bash.exe -c "echo '\$TERM'"

    // JavaのProcessBuilderは、一般的な引数分解を行う想定で " によりエスケープする
    // しかし、WSL版bashは一般的な引数分解を行わないため、異常な挙動を起こしてしまう
    // 具体的にはProcessBuilderが付けた " をbashにおける " とみなし、内部の記号を展開する
    // これにより、 " ` \ $ は暗黙の中間bashによって処理される

    // コマンド引数分解用の中間bashによる展開を回避するため、
    // コマンドを16進表記して直接埋め込み、
    // bashにパイプすることで任意の文字列を実行する方式を採った

    val encoded = bashScript.toByteArray().map { it formatAs "%X" }.join("")
    return listOf("bash", "-c", "echo $encoded | xxd -p -r | bash")
}

fun String.escapeBash() = this
    .replace("'", "'\\''")
    .let { "'$it'" }
