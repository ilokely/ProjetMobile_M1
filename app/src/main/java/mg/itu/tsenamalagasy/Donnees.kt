package mg.itu.tsenamalagasy

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/**
 * Tsena Malagasy — couche données (Room).
 *
 * Reprend la structure enseignée en mini-TP 7 : une Entity par table,
 * un DAO pour les requêtes (exposées en Flow pour se recomposer toutes
 * seules), et une Database comme point d'assemblage (singleton).
 *
 * Le prix est nullable (Double?) : un producteur peut publier une
 * annonce "à négocier" sans prix fixé — même null safety que le
 * mini-TP 1 (Collectes.kt : Produit.prixKg).
 */

// ----------------------------------------------------------------------------
// ENTITIES
// ----------------------------------------------------------------------------

@Entity(tableName = "annonces")
data class Annonce(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nomProduit: String,
    val village: String,
    val quantiteKg: Double,
    val prixKg: Double?,           // null = "à négocier"
    val nomProducteur: String,
    val telephone: String,
    val dateISO: String,           // "2026-09-17"
    val vendue: Boolean = false,
    val estMienne: Boolean = false, // true = publiée depuis cet appareil (écran "Mes publications")
)

// ----------------------------------------------------------------------------
// DAO — requêtes exposées en Flow (ré-émission automatique à chaque écriture)
// ----------------------------------------------------------------------------

@Dao
interface AnnonceDao {

    /** Toutes les annonces actives, les plus récentes d'abord. */
    @Query("SELECT * FROM annonces WHERE vendue = 0 ORDER BY dateISO DESC, id DESC")
    fun annoncesActives(): Flow<List<Annonce>>

    /** Filtrées par village (recherche locale, cas d'usage principal de l'app). */
    @Query("SELECT * FROM annonces WHERE vendue = 0 AND village = :village ORDER BY dateISO DESC")
    fun parVillage(village: String): Flow<List<Annonce>>

    /** Triées par prix croissant ; les annonces sans prix (NULL) en dernier. */
    @Query("SELECT * FROM annonces WHERE vendue = 0 ORDER BY prixKg IS NULL, prixKg ASC")
    fun parPrixCroissant(): Flow<List<Annonce>>

    /** Une annonce précise (écran de détail). */
    @Query("SELECT * FROM annonces WHERE id = :id")
    suspend fun parId(id: Int): Annonce?

    /** Liste des villages distincts, pour construire les filtres. */
    @Query("SELECT DISTINCT village FROM annonces ORDER BY village ASC")
    fun villages(): Flow<List<String>>

    /** Mes propres annonces (publiées depuis cet appareil), vendues incluses. */
    @Query("SELECT * FROM annonces WHERE estMienne = 1 ORDER BY dateISO DESC, id DESC")
    fun mesAnnonces(): Flow<List<Annonce>>

    @Insert
    suspend fun inserer(annonce: Annonce): Long

    @Update
    suspend fun modifier(annonce: Annonce)

    @Query("DELETE FROM annonces WHERE id = :id")
    suspend fun supprimer(id: Int)
}

// ----------------------------------------------------------------------------
// DATABASE — point d'assemblage (singleton), identique au patron du cours
// ----------------------------------------------------------------------------

@Database(entities = [Annonce::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun annonceDao(): AnnonceDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun obtenir(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tsena_malagasy.db",
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { instance = it }
            }
    }
}

/** Jeu de données de démonstration, inséré au premier lancement. */
val annoncesInitiales = listOf(
    Annonce(nomProduit = "Vanille Bourbon", village = "Ambodivoara", quantiteKg = 8.0,
        prixKg = 250_000.0, nomProducteur = "RAKOTO Jean", telephone = "034 12 345 67",
        dateISO = "2026-09-15"),
    Annonce(nomProduit = "Riz Makalioka", village = "Ambodivoara", quantiteKg = 120.0,
        prixKg = 2_800.0, nomProducteur = "RANDRIA Paul", telephone = "033 98 765 43",
        dateISO = "2026-09-16"),
    Annonce(nomProduit = "Litchi", village = "Antsirabe Nord", quantiteKg = 60.0,
        prixKg = null, nomProducteur = "RASOA Marie", telephone = "032 55 112 20",
        dateISO = "2026-09-17"),
    Annonce(nomProduit = "Café Arabica", village = "Antsirabe Nord", quantiteKg = 35.0,
        prixKg = 12_000.0, nomProducteur = "RASOA Marie", telephone = "032 55 112 20",
        dateISO = "2026-09-14"),
    Annonce(nomProduit = "Girofle", village = "Ambodivoara", quantiteKg = 15.0,
        prixKg = 38_000.0, nomProducteur = "RAKOTO Jean", telephone = "034 12 345 67",
        dateISO = "2026-09-13"),
)
