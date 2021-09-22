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
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.compile.AbstractCompile

/**
 * License definition and management plugin for the Gradle build system
 */
class LicensePlugin : Plugin<Project> {

    override fun apply(project: Project) {
        // Create the Plugin extension object (for users to configure our execution).
        val extension: Licensing = project.extensions.create(Licensing.NAME, Licensing::class.java, project)

        val generateLicenseFiles = project.tasks.create("generateLicenseFiles", LicenseInjector::class.java, extension)

        project.afterEvaluate { p ->
            // the task will only build files that it needs to (and will only run once)
            p.tasks.forEach { task ->
                when (task) {
                    is AbstractCompile -> task.dependsOn(generateLicenseFiles)
                    is AbstractArchiveTask -> task.dependsOn(generateLicenseFiles)
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




