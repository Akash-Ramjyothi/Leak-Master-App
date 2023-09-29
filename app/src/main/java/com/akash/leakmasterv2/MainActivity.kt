package com.akash.leakmasterv2

import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import okhttp3.*
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var dbRef: DatabaseReference // Creating reference for Database
    val TAG = "DEBUG TAG" // TAG for Log debugging
    private lateinit var gasSensorValuesList: ArrayList<GasSensorValuesModel> // Array list to store fetched data
    private val THRESHOLD_VALUE = 5 // Threshold value to send Alarm
    private var SMS_SENT_INDICATOR =
        false // To indentify if the SMS is sent in a single event or not

    override fun onCreate(savedInstanceState: Bundle?) {
        val firebase: DatabaseReference =
            FirebaseDatabase.getInstance().getReference() // Creating reference for Firebase
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
    private fun saveUserData() {

        // Variables to store data
        val phoneNumber = 8939928002 // Phone number of user for SMS
        val emailId = "akash.ramjyothi@gmail.com"
        val address = "Address 123 abc"

        val userData = UserDataModel(
            phoneNumber, emailId, address
        ) // Making our Model ready to push to Firebase
        Log.d(TAG, "userData: " + userData)

        // Push data to Firebase
        dbRef.child("UserData").setValue(userData)
    }

    // Function to fetch data
    private fun fetchGasSensorData() {
        val mq2valueTextView =
            findViewById(R.id.mq2value_text) as TextView // Referencing TextView in MainActivity

        SMS_SENT_INDICATOR = false // Marking SMS sent as false

        dbRef = FirebaseDatabase.getInstance()
            .getReference("GasSensorValues") // Referencing data inside path GasSensorValues

        // Function to check if any change in Data is there
        dbRef.addValueEventListener(object : ValueEventListener { // Firebase read function
            override fun onDataChange(snapshot: DataSnapshot) {

                gasSensorValuesList.clear()

                if (snapshot.exists()) {
                    val lengthOfSnapshot = snapshot.childrenCount;
                    // Log.d(TAG,"Snapshot Count: " + lengthOfSnapshot) // Length of all Snapshots

                    val requiredChild =
                        snapshot.child((lengthOfSnapshot - 1).toString()) // Accessing the required latest node
                    Log.d(TAG, "Required Child: " + requiredChild)

                    for (i in requiredChild.children) {
                        Log.d(
                            TAG, "Iterating i: ${i}"
                        ) // DataSnapshot { key = -NfKM7EKldV3IejRXObm, value = {mq2Value=6} }
                        Log.d(TAG, "Finding MQ-2 Value: ${i.value}") // {mq2Value=6}

                        val gasSensorValue =
                            i.getValue(GasSensorValuesModel::class.java) // Variable to get actual mq2Value data
                        val mq2SensorValueAny = gasSensorValue?.mq2Value
                        val mq2SensorValueNumber =
                            mq2SensorValueAny as? Number // Safe cast Any to Number
                        Log.d(
                            TAG, "Correct MQ-2 Value: ${mq2SensorValueNumber}"
                        ) // Final correct result

                        mq2valueTextView.setText("MQ-2 Sensor value: ${mq2SensorValueNumber}") // Displaying MQ-2 sensor value in TextView

                        // Send Alert SMS when MQ-2 Sensor value is above threshold eg: >= 5
                        if (mq2SensorValueNumber != null && mq2SensorValueNumber.toInt() >= THRESHOLD_VALUE && SMS_SENT_INDICATOR==false) {

                            Toast.makeText(
                                applicationContext, "ALERT!!! Gas Leak Detected", Toast.LENGTH_SHORT
                            ).show() // Toast message

                            // Call SMS API to send Alert SMS
                            sendSMS("+91 89399 28002", object : Callback {
                                override fun onFailure(call: Call, e: IOException) {
                                    // Handle network error
                                    e.printStackTrace()
                                }

                                override fun onResponse(call: Call, response: Response) {
                                    val responseData = response.body?.string()
                                    // Handle the response data here
                                    System.out.println("SMS Response: " + responseData)
                                }
                            })

                            SMS_SENT_INDICATOR=true
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    // Function to initiate SMS with API
    private fun sendSMS(phoneNumber: String, callback: Callback) {

        val smsApiUrl = "https://sms-api-71h0.onrender.com/api/${phoneNumber}"

        val client = OkHttpClient() // Create object to use OkHttp

        // Performing POST Request
        val request = Request.Builder().url(smsApiUrl)
            .post(RequestBody.create(null, ByteArray(0))) // Empty request body
            .build()

        client.newCall(request).enqueue(callback)
    }

}