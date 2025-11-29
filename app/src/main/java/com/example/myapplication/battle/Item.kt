package com.example.myapplication.items

import android.content.Context
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.data.db.ItemEntity
//item object. contains more variables for future implementations
class Item(
    val id: String,
    val displayName: String,
    val healing: Int,
    val sideAffect: Int,
    val effect: String,
    val displayDescription: String,
    val iconRoot: String,
    val colorDisplay: String
) {

    companion object {

        //  By the displayname, load from room
        suspend fun searchByDisplayName(context: Context, displayName: String): Item? {
            val db = AppDatabase.get(context)
            val dao = db.speciesDao()
            val entity: ItemEntity = dao.getItemByDisplayNameNow(displayName) ?: return null
            return fromEntity(entity)
        }

        fun fromEntity(entity: ItemEntity): Item = Item(
            id = entity.id,
            displayName = entity.displayName,
            healing = entity.healing,
            sideAffect = entity.sideAffect,
            effect = entity.effect,
            displayDescription = entity.displayDescription,
            iconRoot = entity.iconRoot,
            colorDisplay = entity.colorDisplay
        )
    }

    //depending on the effect conversion
    fun returnAffect(): Int? {
        return when (effect) {
            "healing" -> healing
            "candy1"  -> 1
            else      -> null
        }
    }
}
