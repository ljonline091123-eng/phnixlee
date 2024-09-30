package com.zhaocai.business.vendor.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;


/**
 * 供应商资质
 *
 * @author chenming
 * @date 2024/05/30
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class VendorCertificationVO {

    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "文件url")
    private String attachmentFileUrl;

    @ApiModelProperty(value = "文件名")
    private String attachmentFileName;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "有效期开始时间")
    private Date effectiveBeginDate;

    @JsonFormat(pattern = "yyyy-MM-dd",timezone = "GMT+8")
    @ApiModelProperty(value = "有效期结束时间")
    private Date effectiveEndDate;
}
