package com.example.myapplication

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlin.random.Random

class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var permissionHandler: PermissionHandler
    private val playerLocation = LatLng(49.2606, -123.2460)
    private var mapReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Initialize permission handler
        permissionHandler = PermissionHandler(this)

        // Check permissions before initializing map
        if (permissionHandler.checkAndRequestPermissions()) {
            initializeMap()
        }
        // If permissions not granted, initializeMap() will be called
        // after user grants permissions in onRequestPermissionsResult
    }

    private fun initializeMap() {
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        permissionHandler.handlePermissionResult(
            requestCode,
            permissions,
            grantResults,
            onGranted = {
                // Permission granted - initialize map
                if (!mapReady) {
                    initializeMap()
                }
            },
            onDenied = {
                // Permission denied - you could close the app or show explanation
                Toast.makeText(
                    this,
                    "App cannot function without location permission",
                    Toast.LENGTH_LONG
                ).show()
                // Optionally: finish() to close the app
            }
        )
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        mapReady = true

        googleMap.addMarker(
            MarkerOptions()
                .position(playerLocation)
                .title("You")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )

        val monsters = generateMonstersAroundPlayer()
        spawnMonstersOnMap(monsters)

        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(playerLocation, 15f))

        googleMap.setOnMarkerClickListener { marker ->
            val monster = marker.tag as? Monster
            if (monster != null) {
                Toast.makeText(
                    this,
                    "Monster: ${monster.name}\nLevel: ${monster.level}\nID: ${monster.id}",
                    Toast.LENGTH_LONG
                ).show()
                true
            } else {
                false
            }
        }
    }

    data class Monster(
        val id: String,
        val name: String,
        val level: Int,
        val latitude: Double,
        val longitude: Double
    )

    private fun generateMonstersAroundPlayer(): List<Monster> {
        val monsters = mutableListOf<Monster>()
        val gridSize = 0.002
        val range = 2

        for (dx in -range..range) {
            for (dy in -range..range) {
                if (dx == 0 && dy == 0) continue

                val cellLat = playerLocation.latitude + (dy * gridSize)
                val cellLon = playerLocation.longitude + (dx * gridSize)
                val cellX = ((cellLon / gridSize) * 1000).toInt()
                val cellY = ((cellLat / gridSize) * 1000).toInt()
                val seed = ((cellX.toLong() and 0xFFFFFFFF) shl 32) or (cellY.toLong() and 0xFFFFFFFF)
                val random = kotlin.random.Random(seed)
                val monstersInCell = random.nextInt(1, 3)

                for (i in 0 until monstersInCell) {
                    val latOffset = (random.nextDouble() - 0.5) * gridSize * 0.8
                    val lonOffset = (random.nextDouble() - 0.5) * gridSize * 0.8
                    val monsterLat = cellLat + latOffset
                    val monsterLon = cellLon + lonOffset
                    val level = random.nextInt(1, 21)

                    monsters.add(
                        Monster(
                            id = "${cellX}_${cellY}_$i",
                            name = "Slime",
                            level = level,
                            latitude = monsterLat,
                            longitude = monsterLon
                        )
                    )
                }
            }
        }
        return monsters
    }

    private fun spawnMonstersOnMap(monsters: List<Monster>) {
        monsters.forEach { monster ->
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(monster.latitude, monster.longitude))
                    .title(monster.name)
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            marker?.tag = monster
        }
    }
}