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


open class LicenseData(var name: String, var license: License) : java.io.Serializable, Comparable<LicenseData> {
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
     * The Berne Convention leaves "implementation details" up to individual countries.
     *
     * In the US (which is the relevant country for FSF documents), copyright notices are defined by Title 17, Chapter 4;
     * for "visually perceptive copies" (which includes software). http://www.copyright.gov/title17/92chap4.html
     *
     * These take the form :
     *
     *  © year name
     *
     * where
     *
     * `©` is specifically that symbol (not "(c)", which has no legal value), and can be replaced by "Copyright" or "Copr." (§ 401(b)(1))
     *
     * `year` is the year of first publication of the work, or the year of first publication of the compilation or derivative work if relevant
     *
     * `name` is the name of the owner of the copyright.
     *
     * What counts is years of publication; for software this is generally considered years in which the software is released.
     *
     * So if you release a piece of software in 2014, and release it again in 2016 without making changes in 2015, the years of publication
     * would be 2014 and 2016, and the copyright notices would be "© 2014" in the first release and "© 2016" in the second release.
     *
     *
     * When scanning for license copyright dates, we use the date of publication (the date of the manifest file, within the published jar)
     */
    internal var copyright = LocalDate.now().year


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
    fun extra(name: String, license: License, licenseAction: Action<ExtraLicenseData>) {
        val licenseData = ExtraLicenseData(name, license)
        licenseAction.execute(licenseData)
        extras.add(licenseData)
    }

    /**
     * Specifies the extra license information for this project
     */
    fun extra(name: String, license: License, licenseAction: (ExtraLicenseData) -> Unit) {
        val licenseData = ExtraLicenseData(name, license)
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
        if (copyright != other.copyright) return false
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
        result = 31 * result + copyright
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

        s.writeInt(copyright)

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

    // this is used to load license data from supported dependencies
    @Throws(IOException::class)
    fun readObject(s: ObjectInputStream) {
        name = s.readUTF()

        val licenseName = s.readUTF()
        license = License.valueOf(licenseName)
        description = s.readUTF()

        copyright = s.readInt()

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
        if (extrasSize > 0) {
            for (i in 1..extrasSize) {
                val dep = ExtraLicenseData("", License.CUSTOM)
                dep.readObject(s) // can recursively create objects
                extras.add(dep)
            }
        }
    }

    companion object {
        private const val serialVersionUID = 1L

        // NOTE: we ALWAYS use unix line endings!
        private const val NL = "\n"
        private const val HEADER1 = " - "
        private const val HEADER4 = " ---- "
        private const val SPACER3 = "   "

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
            line(prefixOffset, b, HEADER1, license.name, " - ", license.description)

            line(prefixOffset, b, SPACER3, "[", license.license.preferedName, "]")

            license.urls.forEach {
                line(prefixOffset, b, SPACER3, it)
            }

            // as per US Code Title 17, Chapter 4; for "visually perceptive copies" (which includes software).
            // http://www.copyright.gov/title17/92chap4.html
            // it is ONLY... © year name  (or Copyright year name)
            // authors go on a separate line
            if (license is ExtraLicenseData) {
                if (license.copyright != 0) {
                    // we only want to have the copyright info IF we specified it on the extra license info (otherwise, they are under
                    // the same copyright date as the parent)
                    line(prefixOffset, b, SPACER3, "Copyright ", license.copyright)
                }
            } else {
                line(prefixOffset, b, SPACER3, "Copyright ", license.copyright)
            }

            license.authors.forEach {
                line(prefixOffset, b, SPACER3, "  ", it)
            }

            if (license.license === License.CUSTOM) {
                line(prefixOffset, b, HEADER4)
            }

            license.notes.forEach {
                line(prefixOffset, b, SPACER3, it)
            }

            // now add the DEPENDENCY license information. This info is nested (and CAN contain duplicates from elsewhere!)
            if (license.extras.isNotEmpty()) {
                var isFirstExtra = true

                b.append(NL)
                line(prefixOffset, b, SPACER3, "Extra license information")
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
