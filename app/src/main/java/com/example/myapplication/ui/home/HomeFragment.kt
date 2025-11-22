package com.example.myapplication.ui.home

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.myapplication.R
import com.example.myapplication.battle.ui.BattleActivity
import com.example.myapplication.data.Seeder
import com.example.myapplication.data.SpeciesRepository
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.databinding.FragmentHomeBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.random.Random

class HomeFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var googleMap: GoogleMap
    private lateinit var repository: SpeciesRepository
    private val playerLocation = LatLng(49.2606, -123.2460) // Surrey, BC
    private var availableSpeciesIds: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // Initialize database and repository
        val db = AppDatabase.get(requireContext())
        repository = SpeciesRepository(db.speciesDao())

        // Seed database
        lifecycleScope.launch(Dispatchers.IO) {
            Seeder.run(requireContext(), repository)
            Log.d("GeoMon", "Database transferred")
            
            // Load available species IDs
            repository.allSpecies().collect { speciesList ->
                availableSpeciesIds = speciesList.map { it.id }
                Log.d("GeoMon", "Loaded ${availableSpeciesIds.size} species")
            }
        }

        // Initialize map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        return root
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Add player marker
        googleMap.addMarker(
            MarkerOptions()
                .position(playerLocation)
                .title("You")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))
        )

        // Generate and spawn monsters
        val monsters = generateMonstersAroundPlayer()
        spawnMonstersOnMap(monsters)

        // Move camera to player
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(playerLocation, 15f))

        // Handle marker clicks
        googleMap.setOnMarkerClickListener { marker ->
            val monster = marker.tag as? Monster
            if (monster != null) {
                // Show toast
                Toast.makeText(
                    requireContext(),
                    "Encountered ${monster.name} (Lv.${monster.level})!",
                    Toast.LENGTH_SHORT
                ).show()

                // Launch battle
                val intent = Intent(requireContext(), BattleActivity::class.java).apply {
                    putExtra("ENEMY_SPECIES_ID", monster.speciesId)
                    putExtra("ENEMY_LEVEL", monster.level)
                }
                startActivity(intent)
                true
            } else {
                false
            }
        }
    }

    data class Monster(
        val id: String,
        val speciesId: String,
        val name: String,
        val level: Int,
        val latitude: Double,
        val longitude: Double
    )

    private fun generateMonstersAroundPlayer(): List<Monster> {
        if (availableSpeciesIds.isEmpty()) {
            Log.d("GeoMon", "No species available yet")
            return emptyList()
        }

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
                val random = Random(seed)
                val monstersInCell = random.nextInt(1, 3)

                for (i in 0 until monstersInCell) {
                    val latOffset = (random.nextDouble() - 0.5) * gridSize * 0.8
                    val lonOffset = (random.nextDouble() - 0.5) * gridSize * 0.8
                    val monsterLat = cellLat + latOffset
                    val monsterLon = cellLon + lonOffset
                    val level = random.nextInt(1, 21)

                    val speciesId = availableSpeciesIds.random(random)
                    val speciesName = speciesId.replaceFirstChar { it.uppercase() }

                    monsters.add(
                        Monster(
                            id = "${cellX}_${cellY}_$i",
                            speciesId = speciesId,
                            name = speciesName,
                            level = level,
                            latitude = monsterLat,
                            longitude = monsterLon
                        )
                    )
                }
            }
        }
        
        Log.d("GeoMon", "Generated ${monsters.size} monsters")
        return monsters
    }

    private fun spawnMonstersOnMap(monsters: List<Monster>) {
        monsters.forEach { monster ->
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(monster.latitude, monster.longitude))
                    .title("${monster.name} (Lv.${monster.level})")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )
            marker?.tag = monster
        }
        Log.d("GeoMon", "Spawned ${monsters.size} markers")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
