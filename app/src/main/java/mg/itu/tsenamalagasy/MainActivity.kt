package mg.itu.tsenamalagasy

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inbox
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
            TsenaMalagasyTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
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

    NavHost(navController = navController, startDestination = "accueil") {

        composable("accueil") {
            EcranListe(
                viewModel = viewModel,
                navController = navController,
                onAnnonceClick = { id -> navController.navigate("detail/$id") },
                onPublierClick = { navController.navigate("publier") },
            )
        }

        composable("mesPublications") {
            EcranMesPublications(
                viewModel = viewModel,
                navController = navController,
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

        composable("profil") {
            EcranProfil(
                viewModel = viewModel,
                navController = navController,
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

/** Barre de navigation basse partagée entre Accueil et Mes publications. */
@Composable
fun BarreNavigation(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val routeActuelle = backStackEntry?.destination?.route

    NavigationBar {
        NavigationBarItem(
            selected = routeActuelle == "accueil",
            onClick = {
                navController.navigate("accueil") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Filled.Home, contentDescription = null) },
            label = { Text("Accueil") },
        )
        NavigationBarItem(
            selected = routeActuelle == "mesPublications",
            onClick = {
                navController.navigate("mesPublications") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Filled.Storefront, contentDescription = null) },
            label = { Text("Mes publications") },
        )
        NavigationBarItem(
            selected = routeActuelle == "profil",
            onClick = {
                navController.navigate("profil") {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
            icon = { Icon(Icons.Filled.Person, contentDescription = null) },
            label = { Text("Profil") },
        )
    }
}

// ----------------------------------------------------------------------------
// ÉCRAN 1 — ACCUEIL : toutes les annonces, filtrer par village, trier
// ----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranListe(
    viewModel: AnnonceViewModel,
    navController: NavHostController,
    onAnnonceClick: (Int) -> Unit,
    onPublierClick: () -> Unit,
) {
    val etat by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Tsena Malagasy", fontWeight = FontWeight.Bold)
                        Text(
                            "${etat.annonces.size} annonce(s) disponible(s)",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        bottomBar = { BarreNavigation(navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onPublierClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Publier") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {

            Spacer(Modifier.height(12.dp))

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
            Spacer(Modifier.height(10.dp))

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
            Spacer(Modifier.height(16.dp))

            if (etat.annonces.isEmpty()) {
                EtatVide(
                    icone = Icons.Filled.Inbox,
                    message = "Aucune annonce pour ce village",
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(etat.annonces) { a ->
                        CarteAnnonce(a, onClick = { onAnnonceClick(a.id) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun EtatVide(icone: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Column(
        Modifier.fillMaxWidth().padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            icone,
            contentDescription = null,
            modifier = Modifier.size(56.dp),
            tint = MaterialTheme.colorScheme.outline,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun CarteAnnonce(annonce: Annonce, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(
            Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(emojiPourProduit(annonce.nomProduit), style = MaterialTheme.typography.titleLarge)
            }
            Spacer(Modifier.width(12.dp))

            Column(Modifier.weight(1f)) {
                Text(annonce.nomProduit, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "${annonce.quantiteKg} kg — ${annonce.village}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    "${annonce.nomProducteur} · ${annonce.dateISO}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Spacer(Modifier.width(8.dp))

            PrixBadge(annonce.prixKg)
        }
    }
}

@Composable
fun PrixBadge(prixKg: Double?) {
    val (couleurFond, couleurTexte, texte) = if (prixKg != null) {
        Triple(
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer,
            "${formatAriary(prixKg)}/kg",
        )
    } else {
        Triple(
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            "à négocier",
        )
    }
    Surface(color = couleurFond, shape = RoundedCornerShape(10.dp)) {
        Text(
            texte,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = couleurTexte,
        )
    }
}

// ----------------------------------------------------------------------------
// ÉCRAN 2 — MES PUBLICATIONS : mes annonces, avec ajustement du stock
// ----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranMesPublications(
    viewModel: AnnonceViewModel,
    navController: NavHostController,
    onAnnonceClick: (Int) -> Unit,
    onPublierClick: () -> Unit,
) {
    val mesAnnonces by viewModel.mesAnnonces.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mes publications", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        bottomBar = { BarreNavigation(navController) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onPublierClick,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Publier") },
            )
        },
    ) { padding ->
        Column(Modifier.padding(padding).padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))

            if (mesAnnonces.isEmpty()) {
                EtatVide(
                    icone = Icons.Filled.Storefront,
                    message = "Vous n'avez encore publié aucune annonce",
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(mesAnnonces) { a ->
                        CarteMaPublication(
                            annonce = a,
                            onClick = { onAnnonceClick(a.id) },
                            onStockChange = { nouvelleQuantite -> viewModel.modifierStock(a, nouvelleQuantite) },
                        )
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun CarteMaPublication(
    annonce: Annonce,
    onClick: () -> Unit,
    onStockChange: (Double) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { onClick() },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(48.dp)
                        .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(emojiPourProduit(annonce.nomProduit), style = MaterialTheme.typography.titleLarge)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(annonce.nomProduit, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        annonce.village,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(8.dp))
                if (annonce.vendue) {
                    Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                        Text(
                            "Vendue",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                } else {
                    PrixBadge(annonce.prixKg)
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "Stock disponible",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onStockChange(annonce.quantiteKg - 1.0) },
                        enabled = !annonce.vendue && annonce.quantiteKg > 0.0,
                    ) {
                        Icon(Icons.Filled.Remove, contentDescription = "Diminuer le stock")
                    }
                    Text(
                        "${annonce.quantiteKg} kg",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(64.dp),
                    )
                    IconButton(
                        onClick = { onStockChange(annonce.quantiteKg + 1.0) },
                        enabled = !annonce.vendue,
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Augmenter le stock")
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// ÉCRAN 3 — PROFIL : identité du producteur (pré-remplit Publier) + stats
// ----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranProfil(
    viewModel: AnnonceViewModel,
    navController: NavHostController,
) {
    val profilNom by viewModel.profilNom.collectAsState()
    val profilTelephone by viewModel.profilTelephone.collectAsState()
    val mesAnnonces by viewModel.mesAnnonces.collectAsState()
    val context = LocalContext.current

    var nom by remember(profilNom) { mutableStateOf(profilNom) }
    var telephone by remember(profilTelephone) { mutableStateOf(profilTelephone) }

    val actives = mesAnnonces.count { !it.vendue }
    val vendues = mesAnnonces.count { it.vendue }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        bottomBar = { BarreNavigation(navController) },
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(
                    Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier
                            .size(72.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(36.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        profilNom.ifBlank { "Producteur" },
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    if (profilTelephone.isNotBlank()) {
                        Text(
                            profilTelephone,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CarteStat(modifier = Modifier.weight(1f), valeur = actives.toString(), libelle = "Actives")
                CarteStat(modifier = Modifier.weight(1f), valeur = vendues.toString(), libelle = "Vendues")
                CarteStat(modifier = Modifier.weight(1f), valeur = mesAnnonces.size.toString(), libelle = "Total")
            }

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("Mes coordonnées", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Utilisées pour pré-remplir vos prochaines annonces.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        nom, { nom = it },
                        label = { Text("Votre nom") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        telephone, { telephone = it },
                        label = { Text("Téléphone") },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            viewModel.enregistrerProfil(nom, telephone)
                            Toast.makeText(context, "Profil enregistré", Toast.LENGTH_SHORT).show()
                        },
                        enabled = nom.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text("Enregistrer")
                    }
                }
            }
        }
    }
}

@Composable
fun CarteStat(modifier: Modifier = Modifier, valeur: String, libelle: String) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp)) {
        Column(
            Modifier.fillMaxWidth().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(valeur, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Text(libelle, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ----------------------------------------------------------------------------
// ÉCRAN 4 — DÉTAIL : voir l'annonce, contacter le producteur
// ----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Retour")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
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

        Column(Modifier.padding(padding).padding(16.dp)) {

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(56.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(emojiPourProduit(a.nomProduit), style = MaterialTheme.typography.headlineSmall)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(a.nomProduit, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    a.village,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        PrixBadge(a.prixKg)
                        Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(10.dp)) {
                            Text(
                                "${a.quantiteKg} kg disponibles",
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Publiée le ${a.dateISO}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("Producteur", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(a.nomProducteur, style = MaterialTheme.typography.bodyLarge)
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Phone, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(a.telephone, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    // Intent implicite : appeler le producteur (patron du mini-TP 3)
                    val intent = Intent(Intent.ACTION_DIAL, "tel:${a.telephone}".toUri())
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Icon(Icons.Filled.Call, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Appeler le producteur")
            }

            if (a.estMienne) {
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        viewModel.marquerVendue(a)
                        Toast.makeText(context, "Annonce marquée comme vendue", Toast.LENGTH_SHORT).show()
                        onRetour()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Marquer comme vendue")
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------
// ÉCRAN 5 — PUBLIER : formulaire producteur (state hoisting local + remember)
// ----------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EcranPublier(
    viewModel: AnnonceViewModel,
    onTermine: () -> Unit,
) {
    val profilNom by viewModel.profilNom.collectAsState()
    val profilTelephone by viewModel.profilTelephone.collectAsState()

    var nomProduit by remember { mutableStateOf("") }
    var village by remember { mutableStateOf("") }
    var quantite by remember { mutableStateOf("") }
    var prix by remember { mutableStateOf("") }
    var nomProducteur by remember { mutableStateOf(profilNom) }
    var telephone by remember { mutableStateOf(profilTelephone) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Publier une annonce") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    OutlinedTextField(
                        nomProduit, { nomProduit = it },
                        label = { Text("Produit (ex. Vanille)") },
                        leadingIcon = { Icon(Icons.Filled.ShoppingBasket, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        village, { village = it },
                        label = { Text("Village") },
                        leadingIcon = { Icon(Icons.Filled.LocationOn, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        quantite, { quantite = it },
                        label = { Text("Quantité (kg)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        prix, { prix = it },
                        label = { Text("Prix / kg (laisser vide = à négocier)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        nomProducteur, { nomProducteur = it },
                        label = { Text("Votre nom") },
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        telephone, { telephone = it },
                        label = { Text("Téléphone") },
                        leadingIcon = { Icon(Icons.Filled.Phone, contentDescription = null) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

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
                    Toast.makeText(context, "Annonce publiée !", Toast.LENGTH_SHORT).show()
                    onTermine()
                },
                enabled = nomProduit.isNotBlank() && village.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("Publier l'annonce")
            }
        }
    }
}

/** Émoji évocateur du produit, pour l'avatar des cartes (aucune image à héberger). */
fun emojiPourProduit(nomProduit: String): String = when {
    nomProduit.contains("vanille", ignoreCase = true) -> "🌿"
    nomProduit.contains("riz", ignoreCase = true) -> "🌾"
    nomProduit.contains("caf", ignoreCase = true) -> "☕"
    nomProduit.contains("girofle", ignoreCase = true) -> "🌸"
    nomProduit.contains("litchi", ignoreCase = true) -> "🍒"
    nomProduit.contains("banane", ignoreCase = true) -> "🍌"
    nomProduit.contains("mangue", ignoreCase = true) -> "🥭"
    nomProduit.contains("ananas", ignoreCase = true) -> "🍍"
    else -> "🧺"
}

/** Formate un montant en ariary : 1250000.0 -> "1 250 000 Ar" (mini-TP 1). */
fun formatAriary(montant: Double): String {
    val entier = montant.toLong().toString()
    val groupes = entier.reversed().chunked(3).joinToString(" ").reversed()
    return "$groupes Ar"
}
