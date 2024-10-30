package com.zhaocai.business.bidding.vo.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.procurement.domain.ProcurementSchemeBidding;
import com.zhaocai.business.procurement.vo.req.ProcurementSchemeBiddingReqVO;
import com.zhaocai.business.procurement.vo.req.ProcurementSchemeReqVO;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.vendor.vo.req.VendorManagementListQueryDataVO;
import com.zhaocai.common.core.bean.ValidateGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * @author ssy
 * @date 2024/5/27 15:51
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TenderNoticeVO", description = "招标公告VO")
public class TenderNoticeVO implements Serializable {
    private static final long serialVersionUID = 4206454874416239487L;

    @ApiModelProperty(value =  "招标项目id")
    private Long projectId;

    @ApiModelProperty(value =  "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "采购方案类型（1公开招标 2邀请招标 3询价采购 4单一来源）")
    private Integer schemeType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value =  "投标截止时间")
    @NotNull(message = "投标截止时间不能为空", groups = {ValidateGroup.AddGroup.class})
    private Date applyTime;

    @ApiModelProperty(value =  "联系人")
    private String contact;

    @ApiModelProperty(value =  "联系电话")
    private String phone;

    @ApiModelProperty(value =  "联系邮箱")
    private String email;

    @ApiModelProperty(value =  "招标公告模板id")
    private Long templateId;

    @ApiModelProperty(value =  "招标公告附件id")
    private Long attachId;

    @ApiModelProperty(value = "招标文件附件")
    private List<AttachmentRequestVO> biddingDocAttachList;

    /** 设置供应商范围 */
    @ApiModelProperty(value =  "设置供应商范围")
    private List<Long> vendorIds;

    @ApiModelProperty(value =  "供应商查询参数")
    private VendorManagementListQueryDataVO vendorQueryParam;


    @ApiModelProperty(value =  "招标公告id")
    private Long id;

    /** 发布公告联系人id */
    @ApiModelProperty(value =  "发布公告联系人id")
    private Long contactIdNotice;

    /** 发布公告联系人 */
    @ApiModelProperty(value =  "发布公告联系人")
    private String contactNotice;

    /** 发布公告联系电话 */
    @ApiModelProperty(value =  "发布公告联系电话")
    private String phoneNotice;

    /** 发布公告联系邮箱 */
    @ApiModelProperty(value =  "发布公告联系邮箱")
    private String emailNotice;

    /** 发布公告报名截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value =  "发布公告报名截止时间")
    private Date applyTimeNotice;

    /** 发布公告招标公告附件id */
    @ApiModelProperty(value =  "发布公告招标公告附件id")
    private Long attachIdNotice;


    /** 通过的供应商报名列表 */
    @ApiModelProperty(value =  "通过的供应商报名列表")
    private List<Long> vendorApplyIds;

    /** 付款方式 [
     { label: "现付", value: 1 },
     { label: "旬付", value: 2 },
     { label: "月付", value: 3 },
     { label: "资金利息", value: 4 },
     ], */
    @ApiModelProperty(value =  "付款方式")
    private Integer paymentType;


    /**
     * 增加废标后操作可以 修改采购方案招标类型和招标文件
     * Time:2024/10/23 下午2:36
     * */

    @ApiModelProperty(value = "采购方案基本信息")
    private ProcurementSchemeReqVO procurementScheme;

    @ApiModelProperty(value = "采购方案招标信息")
    private ProcurementSchemeBiddingReqVO procurementSchemeBidding;
}
