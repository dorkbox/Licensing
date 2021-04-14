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

        val generateLicenseFiles = project.tasks.create("generateLicenseFiles", LicenseInjector::class.java, extension).apply {
            group = "other"
        }


        // collect ALL projected + children projects.
        val projects = mutableListOf<Project>()
        val recursive = LinkedList<Project>()
        recursive.add(project)


        var next: Project
        while (recursive.isNotEmpty()) {
            next = recursive.poll()
            projects.add(next)
            recursive.addAll(next.childProjects.values)
        }

        projects.forEach { p1 ->
            p1.afterEvaluate { p ->
                // the task will only build files that it needs to (and will only run once)
                p.tasks.forEach { task ->
                    when (task) {
                        is AbstractCompile -> task.dependsOn(generateLicenseFiles)
                        is AbstractArchiveTask -> task.dependsOn(generateLicenseFiles)
                    }
                }
            }
        }

        // Make sure to cleanup the any possible license file on clean
        project.gradle.taskGraph.whenReady { it ->
            val isClean = it.allTasks.firstOrNull { it.name == "clean" } != null
            if (isClean) {
                println("\tCleaning license data")
                extension.allPossibleOutput().forEach {
                    it.delete()
                }
            }
        }
    }
}




