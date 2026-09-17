@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package mg.itu.tsenamalagasy

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Tsena Malagasy — écran unique d'entrée.
 *
 * Structure reprise du mini-TP 5/6/7 : un seul ViewModel partagé, créé au
 * niveau de AppNavigation() et injecté dans chaque écran — l'état survit
 * donc à la navigation ET à la rotation (mini-TP 3).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val viewModel: AnnonceViewModel = viewModel()

    NavHost(navController = navController, startDestination = "liste") {

        composable("liste") {
            EcranListe(
                viewModel = viewModel,
                onAnnonceClick = { id -> navController.navigate("detail/$id") },
                onPublierClick = { navController.navigate("publier") },
            )
        }

        composable("detail/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")?.toIntOrNull()
            EcranDetail(
                id = id,
                viewModel = viewModel,
                onRetour = { navController.popBackStack() },
            )
        }

        composable("publier") {
            EcranPublier(
                viewModel = viewModel,
                onTermine = { navController.popBackStack() },
            )
        }
    }
}

// ----------------------------------------------------------------------------
// ÉCRAN 1 — LISTE : parcourir les annonces, filtrer par village, trier
// ----------------------------------------------------------------------------

@Composable
fun EcranListe(
    viewModel: AnnonceViewModel,
    onAnnonceClick: (Int) -> Unit,
    onPublierClick: () -> Unit,
) {
    val etat by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Tsena Malagasy") })
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onPublierClick) { Text("+") }
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {

            Text(
                "${etat.annonces.size} annonce(s) disponible(s)",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))

            // --- Tri (when / FilterChip, comme mini-TP 7) ---
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = etat.mode == ModeTri.RECENTES,
                    onClick = { viewModel.changerMode(ModeTri.RECENTES) },
                    label = { Text("Récentes") },
                )
                FilterChip(
                    selected = etat.mode == ModeTri.PRIX_CROISSANT,
                    onClick = { viewModel.changerMode(ModeTri.PRIX_CROISSANT) },
                    label = { Text("Prix croissant") },
                )
            }
            Spacer(Modifier.height(8.dp))

            // --- Filtre par village ---
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilterChip(
                        selected = etat.villageFiltre == null,
                        onClick = { viewModel.changerVillage(null) },
                        label = { Text("Tous les villages") },
                    )
                }
                items(etat.villages) { v ->
                    FilterChip(
                        selected = etat.villageFiltre == v,
                        onClick = { viewModel.changerVillage(v) },
                        label = { Text(v) },
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            LazyColumn {
                items(etat.annonces) { a ->
                    CarteAnnonce(a, onClick = { onAnnonceClick(a.id) })
                }
            }
        }
    }
}

@Composable
fun CarteAnnonce(annonce: Annonce, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(annonce.nomProduit, style = MaterialTheme.typography.titleMedium)
                Text(
                    "${annonce.quantiteKg} kg — ${annonce.village}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    annonce.nomProducteur,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            // Null safety (?.let / ?:), directement issu du mini-TP 1 : formatAriary
            Text(
                annonce.prixKg?.let { "${formatAriary(it)}/kg" } ?: "à négocier",
                style = MaterialTheme.typography.titleMedium,
                color = if (annonce.prixKg != null) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.tertiary,
            )
        }
    }
}

// ----------------------------------------------------------------------------
// ÉCRAN 2 — DÉTAIL : voir l'annonce, contacter le producteur
// ----------------------------------------------------------------------------

@Composable
fun EcranDetail(
    id: Int?,
    viewModel: AnnonceViewModel,
    onRetour: () -> Unit,
) {
    var annonce by remember { mutableStateOf<Annonce?>(null) }
    androidx.compose.runtime.LaunchedEffect(id) {
        if (id != null) annonce = viewModel.charger(id)
    }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(annonce?.nomProduit ?: "Détail") },
                navigationIcon = {
                    IconButton(onClick = onRetour) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
            )
        }
    ) { padding ->
        val a = annonce
        if (a == null) {
            Column(Modifier.padding(padding).padding(24.dp)) {
                Text("Chargement…")
            }
            return@Scaffold
        }

        Column(Modifier.padding(padding).padding(24.dp)) {
            Text(a.nomProduit, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text("Village : ${a.village}", style = MaterialTheme.typography.bodyLarge)
            Text("Quantité disponible : ${a.quantiteKg} kg", style = MaterialTheme.typography.bodyLarge)
            Text(
                a.prixKg?.let { "Prix : ${formatAriary(it)} / kg" } ?: "Prix : à négocier",
                style = MaterialTheme.typography.bodyLarge,
            )
            Spacer(Modifier.height(16.dp))
            Text("Producteur", style = MaterialTheme.typography.titleMedium)
            Text(a.nomProducteur, style = MaterialTheme.typography.bodyLarge)
            Text(a.telephone, style = MaterialTheme.typography.bodyLarge)

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    // Intent implicite : appeler le producteur (patron du mini-TP 3)
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${a.telephone}"))
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(Icons.Filled.Call, contentDescription = null)
                Spacer(Modifier.height(0.dp))
                Text("  Appeler le producteur")
            }

            Spacer(Modifier.height(12.dp))
            Button(
                onClick = { viewModel.marquerVendue(a); onRetour() },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Marquer comme vendue")
            }
        }
    }
}

// ----------------------------------------------------------------------------
// ÉCRAN 3 — PUBLIER : formulaire producteur (state hoisting local + remember)
// ----------------------------------------------------------------------------

@Composable
fun EcranPublier(
    viewModel: AnnonceViewModel,
    onTermine: () -> Unit,
) {
    var nomProduit by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var quantite by remember { mutableStateOf("") }
    var prix by remember { mutableStateOf("") }
    var nomProducteur by remember { mutableStateOf("") }
    var telephone by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Publier une annonce") }) }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(nomProduit, { nomProduit = it }, label = { Text("Produit (ex. Vanille)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(village, { village = it }, label = { Text("Village") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(quantite, { quantite = it }, label = { Text("Quantité (kg)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(prix, { prix = it }, label = { Text("Prix / kg (laisser vide = à négocier)") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(nomProducteur, { nomProducteur = it }, label = { Text("Votre nom") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(telephone, { telephone = it }, label = { Text("Téléphone") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    viewModel.publier(
                        nomProduit = nomProduit,
                        village = village,
                        quantiteKg = quantite.toDoubleOrNull() ?: 0.0,
                        prixKg = prix.toDoubleOrNull(),      // vide -> null -> "à négocier"
                        nomProducteur = nomProducteur,
                        telephone = telephone,
                        dateISO = "2026-09-17",
                    )
                    onTermine()
                },
                enabled = nomProduit.isNotBlank() && village.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Publier l'annonce")
            }
        }
    }
}

/** Formate un montant en ariary : 1250000.0 -> "1 250 000 Ar" (mini-TP 1). */
fun formatAriary(montant: Double): String {
    val entier = montant.toLong().toString()
    val groupes = entier.reversed().chunked(3).joinToString(" ").reversed()
    return "$groupes Ar"
}
