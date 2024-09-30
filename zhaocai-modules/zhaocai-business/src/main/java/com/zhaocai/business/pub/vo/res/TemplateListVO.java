package com.zhaocai.business.pub.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 模板列表查询
 *
 * @author chenming
 * @date 2024-06-25
 */
@Data
public class TemplateListVO extends AdviceObject {

    @ApiModelProperty(value = "模板id")
    private Long id;

    @ApiModelProperty(value = "模板名称")
    private String templateName;

    @ApiModelProperty(hidden = true)
    private Integer templateType;

    @ApiModelProperty(value = "创建者")
    private String createBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss",timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    @ApiModelProperty(value = "使用单位名称")
    private String usingUnitName;

    @ApiModelProperty(value = "合同类型")
    private String contractType;

    @ApiModelProperty(value = "合同类型名称")
    private String contractName;

    @DictCache(dictBizEnum = DictBizEnum.TEMPLATE_TYPE,filedName = "templateType")
    @ApiModelProperty(value = "模板分类")
    private String templateTypeText;

    @ApiModelProperty(value = "文件名称")
    private String fileName;

    @ApiModelProperty(value = "文件url")
    private String fileUrl;

    @ApiModelProperty(value = "附件id")
    private Long attachmentId;
}
