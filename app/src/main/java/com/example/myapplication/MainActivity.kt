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
import kotlinx.coroutines.withContext
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.ImageView
import android.widget.Button
import androidx.appcompat.app.AlertDialog
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import com.example.myapplication.ui.chat.MonsterAI
import com.google.android.gms.maps.model.BitmapDescriptor
import kotlin.math.roundToInt
import com.example.myapplication.spawn.ItemSpawner
import com.example.myapplication.spawn.ItemSpawn
import android.os.Handler
import android.os.Looper
import com.bumptech.glide.Glide
import com.example.myapplication.ui.home.ChangeAvatarDialogFragment
class MainActivity : AppCompatActivity(), OnMapReadyCallback, ChangeAvatarDialogFragment.OnAvatarUpdatedListener {

    private lateinit var binding: ActivityMainBinding

    private lateinit var googleMap: GoogleMap
    private lateinit var playerMarkerOptions: MarkerOptions
    private var playerMarker: Marker? = null
    private val zoomValue = 15f
    private var previousPlayerLatLng: LatLng? = null
    private var currentPlayerDirection: String = "down" // down, up, left, right
    private var cachedPlayerAvatarBitmap: Bitmap? = null

    private lateinit var trackingViewModel: TrackingViewModel
    private lateinit var serviceIntent: Intent
    private lateinit var repository: SpeciesRepository
    private var availableSpeciesIds: List<String> = emptyList()

    private val monsterMarkers = mutableListOf<Marker>()
    private val itemMarkers = mutableListOf<Marker>()
    private val otherPlayerMarkers = mutableMapOf<String, Marker>()
    private val otherPlayerLocations = mutableMapOf<String, LatLng>() // Track last known positions
    private val otherPlayerDirections = mutableMapOf<String, String>() // Track each player's direction
    private val otherPlayerAvatars = mutableMapOf<String, Bitmap>() // Cache other players' avatars
    private var otherPlayersListener: ValueEventListener? = null
    private val nearbyRadius = 0.01 // ~1km radius
    private val monsterThreshold = 10 // minimum monsters in area
    private var playerMonsterId: String? = null
    private lateinit var permissionHandler: PermissionHandler
    private var isServiceBound = false

