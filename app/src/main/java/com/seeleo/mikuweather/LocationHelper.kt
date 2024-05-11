@file:Suppress("DEPRECATION", "unused")

package com.seeleo.mikuweather

import android.Manifest.permission
import android.app.Activity
import android.content.Context
import android.content.Context.LOCATION_SERVICE
import android.content.Intent
import android.location.*
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationToken
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.OnTokenCanceledListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume

object LocationHelper {
    fun getLocationServiceStatus(context: Context): Boolean {
        return (context.getSystemService(LOCATION_SERVICE) as LocationManager)
            .isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * 打开定位服务设置
     */
    fun openLocationSetting(context: Context): Boolean {
        return try {
            val settingsIntent = Intent()
            settingsIntent.action = Settings.ACTION_LOCATION_SOURCE_SETTINGS
            settingsIntent.addCategory(Intent.CATEGORY_DEFAULT)
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            settingsIntent.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
            context.startActivity(settingsIntent)
            true
        } catch (ex: java.lang.Exception) {
            false
        }
    }

    /**
     * 获取当前定位
     */
    @RequiresPermission(anyOf = [permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION])
    suspend fun getLocation(context: Activity, timeOut: Long = 2000): Location {
        val location = getLocationByFusedLocationProviderClient(context)
        //默认使用FusedLocationProviderClient 如果FusedLocationProviderClient不可用或获取失败，则使用LocationManager进行二次获取
        Log.d("LocationHelper", "getLocation:$location")
        return if (location.latitude == 0.0) {
            getLocationByLocationManager(context, timeOut)
        } else {
            location
        }
    }

    @RequiresPermission(anyOf = [permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION])
    private suspend fun getLocationByLocationManager(
        context: Activity,
        timeOut: Long = 2000
    ): Location {
        Log.d("LocationHelper", "getLocationByLocationManager")
        val locationManager = context.getSystemService(LOCATION_SERVICE) as LocationManager
        //检查LocationManager是否可用
        return if (LocationManagerUtils.checkLocationManagerAvailable(locationManager)) {
            //使用LocationManager获取当前经纬度
            val location = LocationManagerUtils.getCurrentPosition(locationManager, timeOut)
            if (location.latitude == 0.0) {
                LocationManagerUtils.getLastLocation(locationManager)
            } else {
                location
            }
        } else {
            //获取失败，则采用默认经纬度
            LocationManagerUtils.createDefaultLocation()
        }
    }

    object LocationManagerUtils {
        const val TAG = "LocationManagerUtils"

        /**
         * @mLocationManager 传入LocationManager对象
         * @minDistance  位置变化最小距离：当位置距离变化超过此值时，将更新位置信息（单位：米）
         * @timeOut 超时时间，如果超时未返回，则直接使用默认值
         */
        @RequiresPermission(anyOf = [permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION])
        suspend fun getCurrentPosition(
            mLocationManager: LocationManager,
            timeOut: Long = 3000,
        ): Location {
            var locationListener: LocationListener? = null
            return try {
                //超时未返回则直接获取失败，返回默认值
                withTimeout(timeOut) {
                    suspendCancellableCoroutine { continuation ->
                        //获取最佳定位方式，如果获取不到则默认采用网络定位。
                        var bestProvider = mLocationManager.getBestProvider(createCriteria(), true)
                        if (bestProvider.isNullOrEmpty() || bestProvider == "passive") {
                            bestProvider = "network"
                        }
                        Log.d(TAG, "getCurrentPosition:bestProvider:${bestProvider}")
                        locationListener = object : LocationListener {
                            override fun onLocationChanged(location: Location) {
                                Log.d(
                                    TAG,
                                    "getCurrentPosition:onCompete:${location.latitude},${location.longitude}"
                                )
                                if (continuation.isActive) {
                                    continuation.resume(location)
                                    mLocationManager.removeUpdates(this)
                                }
                            }

                            override fun onProviderDisabled(provider: String) {
                            }

                            override fun onProviderEnabled(provider: String) {
                            }
                        }
                        //开始定位
                        mLocationManager.requestLocationUpdates(
                            bestProvider,
                            1000, 0f,
                            locationListener!!
                        )
                    }
                }
            } catch (e: Exception) {
                try {
                    locationListener?.let {
                        mLocationManager.removeUpdates(it)
                    }
                } catch (e: Exception) {
                    Log.d(TAG, "getCurrentPosition:removeUpdate:${e.message}")
                }
                //超时直接返回默认的空对象
                Log.d(TAG, "getCurrentPosition:onError:${e.message}")
                return createDefaultLocation()
            }
        }

        @RequiresPermission(anyOf = [permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION])
        suspend fun repeatLocation(mLocationManager: LocationManager): Location {
            return suspendCancellableCoroutine { continuation ->
                //获取最佳定位方式，如果获取不到则默认采用网络定位。
                var bestProvider = mLocationManager.getBestProvider(createCriteria(), true)
                if (bestProvider.isNullOrEmpty() || bestProvider == "passive") {
                    bestProvider = "network"
                }
                Log.d(TAG, "getCurrentPosition:bestProvider:${bestProvider}")
                val locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        Log.d(
                            TAG,
                            "getCurrentPosition:onCompete:${location.latitude},${location.longitude}"
                        )
                        if (continuation.isActive) {
                            continuation.resume(location)
                        }
                        mLocationManager.removeUpdates(this)
                    }

                    override fun onProviderDisabled(provider: String) {
                    }

                    override fun onProviderEnabled(provider: String) {
                    }
                }
                //开始定位
                mLocationManager.requestLocationUpdates(bestProvider, 1000, 0f, locationListener)
            }
        }

