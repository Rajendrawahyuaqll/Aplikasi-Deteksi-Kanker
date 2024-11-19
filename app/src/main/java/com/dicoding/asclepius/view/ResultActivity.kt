package com.dicoding.asclepius.view

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.dicoding.asclepius.databinding.ActivityResultBinding

class ResultActivity : AppCompatActivity() {
    private lateinit var binding: ActivityResultBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBackButton()
        setupResultDisplay()
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            finish()
        }
    }

    private fun setupResultDisplay() {
        try {
            val isCancer = intent.getBooleanExtra("IS_CANCER", false)
            val confidenceScore = intent.getFloatExtra("CONFIDENCE_SCORE", 0.0f)
            val imageUriString = intent.getStringExtra("IMAGE_URI")  // Mengambil URI gambar
            val imageUri = imageUriString?.let { Uri.parse(it) }

            imageUri?.let {
                binding.resultImage.setImageURI(it)
            }
            val confidencePercentage = String.format("%d%%", (confidenceScore * 100).toInt())
            val resultText = if (isCancer) {
                "Detected: Cancer\nConfidence Score: $confidencePercentage"
            } else {
                "Non Cancer\nConfidence Score: $confidencePercentage"
            }
            binding.resultText.text = resultText
        } catch (e: Exception) {
            binding.resultText.text = "Error loading results"
            e.printStackTrace()
        }
    }
}