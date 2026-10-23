package com.example.vinilos.viewmodel

import com.example.vinilos.data.model.Collector
import com.example.vinilos.data.model.Session
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionViewModelTest {

    private val manolo = Collector(100, "Manolo Bellon", "manollo@caracol.com.co")
    private val jaime = Collector(101, "Jaime Monsalve", "jmonsalve@rtvc.com.co")

    @Test
    fun alInicioNoHayPerfilNiAccionesDeColeccionista() {
        // Given
        val vm = SessionViewModel()

        // When
        val sesion = vm.session.value

        // Then
        assertEquals(Session.None, sesion)
        assertFalse(sesion.collectorActionsEnabled)
    }

    @Test
    fun ingresarComoVisitanteNoTieneColeccionistaNiAcciones() {
        // Given
        val vm = SessionViewModel()

        // When
        vm.enterAsVisitor()

        // Then
        assertEquals(Session.Visitor, vm.session.value)
        assertFalse(vm.session.value.collectorActionsEnabled)
    }

    @Test
    fun ingresarComoColeccionistaGuardaSusDatosYHabilitaLasAcciones() {
        // Given
        val vm = SessionViewModel()

        // When
        vm.enterAsCollector(manolo)

        // Then
        assertEquals(Session.Collector(manolo), vm.session.value)
        assertTrue(vm.session.value.collectorActionsEnabled)
    }

    @Test
    fun cambiarDeColeccionistaConservaSoloElNuevo() {
        // Given
        val vm = SessionViewModel()
        vm.enterAsCollector(manolo)

        // When
        vm.enterAsCollector(jaime)

        // Then
        assertEquals(Session.Collector(jaime), vm.session.value)
    }

    @Test
    fun cambiarDeColeccionistaAVisitanteNoDejaRestosDelAnterior() {
        // Given
        val vm = SessionViewModel()
        vm.enterAsCollector(manolo)

        // When
        vm.enterAsVisitor()

        // Then
        assertEquals(Session.Visitor, vm.session.value)
        assertFalse(vm.session.value.collectorActionsEnabled)
    }

    @Test
    fun cambiarDeVisitanteAColeccionistaHabilitaLasAcciones() {
        // Given
        val vm = SessionViewModel()
        vm.enterAsVisitor()

        // When
        vm.enterAsCollector(jaime)

        // Then
        assertEquals(Session.Collector(jaime), vm.session.value)
        assertTrue(vm.session.value.collectorActionsEnabled)
    }

    @Test
    fun clearDejaLaSesionSinPerfil() {
        // Given
        val vm = SessionViewModel()
        vm.enterAsCollector(manolo)

        // When
        vm.clear()

        // Then
        assertEquals(Session.None, vm.session.value)
        assertFalse(vm.session.value.collectorActionsEnabled)
    }

    @Test
    fun clearSinSesionNoCambiaNada() {
        // Given
        val vm = SessionViewModel()

        // When
        vm.clear()

        // Then
        assertEquals(Session.None, vm.session.value)
    }
}
