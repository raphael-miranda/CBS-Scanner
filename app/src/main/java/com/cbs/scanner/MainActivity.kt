package com.cbs.scanner

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.slider.Slider
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private val RunningNrKey: String = "RunningNr"
    private val FTPServerKey: String = "FTPServerAddress"
    private val FTPUserNameKey: String = "FTPUserName"
    private val FTPPasswordKey: String = "FTPPassword"
    private val FTPProtocolKey: String = "FTPProtocol"
    private val FTPPortKey: String = "FTPPort"
    private val LocationKey: String = "Location"

    private lateinit var sliderSelector: Slider
    private lateinit var chkManual: CheckBox

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

            if (isNewCarton(result.contents)) {
                txtLocation.text = getCurrentLocation()
                txtCartonNr.text = result.contents
                txtPartNr.text = ""
                txtDNr.text = ""
                txtQuantity.text = ""

                txtRunningNr.text = String.format(Locale.getDefault(), "%d", getCurrentRunningNr())
            } else {
                showAlert("Double Carton", "You scanned double carton.")
                clear()
            }

        }
    }

    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    private val qrCodeLauncher = registerForActivityResult<ScanOptions, ScanIntentResult>(
        ScanContract()
    ) { result: ScanIntentResult ->
        if (result.contents == null) {
            Toast.makeText(this, "Cancelled", Toast.LENGTH_LONG).show()
        } else {
            val parseData = result.contents.split(";")
            val carton = parseData[0]
            if (isNewCarton(carton)) {
                txtLocation.text = getCurrentLocation()
                txtCartonNr.text = parseData[0]
                txtPartNr.text = parseData[1]
                txtDNr.text = parseData[2]
                txtQuantity.text = parseData[3]

                txtRunningNr.text = String.format(Locale.getDefault(), "%d", getCurrentRunningNr())
            } else {
                showAlert("Double Carton", "You scanned double carton.")
                clear()
            }

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

        txtLocation.text = getCurrentLocation()
        txtRunningNr.text = String.format(Locale.getDefault(), "%d", getCurrentRunningNr())

        val btnSettings: ImageButton = findViewById(R.id.btnSettings)
        btnSettings.setOnClickListener {
            showSettingsDialog()
        }

        chkManual = findViewById(R.id.chkManual)
        chkManual.setOnCheckedChangeListener { _, result ->
            if (result) {
                txtPartNr.isEnabled = true
                txtCartonNr.isEnabled = true
                txtDNr.isEnabled = true
                txtQuantity.isEnabled = true
            } else {
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

        val btnNext: Button = findViewById(R.id.btnNext)
        btnNext.setOnClickListener {
            next()
        }

        val btnClear: Button = findViewById(R.id.btnClear)
        btnClear.setOnClickListener {
            clear()
        }

        val btnView: Button = findViewById(R.id.btnView)
        btnView.setOnClickListener {
            val intent = Intent(this, ViewActivity::class.java)
            startActivity(intent)
        }

        val btnReset: Button = findViewById(R.id.btnReset)
        btnReset.setOnClickListener {
            val dialog = AlertDialog.Builder(this)
                .setTitle("Reset Confirm")
                .setMessage("Are you sure to reset all?")
                .setPositiveButton("Yes") { _, _ ->
                    reset()
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.cancel()
                }
                .create()

            dialog.show()

        }

        val btnUpload: Button = findViewById(R.id.btnUpload)
        btnUpload.setOnClickListener {
            val sharedPreferences = getSharedPreferences(packageName, Context.MODE_PRIVATE)
            val ftpServer = sharedPreferences.getString(FTPServerKey, "")
            val ftpUsername = sharedPreferences.getString(FTPUserNameKey, "") as String
            val ftpPassword = sharedPreferences.getString(FTPPasswordKey, "") as String
            val ftpPort = sharedPreferences.getString(FTPPortKey, "") as String

            if (ftpServer.isNullOrEmpty()) {
                showAlert("Error", "You didn't register FTP Server credentials. Please register them in Settings.")

            } else if (ftpPort.isEmpty()) {
                uploadFileUsingFTP(ftpServer, ftpUsername, ftpPassword)
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    uploadFileUsingSFTP(ftpServer, ftpPort.toInt(), ftpUsername, ftpPassword)
                }
            }

        }

        // Initialize the permission launcher
        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            handlePermissionsResult(permissions)
        }
        checkAndRequestPermissions()
    }

    private fun checkAndRequestPermissions(): Boolean {
        val permissionsNeeded = mutableListOf<String>()

        if (Build.VERSION.SDK_INT <= 32) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            }
        } else {
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_IMAGES) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_IMAGES)
            }
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (checkSelfPermission(Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            permissionLauncher.launch(permissionsNeeded.toTypedArray())
            return false
        }

        return true // Permissions already granted
    }

    private fun handlePermissionsResult(permissions: Map<String, Boolean>) {
        permissions.forEach { (permission, isGranted) ->
            if (isGranted) {
                Log.d("Permissions", "$permission granted")
            } else {
                Log.d("Permissions", "$permission denied")
            }
        }
    }

    private fun showSettingsDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)

        // Find the input fields in the inflated layout
        val ftpServerEditText: EditText = dialogView.findViewById(R.id.txtFtpServer)
        val usernameEditText: EditText = dialogView.findViewById(R.id.txtUsername)
        val passwordEditText: EditText = dialogView.findViewById(R.id.txtPassword)

        // Create the dialog
        val dialog = AlertDialog.Builder(this)
            .setTitle("Enter FTP Credentials")
            .setView(dialogView)  // Use the custom inflated layout
            .setPositiveButton("Save") { _, _ ->

                val ftpServer = ftpServerEditText.text.toString()
                val username = usernameEditText.text.toString()
                val password = passwordEditText.text.toString()

                // save FTP credentials
                val sharedPreferences = getSharedPreferences(packageName, Context.MODE_PRIVATE)
                val editor = sharedPreferences.edit()
                editor.putString(FTPServerKey, ftpServer)
                editor.putString(FTPUserNameKey, username)
                editor.putString(FTPPasswordKey, password)
                editor.apply()
            }
            .setNegativeButton("Close") { dialog, _ ->
                dialog.cancel() // Close the dialog when "Cancel" is clicked
            }
            .create()

        dialog.show()
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


    private fun next() {
        if (txtCartonNr.text.isEmpty()) {
            showAlert("Empty Data", "There is no scanned data. Please scan data.")
            return
        }

        if (!checkAndRequestPermissions()) return

        try {

            var dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())

            dateFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
            val currentTime = dateFormat.format(Date())

            val scannedData = String.format(Locale.getDefault(), "%s | %s | %s | %s | %s | %s | %s | %s",
                paddedString(currentDate, 12),
                paddedString(currentTime, 12),
                paddedString(txtRunningNr.text.toString(), 12),
                paddedString(txtCartonNr.text.toString(), 12),
                paddedString(txtQuantity.text.toString(), 12),
                paddedString(txtDNr.text.toString(), 12),
                paddedString(txtLocation.text.toString(), 12),
                paddedString(txtPartNr.text.toString(), 12)
            )
            Log.d("==========", scannedData)

            // Create the file
            val file = File(getCBSScannerDir(this), "${currentDate}.txt")
            FileOutputStream(file, true).use { fos ->
                fos.write(scannedData.toByteArray())
                fos.write("\n".toByteArray())
            }
            Log.d("FileSave", "File saved at: ${file.absolutePath}")
            increaseRunningNr()
            clear()
            Toast.makeText(this, "Scanned data was saved successfully!", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Log.e("FileSave", "Error saving file: ${e.message}")
            Toast.makeText(this, "Save file failed!", Toast.LENGTH_LONG).show()
        }
    }

    private fun isNewCarton(carton: String) : Boolean {
        var dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val currentDate = dateFormat.format(Date())

        val file = File(getCBSScannerDir(this), "${currentDate}.txt")
        if (!file.exists()) {
            return true
        }

        var result = true

        try {
            val inputStream = FileInputStream(file)
            val inputStreamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputStreamReader)
            var line: String?

            // Read each line of the file
            while (bufferedReader.readLine().also { line = it } != null) {
                if (!line.isNullOrEmpty() && line!!.contains(carton)) {
                    result = false
                }
            }

            // Close the streams
            bufferedReader.close()
            inputStreamReader.close()
            inputStream.close()

        } catch (e: IOException) {
            e.printStackTrace()
        }

        return result
    }

    private fun clear() {
        txtRunningNr.text = String.format(Locale.getDefault(), "%d", getCurrentRunningNr())
        txtLocation.text = getCurrentLocation()
        txtCartonNr.text = ""
        txtPartNr.text = ""
        txtDNr.text = ""
        txtQuantity.text = ""
    }

    private fun reset() {
        deleteCurrentFile()

        val sharedPreferences = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putInt(RunningNrKey, 1)
        editor.putString(LocationKey, "PA")
        editor.apply()
    }

    private fun uploadFileUsingFTP(ftpServer: String, ftpUsername: String, ftpPassword: String) {

        val ftpClient = FTPClient()
        try {
            val dir = getCBSScannerDir(this)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            val file = File(dir, "${currentDate}.txt")
            if (!file.exists()) {
                showAlert("Error", "There is no file to upload!")
                return
            }

            ftpClient.connect(ftpServer)
            ftpClient.login(ftpUsername, ftpPassword)
            ftpClient.enterLocalPassiveMode()
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE)

            val inputStream = file.inputStream()
            val remoteFileName = file.name

            val success = ftpClient.storeFile(remoteFileName, inputStream)
            inputStream.close()

            if (success) {
                Log.d("FTP", "File uploaded successfully")

                AlertDialog.Builder(this)
                    .setTitle("Upload Success")
                    .setMessage("File uploaded successfully.")
                    .setPositiveButton("OK") { dialog, _ ->
                        reset()
                        dialog.dismiss()
                    }
                    .create()
                    .show()

            } else {
                Log.e("FTP", "File upload failed")
                showAlert("Upload failed!", "File upload failed.")
            }
        } catch (e: Exception) {
            Log.e("FTP", "Error uploading file: ${e.message}")
            showAlert("Upload failed!", "Error uploading file: ${e.message}")
        } finally {
            try {
                ftpClient.logout()
                ftpClient.disconnect()
            } catch (e: Exception) {
                Log.e("FTP", "Error disconnecting: ${e.message}")
            }
        }
    }

    fun uploadFileUsingSFTP(
        host: String,
        port: Int,
        username: String,
        password: String
    ) {
        try {
            val dir = getCBSScannerDir(this)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            val file = File(dir, "${currentDate}.txt")
            if (!file.exists()) {
                showAlert("Error", "There is no file to upload!")
                return
            }

            val jsch = JSch()
            val session: Session = jsch.getSession(username, host, port)
            session.setPassword(password)

            // Disable host key checking for simplicity (use a more secure approach in production)
            val config = java.util.Properties()
            config["StrictHostKeyChecking"] = "no"
            session.setConfig(config)

            // Connect to the server
            session.connect()

            // Open the SFTP channel
            val channel = session.openChannel("sftp") as ChannelSftp
            channel.connect()
            println("SFTP channel opened.")

            val defaultDir = channel.pwd()

            // Upload the file
            val fileInputStream  = FileInputStream(file)
            channel.put(fileInputStream , file.name)
            fileInputStream .close()

            println("File uploaded successfully to $defaultDir/${file.name}")
            AlertDialog.Builder(this)
                .setTitle("Upload Success")
                .setMessage("File uploaded successfully.")
                .setPositiveButton("OK") { dialog, _ ->
                    reset()
                    dialog.dismiss()
                }
                .create()
                .show()

            // Disconnect the channel and session
            channel.disconnect()
            session.disconnect()
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error uploading file: ${e.message}")
            showAlert("Upload Failed", "Error uploading file: ${e.message}")
        }
    }

    private fun getCurrentLocation(): String? {
        val sharedPreferences = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        return sharedPreferences.getString(LocationKey, "PA")
    }

    private fun saveCurrentLocation(location: String) {
        val sharedPreferences = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(LocationKey, location)
        editor.apply()
    }

    private fun getCurrentRunningNr(): Int {
        val sharedPreferences = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        return sharedPreferences.getInt(RunningNrKey, 1)
    }


    private fun increaseRunningNr() {
        val sharedPreferences = getSharedPreferences(packageName, Context.MODE_PRIVATE)
        val oldRunningNr = sharedPreferences.getInt(RunningNrKey, 1)
        val editor = sharedPreferences.edit()
        editor.putInt(RunningNrKey, oldRunningNr + 1)
        editor.putString(LocationKey, txtLocation.text.toString())
        editor.apply()
    }

    private fun deleteCurrentFile() {
        try {
            val dir = getCBSScannerDir(this)

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val currentDate = dateFormat.format(Date())
            val file = File(dir, "${currentDate}.txt")

            if (file.exists()) {
                val deleted = file.delete()
                if (deleted) {
                    println("File deleted successfully: ${file.absolutePath}")
                } else {
                    println("Failed to delete file: ${file.absolutePath}")
                }
            } else {
                println("File does not exist: ${file.absolutePath}")
            }
        } catch (e: Exception) {
            println("Error deleting file: ${e.message}")
        }
    }

    private fun showAlert(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

}