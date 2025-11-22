package com.example.myapplication.ui.home

import android.content.Context.BIND_AUTO_CREATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
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
import com.example.myapplication.services.tracking.TrackingService
import com.example.myapplication.services.tracking.TrackingViewModel
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.random.Random

class HomeFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    
    private lateinit var googleMap: GoogleMap
    private lateinit var  playerMarkerOptions: MarkerOptions
    private var playerMarker: Marker? = null
    private val zoomValue = 15f

    private lateinit var trackingViewModel: TrackingViewModel
    private lateinit var serviceIntent: Intent
    private lateinit var repository: SpeciesRepository
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
        serviceIntent = Intent(requireActivity(), TrackingService::class.java)
        trackingViewModel = ViewModelProvider(requireActivity()).get(TrackingViewModel::class.java)

        mapFragment?.getMapAsync(this)

        return root
    }

    fun initTrackingService() {
        if (!(trackingViewModel.serviceStarted.value!!)) {
            requireActivity().startService(serviceIntent)
            trackingViewModel.serviceStarted.value = true
        }
        requireActivity()
            .bindService(serviceIntent, trackingViewModel, BIND_AUTO_CREATE)

        trackingViewModel.latLng.observe(this, Observer { it -> updateMap(it) })
    }

    fun updateMap(latLng: LatLng) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
            latLng, zoomValue)
        googleMap.animateCamera(cameraUpdate)
        playerMarker?.remove()
        playerMarkerOptions.position(latLng)
        playerMarker = googleMap.addMarker(playerMarkerOptions)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        playerMarkerOptions = MarkerOptions()
            .title("You")
            .icon(BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_AZURE))






        // Generate and spawn monsters
        val monsters = generateMonstersAroundPlayer()
        spawnMonstersOnMap(monsters)

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

                // Launch battle with monster names
                val intent = Intent(requireContext(), BattleActivity::class.java).apply {
                    putExtra(BattleActivity.EXTRA_PLAYER_NAME, "Molediver") // Default player monster
                    putExtra(BattleActivity.EXTRA_ENEMY_NAME, monster.speciesId) // Enemy species ID as name
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

                    // Pick random species from database
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
        trackingViewModel.serviceStarted.value = false
        requireActivity().unbindService(trackingViewModel)
        requireActivity().stopService(serviceIntent)
    }
}
