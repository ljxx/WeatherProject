package com.ylx.weatherproject.gson;

/**
 * ========================================
 * <p/>
 * 版 权：蓝吉星讯 版权所有 （C） 2017
 * <p/>
 * 作 者：yanglixiang
 * <p/>
 * 版 本：1.0
 * <p/>
 * 创建日期：2017/5/12  下午4:06
 * <p/>
 * 描 述：
 * <p/>
 * 修订历史：
 * <p/>
 * ========================================
 */
public class AQI {
    public AQICity city;

    public class AQICity{
        public String aqi;
        public String pm25;
    }
}
