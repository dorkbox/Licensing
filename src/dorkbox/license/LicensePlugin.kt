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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.AbstractCompile
import java.util.*

/**
 * License definition and management plugin for the Gradle build system
 */
class LicensePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val publications = project.extensions.getByType(PublishingExtension::class.java).publications.filter { MavenPublication::class.java.isAssignableFrom(it.javaClass) }

        // Create the Plugin extension object (for users to configure our execution).
        val extension: Licensing = project.extensions.create(Licensing.NAME, Licensing::class.java, project)

        val generateLicenseFiles = project.tasks.register("generateLicenseFiles", LicenseInjector::class.java, extension, publications)

        // the task will only build files that it needs to (and will only run once)
        project.tasks.withType(AbstractCompile::class.java) { task ->
            // make sure that the license info is always built before the task
            task.dependsOn(generateLicenseFiles)
        }

        // the task will only build files that it needs to (and will only run once)
        project.tasks.withType(AbstractArchiveTask::class.java) { task ->
            // make sure that the license info is always built before the task
            task.dependsOn(generateLicenseFiles)

            // don't include the license file from the root directory (which happens by default).
            // make sure that our license files are included in task resources (when building a jar, for example)
            task.from(extension.jarOutput)
        }

        // Make sure to clean-up any possible license file on clean
        project.gradle.taskGraph.whenReady { it ->
            val hasClean = project.gradle.startParameter.taskNames.filter { taskName ->
                taskName.lowercase(Locale.getDefault()).contains("clean")
            }

            // This MUST be first, since it loads license data that is used elsewhere
            // show scanning or missing, but not both
            // NOTE: we scan the dependencies in ALL subprojects as well.
            extension.scanDependencies

            if (hasClean.isNotEmpty()) {
                extension.allPossibleOutput().forEach {
                    it.delete()
                }

                val task = project.allprojects.last().tasks.last()
                task.doLast {
                    println("\tRefreshing license data...")
                    // always regen the license files (technically, a `clean` should remove temp stuff -- however license files are not temp!)
                    generateLicenseFiles.get().doTaskInternal()
                }
            }
        }
    }
}
