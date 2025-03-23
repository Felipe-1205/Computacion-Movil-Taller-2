package com.example.supertaller2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.supertaller2.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.Camara.setOnClickListener() {
            startActivity(Intent(this, Imagen::class.java))
        }
        binding.mapa.setOnClickListener() {
            startActivity(Intent(this, Mapa::class.java))
        }
    }
}