    private val heartbeatHandler = Handler(Looper.getMainLooper())
    private val heartbeatInterval = 30000L // 30 seconds
    private val heartbeatRunnable = object : Runnable {
        override fun run() {
            AuthManager.userId?.let { userId ->
                User.updateLastActive(userId)
                Log.d("GeoMon", "Updated lastActive timestamp")
            }
            heartbeatHandler.postDelayed(this, heartbeatInterval)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        MonsterAI.initialize(this)

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.playerPanel.setOnClickListener {
            com.example.myapplication.ui.playerstats.PlayerStatsDialogFragment()
                .show(supportFragmentManager, "PlayerStatsDialog")
        }

        binding.btnInventory.setOnClickListener {
            startActivity(Intent(this, com.example.myapplication.ui.pokedex.PokedexActivity::class.java))
        }


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
                withContext(Dispatchers.Main) {
                    updateMonsterPanel()
                    refreshPlayerAvatar()
                }

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
                val index = user.activeMonsterIndex
                Log.d("GeoMon", "User exists, verifying first monster: $index")
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
                    withContext(Dispatchers.Main) {
                        updateMonsterPanel()
                        refreshPlayerAvatar()
                    }
                    User.addMonster(userId, playerMonster.id)
                    Log.d("GeoMon", "Created new player monster: ${playerMonster.id}")
                } else {
                    playerMonsterId = existingMonster.id
                    withContext(Dispatchers.Main) {
                        updateMonsterPanel()
                        refreshPlayerAvatar()
                    }
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
        if (trackingViewModel.serviceStarted.value != true) {
            Log.d("GeoMon", "Starting tracking service")
            startService(serviceIntent)
            trackingViewModel.serviceStarted.value = true
        }
        Log.d("GeoMon", "Binding to tracking service")
        bindService(serviceIntent, trackingViewModel, BIND_AUTO_CREATE)
        isServiceBound = true

        Log.d("GeoMon", "Setting up latLng observer")
        trackingViewModel.latLng.observe(this, Observer { it ->
            Log.d("GeoMon", "Received location update: $it")
            updateMap(it)
        })
    }

    override fun onResume() {
        super.onResume()
        // When coming back from MonsterInfoActivity, refresh active monster
        refreshPlayerMonsterFromUser()

        // Refresh player avatar in bottom panel
        refreshPlayerAvatar()

        // Start heartbeat to keep user active
        heartbeatHandler.post(heartbeatRunnable)

        // Refresh monsters on map (in case any were defeated/captured in battle)
        if (::googleMap.isInitialized) {
            val currentLatLng = trackingViewModel.latLng.value
            if (currentLatLng != null) {
                fetchAndDisplayNearbyMonsters(currentLatLng)
                displayOtherPlayers(currentLatLng)
            }
        }

       // checkAndSpawnMonsters()

    }

    override fun onPause() {
        super.onPause()
        // Stop heartbeat when activity is paused
        heartbeatHandler.removeCallbacks(heartbeatRunnable)
    }
    
    //refresh for choosing the starting monster
    private fun refreshPlayerMonsterFromUser() {
        val userId = AuthManager.userId ?: return

        FirebaseManager.usersRef.child(userId).get()
            .addOnSuccessListener { snapshot ->

                val activeIndex = snapshot.child("activeMonsterIndex").getValue(Int::class.java) ?: 0


                val monsterIds = mutableListOf<String>()
                snapshot.child("monsterIds").children.forEach { child ->
                    child.getValue(String::class.java)?.let { monsterIds.add(it) }
                }

                val chosenId = monsterIds.getOrNull(activeIndex) ?: monsterIds.firstOrNull()

                if (chosenId != null) {
                    playerMonsterId = chosenId
                    Log.d(
                        "GeoMon",
                        "Refreshed active player monster: id=$playerMonsterId index=$activeIndex"
                    )
                    // Update the monster panel UI
                    updateMonsterPanel()
                } else {
                    Log.e("GeoMon", "No monsterIds found for user when refreshing player monster")
                }
            }
            .addOnFailureListener { e ->
                Log.e("GeoMon", "Failed to refresh player monster: ${e.message}")
            }
    }

    private fun updateMap(latLng: LatLng) {
        val cameraUpdate = CameraUpdateFactory.newLatLngZoom(latLng, zoomValue)
        googleMap.animateCamera(cameraUpdate)

        // Calculate direction based on movement
        if (previousPlayerLatLng != null) {
            calculateDirection(latLng)
        }

        // Remove old marker and add new one with updated direction icon
        playerMarker?.remove()
        playerMarkerOptions.position(latLng)
        playerMarkerOptions.icon(getDirectionIcon())
        playerMarker = googleMap.addMarker(playerMarkerOptions)

        // Update previous location for next direction calculation
        previousPlayerLatLng = latLng

        // Update user location in Firebase for multiplayer
        AuthManager.userId?.let { userId ->
            User.updateLocation(userId, latLng.latitude, latLng.longitude)
        }

        // Check and spawn monsters around player
        checkAndSpawnMonsters()

        // ✅ NEW: spawn items around player
        checkAndSpawnItems()

        // Show other players on the map
        displayOtherPlayers(latLng)
    }


    private fun displayOtherPlayers(playerLatLng: LatLng) {
        val currentUserId = AuthManager.userId ?: return

        // Remove old listener if exists
        otherPlayersListener?.let {
            FirebaseManager.usersRef.removeEventListener(it)
        }

        // Create continuous listener for real-time updates
        otherPlayersListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val activeThreshold = System.currentTimeMillis() - 5 * 60 * 1000 // 5 minutes
                val currentPlayerIds = mutableSetOf<String>()

                for (child in snapshot.children) {
                    val userId = child.key ?: continue

                    // Skip current user
                    if (userId == currentUserId) continue

                    val user = User.fromSnapshot(child) ?: continue

                    // Skip inactive users
                    if (user.lastActive < activeThreshold) {
                        continue
                    }

                    // Check if within visible radius
                    if (!isWithinRadius(playerLatLng, user.latitude, user.longitude)) {
                        continue
                    }

                    currentPlayerIds.add(userId)

                    val newLocation = LatLng(user.latitude, user.longitude)
                    val previousLocation = otherPlayerLocations[userId]

                    // Calculate direction based on movement
                    val direction = calculateDirectionForPlayer(newLocation, previousLocation)

                    // Update location and direction tracking
                    otherPlayerLocations[userId] = newLocation
                    otherPlayerDirections[userId] = direction

                    // Load avatar if not cached
                    if (user.avatarUrl.isNotBlank() && !otherPlayerAvatars.containsKey(userId)) {
                        Glide.with(this@MainActivity)
                            .asBitmap()
                            .load(user.avatarUrl)
                            .into(object : com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                                override fun onResourceReady(
                                    resource: Bitmap,
                                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                                ) {
                                    otherPlayerAvatars[userId] = resource
                                    updateOtherPlayerMarker(userId, newLocation, direction, user.displayName, resource)
                                }

                                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {}
                            })
                    } else {
                        // Update marker with cached avatar or no avatar
                        updateOtherPlayerMarker(userId, newLocation, direction, user.displayName, otherPlayerAvatars[userId])
                    }
                }

                // Remove markers for players no longer visible
                val playersToRemove = otherPlayerMarkers.keys.filter { !currentPlayerIds.contains(it) }
                playersToRemove.forEach { userId ->
                    otherPlayerMarkers[userId]?.remove()
                    otherPlayerMarkers.remove(userId)
                    otherPlayerLocations.remove(userId)
                    otherPlayerDirections.remove(userId)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("GeoMon", "Failed to fetch other players: ${error.message}")
            }
        }

