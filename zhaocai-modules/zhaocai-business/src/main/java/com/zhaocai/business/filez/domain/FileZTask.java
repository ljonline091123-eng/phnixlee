package com.zhaocai.business.filez.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * FileZ文档任务对象 tb_file_z_task
 *
 * @author WH
 * @date 2024-07-04
 */
@Data
@TableName(value = "tb_file_z_task")
public class FileZTask extends BaseEntity {

    /**
     * 附件id
     */
    @ApiModelProperty(value = "附件id")
    private Long attachmentId;

    /**
     * 业务编码
     */
    @ApiModelProperty(value = "业务编码")
    private String businessCode;

    /**
     * 业务 id
     */
    @ApiModelProperty(value = "业务 id")
    private Long businessId;

    /**
     * 任务状态 0:执行中 1:执行成功  2:执行失败
     */
    @ApiModelProperty(value = "任务状态 0:执行中 1:执行成功  2:执行失败")
    private Integer taskState;

    /**
     * 请求时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "请求时间")
    private Date requestTime;

    /**
     * 请求类型编码
     */
    @ApiModelProperty(value = "请求类型编码")
    private String requestTypeCode;

    /**
     * 请求类型
     */
    @ApiModelProperty(value = "请求类型")
    private String requestType;

    /**
     * 请求路径
     */
    @ApiModelProperty(value = "请求路径")
    private String requestUrl;

    /**
     * 请求参数
     */
    @ApiModelProperty(value = "请求参数")
    private String requestBody;

    /**
     * 任务 id
     */
    @ApiModelProperty(value = "任务 id")
    private String taskId;

    /**
     * 响应时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "响应时间")
    private Date responseTime;

    /**
     * 响应状态
     */
    @ApiModelProperty(value = "响应状态")
    private String responseCode;

    /**
     * 响应体
     */
    @ApiModelProperty(value = "响应体")
    private String responseBody;

    /**
     * 回调状态
     */
    @ApiModelProperty(value = "回调状态")
    private String callBackCode;

    /**
     * 请求回调时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value = "请求回调时间")
    private Date callBackTime;

    /**
     * 回调数据
     */
    @ApiModelProperty(value = "回调数据")
    private String callBackBody;
}
