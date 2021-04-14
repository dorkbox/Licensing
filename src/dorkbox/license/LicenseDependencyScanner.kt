package dorkbox.license

import License
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import java.io.*
import java.time.Instant
import java.time.ZoneId
import java.util.*
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object LicenseDependencyScanner {
    // scans and loads license data into the extension
    // - from jars on runtime/compile classpath

    // THIS MUST BE IN "afterEvaluate" or run from a specific task.
    fun scanForLicenseData(project: Project, licenses: MutableList<LicenseData>): Triple<MutableList<String>, MutableList<String>, MutableList<String>> {
        val preloadedText = mutableListOf<String>();
        val embeddedText = mutableListOf<String>();
        val missingText = mutableListOf<String>();


        // NOTE: there will be some duplicates, so we want to remove them
        val projectDependencies = (scan(project, "compileClasspath") + scan(project, "runtimeClasspath")).toSet().toList()

        val missingLicenseInfo = mutableSetOf<Dependency>()
        val actuallyMissingLicenseInfo = mutableSetOf<Dependency>()

        if (licenses.isNotEmpty()) {
            // when we scan, we ONLY want to scan a SINGLE LAYER (if we have license info for module ID, then we don't need license info for it's children)
            val primaryLicense = licenses.first()

            // scan to see if we have in our predefined section
            projectDependencies.forEach { info: Dependency ->
                val data: LicenseData? = try {
                    AppLicensing.getLicense(info.mavenId())
                } catch (e: Exception) {
                    println("\tError getting license information for ${info.mavenId()}")
                    null
                }

                if (data == null) {
                    missingLicenseInfo.add(info)
                } else {
                    if (!primaryLicense.extras.contains(data)) {
                        preloadedText.add("\t\t${info.mavenId()} [${data.license}]")

                        // NOTE: the END copyright for these are determined by the DATE of the files!
                        //   Some dates are WRONG (because the jar build is mucked with), so we manually fix it
                        if (data.copyright == 0) {
                            // get the OLDEST date from the artifacts and use that as the copyright date
                            var oldestDate = 0L
                            info.artifacts.forEach { artifact ->
                                // get the date of the manifest file (which is the first entry)
                                ZipInputStream(FileInputStream(artifact.file)).use {
                                    oldestDate = oldestDate.coerceAtLeast(it.nextEntry.lastModifiedTime.toMillis())
                                }
                            }

                            if (oldestDate == 0L) {
                                oldestDate = Instant.now().toEpochMilli()
                            }

                            // as per US Code Title 17, Chapter 4; for "visually perceptive copies" (which includes software).
                            // http://www.copyright.gov/title17/92chap4.html
                            // it is ONLY... © year name
                            val year = Date(oldestDate).toInstant().atZone(ZoneId.systemDefault()).toLocalDate().year
                            data.copyright = year
                        }

                        // otherwise the copyright was specified

                        primaryLicense.extras.add(data)
                    }
                }
            }


            // now scan to see if the jar has a license blob in it
            if (missingLicenseInfo.isNotEmpty()) {
                missingLicenseInfo.forEach { info ->
                    // see if we have it in the dependency jar
                    var licenseData: License? = null
                    info.artifacts.forEach search@{ artifact ->
                        val file = artifact.file
                        try {
                            if (file.canRead()) {
                                ZipFile(file).use {
                                    // read the license blob information
                                    val ze = it.getEntry(LicenseInjector.LICENSE_BLOB)
                                    if (ze != null) {
                                        it.getInputStream(ze).use { licenseStream ->
                                            try {
                                                ObjectInputStream(licenseStream).use { ois ->
                                                    val data = LicenseData("", License.CUSTOM)
                                                    ois.readInt() // weird stuff from serialization. No idea what this value is for, but it is REQUIRED
                                                    data.readObject(ois)
                                                    licenseData = data.license

                                                    // as per US Code Title 17, Chapter 4; for "visually perceptive copies" (which includes software).
                                                    // http://www.copyright.gov/title17/92chap4.html
                                                    // it is ONLY... © year name
                                                    //
                                                    // this is correctly saved in the license blob
                                                    if (!primaryLicense.extras.contains(data)) {
                                                        primaryLicense.extras.add(data)
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                println("\t$info  [ERROR $file], ${e.message ?: e.javaClass}")
                                            }

                                            return@search
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            println("\t$info  [ERROR $file], ${e.message ?: e.javaClass}")
                        }
                    }


                    if (licenseData != null) {
                        embeddedText.add("\t\t$info  [$licenseData]")
                    } else {
                        actuallyMissingLicenseInfo.add(info)
                    }
                }
            }

            if (actuallyMissingLicenseInfo.isNotEmpty()) {
                actuallyMissingLicenseInfo.forEach { missingDep ->
                    missingText.add("\t   ${missingDep.mavenId()}")
                }
            }
        }

        return Triple(preloadedText, embeddedText, missingText)
    }


    /**
     * THIS MUST BE IN "afterEvaluate" or run from a specific task.
     *
     *  NOTE: it is possible, when we have a project DEPEND on an older version of that project (ie: bootstrapped from an older version)
     *    we can have quite deep recursion. A project can never depend on itself, but we check if a project has already been added, and
     *    don't parse it more than once
     *
     *    This is an actual problem...
     */
    private fun scan(project: Project, configurationName: String): List<Dependency> {

        val projectDependencies = mutableListOf<Dependency>()
        val config = project.configurations.getByName(configurationName)
        if (!config.isCanBeResolved) {
            return projectDependencies
        }

        try {
            config.resolve()
        } catch (e: Throwable) {
            println("Unable to resolve the $configurationName configuration for the project ${project.name}")
        }

        val list = LinkedList<ResolvedDependency>()

        config.resolvedConfiguration.lenientConfiguration.getFirstLevelModuleDependencies(org.gradle.api.specs.Specs.SATISFIES_ALL).forEach { dep ->
            list.add(dep)
        }

        var next: ResolvedDependency
        while (list.isNotEmpty()) {
            next = list.poll()

            val module = next.module.id
            val group = module.group
            val name = module.name
            val version = module.version

            val artifacts = try {
                next.moduleArtifacts.map { artifact: ResolvedArtifact ->
                    val artifactModule = artifact.moduleVersion.id
                    Artifact(artifactModule.group, artifactModule.name, artifactModule.version, artifact.file.absoluteFile)
                }
            } catch (e: Exception) {
                listOf()
            }
            projectDependencies.add(Dependency(group, name, version, artifacts))
        }

        return projectDependencies
    }

    internal data class Dependency(val group: String,
                          val name: String,
                          val version: String,
                          val artifacts: List<Artifact>) {

        fun mavenId(): String {
            return "$group:$name:$version"
        }

        override fun toString(): String {
            return mavenId()
        }
    }

    internal data class Artifact(val group: String, val name: String, val version: String, val file: File) {
        val id: String
            get() {
                return "$group:$name:$version"
            }
    }
}
