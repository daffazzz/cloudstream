package recloudstream

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.getQualityFromName
import com.lagradost.cloudstream3.utils.newExtractorLink
import org.jsoup.nodes.Document

class VidPlusExtractor : ExtractorApi() {
    override val name = "VidPlus"
    override val mainUrl = "https://player.vidplus.to"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        try {
            val document = app.get(url, referer = referer).document
            
            // Method 1: Look for direct video sources in script tags
            extractFromScripts(document, callback)
            
            // Method 2: Look for iframe sources
            extractFromIframes(document, callback)
            
            // Method 3: Look for JSON data with video sources
            extractFromJsonData(document, callback)
            
        } catch (e: Exception) {
            // If extraction fails, we can still try to pass the embed URL to other extractors
            callback.invoke(
                newExtractorLink(
                    name,
                    name,
                    url
                ) {
                    quality = Qualities.Unknown.value
                    type = ExtractorLinkType.VIDEO
                    referer = this@VidPlusExtractor.mainUrl
                }
            )
        }
    }

    private fun extractFromScripts(document: Document, callback: (ExtractorLink) -> Unit) {
        document.select("script").forEach { script ->
            val scriptContent = script.data()
            
            // Look for common video source patterns
            val patterns = listOf(
                Regex("\"file\"\\s*:\\s*\"([^\"]+\\.m3u8[^\"]*)\"|'file'\\s*:\\s*'([^']+\\.m3u8[^']*)'"),
                Regex("\"src\"\\s*:\\s*\"([^\"]+\\.mp4[^\"]*)\"|'src'\\s*:\\s*'([^']+\\.mp4[^']*)'"),
                Regex("source\\s*:\\s*\"([^\"]+)\"|source\\s*:\\s*'([^']+)'"),
                Regex("url\\s*:\\s*\"([^\"]+\\.m3u8[^\"]*)\"|url\\s*:\\s*'([^']+\\.m3u8[^']*)'")
            )
            
            patterns.forEach { pattern ->
                pattern.findAll(scriptContent).forEach { match ->
                    val videoUrl = match.groupValues[1].ifEmpty { match.groupValues[2] }
                    if (videoUrl.isNotEmpty() && (videoUrl.contains(".m3u8") || videoUrl.contains(".mp4"))) {
                        val quality = extractQualityFromUrl(videoUrl)
                        val type = if (videoUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                        
                        callback.invoke(
                            newExtractorLink(
                                name,
                                "$name ${quality}p",
                                videoUrl
                            ) {
                                this.quality = quality
                                this.type = type
                                referer = mainUrl
                            }
                        )
                    }
                }
            }
        }
    }

    private fun extractFromIframes(document: Document, callback: (ExtractorLink) -> Unit) {
        document.select("iframe").forEach { iframe ->
            val src = iframe.attr("src")
            if (src.isNotEmpty() && !src.startsWith("data:")) {
                // This iframe might contain the actual video source
                callback.invoke(
                    newExtractorLink(
                        name,
                        "$name Iframe",
                        src
                    ) {
                        quality = Qualities.Unknown.value
                        type = ExtractorLinkType.VIDEO
                        referer = mainUrl
                    }
                )
            }
        }
    }

    private fun extractFromJsonData(document: Document, callback: (ExtractorLink) -> Unit) {
        // Look for JSON data that might contain video sources
        document.select("script[type='application/json'], script:containsData(sources)").forEach { script ->
            val scriptContent = script.data()
            
            // Try to extract video URLs from JSON-like structures
            val jsonPattern = Regex("\"(?:file|src|url)\"\\s*:\\s*\"([^\"]+)\"")
            jsonPattern.findAll(scriptContent).forEach { match ->
                val videoUrl = match.groupValues[1]
                if (videoUrl.contains(".m3u8") || videoUrl.contains(".mp4")) {
                    val quality = extractQualityFromUrl(videoUrl)
                    val type = if (videoUrl.contains(".m3u8")) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    
                    callback.invoke(
                        newExtractorLink(
                            name,
                            "$name ${quality}p",
                            videoUrl
                        ) {
                            this.quality = quality
                            this.type = type
                            referer = mainUrl
                        }
                    )
                }
            }
        }
    }

    private fun extractQualityFromUrl(url: String): Int {
        // Try to extract quality from URL
        val qualityPatterns = listOf(
            Regex("(\\d{3,4})p"),
            Regex("(\\d{3,4})x\\d+"),
            Regex("_(\\d{3,4})_")
        )
        
        qualityPatterns.forEach { pattern ->
            pattern.find(url)?.let { match ->
                return getQualityFromName(match.groupValues[1])
            }
        }
        
        // Default quality if not found
        return when {
            url.contains("1080") -> Qualities.P1080.value
            url.contains("720") -> Qualities.P720.value
            url.contains("480") -> Qualities.P480.value
            url.contains("360") -> Qualities.P360.value
            else -> Qualities.Unknown.value
        }
    }
}
