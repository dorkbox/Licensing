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
import org.gradle.api.Project

open class Licensing(project: Project) {
    private val projectName = project.name

    val licenses = ArrayList<LicenseData>()

    companion object {
        fun getLicense(licenseFile: String) : String{
            // license files are located in this package...
            val stream = Licensing::class.java.getResourceAsStream(licenseFile)
            // .use{} will close the stream when it's done...
            return stream?.bufferedReader()?.use { it.readText() } ?: ""
        }

        internal const val NAME = "licensing"
    }

    /**
     * Adds a new license section using the project's name as the assigned name
     */
    fun license(license: License, licenseData: Action<LicenseData>) {
        val licenseAction = LicenseData(projectName, license)
        licenseData.execute(licenseAction)
        licenses.add(licenseAction)
    }

    /**
     * Adds a new license section using the specified name as the assigned name
     */
    fun license(name: String, license: License, licenseData: Action<LicenseData>) {
        val licenseAction = LicenseData(name, license)
        licenseData.execute(licenseAction)
        licenses.add(licenseAction)
    }
}
