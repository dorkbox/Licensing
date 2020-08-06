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

import License
import org.gradle.api.Action
import org.gradle.api.GradleException
import java.io.IOException
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.time.LocalDate


class LicenseData(var name: String, var license: License) : java.io.Serializable, Comparable<LicenseData> {
    /**
     * Description/title
     */
    var description = ""

    /**
     * If not specified, will be blank after the name
     */
    fun description(description: String) {
        this.description = description
    }

    /**
     * Copyright
     */
    val copyrights = mutableListOf<Int>()

    /**
     * If not specified, will use the current year
     */
    fun copyright(copyright: Int = LocalDate.now().year): CopyrightRange {
        copyrights.add(copyright)
        return CopyrightRange(this, copyright, copyrights)
    }

    /**
     * URL
     */
    val urls = mutableListOf<String>()

    /**
     * Specifies the URLs this project is located at
     */
    fun url(url: String) {
        urls.add(url)
    }

    /**
     * Notes
     */
    val notes = mutableListOf<String>()

    /**
     * Specifies any extra notes (or copyright info) as needed
     */
    fun note(note: String) {
        notes.add(note)
    }

    /**
     * AUTHOR
     */
    val authors = mutableListOf<String>()

    /**
     * Specifies the authors of this project
     */
    fun author(author: String) {
        authors.add(author)
    }

    /**
     * Extra License information
     */
    val extras = mutableListOf<LicenseData>()

    /**
     * Specifies the extra license information for this project
     */
    fun extra(name: String, license: License, licenseAction: Action<LicenseData>) {
        val licenseData = LicenseData(name, license)
        licenseAction.execute(licenseData)
        extras.add(licenseData)
    }

    /**
     * Specifies the extra license information for this project
     */
    fun extra(name: String, license: License, licenseAction: (LicenseData) -> Unit) {
        val licenseData = LicenseData(name, license)
        licenseAction(licenseData)
        extras.add(licenseData)
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
        if (description != other.description) return false
        if (copyrights != other.copyrights) return false
        if (urls != other.urls) return false
        if (notes != other.notes) return false
        if (authors != other.authors) return false
        if (extras != other.extras) return false

        return true
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + license.hashCode()
        result = 31 * result + description.hashCode()
        result = 31 * result + copyrights.hashCode()
        result = 31 * result + urls.hashCode()
        result = 31 * result + notes.hashCode()
        result = 31 * result + authors.hashCode()
        result = 31 * result + extras.hashCode()
        return result
    }

    @Throws(IOException::class)
    fun writeObject(s: ObjectOutputStream) {
        s.writeUTF(name)
        s.writeUTF(license.name)
        s.writeUTF(description)

        s.writeInt(copyrights.size)
        copyrights.forEach {
            s.writeInt(it)
        }

        s.writeInt(urls.size)
        urls.forEach {
            s.writeUTF(it)
        }

        s.writeInt(notes.size)
        notes.forEach {
            s.writeUTF(it)
        }

        s.writeInt(authors.size)
        authors.forEach {
            s.writeUTF(it)
        }

        s.writeInt(extras.size)
        if (extras.size > 0) {
            extras.forEach {
                it.writeObject(s)
            }
        }
    }

    // Gradle only needs to serialize objects, so this isn't strictly needed
    @Throws(IOException::class)
    fun readObject(s: ObjectInputStream) {
        name = s.readUTF()
        license = License.valueOfLicenseName(s.readUTF())
        description = s.readUTF()


        val copyrightsSize = s.readInt()
        for (i in 1..copyrightsSize) {
            copyrights.add(s.readInt())
        }

        val urlsSize = s.readInt()
        for (i in 1..urlsSize) {
            urls.add(s.readUTF())
        }

        val notesSize = s.readInt()
        for (i in 1..notesSize) {
            notes.add(s.readUTF())
        }

        val authorsSize = s.readInt()
        for (i in 1..authorsSize) {
            authors.add(s.readUTF())
        }

        val extrasSize = s.readInt()
        for (i in 1..extrasSize) {
            val dep = LicenseData("", License.CUSTOM)
            dep.readObject(s) // can recursively create objects
            extras.add(dep)
        }
    }

