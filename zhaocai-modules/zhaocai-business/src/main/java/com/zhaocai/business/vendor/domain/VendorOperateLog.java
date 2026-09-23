package com.zhaocai.business.vendor.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 供应商操作日志对象 tb_vendor_operate_log
 *
 * @author WH
 * @date 2024-06-25
 */
@Data
@TableName(value = "tb_vendor_operate_log")
public class VendorOperateLog extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 供应商 id
     */
    @ApiModelProperty(value = "供应商 id")
    private Long vendorId;

    /**
     * 操作业务类型
     */
    @ApiModelProperty(value = "操作业务类型")
    private String businessCode;

    /**
     * 状态
     */
    @ApiModelProperty(value = "状态")
    private Integer state;

    /**
     * 流程实例 id
     */
    @ApiModelProperty(value = "流程实例 id")
    private String wfProcessId;


}
