package com.ylx.weatherproject.service;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;

import com.ylx.weatherproject.gson.Weather;
import com.ylx.weatherproject.util.HttpUtil;
import com.ylx.weatherproject.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

/**
 * ========================================
 * <p/>
 * 版 权：蓝吉星讯 版权所有 （C） 2017
 * <p/>
 * 作 者：yanglixiang
 * <p/>
 * 版 本：1.0
 * <p/>
 * 创建日期：2017/5/12  下午10:46
 * <p/>
 * 描 述：
 * <p/>
 * 修订历史：
 * <p/>
 * ========================================
 */
public class AutoUpdateService extends Service {

    private static final String WEATHER_URL = "http://guolin.tech/api/";

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        /**
         * 更新天气
         */
        updateWeather();
        /**
         * 更新必应图片
         */
        updateBingPic();

        AlarmManager manager = (AlarmManager) getSystemService(ALARM_SERVICE);
        int anHour = 8 * 60 * 60 * 1000; //这是8小时的毫秒数
        long triggerAtTime = SystemClock.elapsedRealtime() + anHour;
        Intent ii = new Intent(this, AutoUpdateService.class);
        PendingIntent pi = PendingIntent.getService(this, 0, ii, 0);
        manager.cancel(pi);
        manager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtTime, pi);

        return super.onStartCommand(intent, flags, startId);
    }

    /**
     * 更新必应每日一图片
     */
    private void updateBingPic() {
        String requestBingUrl = WEATHER_URL + "bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String bingImgUrl = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                editor.putString("bing_pic", null);
                editor.apply();
            }

            @Override
            public void onFailure(Call call, IOException e) {

            }
        });
    }

    /**
     * 更新天气信息
     */
    private void updateWeather() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather", null);
        if(weatherString != null){ //读取缓存是为了得到天气id
            //有缓存时，直接解析缓存数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            String weatherId = weather.basic.weatherId;
            String weatherUrl = WEATHER_URL + "weather?cityid=" + weatherId + "&key=ef10ef1548ca42eebec46901ee2c49b8";

            HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    String responseText = response.body().string();
                    Weather weather = Utility.handleWeatherResponse(responseText);
                    if(weather != null && "ok".equals(weather.status)){
                        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(AutoUpdateService.this).edit();
                        editor.putString("weather", responseText);
                        editor.apply();
                    }

                }

                @Override
                public void onFailure(Call call, IOException e) {

                }
            });


        }
    }
}
