package com.example.posestimator.ui.main

import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.example.posestimator.databinding.ActivityMainBinding
import com.example.posestimator.extensions.averageColor
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.PoseLandmark
import com.google.mlkit.vision.pose.accurate.AccuratePoseDetectorOptions
import com.jakewharton.rxbinding4.view.clicks
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import timber.log.Timber
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.atan2

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()
    private val disposeBag: CompositeDisposable by lazy { CompositeDisposable() }
    private lateinit var poseDetector: PoseDetector

    private var imageUri: Uri? = null
    private var resizedBitmap: Bitmap? = null
    private var angleText = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupPoseDetector()
        setupUI()

    }

    private fun setupPoseDetector() {
        // Pose Detect
        val options = AccuratePoseDetectorOptions.Builder()
            .setDetectorMode(AccuratePoseDetectorOptions.SINGLE_IMAGE_MODE)
            .build()

        poseDetector = PoseDetection.getClient(options)
    }

    private fun setupUI() {
        binding.loadImageBtn.clicks()
            .throttleFirst(2, TimeUnit.SECONDS)
            .subscribe {
                imageUri = null
                binding.imageView.isVisible = false
                binding.angleText.text = ""
                pickerContent.launch("image/*")
            }.addTo(disposeBag)

        binding.detectBtn.clicks()
            .throttleFirst(2, TimeUnit.SECONDS)
            .subscribe {
                if (imageUri != null) {

                    viewModel.isLoading.onNext(true)

                    val inputStream = contentResolver.openInputStream(imageUri!!)
                    val imageBitmap = BitmapFactory.decodeStream(inputStream)
                    runPoseAlgorithm(imageBitmap)
                } else {
                    Toast.makeText(
                        this,
                        "Please select an image before POS detection",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }.addTo(disposeBag)

        viewModel.isLoading.subscribe {
            binding.progressBar.isVisible = it
        }.addTo(disposeBag)

    }


    private val pickerContent =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                binding.imageView.isVisible = true

                binding.imageView.setImageURI(uri)

                imageUri = uri


            } else {
                Toast.makeText(
                    this, "There was an error getting image", Toast.LENGTH_SHORT
                ).show()
            }

        }


    private fun runPoseAlgorithm(imageBitmap: Bitmap) {
        val rotationDegree = 0

        val width: Int = imageBitmap.width
        val height: Int = imageBitmap.height

        resizedBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, width, height)

        val image = InputImage.fromBitmap(resizedBitmap!!, rotationDegree)

        poseDetector.process(image)
            .addOnSuccessListener { pose -> processPose(pose) }
            .addOnFailureListener {
                Toast.makeText(
                    this,
                    "Pose detection failed on the current image",
                    Toast.LENGTH_SHORT
                ).show()
                viewModel.isLoading.onNext(false)
            }
    }

    private fun processPose(pose: Pose) {
        //Detect all landmarks from the image
        try {

            //TOP PART
            val leftShoulder = pose.getPoseLandmark(PoseLandmark.LEFT_SHOULDER)
            val leftHip = pose.getPoseLandmark(PoseLandmark.LEFT_HIP)

            val rightShoulder = pose.getPoseLandmark(PoseLandmark.RIGHT_SHOULDER)
            val rightHip = pose.getPoseLandmark(PoseLandmark.RIGHT_HIP)

            val leftWrist = pose.getPoseLandmark(PoseLandmark.LEFT_WRIST)
            val rightWrist = pose.getPoseLandmark(PoseLandmark.RIGHT_WRIST)

            //BOTTOM PART
            val leftKnee = pose.getPoseLandmark(PoseLandmark.LEFT_KNEE)
            val leftAnkle = pose.getPoseLandmark(PoseLandmark.LEFT_ANKLE)

            val rightKnee = pose.getPoseLandmark(PoseLandmark.RIGHT_KNEE)
            val rightAnkle = pose.getPoseLandmark(PoseLandmark.RIGHT_ANKLE)


            //SHOUlDER
            val lShoulderX = leftShoulder!!.position.x
            val lShoulderY = leftShoulder.position.y
            val rShoulderX = rightShoulder!!.position.x
            val rShoulderY = rightShoulder.position.y


            //HIP
            val lHipX = leftHip!!.position.x
            val lHipY = leftHip.position.y
            val rHipX = rightHip!!.position.x
            val rHipY = rightHip.position.y


            //KNEE
            val lKneeX = leftKnee!!.position.x
            val lKneeY = leftKnee.position.y
            val rKneeX = rightKnee!!.position.x
            val rKneeY = rightKnee.position.y


            //ANKLE
            val lAnkleX = leftAnkle!!.position.x
            val lAnkleY = leftAnkle.position.y
            val rAnkleX = rightAnkle!!.position.x
            val rAnkleY = rightAnkle.position.y



            angleText += "Left angle:" + getAngle(
                leftHip,
                leftShoulder,
                leftWrist!!
            ) + "\nRight angle:"
            getAngle(rightHip, rightShoulder, rightWrist!!)

            drawPosPoints(
                lShoulderX,
                lShoulderY,
                rShoulderX,
                rShoulderY,
                lHipX,
                lHipY,
                rHipX,
                rHipY,
                lKneeX,
                lKneeY,
                rKneeX,
                rKneeY,
                lAnkleX,
                lAnkleY,
                rAnkleX,
                rAnkleY
            )

        } catch (e: Exception) {
            viewModel.isLoading.onNext(false)
            Toast.makeText(this, "Pose detection failed", Toast.LENGTH_SHORT).show()
        }

    }


    //Draw on canvas
    private fun drawPosPoints(
        lShoulderX: Float, lShoulderY: Float, rShoulderX: Float, rShoulderY: Float,
        lHipX: Float, lHipY: Float, rHipX: Float, rHipY: Float,
        lKneeX: Float, lKneeY: Float, rKneeX: Float, rKneeY: Float,
        lAnkleX: Float, lAnkleY: Float, rAnkleX: Float, rAnkleY: Float,
    ) {
        Timber.d("All points sucessfully detected")

        //To draw on canvas
        val shirtPaint = Paint()
        val trouserPaint = Paint()


        //Get average shirt color
        val shirtColorList = mutableListOf<Int>()
        shirtColorList.add(resizedBitmap!!.getPixel(rShoulderX.toInt(), rShoulderY.toInt()))
        shirtColorList.add(resizedBitmap!!.getPixel(rHipX.toInt(), rHipY.toInt()))
        shirtColorList.add(resizedBitmap!!.getPixel(lHipX.toInt(), lHipY.toInt()))
        shirtColorList.add(resizedBitmap!!.getPixel(lShoulderX.toInt(), lShoulderY.toInt()))


        val shirtColor = shirtColorList.averageColor()
        trouserPaint.color = shirtColor
        trouserPaint.strokeWidth = 3f

        //Get average trouser color
        val trouserColorList = mutableListOf<Int>()
        trouserColorList.add(resizedBitmap!!.getPixel(rKneeX.toInt(), rKneeY.toInt()))
        trouserColorList.add(resizedBitmap!!.getPixel(rAnkleX.toInt(), rAnkleY.toInt()))
        trouserColorList.add(resizedBitmap!!.getPixel(lKneeX.toInt(), lKneeY.toInt()))
        trouserColorList.add(resizedBitmap!!.getPixel(lAnkleX.toInt(), lAnkleY.toInt()))


        val trouserColor = trouserColorList.averageColor()
        shirtPaint.color = trouserColor
        shirtPaint.strokeWidth = 3f


        val drawBitmap = Bitmap.createBitmap(
            resizedBitmap!!.width,
            resizedBitmap!!.height,
            resizedBitmap!!.config
        )

        val canvas = Canvas(drawBitmap)

        //load original image into the canvas
        canvas.drawBitmap(resizedBitmap!!, 0f, 0f, null)


        //Draw lines on the image

        //Top part
        canvas.drawLine(lShoulderX, lShoulderY, rShoulderX, rShoulderY, shirtPaint)
        canvas.drawLine(rShoulderX, rShoulderY, rHipX, rHipY, shirtPaint)
        canvas.drawLine(rHipX, rHipY, lHipX, lHipY, shirtPaint)
        canvas.drawLine(lHipX, lHipY, lShoulderX, lShoulderY, shirtPaint)
        canvas.drawLine(rHipX, rHipY, lHipX, lHipY, shirtPaint)


        //Bottom part

        //Right leg
        canvas.drawLine(rHipX, rHipY, rKneeX, rKneeY, trouserPaint)
        canvas.drawLine(rKneeX, rKneeY, rAnkleX, rAnkleY, trouserPaint)

        //Left leg
        canvas.drawLine(lHipX, lHipY, lKneeX, lKneeY, trouserPaint)
        canvas.drawLine(lKneeX, lKneeY, lAnkleX, lAnkleY, trouserPaint)

        canvas.drawLine(lAnkleX, lAnkleY, rAnkleX, rAnkleY, trouserPaint)

        binding.imageView.setImageBitmap(drawBitmap)

        binding.angleText.text = angleText

        viewModel.isLoading.onNext(false)
    }


    private fun getAngle(
        startPoint: PoseLandmark,
        middlePoint: PoseLandmark,
        endPoint: PoseLandmark
    ): Double {

        var result = Math.toDegrees(
            atan2(
                endPoint.position.y - middlePoint.position.y,
                endPoint.position.x - middlePoint.position.x
            ).toDouble() -
                    atan2(
                        startPoint.position.y - middlePoint.position.y,
                        startPoint.position.x - middlePoint.position.x
                    ).toDouble()
        )

        result = abs(result)

        if (result > 180) {
            result = (360.0 - result)
        }

        return result
    }

    override fun onDestroy() {
        super.onDestroy()
        disposeBag.dispose()
    }
}