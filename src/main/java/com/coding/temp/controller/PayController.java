package com.coding.temp.controller;


import com.jayway.jsonpath.JsonPath;
import com.alibaba.fastjson.JSONObject;
import com.coding.temp.utils.JsoupSample;
import io.reactivex.Observable;
import okhttp3.Headers;
import okhttp3.OkHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author Zhang Yongwei
 * @version 1.0
 * @date 2019-04-22
 */
@RestController
@RequestMapping("pay")
public class PayController {

    /**
     * 移动充值获取二维码
     * @param mobile
     * @param amount
     * @return
     */
    @RequestMapping(value = "ydCode",method = RequestMethod.POST)
    public JSONObject ydCode(String mobile,Integer amount){
        JSONObject result = new JSONObject();
        try{
            String url = JsoupSample.getQrCode(mobile,amount);
            result.put("code","0000");
            result.put("msg","success");
            result.put("data",url);
        }catch (Exception e){
            result.put("code","9999");
            result.put("msg","error");
        }
        return result;
    }

    /**
     * 电信获取验证码
     * @return
     */
    @RequestMapping(value = "/validate-code", method = RequestMethod.GET)
    public Observable<Map<String, String>> validateCode() {

        OkHttpClient.Builder builder = new OkHttpClient.Builder();
        builder.followRedirects(false);

        String timeStamp = String.valueOf(System.currentTimeMillis());
        Map<String, String> cookies = getInitCookies();
        StringBuilder reqCookieValues = new StringBuilder(cookies.entrySet().stream().map(kv -> kv.getKey() + "=" + kv.getValue()).collect(Collectors.joining("; ")));
        return TopUpService.captcha(timeStamp, reqCookieValues.toString()).map(response -> {
            //获取验证码
            String jSessionCookie = response.headers().values("Set-Cookie").get(0).split(";")[0];
            reqCookieValues.append("; " + jSessionCookie);

            byte[] data = response.body().bytes();
            return new HashMap<String, String>() {{
                put("image", "data:image/png;base64," + Base64.getEncoder().encodeToString(data));
                put("cookies", reqCookieValues.toString());
            }};
        });
    }

    /**
     * 电信获取支付宝二维码
     * @param validateCode
     * @param cookies
     * @param mobile
     * @return
     */
    @RequestMapping(value = "/pay-order", method = RequestMethod.GET)
    public Observable<String> payOrder(String validateCode, String cookies,String mobile,Integer amount) {
        CommonRequestFactory.CommonRequest req = CommonRequestFactory.create(null);
        final Map<String, String> alipayCookies = new HashMap<>();

        return TopUpService.doTrade(mobile, validateCode, amount, cookies).switchMap(response -> {
            //获取订单号
            String data = new String(response.body().bytes(), StandardCharsets.UTF_8);
            System.out.println("提交订单响应: " + data);
            String orderId = JsonPath.read(data, "$.dataObject.orderId");
            return TopUpService.doOrder(orderId, cookies);
        }).switchMap(response -> {
            String html = new String(response.body().bytes(), StandardCharsets.UTF_8);
            Document doc = Jsoup.parse(html);
            Element reqParams = doc.selectFirst("input[name='request_params']");
            String values = reqParams.val();


            return req.postForm(
                    "http://paygo.189.cn:9778/189pay/service",
                    new HashMap<String, String>() {{
                        put("Cookie", cookies);
                    }},
                    new HashMap<String, Object>() {{
                        put("request_params", values);
                    }});

        }).switchMap(response -> {
            String html = new String(response.body().bytes(), StandardCharsets.UTF_8);
            Document doc = Jsoup.parse(html);
            Map<String, Object> body = doc.select("input[type=hidden]").stream().collect(Collectors.toMap(ele -> ele.attr("name"), ele -> (Object) ele.val()));

            return req.postForm("https://mapi.alipay.com/gateway.do?_input_charset=utf-8", new HashMap<>(), body);
        }).switchMap(response -> {
            //mapi.alipay.com -> Response
            //Request -> unitradeadapter.alipay.com

            Map<String, String> reqCookies = generateAlipayCookie(response.headers());
            reqCookies.keySet().forEach(key -> alipayCookies.put(key, reqCookies.get(key)));
            Map<String, String> headers = new HashMap<>();
            String cookieValue = String.format("zone=%s; ALIPAYJSESSIONID=%s; ctoken=%s", alipayCookies.get("zone"), alipayCookies.get("ALIPAYJSESSIONID"), alipayCookies.get("ctoken"));
            headers.put("Cookie", cookieValue);

            String location = response.headers().get("Location");
            return req.get(location, headers);
        }).switchMap(response -> {
            //unitradeadapter.alipay.com -> Response
            //Request -> excashier.alipay.com

            Map<String, String> reqCookies = generateAlipayCookie(response.headers());
            if (reqCookies.containsKey("ALIPAYJSESSIONID")) {
                alipayCookies.put("ALIPAYJSESSIONID", reqCookies.get("ALIPAYJSESSIONID"));
            }

            Map<String, String> headers = new HashMap<>();
            String cookieValue = String.format("zone=%s; ALIPAYJSESSIONID=%s; ctoken=%s", alipayCookies.get("zone"), alipayCookies.get("ALIPAYJSESSIONID"), alipayCookies.get("ctoken"));
            headers.put("Cookie", cookieValue);

            String location = response.headers().get("Location");
            return req.get(location, headers);
        }).map(response -> {
            //excashier.alipay.com -> Response

            String data = response.body().string();
            Document doc = Jsoup.parse(data);
            String qrImageUrl = doc.selectFirst("#J_qrImgUrl").attr("value");
            return qrImageUrl;
        });
    }


    private static Map<String, String> getInitCookies() {
        Supplier<String> get16String = () -> UUID.randomUUID().toString().replaceAll("-", "").substring(0, 16);

        Map<String, String> cookies = new HashMap<>();
        cookies.put("svid", get16String.get());
        cookies.put("s_fid", get16String.get() + "-" + get16String.get());
        cookies.put("loginStatus", "non-logined");
        cookies.put("lvid", UUID.randomUUID().toString().replaceAll("-", ""));
        cookies.put("nvid", "1");
        cookies.put("trkId", UUID.randomUUID().toString());
        cookies.put("s_cc", "true");
        cookies.put("trkHmClickCoords", "587%2C1060%2C2108");
        return cookies;
    }

    public static Map<String, String> generateAlipayCookie(Headers headers) {

        return headers.values("Set-Cookie")
                .stream()
                .map(cookie -> cookie.split(";")[0].split("="))
                .filter(kv -> Arrays.asList("zone", "ALIPAYJSESSIONID", "ctoken").indexOf(kv[0]) != -1)
                .collect(Collectors.toMap(kv -> kv[0], kv -> kv[1]));

    }
}
