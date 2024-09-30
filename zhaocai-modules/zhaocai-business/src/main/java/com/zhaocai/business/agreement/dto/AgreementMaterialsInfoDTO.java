package com.zhaocai.business.agreement.dto;

import com.zhaocai.business.procurement.domain.MaterialsList;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 合同清单数据
 *
 * @author chenming
 * @date 2024-09-05
 */
@Data
@Builder
public class AgreementMaterialsInfoDTO {

    /**
     * 交易标的物名称
     */
    private String subjectMatterName;

    /**
     * 交易标的物编码
     */
    private String subjectMatterCode;

    /**
     * 含税总金额
     */
    private BigDecimal totalAmountInclTax;

    /**
     * 不含税总金额
     */
    private BigDecimal totalAmountExclTax;

    /**
     * 回写的物料清单
     */
    private List<MaterialsList> materialsLists;
}
