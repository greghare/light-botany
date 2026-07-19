package com.thelightphone.botany

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase

@Entity(tableName = "plant_history")
data class PlantHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val commonName: String,
    val latinName: String,
    val family: String,
    val description: String,
    val isNative: Boolean,
    val isPoisonous: Boolean,
    val photoFileName: String,
    val dateIso: String,
    val dateLabel: String,
)

@Dao
interface PlantHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(entity: PlantHistoryEntity): Long

    @Query("SELECT * FROM plant_history ORDER BY id DESC")
    fun listAll(): List<PlantHistoryEntity>

    @Query("SELECT * FROM plant_history WHERE id = :id")
    fun get(id: Long): PlantHistoryEntity?
}

@Database(entities = [PlantHistoryEntity::class], version = 1, exportSchema = false)
abstract class PlantHistoryDatabase : RoomDatabase() {
    abstract fun plantHistoryDao(): PlantHistoryDao

    companion object {
        const val DATABASE_NAME = "botany_history.db"
    }
}
