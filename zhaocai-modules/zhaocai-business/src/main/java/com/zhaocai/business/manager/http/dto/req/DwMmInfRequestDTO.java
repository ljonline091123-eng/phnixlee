package com.zhaocai.business.manager.http.dto.req;

import lombok.Data;

/**
 * 资产 dm073、dm073 数据查询
 *
 * @author chenming
 * @date 2024-08-29
 */
@Data
public class DwMmInfRequestDTO extends UnderlyingPlatformBaseDTO{

    @Override
    public Boolean getLogResponseData() {
        return false;
    }
}
