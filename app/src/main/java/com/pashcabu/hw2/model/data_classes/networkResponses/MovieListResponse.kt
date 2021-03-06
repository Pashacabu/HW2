package com.pashcabu.hw2.model.data_classes.networkResponses

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MovieListResponse(
    @SerialName("dates")
    val dates: Dates? = null,
    @SerialName("page")
    val page: Int? = null,
    @SerialName("total_pages")
    val totalPages: Int? = null,
    @SerialName("results")
    val results: MutableList<Movie?>? = null,
    @SerialName("total_results")
    val totalResults: Int? = null
)

@Serializable
data class Dates(
    @SerialName("maximum")
    val maximum: String? = null,
    @SerialName("minimum")
    val minimum: String? = null
)

@Serializable
data class Movie(
    @SerialName("overview")
    val overview: String? = null,
    @SerialName("original_language")
    val originalLanguage: String? = null,
    @SerialName("original_title")
    val originalTitle: String? = null,
    @SerialName("video")
    val video: Boolean? = null,
    @SerialName("title")
    var title: String? = null,
    @SerialName("genre_ids")
    var genreIds: List<Int?>? = null,
    var genres: List<String?>? = listOf(),
    @SerialName("poster_path")
    var posterPath: String? = null,
    @SerialName("backdrop_path")
    var backdropPath: String? = null,
    @SerialName("release_date")
    var releaseDate: String? = null,
    @SerialName("popularity")
    var popularity: Double? = null,
    @SerialName("vote_average")
    var voteAverage: Double? = null,
    @SerialName("id")
    var id: Int? = null,
    @SerialName("adult")
    var adult: Boolean? = null,
    @SerialName("vote_count")
    var voteCount: Int? = null,
    var addedToFavourite: Boolean = false
)

