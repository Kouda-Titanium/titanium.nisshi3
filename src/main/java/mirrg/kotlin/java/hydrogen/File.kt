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

import java.io.File
import java.io.FileNotFoundException
import java.io.IOException

fun File.mkdirsOrThrow() {
    if (isDirectory) return
    if (exists()) throw IOException("Could not create the directory because it is occupied: $absolutePath")
    if (!mkdirs()) throw IOException("Could not create the directory: $absolutePath")
}

fun File.deleteOrThrow() {
    if (!exists()) throw FileNotFoundException("File $absolutePath not found")
    if (!delete()) throw IOException("Could not delete the file: $absolutePath")
}
