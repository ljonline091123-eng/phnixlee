package com.zhaocai.business.vendor.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 供应商合同 vo
 *
 * @author chenming
 * @date 2024-06-22
 */
@Data
public class VendorAgreementVO{

    @ApiModelProperty(value = "项目名称")
    private String belongAccountingItem;

    @ApiModelProperty(value = "合同名称")
    private String agreementName;

    @ApiModelProperty(value = "合同编号")
    private String agreementCode;

    @ApiModelProperty(value = " 签约单位纳税人识别号")
    private String identificationNumber;

    @ApiModelProperty(value = "甲方")
    private String partyAName;

    @ApiModelProperty(value = "甲方联系人电话")
    private String partyAContactPhone;

    @ApiModelProperty(value = "甲方联系人名称")
    private String partyAContactName;

    @ApiModelProperty(value = "乙方")
    private String partyBName;

    @ApiModelProperty(value = "乙方联系电话")
    private String partyBContactPhone;

    @ApiModelProperty(value = "乙方联系人")
    private String partyBContactName;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "创建人")
    private String createBy;

    @ApiModelProperty(value = "创建人联系电话")
    private String createPhone;

    @ApiModelProperty(value = "合同文件id")
    private Long attachmentId;

    @ApiModelProperty(value = "合同文件名称")
    private String attachmentName;

    @ApiModelProperty(value = "合同文件下载类型")
    private String fileType;
}
