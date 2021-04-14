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
import org.gradle.api.artifacts.Dependency
import org.gradle.api.tasks.SourceSet
import java.io.File

open class Licensing(private val project: Project) {
    companion object {
        fun getLicense(licenseFile: String) : String{
            // license files are located in this package...
            val stream = Licensing::class.java.getResourceAsStream(licenseFile)
            // .use{} will close the stream when it's done...
            return stream?.bufferedReader()?.use { it.readText() } ?: ""
        }

        internal const val NAME = "licensing"
    }

    private val projectName = project.name

    val licenses = mutableListOf<LicenseData>()

    val outputBuildDir: File = File(project.buildDir, "licensing")
    val outputRootDir: File = project.rootDir

    /**
     * Gets a list of files, representing the on-disk location of each generated license file
     */
    val jarOutput: List<File> by lazy {
        // have to get the list of flattened license files, so we can get ALL of the files needed
        val flatLicenseFiles = mutableSetOf<String>()
        licenses.forEach {
            flattenDep(it, flatLicenseFiles)
        }

        val files = mutableSetOf<File>()

        /// outputBuildDir
        files.add(File(outputBuildDir, LicenseInjector.LICENSE_FILE))
        files.add(File(outputBuildDir, LicenseInjector.LICENSE_BLOB))
        flatLicenseFiles.forEach {
            if (it.isNotBlank()) {
                files.add(File(outputBuildDir, it))
            } else {
                println("UNKNOWN!")
            }
        }

        files.toList()
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
        files.add(File(outputBuildDir, LicenseInjector.LICENSE_FILE))
        files.add(File(outputBuildDir, LicenseInjector.LICENSE_BLOB))
        flatLicenseFiles.forEach {
            if (it.isNotBlank()) {
                files.add(File(outputBuildDir, it))
            }
        }

        /// root dir
        files.add(File(outputRootDir, LicenseInjector.LICENSE_FILE))
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
        val licenseFileNames = License.values().map { it.licenseFile }.filter { it.isNotEmpty() }

        val files = mutableListOf<File>()

        /// outputBuildDir
        files.add(File(outputBuildDir, LicenseInjector.LICENSE_FILE))
        files.add(File(outputBuildDir, LicenseInjector.LICENSE_BLOB))
        licenseFileNames.forEach {
            val file = File(outputBuildDir, it)
            if (file.exists()) {
                files.add(file)
            }
        }

        /// root dir
        files.add(File(outputRootDir, LicenseInjector.LICENSE_FILE))
        licenseFileNames.forEach {
            val file = File(outputRootDir, it)
            if (file.exists()) {
                files.add(file)
            }
        }

        return files
    }


    // scan as part of the plugin
    fun scanDependencies(project: Project): Triple<MutableList<String>, MutableList<String>, MutableList<String>> {
        // now we want to add license information that we know about from our dependencies to our list
        // just to make it clear, license information CAN CHANGE BETWEEN VERSIONS! For example, JNA changed from GPL to Apache in version 4+
        // we associate the artifact group + id + (start) version as a license.
        // if a license for a dependency is UNKNOWN, then we emit a warning to the user to add it as a pull request
        // if a license version is not specified, then we use the default
        val textOutput = LicenseDependencyScanner.scanForLicenseData(project, this.licenses)


        // we only should include the kotlin license information IF we actually use kotlin.
        //
        // If kotlin is not used, we should suppress the license
        val doesNotUseKotlin = try {
            val sourceSets = project.extensions.getByName("sourceSets") as org.gradle.api.tasks.SourceSetContainer
            val mainSourceSet: SourceSet = sourceSets.getByName("main")
            val kotlin = (mainSourceSet as org.gradle.api.internal.HasConvention).convention.getPlugin(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet::class.java).kotlin

            kotlin.files.none { it.name.endsWith(".kt") }
        } catch (e: Exception) {
            false
        }

        if (doesNotUseKotlin && licenses.isNotEmpty()) {
            val extras: MutableList<LicenseData>? = licenses.firstOrNull()?.extras
            if (extras?.isNotEmpty() == true) {
                extras.removeIf {
                    it.mavenId == "org.jetbrains.kotlin" || it.mavenId == "org.jetbrains.kotlinx"
                }
            }
        }

        return textOutput
    }

    /**
     * Adds a new license section using the project's name as the assigned name
     */
    fun license(license: License, licenseAction: Action<LicenseData>) {
        val licenseData = LicenseData(projectName, license)
        licenseAction.execute(licenseData)
        licenses.add(licenseData)
    }

    /**
     * Adds a new license section using the specified name as the assigned name
     */
    fun license(name: String, license: License, licenseAction: Action<LicenseData>) {
        val licenseData = LicenseData(name, license)
        licenseAction.execute(licenseData)
        licenses.add(licenseData)
    }
}
