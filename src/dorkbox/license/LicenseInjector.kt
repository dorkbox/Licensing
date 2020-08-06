package dorkbox.license

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.*
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import javax.inject.Inject



internal open class LicenseInjector @Inject constructor(@Internal val extension: Licensing) : DefaultTask() {
    // only want to build these files once
    private var alreadyBuilt = false

    companion object {
        const val LICENSE_FILE = "LICENSE"
        const val LICENSE_BLOB = "LICENSE.blob"
    }

    @Input lateinit var licenses: MutableList<LicenseData>
    @OutputDirectory lateinit var outputDir: File
    @InputDirectory lateinit var rootDir: File

    init {
        outputs.upToDateWhen {
            alreadyBuilt || !(checkLicenseFiles(outputDir, licenses) && checkLicenseFiles(rootDir, licenses))
        }
    }

    @TaskAction
    fun doTask() {
        if (alreadyBuilt) {
            return
        }
        alreadyBuilt = true

        // now we want to add license information that we know about from our dependencies to our list
        // just to make it clear, license information CAN CHANGE BETWEEN VERSIONS! For example, JNA changed from GPL to Apache in version 4+
        // we associate the artifact group + id + (start) version as a license.
        // if a license for a dependency is UNKNOWN, then we emit a warning to the user to add it as a pull request
        // if a license version is not specified, then we use the default
        DependencyScanner(project, extension).scanForLicenseData()

        // true if there was any work done
        didWork = buildLicenseFiles(outputDir, licenses) && buildLicenseFiles(rootDir, licenses)
    }

    /**
     * @return true when there is work that needs to be done
     */
    private fun checkLicenseFiles(outputDir: File, licenses: MutableList<LicenseData>): Boolean {
        var needsToDoWork = false
        if (!outputDir.exists()) outputDir.mkdirs()

        val licenseText = LicenseData.buildString(licenses)
        val licenseFile = File(outputDir, LICENSE_FILE)

        if (fileIsNotSame(licenseFile, licenseText)) {
            // write out the LICENSE and various license files
            needsToDoWork = true
        }

        if (!needsToDoWork) {
            licenses.forEach {
                val license = it.license
                val file = File(outputDir, license.licenseFile)
                val sourceText = license.licenseText

                if (fileIsNotSame(file, sourceText)) {
                    needsToDoWork = true
                }
            }
        }


        return needsToDoWork
    }

    /**
     * @return true when there is work that has be done
     */
    private fun buildLicenseFiles(outputDir: File, licenses: MutableList<LicenseData>): Boolean {
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

                // save off the blob, so we can check when reading dependencies if we can
                // import this license info as extra license info for the project
                ObjectOutputStream(FileOutputStream(licenseBlob)).use { oos ->
                    oos.writeInt(licenses.size)

                    licenses.forEach {
                        it.writeObject(oos)
                    }
                }

                hasDoneWork = true
            }

            licenses.forEach {
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
