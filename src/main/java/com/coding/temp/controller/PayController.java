package com.coding.temp.controller;

import com.alibaba.fastjson.JSONObject;
import com.coding.temp.utils.JsoupSample;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Zhang Yongwei
 * @version 1.0
 * @date 2019-04-22
 */
@RestController
@RequestMapping("pay")
public class PayController {

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
}
