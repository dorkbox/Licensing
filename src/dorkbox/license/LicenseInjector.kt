package dorkbox.license

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.*
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
        outputs.upToDateWhen {
            !(checkLicenseFiles(extension.outputBuildDir, licenses) && checkLicenseFiles(extension.outputRootDir, licenses))
        }
    }

    @TaskAction
    fun doTask() {
        // true if there was any work done. checks while it goes as well
        didWork = buildLicenseFiles(extension.outputBuildDir, licenses, true) && buildLicenseFiles(extension.outputRootDir, licenses, false)
    }

    /**
     * @return true when there is work that needs to be done
     */
    private fun checkLicenseFiles(outputDir: File, licenses: MutableList<LicenseData>): Boolean {
        if (!outputDir.exists()) outputDir.mkdirs()

        val licenseText = LicenseData.buildString(licenses)
        val licenseFile = File(outputDir, LICENSE_FILE)

        if (fileIsNotSame(licenseFile, licenseText)) {
            // work needs doing
            return true
        }

        licenses.forEach {
            val license = it.license

            if (license != License.UNKNOWN) {
                val file = File(outputDir, license.licenseFile)
                val sourceText = license.licenseText

                if (fileIsNotSame(file, sourceText)) {
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

                // DO NOT write license text/info for custom or unknown licenses
                if (license != License.UNKNOWN && license != License.CUSTOM) {
                    val file = File(outputDir, license.licenseFile)
                    val sourceText = license.licenseText

                    if (fileIsNotSame(file, sourceText)) {
                        // write out the various license text files
                        file.writeText(sourceText)

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
     * @return TRUE if the file IS NOT THE SAME, FALSE if the file IS THE SAME
     */
    private fun fileIsNotSame(outputFile: File, sourceText: String): Boolean {
        if (!outputFile.canRead()) {
            return true // not the same, so work needs to be done
        }

        return try {
            !(sourceText.toByteArray() contentEquals outputFile.readBytes())
        } catch (e: Exception) {
            return true // not the same, so work needs to be done
        }
    }
}
