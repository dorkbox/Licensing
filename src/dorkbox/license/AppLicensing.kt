package dorkbox.license

import License
import com.dorkbox.version.Version
import org.gradle.api.IllegalDependencyNotation

/**
 * Creates a license chain, based on the mavenId, so other license info can be looked up backed on THAT maven ID
 *
 * For example...
 *  :: "com.dorkbox:Version:1.0", with license data as APACHE_2
 *  :: "com.dorkbox:Version:2.0", with license data as GPL_3
 *      "com.dorkbox:Version:1.0" -> it will return APACHE_2
 *      "com.dorkbox:Version:2.0" -> it will return GPL_3
 *      "com.dorkbox:Version:3.0" -> it will return GPL_3
 *
 *  :: "com.dorkbox:Version", with license data as APACHE_2
 *      "com.dorkbox:Version:1.0" -> it will return APACHE_2
 *      "com.dorkbox:Version:2.0" -> it will return APACHE_2
 *      "com.dorkbox:Version:3.0" -> it will return APACHE_2
 *
 * This will return the Version project license info, because ALL group license info will be collapsed to a single license!! BE CAREFUL!
 *  :: "com.dorkbox", with license data as APACHE_2
 *      "com.dorkbox:Version:1.0" -> it will return APACHE_2
 *      "com.dorkbox:Version:2.0" -> it will return APACHE_2
 *      "com.dorkbox:Console:2.0" -> it will return APACHE_2, AND it will return the Version project license info!! DO NOT DO THIS!
 *
 */
data class LicenseChain(val mavenId: String, val licenseData: LicenseData)

object AppLicensing {
    // have to add the version this license applies from
    private val allLicenseData = mutableMapOf<String, MutableList<Pair<Version, LicenseData>>>()

