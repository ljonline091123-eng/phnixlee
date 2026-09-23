package com.zhaocai.business.procurement.dto.imports;

import com.zhaocai.common.core.annotation.Excel;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/** 采购物料清单 Excel 输入模型，不参与领域持久化。 */
@Data
public class MaterialsListImportDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    @Excel(name = "序号")
    private String xh;

    private String materialsId;

    private String materialsUniqueId;

    @Excel(name = "清单名称（导入）")
    private String materialsNameImport;

    @Excel(name = "特征值特征项")
    private String specification;

    @Excel(name = "清单数量")
    private BigDecimal count;

    @Excel(name = "单价(含税)")
    private BigDecimal unitPriceInclTax;

    @Excel(name = "税率(%)")
    private BigDecimal taxRate;

    @Excel(name = "备注")
    private String remark;
}
