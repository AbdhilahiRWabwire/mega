package mega.privacy.android.app.presentation.node.model.mapper

import mega.privacy.android.icon.pack.R as IconPackR
import androidx.annotation.DrawableRes
import mega.privacy.android.app.R
import mega.privacy.android.domain.entity.node.TypedFileNode

private val cache = mutableMapOf<String, Int>()

@DrawableRes
internal fun getFileIcon(fileNode: TypedFileNode): Int {
    val extension = fileNode.name.substringAfterLast(
        delimiter = ".",
        missingDelimiterValue = ""
    )
    return cache.getOrElse(extension) {
        getFileIcon(extension).also {
            cache[extension] = it
        }
    }
}

@DrawableRes
private fun getFileIcon(extension: String) =
    when (extension) {
        "3ds", "3dm", "max", "obj" -> IconPackR.drawable.ic_3d_medium_solid
        "aec", "aep", "aepx", "aes", "aet", "aetx" -> IconPackR.drawable.ic_aftereffects_medium_solid
        "aif", "aiff", "wav", "flac", "iff", "m4a", "wma", "oga", "ogg", "mp3", "3ga", "opus", "weba" -> IconPackR.drawable.ic_audio_medium_solid
        "dwg", "dxf" -> IconPackR.drawable.ic_cad_medium_solid
        "bz2", "gz", "rar", "tar", "tbz", "tgz", "zip", "deb", "udeb", "rpm", "air", "7z", "bz", "bzip2", "cab", "lha", "gzip", "ace", "arc", "pkg" -> IconPackR.drawable.ic_compressed_medium_solid
        "accdb", "db", "dbf", "mdb", "pdb", "sql" -> R.drawable.database_thumbnail
        "dmg" -> IconPackR.drawable.ic_dmg_medium_solid
        "dwt" -> R.drawable.dreamweaver_thumbnail
        "xla", "xlam", "xll", "xlm", "xls", "xlsm", "xlsx", "xlt", "xltm", "xltx" -> IconPackR.drawable.ic_excel_medium_solid
        "apk", "app", "bat", "com", "exe", "gadget", "msi", "pif", "vb", "wsf" -> IconPackR.drawable.ic_executable_medium_solid
        "as", "asc", "ascs" -> IconPackR.drawable.ic_web_lang_medium_solid
        "fla" -> R.drawable.flash_thumbnail
        "fnt", "fon", "otf", "ttf" -> IconPackR.drawable.ic_font_medium_solid
        "gpx", "kml", "kmz" -> R.drawable.gis_thumbnail
        "dhtml", "htm", "html", "shtml", "xhtml" -> R.drawable.html_thumbnail
        "ai", "aia", "aip", "ait", "art", "irs" -> IconPackR.drawable.ic_illustrator_medium_solid
        "jpg", "jpeg", "tga", "tif", "tiff", "bmp", "gif", "png" -> IconPackR.drawable.ic_image_medium_solid
        "indd" -> IconPackR.drawable.ic_indesign_medium_solid
        "class", "jar", "java" -> R.drawable.java_thumbnail
        "mid", "midi" -> R.drawable.midi_thumbnail
        "pdf" -> IconPackR.drawable.ic_pdf_medium_solid
        "abr", "csh", "psb", "psd" -> IconPackR.drawable.ic_photoshop_medium_solid
        "asx", "m3u", "pls" -> R.drawable.playlist_thumbnail
        "pcast" -> R.drawable.podcast_thumbnail
        "pot", "potm", "potx", "ppam", "ppc", "pps", "ppsm", "ppsx", "ppt", "pptm", "pptx" -> IconPackR.drawable.ic_powerpoint_medium_solid
        "plb", "ppj", "prproj", "prtpset" -> IconPackR.drawable.ic_premiere_medium_solid
        "3fr", "mef", "arw", "bay", "cr2", "dcr", "dng", "erf", "fff", "mrw", "nef", "orf", "pef", "rw2", "rwl", "srf" -> IconPackR.drawable.ic_raw_medium_solid
        "ra", "ram", "rm" -> R.drawable.real_audio_thumbnail
        "c", "cc", "cgi", "cpp", "cxx", "dll", "h", "hpp", "pl", "py", "sh" -> R.drawable.source_thumbnail
        "123", "gsheet", "nb", "ots", "sxc", "xlr" -> IconPackR.drawable.ic_spreadsheet_medium_solid
        "srt" -> R.drawable.subtitles_thumbnail
        "swf", "flv" -> R.drawable.swf_thumbnail
        "ans", "ascii", "log", "rtf", "txt", "wpd" -> IconPackR.drawable.ic_text_medium_solid
        "torrent" -> IconPackR.drawable.ic_torrent_medium_solid
        "vcard", "vcf" -> R.drawable.vcard_thumbnail
        "cdr", "eps", "ps", "svg", "svgz" -> IconPackR.drawable.ic_vector_medium_solid
        "3g2", "3gp", "asf", "avi", "mkv", "mov", "mpeg", "mpg", "wmv", "3gpp", "h261", "h263", "h264", "jpgv", "jpm", "jpgm", "mp4", "mp4v", "mpg4", "mpe", "m1v", "m2v", "ogv", "qt", "m4u", "webm", "f4v", "fli", "m4v", "mk3d", "movie" -> IconPackR.drawable.ic_video_medium_solid
        "vob" -> R.drawable.video_vob_thumbnail
        "asp", "aspx", "php", "php3", "php4", "php5", "phtml", "css", "inc", "js", "xml" -> IconPackR.drawable.ic_web_data_medium_solid
        "doc", "docm", "docx", "dot", "dotx", "wps" -> IconPackR.drawable.ic_word_medium_solid
        "pages" -> IconPackR.drawable.ic_pages_medium_solid
        "Xd" -> IconPackR.drawable.ic_experiencedesign_medium_solid
        "key" -> IconPackR.drawable.ic_keynote_medium_solid
        "numbers" -> IconPackR.drawable.ic_numbers_medium_solid
        "odp", "odt", "ods" -> IconPackR.drawable.ic_openoffice_medium_solid
        "sketch" -> R.drawable.ic_sketch_thumbnail
        "url" -> IconPackR.drawable.ic_url_medium_solid
        else -> IconPackR.drawable.ic_generic_medium_solid
    }