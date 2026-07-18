package com.thelightphone.bible

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(
    tableName = "highlights",
    primaryKeys = ["bookIdx", "chapter", "verse"],
)
data class HighlightEntity(
    val bookIdx: Int,
    val book: String,
    val chapter: Int,
    val verse: Int,
    val text: String,
    val dateIso: String,
)

@Dao
interface HighlightDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsertAll(highlights: List<HighlightEntity>)

    @Query("DELETE FROM highlights WHERE bookIdx = :bookIdx AND chapter = :chapter AND verse IN (:verses)")
    fun delete(bookIdx: Int, chapter: Int, verses: List<Int>)

    @Query("SELECT verse FROM highlights WHERE bookIdx = :bookIdx AND chapter = :chapter")
    fun highlightedVerses(bookIdx: Int, chapter: Int): List<Int>

    @Query("SELECT * FROM highlights ORDER BY bookIdx ASC, chapter ASC, verse ASC")
    fun listAll(): List<HighlightEntity>
}

@Database(entities = [HighlightEntity::class], version = 1, exportSchema = false)
abstract class HighlightDatabase : RoomDatabase() {
    abstract fun highlightDao(): HighlightDao

    companion object {
        const val DATABASE_NAME = "bible_highlights.db"
    }
}
