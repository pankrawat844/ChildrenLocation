package com.app.childrenlocation

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.location.Location
import android.os.*
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import org.greenrobot.eventbus.EventBus


class MyBackgroundService : Service() {

    companion object {
        private val CHANNEL_ID = "channel_id"
        private val EXTRA_STARTED_FROM_NOTIFICATION =
            "com.app.childrenlocation. started from notification"
        private val UPDATE_INTERVAL_IN_MILL: Long = 10000
        private val FASTEST_UPDATE_INTERVAL_IN_MILL = UPDATE_INTERVAL_IN_MILL / 2
        private val NOTIFICATION_ID = 1234

    }

    private val mBinder=LocalBinder()
    inner class LocalBinder:Binder() {
        internal  val service:MyBackgroundService
        get() = this@MyBackgroundService
    }
    private var mChangingConfirgation=false
    private var mNofiticationManager:NotificationManager?=null
    private var locationRequest:LocationRequest?=null
    private var  fusedLocationProviderClient:FusedLocationProviderClient?=null
    private var locationCallback:LocationCallback?=null
    private var mserviceHandler:Handler?=null
    private var mLocation:Location?=null
    private  var firebaseDatabase:FirebaseDatabase?=null
    private var databaseReference:DatabaseReference?=null
    private val notification:Notification
    get() {
        val intent=Intent(this,MyBackgroundService::class.java)
        val text= com.app.childrenlocation.Common.getLocationText(mLocation)
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION,true)
        val servicePendingIntent=PendingIntent.getService(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT)
        val activityPendingIntent=PendingIntent.getActivity(this,0,intent,PendingIntent.FLAG_UPDATE_CURRENT)
        val builder=NotificationCompat.Builder(this)
            .addAction(R.drawable.ic_baseline_launch_24,"Launch",activityPendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel,"Cancel",servicePendingIntent)
            .setContentText(text)
            .setContentTitle(com.app.childrenlocation.Common.getLocationTitle(this))
            .setOngoing(true)
            .setPriority(Notification.PRIORITY_HIGH)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setTicker(text)
            .setWhen(System.currentTimeMillis())
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
            builder.setChannelId(CHANNEL_ID)
        return builder.build()
    }
    override fun onBind(intent: Intent): IBinder? {
       stopForeground(true)
        mChangingConfirgation=false
        return mBinder
    }

    override fun onRebind(intent: Intent?) {
        stopForeground(true)
        mChangingConfirgation=false
        super.onRebind(intent)

    }

    override fun onUnbind(intent: Intent?): Boolean {
        if(!mChangingConfirgation && com.app.childrenlocation.Common.requestingLocationUpdates(this))
        startForeground(NOTIFICATION_ID,notification)
            return super.onUnbind(intent)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val startedFromNotification=intent?.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,false)
        if(startedFromNotification!!)
        {
            removeLocationUpdates()
            stopSelf()
        }
        return Service.START_NOT_STICKY
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        mChangingConfirgation=true
    }

     fun removeLocationUpdates() {
        try {
            fusedLocationProviderClient?.removeLocationUpdates(locationCallback!!)
            com.app.childrenlocation.Common.setReqestingLocationUpdates(this,false)

        }catch (e:Exception)
        {
            com.app.childrenlocation.Common.setReqestingLocationUpdates(this,false)
            Log.e("error", "Lost Location")
        }
    }

    override fun onCreate() {
        firebaseDatabase= FirebaseDatabase.getInstance()
        databaseReference=firebaseDatabase!!.getReference("location")
        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this)
        locationCallback=object :LocationCallback(){
            override fun onLocationResult(p0: LocationResult?) {
                super.onLocationResult(p0)
                onNewLocation(p0?.lastLocation)
            }
        }
        createLocationRequest()
        getLastLocation()
        val handlerThread=HandlerThread("Background")
        handlerThread.start()
        mserviceHandler= Handler(handlerThread.looper)
        mNofiticationManager=getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
        {
            val name=packageName
            val mChannel=NotificationChannel(CHANNEL_ID,name,NotificationManager.IMPORTANCE_DEFAULT)
            mNofiticationManager!!.createNotificationChannel(mChannel)

        }
    }

    private fun getLastLocation() {
        try {
            fusedLocationProviderClient!!.lastLocation.addOnCompleteListener {
                task: Task<Location> ->
                if(task.isSuccessful && task.result!=null)
                    mLocation=task.result
                    else
                    Log.e("Error","Failed to Get Location")
            }
        }catch (e:Exception)
        {
            Log.e("Error",e.message)

        }
    }

    private fun createLocationRequest() {
        locationRequest= LocationRequest()
        locationRequest!!.interval= UPDATE_INTERVAL_IN_MILL
        locationRequest!!.fastestInterval= FASTEST_UPDATE_INTERVAL_IN_MILL
        locationRequest!!.priority=LocationRequest.PRIORITY_HIGH_ACCURACY

    }

    private fun onNewLocation(lastLocation: Location?) {
        val usersRef = databaseReference!!.child("users")
        val map:HashMap<String,com.app.childrenlocation.model.Location>?=HashMap()
        map!!.put(Build.MANUFACTURER + " " + Build.MODEL + " " + Build.VERSION.RELEASE,com.app.childrenlocation.model.Location(
            Build.MANUFACTURER + " " + Build.MODEL + " " + Build.VERSION.RELEASE,lastLocation!!.latitude,lastLocation!!.longitude))
        databaseReference!!.setValue(map)
        mLocation=lastLocation!!

        EventBus.getDefault().postSticky(BackgroundLocation(mLocation!!))
        if(serviceIsRunningInForeground(this))
            mNofiticationManager!!.notify(NOTIFICATION_ID,notification)
    }

    private fun serviceIsRunningInForeground(context: Context): Boolean {
        val manager=context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for(service in manager.getRunningServices(Int.MAX_VALUE))
        {
            if(javaClass.name.equals(service.service.className))
                if(service.foreground)
                    return true
        }
        return false
    }

    override fun onDestroy() {
        mserviceHandler!!.removeCallbacksAndMessages(null )
        super.onDestroy()
    }

    fun requestLocationUpdates()
    {
        Common.setReqestingLocationUpdates(this,true)
        startService(Intent(applicationContext,MyBackgroundService::class.java))
        try {
            fusedLocationProviderClient!!.requestLocationUpdates(locationRequest,locationCallback!!,
                Looper.myLooper())
        }catch (ex:SecurityException)
        {
            Common.setReqestingLocationUpdates(this,false)
            Log.e("error","Lost location"+ex.message)
        }
    }
}
