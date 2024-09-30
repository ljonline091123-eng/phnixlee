package com.zhaocai.business.manager.http.dto.req;


import lombok.Data;

/**
 * 底层逻辑分页基础 DTO
 *
 * @author chenming
 * @date 2024-07-09
 */
@Data
public class UnderlyingPlatformPageBaseDTO extends UnderlyingPlatformBaseDTO {

    /**
     * 页码
     */
    protected Integer pageNum;

    /**
     * 每页大小
     */
    protected int pageSize;

    /**
     * 排序方式（asc 或 desc）
     */
    protected String order;

    /**
     * 排序字段（多字段排序以逗号分割）
     */
    protected String orderField;
}
