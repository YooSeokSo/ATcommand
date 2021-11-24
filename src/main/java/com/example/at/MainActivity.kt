package com.example.at

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android_serialport_api.SerialPort
import android_serialport_api.SerialPortFinder
import androidx.preference.PreferenceManager
import com.example.SettingsActivity
import java.io.*


class MainActivity : AppCompatActivity() {
    var SERIAL_PORT_NANE = "/dev/smd11"
    var SERIAL_BAUDRATE = 9600
    var serialPort: SerialPort? = null
    var inputStream: InputStream? = null
    var outputStream: OutputStream? = null
    lateinit var scrollview: ScrollView
    lateinit var serialThread: Thread
    lateinit var strBuilder: StringBuilder
     override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
//         OpenSerialPort(SERIAL_PORT_NANE)
//         StartRxThread()
         val pref = PreferenceManager.getDefaultSharedPreferences(this)
         try {
             val su = Runtime.getRuntime().exec("su")
             val outputStream = DataOutputStream(su.outputStream)
             outputStream.writeBytes("setenforce 0\n")
             outputStream.flush()
             outputStream.writeBytes("exit\n")
             outputStream.flush()
             su.waitFor()
         } catch (e: IOException) {
             Toast.makeText(this,"슈퍼유저 권한이 필요합니다.",Toast.LENGTH_SHORT).show()
             Log.d("SerialExam", "ex: $e")
         }
         val btn = findViewById<Button>(R.id.button)
         val btn2 = findViewById<Button>(R.id.button2)
         val btn3 = findViewById<Button>(R.id.button3)
         val btn4 = findViewById<Button>(R.id.button5)
         scrollview = findViewById(R.id.response)
         strBuilder = StringBuilder()
         findViewById<EditText>(R.id.atcommand).setOnEditorActionListener { v, actionId, event ->
             var handled = false
             if(actionId == EditorInfo.IME_ACTION_GO){
                 val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                 imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                 handled = true
             }
             if(findViewById<EditText>(R.id.port).text.toString() == "") {
                 Toast.makeText(this, "커맨드를 입력해주세요", Toast.LENGTH_SHORT).show()
             }else{
                 try {
                     var data = findViewById<EditText>(R.id.atcommand).text.toString()
                     Log.d("SerialExam","send date: $data")
                     SendData(data+'\r')
                 }catch (e: IOException){
                     e.printStackTrace()
                 }

             }
             handled

         }
         btn.setOnClickListener {
             if(findViewById<EditText>(R.id.port).text.toString() == ""
                 || findViewById<EditText>(R.id.atcommand).text.toString() == ""
                 || findViewById<TextView>(R.id.textView).text.toString() == "No Connect"){
                 Toast.makeText(this, "포트와 커멘드를 입력하세요",Toast.LENGTH_SHORT).show()
             }else{
                 try {
                     var data = findViewById<EditText>(R.id.atcommand).text.toString()
                     Log.d("SerialExam","send date: $data")
                     SendData(data+'\r')
                 } catch (e: IOException){
                     e.printStackTrace()
                 }
             }
         }
         btn2.setOnClickListener {
             if(findViewById<EditText>(R.id.port).text.toString() == "") {
                 Toast.makeText(this, "포트를 입력하세요", Toast.LENGTH_SHORT).show()
             }else{
                 try {
                     SERIAL_PORT_NANE = findViewById<EditText>(R.id.port).text.toString()
                     SERIAL_BAUDRATE = pref.getString("baudrate","9600")?.toInt()!!
                     Log.d("SerialExam", SERIAL_BAUDRATE.toString())
                     OpenSerialPort(SERIAL_PORT_NANE)
                     StartRxThread()
                     findViewById<TextView>(R.id.textView).text = SERIAL_PORT_NANE
                 }catch (e: IOException){
                     e.printStackTrace()
                 }

             }
         }
         btn3.setOnClickListener {
             if(findViewById<TextView>(R.id.textView).text.toString() == "No Connect") {
                 Toast.makeText(this, "포트가 연결되어 있지 않습니다.", Toast.LENGTH_SHORT).show()
             }else{
                 try {
                     serialThread.interrupt()
                     serialPort?.close()
                     findViewById<TextView>(R.id.textView).text = "No Connect"
                 }catch (e: IOException){
                     e.printStackTrace()
                 }

             }
         }
         btn4.setOnClickListener {
             val intent = Intent(this, SettingsActivity::class.java)
             startActivity(intent)
         }
     }

    private fun OpenSerialPort(name: String) {
//        val serialPortFinder: SerialPortFinder = SerialPortFinder()
//        val devices: Array<String> = serialPortFinder.allDevices
//        val devicesPath: Array<String> = serialPortFinder.allDevicesPath
//        for (device in devices) {
//            if (device.contains(name, true)) {
//                val index = devices.indexOf(device)
//                //serialPort = SerialPort(File(devicesPath[index]), SERIAL_BAUDRATE, 0)
//                break
//            }
//        }
        serialPort = SerialPort(File(name),SERIAL_BAUDRATE,0)
        if(serialPort == null){
            Toast.makeText(this, "다른 포트를 입력하세요",Toast.LENGTH_SHORT).show()
            Log.d("SerialExam","cant open port")
            return
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
    @SuppressLint("SetTextI18n")
    private fun OnReceiveData(buffer: ByteArray, size: Int){
        if(size<1)return
        val text = findViewById<TextView>(R.id.textView2)
        for (i in 0 until size){
            strBuilder.append(String.format("%c", buffer[i].toInt()))
            runOnUiThread { text.text = strBuilder}
        }
        strBuilder.append("\n")
        scrollview.post {
            scrollview.fullScroll(ScrollView.FOCUS_DOWN)
        }
        Log.d("SerialExam", "rx:$strBuilder")
    }
    private fun SendData(data: String){
        try {
            outputStream?.write(data.encodeToByteArray())
        }catch (e: IOException){e.printStackTrace()}
    }

    override fun onDestroy() {
        if(serialPort != null) {
            serialThread.interrupt()
            serialPort?.close()
        }
        super.onDestroy()
    }
}