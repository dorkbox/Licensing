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

class ExtraLicenseData(name: String, license: License) : LicenseData(name, license) {
    init {
        // we change this to 0, so we know if it has been otherwise set by a dependency
        copyright = 0
    }

    /**
     * If not specified, will use the current year. This only applies to extra license information (as it is necessary to record for the
     * original date of publication)
     */
    fun copyright(copyright: Int) {
        super.copyright = copyright
    }
}
