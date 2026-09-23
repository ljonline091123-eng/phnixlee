package com.zhaocai.business.manager.http.dto.req;

import lombok.Data;

/**
 * 系统字典列表请求参数
 *
 * @author chenming
 * @date 2024-08-10
 */
@Data
public class BaseDictListRequestDTO extends UnderlyingPlatformBaseDTO{

    /**
     * 字典类型
     */
    private String dictType;

    public BaseDictListRequestDTO(String dictType) {
        this.dictType = dictType;
    }
}
