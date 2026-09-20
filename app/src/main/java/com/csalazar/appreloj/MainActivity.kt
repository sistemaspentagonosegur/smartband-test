package com.csalazar.appreloj

import android.Manifest
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.csalazar.appreloj.ui.theme.AppRelojTheme
import com.csalazar.appreloj.util.startMqttService
import com.oudmon.ble.base.bean.SleepDetail
import com.oudmon.ble.base.bluetooth.DeviceManager
import com.oudmon.ble.base.communication.CommandHandle
import com.oudmon.ble.base.communication.Constants
import com.oudmon.ble.base.communication.LargeDataHandler
import com.oudmon.ble.base.communication.bigData.bean.ContactBean
import com.oudmon.ble.base.communication.req.SimpleKeyReq
import com.oudmon.ble.base.util.SleepAnalyzerUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.collections.mutableListOf
import kotlin.time.Duration.Companion.milliseconds


const val TAG = "RESULTADO66"
class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkAndRequestBluetoothPermissions()
        enableEdgeToEdge()
        setContent {
            AppRelojTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { paddingValues ->
                    MainPage(paddingValues)
                }
            }
        }
        subscribeToViewModel()
    }

    private val requestBluetoothPermissions = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val scanGranted = permissions[Manifest.permission.BLUETOOTH_SCAN] ?: false
        val connectGranted = permissions[Manifest.permission.BLUETOOTH_CONNECT] ?: false
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        if (scanGranted || locationGranted) {
            Log.d("BLUECS", "PERMISOS CONCEDIDOS")
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

    private fun subscribeToViewModel() {
        lifecycleScope.launch {

            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.bluetoothEvents.collect { state ->
                    if(state.lastOrNull()?.connected ?: false) {
                        lifecycleScope.launch {
                            startMqttService()
                        }
                    }

                }
            }
        }
    }
}

@Composable
fun MainPage(
    paddingValues: PaddingValues = PaddingValues(8.dp),
    viewModel: MainViewModel = viewModel()
) {
    val deviceList by viewModel.devices.collectAsState()

    Column(modifier = Modifier.padding(paddingValues)) {
        Button(
            onClick = {
                viewModel.startScanning()
            }
        ) {
            Text("Escanear nuevamente")
        }
        LazyColumn(
            contentPadding = PaddingValues(16.dp), // Margen exterior de la lista completa
            verticalArrangement = Arrangement.spacedBy(8.dp) // Espaciado entre items
        ) {
            items(
                items = deviceList,
                key = { device ->
                    device.deviceAddress ?: device.deviceName
                } // La clave (key) optimiza el rendimiento en recomposiciones
            ) { device ->
                Card(
                    onClick = {
                        viewModel.connectDevice(device)
                    }
                ) {
                    Column (modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                        Text(device.deviceName)
                        device.deviceAddress?.let { Text(it, fontSize = 8.sp) }
                    }
                }
            }
        }
    }
}


fun sleep() {
//        final SleepDetail[] b = new SleepDetail[1];
//        final SleepDetail[] yes = new SleepDetail[1];
//        SleepAnalyzerUtils.getInstance().syncSleepDetail("E7:E9:42:AE:59:1B", 1, new ISleepCallback() {
//            @Override
//            public void sleepData(SleepDetail detail) {
//                yes[0] =detail;
//                Log.i(TAG, yes[0].toString());
//            }
//        });
//        SleepAnalyzerUtils.getInstance().syncSleepDetail("E7:E9:42:AE:59:1B", 2, new ISleepCallback() {
//            @Override
//            public void sleepData(SleepDetail detail) {
//                b[0] =detail;
//                Log.i(TAG, b[0].toString());
//            }
//        });
    SleepAnalyzerUtils.getInstance()
        .syncSleepReturnSleepDisplay(DeviceManager.getInstance().deviceAddress, 5) { display ->
            if (display != null) {
                Log.i(TAG, display.toString())
            }
        }
}

fun newSleep() {
//        LargeDataHandler.getInstance().syncSleepList(0xff) { resp ->
//            if (resp != null) {
//                Log.i(TAG, resp.toString())
//            }
//        }
}

fun contactList(){
    val list= mutableListOf<ContactBean>()
    for (item in 1..50){
        val bean=ContactBean()
        bean.contactName= "11111$item"
        bean.phoneNumber="18682313467"
        list.add(bean)
    }
    LargeDataHandler.getInstance().syncContactMore(list) {
        Log.i(TAG, it.type.toString())
    }
}

fun calc() {
    val y = SleepDetail()
    y.deviceAddress = "E7:E9:42:AE:59:1B"
    y.dateStr = "2021-09-06"
    y.index_str =
        "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35"
    y.quality =
        "515022,515013,515031,515040,515074,515062,515055,515023,515035,515040,515070,515090,515061,515030,515040,515073,515090,515072,515057,515043,515051,515128,515070,515092,515066,515030,515052,515070,515128,515128,515076,515114,515063,515031,515040,205087"
    y.interval = 900
    val b = SleepDetail()
    b.deviceAddress = "E7:E9:42:AE:59:1B"
    b.dateStr = "2021-09-05"
    b.index_str = "0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,92,93,94,95"
    b.quality =
        "515033,515040,515070,515060,515051,515020,515031,515052,515077,515090,515128,515071,515090,515063,515030,515041,515070,515090,515075,515051,515042,515059,515070,215094,114084,515065,515051,515033"
    b.interval = 900
    val display = SleepAnalyzerUtils.getInstance().getNewDisplayModel(y, b)
    Log.i(TAG, display.toString())
}
