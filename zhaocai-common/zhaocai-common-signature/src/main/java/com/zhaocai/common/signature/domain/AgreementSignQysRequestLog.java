package com.zhaocai.common.signature.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 合同签章-契约锁请求日志对象
 *
 * @author chenming
 * @date 2024-09-09
 */
@Data
@TableName(value = "tb_agreement_sign_qys_request_log")
public class AgreementSignQysRequestLog{
    /**
     * id
     */
    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 交易流水号
     */
    @TableField("bizId")
    private String bizId;

    /**
     * 业务编码
     */
    @TableField("business_code")
    private String businessCode;

    /**
     * 合同id
     */
    @TableField("business_id")
    private Long businessId;

    /**
     * 申请人名称
     */
    @TableField("applicant_name")
    private String applicantName;

    /**
     * 申请人联系方式
     */
    @TableField("applicant_mobile")
    private String applicantMobile;

    /**
     * 请求接口名称
     */
    @TableField("request_name")
    private String requestName;

    /**
     * 请求url
     */
    @TableField("request_url")
    private String requestUrl;

    /**
     * 请求参数
     */
    @TableField("request_params")
    private String requestParams;

    /**
     * 请求时间
     */
    @TableField("request_time")
    private Date requestTime;

    /**
     * 响应码
     */
    @TableField("response_code")
    private String responseCode;

    /**
     * 响应消息
     */
    @TableField("response_message")
    private String responseMessage;

    /**
     * 响应数据
     */
    @TableField("response_result")
    private String responseResult;

    /**
     * 响应时间
     */
    @TableField("response_time")
    private Date responseTime;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private Date createTime;

    /**
     * 修改时间
     */
    @TableField("update_time")
    private Date updateTime;
}
