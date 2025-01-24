package com.cbs.scanner

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions


class MainActivity : AppCompatActivity() {

    private lateinit var txtRunningNr: TextView
    private lateinit var txtLocation: TextView
    private lateinit var txtPartNr: TextView
    private lateinit var txtCartonNr: TextView
    private lateinit var txtDNr: TextView
    private lateinit var txtQuantity: TextView

    private val barcodeLauncher = registerForActivityResult<ScanOptions, ScanIntentResult>(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents == null) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(
                this,
                "Scanned: " + result.contents,
                Toast.LENGTH_LONG
            ).show()
            Log.d("===========", result.contents)

            val parseData = result.contents.split(";")
            txtLocation.setText("PA")
            txtCartonNr.setText(parseData[0])
            txtPartNr.setText(parseData[1])
            txtDNr.setText(parseData[2])
            txtQuantity.setText(parseData[3])
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

        txtRunningNr = findViewById(R.id.txtRunning)
        txtLocation = findViewById(R.id.txtLocation)
        txtPartNr = findViewById(R.id.txtPart)
        txtCartonNr = findViewById(R.id.txtCarton)
        txtDNr = findViewById(R.id.txtD)
        txtQuantity = findViewById(R.id.txtQuantity)

        val btnSettings: ImageButton = findViewById(R.id.btnSettings)
        btnSettings.setOnClickListener {

        }

        val btnScan : Button = findViewById(R.id.btnScan)
        btnScan.setOnClickListener {
            scan()
        }

    }

    private fun scan() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.ALL_CODE_TYPES)
        options.setPrompt("Scan a barcode")
        options.setCameraId(0) // Use a specific camera of the device
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        barcodeLauncher.launch(options)
    }


}