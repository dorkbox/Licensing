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
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.File

/**
 * License definition and management plugin for the Gradle build system
 */
class LicensePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val outputDir = File(project.buildDir, "licensing")

        // Create the Plugin extension object (for users to configure our execution).
        val extension = project.extensions.create(Licensing.NAME, Licensing::class.java, project, outputDir)

        val licenseInjector = project.tasks.create("generateLicenseFiles", LicenseInjector::class.java).apply {
            group = "other"
        }

        licenseInjector.outputDir = outputDir
        licenseInjector.rootDir = project.rootDir
        licenseInjector.licenses = extension.licenses

        // the task will only build files that it needs to (and will only run once)
        project.tasks.forEach {
            when (it) {
                is KotlinCompile       -> it.dependsOn += licenseInjector
                is AbstractCompile     -> it.dependsOn += licenseInjector
                is AbstractArchiveTask -> it.dependsOn += licenseInjector
            }
        }



        project.afterEvaluate { prj ->
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
                    val publishingExt = prj.extensions.getByType(PublishingExtension::class.java)
                    publishingExt.publications.forEach {
                        if (MavenPublication::class.java.isAssignableFrom(it.javaClass)) {
                            it as MavenPublication

                            // add the license information. ONLY THE FIRST ONE!
                            val licenseData = extension.licenses.first()
                            val license = licenseData.license
                            it.pom.licenses { licSpec ->
                                licSpec.license { newLic ->
                                    newLic.name.set(license.preferedName)
                                    newLic.url.set(license.preferedUrl)

                                    // only include license "notes" if we are a custom license **which is the license itself**
                                    if (license == License.CUSTOM) {
                                        val notes = licenseData.notes.asSequence().joinToString("")
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
                        it.from(extension.output())
                    }
                }
            }
        }
    }
}


