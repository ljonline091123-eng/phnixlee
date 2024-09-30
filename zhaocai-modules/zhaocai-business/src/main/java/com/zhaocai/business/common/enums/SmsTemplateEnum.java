package com.zhaocai.business.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author ssy
 * @date 2024/9/3 15:52
 */
@Getter
@AllArgsConstructor
public enum SmsTemplateEnum {

    /**
     * 短信模版定义
     * 1.招标公告-新 code：SMS_471995094 内容：${unit_name}公司${name}项目${Task}招标已发布，请贵司于${time}时间前登陆https://zc.hncig.cn:32068/查看！
     * 2.密码找回 code：SMS_470540115 内容：湖南建投集团招采网-尊敬的${name}用户，您找回密码的验证码为${password}，感谢您的支持！
     * 3.未中标通知 code：SMS_468845593 内容：贵司于${time}所递交的${Projectname}项目的投标文件，经过评标小组认真评定，已确定未中标，感谢贵单位参与我司本次招标活动！
     * 4.中标通知 code：SMS_468990612 内容：贵司于${time}所递交的${Projectname}项目的投标文件，经过评标小组认真评定，已确定中标，请贵司在5日内与招标单位联系签订相关合同！
     * 5.专家评标通知 code：SMS_468985597 内容：${name}专家您好！${place}招标确定了您为${catalogue}评标专家，请您及时登录https://zc.hncig.cn:32068/进行评标工作！
     * 6.招标公告 code：SMS_468760345 内容：${unit_name}正在发布${name}类型招标公告，招标截止时间${time}（x年x月x日x时x分x秒）前可登陆湖南建投集团招采网站（https://info-uat.hncig.cn:82）报名投标！
     * 7.验证码短信 code：SMS_300366210 内容：您的验证码为：${code}，请勿泄露于他人！
     */

    TENDER_NOTICE("SMS_471995094", "${unit_name}公司${name}项目${Task}招标已发布，请贵司于${time}时间前登陆https://zc.hncig.cn:32068/查看！"),
    GET_PWD_BACK("SMS_470540115", "湖南建投集团招采网-尊敬的${name}用户，您找回密码的验证码为${password}，感谢您的支持！"),
    UN_BID_NOTICE("SMS_468845593", "贵司于${time}所递交的${Projectname}项目的投标文件，经过评标小组认真评定，已确定未中标，感谢贵单位参与我司本次招标活动！"),
    BID_NOTICE("SMS_468990612", "贵司于${time}所递交的${Projectname}项目的投标文件，经过评标小组认真评定，已确定中标，请贵司在5日内与招标单位联系签订相关合同！"),
    EXPERT_BID_EVA_NOTICE("SMS_468985597", "${name}专家您好！${place}招标确定了您为${catalogue}评标专家，请您及时登录https://zc.hncig.cn:32068/进行评标工作！"),
    TENDER_NOTICE_OLD("SMS_468760345", "${unit_name}正在发布${name}类型招标公告，招标截止时间${time}（x年x月x日x时x分x秒）前可登陆湖南建投集团招采网站（https://info-uat.hncig.cn:82）报名投标！"),
    VERIFY_CODE_SMS("SMS_300366210", "您的验证码为：${code}，请勿泄露于他人"),

    ;

    private final String code;

    private final String desc;

}
