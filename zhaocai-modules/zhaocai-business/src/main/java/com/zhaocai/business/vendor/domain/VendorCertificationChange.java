package com.zhaocai.business.vendor.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 供应商资质变更对象 tb_vendor_certification_change
 *
 * @author lsn
 * @date 2024-08-06
 */
@Data
@TableName(value = "tb_vendor_certification_change")
public class VendorCertificationChange extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /**
     * 供应商资质id
     */
    @ApiModelProperty(value = "供应商资质id")
    private Long certificationId;

    /**
     * 版本号
     */
    @ApiModelProperty(value = "版本号")
    private Integer version;

    /**
     * 变更状态
     */
    @ApiModelProperty(value = "变更状态")
    private Integer changeStatus;

    /**
     * 供应商id
     */
    @ApiModelProperty(value = "供应商id")
    private Long vendorId;

    /**
     * 业务编码
     */
    @ApiModelProperty(value = "业务编码")
    private String businessCode;

    /**
     * 业务id
     */
    @ApiModelProperty(value = "业务id")
    private Long businessId;

    /**
     * 附件文件路径
     */
    @ApiModelProperty(value = "附件文件路径")
    private String attachmentFileUrl;

    /**
     * 附件文件名
     */
    @ApiModelProperty(value = "附件文件名")
    private String attachmentFileName;

    /**
     * 有效期开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "有效期开始时间")
    private Date effectiveBeginDate;

    /**
     * 有效期结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd")
    @ApiModelProperty(value = "有效期结束时间")
    private Date effectiveEndDate;
}
