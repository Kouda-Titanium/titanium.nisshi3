/*
 * Copyright 2022 MirrgieRiana
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:Suppress("unused", "SpellCheckingInspection")

package mirrg.kotlin.java.hydrogen

import java.nio.charset.Charset


// Percent Encoding

/**
 * 指定された文字および `%` をパーセントエンコードします。
 * 半角スペースを自動的に `+` に置換しません。
 */
inline fun String.encodePercent(charset: Charset = Charsets.UTF_8, shouldBeEncoded: (Char) -> Boolean): String {
    val sb = StringBuilder()
    this.toCharArray().forEach { char ->
        if (char == '%' || shouldBeEncoded(char)) {
            char.toString().toByteArray(charset).forEach { byte ->
                sb.append(String.format("%%%02X", byte.toInt() and 0xFF))
            }
        } else {
            sb.append(char)
        }
    }
    return sb.toString()
}

private val pattern = """(%[0-9a-fA-F][0-9a-fA-F])+""".toRegex()

/**
 * すべてのパーセントエンコードされたシーケンスをデコードします。
 * `+`は半角スペースに置換されません。
 */
fun String.decodePercent(charset: Charset = Charsets.UTF_8): String {
    return pattern.replace(this) { matchResult ->
        val string = matchResult.value
        val length = string.length / 3
        val bytes = ByteArray(length)
        repeat(length) { i ->
            bytes[i] = string.substring(3 * i + 1, 3 * i + 3).toInt(16).toByte()
        }
        bytes.toString(charset)
    }
}
