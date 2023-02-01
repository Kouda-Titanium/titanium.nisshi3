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

package mirrg.kotlin.hydrogen

import kotlin.math.ceil
import kotlin.math.floor


// 上限・下限
infix fun Byte.atMost(other: Byte) = coerceAtMost(other)
infix fun Byte.atLeast(other: Byte) = coerceAtLeast(other)
infix fun Short.atMost(other: Short) = coerceAtMost(other)
infix fun Short.atLeast(other: Short) = coerceAtLeast(other)
infix fun Int.atMost(other: Int) = coerceAtMost(other)
infix fun Int.atLeast(other: Int) = coerceAtLeast(other)
infix fun Long.atMost(other: Long) = coerceAtMost(other)
infix fun Long.atLeast(other: Long) = coerceAtLeast(other)
infix fun Float.atMost(other: Float) = coerceAtMost(other)
infix fun Float.atLeast(other: Float) = coerceAtLeast(other)
infix fun Double.atMost(other: Double) = coerceAtMost(other)
infix fun Double.atLeast(other: Double) = coerceAtLeast(other)
infix fun <T : Comparable<T>> T.atMost(other: T) = coerceAtMost(other)
infix fun <T : Comparable<T>> T.atLeast(other: T) = coerceAtLeast(other)


// 中置比較
infix fun <T : Comparable<T>> T.min(other: T) = if (this <= other) this else other
infix fun <T : Comparable<T>> T.max(other: T) = if (this >= other) this else other
infix fun <T : Comparable<T>> T.cmp(other: T) = compareTo(other)


// 丸め
fun Float.floorToInt() = floor(this).toInt()
fun Float.floorToLong() = floor(this).toLong()
fun Double.floorToInt() = floor(this).toInt()
fun Double.floorToLong() = floor(this).toLong()
fun Float.ceilToInt() = ceil(this).toInt()
fun Float.ceilToLong() = ceil(this).toLong()
fun Double.ceilToInt() = ceil(this).toInt()
fun Double.ceilToLong() = ceil(this).toLong()
