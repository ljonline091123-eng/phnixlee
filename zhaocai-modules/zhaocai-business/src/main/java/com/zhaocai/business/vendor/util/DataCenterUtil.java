package com.zhaocai.business.vendor.util;

import cn.hutool.core.util.IdUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import com.zhaocai.business.manager.template.config.UnderlingPlatformConfig;
import com.zhaocai.business.vendor.config.DataMiddlePlatformConfig;
import com.zhaocai.business.vendor.domain.TInterfaceLog;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.service.ITInterfaceLogService;
import com.zhaocai.common.core.utils.SpringUtils;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Date;

/**
 * 中台对接工具类
 *
 * @author hyt
 * @date 2023/7/20 9:14
 */
@Component
public class DataCenterUtil {

    @Autowired
    private DataMiddlePlatformConfig dataMiddlePlatformConfig;

    @Autowired
    private UnderlingPlatformConfig underlingPlatformConfig;

    /**
     * 超时时间
     */
    private static final Integer TIME_OUT = 3000000;

    public String sendFileDataCenter(String files, String TYDTC_API_KEY, String type, String createBy, Long jobId) {
        boolean flag = false;
        String result = null;
        Long start = System.currentTimeMillis();
        //加密数据格式tyStamp+sendData+tydtcAppSecret md5加密
        String tySign = DigestUtils.md5Hex(start + files + this.dataMiddlePlatformConfig.getTydtcAppSecret());
        //地址
        String url = underlingPlatformConfig.getBaseUrl() + "/" + TYDTC_API_KEY + "?tyStamp=" + start + "&tySign=" + tySign;
        String receive = "";
        Long end = System.currentTimeMillis();
        try {
            File file = new File(files);
            //获取token
            String token = getToken();
            if (StringUtils.isNotEmpty(token)) {
                HttpResponse response = HttpRequest.post(url)
                        .header("TYDTC_APP_TOKEN", token)
                        .header("Content-Type", "multipart/form-data")
                        .form("file", file).timeout(20000).execute();
                end = System.currentTimeMillis();
                if (response != null && response.isOk()) {
                    receive = response.body();
                    if (StringUtils.isNotEmpty(receive)) {
                        JSONObject object = JSONObject.parseObject(receive);
                        if (object != null && object.containsKey("code") && object.getInteger("code") == 200) {
                            //成功的
                            flag = true;
                            result = receive;
                        }
                    }
                } else {
                    receive = String.valueOf(response);
                }
            } else {
                receive = "获取token失败!";
                flag = false;
            }
        } catch (Exception e) {
            receive = e.getMessage();
            flag = false;
        }

        /*try {
            //保存访问的日志
            saveLog(start, url, files, receive, end, type, flag, createBy, jobId);
        } catch (Exception e) {
            e.printStackTrace();
        }*/
        if (receive.contains("Read timed out")) {
            result = "-10000";
        }
        return result;
    }


