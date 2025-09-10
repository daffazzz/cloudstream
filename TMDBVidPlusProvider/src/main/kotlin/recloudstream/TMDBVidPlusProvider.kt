package recloudstream

import com.fasterxml.jackson.annotation.JsonProperty
import com.lagradost.cloudstream3.Actor
import com.lagradost.cloudstream3.ActorData
import com.lagradost.cloudstream3.Episode
import com.lagradost.cloudstream3.HomePageList
import com.lagradost.cloudstream3.HomePageResponse
import com.lagradost.cloudstream3.LoadResponse
import com.lagradost.cloudstream3.MainAPI
import com.lagradost.cloudstream3.MainPageRequest
import com.lagradost.cloudstream3.SearchResponse
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.TvType
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.mainPageOf
import com.lagradost.cloudstream3.newEpisode
import com.lagradost.cloudstream3.newHomePageResponse
import com.lagradost.cloudstream3.newMovieLoadResponse
import com.lagradost.cloudstream3.newMovieSearchResponse
import com.lagradost.cloudstream3.newTvSeriesLoadResponse
import com.lagradost.cloudstream3.newTvSeriesSearchResponse
import com.lagradost.cloudstream3.utils.AppUtils.tryParseJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.loadExtractor

class TMDBVidPlusProvider : MainAPI() {

    // TMDB API Configuration
    private val tmdbApiKey = "f6d3ebb49663df38c7b2fad96e95a16b" // User needs to replace this
    private val tmdbBaseUrl = "https://api.themoviedb.org/3"
    private val tmdbImageBaseUrl = "https://image.tmdb.org/t/p/w500"
    private val vidPlusBaseUrl = "https://player.vidplus.to/embed"

    // Data classes for TMDB API responses
    data class TMDBSearchResponse(
        @JsonProperty("results") val results: List<TMDBItem>,
        @JsonProperty("total_pages") val totalPages: Int
    )

    data class TMDBItem(
        @JsonProperty("id") val id: Int,
        @JsonProperty("title") val title: String? = null,
        @JsonProperty("name") val name: String? = null,
        @JsonProperty("overview") val overview: String,
        @JsonProperty("poster_path") val posterPath: String?,
        @JsonProperty("backdrop_path") val backdropPath: String?,
        @JsonProperty("release_date") val releaseDate: String? = null,
        @JsonProperty("first_air_date") val firstAirDate: String? = null,
        @JsonProperty("media_type") val mediaType: String? = null,
        @JsonProperty("vote_average") val voteAverage: Double
    )

    data class TMDBMovieDetails(
        @JsonProperty("id") val id: Int,
        @JsonProperty("title") val title: String,
        @JsonProperty("overview") val overview: String,
        @JsonProperty("poster_path") val posterPath: String?,
        @JsonProperty("backdrop_path") val backdropPath: String?,
        @JsonProperty("release_date") val releaseDate: String,
        @JsonProperty("runtime") val runtime: Int?,
        @JsonProperty("genres") val genres: List<TMDBGenre>,
        @JsonProperty("cast") val cast: List<TMDBCast>? = null,
        @JsonProperty("vote_average") val voteAverage: Double
    )

    data class TMDBTVDetails(
        @JsonProperty("id") val id: Int,
        @JsonProperty("name") val name: String,
        @JsonProperty("overview") val overview: String,
        @JsonProperty("poster_path") val posterPath: String?,
        @JsonProperty("backdrop_path") val backdropPath: String?,
        @JsonProperty("first_air_date") val firstAirDate: String,
        @JsonProperty("number_of_seasons") val numberOfSeasons: Int,
        @JsonProperty("genres") val genres: List<TMDBGenre>,
        @JsonProperty("seasons") val seasons: List<TMDBSeason>,
        @JsonProperty("cast") val cast: List<TMDBCast>? = null,
        @JsonProperty("vote_average") val voteAverage: Double
    )

