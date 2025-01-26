package com.cbs.scanner

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ViewActivity : AppCompatActivity() {

    private lateinit var txtFileName: TextView
    private lateinit var txtTitle: TextView
    private lateinit var txtContent: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_view)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val btnBack: ImageButton = findViewById(R.id.btnBack)
        btnBack.setOnClickListener {
            finish()
        }

        txtFileName = findViewById(R.id.txtFileName)
        txtTitle = findViewById(R.id.txtTitle)
        txtContent = findViewById(R.id.txtContent)

        val title = String.format(Locale.getDefault(), "%s | %s | %s | %s | %s | %s | %s | %s",
            paddedString("Current Date", 12),
            paddedString("Current Time", 12),
            paddedString("Running Nr.", 12),
            paddedString("Carton Nr.", 12),
            paddedString("Qtty", 12),
            paddedString("D-Nr", 12),
            paddedString("Location", 12),
            paddedString("Part Nr.", 12)
        )
        txtTitle.text = title

        showContent()
    }


    @SuppressLint("SetTextI18n")
    private fun showContent() {
        val dir = getCBSScannerDir(this)
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val file = File(dir, "${currentDate}.txt")
        if (!file.exists()) {
            return
        }

        txtFileName.text = "${currentDate}.txt"

        try {
            val inputStream = FileInputStream(file)
            val inputStreamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            val stringBuilder = StringBuilder()
            var line: String?

            // Read each line of the file
            while (bufferedReader.readLine().also { line = it } != null) {
                stringBuilder.append(line).append("\n")
            }

            // Close the streams
            bufferedReader.close()
            inputStreamReader.close()
            inputStream.close()

            txtContent.text = stringBuilder.toString()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

}