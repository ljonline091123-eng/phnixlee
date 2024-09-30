package com.zhaocai.business.manager.http.dto.req;

import lombok.Data;

/**
 * 最小核算项目详细信息请求
 *
 * @author chenming
 * @date 2024-07-14
 */
@Data
public class MinProjectDetailRequestDTO extends UnderlyingPlatformBaseDTO{

    /**
     * 最小核酸项目
     */
    private String minAccountCode;

    public MinProjectDetailRequestDTO(String minAccountCode) {
        this.minAccountCode = minAccountCode;
    }
}
