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

import dorkbox.license.Licensing
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly

/**
 * License data.
 */
enum class License constructor(internal val names: Collection<String>, internal val licenseFile: String, internal val urls: Collection<String>) {

    // @formatter:off
    UNKNOWN(listOf("Unknown License"),
            "",
            listOf("")),

    CUSTOM(listOf("Custom License"),
           "",
           listOf("")),

    AFL(listOf("Academic Free License (\"AFL\") v. 3.0",
               "AFL",
               "AFL 3.0",
               "Academic Free License 3.0"),
        "LICENSE.AFLv3",
        listOf("http://opensource.org/licenses/afl-3.0")),


    AGPL(listOf("The Affero GPL License",
                "Affero GPL",
                "AGPL",
                "Affero GPL 3",
                "GNU AFFERO GENERAL PUBLIC LICENSE, Version 3 (AGPL-3.0)",
                "GNU AFFERO GENERAL PUBLIC LICENSE, Version 3",
                "GNU AFFERO GENERAL PUBLIC LICENSE (AGPL-3.0)",
                "GNU AFFERO GENERAL PUBLIC LICENSE"),
         "LICENSE.AGPLv3",
         listOf("http://www.gnu.org/licenses/agpl.html",
                "http://www.gnu.org/licenses/agpl.txt",
                "http://www.opensource.org/licenses/agpl-v3.html",
                "http://www.opensource.org/licenses/agpl-v3",
                "http://opensource.org/licenses/agpl-v3.html",
                "http://opensource.org/licenses/agpl-v3")),


    @Deprecated("Use Apache v2")
    APACHE_1_1(listOf("The Apache Software License, Version 1.1",
                      "Apache 1.1",
                      "Apache Software License, Version 1.1",
                      "Apache Software License 1.1",
                      "Apache License 1.1"),
               "LICENSE.Apachev1.1",
               listOf("http://www.apache.org/licenses/LICENSE-1.1",
                      "http://www.apache.org/licenses/LICENSE-1.1.txt",
                      "http://apache.org/licenses/LICENSE-1.1",
                      "http://apache.org/licenses/LICENSE-1.1.txt",
                      "http://www.opensource.org/licenses/Apache-1.1",
                      "http://opensource.org/licenses/Apache-1.1")),


    APACHE_2(listOf("The Apache Software License, Version 2.0",
                    "Apache 2",
                    "Apache Software License, Version 2.0",
                    "Apache License, version 2.0",
                    "Apache License, version 2.0",
                    "Apache Software License 2.0",
                    "Apache License Version 2.0",
                    "Apache License 2.0"),
             "LICENSE.Apachev2",
             listOf("http://www.apache.org/licenses/LICENSE-2.0",
                    "http://www.apache.org/licenses/LICENSE-2.0.txt",
                    "http://www.apache.org/licenses/LICENSE-2.0.html",
                    "http://apache.org/licenses/LICENSE-2.0",
                    "http://apache.org/licenses/LICENSE-2.0.txt",
                    "http://apache.org/licenses/LICENSE-2.0.html",
                    "http://www.opensource.org/licenses/Apache-2.0",
                    "http://opensource.org/licenses/Apache-2.0")),


    BSD_2(listOf("BSD 2-Clause \"Simplified\" or \"FreeBSD\" license",
                 "BSD 2",
                 "FreeBSD",
                 "FreeBSD License",
                 "Simplified BSD License"),
          "LICENSE.BSD2",
          listOf("http://opensource.org/licenses/BSD-2-Clause",
                 "http://opensource.org/licenses/bsd-license")),


    BSD_3(listOf("BSD 3-Clause License",
                 "BSD",
                 "BSD 3",
                 "BSD License",
                 "BSD 3 License",
                 "New BSD License",
                 "Revised BSD License",
                 "BSD 3-Clause",
                 "BSD 3-Clause \"New\" or \"Revised\" license"),
          "LICENSE.BSD3",
          listOf("http://opensource.org/licenses/BSD-3-Clause",
                 "http://asm.objectweb.org/license.html",
                 "http://asm.ow2.org/license.html",
                 "http://antlr.org/license.html")),


