package com.zhaocai.business.common.sms;

import com.alibaba.fastjson.JSON;
import org.dromara.sms4j.api.SmsBlend;
import org.dromara.sms4j.api.entity.SmsResponse;
import org.dromara.sms4j.core.factory.SmsFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;

/**
 * @author ssy
 * @date 2024/7/4 11:41
 */
@Component
public class SmsSenderUtil {

    private static final Logger log = LoggerFactory.getLogger(SmsSenderUtil.class);
    /** 自定义的标识，configId这里取得是配置文件中默认值 */
    private static final String DEFAULT_CONFIG_ID = "tx1";

    @Deprecated
    public void sendMessage(String unitName, String name, String applyTime, List<String> phoneList){
        for (String phone : phoneList){
            sendMessage(unitName, name, applyTime, phone);
        }
        return;
    }

    @Deprecated
    public void sendMessage(String unitName, String name, String applyTime, String phone){
        try {
            SmsBlend smsBlend = SmsFactory.getSmsBlend("tx1");
            LinkedHashMap<String, String> var2 = new LinkedHashMap<>();
            //发布公司
//        var2.put("unit_name","深圳晟晟科技有限公司");
            var2.put("unit_name", unitName);
            //采购类型
            var2.put("name", name);
            //招投标截止时间
            var2.put("time", applyTime);

            SmsResponse smsResponse = smsBlend.sendMessage(phone, var2);
        } catch (Exception ex) {
            log.error("发送短信失败:{}", ex.getMessage());
        }

    }

    public void sendMessage(String templateId, String phone, LinkedHashMap<String, String> varParam){
        sendMessage(DEFAULT_CONFIG_ID, templateId, phone, varParam);
    }

    public void sendMessage(String templateId, List<String> phones, LinkedHashMap<String, String> varParam){
        sendMessage(DEFAULT_CONFIG_ID, templateId, phones, varParam);
    }

    public void sendMessage(String configId, String templateId, String phone, LinkedHashMap<String, String> varParam){
        try {
            SmsBlend smsBlend = SmsFactory.getSmsBlend(configId);
            SmsResponse smsResponse = smsBlend.sendMessage(phone, templateId, varParam);

            if (smsResponse.isSuccess()){
            } else {
                log.info("发送短信失败:{}", JSON.toJSONString(smsResponse.getData()));
            }
        } catch (Exception ex) {
            log.info("发送短信失败:{}", ex.getMessage());
        }
    }

    public void sendMessage(String configId, String templateId, List<String> phones, LinkedHashMap<String, String> varParam){
        try {
            SmsBlend smsBlend = SmsFactory.getSmsBlend(configId);
            SmsResponse smsResponse = smsBlend.massTexting(phones, templateId, varParam);

            if (smsResponse.isSuccess()){
            } else {
                log.info("发送短信失败:{}", JSON.toJSONString(smsResponse.getData()));
            }
        } catch (Exception ex) {
            log.error("发送短信失败:{}", ex.getMessage());
        }
    }



}
