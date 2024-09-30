package com.zhaocai.business.filez.vo.res;

import com.alibaba.fastjson.annotation.JSONField;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 文件元数据封装类
 *
 * @author lijie@lenovocloud.com
 */
@Data
@NoArgsConstructor
public class DocMeta implements Serializable {
    @JSONField(ordinal = 1)
    private String id;

    @JSONField(ordinal = 2)
    private String version;

    @JSONField(ordinal = 3)
    private String name;

    @JSONField(ordinal = 4)
    private String description;

    @JSONField(ordinal = 5)
    private Long size;

    @JSONField(ordinal = 6, name = "created_at", format = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date createdAt;

    @JSONField(ordinal = 7, name = "created_by")
    private User createdBy;

    @JSONField(ordinal = 8, name = "modified_at", format = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    private Date modifiedAt;

    @JSONField(ordinal = 9, name = "modified_by")
    private User modifiedBy;

    @JSONField(ordinal = 10)
    private DocPermission permissions;

    @JSONField(ordinal = 11)
    private String role;

    @JSONField(ordinal = 12)
    private Extension extension;

    @JSONField(ordinal = 13)
    private WaterMark waterMark;

    @JSONField(ordinal = 14)
    private boolean needWaterMark = false;

    @Builder
    public DocMeta(String id, String version, String name, String description, Long size, Date createdAt, User createdBy, Date modifiedAt, User modifiedBy, DocPermission permissions, String role, Extension extension, WaterMark waterMark, boolean needWaterMark) {
        this.id = id;
        this.version = version;
        this.name = name;
        this.description = description;
        this.size = size;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
        this.modifiedAt = modifiedAt;
        this.modifiedBy = modifiedBy;
        this.permissions = permissions;
        this.role = role;
        this.extension = extension;
        this.waterMark = waterMark;
        this.needWaterMark = needWaterMark;
    }
}