        @RequiresPermission(anyOf = [permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION])
        fun getLastLocation(mLocationManager: LocationManager): Location {
            //获取最佳定位方式，如果获取不到则默认采用网络定位。
            var currentProvider = mLocationManager.getBestProvider(createCriteria(), true)
            if (currentProvider.isNullOrEmpty() || currentProvider == "passive") {
                currentProvider = "network"
            }
            return mLocationManager.getLastKnownLocation(currentProvider) ?: createDefaultLocation()
        }

        //创建定位默认值
        fun createDefaultLocation(): Location {
            val location = Location("network")
            location.longitude = 0.0
            location.latitude = 0.0
            return location
        }

        private fun createCriteria(): Criteria {
            return Criteria().apply {
                accuracy = Criteria.ACCURACY_FINE
                isAltitudeRequired = false
                isBearingRequired = false
                isCostAllowed = true
                powerRequirement = Criteria.POWER_HIGH
                isSpeedRequired = false
            }
        }

        ///定位是否可用
        fun checkLocationManagerAvailable(mLocationManager: LocationManager): Boolean {
            return mLocationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) ||
                    mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        }
    }

    @RequiresPermission(anyOf = [permission.ACCESS_COARSE_LOCATION, permission.ACCESS_FINE_LOCATION])
    private suspend fun getLocationByFusedLocationProviderClient(context: Activity): Location {
        Log.d("LocationHelper", "getLocationByFusedLocationProviderClient")
        //使用FusedLocationProviderClient进行定位
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        return if (FusedLocationProviderUtils.checkFusedLocationProviderAvailable(
                fusedLocationClient
            )
        ) {
            withContext(Dispatchers.IO) {
                //使用FusedLocationProviderClient获取当前经纬度
                val location = FusedLocationProviderUtils.getCurrentPosition(fusedLocationClient)
                if (location.latitude == 0.0) {
                    FusedLocationProviderUtils.getLastLocation(fusedLocationClient)
                } else {
                    location
                }
            }
        } else {
            LocationManagerUtils.createDefaultLocation()
        }
    }

    object FusedLocationProviderUtils {
        private const val TAG = "FusedLocationUtils"

        @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
        suspend fun checkFusedLocationProviderAvailable(fusedLocationClient: FusedLocationProviderClient): Boolean {
            return try {
                withTimeout(1000) {
                    suspendCancellableCoroutine { continuation ->
                        fusedLocationClient.locationAvailability.addOnFailureListener {
                            Log.d(TAG, "locationAvailability:addOnFailureListener:${it.message}")
                            if (continuation.isActive) {
                                continuation.resume(false)
                            }
                        }.addOnSuccessListener {
                            Log.d(
                                TAG,
                                "locationAvailability:addOnSuccessListener:${it.isLocationAvailable}"
                            )
                            if (continuation.isActive) {
                                continuation.resume(it.isLocationAvailable)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                return false
            }
        }

        ///获取最后已知的定位信息
        @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
        suspend fun getLastLocation(fusedLocationClient: FusedLocationProviderClient): Location {
            return suspendCancellableCoroutine { continuation ->
                fusedLocationClient.lastLocation.addOnSuccessListener {
                    if (continuation.isActive) {
                        Log.d(TAG, "current location success:$it")
                        if (it != null) {
                            continuation.resume(it)
                        } else {
                            continuation.resume(createDefaultLocation())
                        }
                    }
                }.addOnFailureListener {
                    continuation.resume(createDefaultLocation())
                }
            }
        }

        /**
         * 获取当前定位，需要申请定位权限
         *
         */
        @RequiresPermission(anyOf = ["android.permission.ACCESS_COARSE_LOCATION", "android.permission.ACCESS_FINE_LOCATION"])
        suspend fun getCurrentPosition(fusedLocationClient: FusedLocationProviderClient): Location {
            return suspendCancellableCoroutine { continuation ->
                fusedLocationClient.getCurrentLocation(createLocationRequest(),
                    object : CancellationToken() {
                        override fun onCanceledRequested(p0: OnTokenCanceledListener): CancellationToken {
                            return CancellationTokenSource().token
                        }

                        override fun isCancellationRequested(): Boolean {
                            return false
                        }
                    }).addOnSuccessListener {
                    if (continuation.isActive) {
                        Log.d(TAG, "current location success:$it")
                        if (it != null) {
                            continuation.resume(it)
                        } else {
                            continuation.resume(createDefaultLocation())
                        }
                    }
                }.addOnFailureListener {
                    Log.d(TAG, "current location fail:$it")
                    if (continuation.isActive) {
                        continuation.resume(createDefaultLocation())
                    }
                }.addOnCanceledListener {
                    Log.d(TAG, "current location cancel:")
                    if (continuation.isActive) {
                        continuation.resume(createDefaultLocation())
                    }
                }
            }
        }

        //创建当前LocationRequest对象
        private fun createLocationRequest(): CurrentLocationRequest {
            return CurrentLocationRequest.Builder()
                .setDurationMillis(1000)
                .setMaxUpdateAgeMillis(5000)
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .build()
        }

        //创建默认值
        private fun createDefaultLocation(): Location {
            val location = Location("network")
            location.longitude = 0.0
            location.latitude = 0.0
            return location
        }
    }
}