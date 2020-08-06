package dorkbox.license

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.tasks.Internal
import java.io.*
import java.time.Instant
import java.time.ZoneId
import java.util.*
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

class DependencyScanner(private val project: Project, private val extension: Licensing) {
    fun scanForLicenseData() {
        // The default configuration extends from the runtime configuration, which means that it contains all the dependencies and artifacts of the runtime configuration, and potentially more.
        // THIS MUST BE IN "afterEvaluate" or run from a specific task.
        // Using the "runtime" classpath (weirdly) DOES NOT WORK. Only "default" works.

        val projectDependencies = mutableListOf<Dependency>()
        val existingNames = mutableSetOf<String>()

        // we scan BOTH, because we want ALL POSSIBLE DEPENDENCIES for our project.
        project.configurations.getByName("compileClasspath").resolvedConfiguration.firstLevelModuleDependencies.forEach { dep ->
            // we know the FIRST series will exist
            val makeDepTree = makeDepTree(dep, existingNames)
            if (makeDepTree != null) {
                // it's only null if we've ALREADY scanned it
                projectDependencies.add(makeDepTree)
            }
        }
        project.configurations.getByName("runtimeClasspath").resolvedConfiguration.firstLevelModuleDependencies.forEach { dep ->
            // we know the FIRST series will exist
            val makeDepTree = makeDepTree(dep, existingNames)
            if (makeDepTree != null) {
                // it's only null if we've ALREADY scanned it
                projectDependencies.add(makeDepTree)
            }
        }


        val missingLicenseInfo = mutableSetOf<Dependency>()
        val actuallyMissingLicenseInfo = mutableSetOf<Dependency>()

        if (extension.licenses.isNotEmpty()) {
            // when we scan, we ONLY want to scan a SINGLE LAYER (if we have license info for module ID, then we don't need license info for it's children)
            println("\t\tScanning for preloaded license data...")

            val primaryLicense = extension.licenses.first()

            // scan to see if we have in our predefined section
            projectDependencies.forEach { info: Dependency ->
                val license: LicenseData? = try {
                    AppLicensing.getLicense(info.mavenId())
                } catch (e: Exception) {
                    println("Error getting license information for ${info.mavenId()}")
                    e.printStackTrace()
                    null
                }

                if (license == null) {
                    missingLicenseInfo.add(info)
                } else {
                    if (!primaryLicense.extras.contains(license)) {
                        println("\t\t\t${info.mavenId()}")

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
                        license.copyright = year

                        primaryLicense.extras.add(license)
                    }
                }
            }



            println("\t\tScanning for embedded license data...")

            // now scan to see if the jar has a license blob in it
            if (missingLicenseInfo.isNotEmpty()) {
                missingLicenseInfo.forEach { info ->
                    // see if we have it in the dependency jar
                    val output = ByteArrayOutputStream()
                    var missingFound = false
                    info.artifacts.forEach search@{ artifact ->
                        ZipFile(artifact.file).use {
                            try {
                                // read the license blob information
                                val ze = it.getEntry(LicenseInjector.LICENSE_BLOB)
                                if (ze != null) {
                                    it.getInputStream(ze).use { licenseStream ->
                                        licenseStream.copyTo(output)
                                        missingFound = true
                                        return@search
                                    }
                                }
                            }
                            catch (ignored: Exception) {
                            }
                        }
                    }

                    if (!missingFound) {
                        actuallyMissingLicenseInfo.add(info)
                    } else {
                        println("\t\t\t$info")

                        try {
                            ObjectInputStream(ByteArrayInputStream(output.toByteArray())).use { ois ->
                                val size = ois.readInt()
                                for (i in 0 until size) {
                                    val license = LicenseData("", License.CUSTOM)
                                    license.readObject(ois)

                                    // as per US Code Title 17, Chapter 4; for "visually perceptive copies" (which includes software).
                                    // http://www.copyright.gov/title17/92chap4.html
                                    // it is ONLY... © year name
                                    //
                                    // this is correctly saved in the license blob
                                    if (!primaryLicense.extras.contains(license)) {
                                        primaryLicense.extras.add(license)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }

            if (actuallyMissingLicenseInfo.isNotEmpty()) {
                println("\t\tLicense information is missing for the following.  Please submit an issue with this information to include it in future license scans")

                actuallyMissingLicenseInfo.forEach { missingDep ->
                    val flatDependencies = mutableSetOf<Dependency>()
                    missingDep.children.forEach {
                        flattenDep(it, flatDependencies)
                    }

                    val flat = flatDependencies.map { it.mavenId() }
                    val extras = if (flat.isEmpty()) {
                        ""
                    } else {
                        flat.toString()
                    }

                    println("\t\t   ${missingDep.mavenId()} $extras")
                }
            }
        }
    }



    // how to resolve dependencies
    // NOTE: it is possible, when we have a project DEPEND on an older version of that project (ie: bootstrapped from an older version)
    //  we can have infinite recursion.
    //  This is a problem, so we limit how much a dependency can show up the the tree
    private fun makeDepTree(dep: ResolvedDependency, existingNames: MutableSet<String>): Dependency? {
        val module = dep.module.id
        val group = module.group
        val name = module.name
        val version = module.version

        if (!existingNames.contains("$group:$name")) {
            // println("Searching: $group:$name:$version")
            val artifacts: List<DependencyInfo> = dep.moduleArtifacts.map { artifact: ResolvedArtifact ->
                val artifactModule = artifact.moduleVersion.id
                DependencyInfo(artifactModule.group, artifactModule.name, artifactModule.version, artifact.file.absoluteFile)
            }

            val children = mutableListOf<Dependency>()
            dep.children.forEach {
                existingNames.add("$group:$name")
                val makeDep = makeDepTree(it, existingNames)
                if (makeDep != null) {
                    children.add(makeDep)
                }
            }

            return Dependency(group, name, version, artifacts, children.toList())
        }

        // we already have this dependency in our chain.
        return null
    }

    /**
     * Flatten the dependency children
     */
    fun flattenDeps(dep: Dependency): List<Dependency> {
        val flatDeps = mutableSetOf<Dependency>()
        flattenDep(dep, flatDeps)
        return flatDeps.toList()
    }

    private fun flattenDep(dep: Dependency, flatDeps: MutableSet<Dependency>) {
        flatDeps.add(dep)
        dep.children.forEach {
            flattenDep(it, flatDeps)
        }
    }

    data class Dependency(val group: String,
                          val name: String,
                          val version: String,
                          val artifacts: List<DependencyInfo>,
                          val children: List<Dependency>) {

        fun mavenId(): String {
            return "$group:$name:$version"
        }

        override fun toString(): String {
            return mavenId()
        }
    }

    data class DependencyInfo(val group: String, val name: String, val version: String, val file: File) {
        val id: String
            get() {
                return "$group:$name:$version"
            }
    }
}
