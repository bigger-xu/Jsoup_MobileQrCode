package com.coding.temp.utils;


import com.coding.temp.entity.BaseResponse;
import com.coding.temp.entity.CreateOrderResponse;
import io.reactivex.Observable;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.http.*;

import java.util.Map;

public interface Shop10086Service {
    @GET("i/")
    Observable<Response<ResponseBody>> fetchOrder(@Query("f") String f, @Query("mobileNo") String mobile, @Query("amount") int amount);

    default Observable<Response<ResponseBody>> fetchOrder(String mobile, int amount) {
        return this.fetchOrder("rechargecredit", mobile, amount);
    }

    @POST("i/v1/pay/saveorder/{mobile}")
    Observable<Response<BaseResponse<CreateOrderResponse>>> createOrder(@Header("Referer") String referer, @Path("mobile") String mobile, @Query("provinceId") int provinceId, @Body Map<String, Object> data);

    @GET
    @Headers({
            //"Connection: keep-alive",
            //"Upgrade-Insecure-Requests: 1",
            "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36"
            //"DNT: 1",
            //"Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
            //"Accept-Encoding: gzip, deflate, br",
            //"Accept-Language: en,zh-CN;q=0.9,zh;q=0.8",
    })
    Observable<Response<ResponseBody>> getUrl(@Url String url);

    @GET
    @Headers({
            //"Connection: keep-alive",
            //"Upgrade-Insecure-Requests: 1",
            "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/71.0.3578.98 Safari/537.36"
            //"DNT: 1",
            //"Accept: text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8",
            //"Accept-Encoding: gzip, deflate, br",
            //"Accept-Language: en,zh-CN;q=0.9,zh;q=0.8",
    })
    Observable<Response<ResponseBody>> getUrlWithHeaders(@Url String url, @HeaderMap Map<String, String> headers);
}
