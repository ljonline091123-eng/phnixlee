package com.zhaocai.business.vendor.util;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSONObject;
import com.zhaocai.business.manager.template.config.UnderlingPlatformConfig;
import com.zhaocai.business.vendor.config.DataMiddlePlatformConfig;
import com.zhaocai.business.vendor.domain.Vendor;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import java.io.File;

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
    private static final Integer TIME_OUT = 30000;

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


    private JSONObject sendToDataCenter(JSONObject dataObject, String TYDTC_API_KEY) {
        boolean flag = false;
        JSONObject data = new JSONObject();
        Long start = System.currentTimeMillis();
        //发送的数据
        String sendData = dataObject.toJSONString();

        //加密数据格式tyStamp+sendData+tydtcAppSecret md5加密
        String tySign = DigestUtils.md5Hex(start + sendData + this.dataMiddlePlatformConfig.getTydtcAppSecret());

        //地址
        String url = underlingPlatformConfig.getBaseUrl() + "/" + TYDTC_API_KEY + "?tyStamp=" + start + "&tySign=" + tySign;
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

        /*try {
            //保存访问的日志
            saveLog(start, url, sendData, receive, end, type, flag, createBy, jobId);
        } catch (Exception e) {
            e.printStackTrace();
        }*/

        if (receive.contains("Read timed out")) {
            data = new JSONObject(Integer.parseInt("-10000"));
        }

        return data;
    }


    /*private void saveLog(Long start, String url, String sendData, String receive, Long end, String type, boolean flag, String createBy, Long jobId) {
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
        SpringUtils.getBean(ITInterfaceLogService.class).insertTInterfaceLog(log);
        //记录推送失败的次数
        //获取IFailedPushService实例
        IFailedPushService bean = SpringUtils.getBean(IFailedPushService.class);
        LambdaQueryWrapper<FailedPush> lambdaQuery = Wrappers.lambdaQuery();
        lambdaQuery.eq(FailedPush::getBusinessId, log.getBusinessId());
        if (flag) {
            bean.getBaseMapper().delete(lambdaQuery);
        } else {
            FailedPush failedPush = new FailedPush();
            failedPush.setBusinessId(log.getBusinessId());
            //查询该业务id是否存在表里
            FailedPush failed = bean.getBaseMapper().selectOne(lambdaQuery);
            //如果业务id为null,则插入新数据，如果有，则修改错误次数
            if (failed == null) {
                failedPush.setCreateBy(createBy);
                failedPush.setSendFlag("0");
                failedPush.setFailCount(1L);
                bean.save(failedPush);
            } else {
                failed.setFailCount(failed.getFailCount() + 1);
                bean.updateById(failed);
            }
        }
    }
*/
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
        String ty = (String) object.get("type");
        boolean flag = false;
        JSONObject data = new JSONObject();
        if (StringUtils.isNotEmpty(ty)) {
            JSONObject dataObject = new JSONObject();
            String tydtcApiKey = "";
            String type = "";
            //根据类型的不同,判断是新增,修改,删除接口
            if (Vendor.LOG_TYPE_ADD.equals(ty)) {
                object.put("approval_status", "-1"); //待新增
                dataObject.put("data", object);
            } else if (Vendor.LOG_TYPE_MODIFY.equals(ty)) {
                object.put("report_status", "-2");
                JSONObject conditions = new JSONObject();
                dataObject.put("values", object);
                conditions.put("internal_id", "=");
                dataObject.put("conditions", conditions);
                JSONObject conditionValues = new JSONObject();
                conditionValues.put("internal_id", object.get("internal_id"));
                dataObject.put("conditionValues", conditionValues);
            } else if (Vendor.LOG_TYPE_REMOVE.equals(ty)) {
                object.put("report_status", "-3");
                JSONObject conditions = new JSONObject();
                conditions.put("internal_id", "=");
                dataObject.put("conditions", conditions);
                JSONObject conditionValues = new JSONObject();
                conditionValues.put("internal_id", object.get("internal_id"));
                dataObject.put("conditionValues", conditionValues);
            } else if (Vendor.LOG_TYPE_ENABLE.equals(ty)) {
                object.put("report_status", "-2");
                JSONObject conditions = new JSONObject();
                dataObject.put("values", object);
                conditions.put("internal_id", "=");
                dataObject.put("conditions", conditions);
                JSONObject conditionValues = new JSONObject();
                conditionValues.put("internal_id", object.get("internal_id"));
                dataObject.put("conditionValues", conditionValues);
            } else if (Vendor.LOG_TYPE_DISABLE.equals(ty)) {
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
            data = sendToDataCenter(dataObject, tydtcApiKey);
        }

        return data;
    }


}
