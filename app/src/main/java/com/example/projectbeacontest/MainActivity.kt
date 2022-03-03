package com.example.projectbeacontest

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import org.w3c.dom.Text
import java.nio.ByteBuffer
import java.util.*


class MainActivity : AppCompatActivity() {

    private var mScanFilter: ScanFilter? = null
    private var mScanSettings: ScanSettings? = null

    private lateinit var mBluetoothAdapter : BluetoothAdapter

    protected var mScanCallback = object: ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult?) {

            Log.i("scancallback", "called")
            var mScanRecord: ScanRecord? = result?.getScanRecord()
            var manufacturerData: ByteArray? = mScanRecord?.getManufacturerSpecificData(224)
            var mRssi: Int = result?.getRssi() ?: -1    // rssi used to compute distance

            findViewById<TextView>(R.id.textView).text = mRssi.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Log.i("app:","Started")

        var mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        // Detect beacons
        var mBluetoothLeScanner = mBluetoothAdapter.bluetoothLeScanner

        Log.i("ble", mBluetoothLeScanner.toString())

        // Emit beacon
        var mBluetoothLeAdvertiser = mBluetoothAdapter.bluetoothLeAdvertiser

        setScanFilter()
        setScanSettings()

        // PERMISSION CHECK FOR BLUETOOTHLESCANNER
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }

        mBluetoothLeScanner.startScan(Arrays.asList(mScanFilter), mScanSettings, mScanCallback);

    }

    private fun setScanFilter() {
        var mBuilder : ScanFilter.Builder = ScanFilter.Builder()
        var mManufacturerData : ByteBuffer = ByteBuffer.allocate(23)
        var mManufacturerDataMask : ByteBuffer = ByteBuffer.allocate(24)

        var uuidString = "0CF052C297CA407C84F8B62AAC4E9020"
        var uuid : ByteArray = uuidString.toByteArray()

        mManufacturerData.put(0, 0xBE.toByte())
        mManufacturerData.put(1, 0xAC.toByte())

        for (i in 2..17) {
            mManufacturerData.put(i, uuid[i - 2])
        }

        for (i in 0..17) {
            mManufacturerDataMask.put(0x01.toByte())
        }

        mBuilder.setManufacturerData(224, mManufacturerData.array(), mManufacturerDataMask.array());
        mScanFilter = mBuilder.build()

        Log.i("scanFilter", mScanFilter.toString())
    }

    private fun setScanSettings() {
        val mBuilder = ScanSettings.Builder()
        mBuilder.setReportDelay(0)
        mBuilder.setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
        mScanSettings = mBuilder.build()

        Log.i("scanSettings", mScanSettings.toString())
    }

    fun calculateDistance(txPower: Int, rssi: Double): Double {
        if (rssi == 0.0) {
            return -1.0 // if we cannot determine accuracy, return -1.
        }
        val ratio = rssi * 1.0 / txPower
        return if (ratio < 1.0) {
            Math.pow(ratio, 10.0)
        } else {
            0.89976 * Math.pow(ratio, 7.7095) + 0.111
        }
    }

    private fun getDistance(accuracy : Double) : String {
        if (accuracy == -1.0) {
            return "Unknown";
        } else if (accuracy < 1) {
            return "Immediate";
        } else if (accuracy < 3) {
            return "Near";
        } else {
            return "Far";
        }
    }
}