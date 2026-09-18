package mg.itu.tsenamalagasy

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Tsena Malagasy — la couche qui porte l'état (mini-TP 6/7).
 *
 * L'état ne vit pas dans un remember{} (perdu à la rotation) mais dans un
 * StateFlow porté par le ViewModel : il survit à la rotation de l'écran et
 * aux allers-retours de navigation, exactement comme démontré au mini-TP 6.
 *
 * combine() fusionne plusieurs Flow Room (annonces + villages + mode de tri)
 * en un seul état d'écran, recalculé automatiquement à chaque changement —
 * même patron que ProduitsViewModel du mini-TP 7.
 */

enum class ModeTri { RECENTES, PRIX_CROISSANT }

data class EtatUi(
    val annonces: List<Annonce> = emptyList(),
    val villages: List<String> = emptyList(),
    val villageFiltre: String? = null,   // null = tous les villages
    val mode: ModeTri = ModeTri.RECENTES,
)

class AnnonceViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.obtenir(application).annonceDao()
    private val villageFiltre = MutableStateFlow<String?>(null)
    private val mode = MutableStateFlow(ModeTri.RECENTES)

    private val prefsProfil = application.getSharedPreferences("profil", Context.MODE_PRIVATE)
    private val _profilNom = MutableStateFlow(prefsProfil.getString("nom", "") ?: "")
    private val _profilTelephone = MutableStateFlow(prefsProfil.getString("telephone", "") ?: "")
    val profilNom: StateFlow<String> = _profilNom
    val profilTelephone: StateFlow<String> = _profilTelephone

    init {
        // Premier lancement : la base est vide -> on insère le jeu de démo.
        viewModelScope.launch {
            annoncesInitiales.forEach { dao.inserer(it) }
        }
    }

    val uiState: StateFlow<EtatUi> =
        combine(
            dao.annoncesActives(),
            dao.parPrixCroissant(),
            dao.villages(),
            villageFiltre,
            mode,
        ) { flux ->
            @Suppress("UNCHECKED_CAST")
            val parDate = flux[0] as List<Annonce>
            @Suppress("UNCHECKED_CAST")
            val parPrix = flux[1] as List<Annonce>
            @Suppress("UNCHECKED_CAST")
            val villages = flux[2] as List<String>
            val village = flux[3] as String?
            val modeCourant = flux[4] as ModeTri

            // Choix de la liste de base selon le mode (expression when, mini-TP 1).
            val base = when (modeCourant) {
                ModeTri.RECENTES -> parDate
                ModeTri.PRIX_CROISSANT -> parPrix
            }
            // Filtre par village en mémoire (opération de collection : filter).
            val liste = village?.let { v -> base.filter { it.village == v } } ?: base

            EtatUi(annonces = liste, villages = villages, villageFiltre = village, mode = modeCourant)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = EtatUi(),
        )

    /** Mes propres annonces (écran "Mes publications"), recalculée à chaque écriture. */
    val mesAnnonces: StateFlow<List<Annonce>> =
        dao.mesAnnonces().stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    fun changerVillage(village: String?) {
        villageFiltre.value = village
    }

    fun changerMode(nouveau: ModeTri) {
        mode.value = nouveau
    }

    /** Publier une nouvelle annonce (utilisée par l'écran "Publier"). */
    fun publier(
        nomProduit: String,
        village: String,
        quantiteKg: Double,
        prixKg: Double?,
        nomProducteur: String,
        telephone: String,
        dateISO: String,
    ) {
        viewModelScope.launch {
            dao.inserer(
                Annonce(
                    nomProduit = nomProduit,
                    village = village,
                    quantiteKg = quantiteKg,
                    prixKg = prixKg,
                    nomProducteur = nomProducteur,
                    telephone = telephone,
                    dateISO = dateISO,
                    estMienne = true,
                )
            )
        }
    }

    suspend fun charger(id: Int): Annonce? = dao.parId(id)

    fun marquerVendue(annonce: Annonce) {
        viewModelScope.launch {
            dao.modifier(annonce.copy(vendue = true))
        }
    }

    /** Ajuste le stock disponible d'une de mes annonces (écran "Mes publications"). */
    fun modifierStock(annonce: Annonce, nouvelleQuantite: Double) {
        viewModelScope.launch {
            dao.modifier(annonce.copy(quantiteKg = nouvelleQuantite.coerceAtLeast(0.0)))
        }
    }

    /** Enregistre le nom/téléphone du producteur, réutilisés pour pré-remplir l'écran Publier. */
    fun enregistrerProfil(nom: String, telephone: String) {
        prefsProfil.edit().putString("nom", nom).putString("telephone", telephone).apply()
        _profilNom.value = nom
        _profilTelephone.value = telephone
    }
}
