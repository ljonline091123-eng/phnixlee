package com.zhaocai.business.agreement.vo.res;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 合同签章签署位置对象
 *
 * @author chenming
 * @date 2024-09-14
 */
@Data
public class AgreementSignStamperVO extends AdviceObject {

    @ApiModelProperty(value = "类型 1=：发起方,2=：接收方")
    private Integer type;

    @ApiModelProperty(value = "模板 id")
    private Long templateId;

    @ApiModelProperty(value = "签署类型")
    private String signType;

    @ApiModelProperty(value = "关键词")
    private String keyWord;

    @ApiModelProperty(value = "签署坐标页码")
    private Integer signPage;

    @ApiModelProperty(value = "签署 X 轴坐标")
    private BigDecimal offsetX;

    @ApiModelProperty(value = "签署 Y 轴坐标")
    private BigDecimal offsetY;

    @DictCache(dictBizEnum = DictBizEnum.AGREEMENT_STAMPER_TYPE,filedName = "signType")
    @ApiModelProperty(value = "签署类型-文本")
    private String signTypeText;
}
