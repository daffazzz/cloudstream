// use an integer for version numbers
version = 1

cloudstream {
    // All of these properties are optional, you can safely remove any of them.

    description = "Watch movies and TV series using TMDB data with VidPlus player"
    authors = listOf("Assistant")

    /**
     * Status int as one of the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta-only
     **/
    status = 3 // Beta-only for testing

    tvTypes = listOf("Movie", "TvSeries")
    iconUrl = "https://www.google.com/s2/favicons?domain=player.vidplus.to&sz=%size%"

    isCrossPlatform = true
}
