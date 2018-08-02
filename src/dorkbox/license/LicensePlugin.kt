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
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import java.io.File
import java.lang.Exception

/**
 * License definition and management plugin for the Gradle build system
 */
class LicensePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val outputDir = File(project.buildDir, "licensing")

        // Create the Plugin extension object (for users to configure our execution).
        val extension = project.extensions.create(Licensing.NAME, Licensing::class.java, project)

        project.afterEvaluate {
            val licensing = extension.licenses
            if (licensing.isNotEmpty()) {
                extension.licenses.forEach {
                    when {
                        it.name.isEmpty() -> throw GradleException("The name of the project this license applies to must be set for the '${it.license.preferedName}' license")
                        it.authors.isEmpty() -> throw GradleException("An author must be specified for the '${it.license.preferedName}' license")
                    }
                }

                // add the license information to maven POM, if applicable
                try {
                    val publishingExt = project.extensions.getByType(PublishingExtension::class.java)
                    publishingExt.publications.forEach {
                        if (MavenPublication::class.java.isAssignableFrom(it.javaClass)) {
                            it as MavenPublication

                            // add the license information. ONLY THE FIRST ONE!
                            val liceseData = extension.licenses.first()
                            val license = liceseData.license
                            it.pom.licenses { licSpec ->
                                licSpec.license { newLic ->
                                    newLic.name.set(license.preferedName)
                                    newLic.url.set(license.preferedUrl)

                                    // only include license "notes" if we are a custom license **which is the license itself**
                                    if (license == License.CUSTOM) {
                                        val notes = liceseData.notes.asSequence().joinToString("")
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


                val hasClean = project.gradle.startParameter.taskNames.filter { it.toLowerCase().contains("clean") }
                if (hasClean.isNotEmpty()) {
                    val task = project.tasks.last { it.name == hasClean.last() }

                    task.doLast {
                        buildLicenseFiles(outputDir, extension.licenses)
                        buildLicenseFiles(project.rootDir, extension.licenses)
                    }
                }
                else {
                    buildLicenseFiles(outputDir, extension.licenses)
                    buildLicenseFiles(project.rootDir, extension.licenses)
                }

                // make sure that our output dir is included when building
                val javaSourceSet = it.convention.getPlugin(JavaPluginConvention::class.java).sourceSets.findByName("main")
                javaSourceSet!!.resources.srcDirs(outputDir)
            }
        }
    }

    private fun buildLicenseFiles(outputDir: File, licenses: ArrayList<LicenseData>) {
        if (!outputDir.exists()) outputDir.mkdirs()

        val licenseText = LicenseData.buildString(licenses)
        val licenseFile = File(outputDir, "LICENSE")

        if (fileIsNotSame(licenseFile, licenseText)) {
            // write out the LICENSE and various license files
            licenseFile.writeText(licenseText)
        }

        licenses.forEach {
            val license = it.license
            val file = File(outputDir, license.licenseFile)
            val sourceText = license.licenseText

            if (fileIsNotSame(file, sourceText)) {
                file.writeText(sourceText)
            }
        }
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
