package com.dicoding.asclepius.helper

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import com.dicoding.asclepius.R
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.CastOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.task.core.BaseOptions
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier


class ImageClassifierHelper(
    var threshold: Float = 0.1f,
    var maxResult: Int = 3,
    val modelName: String = "cancer_classification.tflite",
    val context: Context,
    val classifierListener: ClassifierListener?

) {
    private var imageClassifier: ImageClassifier? = null

    interface ClassifierListener {
        fun onError(error: String)
        fun onResults(
            results: List<Classifications>?,
            inferenceTime: Long
        )

    }

    init {
        setupImageClassifier()
    }

    companion object{
        private const val TAG = "ImageClassifierHelper"
    }

    private fun setupImageClassifier() {
        val optionBuilder = ImageClassifier.ImageClassifierOptions.builder()
            .setScoreThreshold(threshold)
            .setMaxResults(maxResult)
        val baseOptionBuilder = BaseOptions.builder()
            .setNumThreads(4)
        optionBuilder.setBaseOptions(baseOptionBuilder.build())
        try {
            imageClassifier =
                ImageClassifier.createFromFileAndOptions(context, modelName, optionBuilder.build())
        } catch (e: IllegalStateException){
            classifierListener?.onError(context.getString(R.string.image_classifier_failed))
            Log.e(TAG, e.message.toString())
        }
    }

    fun classifyStaticImage(imageUri: Uri) {
        if (imageClassifier == null) {
            setupImageClassifier()
        }

        val imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(224, 224, ResizeOp.ResizeMethod.NEAREST_NEIGHBOR))
            .add(CastOp(DataType.UINT8))
            .build()
        try {
            val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val source = ImageDecoder.createSource(context.contentResolver, imageUri)
                ImageDecoder.decodeBitmap(source)
            } else {
                @Suppress("DEPRECATION") // Mengabaikan peringatan depresiasi untuk Android di bawah API 28
                MediaStore.Images.Media.getBitmap(context.contentResolver, imageUri)
            }.copy(Bitmap.Config.ARGB_8888, true)

            val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))
            var inferenceTime = SystemClock.uptimeMillis()
            val results = imageClassifier?.classify(tensorImage)
            inferenceTime = SystemClock.uptimeMillis() - inferenceTime
            classifierListener?.onResults(results, inferenceTime)

        } catch (e: Exception) {
            classifierListener?.onError(context.getString(R.string.classification_failed))
            Log.e(TAG, "Classification error: ${e.message}")
        }
    }
}