    // NOTE: the END copyright for these are determined by the DATE of the files!
    //
    // Super important! These are dependency projects -- The only requirement (in the most general, permissive way) is that we provide
    // attribution to each. HOWEVER, if there is a library that DOES NOT provide proper/correct attributions to THEMSELVES (meaning, they are
    // a modification to another library and do not credit that library) -- then we are not transversly in violation. THEY are in violation,
    // and not us, since we are correctly attributing their work (they are incorrectly attributing whatever THEY should).
    //
    // We DO NOT have to maintain a FULL HISTORY CHAIN of contributions of all dependent libraries -- only the library + license that we use.
    private val map = listOf(
            LicenseChain("org.jetbrains.kotlin",
                         LicenseData("Kotlin", License.APACHE_2).apply {
                             author("JetBrains s.r.o. and Kotlin Programming Language contributors")
                             url("https://github.com/JetBrains/kotlin")
                             note("Kotlin Compiler, Test Data+Libraries, and Tools repository contain third-party code, to which different licenses may apply")
                             note("See: https://github.com/JetBrains/kotlin/blob/master/license/README.md")
                         }
            ),
            LicenseChain("org.jetbrains:annotations",
                         LicenseData("Java Annotations", License.APACHE_2).apply {
                             description("Annotations for JVM-based languages")
                             url("https://github.com/JetBrains/java-annotations")
                             author("JetBrains s.r.o.")
                         }
            ),
            LicenseChain("org.jetbrains.kotlinx",
                         LicenseData("kotlinx.coroutines", License.APACHE_2).apply {
                             description("Library support for Kotlin coroutines with multiplatform support")
                             url("https://github.com/Kotlin/kotlinx.coroutines")
                             author("JetBrains s.r.o.")
                         }
            ),
            LicenseChain("io.github.microutils:kotlin-logging",
                         LicenseData("kotlin-logging", License.APACHE_2).apply {
                             description("Lightweight logging framework for Kotlin")
                             url("https://github.com/MicroUtils/kotlin-logging")
                             author("Ohad Shai")
                         }
            ),
            LicenseChain("org.slf4j:slf4j-api",
                         LicenseData("SLF4J", License.MIT).apply {
                             description("Simple facade or abstraction for various logging frameworks")
                             url("http://www.slf4j.org")
                             author("QOS.ch")
                         }
            ),
            LicenseChain("net.java.dev.jna:jna:1.0",
                         LicenseData("JNA", License.LGPLv2_1).apply {
                             description("Simplified native library access for Java.")
                             url("https://github.com/twall/jna")
                             author("Timothy Wall")
                         }
            ),
            LicenseChain("net.java.dev.jna:jna:4.0",
                         LicenseData("JNA", License.APACHE_2).apply {
                             description("Simplified native library access for Java.")
                             url("https://github.com/twall/jna")
                             author("Timothy Wall")
                         }
            ),

            LicenseChain("net.java.dev.jna:jna-platform:1.0",
                         LicenseData("JNA-Platform", License.LGPLv2_1).apply {
                             description("Mappings for a number of commonly used platform functions")
                             url("https://github.com/twall/jna")
                             author("Timothy Wall")
                         }
            ),
            LicenseChain("net.java.dev.jna:jna-platform:4.0",
                         LicenseData("JNA-Platform", License.APACHE_2).apply {
                             description("Mappings for a number of commonly used platform functions")
                             url("https://github.com/twall/jna")
                             author("Timothy Wall")
                         }
            ),

            LicenseChain("com.hierynomus:sshj",
                         LicenseData("SSHJ", License.APACHE_2).apply {
                             description("SSHv2 library for Java")
                             url("https://github.com/hierynomus/sshj")
                             author("Jeroen van Erp")
                             author("SSHJ Contributors")

                             extra("Apache MINA", License.APACHE_2) {
                                 it.url("https://mina.apache.org/sshd-project/")
                                 it.author("The Apache Software Foundation")
                             }
                             extra("Apache Commons-Net", License.APACHE_2) {
                                  it.url("https://commons.apache.org/proper/commons-net/")
                                  it.author("The Apache Software Foundation")
                              }
                             extra("JZlib", License.APACHE_2) {
                                  it.url("http://www.jcraft.com/jzlib")
                                  it.author("Atsuhiko Yamanaka")
                                  it.author("JCraft, Inc.")
                              }
                             extra("Bouncy Castle Crypto", License.APACHE_2) {
                                  it.url("http://www.bouncycastle.org")
                                  it.author("The Legion of the Bouncy Castle Inc")
                              }
                             extra("ed25519-java", License.CC0) {
                                  it.url("https://github.com/str4d/ed25519-java")
                                  it.author("https://github.com/str4d")
                             }
                         }
            ),

            LicenseChain("org.bouncycastle",
                         LicenseData("Bouncy Castle Crypto", License.APACHE_2).apply {
                             description("Lightweight cryptography API and JCE Extension")
                             author("The Legion of the Bouncy Castle Inc")
                             url("http://www.bouncycastle.org")
                         }
            ),

            LicenseChain("com.fasterxml.uuid:java-uuid-generator",
                         LicenseData("Java Uuid Generator", License.APACHE_2).apply {
                             description("A set of Java classes for working with UUIDs")
                             author("Tatu Saloranta (tatu.saloranta@iki.fi)")
                             author("Contributors. See source release-notes/CREDITS")
                             url("https://github.com/cowtowncoder/java-uuid-generator")
                         }
            ),
            LicenseChain("org.tukaani:xz",
                         LicenseData("XZ for Java", License.CC0).apply {
                             description("Complete implementation of XZ data compression in pure Java")
                             author("Lasse Collin")
                             author("Igor Pavlov")
                             url("https://tukaani.org/xz/java.html")
                         }
            ),
            LicenseChain("io.netty",
                         LicenseData("Netty", License.APACHE_2).apply {
                             description("An event-driven asynchronous network application framework")
                             author("The Netty Project")
                             author("Contributors. See source NOTICE")
                             url("https://netty.io")
                         }
            ),
            LicenseChain("org.lwjgl:lwjgl-xxhash",
                         LicenseData("Lightweight Java Game Library", License.BSD_3).apply {
                             description("Java library that enables cross-platform access to popular native APIs")
                             author("Lightweight Java Game Library")
                             url("https://github.com/LWJGL/lwjgl3")
                         }
            ),

            LicenseChain("com.github.ben-manes:gradle-versions-plugin",
                         LicenseData("Gradle Versions Plugin", License.APACHE_2).apply {
                             description("This plugin provides a task to determine which dependencies have updates")
                             author("Ben Manes")
                             url("https://github.com/ben-manes/gradle-versions-plugin")
                         }
            ),

            LicenseChain("org.json:json",
                         LicenseData("JSON in Java", License.JSON).apply {
                             description("A light-weight language independent data interchange format.")
                             author("JSON.org")
                             url("https://github.com/stleary/JSON-java")
                             url("https://www.json.org/json-en.html")
                         }
            ),

            LicenseChain("com.esotericsoftware:kryo",
                         LicenseData("Kryo", License.BSD_3).apply {
                             description("Fast and efficient binary object graph serialization framework for Java")
                             author("Nathan Sweet")
                             url("https://github.com/EsotericSoftware/kryo")

                             extra("ReflectASM", License.BSD_3) {
                                 it.url("https://github.com/EsotericSoftware/reflectasm")
                                 it.author("Nathan Sweet")
                             }
                             extra("Objenesis ", License.APACHE_2) {
                                 it.url("http://objenesis.org")
                                 it.author("Objenesis Team and all contributors")
                             }
                             extra("MinLog-SLF4J", License.BSD_3) {
                                 it.url("https://github.com/EsotericSoftware/minlog")
                                 it.author("Nathan Sweet")
                             }
                         }
            ),

            LicenseChain("de.javakaffee:kryo-serializers",
                         LicenseData("Kryo Serializers", License.APACHE_2).apply {
                             description("Extra kryo serializers")
                             url("https://github.com/magro/kryo-serializers")
                             author("Martin Grotzke")
                             author("Rafael Winterhalter")
                         }
            ),

            // most of the time this is just SWT, but each arch/os has it's own id, so it's dumb to include them all
            LicenseChain("org.eclipse.platform",
                         LicenseData("Eclipse Platform", License.EPL).apply {
                             description("Frameworks and common services to support the use of Eclipse and it's tools (SWT)")
                             author("The Eclipse Foundation, Inc.")
                             url("https://projects.eclipse.org/projects/eclipse.platform")
                         }
            ),


            LicenseChain("net.jodah:typetools",
                         LicenseData("TypeTools", License.APACHE_2).apply {
                             description("A simple, zero-dependency library for working with types. Supports Java 1.6+ and Android.")
                             author("Jonathan Halterman and friends")
                             url("https://github.com/jhalterman/typetools")
                         }
            )
    )
    // NOTE: the END copyright for these are determined by the DATE of the files!



