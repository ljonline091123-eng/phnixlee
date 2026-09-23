package com.zhaocai.business.vendor.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 供应商资质VO
 *
 * @author chenming
 * @date 2024/05/30
 */
@Data
public class VendorCertificationRequestVO {
    @ApiModelProperty(value = "资质id")
    private Long id;

    @ApiModelProperty(value = "附件文件路径")
    private String attachmentFileUrl;

    @ApiModelProperty(value = "附件文件名")
    private String attachmentFileName;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "有效期开始时间")
    private Date effectiveBeginDate;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "有效期结束时间")
    private Date effectiveEndDate;
}
