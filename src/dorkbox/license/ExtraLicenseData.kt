package dorkbox.license

import License

class ExtraLicenseData(name: String, license: License) : LicenseData(name, license) {
    /**
     * If not specified, will use the current year. This only applies to extra license information (as it is necessary to record for the
     * original date of publication)
     */
    fun copyright(copyright: Int) {
        this.copyright = copyright
    }
}
