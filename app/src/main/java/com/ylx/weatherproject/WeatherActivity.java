package com.ylx.weatherproject;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.ylx.weatherproject.gson.Forecast;
import com.ylx.weatherproject.gson.Weather;
import com.ylx.weatherproject.service.AutoUpdateService;
import com.ylx.weatherproject.util.HttpUtil;
import com.ylx.weatherproject.util.Utility;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class WeatherActivity extends AppCompatActivity {

    private ScrollView weatherLayout;
    private LinearLayout forecastLayout;
    private ImageView bingPicImg;
    private TextView changeCity,titleCity, titleUpdateTime, degreeText, weatherInfoText, aqiText, pm25Text, comfortText, carWashText, sportText;

    public SwipeRefreshLayout swipRefresh;

    public DrawerLayout drawerLayout;

    private SharedPreferences prefs;

    private String main_weatherId = "";

    private static final String WEATHER_URL = "http://guolin.tech/api/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initTheme();
        setContentView(R.layout.activity_weather);
        initView();
    }

    private void initTheme() {
        if(Build.VERSION.SDK_INT >= 21){ //判断sdk api是否大于api 21
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        }
    }


    /**
     * 初始化各控件
     */
    private void initView() {
        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        swipRefresh = (SwipeRefreshLayout) findViewById(R.id.swip_refresh);
        weatherLayout = (ScrollView) findViewById(R.id.weather_layout);
        forecastLayout = (LinearLayout) findViewById(R.id.forecast_layout);
        bingPicImg = (ImageView) findViewById(R.id.bing_pic_img);
        changeCity = (TextView) findViewById(R.id.change_city);
        titleCity = (TextView) findViewById(R.id.title_city);
        titleUpdateTime = (TextView) findViewById(R.id.title_update_time);
        degreeText = (TextView) findViewById(R.id.degree_text);
        weatherInfoText = (TextView) findViewById(R.id.weather_info_text);
        aqiText = (TextView) findViewById(R.id.aqi_text);
        pm25Text = (TextView) findViewById(R.id.pm25_text);
        comfortText = (TextView) findViewById(R.id.comfort_text);
        carWashText = (TextView) findViewById(R.id.car_wash_text);
        sportText = (TextView) findViewById(R.id.sport_text);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String weatherString = prefs.getString("weather",null);

        /**
         * 刷新样式
         */
        swipRefresh.setColorSchemeResources(R.color.colorAccent);

        if(weatherString != null){
            //有缓存时直接解析天气数据
            Weather weather = Utility.handleWeatherResponse(weatherString);
            main_weatherId = weather.basic.weatherId;
            showWeatherInfo(weather);
        } else {
            //无缓存时，去服务器查询天气
            main_weatherId = getIntent().getStringExtra("weather_id");
            weatherLayout.setVisibility(View.VISIBLE);
            requestWeather(main_weatherId);
        }

        initListener();



        String bingPic = prefs.getString("bing_pic",null);
        if(bingPic != null){
            Glide.with(WeatherActivity.this).load(bingPic).into(bingPicImg);
        } else {
            loadBingPic();
        }
    }

    private void initListener() {
        /**
         * 切换城市
         */
        changeCity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });

        /**
         * 刷新
         */
        swipRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                requestWeather(main_weatherId);
            }
        });
    }


    /**
     * 加载必应每日一图
     */
    private void loadBingPic(){
        String requestBingPic = WEATHER_URL + "bing_pic";
        HttpUtil.sendOkHttpRequest(requestBingPic, new Callback() {
            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String picUrl = response.body().string();
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                editor.putString("bing_pic",picUrl);
                editor.apply();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Glide.with(WeatherActivity.this).load(picUrl).into(bingPicImg);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                Toast.makeText(WeatherActivity.this, "必应每日一图加载失败", Toast.LENGTH_SHORT).show();
            }
        });
    }

    /**
     * 根据天气id，查询城市天气信息
     * @param weatherId
     */
    public void requestWeather(String weatherId) {
        String weatherUrl = WEATHER_URL + "weather?cityid=" + weatherId + "&key=ef10ef1548ca42eebec46901ee2c49b8";
        HttpUtil.sendOkHttpRequest(weatherUrl, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                final String responseText = response.body().string();
                final Weather weather = Utility.handleWeatherResponse(responseText);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if(weather != null && "ok".equals(weather.status)){
                            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(WeatherActivity.this).edit();
                            editor.putString("weather", responseText);
                            editor.apply();
                            showWeatherInfo(weather);
                            Intent intent = new Intent(WeatherActivity.this, AutoUpdateService.class);
                            startService(intent);
                        } else {
                            Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        }

                        swipRefresh.setRefreshing(false);
                    }
                });
            }

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(WeatherActivity.this, "获取天气信息失败", Toast.LENGTH_SHORT).show();
                        swipRefresh.setRefreshing(false);
                    }
                });
            }
        });
    }

    /**
     * 处理并展示weather实体类中数据
     * @param weather
     */
    private void showWeatherInfo(Weather weather) {
        String cityName = weather.basic.cityName;
        String updateTime = weather.basic.update.updateTime.split(" ")[1];
        String degree = weather.now.temperature + "°C";
        String weatherInfo = weather.now.more.info;
        titleCity.setText(cityName);
        titleUpdateTime.setText("更新时间：" + updateTime);
        degreeText.setText(degree);
        weatherInfoText.setText(weatherInfo);
        forecastLayout.removeAllViews(); ///////////?????????

        for (Forecast forecast : weather.forecastList){
            View mView = LayoutInflater.from(this).inflate(R.layout.forecast_item, forecastLayout, false);
            TextView dateText = (TextView) mView.findViewById(R.id.date_text);
            TextView infoText = (TextView) mView.findViewById(R.id.info_text);
            TextView maxText = (TextView) mView.findViewById(R.id.max_text);
            TextView minText = (TextView) mView.findViewById(R.id.min_text);
            dateText.setText(forecast.date);
            infoText.setText(forecast.more.info);
            maxText.setText(forecast.temperature.max);
            minText.setText(forecast.temperature.min);
            forecastLayout.addView(mView);
        }

        if(weather.aqi != null){
            aqiText.setText(weather.aqi.city.aqi);
            pm25Text.setText(weather.aqi.city.pm25);
        }

        String comfort = "舒适度：" + weather.suggestion.comfort.info;
        String carWash = "洗车指数：" + weather.suggestion.carWash.info;
        String sport = "运动建议：" + weather.suggestion.sport.info;
        comfortText.setText(comfort);
        carWashText.setText(carWash);
        sportText.setText(sport);
        weatherLayout.setVisibility(View.VISIBLE);
    }
}
