package com.zhaocai.business.procurement.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 项目管理-最小核算项目信息列表属性
 *
 */
@Data
public class MinProjectListVO extends AdviceObject {
    @ApiModelProperty(value = "id")
    private Long id;

    @ApiModelProperty(value = "最小核算项目编码")
    private String minAccountCode;

    @ApiModelProperty(value = "项目全称")
    private String minAccountFullName;

    @ApiModelProperty(value = "项目简称")
    private String minAccountSimpleName;

    @ApiModelProperty(value = "归属项目部")
    private String projectDepartment;

    @ApiModelProperty(value = "项目业态")
    private String prjState;

    @DictCache(dictBizEnum= DictBizEnum.UNDERLING_PROJECT_FORMAT,filedName = "prjState")
    @ApiModelProperty(value = "项目业态")
    private String prjStateText;

    @ApiModelProperty(value = "工程类型")
    private String prgType;

    @ApiModelProperty(value = "工程类型文本")
    private String prgTypeText;

    @ApiModelProperty(value = "项目资金来源")
    private String moneySec;

    @DictCache(dictBizEnum= DictBizEnum.project_funds_source,filedName = "moneySec")
    @ApiModelProperty(value = "项目资金来源文本")
    private String moneySecText;

    @ApiModelProperty(value = "项目管理模式")
    private String prjManageModel;

    @DictCache(dictBizEnum= DictBizEnum.PROJECT_MANAGE_MODEL,filedName = "prjManageModel")
    @ApiModelProperty(value = "项目管理模式文本")
    private String prjManageModelText;


}