    BSL(listOf("Boost Software License 1.0 (BSL-1.0)",
               "Boost",
               "BSL",
               "BSL-1.0",
               "BSL 1.0",
               "Boost Software License Version 1.0",
               "Boost Software License 1.0"),
        "LICENSE.BSL",
        listOf("http://www.opensource.org/licenses/BSL-1.0",
               "http://opensource.org/licenses/BSL-1.0")),


    CC0(listOf("Public Domain, per Creative Commons CC0",
               "CC0",
               "CC0 1.0 Universal"),
        "LICENSE.CC0",
        listOf("http://creativecommons.org/publicdomain/zero/1.0/")),


    @Deprecated("Use CC-BY 3")
    CC_BY_25(listOf("Creative Commons Attribution (CC-A) 2.5",
                    "CC-BY 2.5",
                    "CC-A 2.5",
                    "Attribution 2.5 Generic (CC BY 2.5)"),
             "LICENSE.CC_BY_2.5",
             listOf("https://creativecommons.org/licenses/by/2.5/legalcode")),


    CC_BY_3(listOf("Creative Commons Attribution (CC-A) 3.0",
                   "CC-A 3.0",
                   "CC-A",
                   "CC-BY 3",
                   "CC-BY 3.0",
                   "Attribution 3.0 Unported (CC BY 3.0)"),
            "LICENSE.CC_BY_3",
            listOf("https://creativecommons.org/licenses/by/3.0/legalcode")),


    CDDL(listOf("Common Development and Distribution License",
                "CDDL",
                "Common Development and Distribution License (CDDL)",
                "CDDL License",
                "COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0"),
         "VERSION.CDDL",
         listOf("http://opensource.org/licenses/CDDL-1.0")),


    CDDL_1_1(listOf("Common Development and Distribution License Version 1.1",
                    "CDDL 1.1",
                    "Common Development and Distribution License (CDDL 1.1)",
                    "CDDL License v1.1",
                    "COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.1"),
             "VERSION.CDDLv1.1",
             listOf("https://javaee.github.io/glassfish/LICENSE")),


    @Deprecated("Use EPL")
    CPL(listOf("Common Public License Version 1.0", "CPL"),
        "LICENSE.CPL",
        listOf("http://www.opensource.org/licenses/cpl1.0.txt")),


    EDL(listOf("Eclipse Distribution License (EDL)",
               "EDL",
               "EDL 1.0",
               "Eclipse Distribution License",
               "Eclipse Distribution License - v 1.0"),
        "LICENSE.EDL",
        listOf("http://www.eclipse.org/org/documents/edl-v10.html")),


    EPL(listOf("Eclipse Public License (EPL)",
               "EPL",
               "EPL 1.0",
               "Eclipse Public License",
               "Eclipse Public License - v 1.0",
               "Eclipse Public License Version 1.0"),
        "LICENSE.EPL",
        listOf("http://www.eclipse.org/legal/epl-v10.html",
               "http://opensource.org/licenses/EPL-1.0",
               "http://www.opensource.org/licenses/EPL-1.0",
               "http://opensource.org/licenses/eclipse-1.0.txt")),


    GPLv2(listOf("GNU General Public License, version 2",
                 "GPLv2",
                 "GNU General Public License (GPLv2)",
                 "The GNU General Public License, Version 2"),
          "LICENSE.GPLv2",
          listOf("http://www.gnu.org/licenses/gpl-2.0.txt",
                 "http://opensource.org/licenses/GPL-2.0")),


    GPLv2_CLASSPATH(listOf("GNU General Public License, version 2, with the Classpath Exception",
                           "GPLv2, with the Classpath Exception",
                           "GNU General Public License (GPLv2), with the Classpath Exception",
                           "The GNU General Public License, Version 2, with the Classpath Exception"),
                    "LICENSE.GPLv2_CP",
                    listOf("https://www.gnu.org/software/classpath/license.html")),


    GPLv3(listOf("GNU General Public License, version 3",
                 "GPL",
                 "GPLv3",
                 "GNU General Public License (GPLv3)",
                 "The GNU General Public License, Version 3"),
          "LICENSE.GPLv3",
          listOf("http://www.gnu.org/licenses/gpl-3.0.txt", "http://opensource.org/licenses/GPL-3.0")),


