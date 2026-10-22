package com.example.vinilos.viewmodel

import androidx.lifecycle.ViewModel
import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Sesión compartida por todas las pantallas. Se crea atada a la Activity, por lo que
 * sobrevive a la rotación; no se persiste entre cierres de la app.
 */
class SessionViewModel : ViewModel() {

    private val _session = MutableStateFlow<Session>(Session.None)
    val session: StateFlow<Session> = _session.asStateFlow()

    fun enterAsVisitor() {
        _session.value = Session.Visitor
    }

    fun enterAsCollector(collector: Collector) {
        _session.value = Session.Collector(collector)
    }

    fun clear() {
        _session.value = Session.None
    }
}
