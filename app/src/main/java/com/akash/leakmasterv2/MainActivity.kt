package com.akash.leakmasterv2

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Vibrator
import android.provider.MediaStore.Audio.Media
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.airbnb.lottie.LottieAnimationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.yagmurerdogan.toasticlib.Toastic
import okhttp3.*
import okio.IOException
import okhttp3.Call
import okhttp3.Request
import okhttp3.Response
import okhttp3.OkHttpClient
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody

class MainActivity : AppCompatActivity() {

    private lateinit var dbRef: DatabaseReference // Creating reference for Database
    val TAG = "DEBUG TAG" // TAG for Log debugging
    private lateinit var gasSensorValuesList: ArrayList<GasSensorValuesModel> // Array list to store fetched data
    private val THRESHOLD_VALUE = 7 // Threshold value to send Alarm
    private var SMS_SENT_INDICATOR =
        false // To indentify if the SMS is sent in a single event or not
    private lateinit var mediaPlayer: MediaPlayer // Creating object for MediaPlayer to play MP3 sound

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

        val gasStatusTextView =
            findViewById<TextView>(R.id.gasStatus) // Referencing Gas Status textview

        val lottieFileViewLottie =
            findViewById<LottieAnimationView>(R.id.lottieAnimationView) // Referencing Lottie Animation View widget

