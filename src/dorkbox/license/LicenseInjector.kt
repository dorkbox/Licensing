/*
 * Copyright 2023 dorkbox, llc
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
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.util.*
import javax.inject.Inject

internal open class LicenseInjector @Inject constructor(@Internal val extension: Licensing, @Internal val publications: List<MavenPublication>) : DefaultTask() {
    companion object {
        const val LICENSE_FILE = "LICENSE"
        const val LICENSE_BLOB = "LICENSE.blob"
    }


    @Input val licenses = extension.licenses
    @OutputFiles val outputFiles = extension.output

    init {
        group = "other"
        outputs.upToDateWhen {
            filesUpToDate
        }
    }

    private val filesUpToDate: Boolean by lazy {
            val checkBuild = checkLicenseFiles(extension.outputBuildDir, licenses)
            val checkRoot = checkLicenseFiles(extension.outputRootDir, licenses)

//            println("Valid at : ${extension.outputBuildDir} : $checkBuild")
//            println("Valid at : ${extension.outputRootDir} : $checkRoot")

            if (!checkBuild) {
//                println("UpToDate: false")
                return@lazy false
            }

            if (!checkRoot) {
//                println("UpToDate: false")
                return@lazy false
            }

            return@lazy true
        }



    @TaskAction
    fun doTask() {
        if (!filesUpToDate) {
            println("\tGenerating License data")
        }
        doTaskInternal()
    }

    fun doTaskInternal() {
        // This MUST be first, since it loads license data that is used elsewhere
        // show scanning or missing, but not both
        // NOTE: we scan the dependencies in ALL subprojects as well.
        val (preloadedText, embeddedText, missingText) = extension.scanDependencies


        // validate the license text configuration section in the gradle file
        val licensing = extension.licenses
        if (licensing.isNotEmpty()) {
            licensing.forEach {
                when {
                    it.name.isEmpty()    -> throw GradleException("The name of the project this license applies to must be set for the '${it.license.preferredName}' license")
                    it.authors.isEmpty() -> throw GradleException("An author must be specified for the '${it.license.preferredName}' license")
                }
            }

            // add the license information to maven POM, if applicable
            try {
                publications.forEach {
                    // get the license information. ONLY FROM THE FIRST ONE! (which is the license for our project)
                    val licenseData = licensing.first()
                    val license = licenseData.license

                    it.pom.licenses { licSpec ->
                        licSpec.license { newLic ->
                            newLic.name.set(license.preferredName)
                            newLic.url.set(license.preferredUrl)

                            // only include license "notes" if we are a custom license **which is the license itself**
                            if (license == License.CUSTOM) {
                                val notes = licenseData.notes.joinToString("")
                                newLic.comments.set(notes)
                            }
                        }
                    }
                }
            } catch (ignored: Exception) {
                // there aren't always maven publishing used
            }
        }


        if (!filesUpToDate) {
            // true if there was any work done. checks while it goes as well
            val buildDir = buildLicenseFiles(extension.outputBuildDir, licenses, true)
            val rootDir = buildLicenseFiles(extension.outputRootDir, licenses, false)

            didWork = buildDir || rootDir
        }


        val hasArchiveTask = project.gradle.taskGraph.allTasks.filterIsInstance<AbstractArchiveTask>().isNotEmpty()
        if (hasArchiveTask || didWork) {
            if (preloadedText.isNotEmpty()) {
                println("\tPreloaded license data:")
                preloadedText.forEach {
                    println(it)
                }
            }
            if (embeddedText.isNotEmpty()) {
                println("\tEmbedded license data:")
                embeddedText.forEach {
                    println(it)
                }
            }
            if (missingText.isNotEmpty()) {
                println("\tMissing license data:")
                missingText.forEach {
                    println(it)
                }
                println("\tPlease submit an issue with this information to include it in future license scans.")
            }
        }
    }

    /**
     * @return false when there is work that needs to be done
     */
    private fun checkLicenseFiles(outputDir: File, licenses: MutableList<LicenseData>): Boolean {
        if (!outputDir.exists()) outputDir.mkdirs()

        val licenseBytes = LicenseData.buildString(licenses).toByteArray(Charsets.UTF_8)
        val licenseFile = File(outputDir, LICENSE_FILE)

//        println("\tLicenses: ${licenses.joinToString()}")

        // check the license file first
        if (!fileIsSame(licenseFile, licenseBytes)) {
//            println("\t\tFile $licenseFile is not the same as the license file")
            // work needs doing
            return false
        }

        licenses.forEach {
            val license = it.license

            if (license != License.UNKNOWN) {
                val file = File(outputDir, license.licenseFile)
                val sourceBytes = license.licenseBytes

                if (!fileIsSame(file, sourceBytes)) {
                    // work needs doing

//                    println("\t\tFile $licenseFile is not the same as the license file")
                    return false
                }
            }
        }

        return true
    }

    /**
     * @return true when there is work that has been done
     */
    private fun buildLicenseFiles(outputDir: File, licenses: MutableList<LicenseData>, buildLicenseBlob: Boolean): Boolean {
        var hasDoneWork = false

        if (!outputDir.exists()) outputDir.mkdirs()

        val licenseBytes = LicenseData.buildString(licenses).toByteArray(Charsets.UTF_8)

        if (licenseBytes.isEmpty()) {
            println("\tNo License information defined in the project.  Unable to build license data")
        } else {
            val licenseFile = File(outputDir, LICENSE_FILE)
            val licenseBlob = File(outputDir, LICENSE_BLOB)

            if (!fileIsSame(licenseFile, licenseBytes)) {
                // write out the LICENSE files
                licenseFile.writeBytes(licenseBytes)

                if (buildLicenseBlob) {
                    // save off the blob, so we can check when reading dependencies if we can
                    // import this license info as extra license info for the project
                    ObjectOutputStream(FileOutputStream(licenseBlob)).use { oos ->
                        oos.writeInt(licenses.size)

                        licenses.forEach {
                            it.writeObject(oos)
                        }
                    }
                }

                hasDoneWork = true
            }

            // for the license files, we have to FLATTEN the list of licenses!
            val flattenedLicenses = mutableSetOf<LicenseData>()
            val scanningLicenses: LinkedList<LicenseData> = LinkedList<LicenseData>()
            scanningLicenses.addAll(licenses)

            while (scanningLicenses.isNotEmpty()) {
                val license = scanningLicenses.remove()
                val wasAdded = flattenedLicenses.add(license)
                if (wasAdded) {
                    // should always be added, but MAYBE there is a loop somewhere. this hopefully prevents that
                    scanningLicenses.addAll(license.extras)
                }
            }

            flattenedLicenses.forEach {
                val license = it.license

                // DO NOT write license text/info for custom/unknown/commercial/etc licenses (these have no license text files)
                if (license.licenseFile.isNotEmpty()) {
                    val file = File(outputDir, license.licenseFile)
                    val sourceBytes = license.licenseBytes

                    // have to replace the PRIMARY license bytes section with the proper license


                    if (!fileIsSame(file, sourceBytes)) {
                        // write out the various license text files (NOTE: we force LF for everything!)
                        file.writeBytes(sourceBytes)


                        hasDoneWork = true
                    }
                }
            }
        }

        return hasDoneWork
    }

    /**
     * this is so we can check if we need to re-write the file. This is done to
     * save write cycles on low-end drives where write frequency is an issue
     *
     * @return FALSE if the file IS DIFFERENT, TRUE if the file IS THE SAME
     */
    private fun fileIsSame(outputFile: File, sourceBytes: ByteArray): Boolean {
//        println("\t\tLicenseFile: $outputFile")

        if (!outputFile.canRead()) {
//            println("\t\tLicenseFile does not exist: $outputFile")
            return false // not the same, so work needs to be done
        }

        val fileSize = outputFile.length()

        if (sourceBytes.size.toLong() == fileSize) {
//            println("\t\tLicenseSize are equal")
            return true
        }

        return try {
            val b = sourceBytes contentEquals outputFile.readBytes()
//            println("\t\tLicenseBytes are equal: $b")
            !b
        } catch (e: Exception) {
            return false // not the same, so work needs to be done
        }
    }
}