    GPLv3_CLASSPATH(listOf("GNU General Public License, version 3, with the Classpath Exception",
                           "GPLv3, with the Classpath Exception",
                           "GNU General Public License (GPLv3), with the Classpath Exception",
                           "GNU General Public License, version 3, with the Classpath Exception",
                           "The GNU General Public License, Version 3, with the Classpath Exception"),
                    "LICENSE.GPLv3_CP",
                    listOf("https://www.gnu.org/software/classpath/license.html")),


    JSON(listOf("The JSON License"),
         "LICENSE.JSON",
         listOf("http://www.json.org/license.html")),


    LGPLv2_1(listOf("GNU Lesser General Public License, version 2.1",
                    "LGPL-2.1",
                    "LGPL 2.1",
                    "GNU Library General Public License, version 2.1",
                    "GNU Library General Public License (LGPL-2.1)",
                    "GNU Lesser General Public License (LGPL-2.1)",
                    "GNU \"Lesser\" General Public License, version 2.1",
                    "GNU \"Lesser\" General Public License (LGPL-2.1)",
                    "GNU Library or \"Lesser\" General Public License, version 2.1",
                    "GNU Library or \"Lesser\" General Public License (LGPL-2.1)",
                    "GNU Lesser Public License, version 2.1"),
             "LICENSE.LGPLv2.1",
             listOf("http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html",
                    "https://opensource.org/licenses/LGPL-2.1",
                    "http://www.gnu.org/licenses/lgpl.html")),


    LGPLv3(listOf("GNU Lesser General Public License, version 3",
                  "LGPL-3",
                  "LGPL 3",
                  "GNU Library General Public License, version 3",
                  "GNU Library General Public License (LGPL-3)",
                  "GNU Lesser General Public License (LGPL-3)",
                  "GNU \"Lesser\" General Public License, version 3",
                  "GNU \"Lesser\" General Public License (LGPL-3)",
                  "GNU Library or \"Lesser\" General Public License, version 3",
                  "GNU Library or \"Lesser\" General Public License (LGPL-3)",
                  "GNU Lesser Public License, version 3"),
           "LICENSE.LGPLv3",
           listOf("http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html",
                  "http://www.opensource.org/licenses/lgpl-license",
                  "http://opensource.org/licenses/lgpl-license",
                  "http://www.gnu.org/licenses/lgpl.html")),


    MIT_X11(listOf("MIT License",
                   "MIT",
                   "X11",
                   "X11 License",
                   "MIT/X Consortium License",
                   "Expat License",
                   "Bouncy Castle Licence",
                   "The PostgreSQL License"),
            "LICENSE.MIT",
            listOf("http://opensource.org/licenses/MIT",
                   "http://www.opensource.org/licenses/MIT",
                   "https://www.bouncycastle.org/licence.html",
                   "http://www.postgresql.org/about/licence/")),


    MIT(MIT_X11.names, MIT_X11.licenseFile, MIT_X11.urls),


    ICU(listOf("ICU License", "ICU"),
        "LICENSE.ICU",
        listOf("http://source.icu-project.org/repos/icu/icu/branches/maint/maint-4-8/license.html")),


    @Deprecated("Use MOZILLA_2")
    MOZILLA_1_1(listOf("Mozilla Public License 1.1",
                       "MPL-1.1",
                       "Mozilla Public License, Version 1.1"),
                "LICENSE.MPLv1.1",
                listOf("http://opensource.org/licenses/MPL-1.1",
                       "http://www.mozilla.org/media/MPL/1.1/index.txt")),


    MOZILLA_2(listOf("Mozilla Public License 2.0",
                     "MPL-2",
                     "Mozilla Public License, Version 2.0"),
              "LICENSE.MPLv2",
              listOf("http://opensource.org/licenses/MPL-2.0",
                     "http://www.mozilla.org/MPL/2.0/index.txt")),


    MS_PL(listOf("Microsoft Public License (MS-PL)",
                 "MS-PL",
                 "Microsoft Public License"),
          "LICENSE.MSPL",
          listOf("http://opensource.org/licenses/ms-pl.html",
                 "http://opensource.org/licenses/MS-PL")),


    OSGI(listOf("OSGi Specification License, Version 2.0"),
         "LICENSE.OSGI",
         listOf("http://www.osgi.org/Specifications/Licensing")),


    OFL(listOf("Open Font License"),
        "LICENSE.OFLv1.1",
        listOf("http://scripts.sil.org/OFL",
               "http://opensource.org/licenses/OFL-1.1")),


