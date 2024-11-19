package com.dicoding.asclepius.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.viewModels
import com.dicoding.asclepius.databinding.ActivityMainBinding
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.yalantis.ucrop.UCrop
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var currentImageUri: Uri? = null
    private val viewModel: MainViewModel by viewModels()


    companion object {
        private const val GALLERY_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupObservers()

        viewModel.currentImageUri.value?.let {
            binding.previewImageView.setImageURI(it)
        }

        binding.galleryButton.setOnClickListener {
            startGallery()
        }
        binding.analyzeButton.setOnClickListener {
            if (viewModel.currentImageUri.value != null) {
                analyzeImage()
            } else {
                showToast("Tidak ada gambar untuk dianalisis. Silakan pilih gambar terlebih dahulu.")
            }
        }
    }



    private fun startGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    private fun setupObservers() {
        // Observer untuk melihat perubahan pada currentImageUri
        viewModel.currentImageUri.observe(this) { uri ->
            if (uri != null) {
                // Jika URI tidak null, tampilkan gambar di previewImageView
                binding.previewImageView.setImageURI(uri)
            } else {
                // Jika URI null, bersihkan gambar di previewImageView
                binding.previewImageView.setImageDrawable(null)
            }
        }
    }


    private fun startCrop(uri: Uri) {
        val destinationUri = Uri.fromFile(File(cacheDir, "cropped_image.jpg"))
        UCrop.of(uri, destinationUri)
            .withAspectRatio(1f, 1f)
            .withMaxResultSize(1080, 1080)
            .start(this)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == GALLERY_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            currentImageUri = data?.data
            currentImageUri?.let {
                viewModel.setImageUri(it)  // Simpan URI ke ViewModel
                startCrop(it)
            }
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            val resultUri = UCrop.getOutput(data!!)
            currentImageUri = resultUri
            viewModel.setImageUri(resultUri)  // Simpan URI cropped ke ViewModel
            showImage()
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_CANCELED) {
            currentImageUri = null
            viewModel.setImageUri(null)  // Perbarui ViewModel dengan null
            binding.previewImageView.setImageDrawable(null)
            showToast("Proses cropping dibatalkan. Gambar telah dihapus.")
        } else if (resultCode == UCrop.RESULT_ERROR) {
            val cropError = UCrop.getError(data!!)
            cropError?.let { showToast("Crop Error: ${it.message}") }
        }
    }



    private fun showImage() {
        if (currentImageUri != null) {
            binding.previewImageView.setImageDrawable(null)
            binding.previewImageView.setImageURI(currentImageUri)
            showToast("Gambar berhasil diperbarui: $currentImageUri")
        } else {
            binding.previewImageView.setImageDrawable(null)
            showToast("Tidak ada gambar yang ditampilkan.")
        }
    }

    private fun analyzeImage() {
        try {
            val uri = viewModel.currentImageUri.value  // Ambil URI dari ViewModel
            if (uri == null) {
                showToast("Tidak ada gambar yang valid untuk dianalisis. Silakan pilih dan crop gambar terlebih dahulu.")
                return
            }

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
            val classifier = ImageClassifier.createFromFile(this, "cancer_classification.tflite")
            val tensorImage = TensorImage.fromBitmap(bitmap)
            val results: List<Classifications> = classifier.classify(tensorImage)
            val bestResult = results.first().categories.maxByOrNull { it.score }
            val confidenceScore = bestResult?.score ?: 0.0f
            val isCancer = bestResult?.label == "Cancer"

            moveToResult(isCancer, confidenceScore)
        } catch (e: Exception) {
            showToast("Terjadi kesalahan saat menganalisis gambar: ${e.message}")
        }
    }


    private fun moveToResult(isCancer: Boolean, confidenceScore: Float) {
        val intent = Intent(this, ResultActivity::class.java).apply {
            putExtra("IS_CANCER", isCancer)
            putExtra("CONFIDENCE_SCORE", confidenceScore)
            putExtra("IMAGE_URI", viewModel.currentImageUri.value.toString())  // Pastikan URI yang benar dikirim
        }
        startActivity(intent)
    }


    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
