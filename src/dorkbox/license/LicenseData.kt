/*
 * Copyright 2012 dorkbox, llc
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
package dorkbox.license

import java.util.*

class LicenseData(val name: String, val license: License) : Comparable<LicenseData> {
    /**
     * Copyright
     */
    val copyrights = ArrayList<Int>()

    fun copyright(copyright: Int) {
        copyrights.add(copyright)
    }

    /**
     * URL
     */
    val urls = ArrayList<String>()

    fun url(url: String) {
        urls.add(url)
    }

    /**
     * Notes
     */
    val notes = ArrayList<String>()

    fun note(note: String) {
        notes.add(note)
    }

    /**
     * AUTHOR
     */
    val authors = ArrayList<String>()

    fun author(author: String) {
        authors.add(author)
    }

    /**
     * ignore case when sorting these
     */
    override operator fun compareTo(other: LicenseData): Int {
        return this.name.toLowerCase().compareTo(other.name.toLowerCase())
    }

    override fun toString(): String {
        return this.name
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as LicenseData

        if (name != other.name) return false
        if (license != other.license) return false
        if (copyrights != other.copyrights) return false
        if (urls != other.urls) return false
        if (notes != other.notes) return false
        if (authors != other.authors) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + license.hashCode()
        result = 31 * result + copyrights.hashCode()
        result = 31 * result + urls.hashCode()
        result = 31 * result + notes.hashCode()
        result = 31 * result + authors.hashCode()
        return result
    }

    companion object {
        private val LINE_SEPARATOR = System.getProperty("line.separator")
        private val newLineRegex = "\n".toRegex()

        /**
         * Returns the LICENSE text file, as a combo of the listed licenses. Duplicates are removed.
         */
        fun buildString(licenses: MutableList<LicenseData>): String {
            val b = StringBuilder(256)

            sortAndClean(licenses)

            val NL = LINE_SEPARATOR
            val HEADER = " - "
            val SPACER = "   "
            val SPACR1 = "     "

            var first = true

            licenses.forEach { license ->
                // append a new line AFTER the first entry
                if (first) {
                    first = false
                }
                else {
                    b.append(NL).append(NL)
                }

                b.append(HEADER).append(license.name).append(" - ").append(NL)

                license.urls.forEach {
                    b.append(SPACER).append(it).append(NL)
                }


                b.append(SPACER).append("Copyright")
                license.copyrights.forEach {
                    b.append(" ").append(it).append(",")
                }
                b.deleteCharAt(b.length-1)
                b.append(" - ").append(license.license.preferedName).append(NL)

                license.authors.forEach {
                    b.append(SPACR1).append(it).append(NL)
                }

                if (license.license === License.CUSTOM) {
                    license.notes.forEach {
                        b.append(fixSpace(it, SPACER, 1)).append(NL)
                    }
                }
                else {
                    license.notes.forEach {
                        b.append(SPACER).append(it).append(NL)
                    }
                }
            }

            return b.toString()
        }

        /**
         * fixes new lines that may appear in the text
         * @param text text to format
         * @param spacer how big will the space in front of each line be?
         */
        private fun fixSpace(text: String, spacerSize: String, spacer: Int): String {
            val trimmedText = text.trim { it <= ' ' }

            var space = ""
            for (i in 0 until spacer) {
                space += spacerSize
            }

            return space + trimmedText.replace(newLineRegex, "\n" + space)
        }

        /**
         * Sorts and remove dupes for the list of licenses.
         */
        private fun sortAndClean(licenses: MutableList<LicenseData>) {
            // The FIRST one is always FIRST! (the rest are alphabetical)
            val firstLicense = licenses[0]

            // remove dupes
            val dedupe = HashSet(licenses)

            val copy = ArrayList(dedupe)
            copy.sort()

            // figure out where the first one went
            for (i in copy.indices) {
                if (firstLicense === copy[i]) {
                    copy.removeAt(i)
                    break
                }
            }

            licenses.clear()
            licenses.add(firstLicense)
            licenses.addAll(copy)
        }
    }
}