    private JSONObject sendToDataCenter(JSONObject dataObject, String TYDTC_API_KEY ,String logType , String createBy) {
        boolean flag = false;
        JSONObject data = new JSONObject();
        Long start = System.currentTimeMillis();
        //发送的数据
        String sendData = dataObject.toJSONString();

        //加密数据格式tyStamp+sendData+tydtcAppSecret md5加密
        String tySign = DigestUtils.md5Hex(start + sendData + this.dataMiddlePlatformConfig.getTydtcAppSecret());

        //地址
        String url = dataMiddlePlatformConfig.getDataUrl() + "/" + TYDTC_API_KEY + "?tyStamp=" + start + "&tySign=" + tySign;
        String receive = "";
        Long end = System.currentTimeMillis();
        try {
            //获取token
            String token = getToken();
            if (StringUtils.isNotEmpty(token)) {
                HttpResponse response = HttpRequest.post(url)
                        .header("TYDTC_APP_TOKEN", token)
                        .header("Content-Type", "application/json;charset=UTF-8")
                        .body(sendData)
                        .timeout(TIME_OUT)
                        .execute();
                end = System.currentTimeMillis();
                if (response != null && response.isOk()) {
                    receive = response.body();
                    if (StringUtils.isNotEmpty(receive)) {
                        JSONObject object = JSONObject.parseObject(receive);
                        if (object != null && object.containsKey("code") && object.getInteger("code") == 200) {
                            //成功的
                            flag = true;
                            data = object;
                        } else {
                            flag = false;
                            data = object;
                        }
                    }
                } else {
                    receive = String.valueOf(response);
                }
            } else {
                receive = "获取token失败!";
                flag = false;
            }
        } catch (Exception e) {
            receive = e.getMessage();
            flag = false;
        }

        try {
            //保存访问的日志
            saveLog(start, url, sendData, receive, end, logType, flag, createBy , TYDTC_API_KEY);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (receive.contains("Read timed out")) {
            data = new JSONObject(Integer.parseInt("-10000"));
        }

        return data;
    }


    private void saveLog(Long start, String url, String sendData, String receive, Long end, String type, boolean flag, String createBy , String apiKey) {
        TInterfaceLog log = new TInterfaceLog();
        log.setId(IdUtil.getSnowflake(1, 1).nextId());
        log.setSendTime(new Date(start));
        log.setSendData(sendData);
        log.setSendUrl(url);
        log.setReceiveData(receive);
        log.setReceiveTime(new Date(end));
        log.setLogType(type);
        log.setFlag(flag + "");
        //设置业务id
        JSONObject jsonObject = JSONObject.parseObject(sendData);
        String businessId;
        if (sendData.contains("data")) {
            businessId = jsonObject.getJSONObject("data").getString("internal_id");
        } else if (sendData.contains("values")) {
            businessId = jsonObject.getJSONObject("values").getString("internal_id");
        } else {
            businessId = jsonObject.getJSONObject("conditionValues").getString("internal_id");
        }
        log.setBusinessId(businessId);
        log.setRemark(apiKey);
        SpringUtils.getBean(ITInterfaceLogService.class).insertTInterfaceLog(log);
    }

    public String getToken() {
        String result = HttpRequest.get(this.dataMiddlePlatformConfig.getDataUrl() + "/token")
                .header("TYDTC_APP_ID", this.dataMiddlePlatformConfig.getTydtcAppId())
                .header("TYDTC_APP_KEY", this.dataMiddlePlatformConfig.getTydtcAppKey())
                .timeout(TIME_OUT)
                .execute()
                .body();
        if (StringUtils.isNotEmpty(result)) {
            JSONObject object = JSONObject.parseObject(result);
            if (object != null && object.containsKey("code") && object.getInteger("code") == 200) {
                if (object.containsKey("data")) {
                    return object.getString("data");
                }
            }
        }
        return null;
    }


    //公用推送
    public JSONObject postCommonInfo(JSONObject object, String typeApiKey, String logType, String createBy, Long jobId) {
        boolean flag = false;
        JSONObject data = new JSONObject();
        if (StringUtils.isNotEmpty(logType)) {
            JSONObject dataObject = new JSONObject();
            object.put("report_status", new Date());
            //根据类型的不同,判断是新增,修改,删除接口
            if (Vendor.LOG_TYPE_ADD.equals(logType)) {
                object.put("report_status", "-1"); //待新增
                dataObject.put("data", object);
            } else if (Vendor.LOG_TYPE_MODIFY.equals(logType)) {
                object.put("report_status", "-2");
                JSONObject conditions = new JSONObject();
                dataObject.put("values", object);
                conditions.put("internal_id", "=");
                dataObject.put("conditions", conditions);
                JSONObject conditionValues = new JSONObject();
                conditionValues.put("internal_id", object.get("internal_id"));
                dataObject.put("conditionValues", conditionValues);
            } else if (Vendor.LOG_TYPE_REMOVE.equals(logType)) {
                object.put("report_status", "-3");
                JSONObject conditions = new JSONObject();
                conditions.put("internal_id", "=");
                dataObject.put("conditions", conditions);
                JSONObject conditionValues = new JSONObject();
                conditionValues.put("internal_id", object.get("internal_id"));
                dataObject.put("conditionValues", conditionValues);
            } else if (Vendor.LOG_TYPE_ENABLE.equals(logType)) {
                object.put("report_status", "-2");
                JSONObject conditions = new JSONObject();
                dataObject.put("values", object);
                conditions.put("internal_id", "=");
                dataObject.put("conditions", conditions);
                JSONObject conditionValues = new JSONObject();
                conditionValues.put("internal_id", object.get("internal_id"));
                dataObject.put("conditionValues", conditionValues);
            } else if (Vendor.LOG_TYPE_DISABLE.equals(logType)) {
                object.put("report_status", -2);
                JSONObject conditions = new JSONObject();
                dataObject.put("values", object);
                conditions.put("internal_id", "=");
                dataObject.put("conditions", conditions);
                JSONObject conditionValues = new JSONObject();
                conditionValues.put("internal_id", object.get("internal_id"));
                dataObject.put("conditionValues", conditionValues);
            }
            //访问组织库接口
            data = sendToDataCenter(dataObject, typeApiKey , logType , createBy);
        }
        System.out.println(data.toJSONString());
        return data;
    }


}
