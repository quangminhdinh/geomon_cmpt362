package com.example.myapplication

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.gms.maps.model.Marker
import com.example.myapplication.battle.Monster
import com.example.myapplication.battle.ui.BattleActivity
import com.example.myapplication.data.FirebaseManager
import com.example.myapplication.data.Seeder
import com.example.myapplication.data.SpeciesRepository
import com.example.myapplication.data.db.AppDatabase
import com.example.myapplication.databinding.ActivityMainBinding
import com.example.myapplication.services.tracking.TrackingService
import com.example.myapplication.services.tracking.TrackingViewModel
import com.example.myapplication.ui.home.MenuDialogFragment
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.random.Random
import com.example.myapplication.data.AuthManager
import com.example.myapplication.data.User

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var binding: ActivityMainBinding

    private lateinit var googleMap: GoogleMap
    private lateinit var playerMarkerOptions: MarkerOptions
    private var playerMarker: Marker? = null
    private val zoomValue = 15f

    private lateinit var trackingViewModel: TrackingViewModel
    private lateinit var serviceIntent: Intent
    private lateinit var repository: SpeciesRepository
    private var availableSpeciesIds: List<String> = emptyList()

    private val monsterMarkers = mutableListOf<Marker>()
    private val otherPlayerMarkers = mutableMapOf<String, Marker>()
    private val nearbyRadius = 0.01 // ~1km radius
    private val monsterThreshold = 10 // minimum monsters in area
    private var playerMonsterId: String? = null
    private lateinit var permissionHandler: PermissionHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize database and repository
        val db = AppDatabase.get(this)
        repository = SpeciesRepository(db.speciesDao())

        // Sign in and initialize player data
        lifecycleScope.launch(Dispatchers.IO) {
            // Sign in anonymously
            Log.d("GeoMon", "Signing in...")
            val firebaseUser = AuthManager.signInAnonymously()
            if (firebaseUser == null) {
                Log.e("GeoMon", "Failed to sign in")
                return@launch
            }
            val userId = firebaseUser.uid
            Log.d("GeoMon", "Signed in as: $userId")

            // Seed database
            Log.d("GeoMon", "Starting seeder")
            Seeder.run(this@MainActivity, repository)
            Log.d("GeoMon", "Database transferred")

            // Load available species IDs (use first() since Room Flows never complete)
            Log.d("GeoMon", "Starting to load species")
            val speciesList = repository.allSpecies().first()
            availableSpeciesIds = speciesList.map { it.id }
            Log.d("GeoMon", "Loaded ${availableSpeciesIds.size} species")

            // Check if user exists in Firebase
            val user = User.fetchById(userId)

            if (user == null || user.monsterIds.isEmpty()) {
                // Create new player monster
                Log.d("GeoMon", "Creating player monster for user: $userId")
                val playerMonster = Monster.initializeByName(
                    context = this@MainActivity,
                    name = "Molediver",
                    level = 50,
                    latitude = 0.0,
                    longitude = 0.0
                )
                playerMonsterId = playerMonster.id

                // Create or update user with player monster ID
                User.createOrUpdate(
                    userId = userId,
                    displayName = "Player",
                    monsterIds = listOf(playerMonster.id)
                )
                Log.d("GeoMon", "Created player monster: ${playerMonster.id}")
            } else {
                // Verify the first monster still exists in Firebase
                val firstMonsterId = user.firstMonsterId
                Log.d("GeoMon", "User exists, verifying first monster: $firstMonsterId")
                val existingMonster = firstMonsterId?.let { Monster.fetchById(it) }
                if (existingMonster == null) {
                    Log.d("GeoMon", "First monster not found in Firebase, creating new one")
                    val playerMonster = Monster.initializeByName(
                        context = this@MainActivity,
                        name = "Molediver",
                        level = 50,
                        latitude = 0.0,
                        longitude = 0.0
                    )
                    playerMonsterId = playerMonster.id
                    User.addMonster(userId, playerMonster.id)
                    Log.d("GeoMon", "Created new player monster: ${playerMonster.id}")
                } else {
                    playerMonsterId = existingMonster.id
                    Log.d("GeoMon", "First monster verified: ${existingMonster.name} (${existingMonster.id})")
                }
            }
        }

        // Initialize permission handler
        permissionHandler = PermissionHandler(this)

        // Initialize map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as? SupportMapFragment
        serviceIntent = Intent(this, TrackingService::class.java)
        trackingViewModel = ViewModelProvider(this).get(TrackingViewModel::class.java)

        mapFragment?.getMapAsync(this)

        // Setup menu button
        binding.btnMenu.setOnClickListener {
            MenuDialogFragment().show(supportFragmentManager, MenuDialogFragment.TAG)
        }
    }

    private fun initTrackingService() {
        Log.d("GeoMon", "initTrackingService called")
        Log.d("GeoMon", "serviceStarted.value = ${trackingViewModel.serviceStarted.value}")
        if (!(trackingViewModel.serviceStarted.value!!)) {
            Log.d("GeoMon", "Starting tracking service")
            startService(serviceIntent)
            trackingViewModel.serviceStarted.value = true
        }
        Log.d("GeoMon", "Binding to tracking service")
        bindService(serviceIntent, trackingViewModel, BIND_AUTO_CREATE)

        Log.d("GeoMon", "Setting up latLng observer")
        trackingViewModel.latLng.observe(this, Observer { it ->
            Log.d("GeoMon", "Received location update: $it")
            updateMap(it)
        })
    }

    private fun updateMap(latLng: LatLng) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoomValue)
        googleMap.animateCamera(cameraUpdate)
        playerMarker?.remove()
        playerMarkerOptions.position(latLng)
        playerMarker = googleMap.addMarker(playerMarkerOptions)

        // Update user location in Firebase for multiplayer
        AuthManager.userId?.let { userId ->
            User.updateLocation(userId, latLng.latitude, latLng.longitude)
        }

        // Check and spawn monsters around player
        checkAndSpawnMonsters()

        // Show other players on the map
        displayOtherPlayers(latLng)
    }

    private fun displayOtherPlayers(playerLatLng: LatLng) {
        val currentUserId = AuthManager.userId ?: return

        FirebaseManager.usersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val activeThreshold = System.currentTimeMillis() - 5 * 60 * 1000 // 5 minutes

                for (child in snapshot.children) {
                    val userId = child.key ?: continue

                    // Skip current user
                    if (userId == currentUserId) continue

                    val user = User.fromSnapshot(child) ?: continue

                    // Skip inactive users
                    if (user.lastActive < activeThreshold) {
                        otherPlayerMarkers[userId]?.remove()
                        otherPlayerMarkers.remove(userId)
                        continue
                    }

                    // Check if within visible radius
                    if (!isWithinRadius(playerLatLng, user.latitude, user.longitude)) {
                        otherPlayerMarkers[userId]?.remove()
                        otherPlayerMarkers.remove(userId)
                        continue
                    }

                    // Update or create marker
                    val existingMarker = otherPlayerMarkers[userId]
                    if (existingMarker != null) {
                        existingMarker.position = LatLng(user.latitude, user.longitude)
                    } else {
                        val marker = googleMap.addMarker(
                            MarkerOptions()
                                .position(LatLng(user.latitude, user.longitude))
                                .title(user.displayName)
                                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                        )
                        marker?.let { otherPlayerMarkers[userId] = it }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GeoMon", "Failed to fetch other players: ${error.message}")
            }
        })
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("GeoMon", "onMapReady called")
        googleMap = map
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        playerMarkerOptions = MarkerOptions()
            .title("You")
            .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE))

        // Handle marker clicks
        googleMap.setOnMarkerClickListener { marker ->
            val monster = marker.tag as? Monster
            if (monster != null) {
                Toast.makeText(
                    this,
                    "Encountered ${monster.name} (Lv.${monster.level})!",
                    Toast.LENGTH_SHORT
                ).show()

                if (playerMonsterId == null) {
                    Toast.makeText(this, "Player monster not ready", Toast.LENGTH_SHORT).show()
                    return@setOnMarkerClickListener true
                }

                val intent = Intent(this, BattleActivity::class.java).apply {
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

        FirebaseManager.monstersRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val nearbyMonsters = mutableListOf<Monster>()

                for (child in snapshot.children) {
                    val lat = child.child("latitude").getValue(Double::class.java) ?: continue
                    val lng = child.child("longitude").getValue(Double::class.java) ?: continue

                    if (isWithinRadius(playerLatLng, lat, lng)) {
                        val monster = Monster.fromSnapshot(child)
                        // Only include wild monsters (no owner)
                        if (monster != null && monster.isWild) {
                            nearbyMonsters.add(monster)
                        }
                    }
                }

                Log.d("GeoMon", "Found ${nearbyMonsters.size} nearby wild monsters")

                if (nearbyMonsters.size < monsterThreshold) {
                    val toGenerate = monsterThreshold - nearbyMonsters.size
                    generateAndUploadMonsters(playerLatLng, toGenerate) {
                        fetchAndDisplayNearbyMonsters(playerLatLng)
                    }
                } else {
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
                        // Only include wild monsters (no owner)
                        if (monster != null && monster.isWild) {
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
                    context = this@MainActivity,
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
        monsterMarkers.forEach { it.remove() }
        monsterMarkers.clear()

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

    override fun onDestroy() {
        super.onDestroy()
        trackingViewModel.serviceStarted.value = false
        unbindService(trackingViewModel)
        stopService(serviceIntent)
    }
}
