package com.zhaocai.business.bidding.vo.res;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author ssy
 * @date 2024/6/18 20:29
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "EvalTaskPageListVO", description = "评标任务列表VO")
public class EvalTaskPageListVO extends AdviceObject {

    @ApiModelProperty(value = "采购方案id")
    private Long schemeId;

    @ApiModelProperty(value =  "招标公告id")
    private Long noticeId;

    @ApiModelProperty(value =  "专家是否开启评标（0否-默认 1是）")
    private Integer isEval;

    @ApiModelProperty(value = "采购方案编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value = "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value = "上限价")
    private BigDecimal ceilingPrice;

    @MoneyFormat(filedName = "ceilingPrice")
    @ApiModelProperty(value = "上限价（千分位）")
    private String ceilingPricePattern;

    @ApiModelProperty(value = "采购方式")
    private Integer procurementType;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_TYPE,filedName = "procurementType")
    @ApiModelProperty(value = "采购方式-文本")
    private String procurementTypeText;

    @ApiModelProperty(value =  "专家类别（1技术类 2经济类）")
    private Integer expertType;

    @ApiModelProperty(value = "投标供应商及评分情况信息")
    private List<EvalTaskContentVO> evalTaskContentVOList;


}
