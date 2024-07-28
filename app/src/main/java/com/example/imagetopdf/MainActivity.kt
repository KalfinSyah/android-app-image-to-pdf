package com.example.imagetopdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.File
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Image
import com.itextpdf.io.image.ImageDataFactory
import com.itextpdf.io.source.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {
    private lateinit var btnSelectImg: Button
    private lateinit var btnConvertToPdf: Button
    private lateinit var txtvNoImgSelected: TextView
    private lateinit var txtvStatus: TextView
    private lateinit var selectedImgUri: String
    private lateinit var imgName: String

    private val selectImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImgUri = uri.toString()
            imgName = getImageName(uri)
            txtvNoImgSelected.text =imgName
            btnConvertToPdf.isEnabled = true
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnSelectImg = findViewById<Button>(R.id.btnSelectImg)
        btnConvertToPdf = findViewById<Button>(R.id.btnConvertToPdf)
        txtvNoImgSelected = findViewById<TextView>(R.id.txtvNoImgSelected)
        txtvStatus = findViewById<TextView>(R.id.txtvStatus)

        btnSelectImg.setOnClickListener {
            selectImg()
        }
        btnConvertToPdf.setOnClickListener {
            convertImg()
        }
    }

    private fun selectImg() {
        selectImageLauncher.launch("image/jpeg,image/jpg/,image/png")
    }
    private fun convertImg() {
        try {
            // Get the Bitmap from the URI
            val imageUri = Uri.parse(selectedImgUri)
            val inputStream = contentResolver.openInputStream(imageUri) ?: return
            val bitmap = BitmapFactory.decodeStream(inputStream)

            // Define the path for the PDF
            val pdfDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), "ImageConverter")
            if (!pdfDir.exists()) {
                pdfDir.mkdirs()
            }
            val pdfFile = File(pdfDir, "${imgName.substringBeforeLast(".")}.pdf")
            val pdfWriter = PdfWriter(pdfFile.absolutePath)
            val pdfDocument = PdfDocument(pdfWriter)
            val document = Document(pdfDocument)

            // Convert Bitmap to iText Image
            val imageStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, imageStream)
            val imageData = ImageDataFactory.create(imageStream.toByteArray())
            val itextImage = Image(imageData)

            // Add the image to the PDF
            document.add(itextImage)

            // Close the document
            document.close()

            // Update status text view
            txtvStatus.text = "PDF created successfully: ${pdfFile.absolutePath}"
        } catch (e: Exception) {
            e.printStackTrace()
            txtvStatus.text = "Failed to create PDF: ${e.message}"
        }
    }
    private fun getImageName(uri: Uri): String {
        var name = "Unknown"
        val cursor = contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }
}