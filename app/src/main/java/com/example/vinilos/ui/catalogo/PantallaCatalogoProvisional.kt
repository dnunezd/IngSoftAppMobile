package com.example.vinilos.ui.catalogo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
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

const val TagCatalogo = "catalogo"
const val TagRolActivo = "catalogo_rol"
const val TagCatalogoAlbumProvisional = "catalogo_album_provisional"

/**
 * Álbum usado por el enlace provisional mientras HU01 no tiene lista real.
 * Corresponde a "Buscando América" en los datos de ejemplo de BackVynils
 * (ids de álbum empiezan en 100, igual que collectors/artistas).
 */
const val AlbumIdProvisional = 100

/** Destino provisional del ingreso: HU01 lo reemplaza por el catálogo real. */
@Composable
fun PantallaCatalogoProvisional(
    session: Session,
    onAtras: () -> Unit,
    onSeleccionarAlbum: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .testTag(TagCatalogo)
    ) {
        EncabezadoVinilo(modifier = Modifier.padding(horizontal = 12.dp), onAtras = onAtras)
        Column(modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(
                text = stringResource(R.string.catalogo_titulo),
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
                modifier = Modifier.testTag(TagRolActivo)
            )
            Text(
                text = stringResource(R.string.catalogo_album_provisional_nota),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp)
            )
            Button(
                onClick = { onSeleccionarAlbum(AlbumIdProvisional) },
                modifier = Modifier.padding(top = 8.dp).testTag(TagCatalogoAlbumProvisional)
            ) {
                Text(stringResource(R.string.catalogo_album_provisional_boton))
            }
        }
    }
}
