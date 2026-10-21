package com.example.vinilos.ui.artistas

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.vinilos.R
import com.example.vinilos.data.model.Session
import com.example.vinilos.ui.ingreso.EncabezadoVinilo

const val TagArtistas = "artistas"
const val TagRolArtistas = "artistas_rol"

/** Destino provisional del listado de artistas: HU03 lo reemplaza por el listado real. */
@Composable
fun PantallaArtistasProvisional(
    session: Session,
    onAtras: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(TagArtistas)
    ) {
        EncabezadoVinilo(modifier = Modifier.padding(horizontal = 12.dp), onAtras = onAtras)
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.artistas_titulo),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = when (session) {
                    Session.None -> stringResource(R.string.catalogo_rol_ninguno)
                    Session.Visitor -> stringResource(R.string.catalogo_rol_visitante)
                    is Session.Collector ->
                        stringResource(R.string.catalogo_rol_coleccionista, session.collector.name)
                },
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag(TagRolArtistas)
            )
        }
    }
}
