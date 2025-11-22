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
import com.example.myapplication.battle.Monster
import com.example.myapplication.battle.ui.BattleActivity
import com.example.myapplication.data.FirebaseManager
import com.example.myapplication.data.Seeder
import com.example.myapplication.data.SpeciesRepository
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.services.tracking.TrackingService
import com.example.myapplication.services.tracking.TrackingViewModel
import com.google.android.gms.maps.model.Marker
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random
import android.content.Context
import com.example.myapplication.PermissionHandler

class HomeFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var googleMap: GoogleMap
    private lateinit var playerMarkerOptions: MarkerOptions
    private var playerMarker: Marker? = null
    private val zoomValue = 15f

    private lateinit var trackingViewModel: TrackingViewModel
    private lateinit var serviceIntent: Intent
    private lateinit var repository: SpeciesRepository
    private var availableSpeciesIds: List<String> = emptyList()

    private val monsterMarkers = mutableListOf<Marker>()
    private val nearbyRadius = 0.01 // ~1km radius
    private val monsterThreshold = 10 // minimum monsters in area
    private var playerMonsterId: String? = null
    private lateinit var permissionHandler: PermissionHandler

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

        // Load player monster ID from preferences
        val prefs = requireContext().getSharedPreferences("player_data", Context.MODE_PRIVATE)
        playerMonsterId = prefs.getString("player_monster_id", null)
        Log.d("GeoMon", "Loaded playerMonsterId from prefs: $playerMonsterId")

        // Seed database, load species, and create player monster if needed
        lifecycleScope.launch(Dispatchers.IO) {
            Log.d("GeoMon", "Starting seeder")
            Seeder.run(requireContext(), repository)
            Log.d("GeoMon", "Database transferred")

            // Load available species IDs (use first() since Room Flows never complete)
            Log.d("GeoMon", "Starting to load species")
            val speciesList = repository.allSpecies().first()
            availableSpeciesIds = speciesList.map { it.id }
            Log.d("GeoMon", "Loaded ${availableSpeciesIds.size} species")

            Log.d("GeoMon", "After collect - checking playerMonsterId: $playerMonsterId")
            // Create player monster if it doesn't exist or if it's missing from Firebase
            if (playerMonsterId == null) {
                Log.d("GeoMon", "playerMonsterId is null, creating player monster")
                val playerMonster = Monster.initializeByName(
                    context = requireContext(),
                    name = "Molediver",
                    level = 50,
                    latitude = 0.0,
                    longitude = 0.0
                )
                playerMonsterId = playerMonster.id
                prefs.edit().putString("player_monster_id", playerMonster.id).apply()
                Log.d("GeoMon", "Created player monster: ${playerMonster.id}")
            } else {
                // Verify the saved player monster still exists in Firebase
                Log.d("GeoMon", "Verifying player monster exists in Firebase: $playerMonsterId")
                val existingMonster = Monster.fetchById(playerMonsterId!!)
                if (existingMonster == null) {
                    Log.d("GeoMon", "Player monster not found in Firebase, creating new one")
                    val playerMonster = Monster.initializeByName(
                        context = requireContext(),
                        name = "Molediver",
                        level = 50,
                        latitude = 0.0,
                        longitude = 0.0
                    )
                    playerMonsterId = playerMonster.id
                    prefs.edit().putString("player_monster_id", playerMonster.id).apply()
                    Log.d("GeoMon", "Created new player monster: ${playerMonster.id}")
                } else {
                    Log.d("GeoMon", "Player monster verified: ${existingMonster.name} (${existingMonster.id})")
                }
            }
        }

        // Initialize permission handler
        permissionHandler = PermissionHandler(requireActivity())

        // Initialize map
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        serviceIntent = Intent(requireActivity(), TrackingService::class.java)
        trackingViewModel = ViewModelProvider(requireActivity()).get(TrackingViewModel::class.java)

        mapFragment?.getMapAsync(this)

        return root
    }

    fun initTrackingService() {
        Log.d("GeoMon", "initTrackingService called")
        Log.d("GeoMon", "serviceStarted.value = ${trackingViewModel.serviceStarted.value}")
        if (!(trackingViewModel.serviceStarted.value!!)) {
            Log.d("GeoMon", "Starting tracking service")
            requireActivity().startService(serviceIntent)
            trackingViewModel.serviceStarted.value = true
        }
        Log.d("GeoMon", "Binding to tracking service")
        requireActivity()
            .bindService(serviceIntent, trackingViewModel, BIND_AUTO_CREATE)

        Log.d("GeoMon", "Setting up latLng observer")
        trackingViewModel.latLng.observe(this, Observer { it ->
            Log.d("GeoMon", "Received location update: $it")
            updateMap(it)
        })
    }

    fun updateMap(latLng: LatLng) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(
            latLng, zoomValue)
        googleMap.animateCamera(cameraUpdate)
        playerMarker?.remove()
        playerMarkerOptions.position(latLng)
        playerMarker = googleMap.addMarker(playerMarkerOptions)

        // Check and spawn monsters around player
        checkAndSpawnMonsters()
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("GeoMon", "onMapReady called")
        googleMap = map
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        playerMarkerOptions = MarkerOptions()
            .title("You")
            .icon(BitmapDescriptorFactory.defaultMarker(
                BitmapDescriptorFactory.HUE_AZURE))

        // Handle marker clicks
        googleMap.setOnMarkerClickListener { marker ->
            val monster = marker.tag as? Monster
            if (monster != null) {
                Toast.makeText(
                    requireContext(),
                    "Encountered ${monster.name} (Lv.${monster.level})!",
                    Toast.LENGTH_SHORT
                ).show()

                if (playerMonsterId == null) {
                    Toast.makeText(requireContext(), "Player monster not ready", Toast.LENGTH_SHORT).show()
                    return@setOnMarkerClickListener true
                }

                val intent = Intent(requireContext(), BattleActivity::class.java).apply {
                    putExtra(BattleActivity.EXTRA_PLAYER_ID, playerMonsterId)
                    putExtra(BattleActivity.EXTRA_ENEMY_ID, monster.id)
                }
                startActivity(intent)
                true
            } else {
                false
            }
        }

        // Check permissions before starting tracking service
        if (permissionHandler.checkAndRequestPermissions()) {
            initTrackingService()
        }
    }

    private fun checkAndSpawnMonsters() {
        val playerLatLng = trackingViewModel.latLng.value ?: return

        // Query Firebase for nearby monsters
        FirebaseManager.monstersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nearbyMonsters = mutableListOf<Monster>()

                // Filter monsters within radius
                for (child in snapshot.children) {
                    val lat = child.child("latitude").getValue(Double::class.java) ?: continue
                    val lng = child.child("longitude").getValue(Double::class.java) ?: continue

                    if (isWithinRadius(playerLatLng, lat, lng)) {
                        val monster = Monster.fromSnapshot(child)
                        if (monster != null) {
                            nearbyMonsters.add(monster)
                        }
                    }
                }

                Log.d("GeoMon", "Found ${nearbyMonsters.size} nearby monsters")

                // Generate more if below threshold
                if (nearbyMonsters.size < monsterThreshold) {
                    val toGenerate = monsterThreshold - nearbyMonsters.size
                    generateAndUploadMonsters(playerLatLng, toGenerate) {
                        // Re-fetch and display after generation completes
                        fetchAndDisplayNearbyMonsters(playerLatLng)
                    }
                } else {
                    // Display existing nearby monsters on map
                    displayMonstersOnMap(nearbyMonsters)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GeoMon", "Firebase error: ${error.message}")
            }
        })
    }

    private fun isWithinRadius(center: LatLng, lat: Double, lng: Double): Boolean {
        val latDiff = Math.abs(center.latitude - lat)
        val lngDiff = Math.abs(center.longitude - lng)
        return latDiff <= nearbyRadius && lngDiff <= nearbyRadius
    }

    private fun fetchAndDisplayNearbyMonsters(playerLatLng: LatLng) {
        FirebaseManager.monstersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nearbyMonsters = mutableListOf<Monster>()
                for (child in snapshot.children) {
                    val lat = child.child("latitude").getValue(Double::class.java) ?: continue
                    val lng = child.child("longitude").getValue(Double::class.java) ?: continue
                    if (isWithinRadius(playerLatLng, lat, lng)) {
                        val monster = Monster.fromSnapshot(child)
                        if (monster != null) {
                            nearbyMonsters.add(monster)
                        }
                    }
                }
                displayMonstersOnMap(nearbyMonsters)
            }
            override fun onCancelled(error: DatabaseError) {
                Log.e("GeoMon", "Firebase error: ${error.message}")
            }
        })
    }

    private fun generateAndUploadMonsters(center: LatLng, count: Int, onComplete: () -> Unit = {}) {
        if (availableSpeciesIds.isEmpty()) {
            Log.d("GeoMon", "No species available yet")
            onComplete()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            for (i in 0 until count) {
                val latOffset = (Random.nextDouble() - 0.5) * nearbyRadius * 2
                val lngOffset = (Random.nextDouble() - 0.5) * nearbyRadius * 2
                val monsterLat = center.latitude + latOffset
                val monsterLng = center.longitude + lngOffset
                val level = Random.nextInt(1, 21)
                val speciesId = availableSpeciesIds.random()

                Monster.initializeByName(
                    context = requireContext(),
                    name = speciesId,
                    level = level,
                    latitude = monsterLat,
                    longitude = monsterLng
                )
            }
            Log.d("GeoMon", "Generated and uploaded $count monsters")
            lifecycleScope.launch(Dispatchers.Main) {
                onComplete()
            }
        }
    }

    private fun displayMonstersOnMap(monsters: List<Monster>) {
        // Clear old markers
        monsterMarkers.forEach { it.remove() }
        monsterMarkers.clear()

        // Add new markers with unique color (green)
        monsters.forEach { monster ->
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(monster.latitude, monster.longitude))
                    .title("${monster.name} (Lv.${monster.level})")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
            )
            marker?.tag = monster
            marker?.let { monsterMarkers.add(it) }
        }
        Log.d("GeoMon", "Displayed ${monsters.size} monsters on map")
    }

    @Deprecated("Deprecated in Java")
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
            onGranted = { initTrackingService() },
            onDenied = { Log.e("GeoMon", "Location permission denied") }
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        trackingViewModel.serviceStarted.value = false
        requireActivity().unbindService(trackingViewModel)
        requireActivity().stopService(serviceIntent)
    }
}