    init {
        map.forEach {
            license(it)
        }
    }

    // NOTE: generated license information copyright date is based on the DATE of the manifest file in the jar!
    fun getLicense(name: String): LicenseData? {
        var (moduleId, version) = getFromModuleName(name)

        var internalList = allLicenseData[moduleId]
        var offset = 1
        while (internalList == null && offset < 3) {
            // try using simpler module info (since we can specify more generic license info)
            val thing = getFromModuleName(name, offset++)
            moduleId = thing.first
            version = thing.second


            internalList = allLicenseData[moduleId]
        }

//        println("  -  found list entries")
        internalList?.forEach {
//            println("  -  checking $version against ${it.first}")
            // if MY version is >= to the version saved in our internal DB, then we use that license
            if (version.greaterThanOrEqualTo(it.first)) {
                return it.second
            }
        }

        return null
    }


    private fun license(licenseChain: LicenseChain) {
        val (moduleId, version) = getFromModuleName(licenseChain.mavenId)

        val internalList = allLicenseData.getOrPut(moduleId) { mutableListOf() }
        internalList.add(Pair(version, licenseChain.licenseData))

        // largest version number is first, smallest version number is last.
        // when checking WHAT license applies to WHICH version, we start at the largest (so we stop looking at the first match <= to us)
        // this is because if X.Y=MIT, then X.Y+1=MIT and X+1.Y+1=MIT
        // a real-world example is JNA. JNA v3 -> GPL, JNA v4 -> APACHE

        // to demonstrate:
        //   v1 -> GPL
        //   v4 -> MIT
        //   v8 -> APACHE

        // We are version 6, so we are MIT
        // We are version 12, so we are APACHE
        // We are version 2, so we are GPL
        internalList.sortByDescending { it.first.toString() }
    }

    private fun getFromModuleName(fullName: String, override: Int = 0) : Pair<String, Version> {
        val split = fullName.split(':')

        val moduleId = when ((split.size - override).coerceAtLeast(0)) {
            0 -> {
                fullName
            }
            1 -> {
                split[0]
            }
            else -> {
                val group = split[0]
                val name = split[1]
                "$group:$name"
            }
        }

        val version = when (split.size) {
            3 -> Version.from(split[2])
            else -> Version.from(0)
        }


        if (split.size > 4) {
            throw IllegalDependencyNotation("Supplied String module notation '${moduleId}' is invalid. " +
                                            "Example notations: 'com.dorkbox:Version:1.0', 'org.mockito:mockito-core:1.9.5:javadoc'")
        }

//        println("Got: $moduleId, $version from $fullName")
        return Pair(moduleId, version)
    }
}
