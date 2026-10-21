package com.example.vinilos.data.model

/** Quién está usando la app: nadie aún, un visitante o un coleccionista concreto. */
sealed interface Session {

    /** Única regla para mostrar las acciones exclusivas del coleccionista (crear álbum, comentar...). */
    val collectorActionsEnabled: Boolean
        get() = this is Collector

    data object None : Session

    data object Visitor : Session

    data class Collector(val collector: com.example.vinilos.data.model.Collector) : Session
}
