package com.mpazpro3.app.data

import com.mpazpro3.app.domain.ReglasContenido
import com.mpazpro3.app.model.Asignatura
import com.mpazpro3.app.model.EstadoAprobacion
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * RF-01 y RF-02 en modo local: casos de aprobación, rechazo y filtro que complementan
 * RepositorioLocalTest. Usa la base en memoria (AlmacenLocalMemoria) en lugar de Room.
 */
class ContenidosUtpLocalTest {

    private val almacen = AlmacenLocalMemoria()
    private val repo = RepositorioLocal(almacen, ahora = { 1_800_000_000_000L }, fechaDeHoy = { "24-11-2026" })

    private fun contenido(id: String) = almacen.datos.contenidos.first { it.id == id }

    @Test
    fun aprobar_unContenidoRechazado_loDejaAprobadoConLaNuevaObservacion() = runTest {
        assertEquals(EstadoAprobacion.RECHAZADO, contenido("C4").estado)

        val r = repo.aprobarContenido("C4", "Se corrigió el recurso")

        assertTrue(r.exito)
        assertEquals(EstadoAprobacion.APROBADO, contenido("C4").estado)
        assertEquals("Se corrigió el recurso", contenido("C4").observacionUtp)
    }

    @Test
    fun aprobar_sinObservacion_esValido_laObservacionSoloEsObligatoriaAlRechazar() = runTest {
        val r = repo.aprobarContenido("C1", "")

        assertTrue(r.exito)
        assertEquals(EstadoAprobacion.APROBADO, contenido("C1").estado)
        assertEquals("", contenido("C1").observacionUtp)
    }

    @Test
    fun aprobar_elMismoContenidoDosVeces_actualizaLaObservacionSinViolarRN04() = runTest {
        repo.aprobarContenido("C1", "Primera revisión")

        val r = repo.aprobarContenido("C1", "Segunda revisión")

        assertTrue(r.exito)
        assertEquals("Segunda revisión", contenido("C1").observacionUtp)
    }

    @Test
    fun rechazar_contenidoQueNoExiste_dice_noSeEncontro() = runTest {
        val r = repo.rechazarContenido("XX", "motivo")

        assertFalse(r.exito)
        assertEquals(ReglasContenido.MENSAJE_CONTENIDO_NO_ENCONTRADO, r.mensaje)
    }

    @Test
    fun filtrar_despuesDeAprobar_elContenidoCambiaDeFiltro() = runTest {
        repo.aprobarContenido("C1", "ok")
        val contenidos = almacen.datos.contenidos

        val aprobadosLenguaje = ReglasContenido.filtrar(contenidos, Asignatura.LENGUAJE, EstadoAprobacion.APROBADO)
        val pendientesLenguaje = ReglasContenido.filtrar(contenidos, Asignatura.LENGUAJE, EstadoAprobacion.PENDIENTE)

        assertEquals(listOf("C1"), aprobadosLenguaje.map { it.id })
        assertTrue(pendientesLenguaje.isEmpty())
    }

    @Test
    fun filtrar_sinFiltros_devuelveTodosConLosPendientesPrimero() = runTest {
        val todos = ReglasContenido.filtrar(almacen.datos.contenidos, null, null)

        assertEquals(4, todos.size)
        assertEquals(EstadoAprobacion.PENDIENTE, todos.first().estado)
        assertTrue(todos.takeWhile { it.estado == EstadoAprobacion.PENDIENTE }.size == 2)
    }
}
