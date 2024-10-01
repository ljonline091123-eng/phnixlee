package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.procurement.vo.res.CompMaterialsVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

/**
 * @author ssy
 * @date 2024/6/3 15:04
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "BiddingInfoDetailVO", description = "投标单详情信息VO")
public class BiddingInfoDetailVO extends AdviceObject {

    @ApiModelProperty(value =  "供应商名称")
    private String vendorName;

    @ApiModelProperty(value =  "含税总价")
    private BigDecimal taxPrice;

    @MoneyFormat(filedName = "taxPrice",scale = 2)
    @ApiModelProperty(value =  "含税总价（千分位）")
    private String taxPricePattern;

    @ApiModelProperty(value =  "不含税总价")
    private BigDecimal notTaxPrice;

    @MoneyFormat(filedName = "notTaxPrice",scale = 2)
    @ApiModelProperty(value =  "不含税总价（千分位）")
    private String notTaxPricePattern;

    @ApiModelProperty(value =  "联系人")
    private String contact;

    @ApiModelProperty(value =  "联系电话")
    private String phone;

    @ApiModelProperty(value =  "标书附件id")
    private Long bidAttachId;

    @ApiModelProperty(value =  "投标标书附件")
    private List<AttachmentVO> attachments;

    @ApiModelProperty(value =  "投标单清单报价详情信息")
    private List<BiddingQuotationDetailVO> quotationDetailVOList;

    @ApiModelProperty(value = "价格类型(1.固定价 2.浮动价)")
    private Integer priceType;

//    @ApiModelProperty(value = "交易标的物（ 1（钢筋） 2（砼））")
//    private String subjectMatter;

    @ApiModelProperty(value = "交易标的物类型（ 1（钢筋） 2（砼））")
    private Integer subjectMatterType;

    @ApiModelProperty(value =  "供应商投标清单信息")
    private List<CompMaterialsVO> materialsList;


    /** 二次报价设置（默认0关 1开） */
    @ApiModelProperty(value =  "二次报价设置（默认0关 1开）")
    private Integer twiceQuot;

    /** 二次报价截止时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @ApiModelProperty(value =  "二次报价截止时间")
    private Date twiceTime;

    /* 每开启一次二次报价，就将选中范围的供应商报价 复制一份并升级版本将twiceQuot状态打开。前端通过招标对象的版本和当前版本对比和二次报价开关对比进行开放是否 供应商可以报价 */
    /** 二次报价版本号，对应招标对象的版本号，如果对应不上就是在第*次开启报价时未选中或者是供应商未调价 管理端控制发版号 */
    @ApiModelProperty(value =  "二次报价版本号。从1开始")
    private Integer twiceQuotVersion;

    /** 供应商调价状态(当前二次报价版本) 未被选中进行二次报价的供应商状态为 0未调价 选中的供应商报价了 状态为 1已调价 选中的未进行报价的供应商状态为 2放弃调价  */
    @ApiModelProperty(value =  "供应商调价状态")
    private Integer priceChangeState;

}
