package com.zhaocai.business.bidding.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 招标报名对象 tb_tender_apply
 *
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_tender_apply")
public class TenderApply extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 招标公告id */
    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    /** 采购方案id */
    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    /** 联系人 */
    @ApiModelProperty(value =  "联系人")
    private String contact;

    /** 联系电话 */
    @ApiModelProperty(value =  "联系电话")
    private String phone;

    /** 附件 */
    @ApiModelProperty(value =  "附件")
    private Long attachId;

    /** 供应商id */
    @ApiModelProperty(value =  "供应商id")
    private Long vendorId;

    /** 供应商名称 */
    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    /** 操作者ip */
    @ApiModelProperty(value =  "操作者ip")
    private String ipAddress;

    /** 资审结果（0不通过 1通过） */
    @ApiModelProperty(value =  "资审结果")
    private Integer approveResult;


}