    data class TMDBSeason(
        @JsonProperty("season_number") val seasonNumber: Int,
        @JsonProperty("episode_count") val episodeCount: Int,
        @JsonProperty("name") val name: String
    )

    data class TMDBSeasonDetails(
        @JsonProperty("episodes") val episodes: List<TMDBEpisode>
    )

    data class TMDBEpisode(
        @JsonProperty("id") val id: Int,
        @JsonProperty("name") val name: String,
        @JsonProperty("overview") val overview: String,
        @JsonProperty("episode_number") val episodeNumber: Int,
        @JsonProperty("season_number") val seasonNumber: Int,
        @JsonProperty("still_path") val stillPath: String?
    )

    data class TMDBGenre(
        @JsonProperty("id") val id: Int,
        @JsonProperty("name") val name: String
    )

    data class TMDBCast(
        @JsonProperty("name") val name: String,
        @JsonProperty("character") val character: String,
        @JsonProperty("profile_path") val profilePath: String?
    )

    override var mainUrl = "https://player.vidplus.to"
    override var name = "TMDB VidPlus"
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)
    override var lang = "en"
    override val hasMainPage = true

    override val mainPage = mainPageOf(
        "popular" to "Popular Movies",
        "top_rated" to "Top Rated Movies",
        "upcoming" to "Upcoming Movies",
        "tv/popular" to "Popular TV Shows",
        "tv/top_rated" to "Top Rated TV Shows"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val url = if (request.data.startsWith("tv/")) {
            "$tmdbBaseUrl/${request.data}?api_key=$tmdbApiKey&page=$page"
        } else {
            "$tmdbBaseUrl/movie/${request.data}?api_key=$tmdbApiKey&page=$page"
        }
        val response = app.get(url).text
        val searchResponse = tryParseJson<TMDBSearchResponse>(response)
        
        val items = searchResponse?.results?.map { item ->
            item.toSearchResponse()
        } ?: emptyList()

        return newHomePageResponse(
            listOf(
                HomePageList(
                    request.name,
                    items,
                    true
                )
            ),
            hasNext = searchResponse?.totalPages?.let { page < it } ?: false
        )
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val response = app.get("$tmdbBaseUrl/search/multi?api_key=$tmdbApiKey&query=$query").text
        val searchResponse = tryParseJson<TMDBSearchResponse>(response)
        
        return searchResponse?.results?.map { it.toSearchResponse() } ?: emptyList()
    }

    override suspend fun load(url: String): LoadResponse? {
        val regex = Regex("/(\\d+)/(movie|tv)")
        val matchResult = regex.find(url)
        val tmdbId = matchResult?.groupValues?.get(1)?.toIntOrNull() ?: return null
        val mediaType = matchResult.groupValues[2]

        return when (mediaType) {
            "movie" -> loadMovie(tmdbId)
            "tv" -> loadTVSeries(tmdbId)
            else -> null
        }
    }

    private suspend fun loadMovie(tmdbId: Int): LoadResponse? {
        val response = app.get("$tmdbBaseUrl/movie/$tmdbId?api_key=$tmdbApiKey&append_to_response=credits").text
        val movie = tryParseJson<TMDBMovieDetails>(response) ?: return null

        return newMovieLoadResponse(
            movie.title,
            "$mainUrl/$tmdbId/movie",
            TvType.Movie,
            tmdbId.toString()
        ) {
            plot = movie.overview
            year = movie.releaseDate.take(4).toIntOrNull()
            posterUrl = movie.posterPath?.let { "$tmdbImageBaseUrl$it" }
            backgroundPosterUrl = movie.backdropPath?.let { "$tmdbImageBaseUrl$it" }
            rating = (movie.voteAverage * 1000).toInt()
            tags = movie.genres.map { it.name }
            duration = movie.runtime
            actors = movie.cast?.map { cast ->
                ActorData(
                    Actor(cast.name, cast.profilePath?.let { "$tmdbImageBaseUrl$it" } ?: ""),
                    roleString = cast.character
                )
            }
        }
    }

    private suspend fun loadTVSeries(tmdbId: Int): LoadResponse? {
        val response = app.get("$tmdbBaseUrl/tv/$tmdbId?api_key=$tmdbApiKey&append_to_response=credits").text
        val series = tryParseJson<TMDBTVDetails>(response) ?: return null

        val episodes = mutableListOf<Episode>()
        
        // Load episodes for all seasons
        for (season in series.seasons.filter { it.seasonNumber > 0 }) {
            val seasonResponse = app.get("$tmdbBaseUrl/tv/$tmdbId/season/${season.seasonNumber}?api_key=$tmdbApiKey").text
            val seasonDetails = tryParseJson<TMDBSeasonDetails>(seasonResponse)
            
            seasonDetails?.episodes?.forEach { episode ->
                episodes.add(
                    newEpisode("$tmdbId/tv/${season.seasonNumber}/${episode.episodeNumber}") {
                        name = episode.name
                        season = season.seasonNumber
                        this.episode = episode.episodeNumber
                        description = episode.overview
                        posterUrl = episode.stillPath?.let { "$tmdbImageBaseUrl$it" }
                    }
                )
            }
        }

        return newTvSeriesLoadResponse(
            series.name,
            "$mainUrl/$tmdbId/tv",
            TvType.TvSeries,
            episodes
        ) {
            plot = series.overview
            year = series.firstAirDate.take(4).toIntOrNull()
            posterUrl = series.posterPath?.let { "$tmdbImageBaseUrl$it" }
            backgroundPosterUrl = series.backdropPath?.let { "$tmdbImageBaseUrl$it" }
            rating = (series.voteAverage * 1000).toInt()
            tags = series.genres.map { it.name }
            actors = series.cast?.map { cast ->
                ActorData(
                    Actor(cast.name, cast.profilePath?.let { "$tmdbImageBaseUrl$it" } ?: ""),
                    roleString = cast.character
                )
            }
        }
    }

    private fun TMDBItem.toSearchResponse(): SearchResponse {
        val displayTitle = title ?: name ?: "Unknown"
        val displayDate = releaseDate ?: firstAirDate
        val type = when {
            title != null || mediaType == "movie" -> TvType.Movie
            name != null || mediaType == "tv" -> TvType.TvSeries
            else -> TvType.Movie
        }
        val mediaTypeString = if (type == TvType.Movie) "movie" else "tv"

        return if (type == TvType.Movie) {
            newMovieSearchResponse(
                displayTitle,
                "$mainUrl/$id/$mediaTypeString",
                type
            ) {
                posterUrl = posterPath?.let { "$tmdbImageBaseUrl$it" }
                year = displayDate?.take(4)?.toIntOrNull()
            }
        } else {
            newTvSeriesSearchResponse(
                displayTitle,
                "$mainUrl/$id/$mediaTypeString",
                type
            ) {
                posterUrl = posterPath?.let { "$tmdbImageBaseUrl$it" }
                year = displayDate?.take(4)?.toIntOrNull()
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        // Parse the data to get TMDB ID and type
        val parts = data.split("/")
        val tmdbId = parts[0]
        val mediaType = parts.getOrNull(1) ?: "movie"
        
        val embedUrl = when (mediaType) {
            "movie" -> "$vidPlusBaseUrl/movie/$tmdbId"
            "tv" -> {
                val season = parts.getOrNull(2) ?: "1"
                val episode = parts.getOrNull(3) ?: "1"
                "$vidPlusBaseUrl/tv/$tmdbId/$season/$episode"
            }
            else -> "$vidPlusBaseUrl/movie/$tmdbId"
        }

        // Use loadExtractor to extract video sources from VidPlus embed
        return loadExtractor(embedUrl, subtitleCallback, callback)
    }
}
