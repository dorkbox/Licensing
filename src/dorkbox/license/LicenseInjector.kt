package dorkbox.license

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.util.*
import javax.inject.Inject



internal open class LicenseInjector @Inject constructor(@Internal val extension: Licensing) : DefaultTask() {
    companion object {
        const val LICENSE_FILE = "LICENSE"
        const val LICENSE_BLOB = "LICENSE.blob"
    }


    @Input val licenses = extension.licenses
    @OutputFiles val outputFiles = extension.output

    init {
        group = "other"
        outputs.upToDateWhen {
            !(checkLicenseFiles(extension.outputBuildDir, licenses) && checkLicenseFiles(extension.outputRootDir, licenses))
        }
    }

    @TaskAction
    fun doTask() {
        // This MUST be first, since it loads license data that is used elsewhere
        // show scanning or missing, but not both
        // NOTE: we scan the dependencies in ALL subprojects as well.
        val (preloadedText, embeddedText, missingText) = extension.scanDependencies(project, true)

        // validate the license text configuration section in the gradle file ONLY WHEN PUSHING A JAR
        val licensing = extension.licenses
        if (licensing.isNotEmpty()) {
            extension.licenses.forEach {
                when {
                    it.name.isEmpty() -> throw GradleException("The name of the project this license applies to must be set for the '${it.license.preferredName}' license")
                    it.authors.isEmpty() -> throw GradleException("An author must be specified for the '${it.license.preferredName}' license")
                }
            }

            // add the license information to maven POM, if applicable
            try {
                val publishingExt = project.extensions.getByType(PublishingExtension::class.java)
                publishingExt.publications.forEach {
                    if (MavenPublication::class.java.isAssignableFrom(it.javaClass)) {
                        it as MavenPublication

                        // get the license information. ONLY FROM THE FIRST ONE! (which is the license for our project)
                        val licenseData = extension.licenses.first()
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
                    else {
                        println("Licensing only supports maven pom license injection for now")
                    }
                }
            } catch (ignored: Exception) {
                // there aren't always maven publishing used
            }

            // the task will only build files that it needs to (and will only run once)
            project.tasks.forEach {
                if (it is AbstractArchiveTask) {
                    // don't include the license file from the root directory (which happens by default).
                    // make sure that our license files are included in task resources (when building a jar, for example)
                    it.from(extension.jarOutput)
                }
            }
        }

        // true if there was any work done. checks while it goes as well
        didWork = buildLicenseFiles(extension.outputBuildDir, licenses, true) && buildLicenseFiles(extension.outputRootDir, licenses, false)

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
     * @return true when there is work that needs to be done
     */
    private fun checkLicenseFiles(outputDir: File, licenses: MutableList<LicenseData>): Boolean {
        if (!outputDir.exists()) outputDir.mkdirs()

        val licenseBytes = LicenseData.buildString(licenses).toByteArray(Charsets.UTF_8)
        val licenseFile = File(outputDir, LICENSE_FILE)


        // check the license file first
        if (fileIsNotSame(licenseFile, licenseBytes)) {
            // work needs doing
            return true
        }

        licenses.forEach {
            val license = it.license

            if (license != License.UNKNOWN) {
                val file = File(outputDir, license.licenseFile)
                val sourceBytes = license.licenseBytes

                if (fileIsNotSame(file, sourceBytes)) {
                    // work needs doing
                    return true
                }
            }
        }

        return false
    }

    /**
     * @return true when there is work that has be done
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

            if (fileIsNotSame(licenseFile, licenseBytes)) {
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

            while(scanningLicenses.isNotEmpty()) {
                val license = scanningLicenses.remove()
                val wasAdded = flattenedLicenses.add(license)
                if (wasAdded) {
                    // should always be added, but MAYBE there is a loop somewhere. this hopefully prevents that
                    scanningLicenses.addAll(license.extras)
                }
            }

            flattenedLicenses.forEach {
                val license = it.license

                // DO NOT write license text/info for custom or unknown licenses
                if (license != License.UNKNOWN && license != License.CUSTOM) {
                    val file = File(outputDir, license.licenseFile)
                    val sourceBytes = license.licenseBytes

                    if (fileIsNotSame(file, sourceBytes)) {
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
     * @return TRUE if the file IS DIFFERENT, FALSE if the file IS THE SAME
     */
    private fun fileIsNotSame(outputFile: File, sourceBytes: ByteArray): Boolean {
        if (!outputFile.canRead()) {
            return true // not the same, so work needs to be done
        }

        val fileSize = outputFile.length()

        if (sourceBytes.isEmpty() && fileSize > 0L) {
            return true
        }

        if (sourceBytes.isNotEmpty() && fileSize == 0L) {
            return true
        }

        return try {
            !(sourceBytes contentEquals outputFile.readBytes())
        } catch (e: Exception) {
            return true // not the same, so work needs to be done
        }
    }
}
