package dorkbox.license

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.*
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.util.*
import javax.inject.Inject



internal open class LicenseInjector @Inject constructor(project: Project, @Internal val extension: Licensing) : DefaultTask() {
    companion object {
        const val LICENSE_FILE = "LICENSE"
        const val LICENSE_BLOB = "LICENSE.blob"
    }

    @Input val licenses = extension.licenses

    @OutputFiles val outputFiles = mutableListOf<File>()

    init {
        val outputBuildDir = File(project.buildDir, "licensing")

        /// outputBuildDir
        outputFiles.add(File(outputBuildDir, LICENSE_FILE))
        outputFiles.add(File(outputBuildDir, LICENSE_BLOB))
        licenses.forEach {
            outputFiles.add(File(outputBuildDir, it.license.licenseFile))
        }

        /// root dir
        outputFiles.add(File(project.rootDir, LICENSE_FILE))
        outputFiles.add(File(project.rootDir, LICENSE_BLOB))
        licenses.forEach {
            outputFiles.add(File(project.rootDir, it.license.licenseFile))
        }

        outputs.upToDateWhen {
            !(checkLicenseFiles(outputBuildDir, licenses) && checkLicenseFiles(project.rootDir, licenses))
        }
    }

    @TaskAction
    fun doTask() {
        // now we want to add license information that we know about from our dependencies to our list
        // just to make it clear, license information CAN CHANGE BETWEEN VERSIONS! For example, JNA changed from GPL to Apache in version 4+
        // we associate the artifact group + id + (start) version as a license.
        // if a license for a dependency is UNKNOWN, then we emit a warning to the user to add it as a pull request
        // if a license version is not specified, then we use the default
        DependencyScanner(project, extension).scanForLicenseData()

        // true if there was any work done
        didWork = buildLicenseFiles(File(project.buildDir, "licensing"), licenses, true) && buildLicenseFiles(project.rootDir, licenses, false)
    }

    /**
     * @return true when there is work that needs to be done
     */
    private fun checkLicenseFiles(outputDir: File, licenses: MutableList<LicenseData>): Boolean {
        if (!outputDir.exists()) outputDir.mkdirs()

        val licenseText = LicenseData.buildString(licenses)
        val licenseFile = File(outputDir, LICENSE_FILE)

        if (fileIsNotSame(licenseFile, licenseText)) {
            // write out the LICENSE and various license files
            return true
        }

        licenses.forEach {
            val license = it.license
            val file = File(outputDir, license.licenseFile)
            val sourceText = license.licenseText

            if (fileIsNotSame(file, sourceText)) {
                return true
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

        val licenseText = LicenseData.buildString(licenses)
        if (licenseText.isEmpty()) {
            println("\tNo License information defined in the project.  Unable to build license data")
        } else {
            val licenseFile = File(outputDir, LICENSE_FILE)
            val licenseBlob = File(outputDir, LICENSE_BLOB)

            if (fileIsNotSame(licenseFile, licenseText)) {
                // write out the LICENSE files
                licenseFile.writeText(licenseText)

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
                val file = File(outputDir, license.licenseFile)
                val sourceText = license.licenseText

                if (fileIsNotSame(file, sourceText)) {
                    // write out the various license text files
                    file.writeText(sourceText)

                    hasDoneWork = true
                }
            }
        }

        return hasDoneWork
    }

    /**
     * this is so we can check if we need to re-write the file. This is done to
     * save write cycles on low-end drives where write frequency is an issue
     *
     * @return TRUE if the file IS NOT THE SAME, FALSE if the file IS THE SAME
     */
    private fun fileIsNotSame(outputFile: File, sourceText: String): Boolean {
        return !(outputFile.canRead() && sourceText.toByteArray() contentEquals outputFile.readBytes())
    }
}
