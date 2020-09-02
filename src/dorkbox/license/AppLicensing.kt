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
data class L(val mavenIdWithInfo: String, val licenseData: LicenseData)

object AppLicensing {
    private const val DEBUG = false

    // have to add the version this license applies from
    private val allLicenseData = mutableMapOf<String, MutableList<Pair<Version, LicenseData>>>()

    // NOTE: the END copyright for these are determined by the DATE of the files!
    //   Some dates are WRONG (because the jar build is mucked with), so we manually fix it
    //
    // Super important! These are dependency projects -- The only requirement (in the most general, permissive way) is that we provide
    // attribution to each. HOWEVER, if there is a library that DOES NOT provide proper/correct attributions to THEMSELVES (meaning, they are
    // a modification to another library and do not credit that library) -- then we are not transversly in violation. THEY are in violation,
    // and not us, since we are correctly attributing their work (they are incorrectly attributing whatever THEY should).
    //
    // We DO NOT have to maintain a FULL HISTORY CHAIN of contributions of all dependent libraries -- only the library + license that we use.
    private val data = listOf(
        L("de.marcphilipp.gradle:nexus-publish-plugin",
          LicenseData("Nexus Publish Plugin", License.APACHE_2).apply {
              description("Gradle Plugin that explicitly creates a Staging Repository before publishing to Nexus.")
              author("Marc Philipp")
              url("https://github.com/marcphilipp/nexus-publish-plugin")
          }
        ),
        L("io.codearte.gradle.nexus:gradle-nexus-staging-plugin",
          LicenseData("Gradle Nexus Staging plugin", License.APACHE_2).apply {
              description("A gradle plugin providing tasks to close and promote/release staged repositories.")
              author("Marcin Zajączkowski")
              author("https://github.com/Codearte/gradle-nexus-staging-plugin/graphs/contributors")
              url("https://github.com/Codearte/gradle-nexus-staging-plugin")
          }),
        L("ch.qos.logback",
          LicenseData("Logback", License.APACHE_2).apply {
              description("Logback is a logging framework for Java applications")
              author("QOS.ch")
              url("http://logback.qos.ch")
          }),
        L("org.jetbrains.kotlin",
          LicenseData("Kotlin", License.APACHE_2).apply {
             author("JetBrains s.r.o. and Kotlin Programming Language contributors")
             url("https://github.com/JetBrains/kotlin")
             note("Kotlin Compiler, Test Data+Libraries, and Tools repository contain third-party code, to which different licenses may apply")
             note("See: https://github.com/JetBrains/kotlin/blob/master/license/README.md")
         }),
        // NOTE: 1.4.0 screwed up the build date (the jar dates are 1980, instead of 2020)
        L("org.jetbrains.kotlin:1.4.0",
          LicenseData("Kotlin", License.APACHE_2).apply {
             copyright = 2020
             author("JetBrains s.r.o. and Kotlin Programming Language contributors")
             url("https://github.com/JetBrains/kotlin")
             note("Kotlin Compiler, Test Data+Libraries, and Tools repository contain third-party code, to which different licenses may apply")
             note("See: https://github.com/JetBrains/kotlin/blob/master/license/README.md")
         }),
        L("org.jetbrains:annotations",
          LicenseData("Java Annotations", License.APACHE_2).apply {
             description("Annotations for JVM-based languages")
             url("https://github.com/JetBrains/java-annotations")
             author("JetBrains s.r.o.")
         }),
        L("org.jetbrains.kotlinx",
          LicenseData("kotlinx.coroutines", License.APACHE_2).apply {
             description("Library support for Kotlin coroutines with multiplatform support")
             url("https://github.com/Kotlin/kotlinx.coroutines")
             author("JetBrains s.r.o.")
         }),
        L("io.github.microutils:kotlin-logging",
          LicenseData("kotlin-logging", License.APACHE_2).apply {
             description("Lightweight logging framework for Kotlin")
             url("https://github.com/MicroUtils/kotlin-logging")
             author("Ohad Shai")
         }),
        L("org.slf4j:slf4j-api",
          LicenseData("SLF4J", License.MIT).apply {
             description("Simple facade or abstraction for various logging frameworks")
             url("http://www.slf4j.org")
             author("QOS.ch")
         }),
        L("net.java.dev.jna:jna:1.0",
          LicenseData("JNA", License.LGPLv2_1).apply {
             description("Simplified native library access for Java.")
             url("https://github.com/twall/jna")
             author("Timothy Wall")
         }),
        L("net.java.dev.jna:jna:4.0",
          LicenseData("JNA", License.APACHE_2).apply {
             description("Simplified native library access for Java.")
             url("https://github.com/twall/jna")
             author("Timothy Wall")
         }),
        L("net.java.dev.jna:jna-platform:1.0",
          LicenseData("JNA-Platform", License.LGPLv2_1).apply {
             description("Mappings for a number of commonly used platform functions")
             url("https://github.com/twall/jna")
             author("Timothy Wall")
         }),
        L("net.java.dev.jna:jna-platform:4.0",
          LicenseData("JNA-Platform", License.APACHE_2).apply {
             description("Mappings for a number of commonly used platform functions")
             url("https://github.com/twall/jna")
             author("Timothy Wall")
         }),
        L("com.hierynomus:sshj",
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
         }),
        L("org.bouncycastle",
          LicenseData("Bouncy Castle Crypto", License.APACHE_2).apply {
             description("Lightweight cryptography API and JCE Extension")
             author("The Legion of the Bouncy Castle Inc")
             url("http://www.bouncycastle.org")
         }),
        L("com.fasterxml.uuid:java-uuid-generator",
          LicenseData("Java Uuid Generator", License.APACHE_2).apply {
             description("A set of Java classes for working with UUIDs")
             author("Tatu Saloranta (tatu.saloranta@iki.fi)")
             author("Contributors. See source release-notes/CREDITS")
             url("https://github.com/cowtowncoder/java-uuid-generator")
         }),
        L("org.tukaani:xz",
          LicenseData("XZ for Java", License.CC0).apply {
             description("Complete implementation of XZ data compression in pure Java")
             author("Lasse Collin")
             author("Igor Pavlov")
             url("https://tukaani.org/xz/java.html")
         }),
        L("io.netty",
          LicenseData("Netty", License.APACHE_2).apply {
             description("An event-driven asynchronous network application framework")
             author("The Netty Project")
             author("Contributors. See source NOTICE")
             url("https://netty.io")
         }),
        L("org.lwjgl:lwjgl-xxhash",
          LicenseData("Lightweight Java Game Library", License.BSD_3).apply {
             description("Java library that enables cross-platform access to popular native APIs")
             author("Lightweight Java Game Library")
             url("https://github.com/LWJGL/lwjgl3")
         }),
        L("com.github.ben-manes:gradle-versions-plugin",
          LicenseData("Gradle Versions Plugin", License.APACHE_2).apply {
             description("This plugin provides a task to determine which dependencies have updates")
             author("Ben Manes")
             url("https://github.com/ben-manes/gradle-versions-plugin")
         }),
        L("org.json:json",
          LicenseData("JSON in Java", License.JSON).apply {
             description("A light-weight language independent data interchange format.")
             author("JSON.org")
             url("https://github.com/stleary/JSON-java")
             url("https://www.json.org/json-en.html")
         }),
        L("com.esotericsoftware:kryo",
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
         }),
        L("de.javakaffee:kryo-serializers",
          LicenseData("Kryo Serializers", License.APACHE_2).apply {
             description("Extra kryo serializers")
             url("https://github.com/magro/kryo-serializers")
             author("Martin Grotzke")
             author("Rafael Winterhalter")
         }),
        // most of the time this is just SWT, but each arch/os has it's own id, so it's dumb to include them all
        L("org.eclipse.platform",
          LicenseData("Eclipse Platform", License.EPL).apply {
             description("Frameworks and common services to support the use of Eclipse and it's tools (SWT)")
             author("The Eclipse Foundation, Inc.")
             url("https://projects.eclipse.org/projects/eclipse.platform")
         }),
        L("net.jpountz.lz4:lz4",
          LicenseData("LZ4 and XXhash", License.APACHE_2).apply {
             description("LZ4 compression for Java, based on Yann Collet's work")
             author("Yann Collet")
             author("Adrien Grand")
             url("https://github.com/jpountz/lz4-java")
             url("http://code.google.com/p/lz4/")
         }),
        L("com.conversantmedia:disruptor",
          LicenseData("Conversant Disruptor", License.APACHE_2).apply {
             description("Disruptor is the highest performing intra-thread transfer mechanism available in Java.")
             author("Conversant, Inc")
             url("https://github.com/conversant/disruptor")
         }),
        L("io.aeron",
          LicenseData("Aeron", License.APACHE_2).apply {
             description("Efficient reliable UDP unicast, UDP multicast, and IPC message transport")
             author("Real Logic Limited")
             url("https://github.com/real-logic/aeron")
         }),
        L("org.agrona:agrona",
          LicenseData("Agrona", License.APACHE_2).apply {
             description("A Library of data structures and utility methods for high-performance applications")
             author("Real Logic Limited")
             url("https://github.com/real-logic/agrona")
         }),
        L("org.javassist:javassist",
          LicenseData("Javassist", License.APACHE_2).apply {
          description("Javassist (JAVA programming ASSISTant) makes Java bytecode manipulation simple")
          author("Shigeru Chiba")
          author("Bill Burke")
          author("Jason T. Greene")
          url("http://www.javassist.org")
          url("https://github.com/jboss-javassist/javassist")
          note("Licensed under the MPL/LGPL/Apache triple license")
         }),
        L("net.jodah:typetools",
          LicenseData("TypeTools", License.APACHE_2).apply {
             description("A simple, zero-dependency library for working with types. Supports Java 1.6+ and Android.")
             author("Jonathan Halterman and friends")
             url("https://github.com/jhalterman/typetools")
         }),
        L("com.github.ben-manes.caffeine:caffeine",
          LicenseData("Caffeine", License.APACHE_2).apply {
             description("Caffeine is a high performance, near optimal caching library based on Java 8.")
             author("Ben Manes")
             url("https://github.com/ben-manes/caffeine")
         }),
    )
    // NOTE: the END copyright for these are determined by the DATE of the files!
    //   Some dates are WRONG (because the jar build is mucked with), so we manually fix it



    init {
        data.forEach {
            buildLicenseData(it)
        }
    }

    // NOTE: generated license information copyright date is based on the DATE of the manifest file in the jar!
    fun getLicense(mavenId: String): LicenseData? {

        if (DEBUG) {
            println("  -  searching $mavenId")
        }

        // the version and moduleId info here are correct!
        var (moduleId, version) = getFromModuleName(mavenId)

        // if we DO NOT have this name, we slowly "chop" is smaller to see if we have a subset of it
        var internalList = allLicenseData[moduleId]
        var offset = 1

        while (internalList == null && offset < 3) {
            // iteratively try using a more and more concise module name
            // this enables us finer granularity to control license information
            val thing = getFromModuleName(mavenId, offset++)
            moduleId = thing.first

            internalList = allLicenseData[moduleId]
        }

        if (DEBUG) {
            println("  -  found list entries")
        }

        internalList?.forEach {
            if (DEBUG) {
                println("  -  checking $version against ${it.first}")
            }

            // if MY version is >= to the version saved in our internal DB, then we use that license
            if (version.greaterThanOrEqualTo(it.first)) {
                if (DEBUG) {
                    println("  -  using $version for ${it.second}")
                }
                return it.second
            }
        }

        return null
    }


    private fun buildLicenseData(license: L) {
        // will always return moduleID ("com.dorkbox") and a version ("1.4", or "0" if not defined)
        val (mavenId, version) = getFromModuleName(license.mavenIdWithInfo)

        // assign the maven ID, so we can use this later, as necessary. This is only for internal use
        license.licenseData.mavenId = mavenId


        val internalList = allLicenseData.getOrPut(mavenId) { mutableListOf() }
        internalList.add(Pair(version, license.licenseData))

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

        if (split.size > 4) {
            throw IllegalDependencyNotation("Supplied String module notation '${fullName}' is invalid. " +
                                            "Example notations: 'com.dorkbox:Version:1.0', 'org.mockito:mockito-core:1.9.5:javadoc'")
        }

        val pair = when ((split.size - override).coerceAtLeast(0)) {
            0 -> {
                throw IllegalDependencyNotation("Supplied String module notation is invalid (it is empty). " +
                                                "Example notations: 'com.dorkbox:Version:1.0', 'org.mockito:mockito-core:1.9.5:javadoc'")
            }
            1 -> {
                Pair(split[0], Version.from(0))
            }
            2 -> {
                val group = split[0]
                val name = split[1]

                // check to see if the name is really a version or a name
                try {
                    Pair(group, Version.from(name))
                } catch (e: Exception) {
                    Pair("$group:$name", Version.from(0))
                }
            }
            else -> {
                val group = split[0]
                val name = split[1]
                val ver = split[2]

                // check to see if the name is really a version or a name
                try {
                    Pair("$group:$name", Version.from(ver))
                } catch (e: Exception) {
                    Pair("$group:$name", Version.from(0))
                }
            }
        }


        if (DEBUG) {
            println("Got: ${pair.first}, ver: ${pair.second} from $fullName")
        }

        return pair
    }
}
