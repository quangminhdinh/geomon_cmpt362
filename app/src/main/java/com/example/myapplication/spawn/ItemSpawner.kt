package com.example.myapplication.spawn

import com.google.android.gms.maps.model.LatLng
import kotlin.random.Random

object ItemSpawner {

    private const val gridSize = 0.002
    private const val range = 2

    fun generateItemsAround(playerLocation: LatLng): List<ItemSpawn> {
        val items = mutableListOf<ItemSpawn>()

        for (dx in -range..range) {
            for (dy in -range..range) {
                if (dx == 0 && dy == 0) continue

                val cellLat = playerLocation.latitude + (dy * gridSize)
                val cellLon = playerLocation.longitude + (dx * gridSize)

                val cellX = ((cellLon / gridSize) * 1000).toInt()
                val cellY = ((cellLat / gridSize) * 1000).toInt()
                val seed = ((cellX.toLong() and 0xFFFFFFFF) shl 32) or (cellY.toLong() and 0xFFFFFFFF)

                val random = Random(seed)


                if (random.nextDouble() > 0.20) continue

                val itemsInCell = random.nextInt(0, 2)

                for (i in 0 until itemsInCell) {
                    val latOffset = (random.nextDouble() - 0.5) * gridSize * 0.8
                    val lonOffset = (random.nextDouble() - 0.5) * gridSize * 0.8

                    val itemLat = cellLat + latOffset
                    val itemLon = cellLon + lonOffset

                    val itemName: String
                    if (random.nextBoolean()) {
                        itemName = "Health Potion"
                    } else {
                        itemName = "Super Health Potion"
                    }

                    items.add(
                        ItemSpawn(
                            id = "ITEM_${cellX}_${cellY}_$i",
                            name = itemName,
                            latitude = itemLat,
                            longitude = itemLon
                        )
                    )
                }
            }
        }

        return items
    }
}
