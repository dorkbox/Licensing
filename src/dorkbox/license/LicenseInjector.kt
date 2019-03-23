package dorkbox.license

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File

internal open class LicenseInjector : DefaultTask() {
    // only want to build these files once
    private var alreadyBuilt = false

    lateinit var licenses: MutableList<LicenseData>
    lateinit var outputDir: File
    lateinit var rootDir: File

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

        // true if there was any work done
        didWork = buildLicenseFiles(outputDir, licenses) && buildLicenseFiles(rootDir, licenses)
    }

    private fun checkLicenseFiles(outputDir: File, licenses: MutableList<LicenseData>): Boolean {
        var needsToDoWork = false
        if (!outputDir.exists()) outputDir.mkdirs()

        val licenseText = LicenseData.buildString(licenses)
        val licenseFile = File(outputDir, "LICENSE")

        if (fileIsNotSame(licenseFile, licenseText)) {
            // write out the LICENSE and various license files
            needsToDoWork = true
        }

        licenses.forEach {
            val license = it.license
            val file = File(outputDir, license.licenseFile)
            val sourceText = license.licenseText

            if (fileIsNotSame(file, sourceText)) {
                needsToDoWork = true
            }
        }

        return needsToDoWork
    }

    private fun buildLicenseFiles(outputDir: File, licenses: MutableList<LicenseData>): Boolean {
        var hasDoneWork = false

        if (!outputDir.exists()) outputDir.mkdirs()

        val licenseText = LicenseData.buildString(licenses)
        val licenseFile = File(outputDir, "LICENSE")

        if (fileIsNotSame(licenseFile, licenseText)) {
            // write out the LICENSE and various license files
            licenseFile.writeText(licenseText)
            hasDoneWork = true
        }

        licenses.forEach {
            val license = it.license
            val file = File(outputDir, license.licenseFile)
            val sourceText = license.licenseText

            if (fileIsNotSame(file, sourceText)) {
                file.writeText(sourceText)
                hasDoneWork = true
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
        if (outputFile.canRead()) {
            return !(sourceText.toByteArray() contentEquals outputFile.readBytes())
        }

        return true
    }
}
