package com.example.at

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText

import android.widget.TextView
import android.widget.Toast
import android_serialport_api.SerialPort
import android_serialport_api.SerialPortFinder
import java.io.*

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
         try {
             val su = Runtime.getRuntime().exec("su")
             val outputStream = DataOutputStream(su.outputStream)
             outputStream.writeBytes("setenforce 0\n")
             outputStream.flush()
             outputStream.writeBytes("exit\n")
             outputStream.flush()
             su.waitFor()
         } catch (e: IOException) {
             Log.d("SerialExam", "ex: $e")
         } catch (e: InterruptedException) {
             Log.d("SerialExam", "ex: $e")
         }
         OpenSerialPort(SERIAL_PORT_NANE)
         StartRxThread()
         val btn = findViewById<Button>(R.id.button)
         findViewById<EditText>(R.id.atcommand).setOnEditorActionListener { v, actionId, event ->
             var handled = false
             if(actionId == EditorInfo.IME_ACTION_GO){
                 val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                 imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                 handled = true
             }
             handled
         }
         btn.setOnClickListener {
             if(findViewById<EditText>(R.id.port).text.toString() == ""
                 || findViewById<EditText>(R.id.atcommand).text.toString() == ""){
                 Toast.makeText(this, "포트와 커멘드를 입력하세요",Toast.LENGTH_SHORT).show()
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
        val serialPortFinder: SerialPortFinder = SerialPortFinder()
        val devices: Array<String> = serialPortFinder.allDevices
        val devicesPath: Array<String> = serialPortFinder.allDevicesPath
        for (device in devices) {
            if (device.contains(name, true)) {
                val index = devices.indexOf(device)
                //serialPort = SerialPort(File(devicesPath[index]), SERIAL_BAUDRATE, 0)
                break
            }
        }
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