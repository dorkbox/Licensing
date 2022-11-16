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
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

open class Licensing(private val project: Project) {
    companion object {
        fun getLicense(licenseFile: String) : ByteArray {
            // license files are located in this package...
            val stream = Licensing::class.java.getResourceAsStream(licenseFile)
            // .use{} will close the stream when it's done...
            return stream?.use { it.readBytes() } ?: ByteArray(0)
        }

        internal const val NAME = "licensing"

        /**
         * If the kotlin plugin is applied, and there is a compileKotlin task.. Then kotlin is enabled
         * NOTE: This can ONLY be called from a task, it cannot be called globally!
         */
        fun hasKotlin(project: Project, debug: Boolean = false): Boolean {
            try {
                // check if plugin is available
                project.plugins.findPlugin("org.jetbrains.kotlin.jvm") ?: return false

                if (debug) println("\tHas kotlin plugin")

                // this will check if the task exists, and throw an exception if it does not or return false
                project.tasks.named("compileKotlin", KotlinCompile::class.java).orNull ?: return false

                if (debug) println("\tHas compile kotlin task")

                // check to see if we have any kotlin file
                val sourceSets = project.extensions.getByName("sourceSets") as SourceSetContainer
                val main = sourceSets.getByName("main")
                val kotlin = project.extensions.getByType(org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension::class.java).sourceSets.getByName("main").kotlin

                if (debug) {
                    println("\tmain dirs: ${main.java.srcDirs}")
                    println("\tkotlin dirs: ${kotlin.srcDirs}")

                    project.buildFile.parentFile.walkTopDown().filter { it.extension == "kt" }.forEach {
                        println("\t\t$it")
                    }
                }

                val files = main.java.srcDirs + kotlin.srcDirs
                files.forEach { srcDir ->
                    val kotlinFile = srcDir.walkTopDown().find { it.extension == "kt" }
                    if (kotlinFile?.exists() == true) {
                        if (debug) println("\t Has kotlin file: $kotlinFile")
                        return true
                    }
                }
            } catch (e: Exception) {
                if (debug) e.printStackTrace()
            }

            return false
        }
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
    fun scanDependencies(project: Project, allProjects: Boolean): LicenseDependencyScanner.ScanDep {
        // now we want to add license information that we know about from our dependencies to our list
        // just to make it clear, license information CAN CHANGE BETWEEN VERSIONS! For example, JNA changed from GPL to Apache in version 4+
        // we associate the artifact group + id + (start) version as a license.
        // if a license for a dependency is UNKNOWN, then we emit a warning to the user to add it as a pull request
        // if a license version is not specified, then we use the default
        val depInfo = LicenseDependencyScanner.scanForLicenseData(project, allProjects, this.licenses)


        // we only should include the kotlin license information IF we actually use kotlin.
        //
        // If kotlin is not used, we should suppress the license
        val doesNotUseKotlin = try {
            val kotlin = project.extensions.getByType(org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension::class.java).sourceSets.getByName("main").kotlin

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

        return depInfo
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
