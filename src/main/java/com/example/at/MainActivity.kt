package com.example.at

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText

import android.widget.TextView
import android.widget.Toast
import android_serialport_api.SerialPort
import android_serialport_api.SerialPortFinder
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    val SERIAL_PORT_NANE = "/dev/smd11"
    val SERIAL_BAUDRATE = 9600

    var serialPort: SerialPort? = null
    var inputStream: InputStream? = null
    var outputStream: OutputStream? = null
    lateinit var serialThread: Thread
     override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
         OpenSerialPort(SERIAL_PORT_NANE)
         StartRxThread()
         val btn = findViewById<Button>(R.id.button)
         btn.setOnClickListener {
             if(findViewById<EditText>(R.id.port).text.toString() == null
                 || findViewById<EditText>(R.id.atcommand).text.toString() == null){
                 Toast.makeText(this, "포트와 원하는 커멘드를 입력하세요",Toast.LENGTH_SHORT)
             }else{
                 try {
                     var data = findViewById<EditText>(R.id.atcommand).text.toString()
                     Log.d("SerialExam","send date: $data")
                     SendData(data+'\r')
                     StartRxThread()
                     serialThread.interrupt()
                 } catch (e: IOException){
                     e.printStackTrace()
                 }

             }
         }
    }

    private fun OpenSerialPort(name: String) {
//        val serialPortFinder: SerialPortFinder = SerialPortFinder()
//        val devices: Array<String> = serialPortFinder.allDevices
//        val devicesPath: Array<String> = serialPortFinder.allDevicesPath
//        for (device in devices) {
//            if (device.contains(name, true)) {
//                val index = devices.indexOf(device)
//                serialPort = SerialPort(File(devicesPath[index]), SERIAL_BAUDRATE, 0)
//                break
//            }
//        }
        serialPort = SerialPort(File(SERIAL_PORT_NANE),SERIAL_BAUDRATE,0)
        if(serialPort == null){
            Log.d("SerialExam","cant open port")
        }
        serialPort?.let {
            inputStream = it.inputStream
            outputStream = it.outputStream
        }
    }

    private fun StartRxThread() {
        if(inputStream == null){
            Log.e("SerialExam","Can't open inputstream")
            return
        }
        serialThread = Thread{
            Log.d("SerialExam","start thread")
            while(true){
                try{
                    var buffer = ByteArray(1024)
                    val size = inputStream?.read(buffer)
                    OnReceiveData(buffer, size?:0)
                }catch (e: IOException){e.printStackTrace()}
            }
        }
        serialThread.start()
    }
    private fun OnReceiveData(buffer: ByteArray, size: Int){
        if(size<1)return

        var strBuilder = StringBuilder()
        for (i in 0 until size){
            strBuilder.append(String.format("%c", buffer[i].toInt()))
        }
        var text = findViewById<TextView>(R.id.textView2)
        runOnUiThread { text.text = strBuilder}
        Log.d("SerialExam", "rx:$strBuilder")
    }
    private fun SendData(data: String){
        try {
            outputStream?.write(data.encodeToByteArray())
        }catch (e: IOException){e.printStackTrace()}
    }

    override fun onDestroy() {
        serialThread.interrupt()
        serialPort?.close()
        super.onDestroy()
    }
}