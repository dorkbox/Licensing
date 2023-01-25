/*
 * Copyright 2023 dorkbox, llc
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
import dorkbox.version.Version
import org.gradle.api.IllegalDependencyNotation

/**
 * Creates a license chain, based on the mavenId, so other license info can be looked up backed on THAT maven ID
 *
 * For example...
 *  :: "com.dorkbox:Version:1.0", with license data as APACHE_2
 *  :: "com.dorkbox:Version:2.0", with license data as GPL_3
 *
 *  query:    "com.dorkbox:Version:1.0" -> it will return APACHE_2
 *  query:    "com.dorkbox:Version:2.0" -> it will return GPL_3
 *  query:    "com.dorkbox:Version:3.0" -> it will return GPL_3
 *
 *
 *  :: "com.dorkbox:Version", with license data as APACHE_2
 *
 *  query:    "com.dorkbox:Version:1.0" -> it will return APACHE_2
 *  query:    "com.dorkbox:Version:2.0" -> it will return APACHE_2
 *  query:    "com.dorkbox:Version:3.0" -> it will return APACHE_2
 *
 *
 *  :: "com.dorkbox", with license data as APACHE_2
 *
 *  query:    "com.dorkbox:Version:1.0" -> it will return APACHE_2
 *  query:    "com.dorkbox:Version:2.0" -> it will return APACHE_2
 *  query:    "com.dorkbox:Console:1.0" -> it will return APACHE_2
 *  query:    "com.dorkbox:Console:2.0" -> it will return APACHE_2
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
        // NOTE: 1.4.0 screwed up the build date (the jar dates are 1980, instead of 2020)
        L("org.jetbrains.kotlin:1.4.0",
          LicenseData("Kotlin", License.APACHE_2).apply {
              copyright = 2020
              author("JetBrains s.r.o. and Kotlin Programming Language contributors")
              url("https://github.com/JetBrains/kotlin")
              note("Kotlin Compiler, Test Data+Libraries, and Tools repository contain third-party code, to which different licenses may apply")
              note("See: https://github.com/JetBrains/kotlin/blob/master/license/README.md")
          }),
        L("org.jetbrains.kotlin",
          LicenseData("Kotlin", License.APACHE_2).apply {
             author("JetBrains s.r.o. and Kotlin Programming Language contributors")
             url("https://github.com/JetBrains/kotlin")
             note("Kotlin Compiler, Test Data+Libraries, and Tools repository contain third-party code, to which different licenses may apply")
             note("See: https://github.com/JetBrains/kotlin/blob/master/license/README.md")
         }),
        L("org.jetbrains.kotlinx",
          LicenseData("kotlinx.coroutines", License.APACHE_2).apply {
             description("Library support for Kotlin coroutines with multiplatform support")
             url("https://github.com/Kotlin/kotlinx.coroutines")
             author("JetBrains s.r.o.")
         }),
        L("org.jetbrains:annotations",
          LicenseData("Java Annotations", License.APACHE_2).apply {
              description("Annotations for JVM-based languages")
              url("https://github.com/JetBrains/java-annotations")
              author("JetBrains s.r.o.")
          }),
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
        L("org.slf4j:jcl-over-slf4j",
          LicenseData("JCL to SLF4J", License.MIT).apply {
             description("JCL 1.2 implemented over SLF4J")
             url("http://www.slf4j.org")
             author("QOS.ch")
         }),
        L("org.slf4j:jul-to-slf4j",
          LicenseData("JUL to SLF4J", License.MIT).apply {
             description("Java Util Logging implemented over SLF4J")
             url("http://www.slf4j.org")
             author("QOS.ch")
         }),
        L("org.slf4j:log4j-over-slf4j",
          LicenseData("Log4j to SLF4J", License.MIT).apply {
             description("Log4j implemented over SLF4J")
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
        L("net.java.dev.jna:jna-jpms:5.8",
          LicenseData("JNA", License.APACHE_2).apply {
              description("Simplified native library access for Java.")
              url("https://github.com/twall/jna")
              author("Timothy Wall")
          }),
        L("net.java.dev.jna:jna-platform-jpms:5.8",
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
                 url("https://mina.apache.org/sshd-project/")
                 author("The Apache Software Foundation")
             }
             extra("Apache Commons-Net", License.APACHE_2) {
                 url("https://commons.apache.org/proper/commons-net/")
                 author("The Apache Software Foundation")
              }
             extra("JZlib", License.APACHE_2) {
                 url("http://www.jcraft.com/jzlib")
                 author("Atsuhiko Yamanaka")
                 author("JCraft, Inc.")
              }
             extra("Bouncy Castle Crypto", License.APACHE_2) {
                 url("http://www.bouncycastle.org")
                 author("The Legion of the Bouncy Castle Inc")
              }
             extra("ed25519-java", License.CC0) {
                 url("https://github.com/str4d/ed25519-java")
                 author("https://github.com/str4d")
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
                 url("https://github.com/EsotericSoftware/reflectasm")
                 author("Nathan Sweet")
             }
             extra("Objenesis", License.APACHE_2) {
                 url("http://objenesis.org")
                 author("Objenesis Team and all contributors")
             }
             extra("MinLog-SLF4J", License.BSD_3) {
                 url("https://github.com/EsotericSoftware/minlog")
                 author("Nathan Sweet")
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
        L("org.ow2.asm",
          LicenseData("ASM", License.APACHE_2).apply {
             description("ASM: a very small and fast Java bytecode manipulation framework")
             author("INRIA, France Telecom")
             url("https://asm.ow2.io/")
         }),
        L("com.github.jnr",
          LicenseData("The Java Native Runtime Project", License.APACHE_2).apply {
              description("Java Native code interactions for easier JNI usage")
              author("Wayne Meissner and contributors")
              url("https://github.com/jnr")
          }),
        L("org.lmdbjava:lmdbjava",
          LicenseData("LMDB for Java", License.APACHE_2).apply {
             description("LMDB is an ordered, embedded, durable, key-value store which uses B+ Trees.")
             author("The LmdbJava Open Source Project")
             url("https://github.com/lmdbjava/lmdbjava")

             extra("LMDB", License.OLDAP) {
                 url("https://symas.com/lmdb/")
                 author("Symas Corporation")
             }
         }),
        L("com.intellij:annotations",
          LicenseData("IntelliJ IDEA Annotations", License.APACHE_2).apply {
              description("A set of annotations used for code inspection support and code documentation. ")
              author("JetBrains s.r.o.")
              url("https://github.com/JetBrains/java-annotations")
          }),
        L("org.jetbrains:annotations",
          LicenseData("Annotations for JVM-based languages", License.APACHE_2).apply {
              description("A set of Java annotations which can be used in JVM-based languages.")
              author("JetBrains s.r.o.")
              url("https://github.com/JetBrains/java-annotations")
          }),
        L("net.openhft:chronicle-map",
          LicenseData("Chronicle Map", License.APACHE_2).apply {
             description("Chronicle Map is a super-fast, in-memory, non-blocking, key-value store")
             author("Chronicle Map Contributors")
             url("https://github.com/OpenHFT/Chronicle-Map")
         }),
        L("net.openhft:chronicle-core",
          LicenseData("Chronicle Core", License.APACHE_2).apply {
             description("Library that wraps up low level access")
             author("Chronicle Software")
             url("https://github.com/OpenHFT/Chronicle-Core")
         }),
        L("net.openhft:chronicle-values",
          LicenseData("Chronicle Values", License.APACHE_2).apply {
             description("Poor man's value types, Java 8+")
             author("Chronicle Software")
             url("https://github.com/OpenHFT/Chronicle-Values")
         }),
        L("net.openhft:chronicle-bytes",
          LicenseData("Chronicle Bytes", License.APACHE_2).apply {
             description("Chronicle Bytes contains all the low level memory access wrappers")
             author("Chronicle Software")
             url("https://github.com/OpenHFT/Chronicle-Bytes")
         }),
        L("net.openhft:chronicle-threads",
          LicenseData("Chronicle Threads", License.APACHE_2).apply {
             description("This library provide a high performance thread pool.")
             author("Chronicle Software")
             url("https://github.com/OpenHFT/Chronicle-Threads")
         }),
        L("net.openhft:affinity",
          LicenseData("Thread Affinity", License.APACHE_2).apply {
             description("Lets you bind a thread to a given core")
             author("Chronicle Software")
             url("https://github.com/OpenHFT/Java-Thread-Affinity")
         }),
        L("net.openhft:chronicle-wire",
          LicenseData("Chronicle-Wire", License.APACHE_2).apply {
             description("Wire Format abstraction library")
             author("Chronicle Software")
             url("https://github.com/OpenHFT/Chronicle-Wire")
         }),
        L("net.openhft:compiler",
          LicenseData("Java-Runtime-Compiler", License.APACHE_2).apply {
             description("This takes a String, compiles it and loads it returning you a class")
             author("Chronicle Software")
             url("https://github.com/OpenHFT/Java-Runtime-Compiler")
         }),
        L("net.openhft:chronicle-algorithms",
          LicenseData("Chronicle-Algorithms", License.APACHE_2).apply {
             description("Zero allocation, efficient algorithms")
             author("Chronicle Software")
             url("https://github.com/OpenHFT/Chronicle-Algorithms")
         }),
        L("com.squareup:javapoet",
          LicenseData("JavaPoet", License.APACHE_2).apply {
             description("JavaPoet is a Java API for generating .java source files.")
             author("Square, Inc.")
             url("https://github.com/square/javapoet")
         }),
        L("com.thoughtworks.xstream:xstream",
          LicenseData("XStream", License.BSD_3).apply {
             description("XStream is a simple library to serialize objects to XML and back again.")
             author("Joe Walnes")
             author("XStream Committers")
             url("http://x-stream.github.io/")
         }),
        L("xmlpull:xmlpull",
          LicenseData("XML Pull Parsing API", License.CC0).apply {
             description("XML Pull Parsing API ")
         }),
        L("xpp3:xpp3_min",
          LicenseData("MXP1: Xml Pull Parser 3rd Edition (XPP3)", License.CC0).apply {
             description("XmlPull parsing engine")
             url("https://mvnrepository.com/artifact/xpp3/xpp3_min")
         }),
        L("org.codehaus.jettison:jettison",
          LicenseData("Jettison ", License.APACHE_2).apply {
             description("Jettison is a Java library for converting XML to JSON")
             author("Envoi Solutions LLC")
             url("https://github.com/jettison-json/jettison")
         }),
        L("org.ops4j.pax.url",
          LicenseData("Pax URL", License.APACHE_2).apply {
             description("Custom URL stream handlers for OSGi")
             author("OPS4J Contributors")
             url("https://github.com/ops4j/org.ops4j.pax.url")
         }),
        L("org.apache.maven.resolver",
          LicenseData("Apache Maven Artifact Resolver", License.APACHE_2).apply {
             description("A library for working with artifact repositories and dependency resolution")
             author("The Apache Software Foundation")
             url("https://github.com/apache/maven-resolver")
         }),
        L("org.openjfx",
          LicenseData("OpenJFX", License.GPLv2_CLASSPATH).apply {
             description("OpenJFX client application platform for desktop, mobile and embedded systems")
             author("Oracle and/or its affiliates")
             url("https://github.com/openjdk/jfx")
         }),
        L("com.squareup.moshi",
          LicenseData("Moshi", License.APACHE_2).apply {
             description("A modern JSON library for Kotlin and Java")
             author("Square, Inc")
             url("https://github.com/square/moshi")
         }),
        L("com.squareup.okio",
          LicenseData("OkIO", License.APACHE_2).apply {
             description("A modern I/O library for Android, Kotlin, and Java")
             author("Square, Inc")
             url("https://github.com/square/okio")
         }),
        L("com.squareup.okhttp3",
          LicenseData("OkHttp", License.APACHE_2).apply {
             description("Square’s meticulous HTTP client for the JVM, Android, and GraalVM")
             author("Square, Inc")
             url("https://github.com/square/okhttp")
         }),
        L("com.squareup.retrofit2",
          LicenseData("Retrofit", License.APACHE_2).apply {
             description("A type-safe HTTP client for Android and the JVM")
             author("Square, Inc")
             url("https://github.com/square/retrofit")
         }),
        L("net.sf.trove4j",
          LicenseData("Trove4J Collections", License.LGPLv2_1).apply {
              description("Fast, lightweight implementations of the Java Collections API")
              author("Eric D. Friedman")
              url("http://trove4j.sourceforge.net")

              extra("HashFunctions", License.CC0) {
                  copyright(1999)
                  url("https://mina.apache.org/sshd-project/")
                  author("CERN")
              }
              extra("PrimeFinder", License.CC0) {
                  copyright(1999)
                  url("https://mina.apache.org/sshd-project/")
                  author("CERN")
              }
          }),
        L("com.koloboke",
          LicenseData("Koloboke Collections", License.APACHE_2).apply {
              description("Java Collections till the last breadcrumb of memory and performance ")
              author("Roman Leventov")
              author("Peter K Lawrey")
              author("Brent Douglas")
              url("https://github.com/leventov/Koloboke")
          }),
        L("javatar:javatar",
          LicenseData("Java Tar", License.APACHE_2).apply {
              description("Java TAR compression utility library")
              author("Tim")
              author("ICE Engineering, Inc.")
              url("http://www.trustice.com/java/tar")
          }),
        L("org.flywaydb:flyway-core",
          LicenseData("Flyway Tar", License.APACHE_2).apply {
              description("Flyway database migrations made easy")
              author("Boxfuse GmbH")
              author("Red Gate Software Ltd")
              url("https://github.com/flyway/flyway")
          }),
        L("org.jsoup:jsoup",
          LicenseData("jSoup", License.MIT).apply {
              description("jsoup: Java HTML Parser")
              author("Jonathan Hedley")
              url("https://github.com/jhy/jsoup")
          }),
        L("org.simplejavamail:simple-java-mail",
          LicenseData("Simple Java Mail", License.APACHE_2).apply {
              description("Simple API, Complex Emails (JavaMail SMTP wrapper)")
              author("Benny Bottema")
              url("https://github.com/bbottema/simple-java-mail")
          }),
        L("commons-net:commons-net",
          LicenseData("Apache Commons Net", License.APACHE_2).apply {
              description("A collection of network utilities and protocol implementations")
              author("The Apache Software Foundation")
              url("https://github.com/apache/commons-net")
          }),
        L("org.apache.commons:commons-io",
          LicenseData("Apache Commons IO ", License.APACHE_2).apply {
              author("The Apache Software Foundation")
              url("https://github.com/apache/commons-io")
          }),
        L("org.apache.commons:commons-configuration2",
          LicenseData("Apache Commons Configuration ", License.APACHE_2).apply {
              description("Tools to assist in the reading of configuration/preferences files in various formats")
              author("The Apache Software Foundation")
              url("https://github.com/apache/commons-io")
          }),
        L("org.apache.commons:commons-compress",
          LicenseData("Apache Commons Compress ", License.APACHE_2).apply {
              description("The Apache Commons Compress library defines an API for working with ar, cpio, Unix dump, tar, zip, gzip, XZ, Pack200, bzip2, 7z, arj, lzma, snappy, DEFLATE, lz4, Brotli, Zstandard, DEFLATE64 and Z files.")
              author("The Apache Software Foundation")
              url("https://github.com/apache/commons-compress")
          }),
        L("io.undertow",
          LicenseData("Undertow", License.APACHE_2).apply {
              description("High performance non-blocking webserver")
              author("JBoss")
              author("Red Hat, Inc.")
              author("Individual contributors as listed in files")
              url("https://github.com/undertow-io/undertow")
          }),
        L("com.amazonaws",
          LicenseData("AWS SDK", License.APACHE_2).apply {
              description("The official AWS SDK for Java")
              author("Amazon.com, Inc")
              url("https://github.com/aws/aws-sdk-java")
          }),
        L("io.ktor",
          LicenseData("KTOR", License.APACHE_2).apply {
              description("Framework for quickly creating connected applications in Kotlin")
              author("JetBrains s.r.o.")
              url("https://github.com/ktorio/ktor")
          }),
        L("io.dropwizard.metrics",
          LicenseData("Dropwizard", License.APACHE_2).apply {
              description("Captures JVM and application metrics")
              author("Coda Hale")
              author("Yammer.com")
              author("Dropwizard")
              url("https://github.com/dropwizard/metrics")
          }),
        L("com.vaadin:vaadin",
          LicenseData("Vaadin", License.APACHE_2).apply {
              description("An open platform for building modern web apps for Java back ends")
              author("Vaadin Ltd.")
              url("https://github.com/vaadin/")
          }),
        L("com.vaadin:flow-client",
          LicenseData("Vaadin", License.APACHE_2).apply {
              description("An open platform for building modern web apps for Java back ends")
              author("Vaadin Ltd.")
              url("https://github.com/vaadin/")
          }),
        L("com.vaadin:flow-server",
          LicenseData("Vaadin", License.APACHE_2).apply {
              description("An open platform for building modern web apps for Java back ends")
              author("Vaadin Ltd.")
              url("https://github.com/vaadin/")
          }),
        L("com.vaadin.componentfactory",
          LicenseData("Vaadin Components", License.APACHE_2).apply {
              description("Components for Vaadin")
              author("Vaadin Ltd.")
              url("https://github.com/vaadin/")
          }),
        L("com.vaadin:vaadin-charts-flow",
          LicenseData("Vaadin Components", License.COMMERCIAL).apply {
              description("A feature-rich interactive charting library for Vaadin.")
              author("Vaadin Ltd.")
              url("https://github.com/vaadin/")
          }),
        L("com.github.appreciated:app-layout-addon",
          LicenseData("App Layout Add-on", License.APACHE_2).apply {
              description("A modern and highly customizable Menu with a fluent API")
              author("Johannes Goebel")
              url("https://vaadin.com/directory/component/app-layout-add-on")
          }),
        L("com.github.appreciated:color-picker-field-flow",
          LicenseData("Color Picker Field for Flow", License.APACHE_2).apply {
              description("A color picker field for Vaadin Flow")
              author("Johannes Goebel")
              url("https://vaadin.com/directory/component/color-picker-field-for-flow")
          }),
        L("org.vaadin.erik:slidetab",
          LicenseData("SlideTab", License.APACHE_2).apply {
              description("A tab that can be clicked to slide in an expanding panel")
              author("Erik Lumme")
              url("https://vaadin.com/directory/component/slidetab/overview")
          }),
        L("com.flowingcode.addons:iron-icons",
          LicenseData("Iron Icons", License.APACHE_2).apply {
              description("Vaadin constants for https://github.com/PolymerElements/iron-icons")
              author("Flowing Code S.A.")
              url("https://vaadin.com/directory/component/iron-icons/overview")
          }),
        L("com.flowingcode.addons:font-awesome-iron-iconset",
          LicenseData("FontAwesome Iron Iconset", License.APACHE_2).apply {
              description("Iron iconset based on FontAwesome")
              author("Flowing Code S.A.")
              url("https://vaadin.com/directory/component/fontawesome-iron-iconset")
          }),
        L("com.flowingcode.vaadin.addons:twincolgrid",
          LicenseData("TwinColGrid add-on", License.APACHE_2).apply {
              description("TwinColSelect component based on Vaadin Grids")
              author("Flowing Code S.A.")
              url("https://vaadin.com/directory/component/twincolgrid-add-on/overview")
          }),
        L("org.vaadin:spinkit",
          LicenseData("Spinkit Add-on", License.APACHE_2).apply {
              description("Vaadin Spinkit")
              author("Marco Collovati")
              url("https://vaadin.com/directory/component/spinkit-add-on/overview")
          }),
        L("org.vaadin.stefan:lazy-download-button",
          LicenseData("Lazy Download Button", License.APACHE_2).apply {
              description("Lazy Download Button")
              author("Stefan Uebe")
              url("https://vaadin.com/directory/component/lazy-download-button/overview")
          }),
        L("org.vaadin.olli:file-download-wrapper",
          LicenseData("File Download Wrapper", License.APACHE_2).apply {
              description("Helper add-on for making easy clickable file downloads.")
              author("Olli Tietäväinen")
              url("https://vaadin.com/directory/component/file-download-wrapper/overview")
          }),
        L("org.vaadin.gatanaso:multiselect-combo-box-flow",
          LicenseData("Multiselect Combo Box", License.APACHE_2).apply {
              description("A multiselection component where items are displayed in a drop-down list.")
              author("Goran Atanasovski")
              url("https://vaadin.com/directory/component/multiselect-combo-box/overview")
          }),
        L("org.vaadin.haijian:exporter",
          LicenseData("Exporter", License.APACHE_2).apply {
              description("A simple tool for exporting data from Grid to Excel or CSV")
              author("Haijian Wang")
              url("https://vaadin.com/directory/component/exporter/overview")
          }),
        L("com.mlottmann.VStepper:VStepper",
          LicenseData("VStepper", License.APACHE_2).apply {
              description("Vaadin addon for displaying a series of components one at a time.")
              author("Matthias Lottmann")
              url("https://vaadin.com/directory/component/messagedialog-for-vaadin-flow/overview")
          }),
        L("de.codecamp.vaadin:vaadin-message-dialog",
          LicenseData("Message Dialog", License.APACHE_2).apply {
              description("Message dialog component for Vaadin Flow")
              author("Patrick Schmidt")
              url("https://vaadin.com/directory/component/vstepper")
          }),
        L("dev.mett.vaadin:tooltip",
          LicenseData("Tooltips4Vaadin", License.APACHE_2).apply {
              description("A Tippy.js based Tooltip-Plugin for Vaadin Flow")
              author("Gerrit Sedlaczek")
              url("https://vaadin.com/directory/component/tooltips4vaadin/overview")
          }),
        L("io.github.classgraph:classgraph",
          LicenseData("ClassGraph", License.APACHE_2).apply {
              description("An uber-fast parallelized Java classpath scanner and module scanner")
              author("Luke Hutchison")
              url("https://github.com/classgraph/classgraph")
          }),
        L("com.opencsv:opencsv",
          LicenseData("OpenCSV", License.APACHE_2).apply {
              description("CSV Parser for Java")
              author("Scott Conway")
              author("Andrew Rucker Jones")
              author("Tom Squires")
              url("http://opencsv.sourceforge.net/")
          }),
        L("com.zaxxer:HikariCP",
          LicenseData("HikariCP", License.APACHE_2).apply {
              description("A solid, high-performance, JDBC connection pool")
              author("Brett Wooldridge")
              url("https://github.com/brettwooldridge/HikariCP/")
          }),
        L("org.apache.httpcomponents:httpclient",
          LicenseData("Apache HttpClient", License.APACHE_2).apply {
              description("Apache HttpClient")
              author("The Apache Software Foundation")
              url("https://github.com/apache/httpcomponents-client")
          }),
        L("oauth.signpost",
          LicenseData("Signpost", License.APACHE_2).apply {
              description("A light-weight client-side OAuth library for Java ")
              author("Matthias Kaeppler")
              url("https://github.com/mttkay/signpost")
          }),
        L("org.kurento",
          LicenseData("Kurento", License.APACHE_2).apply {
              description("Kurento WebRTC Media Server")
              author("Kurento")
              url("https://www.kurento.org")
              url("https://github.com/Kurento")
          }),
        L("org.postgresql:postgresql",
          LicenseData("Postgresql", License.BSD_2).apply {
              description("Postgresql JDBC Driver")
              author("PostgreSQL Global Development Group")
              url("https://jdbc.postgresql.org ")
              url("https://github.com/pgjdbc/pgjdbc")
          }),
        L("org.jclarion:image4j",
          LicenseData("image4j", License.LGPLv2_1).apply {
              description("Read and write certain image formats in 100% pure Java")
              author("Ian McDonagh")
              url("https://github.com/imcdonagh/image4j")
          }),
        L("com.github.scribejava",
          LicenseData("ScribeJava", License.MIT).apply {
              description("Simple OAuth/2 library for Java")
              author("Pablo Fernandez")
              url("https://github.com/imcdonagh/image4j")
          }),
        L("com.google.guava:guava",
          LicenseData("ScribeJava", License.APACHE_2).apply {
              description("Google core libraries for Java")
              author("The Guava Authors")
              url("https://github.com/google/guava")
          }),
        L("com.google.api-client",
          LicenseData("Google APIs Client Library for Java", License.APACHE_2).apply {
              description("Google APIs Client Library for Java")
              author("Google Inc.")
              url("https://github.com/googleapis/google-api-java-client")
          }),
        L("com.google.oauth-client",
          LicenseData("Google OAuth Client Library for Java", License.APACHE_2).apply {
              description("Google OAuth Client Library for Java")
              author("Google Inc.")
              url("https://github.com/googleapis/google-oauth-java-client")
          }),
        L("com.google.apis",
          LicenseData("Google Java API Client Services", License.APACHE_2).apply {
              description("Google Java API Client Services")
              author("Google Inc.")
              url("https://github.com/googleapis/google-api-java-client-services")
          }),
        L("com.google.zxing",
          LicenseData("ZXing", License.APACHE_2).apply {
              description("ZXing (\"Zebra Crossing\") barcode scanning library for Java, Android")
              author("ZXing authors")
              url("https://github.com/zxing/zxing")
          }),
        L("com.fasterxml.jackson.core",
          LicenseData("Jackson Core", License.APACHE_2).apply {
              description("Core part of Jackson that defines Streaming API as well as basic shared abstractions")
              author("FasterXML")
              url("https://github.com/FasterXML/jackson-core")
          }),
        L("com.corundumstudio.socketio:netty-socketio",
          LicenseData("Netty SocketIO", License.APACHE_2).apply {
              description("Java implementation of Socket.IO server")
              author("Nikita Koksharov")
              url("https://github.com/mrniko/netty-socketio")
          }),
        L("javax.servlet:javax.servlet-api",
          LicenseData("Java Servlet API", License.CDDL_1_1).apply {
              description("Java Servlet API")
              author("Oracle and/or its affiliates")
              url("https://javaee.github.io/servlet-spec/")
          }),
        L("net.jodah:expiringmap",
          LicenseData("Jodah Expiring Map", License.APACHE_2).apply {
              description("high performance thread-safe map that expires entries")
              author("Jonathan Halterman")
              url("https://github.com/jhalterman/expiringmap")
          }),
        L("com.vaadin:vaadin",
          LicenseData("Vaadin", License.APACHE_2).apply {
              description("An open platform for building modern web apps for Java back ends")
              author("Vaadin Ltd")
              url("https://vaadin.com")
              url("https://github.com/vaadin")
          }),
        L("org.conscrypt:conscrypt-openjdk-uber",
          LicenseData("Conscrypt", License.APACHE_2).apply {
              description("An open platform for building modern web apps for Java back ends")
              author("Google Inc")
              author("The Android Open Source Project")
              author("The Netty Project")
              author("Apache Harmony")
              url("https://github.com/google/conscrypt")
          }),
        L("io.prometheus",
          LicenseData("Prometheus", License.APACHE_2).apply {
              description("The Prometheus monitoring system and time series database")
              author("Fabian Stäber fabian@fstab.de")
              author("Tom Wilkie tom@grafana.com")
              author("Brian Brazil brian.brazil@boxever.com")
              url("https://github.com/prometheus/")
          }),
        L("com.aayushatharva.brotli4j",
          LicenseData("Brotli4j", License.APACHE_2).apply {
              description("Brotli4j provides Brotli compression and decompression for Java")
              author("Aayush Atharva")
              url("https://github.com/hyperxpro/Brotli4j")

              extra("Brotli", License.MIT) {
                url("https://github.com/google/brotli")
                author("The Brotli Authors")
              }
              extra("Netty", License.APACHE_2) {
                author("The Netty Project")
                url("https://netty.io")
              }
          }),
        L("com.github.oshi:oshi-core-java11",
          LicenseData("Brotli4j", License.APACHE_2).apply {
              description("Operating System and Hardware Information library for Java.")
              author("Aayush Atharva")
              url("https://github.com/oshi/oshi")
          }),
        L("com.github.oshi:oshi-core",
          LicenseData("OSHI", License.MIT).apply {
              description("Operating System and Hardware Information library for Java.")
              author("Aayush Atharva")
              url("https://github.com/oshi/oshi")
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
                Pair(split[0], Version(0))
            }
            2 -> {
                val group = split[0]
                val name = split[1]

                // check to see if the name is really a version or a name
                try {
                    Pair(group, Version(name))
                } catch (e: Exception) {
                    Pair("$group:$name", Version(0))
                }
            }
            else -> {
                val group = split[0]
                val name = split[1]
                val ver = split[2]

                // check to see if the name is really a version or a name
                try {
                    Pair("$group:$name", Version(ver))
                } catch (e: Exception) {
                    Pair("$group:$name", Version(0))
                }
            }
        }


        if (DEBUG) {
            println("Got: ${pair.first}, ver: ${pair.second} from $fullName")
        }

        return pair
    }
}
