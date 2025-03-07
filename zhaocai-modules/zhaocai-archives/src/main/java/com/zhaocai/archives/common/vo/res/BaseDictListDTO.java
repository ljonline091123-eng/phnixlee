package com.zhaocai.archives.common.vo.res;

import lombok.Data;

/**
 * 基本字典
 *
 * @author chenming
 * @date 2024-08-10
 */
@Data
public class BaseDictListDTO {

    /**
     * 字典名称
     */
    private String dictName;

    /**
     * 字典类型
     */
    private String dictType;

    /**
     * 字典值
     */
    private String dictValue;

    /**
     * 启用
     */
    private Boolean enable;

    /**
     * ID
     */
    private String id;

    /**
     * 外部系统映射值
     */
    private String otherValue;

    /**
     * 上级ID，一级为0
     */
    private String parentId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 排序
     */
    private Integer sort;
}
