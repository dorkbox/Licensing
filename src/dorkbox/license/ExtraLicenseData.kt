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
