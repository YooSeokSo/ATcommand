package com.example.at

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.*
import android_serialport_api.SerialPort
import android_serialport_api.SerialPortFinder
import androidx.annotation.RequiresApi
import androidx.preference.PreferenceManager
import com.example.SettingsActivity
import java.io.*
import java.time.LocalDateTime


class MainActivity : AppCompatActivity() {
    var SERIAL_PORT_NANE = "smd11"
    var SERIAL_BAUDRATE = 9600
    var serialPort: SerialPort? = null
    var inputStream: InputStream? = null
    var outputStream: OutputStream? = null
    lateinit var scrollview: ScrollView
    lateinit var serialThread: Thread
    lateinit var strBuilder: StringBuilder
     @RequiresApi(Build.VERSION_CODES.O)
     override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
         val btn = findViewById<Button>(R.id.button)
         val btn2 = findViewById<Button>(R.id.button2)
         val btn3 = findViewById<Button>(R.id.button3)
         val btn4 = findViewById<Button>(R.id.button5)
         val btn5 = findViewById<Button>(R.id.button6)
         val btn6 = findViewById<Button>(R.id.button7)
         scrollview = findViewById(R.id.response)
         strBuilder = StringBuilder()
         val pref = PreferenceManager.getDefaultSharedPreferences(this)

         //루트확인 및 selinux 해제
         try {
             val su = Runtime.getRuntime().exec("su")
             val outputStream = DataOutputStream(su.outputStream)
             outputStream.writeBytes("setenforce 0\n")
             outputStream.flush()
             outputStream.writeBytes("exit\n")
             outputStream.flush()
             try {
                 su.waitFor()
                 if (0 == su.exitValue()) {
                     btn2.setEnabled(true)
                     Toast.makeText(this, "슈퍼유저입니다.", Toast.LENGTH_SHORT).show()
                 }else{
                     Toast.makeText(this, "슈퍼유저 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
                 }
             } catch (e: IOException){
                 Toast.makeText(this,"슈퍼유저 권한이 필요합니다.",Toast.LENGTH_SHORT).show()
                 Log.d("SerialExam", "ex: $e")
             }

         } catch (e: IOException) {
             Toast.makeText(this,"슈퍼유저 권한이 필요합니다.",Toast.LENGTH_SHORT).show()
             Log.d("SerialExam", "ex: $e")
         }


         //spinner 설정
         val serialPortFinder: SerialPortFinder = SerialPortFinder()
         val devicesPath: Array<String> = serialPortFinder.allDevicesPath
         val spinner = findViewById<Spinner>(R.id.spinner2)
         spinner.adapter = ArrayAdapter<String>(this,R.layout.support_simple_spinner_dropdown_item,devicesPath)
         // spinner default값 설정
         for (device in devicesPath) {
             if (device.contains(SERIAL_PORT_NANE, true)) {
                 val index = devicesPath.indexOf(device)
                 spinner.setSelection(index)
                 break
             }
         }
         // spinner 설정시 port 설정
         spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
             override fun onNothingSelected(parent: AdapterView<*>?) {
             }

             override fun onItemSelected(
                 parent: AdapterView<*>?,
                 view: View?,
                 position: Int,
                 id: Long
             ) {
                 SERIAL_PORT_NANE = spinner.selectedItem.toString()
                 Log.d("SerialExam",SERIAL_PORT_NANE)
             }

         }
        // 키보드 엔터입력시 동작
         findViewById<EditText>(R.id.atcommand).setOnEditorActionListener { v, actionId, event ->
             var handled = false
             if(actionId == EditorInfo.IME_ACTION_GO){
                 val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                 imm.hideSoftInputFromWindow(currentFocus?.windowToken, 0)
                 handled = true
             }
             if(findViewById<EditText>(R.id.atcommand).text.toString() == "") {
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
         // send button
         btn.setOnClickListener {
             if(findViewById<EditText>(R.id.atcommand).text.toString() == ""){
                 Toast.makeText(this, "커멘드를 입력하세요",Toast.LENGTH_SHORT).show()
             }else if(findViewById<TextView>(R.id.textView).text.toString() == "No Connect"){
                 Toast.makeText(this, "포트가 연결되어있지 않습니다.",Toast.LENGTH_SHORT).show()
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
         //connect button
         btn2.setOnClickListener {
             if(findViewById<TextView>(R.id.textView).text.toString() != "No Connect") {
                 Toast.makeText(this, "이전 연결을 종료해주세요", Toast.LENGTH_SHORT).show()
             }else{
                 try {
                     SERIAL_BAUDRATE = pref.getString("baudrate","9600")?.toInt()!!
                     OpenSerialPort(SERIAL_PORT_NANE)
                 }catch (e: IOException){
                     e.printStackTrace()
                 }
             }
         }
         //Disconnect button
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
         // go setting
         btn4.setOnClickListener {
             val intent = Intent(this, SettingsActivity::class.java)
             startActivity(intent)
         }
         //clear
         btn5.setOnClickListener {
             strBuilder.clear()
             findViewById<TextView>(R.id.textView2).text = ""
         }
         //save button
         btn6.setOnClickListener {
            var path = pref.getString("directory","/ATLog")
             var filename = LocalDateTime.now().toString() + ".txt"
             if (path != null) {
                 writeTextFile(path, filename, strBuilder.toString())
             }
         }
     }

    // save log
    fun writeTextFile(directory:String, filename:String, content:String) {
        // 앱 기본경로 / files / memo
        val dir2 = File(this.getExternalFilesDir(null)?.path+directory)
        if(!dir2.exists()) dir2.mkdirs()
        val fullpath = dir2.path + "/" + filename
        Log.d("SerialExam", fullpath)
        val writer = FileWriter(fullpath)
        val buffer = BufferedWriter(writer)
        buffer.write(content)
        buffer.close()
        writer.close()
        Toast.makeText(this,fullpath,Toast.LENGTH_SHORT).show()
    }
    // open port
    private fun OpenSerialPort(name: String) {
        val serialPortFinder: SerialPortFinder = SerialPortFinder()
        val devices: Array<String> = serialPortFinder.allDevices
        val devicesPath: Array<String> = serialPortFinder.allDevicesPath
        for (device in devicesPath) {
            if (device.contains(name, true)) {
                val index = devicesPath.indexOf(device)
                serialPort = SerialPort(File(devicesPath[index]), SERIAL_BAUDRATE, 0)
                findViewById<TextView>(R.id.textView).text = device
                break
            }
        }
        if(serialPort == null){
            Toast.makeText(this, "다른 포트를 입력하세요",Toast.LENGTH_SHORT).show()
            Log.d("SerialExam","cant open port")
            return
        }
        serialPort?.let {
            inputStream = it.inputStream
            outputStream = it.outputStream
        }
        StartRxThread()
    }
    //start receive data
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
    // make string
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
    }

    private fun SendData(data: String){
        try {
            outputStream?.write(data.encodeToByteArray())
        }catch (e: IOException){e.printStackTrace()}
    }

    override fun onDestroy() {
        try {
            val su = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(su.outputStream)
            outputStream.writeBytes("setenforce 1\n")
            outputStream.flush()
            outputStream.writeBytes("exit\n")
            outputStream.flush()
            su.waitFor()
        } catch (e: IOException) {
            Log.d("SerialExam", "ex: $e")
        }
        if (serialPort != null){
            serialThread.interrupt()
            serialPort?.close()
        }
        super.onDestroy()
    }
}