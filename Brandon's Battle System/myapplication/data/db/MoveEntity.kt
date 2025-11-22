package geomon.myapplication.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
// Move Entry Variable in the database
@Entity(tableName = "moves")
data class MoveEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type1: String?,
    val type2: String?,
    val type3: String?,
    val baseDamage: Float,
    val category: String,
    val accuracy: Float,
    val priority: Int = 0,
    val ppMax: Int = 20,
    val healing: Float,
    val otherEffect: String? = null
)
