package com.example.myapplication   // <-- must match your MapActivity package

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import android.widget.Button
import android.widget.TextView

class BattleActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_battle)

        // get the monster info from the dialog
        val enemyName = intent.getStringExtra("enemy_name") ?: "Wild Monster"
        val enemyLevel = intent.getIntExtra("enemy_level", 1)

        // run button will close the dialog
        findViewById<Button>(R.id.btnRun).setOnClickListener {
            finish()
        }
    }
}

