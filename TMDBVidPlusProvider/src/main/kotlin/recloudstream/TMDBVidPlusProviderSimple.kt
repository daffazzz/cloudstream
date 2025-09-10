package recloudstream

import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor

class TMDBVidPlusProviderSimple : MainAPI() {
    override var mainUrl = "https://player.vidplus.to"
    override var name = "TMDB VidPlus Simple"
    override val supportedTypes = setOf(TvType.Movie)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "popular" to "Popular Movies"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        // Simple static list for testing
        val items = listOf(
            newMovieSearchResponse(
                "The Matrix",
                "$mainUrl/603/movie",
                TvType.Movie
            ),
            newMovieSearchResponse(
                "Inception",
                "$mainUrl/27205/movie", 
                TvType.Movie
            )
        )

        return newHomePageResponse(
            listOf(
                HomePageList(
                    request.name,
                    items,
                    true
                )
            ),
            false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        return listOf(
            newMovieSearchResponse(
                "Search Result: $query",
                "$mainUrl/123/movie",
                TvType.Movie
            )
        )
    }

    override suspend fun load(url: String): LoadResponse? {
        return newMovieLoadResponse(
            "Test Movie",
            url,
            TvType.Movie,
            "123"
        ) {
            plot = "This is a test movie"
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val embedUrl = "$mainUrl/embed/movie/$data"
        return loadExtractor(embedUrl, subtitleCallback, callback)
    }
}
