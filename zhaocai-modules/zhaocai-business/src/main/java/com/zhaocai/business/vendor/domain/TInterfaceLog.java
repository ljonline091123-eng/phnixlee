package com.zhaocai.business.vendor.domain;

/**
 * @author hyt
 * @date 2023/7/20 11:47
 */

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.zhaocai.common.core.annotation.Excel;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

import java.io.Serializable;
import java.util.Date;

/**
 * 接口访问记录对象 t_interface_log
 *
 * @author ruoyi
 * @date 2023-07-20
 */
public class TInterfaceLog implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    @TableId("id")
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 访问时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @Excel(name = "访问时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date sendTime;

    /**
     * 访问参数
     */
    @Excel(name = "访问参数")
    private String sendData;

    @TableField(exist = false)
    private Object sendDataJson;

    /**
     * 访问地址
     */
    @Excel(name = "访问地址")
    private String sendUrl;

    /**
     * 返回参数
     */
    @Excel(name = "返回参数")
    private String receiveData;

    @TableField(exist = false)
    private Object receiveDataJson;

    /**
     * 返回时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @Excel(name = "返回时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date receiveTime;

    /**
     * 日志类型
     */
    @Excel(name = "日志类型")
    private String logType;

    @TableField(exist = false)
    private String logTypeName;

    /**
     * 是否成功
     */
    @Excel(name = "是否成功")
    private String flag;

    @TableField("business_id")
    private String businessId;

    @Excel(name = "备注")
    private String remark;

    public String getBusinessId() {
        return businessId;
    }

    public void setBusinessId(String businessId) {
        this.businessId = businessId;
    }

    public Object getReceiveDataJson() {
        return receiveDataJson;
    }

    public void setReceiveDataJson(Object receiveDataJson) {
        this.receiveDataJson = receiveDataJson;
    }

    public Object getSendDataJson() {
        return sendDataJson;
    }

    public void setSendDataJson(Object sendDataJson) {
        this.sendDataJson = sendDataJson;
    }

    public String getLogTypeName() {
        return logTypeName;
    }

    public void setLogTypeName(String logTypeName) {
        this.logTypeName = logTypeName;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setSendTime(Date sendTime) {
        this.sendTime = sendTime;
    }

    public Date getSendTime() {
        return sendTime;
    }

    public void setSendData(String sendData) {
        this.sendData = sendData;
    }

    public String getSendData() {
        return sendData;
    }

    public void setSendUrl(String sendUrl) {
        this.sendUrl = sendUrl;
    }

    public String getSendUrl() {
        return sendUrl;
    }

    public void setReceiveData(String receiveData) {
        this.receiveData = receiveData;
    }

    public String getReceiveData() {
        return receiveData;
    }

    public void setReceiveTime(Date receiveTime) {
        this.receiveTime = receiveTime;
    }

    public Date getReceiveTime() {
        return receiveTime;
    }

    public void setLogType(String logType) {
        this.logType = logType;
    }

    public String getLogType() {
        return logType;
    }

    public String getFlag() {
        return flag;
    }

    public void setFlag(String flag) {
        this.flag = flag;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this, ToStringStyle.MULTI_LINE_STYLE)
                .append("id", getId())
                .append("sendTime", getSendTime())
                .append("sendData", getSendData())
                .append("sendUrl", getSendUrl())
                .append("receiveData", getReceiveData())
                .append("receiveTime", getReceiveTime())
                .append("logType", getLogType())
                .append("flag", getFlag())
                .append("businessId", getBusinessId())
                .append("remark", getRemark())
                .toString();
    }
}