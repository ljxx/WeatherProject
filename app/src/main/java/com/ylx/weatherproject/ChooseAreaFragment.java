package com.ylx.weatherproject;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ylx.weatherproject.db.City;
import com.ylx.weatherproject.db.County;
import com.ylx.weatherproject.db.Province;
import com.ylx.weatherproject.util.HttpUtil;
import com.ylx.weatherproject.util.Utility;

import org.litepal.crud.DataSupport;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
 * 创建日期：2017/5/12  下午2:29
 * <p/>
 * 描 述：
 * <p/>
 * 修订历史：
 * <p/>
 * ========================================
 */
public class ChooseAreaFragment extends Fragment {

    private static final int LEVEL_PROVICE = 0;
    private static final int LEVEL_CITY = 1;
    private static final int LEVEL_COUNTY = 2;

    private static final String TYPE_PROVINCE = "province";
    private static final String TYPE_CITY = "city";
    private static final String TYPE_COUNTY = "county";

    private static final String ADDRESS_URL = "http://guolin.tech/api/china/";

    private TextView titleText;
    private Button backButton;
    private ListView mListView;

    private List<String> dataList = new ArrayList<String>();
    private ArrayAdapter<String> mAdapter;

    private ProgressDialog progressDialog;

    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 市列表
     */
    private List<City> cityList;

    /**
     * 县列表
     */
    private List<County> countyList;

    /**
     * 选中的省份
     */
    private Province selectedProvince;

    /**
     * 选中的市级
     */
    private City selectedCity;

    /**
     * 选中的县级
     */
    private County selectedCounty;

    /**
     * 当前选中的级别
     */
    private int currentLevel;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View mView = inflater.inflate(R.layout.choose_area,container,false);
        titleText = (TextView) mView.findViewById(R.id.title_text);
        backButton = (Button) mView.findViewById(R.id.back_button);
        mListView = (ListView) mView.findViewById(R.id.list_view);
        mAdapter = new ArrayAdapter<String>(getContext(), android.R.layout.simple_list_item_1, dataList);
        mListView.setAdapter(mAdapter);

        return mView;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        /**
         * 条目点击事件
         */
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(currentLevel == LEVEL_PROVICE){
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if(currentLevel == LEVEL_CITY){
                    selectedCity = cityList.get(position);
                    queryCounties();
                } else if(currentLevel == LEVEL_COUNTY){
                    String weatherId = countyList.get(position).getWeatherId();
                    Intent intent = new Intent(getActivity(), WeatherActivity.class);
                    intent.putExtra("weather_id", weatherId);
                    startActivity(intent);
                    getActivity().finish();
                }
            }
        });

        /**
         * 返回
         */
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(currentLevel == LEVEL_CITY){
                    queryProvinces();
                } else if(currentLevel == LEVEL_COUNTY){
                    queryCities();
                }
            }
        });

        queryProvinces();
    }

    /**
     * 查询全国所有的省，优先从数据库查，如果没有查询到再从服务器查
     */
    private void queryProvinces(){
        titleText.setText("中国");
        backButton.setVisibility(View.GONE);

        provinceList = DataSupport.findAll(Province.class);
        if(provinceList.size() > 0){
            dataList.clear();
            for (Province province : provinceList){
                dataList.add(province.getProvinceName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_PROVICE;
        } else {
            String address = ADDRESS_URL;
            queryFromServer(address, TYPE_PROVINCE);
        }
    }

    /**
     * 查询选中省内所有的市，优先从数据库中查，如果没有查到数据，再到服务器查
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);

        cityList = DataSupport.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if(cityList.size() > 0){
            dataList.clear();
            for(City city : cityList){
                dataList.add(city.getCityName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = ADDRESS_URL + provinceCode;
            queryFromServer(address, TYPE_CITY);
        }
    }

    /**
     * 查询选中市内所有的县，优先从数据库中查，如果没有查到数据，再到服务器查
     */
    private void queryCounties() {
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);

        countyList = DataSupport.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if(countyList.size() > 0){
            dataList.clear();
            for(County county : countyList){
                dataList.add(county.getCountryName());
            }
            mAdapter.notifyDataSetChanged();
            mListView.setSelection(0);
            currentLevel = LEVEL_COUNTY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            int cityCode = selectedCity.getCityCode();

            String address = ADDRESS_URL + provinceCode + "/" + cityCode;
            queryFromServer(address, TYPE_COUNTY);
        }
    }

    /**
     * 根据传入的地址和类型从服务器查询省市县的数据
     */
    private void queryFromServer(String address, final String type){
        //显示加载框
        showProgressDialog();

        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String requestText = response.body().string();
                boolean result = false;
                if(TYPE_PROVINCE.equals(type)){ //解析省级的数据
                    result = Utility.handleProvinceResponse(requestText);
                } else if(TYPE_CITY.equals(type)){ //解析市级的数据
                    result = Utility.handlerCityResponse(requestText, selectedProvince.getId());
                } else if(TYPE_COUNTY.equals(type)){ //解析县级的数据
                    result = Utility.handlerCountyResponse(requestText, selectedCity.getId());
                }

                if(result){
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            //关闭加载框
                            closeProgressDialog();

                            if(TYPE_PROVINCE.equals(type)){
                                queryProvinces();
                            } else if(TYPE_CITY.equals(type)){
                                queryCities();
                            } else if(TYPE_COUNTY.equals(type)){
                                queryCounties();
                            }
                        }
                    });
                }

            }

            @Override
            public void onFailure(Call call, IOException e) {
                //通过runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(),"加载失败",Toast.LENGTH_SHORT).show();
                    }
                });

            }
        });

    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if(progressDialog == null){
            progressDialog = new ProgressDialog(getContext());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭对话框
     */
    protected void closeProgressDialog(){
        if(progressDialog != null){
            progressDialog.dismiss();
        }
    }
}
