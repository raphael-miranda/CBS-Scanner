package com.cbs.scanner

import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.slider.Slider
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var sliderSelector: Slider
    private lateinit var chkManual: CheckBox

    private lateinit var txtRunningNr: TextView
    private lateinit var txtLocation: TextView
    private lateinit var txtPartNr: TextView
    private lateinit var txtCartonNr: TextView
    private lateinit var txtDNr: TextView
    private lateinit var txtQuantity: TextView

    private var runningNumber: Int = 0


    private val barcodeLauncher = registerForActivityResult<ScanOptions, ScanIntentResult>(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents == null) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
        } else {
            txtLocation.text = "PA"
            txtCartonNr.text = result.contents
            txtPartNr.text = ""
            txtDNr.text = ""
            txtQuantity.text = ""

            runningNumber += 1
            txtRunningNr.text = String.format(Locale.getDefault(), "%d", runningNumber)
        }
    }

    private val qrCodeLauncher = registerForActivityResult<ScanOptions, ScanIntentResult>(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents == null) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
        } else {
            val parseData = result.contents.split(";")
            txtLocation.text = "PA"
            txtCartonNr.text = parseData[0]
            txtPartNr.text = parseData[1]
            txtDNr.text = parseData[2]
            txtQuantity.text = parseData[3]

            runningNumber += 1
            txtRunningNr.text = String.format(Locale.getDefault(), "%d", runningNumber)
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

        sliderSelector = findViewById(R.id.sliderSelector)
        txtRunningNr = findViewById(R.id.txtRunning)
        txtLocation = findViewById(R.id.txtLocation)
        txtPartNr = findViewById(R.id.txtPart)
        txtCartonNr = findViewById(R.id.txtCarton)
        txtDNr = findViewById(R.id.txtD)
        txtQuantity = findViewById(R.id.txtQuantity)

        val btnSettings: ImageButton = findViewById(R.id.btnSettings)
        btnSettings.setOnClickListener {

        }

        chkManual = findViewById(R.id.chkManual)
        chkManual.setOnCheckedChangeListener { button, result ->
            if (result) {
                txtLocation.isEnabled = true
                txtPartNr.isEnabled = true
                txtCartonNr.isEnabled = true
                txtDNr.isEnabled = true
                txtQuantity.isEnabled = true
            } else {
                txtLocation.isEnabled = false
                txtPartNr.isEnabled = false
                txtCartonNr.isEnabled = false
                txtDNr.isEnabled = false
                txtQuantity.isEnabled = false
            }
        }

        val btnScan : Button = findViewById(R.id.btnScan)
        btnScan.setOnClickListener {
            scan()
        }

    }

    private fun scan() {

        val options = ScanOptions()
        options.setPrompt("Scan a code")
        options.setCameraId(0) // Use a specific camera of the device
        options.setBeepEnabled(true)
        options.setBarcodeImageEnabled(true)
        options.setOrientationLocked(true)
        options.setCaptureActivity(PortraitCaptureActivity::class.java)

        val sliderValue = sliderSelector.value
        if (sliderValue >= 1.5f) {
            options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
            qrCodeLauncher.launch(options)
        } else {
            options.setDesiredBarcodeFormats(
                ScanOptions.CODE_128,
                ScanOptions.CODE_39,
                ScanOptions.CODE_93,
                ScanOptions.EAN_8,
                ScanOptions.EAN_13,
                ScanOptions.ITF,
                ScanOptions.UPC_A,
                ScanOptions.UPC_E,
                ScanOptions.PDF_417,
                ScanOptions.DATA_MATRIX
            )
            barcodeLauncher.launch(options)
        }
    }


}