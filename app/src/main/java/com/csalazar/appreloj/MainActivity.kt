package com.csalazar.appreloj

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanResult
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ListView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.csalazar.appreloj.domain.SmartWatch
import com.csalazar.appreloj.ui.theme.AppRelojTheme
import com.oudmon.ble.base.scan.BleScannerHelper
import com.oudmon.ble.base.scan.ScanRecord
import com.oudmon.ble.base.scan.ScanWrapperCallback
import java.util.UUID
import kotlin.collections.mutableListOf

class MainActivity : ComponentActivity() {

    val deviceList = mutableListOf<SmartWatch>()
    private var scanSize:Int=0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestBluetoothPermissions()
        enableEdgeToEdge()
        setContent {
            AppRelojTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->

                }
            }
        }
    }

    fun startBluetoothScan() {
        BleScannerHelper.getInstance().scanDevice(this, UUID.randomUUID(), )
    }

    private val requestBluetoothPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val scanGranted = permissions[Manifest.permission.BLUETOOTH_SCAN] ?: false
        val connectGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        if (scanGranted || locationGranted) {
            Log.d("BLUECS", "PERMISOS CONCEDIDOS")
            startBluetoothScan()
        } else {
            // El usuario denegó los permisos
        }
    }

    fun checkAndRequestBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            requestBluetoothPermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            // Android 11 o inferior requiere ubicación para escaneo Bluetooth
            requestBluetoothPermissions.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }
}

@Composable
fun MainPage(innerPadding: Dp) {
    val deviceList = remember {
        mutableListOf<SmartWatch>()
    }

    Column(modifier = Modifier.padding(innerPadding)) {

        Button(
            onClick = {
                startBluetoothScan()
            }
        ) {
            Text("Escanear nuevamente")
        }
        LazyColumn (
            contentPadding = PaddingValues(16.dp), // Margen exterior de la lista completa
            verticalArrangement = Arrangement.spacedBy(8.dp) // Espaciado entre items
        ) {
            items(
                items = deviceList,
                key = { device -> device.deviceAddress ?: device.deviceName} // La clave (key) optimiza el rendimiento en recomposiciones
            ) { device ->
                Card  {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Text(device.deviceName)
                    }
                }
            }
        }
    }
}