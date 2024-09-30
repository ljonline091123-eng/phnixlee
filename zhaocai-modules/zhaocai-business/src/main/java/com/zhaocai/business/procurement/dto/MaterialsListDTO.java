package com.zhaocai.business.procurement.dto;

import com.zhaocai.business.procurement.domain.MaterialsList;
import lombok.Data;

/**
 * 物料清单vo
 *
 * @author chenming
 * @date 2024/05/28
 */
@Data
public class MaterialsListDTO extends MaterialsList {

    private Long splitId;

    private String splitContractName;

    private String contractScope;
}