        FirebaseManager.usersRef.addValueEventListener(otherPlayersListener!!)
    }

    private fun updateOtherPlayerMarker(userId: String, location: LatLng, direction: String, displayName: String, avatarBitmap: Bitmap?) {
        // Remove existing marker
        otherPlayerMarkers[userId]?.remove()

        // Create new marker with directional icon and avatar
        val marker = googleMap.addMarker(
            MarkerOptions()
                .position(location)
                .title(displayName)
                .icon(getOtherPlayerDirectionIcon(direction, avatarBitmap))
        )
        marker?.let { otherPlayerMarkers[userId] = it }
    }

    override fun onMapReady(map: GoogleMap) {
        Log.d("GeoMon", "onMapReady called")
        googleMap = map
        googleMap.mapType = GoogleMap.MAP_TYPE_NORMAL
        playerMarkerOptions = MarkerOptions()
            .title("You")
            .icon(getDirectionIcon())


        /*
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
        } */
        //On click, if player's starting monster is 0 hp, toast a message. If player's starting monster has hp, open a menu.
        googleMap.setOnMarkerClickListener { marker ->
            when (val tag = marker.tag) {

                is Monster -> {
                    lifecycleScope.launch {
                        val playerId = playerMonsterId
                        if (playerId == null) {
                            Toast.makeText(
                                this@MainActivity,
                                "Your starting monster is not ready yet.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }

                        val playerMon = Monster.fetchById(playerId)

                        if (playerMon == null) {
                            Toast.makeText(
                                this@MainActivity,
                                "Could not load your starting monster.",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }

                        if (playerMon.currentHp <= 0f || playerMon.isFainted) {
                            Toast.makeText(
                                this@MainActivity,
                                "Your starting monster has fainted. Swap to another monster!",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }

                        val view = layoutInflater.inflate(R.layout.dialog_monster, null)
                        view.findViewById<TextView>(R.id.tvMonsterName).text  = tag.name
                        view.findViewById<TextView>(R.id.tvMonsterLevel).text = "Lv. ${tag.level}"
                        val spriteName = tag.name.lowercase().replace(" ", "_")
                        val resId = resources.getIdentifier(spriteName, "drawable", packageName)
                        if (resId != 0) {
                            view.findViewById<ImageView>(R.id.imgMonster).setImageResource(resId)
                        }
                        val dlg = AlertDialog.Builder(this@MainActivity)
                            .setView(view)
                            .create()

                        view.findViewById<Button>(R.id.btnFight).setOnClickListener {

                            val intent = Intent(
                                this@MainActivity,
                                BattleActivity::class.java
                            ).apply {
                                putExtra(
                                    BattleActivity.EXTRA_PLAYER_ID,
                                    playerMonsterId
                                )
                                putExtra(
                                    BattleActivity.EXTRA_ENEMY_ID,
                                    tag.id
                                )
                            }
                            startActivity(intent)
                            dlg.dismiss()

                        }

                        view.findViewById<Button>(R.id.btnCancel).setOnClickListener { dlg.dismiss() }

                        dlg.show()
                    }
                    true
                }

                is ItemSpawn -> {
                    val userId = AuthManager.userId
                    if (userId != null) {
                        User.addItem(userId, tag.name, 1)
                        Toast.makeText(
                            this@MainActivity,
                            "Picked up: ${tag.name}",
                            Toast.LENGTH_SHORT
                        ).show()
                        itemMarkers.remove(marker)
                        marker.remove()
                    } else {
                        Toast.makeText(
                            this@MainActivity,
                            "Log in to pick up map items",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    true
                }

                else -> false
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
    private fun dp(sizeDp: Int): Int =
        (sizeDp * resources.displayMetrics.density).roundToInt()

    private fun getDirectionIcon(sizeDp: Int = 42): BitmapDescriptor {
        val resName = currentPlayerDirection // "down", "up", "left", or "right"
        val resId = resources.getIdentifier(resName, "drawable", packageName)

        if (resId == 0) {
            // Fallback to default marker if direction image not found
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
        }

        val drawable: Drawable = ContextCompat.getDrawable(this, resId)
            ?: return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)

        val w = dp(sizeDp)
        val h = dp(sizeDp)

        val bmp: Bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            Bitmap.createScaledBitmap(drawable.bitmap, w, h, true)
        } else {
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(c)
            b
        }

        // Overlay avatar if available
        val finalBitmap = if (cachedPlayerAvatarBitmap != null) {
            overlayAvatarOnMarker(bmp, cachedPlayerAvatarBitmap!!)
        } else {
            bmp
        }

        return BitmapDescriptorFactory.fromBitmap(finalBitmap)
    }

    private fun overlayAvatarOnMarker(markerBitmap: Bitmap, avatarBitmap: Bitmap): Bitmap {
        // Create a mutable copy of the marker bitmap
        val result = markerBitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val avatarSize = dp(58)
        val scaledAvatar = Bitmap.createScaledBitmap(avatarBitmap, avatarSize, avatarSize, true)

        // Create circular avatar
        val circularAvatar = Bitmap.createBitmap(avatarSize, avatarSize, Bitmap.Config.ARGB_8888)
        val avatarCanvas = Canvas(circularAvatar)
        val paint = android.graphics.Paint()
        paint.isAntiAlias = true

        // Draw circle
        avatarCanvas.drawCircle(
            avatarSize / 2f,
            avatarSize / 2f,
            avatarSize / 2f,
            paint
        )

        // Apply avatar using SRC_IN to clip to circle
        paint.xfermode = android.graphics.PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN)
        avatarCanvas.drawBitmap(scaledAvatar, 0f, 0f, paint)

        // Draw white circle border (thicker for better visibility)
        val borderPaint = android.graphics.Paint()
        borderPaint.isAntiAlias = true
        borderPaint.style = android.graphics.Paint.Style.STROKE
        borderPaint.strokeWidth = dp(3).toFloat() // Increased from 2dp to 3dp
        borderPaint.color = android.graphics.Color.WHITE
        avatarCanvas.drawCircle(
            avatarSize / 2f,
            avatarSize / 2f,
            avatarSize / 2f - dp(2).toFloat(),
            borderPaint
        )

        // Position avatar at top-center of the marker
        // Center horizontally and place at the very top
        val x = dp(20).toFloat()//(result.width - avatarSize) / 2f
        val y = dp(-3).toFloat()  // Right at the top

        canvas.drawBitmap(circularAvatar, x, y, null)

        return result
    }

    private fun calculateDirection(newLatLng: LatLng) {
        val prev = previousPlayerLatLng ?: return

        val latDiff = newLatLng.latitude - prev.latitude
        val lngDiff = newLatLng.longitude - prev.longitude

        // Determine primary direction based on larger difference
        if (Math.abs(latDiff) > Math.abs(lngDiff)) {
            // More vertical movement
            currentPlayerDirection = if (latDiff > 0) "up" else "down"
        } else {
            // More horizontal movement
            currentPlayerDirection = if (lngDiff > 0) "right" else "left"
        }
    }

    private fun calculateDirectionForPlayer(newLatLng: LatLng, previousLatLng: LatLng?): String {
        if (previousLatLng == null) return "down"

        val latDiff = newLatLng.latitude - previousLatLng.latitude
        val lngDiff = newLatLng.longitude - previousLatLng.longitude

        // Determine primary direction based on larger difference
        return if (Math.abs(latDiff) > Math.abs(lngDiff)) {
            // More vertical movement
            if (latDiff > 0) "up" else "down"
        } else {
            // More horizontal movement
            if (lngDiff > 0) "right" else "left"
        }
    }

    private fun getOtherPlayerDirectionIcon(direction: String, avatarBitmap: Bitmap?, sizeDp: Int = 42): BitmapDescriptor {
        val resId = resources.getIdentifier(direction, "drawable", packageName)

        if (resId == 0) {
            // Fallback to default marker if direction image not found
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
        }

        val drawable: Drawable = ContextCompat.getDrawable(this, resId)
            ?: return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)

        val w = dp(sizeDp)
        val h = dp(sizeDp)

        val bmp: Bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            Bitmap.createScaledBitmap(drawable.bitmap, w, h, true)
        } else {
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(c)
            b
        }

        // Overlay avatar if available
        val finalBitmap = if (avatarBitmap != null) {
            overlayAvatarOnMarker(bmp, avatarBitmap)
        } else {
            bmp
        }

        return BitmapDescriptorFactory.fromBitmap(finalBitmap)
    }

    private fun monsterIconByName(name: String, sizeDp: Int = 42): BitmapDescriptor {
        val resName = name.lowercase().replace(" ", "_")
        val resId = resources.getIdentifier(resName, "drawable", packageName)
        if (resId == 0) return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)

        val drawable: Drawable = ContextCompat.getDrawable(this, resId)
            ?: return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)

        val w = dp(sizeDp)
        val h = dp(sizeDp)

        val bmp: Bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            Bitmap.createScaledBitmap(drawable.bitmap, w, h, true)
        } else {
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(c)
            b
        }
        return BitmapDescriptorFactory.fromBitmap(bmp)
    }
    private fun checkAndSpawnItems() {
        val playerLatLng = trackingViewModel.latLng.value ?: return

        // Generate items locally around the player using ItemSpawner
        val items = ItemSpawner.generateItemsAround(playerLatLng)
        Log.d("GeoMon", "Generated ${items.size} items around player")
        displayItemsOnMap(items)
    }

    private fun displayItemsOnMap(items: List<ItemSpawn>) {

        itemMarkers.forEach { it.remove() }
        itemMarkers.clear()

        items.forEach { item ->
            Log.d(
                "GeoMon",
                "Placing item: ${item.name} at ${item.latitude}, ${item.longitude}"
            )

            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(item.latitude, item.longitude))
                    .title(item.name)
                    .icon(itemIconByName(item.name))
                    .anchor(0.5f, 1f)
            )
            marker?.let {
                it.tag = item
                itemMarkers.add(it)
            }
        }

        Log.d("GeoMon", "Displayed ${items.size} items on map")
    }

    private fun itemIconByName(name: String, sizeDp: Int = 42): BitmapDescriptor {
        val resName = name.lowercase().replace(" ", "_")
        val resId = resources.getIdentifier(resName, "drawable", packageName)

        if (resId == 0) {
            // fallback: green marker for items
            return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
        }

        val drawable: Drawable = ContextCompat.getDrawable(this, resId)
            ?: return BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)

        val w = dp(sizeDp)
        val h = dp(sizeDp)

        val bmp: Bitmap = if (drawable is BitmapDrawable && drawable.bitmap != null) {
            Bitmap.createScaledBitmap(drawable.bitmap, w, h, true)
        } else {
            val b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val c = Canvas(b)
            drawable.setBounds(0, 0, w, h)
            drawable.draw(c)
            b
        }

        return BitmapDescriptorFactory.fromBitmap(bmp)
    }

    private fun displayMonstersOnMap(monsters: List<Monster>) {
        monsterMarkers.forEach { it.remove() }
        monsterMarkers.clear()

        monsters.forEach { monster ->
            val marker = googleMap.addMarker(
                MarkerOptions()
                    .position(LatLng(monster.latitude, monster.longitude))
                    .title("${monster.name} (Lv.${monster.level})")
                    //.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                    .icon(monsterIconByName(monster.name))
                    .anchor(0.5f, 1f)

            )
            marker?.tag = monster
            marker?.let { monsterMarkers.add(it) }
        }
        Log.d("GeoMon", "Displayed ${monsters.size} monsters on map")
    }



    private fun updateMonsterPanel() {
        lifecycleScope.launch(Dispatchers.IO) {
            val id = playerMonsterId ?: return@launch
            val mon = com.example.myapplication.battle.Monster.fetchById(id) ?: return@launch
            launch(Dispatchers.Main) {
                binding.tvMonsterName.text  = mon.name
                binding.tvMonsterLevel.text = "Lv. ${mon.level}"
                val resId = resources.getIdentifier(
                    mon.name.lowercase().replace(" ", "_"),
                    "drawable",
                    packageName
                )
                if (resId != 0) binding.imgMonster.setImageResource(resId)
            }
        }
    }

    private fun refreshPlayerAvatar() {
        val userId = AuthManager.userId ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            val user = User.fetchById(userId) ?: return@launch

            launch(Dispatchers.Main) {
                if (user.avatarUrl.isNotBlank()) {
                    // Load avatar for bottom panel
                    Glide.with(this@MainActivity)
                        .load(user.avatarUrl)
                        .circleCrop()
                        .into(binding.imgPlayer)

                    // Load avatar bitmap for marker overlay
                    Glide.with(this@MainActivity)
                        .asBitmap()
                        .load(user.avatarUrl)
                        .into(object : com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                            override fun onResourceReady(
                                resource: Bitmap,
                                transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                            ) {
                                cachedPlayerAvatarBitmap = resource
                                // Update marker with new avatar
                                updatePlayerMarkerWithAvatar()
                            }

                            override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                                // Handle cleanup if needed
                            }
                        })
                } else {
                    // Use default avatar
                    binding.imgPlayer.setImageResource(android.R.drawable.sym_def_app_icon)
                    cachedPlayerAvatarBitmap = null
                }
            }
        }
    }

    private fun updatePlayerMarkerWithAvatar() {
        // Update current marker with avatar overlay
        val currentLatLng = trackingViewModel.latLng.value
        if (currentLatLng != null && ::googleMap.isInitialized) {
            playerMarker?.remove()
            playerMarkerOptions.position(currentLatLng)
            playerMarkerOptions.icon(getDirectionIcon())
            playerMarker = googleMap.addMarker(playerMarkerOptions)
        }
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

        // Remove Firebase listener
        otherPlayersListener?.let {
            FirebaseManager.usersRef.removeEventListener(it)
        }

        /*
        unbindService(trackingViewModel)
        stopService(serviceIntent)
         */
        if (isServiceBound) {
            try { unbindService(trackingViewModel) } catch (_: IllegalArgumentException) {}
            isServiceBound = false
        }
        stopService(serviceIntent)

    }

    override fun onAvatarUpdated(avatarUrl: String) {
        // Update bottom panel avatar when avatar changes
        Glide.with(this)
            .load(avatarUrl)
            .circleCrop()
            .into(binding.imgPlayer)

        // Load avatar bitmap for marker overlay
        Glide.with(this)
            .asBitmap()
            .load(avatarUrl)
            .into(object : com.bumptech.glide.request.target.CustomTarget<Bitmap>() {
                override fun onResourceReady(
                    resource: Bitmap,
                    transition: com.bumptech.glide.request.transition.Transition<in Bitmap>?
                ) {
                    cachedPlayerAvatarBitmap = resource
                    // Update marker with new avatar
                    updatePlayerMarkerWithAvatar()
                }

                override fun onLoadCleared(placeholder: android.graphics.drawable.Drawable?) {
                    // Handle cleanup if needed
                }
            })
    }
}