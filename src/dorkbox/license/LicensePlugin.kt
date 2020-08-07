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
import org.gradle.api.Task
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.AbstractCompile
import java.io.*
import java.time.Instant
import java.time.ZoneId
import java.util.*
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream



/**
 * License definition and management plugin for the Gradle build system
 */
class LicensePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Create the Plugin extension object (for users to configure our execution).
        val extension: Licensing = project.extensions.create(Licensing.NAME, Licensing::class.java, project)

        project.afterEvaluate { prj ->
            // This MUST be first!
            extension.scanDependencies()

            val licenseInjector = project.tasks.create("generateLicenseFiles", LicenseInjector::class.java, extension).apply {
                group = "other"
            }

            // the task will only build files that it needs to (and will only run once)
            prj.tasks.forEach {
                when (it) {
                    is AbstractCompile     -> it.dependsOn(licenseInjector)
                    is AbstractArchiveTask -> it.dependsOn(licenseInjector)
                }
            }

            // Make sure to cleanup the generated license files on clean
            project.gradle.taskGraph.whenReady { it ->
                val isClean = it.allTasks.firstOrNull { it.name == "clean" } != null
                if (isClean) {
                    println("\tCleaning license data...")
                    extension.output.forEach {
                        if (it.exists()) {
                            it.delete()
                        }
                    }
                }
            }

            prj.configurations.asIterable().forEach { extension.projectDependencies.addAll(it.dependencies) }

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

                            // get the license information. ONLY FROM THE FIRST ONE! (which is the license for our project)
                            val licenseData = extension.licenses.first()
                            val license = licenseData.license
                            it.pom.licenses { licSpec ->
                                licSpec.license { newLic ->
                                    newLic.name.set(license.preferedName)
                                    newLic.url.set(license.preferedUrl)

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
                prj.tasks.forEach {
                   if (it is AbstractArchiveTask) {
                        // don't include the license file from the root directory (which happens by default).
                        // make sure that our license files are included in task resources (when building a jar, for example)
                        it.from(extension.jarOutput)
                    }
                }
            }
        }
    }
}




