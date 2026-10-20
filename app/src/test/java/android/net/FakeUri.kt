package android.net

import android.os.Parcel

class FakeUri(private val uriString: String = "content://media/test") : Uri() {
    override fun toString(): String = uriString
    override fun isHierarchical(): Boolean = false
    override fun isRelative(): Boolean = false
    override fun getScheme(): String = "content"
    override fun getSchemeSpecificPart(): String = ""
    override fun getEncodedSchemeSpecificPart(): String = ""
    override fun getAuthority(): String = ""
    override fun getEncodedAuthority(): String = ""
    override fun getUserInfo(): String? = null
    override fun getEncodedUserInfo(): String? = null
    override fun getHost(): String? = null
    override fun getPort(): Int = -1
    override fun getPath(): String = ""
    override fun getEncodedPath(): String = ""
    override fun getQuery(): String? = null
    override fun getEncodedQuery(): String? = null
    override fun getFragment(): String? = null
    override fun getEncodedFragment(): String? = null
    override fun getPathSegments(): List<String> = emptyList()
    override fun getLastPathSegment(): String = ""
    override fun buildUpon(): Builder? = null
    override fun describeContents(): Int = 0
    override fun writeToParcel(dest: Parcel, flags: Int) {}
    override fun compareTo(other: Uri?): Int = 0
}
