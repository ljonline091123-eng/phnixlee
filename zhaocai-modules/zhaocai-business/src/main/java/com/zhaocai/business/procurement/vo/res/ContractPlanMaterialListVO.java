package com.zhaocai.business.procurement.vo.res;

import com.zhaocai.business.common.annotations.MoneyFormat;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 项目合约规划物料清单
 *
 * @author chenming
 * @date 2024-07-25
 */
@Data
public class ContractPlanMaterialListVO extends AdviceObject {

    @ApiModelProperty(value = "上限价")
    private BigDecimal upperLimitPrice;

    @ApiModelProperty(value = "交易标的物文本")
    private String subjectMatterText;

    @ApiModelProperty(value = "交易标的物 code")
    private String subjectMatterCode;

    @ApiModelProperty(value = "交易标的物")
    private Integer subjectMatter;

    @ApiModelProperty(value = "物料清单")
    private List<ContractMaterialsListVO> contractMaterialsList;

    @MoneyFormat(filedName = "upperLimitPrice")
    private String upperLimitPriceText;

    public ContractPlanMaterialListVO(BigDecimal upperLimitPrice,List<ContractMaterialsListVO> contractMaterialsList) {
        this.upperLimitPrice = upperLimitPrice;
        this.contractMaterialsList = contractMaterialsList;
    }
}
