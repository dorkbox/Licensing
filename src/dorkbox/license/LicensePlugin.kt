/*
 * Copyright 2025 dorkbox, llc
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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.AbstractCompile

/**
 * License definition and management plugin for the Gradle build system
 */
class LicensePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager.apply("maven-publish")

        val hasArchiveTask = project.gradle.taskGraph.allTasks.filterIsInstance<AbstractArchiveTask>().isNotEmpty()

        // NOTE: there will be some duplicates, so we want to remove them
        // NOTE: the scanning MUST be in the configuration phase.
        //    RESOLUTION CAN HAPPEN LATER!
        val configs = mutableSetOf<Configuration>()

        val projectMavenIds = mutableSetOf<String>()
        project.allprojects.forEach { proj ->
            projectMavenIds.add("${proj.group}:${proj.name}:${proj.version}")

            // root + children
            var config = proj.configurations.getByName("compileClasspath")
            if (config.isCanBeResolved) {
                configs.add(config)
            }

            config = proj.configurations.getByName("runtimeClasspath")
            if (config.isCanBeResolved) {
                configs.add(config)
            }
        }

        // Create the Plugin extension object (for users to configure our execution).
        val extension: Licensing = project.extensions.create(Licensing.NAME, Licensing::class.java, project)

        val generateLicenseFiles = project.tasks.register("generateLicenseFiles", LicenseInjector::class.java, configs, projectMavenIds, extension, hasArchiveTask)

        // Defer logic that depends on project state
        project.afterEvaluate {
            // must be in the plugin.apply() method and not the task specifically, because task.project is deprecated and will be removed.
            val publications: List<MavenPublication> = project.extensions.getByType(PublishingExtension::class.java)
                .publications
                    .filter { MavenPublication::class.java.isAssignableFrom(it.javaClass) }
                    .map { it as MavenPublication }


            generateLicenseFiles.configure { task ->
                // configure the task
                task.publicationsProperty.set(publications)
            }
        }


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


        project.tasks.findByName("clean")?.apply {
            doFirst {
                extension.allPossibleOutput().forEach {
                    it.delete()
                }
                println("\tRefreshing license data ...")
            }
            doLast {
                // always regen the license files (technically, a `clean` should remove temp stuff -- however license files are not temp!)
                generateLicenseFiles.get().doTaskInternal()
            }
        }
    }
}
