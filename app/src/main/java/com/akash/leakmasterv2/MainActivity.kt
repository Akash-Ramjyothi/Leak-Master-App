package com.akash.leakmasterv2

import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Debug
import android.telephony.SmsManager
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import android.Manifest
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {

    private lateinit var dbRef: DatabaseReference // Creating reference for Database
    val TAG = "DEBUG TAG" // TAG for Log debugging
    private lateinit var gasSensorValuesList: ArrayList<GasSensorValuesModel> // Array list to store fetched data
    private val PERMISSION_REQUEST_CODE = 123 // You can use any code you like

    override fun onCreate(savedInstanceState: Bundle?) {
        val firebase : DatabaseReference = FirebaseDatabase.getInstance().getReference() // Creating reference for Firebase
        // Log.d(TAG, "Firebase Value: " + firebase)

        // dbRef = FirebaseDatabase.getInstance().getReference("Employees") // Initialize Firebase database
        dbRef = FirebaseDatabase.getInstance().getReference() // Initialize Firebase database

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        saveUserData() // Calling function to push data to Firebase

        gasSensorValuesList = arrayListOf<GasSensorValuesModel>()

        fetchGasSensorData() // Calling function to fetch data from Firebase
    }

    // Function to Insert data
    private fun saveUserData(){

        // Variables to store data
        val phoneNumber = 8939928002 // Phone number of user for SMS
        val emailId = "akash.ramjyothi@gmail.com"
        val address = "Address 123 abc"

        val userData = UserDataModel(phoneNumber,emailId,address) // Making our Model ready to push to Firebase
        Log.d(TAG,"userData: " + userData)

        // Push data to Firebase
        dbRef.child("UserData").setValue(userData)
    }

    // Function to fetch data
    private fun fetchGasSensorData(){
        val mq2valueTextView = findViewById(R.id.mq2value_text) as TextView // Referencing TextView in MainActivity

        dbRef = FirebaseDatabase.getInstance().getReference("GasSensorValues") // Referencing data inside path GasSensorValues

        dbRef.addValueEventListener(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                gasSensorValuesList.clear()
                if (snapshot.exists()){
                    val lengthOfSnapshot = snapshot.childrenCount;
                    Log.d(TAG,"Snapshot Count: " + lengthOfSnapshot)

                    val requiredChild = snapshot.child((lengthOfSnapshot-1).toString())
                    Log.d(TAG,"Required Child: " + requiredChild)

                    for (i in requiredChild.children){
                        Log.d(TAG,"Iterating i: ${i}")
                        Log.d(TAG,"Finding MQ-2 Value: ${i.value}")

                        val gasSensorValue = i.getValue(GasSensorValuesModel::class.java) // Variable to get actual mq2Value data
                        val mq2SensorValueAny = gasSensorValue?.mq2Value
                        val mq2SensorValueInt = mq2SensorValueAny as? Number // Safe cast Any to Number
                        Log.d(TAG,"Correct MQ-2 Value: ${mq2SensorValueInt}") // Final correct result

                        mq2valueTextView.setText("MQ-2 Sensor value: ${mq2SensorValueInt}") // Displaying MQ-2 sensor value in TextView

                        if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.SEND_SMS), PERMISSION_REQUEST_CODE)
                        }

                        // Send Alert SMS when MQ-2 Sensor value is above threshold eg: >= 5
                        if(mq2SensorValueInt!=null && mq2SensorValueInt.toInt() >= 5){
                            Toast.makeText(applicationContext, "ALERT!!! Gas Leak Datected", Toast.LENGTH_SHORT).show() // Toast message

                            var smsObject = SmsManager.getDefault() // Create object for SMS Manager
                            smsObject.sendTextMessage("8939928002",null,"ALERT!!! Gas Leak Datected",null,null) // Function to send SMS
                        }
                    }

//                    for (leakValues in  snapshot.children){
//                        // Log.d(TAG,"leakValues: " + snapshot.child("GasSensorValues"))
//                        val GasData = leakValues
//                        Log.d(TAG,"leakValues: " + GasData)
//                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }
}