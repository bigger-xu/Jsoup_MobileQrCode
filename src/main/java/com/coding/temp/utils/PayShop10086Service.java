package com.coding.temp.utils;


import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.POST;

import java.util.Map;

public interface PayShop10086Service {

    @FormUrlEncoded
    @POST("paygw/mobileAndBankPay")
    Observable<Response<ResponseBody>> pay(@Header("Referer") String referer, @FieldMap Map<String, String> data);
}