    companion object {
        private const val serialVersionUID = 1L

        // NOTE: we ALWAYS use unix line endings!
        private const val NL = "\n"
        private const val HEADER = " - "
        private const val HEADR4 = " ---- "
        private const val SPACER3 = "   "
        private const val SPACER4 = "     "

        private fun prefix(prefix: Int, builder: StringBuilder): StringBuilder {
            if (prefix == 0) {
                builder.append("")
            } else {
                for (i in 0 until prefix) {
                    builder.append(" ")
                }
            }

            return builder
        }

        private fun line(prefix: Int, builder: StringBuilder, vararg strings: Any) {
            prefix(prefix, builder)

            strings.forEach {
                builder.append(it.toString())
            }

            builder.append(NL)
        }

        /**
         * Returns the LICENSE text file, as a combo of the listed licenses. Duplicates are removed.
         */
        fun buildString(licenses: MutableList<LicenseData>, prefixOffset: Int = 0): String {
            val b = StringBuilder(256)

            sortAndClean(licenses)

            var first = true

            licenses.forEach { license ->
                // append a new line AFTER the first entry
                if (first) {
                    first = false
                }
                else {
                    b.append(NL).append(NL)
                }

                buildLicenseString(b, license, prefixOffset)
            }

            return b.toString()
        }

        // NOTE: we ALWAYS use unix line endings!
        private fun buildLicenseString(b: StringBuilder, license: LicenseData, prefixOffset: Int) {
            line(prefixOffset, b, HEADER, license.name, " - ", license.description)

            license.urls.forEach {
                line(prefixOffset, b, SPACER3, it)
            }

            prefix(prefixOffset, b).append(SPACER3).append("Copyright")
            if (license.copyrights.isEmpty()) {
                // append the current year
                b.append(" ").append(LocalDate.now().year)
            }
            else if (license.copyrights.size == 1) {
                // this is 2001
                b.append(" ").append(license.copyrights.first())
            } else {
                // is this 2001,2002,2004,2014  <OR>   2001-2014
                val sumA = license.copyrights.sum()
                val sumB = license.copyrights.first().rangeTo(license.copyrights.last()).sum()
                if (sumA == sumB) {
                    // this is 2001-2004
                    b.append(" ").append(license.copyrights.first()).append("-").append(license.copyrights.last())
                } else {
                    // this is 2001,2002,2004
                    license.copyrights.forEach {
                        b.append(" ").append(it).append(",")
                    }
                    b.deleteCharAt(b.length-1)
                }
            }
            b.append(HEADER).append(license.license.preferedName).append(NL)

            license.authors.forEach {
                line(prefixOffset, b, SPACER4, it)
            }

            if (license.license === License.CUSTOM) {
                line(prefixOffset, b, HEADR4)
            }

            license.notes.forEach {
                line(prefixOffset, b, SPACER3, it)
            }

            // now add the DEPENDENCY license information. This info is nested (and CAN contain duplicates from elsewhere!)
            if (license.extras.isNotEmpty()) {
                var isFirstExtra = true

                line(prefixOffset, b, SPACER3, NL, "Extra license information")
                license.extras.forEach { extraLicense ->
                    if (isFirstExtra) {
                        isFirstExtra = false
                    } else {
                        b.append(NL)
                    }

                    buildLicenseString(b, extraLicense, prefixOffset + 4)
                }
            }
        }

        /**
         * Sorts and remove dupes for the list of licenses.
         */
        private fun sortAndClean(licenses: MutableList<LicenseData>) {
            if (licenses.isEmpty()) {
                return
            }

            // The FIRST one is always FIRST! (the rest are alphabetical by name)
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

class CopyrightRange internal constructor(private val license: LicenseData,
                                          private val start: Int,
                                          private val copyrights: MutableList<Int>) {
    fun to(copyRight: Int) {
        if (start >= copyRight) {
            throw GradleException("Cannot have a start copyright date that is equal or greater than the `to` copyright date for ${license.name}")
        }

        val newStart = start+1
        if (newStart < copyRight) {
            // increment by 1, since the first part of the range is already added
            copyrights.addAll((newStart).rangeTo(copyRight))
        }
    }

    fun toNow() {
        val nowYear = LocalDate.now().year
        if (start < nowYear) {
            to(nowYear)
        }
    }
}
