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
import org.gradle.api.Project
import java.io.File

open class Licensing(private val project: Project) {
    companion object {
        fun getLicense(licenseFile: String) : ByteArray {
            // license files are located in this package...
            val stream = Licensing::class.java.getResourceAsStream(licenseFile)
            // .use{} will close the stream when it's done...
            return stream?.use { it.readBytes() } ?: ByteArray(0)
        }

        const val LICENSE_FILE = "LICENSE"
        const val LICENSE_BLOB = "LICENSE.blob"

        internal const val NAME = "licensing"
    }

    private val projectName = project.name

    val licenses = mutableListOf<LicenseData>()

    val outputBuildDir: File by lazy {
        File(project.layout.buildDirectory.asFile.get(), "licensing")
    }
    val outputRootDir: File = project.rootDir

    val hasKotlin: Boolean
        get() = project.plugins.hasPlugin("org.jetbrains.kotlin.jvm") ||
                project.plugins.hasPlugin("org.jetbrains.kotlin.multiplatform") ||
                project.plugins.hasPlugin("org.jetbrains.kotlin.android")


    /**
     * Gets a list of files, representing the on-disk location of each generated license file
     */
    val jarOutput: List<File> by lazy {
        // have to get the list of flattened license files, so we can get ALL the files needed
        val flatLicenseFiles = mutableSetOf<String>()
        licenses.forEach {
            flattenDep(it, flatLicenseFiles)
        }

        val files = mutableSetOf<File>()

        /// outputBuildDir
        files.add(File(outputBuildDir, LICENSE_FILE))
        files.add(File(outputBuildDir, LICENSE_BLOB))
        flatLicenseFiles.forEach {
            if (it.isNotBlank()) {
                files.add(File(outputBuildDir, it))
            }
        }

        files.sorted().toList()
    }

    private fun flattenDep(dep: LicenseData, flatFiles: MutableSet<String>) {
        flatFiles.add(dep.license.licenseFile)
        dep.extras.forEach {
            flattenDep(it, flatFiles)
        }
    }

    /**
     * Gets a list of files, representing the on-disk location of each generated license file
     */
    val output: List<File> by lazy {
        // have to get the list of flattened license files, so we can get ALL of the files needed
        val flatLicenseFiles = mutableSetOf<String>()
        licenses.forEach {
            flattenDep(it, flatLicenseFiles)
        }

         val files = mutableSetOf<File>()

        /// outputBuildDir
        files.add(File(outputBuildDir, LICENSE_FILE))
        files.add(File(outputBuildDir, LICENSE_BLOB))
        flatLicenseFiles.forEach {
            if (it.isNotBlank()) {
                files.add(File(outputBuildDir, it))
            }
        }

        /// root dir
        files.add(File(outputRootDir, LICENSE_FILE))
        flatLicenseFiles.forEach {
            if (it.isNotBlank()) {
                files.add(File(outputRootDir, it))
            }
        }

        files.toList()
    }


    /**
     * Gets a list of files, representing the on-disk location of ALL POSSIBLE generated license file
     */
    fun allPossibleOutput(): List<File> {
        val licenseFileNames = License.entries.map { it.licenseFile }.filter { it.isNotEmpty() }
        val files = mutableListOf<File>()

        /// outputBuildDir
        files.add(File(outputBuildDir, LICENSE_FILE))
        files.add(File(outputBuildDir, LICENSE_BLOB))
        licenseFileNames.forEach {
            val file = File(outputBuildDir, it)
            if (file.exists()) {
                files.add(file)
            }
        }

        /// root dir
        files.add(File(outputRootDir, LICENSE_FILE))
        licenseFileNames.forEach {
            val file = File(outputRootDir, it)
            if (file.exists()) {
                files.add(file)
            }
        }

        return files
    }

    /**
     * Adds a new license section using the project's name as the assigned name
     */
    fun license(license: License, licenseAction: LicenseData.() -> Unit) {
        val licenseData = LicenseData(projectName, license)
        licenseAction(licenseData)
        licenses.add(licenseData)
    }

    /**
     * Adds a new license section using the specified name as the assigned name
     */
    fun license(name: String, license: License, licenseAction: LicenseData.() -> Unit) {
        val licenseData = LicenseData(name, license)
        licenseAction(licenseData)
        licenses.add(licenseData)
    }
}