    PHP_3_1(listOf("The PHP License, version 3.01",
                   "PHP License 3.01",
                   "PHP License, version 3.01"),
            "LICENSE.PHPv3.01",
            listOf("http://php.net/license/3_01.txt")),


    PYTHON(listOf("Python License, Version 2 (Python-2.0)",
                  "Python",
                  "Python-2.0",
                  "Python License",
                  "Python License 2.0",
                  "Python License (Python-2.0)",
                  "Python Software Foundation License",
                  "PYTHON SOFTWARE FOUNDATION LICENSE VERSION 2"),
           "LICENSE.PYTHON",
           listOf("http://opensource.org/licenses/PythonSoftFoundation")),


    @Deprecated("Use CC0")
    PUBLIC_DOMAIN(listOf("Public Domain"),
                  "LICENSE.Public",
                  listOf("http://creativecommons.org/licenses/publicdomain/",
                         "http://creativecommons.org/publicdomain/mark/1.0/")),


    RUBY(listOf("Ruby License", "Ruby"),
         "LICENSE.RUBY",
         listOf("http://www.ruby-lang.org/en/LICENSE.txt",
                "http://www.ruby-lang.org/en/about/license.txt")),


    SLEEPYCAT(listOf("The Sleepycat License",
                     "Sleepycat",
                     "The Sleepycat License (Sleepycat)",
                     "Sleepycat License",
                     "The Sleepycat Public License",
                     "The Sleepycat License",
                     "Berkeley Database License",
                     "The Berkeley Database License"),
              "LICENSE.Sleepycat",
              listOf("http://opensource.org/licenses/sleepycat")),


    NCSA(listOf("The University of Illinois/NCSA Open Source License (NCSA)",
                "NCSA",
                "UoI-NCSA",
                "The University of Illinois/NCSA Open Source License",
                "University of Illinois/NCSA Open Source License (NCSA)",
                "University of Illinois/NCSA Open Source License"),
         "LICENSE.NCSA",
         listOf("http://opensource.org/licenses/UoI-NCSA.php")),


    W3C(listOf("The W3C SOFTWARE NOTICE AND LICENSE (W3C)",
               "W3C",
               "The W3C SOFTWARE NOTICE AND LICENSE",
               "W3C SOFTWARE NOTICE AND LICENSE (W3C)",
               "W3C SOFTWARE NOTICE AND LICENSE",
               "W3C® SOFTWARE NOTICE AND LICENSE"),
        "LICENSE.W3C",
        listOf("http://www.w3.org/Consortium/Legal/2002/copyright-software-20021231",
               "http://opensource.org/licenses/W3C.php")),


    WTFPL(listOf("WTFPL – Do What the Fuck You Want to Public License",
                 "WTFPL",
                 "Do What the Fuck You Want to Public License"),
          "LICENSE.WTFPL",
          listOf("http://www.wtfpl.net/",
                 "http://www.wtfpl.net/txt/copying/")),


    ZLIB(listOf("The zlib/libpng License (Zlib)",
                "Zlib",
                "The zlib/libpng License"),
         "LICENSE.ZLIB",
         listOf("http://opensource.org/licenses/zlib-license"));

    // @formatter:on

    val preferedName: String
        get() = names.first()

    val preferedUrl: String
        get() = urls.first()

    val licenseText: String
        get() {
            return Licensing.getLicense(this.licenseFile)
        }


    companion object {
        /**
         * Default is to return UNKNOWN license
         */
        fun valueOfLicenseName(licenseName: String): License {
            if (licenseName.isEmpty()) {
                return UNKNOWN
            }
            val normalizedLicenseName = licenseName.toLowerCaseAsciiOnly()
            for (license in License.values()) {
                for (name in license.names) {
                    if (name.toLowerCaseAsciiOnly() == normalizedLicenseName) {
                        return license
                    }
                }
            }
            return UNKNOWN
        }

        /**
         * Default is to return UNKNOWN license
         */
        fun valueOfLicenseUrl(licenseUrl: String): License {
            if (licenseUrl.isEmpty()) {
                return UNKNOWN
            }
            for (license in License.values()) {
                for (url in license.urls) {
                    if (url == licenseUrl) {
                        return license
                    }
                }
            }

            return UNKNOWN
        }
    }
}
