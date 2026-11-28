package com.example.vinilos.data.remote

/**
 * Only the field HU10 uses to compare favorites. Gson ignores the rest (name, image,
 * description) and leaves the missing one as null; the favorite comparison is only by [id].
 */
data class PerformerDto(
    val id: Int?
)
