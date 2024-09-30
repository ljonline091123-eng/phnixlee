package com.zhaocai.business.pub.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zhaocai.common.core.web.domain.BaseEntity;
import io.swagger.annotations.ApiModelProperty;
import lombok.Getter;
import lombok.Setter;

/**
 * 附件对象 tb_attachment
 * 
 * @author WH
 * @date 2024-05-24
 */
@Getter
@Setter
@TableName(value = "tb_attachment")
public class Attachment extends BaseEntity {
    private static final long serialVersionUID = 1L;

    /** 业务类型 */
    @ApiModelProperty(value =  "业务类型")
    private String businessType;

    /** 业务id */
    @ApiModelProperty(value =  "业务id")
    private Long businessId;

    /** 文件路径 */
    @ApiModelProperty(value =  "文件路径")
    private String fileUrl;

    /** 文件名 */
    @ApiModelProperty(value =  "文件名")
    private String fileName;
}
