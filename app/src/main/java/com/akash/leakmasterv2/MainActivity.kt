package com.akash.leakmasterv2

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.lottie.LottieAnimationView
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
    //private var ANIMATION_COMPLETION_STATUS = true

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

        val parentLayout =
            findViewById<LinearLayout>(R.id.parentLayout) // Referencing parent Linear layout

        val gasStatusTextView = findViewById<TextView>(R.id.gasStatus) // Referencing Gas Status textview

        val lottieFileViewLottie = findViewById<LottieAnimationView>(R.id.lottieAnimationView)

        // Change color of Status Bar
        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS) // Referencing StatusBar

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

                        mq2valueTextView.setText("Gas Sensor value: ${mq2SensorValueNumber} ppm") // Displaying MQ-2 sensor value in TextView

                        if (mq2SensorValueNumber != null && mq2SensorValueNumber.toInt() == 0) { // When DB value is 0

                            window.statusBarColor = Color.BLACK // Changing StatusBar color to BLACK
                            parentLayout.setBackgroundColor(Color.WHITE) // Setting background of layout to WHITE

                            gasStatusTextView.setText("Gas is Turned OFF") // When DB value is 0
                            gasStatusTextView.setTextColor(Color.parseColor("#000000")) // Changing TextView color to Black
                            mq2valueTextView.setTextColor(Color.parseColor("#000000")) // Changing TextView color to Black

                            lottieFileViewLottie.setAnimationFromUrl("https://lottie.host/3324c7b3-f05d-4252-9161-78150f2bf7b3/DHNtQUkLF9.json") // Gas OFF URL
                            lottieFileViewLottie.playAnimation() // Enable Animation


                        } else if (mq2SensorValueNumber != null && mq2SensorValueNumber.toInt() < THRESHOLD_VALUE) { // When DB value is below Threshold

                            window.statusBarColor = Color.parseColor("#8AFF8A") // Changing StatusBar color to GREEN
                            parentLayout.setBackgroundColor(Color.parseColor("#8AFF8A")) // Setting background of layout to GREEN

                            gasStatusTextView.setText("No Gas leak Detected") // When DB value is less than Threshold
                            gasStatusTextView.setTextColor(Color.parseColor("#000000")) // Changing TextView color to white
                            mq2valueTextView.setTextColor(Color.parseColor("#000000")) // Changing TextView color to white

                            lottieFileViewLottie.setAnimationFromUrl("https://bafkreih4cmup5ldmept3jr7yqf2yofqhkzo3233octj4hthtxxr2tht5rq.ipfs.nftstorage.link/") // No Gas Leaking URL
                            lottieFileViewLottie.playAnimation() // Enable Animation


                        } else if (mq2SensorValueNumber != null && mq2SensorValueNumber.toInt() >= THRESHOLD_VALUE) { // Send Alert SMS when MQ-2 Sensor value is above threshold eg: >= 5

                            window.statusBarColor = Color.parseColor("#ffca30") // Changing StatusBar color to RED
                            parentLayout.setBackgroundColor(Color.parseColor("#ffca30")) // Setting background of layout to RED

                            gasStatusTextView.setText("Gas Leak Detected!!!") // When DB value is greater than Threshold
                            gasStatusTextView.setTextColor(Color.parseColor("#ffffff")) // Changing TextView color to white
                            mq2valueTextView.setTextColor(Color.parseColor("#ffffff")) // Changing TextView color to white

                            lottieFileViewLottie.setAnimationFromUrl("https://lottie.host/1fbb8d30-4ae9-4c0d-a5f0-fb52151bdd3d/U0EtFSweRl.json") // Gas Leaking URL
                            lottieFileViewLottie.playAnimation() // Enable Animation

                            // Add an animation listener to perform actions when the animation ends
                            lottieFileViewLottie.addAnimatorListener(object : AnimatorListenerAdapter() {
                                override fun onAnimationEnd(animation: Animator) {
                                    // Animation has ended, you can perform actions here
                                    // For example, show a toast message or start another activity
                                    // Do nothing for now as per your request
                                }
                            })

                            if (SMS_SENT_INDICATOR == false) { // If SMS is not already sent

//                                Toast.makeText(
//                                    applicationContext,
//                                    "ALERT!!! Gas Leak Detected",
//                                    Toast.LENGTH_SHORT
//                                ).show() // Toast message

                                // Call SMS API to send Alert SMS
//                                sendSMS("+91 89399 28002", object : Callback {
//                                    override fun onFailure(call: Call, e: IOException) {
//                                        // Handle network error
//                                        e.printStackTrace()
//                                    }
//
//                                    override fun onResponse(call: Call, response: Response) {
//                                        val responseData = response.body?.string()
//                                        // Handle the response data here
//                                        System.out.println("SMS Response: " + responseData)
//                                    }
//                                })

                                SMS_SENT_INDICATOR = true // Marking SMS sent as true
                            }

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