        // Change color of Status Bar
        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS) // Referencing StatusBar

        SMS_SENT_INDICATOR = false // Marking SMS sent as false

        dbRef = FirebaseDatabase.getInstance()
            .getReference("GasSensorValues") // Referencing data inside path GasSensorValues

        // Function to check if any change in Data is there
        dbRef.addValueEventListener(object : ValueEventListener { // Firebase read function
            override fun onDataChange(snapshot: DataSnapshot) {

                var mq2SensorValueNumberGlobal: Number = -1;

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

                        mq2SensorValueNumberGlobal =
                            mq2SensorValueNumber as Number // Finding correct mq2SensorValue and assigning it to a Global variable

                    }

                    mq2valueTextView.setText("Gas Sensor value: ${mq2SensorValueNumberGlobal} ppm") // Displaying MQ-2 sensor value in TextView

                    uploadDataToThingSpeak(mq2SensorValueNumberGlobal) // Call function to upload data to ThingSpeak

                    // Performing required Checks
                    if (mq2SensorValueNumberGlobal != null && mq2SensorValueNumberGlobal.toInt() <= 0) { // When DB value is 0

                        window.statusBarColor = Color.BLACK // Changing StatusBar color to BLACK
                        parentLayout.setBackgroundColor(Color.WHITE) // Setting background of layout to WHITE

                        gasStatusTextView.setText("Gas is Turned OFF") // When DB value is 0
                        gasStatusTextView.setTextColor(Color.parseColor("#000000")) // Changing TextView color to Black
                        mq2valueTextView.setTextColor(Color.parseColor("#000000")) // Changing TextView color to Black

                        lottieFileViewLottie.setAnimationFromUrl("https://lottie.host/a144eb42-7410-41ec-a912-18e7fddd2088/84OwkSBfoV.json") // Gas OFF URL
                        lottieFileViewLottie.playAnimation() // Enable Animation
                        lottieFileViewLottie.setSpeed(1.0f) // Set Animation Speed

                    } else if (mq2SensorValueNumberGlobal != null && mq2SensorValueNumberGlobal.toInt() < THRESHOLD_VALUE) { // When DB value is below Threshold

                        window.statusBarColor =
                            Color.parseColor("#8AFF8A") // Changing StatusBar color to GREEN
                        parentLayout.setBackgroundColor(Color.parseColor("#8AFF8A")) // Setting background of layout to GREEN

                        gasStatusTextView.setText("No Gas leak Detected") // When DB value is less than Threshold
                        gasStatusTextView.setTextColor(Color.parseColor("#000000")) // Changing TextView color to white
                        mq2valueTextView.setTextColor(Color.parseColor("#000000")) // Changing TextView color to white

                        // lottieFileViewLottie.setAnimationFromUrl("https://lottie.host/59563e3d-b43d-4587-832b-4335dd1d53c1/Rq9pSPSDgc.json") // No Gas Leaking URL
                        lottieFileViewLottie.setAnimationFromUrl("https://lottie.host/0ab8fdc9-b232-474f-8f41-60b9f81261bb/h3JeeqXmui.json") // No Gas Leaking URL
                        lottieFileViewLottie.playAnimation() // Enable Animation
                        lottieFileViewLottie.setSpeed(2.6f) // Set Animation Speed

                    } else if (mq2SensorValueNumberGlobal != null && mq2SensorValueNumberGlobal.toInt() >= THRESHOLD_VALUE) { // Send Alert SMS when MQ-2 Sensor value is above threshold eg: >= 5

                        window.statusBarColor =
                            Color.parseColor("#FF0000") // Changing StatusBar color to RED
                        parentLayout.setBackgroundColor(Color.parseColor("#F4E869")) // Setting background of layout to Yellow

                        gasStatusTextView.setText("Gas Leak Detected!!!") // When DB value is greater than Threshold
                        gasStatusTextView.setTextColor(Color.parseColor("#FF0000")) // Changing TextView color to Red
                        mq2valueTextView.setTextColor(Color.parseColor("#FF0000")) // Changing TextView color to Red

                        // lottieFileViewLottie.setAnimationFromUrl("https://lottie.host/1fbb8d30-4ae9-4c0d-a5f0-fb52151bdd3d/U0EtFSweRl.json") // Gas Leaking URL
                        lottieFileViewLottie.setAnimationFromUrl("https://lottie.host/3ba3d2c8-07fa-4b77-a493-236647192351/RdVih9L78P.json") // Gas Leaking URL
                        lottieFileViewLottie.playAnimation() // Enable Animation
                        lottieFileViewLottie.setSpeed(1.0f) // Set Animation Speed

                        if (SMS_SENT_INDICATOR == false) { // If SMS is not already sent

                            sendAlertNotification() // Send PUSH Notification

                            sendAlertEmail("Akash R", "ar6781@srmist.edu.in") // Send Alert Email

                            Toastic.toastic(
                                context = this@MainActivity,
                                message = "WARNING! Gas Leak Detected",
                                duration = Toastic.LENGTH_LONG,
                                type = Toastic.ERROR,
                                customIcon = R.mipmap.lpg_icon,
                                textColor = Color.RED,
                                isIconAnimated = true
                            ).show() // Custom Toast message

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

                            SMS_SENT_INDICATOR = true // Marking SMS sent as true
                        }

                        // To make mobile Vibrate
                        val vibratorService =
                            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        vibratorService.vibrate(1000)

                        // To play Sound Effect
                        mediaPlayer = MediaPlayer.create(this@MainActivity, R.raw.gas_leak_detected)
                        if (mediaPlayer.isPlaying) { // Pause player if sound is already playing
                            mediaPlayer.pause()
                            mediaPlayer.seekTo(0)
                        }
                        mediaPlayer.start() // Start sound effect
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

        val smsApiUrl = "https://sms-api-71h0.onrender.com/api/${phoneNumber}" // SMS API URL

        val client = OkHttpClient() // Create object to use OkHttp

        // Performing POST Request
        val request = Request.Builder().url(smsApiUrl)
            .post(RequestBody.create(null, ByteArray(0))) // Empty request body
            .build()

        client.newCall(request).enqueue(callback) // Making a new request
    }

    // Function to upload "mq2SensorValueNumberGlobal" to ThingSpeak
    private fun uploadDataToThingSpeak(mq2SensorValue: Number) {
        val apiKey = "PQ00WI8L60NRB5AQ" // API Key for ThingSpeak
        val url =
            "https://api.thingspeak.com/update?api_key=$apiKey&field1=$mq2SensorValue" // GET Request URL to upload data

        val client = OkHttpClient() // Object for OkHTTP

        // Making Requesr body
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        // Performing new Request
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // Handle a successful response here if needed
                    val responseBody = response.body?.string()
                    // responseBody contains the response from the server
                } else {
                    // Handle an unsuccessful response here
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                // Handle network failures here
            }
        })
    }

    // Function to Send WARNING Notification
    private fun sendAlertNotification() {
        // Check if Android OS is above Oreo or not
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "channelId",
                "Leak Master Alert",
                NotificationManager.IMPORTANCE_HIGH
            )
            channel.description = "WARNING! Gas Leak Detected"

            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }

        var builder =
            NotificationCompat.Builder(this, "channelId") // Set Channel ID for notification

        // Set Notification properties
        builder.setSmallIcon(R.mipmap.lpg_icon)
            .setContentTitle("Leak Master Alert")
            .setContentText("WARNING! Gas Leak Detected")
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        with(NotificationManagerCompat.from(this)) {

            // Check for required Permission
            if (ActivityCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.POST_NOTIFICATIONS
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
            notify(1, builder.build()) // Build the Notification
        }
    }

    // Function to send email Alert to user
    private fun sendAlertEmail(recipientName: String, recipientEmail: String) {
        val url = "https://leak-master-email-api.onrender.com/send-email"

        // Create a JSON object with the provided parameters
        val json = """
        {
            "recipientName": "$recipientName",
            "recipientEmail": "$recipientEmail"
        }
    """.trimIndent()

        // Set the JSON media type using the extension function
        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()!!

        // Create an OkHttpClient instance
        val client = OkHttpClient()

        // Create a request body with JSON content
        val body = RequestBody.create(mediaType, json)

        // Create the POST request
        val request = Request.Builder()
            .url(url)
            .post(body)
            .build()

        // Execute the request
        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                val responseBody = response.body?.string()
                // Handle the response here
            }

            override fun onFailure(call: okhttp3.Call, e: java.io.IOException) {
                // Handle network failures here
            }
        })
    }

}