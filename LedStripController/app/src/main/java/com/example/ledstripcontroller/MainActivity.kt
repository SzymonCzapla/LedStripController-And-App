package com.example.ledstripcontroller

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.viewinterop.AndroidView
import android.view.LayoutInflater
import com.skydoves.colorpickerview.ColorPickerView
import com.skydoves.colorpickerview.listeners.ColorEnvelopeListener
import com.example.ledstripcontroller.ui.theme.LedStripControllerTheme
import kotlinx.coroutines.launch
import android.graphics.Color as AndroidColor
import java.util.*


enum class ConnectionState {
    DISCONNECTED, CONNECTING, CONNECTED, ERROR
}

class MainActivity : ComponentActivity() {

    private var bluetoothGatt: BluetoothGatt? = null
    private val connectionState = mutableStateOf(ConnectionState.DISCONNECTED)
    private val errorMessage = mutableStateOf<String?>(null)

    private lateinit var settingsRepository: SettingsRepository

    companion object {
        private val SERVICE_UUID: UUID =
            UUID.fromString("19b10000-e8f2-537e-4f6c-d104768a1214")
        private val CHAR_UUID: UUID =
            UUID.fromString("19b10001-e8f2-537e-4f6c-d104768a1215")
    }

    private fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    private fun hasPermission(permission: String): Boolean =
        ActivityCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            startBleScan()
        } else {
            connectionState.value = ConnectionState.ERROR
            errorMessage.value = "Brak wymaganych uprawnień"
        }
    }
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        settingsRepository = SettingsRepository(applicationContext)

        setContent {
            val settings by settingsRepository.settings.collectAsStateWithLifecycle(
                initialValue = LedSettings()
            )

            val brightness = settings.brightness.toFloat()
            val red = settings.red
            val green = settings.green
            val blue = settings.blue

            LedStripControllerTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainUI(
                        connectionState = connectionState.value,
                        errorMessage = errorMessage.value,
                        brightness = brightness,
                        red = red,
                        green = green,
                        blue = blue,
                        onBrightnessChange = { newBrightness ->
                            lifecycleScope.launch {
                                settingsRepository.saveSettings(
                                    brightness = newBrightness.toInt()
                                )
                            }
                            sendCommand(newBrightness.toInt().toString())
                        },
                        onRetry = {
                            bluetoothGatt?.close()
                            bluetoothGatt = null
                            connectionState.value = ConnectionState.DISCONNECTED
                            checkPermissionsAndStartScan()
                        },
                        onSendColor = { r, g, b ->
                            lifecycleScope.launch {
                                settingsRepository.saveSettings(
                                    red = r,
                                    green = g,
                                    blue = b,
                                    mode = "color"
                                )
                            }
                            sendCommand("color,$r,$g,$b")
                        },
                        onRainbow = {
                            lifecycleScope.launch {
                                settingsRepository.saveSettings(
                                    mode = "rainbow"
                                )
                            }
                            sendCommand("rainbow")
                        },
                        onOff = {
                            lifecycleScope.launch {
                                settingsRepository.saveSettings(
                                    red = 0,
                                    green = 0,
                                    blue = 0,
                                    mode = "off"
                                )
                            }
                            sendCommand("color,0,0,0")
                        }
                    )
                }
            }
        }
        checkPermissionsAndStartScan()
    }

    @SuppressLint("MissingPermission")
    override fun onDestroy() {
        super.onDestroy()
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothManager?.adapter?.bluetoothLeScanner?.stopScan(scanCallback)
        bluetoothGatt?.close()
        bluetoothGatt = null
    }


    private fun checkPermissionsAndStartScan() {
        val needed = requiredPermissions()
        val allGranted = needed.all { hasPermission(it) }
        if (allGranted) {
            startBleScan()
        } else {
            permissionLauncher.launch(needed)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleScan() {
        val bluetoothManager = getSystemService(BluetoothManager::class.java)
        val bluetoothAdapter = bluetoothManager?.adapter
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            connectionState.value = ConnectionState.ERROR
            errorMessage.value = "Bluetooth jest wyłączony"
            return
        }

        val scanner = bluetoothAdapter.bluetoothLeScanner ?: run {
            connectionState.value = ConnectionState.ERROR
            errorMessage.value = "Skaner BLE niedostępny"
            return
        }

        scanner.stopScan(scanCallback)

        connectionState.value = ConnectionState.CONNECTING
        if (hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            scanner.startScan(scanCallback)
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.device?.let { device ->
                if (device.name == "LED STRIP") {
                    if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                        connectionState.value = ConnectionState.CONNECTING
                        device.connectGatt(this@MainActivity, false, gattCallback)
                    }
                }
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                connectionState.value = ConnectionState.ERROR
                errorMessage.value = "Błąd GATT: $status"
                gatt?.close()
                bluetoothGatt = null
                return
            }

            when (newState) {
                BluetoothProfile.STATE_CONNECTED -> {
                    connectionState.value = ConnectionState.CONNECTED
                    errorMessage.value = null
                    bluetoothGatt = gatt
                    if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
                        gatt?.discoverServices()
                    }
                }
                BluetoothProfile.STATE_DISCONNECTED -> {
                    connectionState.value = ConnectionState.DISCONNECTED
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun sendCommand(command: String) {
        val gatt = bluetoothGatt ?: return
        val service = gatt.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHAR_UUID) ?: return

        val data = command.toByteArray(Charsets.UTF_8)

        if (hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeCharacteristic(
                    characteristic,
                    data,
                    BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                )
            } else {
                @Suppress("DEPRECATION")
                characteristic.value = data
                @Suppress("DEPRECATION")
                gatt.writeCharacteristic(characteristic)
            }
        }
    }

}

@Composable
fun MainUI(
    connectionState: ConnectionState,
    errorMessage: String?,
    brightness: Float,
    red: Int,
    green: Int,
    blue: Int,
    onBrightnessChange: (Float) -> Unit,
    onRetry: () -> Unit,
    onSendColor: (Int, Int, Int) -> Unit,
    onRainbow: () -> Unit,
    onOff: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Stan połączenia: $connectionState")

        errorMessage?.let {
            Snackbar { Text(it) }
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (connectionState != ConnectionState.CONNECTED) {
            ActionButton(text = "Połącz ponownie", onClick = onRetry)
        } else {
            Text("✅ Połączono")
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text("Jasność: ${brightness.toInt()}")
        Slider(
            value = brightness,
            onValueChange = onBrightnessChange,
            valueRange = 0f..255f
        )

        Spacer(modifier = Modifier.height(32.dp))

        AndroidView(
            factory = { context ->
                val view = LayoutInflater.from(context)
                    .inflate(R.layout.view_color_picker, null)
                val pickerView = view.findViewById<ColorPickerView>(R.id.colorPickerView)

                val startColor = AndroidColor.rgb(red, green, blue)
                pickerView.setInitialColor(startColor)

                pickerView.setColorListener(
                    ColorEnvelopeListener { envelope, _ ->
                        val rr = envelope.argb[1]
                        val gg = envelope.argb[2]
                        val bb = envelope.argb[3]
                        onSendColor(rr, gg, bb)
                    }
                )
                view
            },
            modifier = Modifier.size(300.dp),
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionButton("🌈 Rainbow", onRainbow)
            ActionButton("⭕ Off", onOff)
        }
    }
}

@Composable
fun ActionButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick) {
        Text(text)
    }
}