package com.app.childrenlocation

import android.content.*
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.EventLog
import android.widget.Toast
import com.karumi.dexter.Dexter
import com.karumi.dexter.DexterBuilder
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import kotlinx.android.synthetic.main.activity_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

class MainActivity : AppCompatActivity(),SharedPreferences.OnSharedPreferenceChangeListener {
    private var mService:MyBackgroundService?=null
    private var mBound=false
    private var mServiceConnection= object : ServiceConnection {
        override fun onServiceDisconnected(p0: ComponentName?) {

        }

        override fun onServiceConnected(p0: ComponentName?, p1: IBinder?) {
            val binder=p1 as MyBackgroundService.LocalBinder
            mService=binder.service
            mBound=true

        }

    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Dexter
            .withActivity(this)
            .withPermissions(Arrays.asList(android.Manifest.permission.ACCESS_COARSE_LOCATION,android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_BACKGROUND_LOCATION,
                android.Manifest.permission.FOREGROUND_SERVICE))
            .withListener(object : MultiplePermissionsListener {


                override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                    request_location_update.setOnClickListener {
                        mService!!.requestLocationUpdates()
                    }
                    remove_location_update.setOnClickListener {
                        mService!!.removeLocationUpdates()
                    }
                    setButtonState(Common.requestingLocationUpdates(this@MainActivity))
                    bindService(Intent(this@MainActivity,MyBackgroundService::class.java),mServiceConnection,Context.BIND_AUTO_CREATE)
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: MutableList<PermissionRequest>?,
                    token: PermissionToken?
                ) {
                }

            }).check()
    }

    override fun onSharedPreferenceChanged(p0: SharedPreferences?, p1: String?) {

        if(p1.equals(Common.KEY_REQUEST_LOCATION_UPDATE))
        {
            setButtonState(p0!!.getBoolean(Common.KEY_REQUEST_LOCATION_UPDATE,false))
        }
    }

    private fun setButtonState(boolean: Boolean) {
        if (boolean)
        {
            remove_location_update.isEnabled=true
            request_location_update.isEnabled=false
        }else
        {
            remove_location_update.isEnabled=false
            request_location_update.isEnabled=true
        }
    }
    @Subscribe(sticky = true,threadMode = ThreadMode.MAIN)
    fun onBackgroundLocationRetrive(event:BackgroundLocation)
    {
        if(event.mLocation!=null)
        {
            Toast.makeText(this,Common.getLocationText(event.mLocation),Toast.LENGTH_SHORT).show()
        }
    }

    override fun onStart() {
        super.onStart()
        PreferenceManager.getDefaultSharedPreferences(this)
            .registerOnSharedPreferenceChangeListener(this)
            EventBus.getDefault().register(this)
    }

    override fun onStop() {
        PreferenceManager.getDefaultSharedPreferences(this)
            .unregisterOnSharedPreferenceChangeListener(this)
        EventBus.getDefault().unregister(this)
        super.onStop()
    }
}
