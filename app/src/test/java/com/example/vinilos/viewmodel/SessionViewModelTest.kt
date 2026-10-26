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
        val vm = SessionViewModel()

        assertEquals(Session.None, vm.session.value)
        assertFalse(vm.session.value.collectorActionsEnabled)
    }

    @Test
    fun ingresarComoVisitanteNoTieneColeccionistaNiAcciones() {
        val vm = SessionViewModel()

        vm.enterAsVisitor()

        assertEquals(Session.Visitor, vm.session.value)
        assertFalse(vm.session.value.collectorActionsEnabled)
    }

    @Test
    fun ingresarComoColeccionistaGuardaSusDatosYHabilitaLasAcciones() {
        val vm = SessionViewModel()

        vm.enterAsCollector(manolo)

        assertEquals(Session.Collector(manolo), vm.session.value)
        assertTrue(vm.session.value.collectorActionsEnabled)
    }

    @Test
    fun cambiarDeColeccionistaConservaSoloElNuevo() {
        val vm = SessionViewModel()
        vm.enterAsCollector(manolo)

        vm.enterAsCollector(jaime)

        assertEquals(Session.Collector(jaime), vm.session.value)
    }

    @Test
    fun cambiarDeColeccionistaAVisitanteNoDejaRestosDelAnterior() {
        val vm = SessionViewModel()
        vm.enterAsCollector(manolo)

        vm.enterAsVisitor()

        assertEquals(Session.Visitor, vm.session.value)
        assertFalse(vm.session.value.collectorActionsEnabled)
    }

    @Test
    fun cambiarDeVisitanteAColeccionistaHabilitaLasAcciones() {
        val vm = SessionViewModel()
        vm.enterAsVisitor()

        vm.enterAsCollector(jaime)

        assertEquals(Session.Collector(jaime), vm.session.value)
        assertTrue(vm.session.value.collectorActionsEnabled)
    }

    @Test
    fun clearDejaLaSesionSinPerfil() {
        val vm = SessionViewModel()
        vm.enterAsCollector(manolo)

        vm.clear()

        assertEquals(Session.None, vm.session.value)
        assertFalse(vm.session.value.collectorActionsEnabled)
    }

    @Test
    fun clearSinSesionNoCambiaNada() {
        val vm = SessionViewModel()

        vm.clear()

        assertEquals(Session.None, vm.session.value)
    }
}
