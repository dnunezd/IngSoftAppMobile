package com.example.vinilos.data.model

/** Who is using the app: no one yet, a visitor, or a specific collector. */
sealed interface Session {

    /** The single rule for showing the collector's exclusive actions (create album, comment...). */
    val collectorActionsEnabled: Boolean
        get() = this is Collector

    data object None : Session

    data object Visitor : Session

    data class Collector(val collector: com.example.vinilos.data.model.Collector) : Session
}
