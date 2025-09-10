# TMDB VidPlus Provider

Provider ini mengintegrasikan data film dan serial TV dari The Movie Database (TMDB) dengan player VidPlus untuk streaming konten.

## Fitur

- **Data dari TMDB**: Menggunakan API TMDB untuk mendapatkan informasi film dan serial TV yang lengkap
- **Player VidPlus**: Menggunakan embed player dari vidplus.to untuk streaming
- **Support Multiple Content**: Mendukung film dan serial TV
- **Rich Metadata**: Informasi lengkap termasuk cast, genre, rating, dll.

## Cara Kerja

### 1. Arsitektur Provider

Provider ini terdiri dari beberapa komponen utama:

- **TMDBVidPlusProvider**: Main API provider yang menghandle pencarian dan metadata
- **VidPlusExtractor**: Extractor khusus untuk mengekstrak video sources dari VidPlus embed
- **TMDBVidPlusPlugin**: Plugin loader yang mendaftarkan provider dan extractor

### 2. Flow Data

1. **Search/Browse**: Provider mengquery TMDB API untuk mendapatkan daftar film/series
2. **Load Details**: Ketika user memilih konten, provider mengambil detail lengkap dari TMDB
3. **Load Links**: Provider membuat URL embed VidPlus berdasarkan TMDB ID
4. **Extract Sources**: VidPlusExtractor mengekstrak video sources dari embed page

### 3. URL Structure

- **Movies**: `https://player.vidplus.to/embed/movie/{tmdb_id}`
- **TV Series**: `https://player.vidplus.to/embed/tv/{tmdb_id}/{season}/{episode}`

## Konfigurasi

### TMDB API Key

**PENTING**: Anda perlu mengganti `YOUR_TMDB_API_KEY_HERE` dengan API key TMDB yang valid:

1. Daftar di [TMDB](https://www.themoviedb.org/)
2. Buat API key di settings
3. Ganti string di file `TMDBVidPlusProvider.kt`:

```kotlin
private val tmdbApiKey = "YOUR_ACTUAL_API_KEY_HERE"
```

## Cara Bypass Video Sources

Provider ini menggunakan beberapa metode untuk mengekstrak video sources dari VidPlus:

### 1. Script Analysis
Menganalisis tag `<script>` untuk mencari pattern video URLs:
- File patterns: `.m3u8`, `.mp4`
- JSON patterns: `"file": "url"`, `"src": "url"`

### 2. Iframe Detection
Mencari iframe yang mungkin berisi player video sebenarnya.

### 3. JSON Data Extraction
Mengekstrak data JSON yang berisi informasi video sources.

### 4. Quality Detection
Otomatis mendeteksi kualitas video dari URL:
- 1080p, 720p, 480p, 360p
- Fallback ke "Unknown" quality

## Struktur Code

```
TMDBVidPlusProvider/
├── build.gradle.kts              # Build configuration
├── src/main/
│   ├── AndroidManifest.xml       # Android manifest
│   └── kotlin/recloudstream/
│       ├── TMDBVidPlusPlugin.kt   # Plugin loader
│       ├── TMDBVidPlusProvider.kt # Main provider
│       └── VidPlusExtractor.kt    # Video extractor
└── README.md                     # Documentation
```

## Data Classes

Provider menggunakan data classes untuk parsing TMDB API:

- `TMDBSearchResponse`: Response pencarian
- `TMDBItem`: Item individual (movie/tv)
- `TMDBMovieDetails`: Detail film
- `TMDBTVDetails`: Detail serial TV
- `TMDBSeason`, `TMDBEpisode`: Data season dan episode

## Metode Ekstraksi Video

### loadExtractor()
Fungsi bawaan Cloudstream yang otomatis mencoba berbagai extractor untuk URL yang diberikan.

### Custom VidPlusExtractor
Extractor khusus yang:
1. Menganalisis HTML page VidPlus
2. Mencari pattern video URLs dalam JavaScript
3. Mengekstrak kualitas dari URL
4. Mengembalikan ExtractorLink dengan informasi lengkap

## Troubleshooting

### Common Issues

1. **API Key Invalid**: Pastikan TMDB API key sudah benar
2. **No Video Sources**: VidPlus mungkin mengubah struktur embed
3. **Quality Detection Failed**: URL tidak mengandung informasi kualitas

### Debug Tips

- Check network requests di developer tools
- Analisis structure HTML VidPlus embed page
- Test dengan berbagai TMDB ID

## Development Notes

### Extending the Provider

Untuk menambahkan support player lain:
1. Buat extractor baru yang extend `ExtractorApi`
2. Implement `getUrl()` method
3. Register di plugin dengan `registerExtractorAPI()`

### Adding More TMDB Endpoints

Provider bisa diperluas untuk menggunakan endpoint TMDB lain:
- Trending content
- Recommendations
- Similar movies/shows
- Person details

## Security Considerations

- TMDB API key harus dijaga kerahasiaannya
- VidPlus embed mungkin berisi ads/trackers
- Selalu validate input dari user

## Legal Notice

Provider ini hanya untuk tujuan edukasi. Pastikan Anda memiliki hak legal untuk mengakses konten yang di-stream.
