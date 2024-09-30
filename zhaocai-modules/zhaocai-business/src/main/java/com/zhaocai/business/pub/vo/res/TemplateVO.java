package com.zhaocai.business.pub.vo.res;

import com.zhaocai.business.agreement.vo.res.AgreementSignStamperVO;
import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

@Data
public class TemplateVO extends AdviceObject {

    @ApiModelProperty(value = "模板id")
    private Long id;

    @ApiModelProperty(value = "模板名称")
    private String templateName;

    @ApiModelProperty(value = "模板分类")
    private Integer templateType;

    @ApiModelProperty(value = "使用单位编码")
    private String usingUnitNo;

    @ApiModelProperty(value = "使用单位名称")
    private String usingUnitName;

    @ApiModelProperty(value = "合同类型")
    private String contractType;

    @ApiModelProperty(value = "合同类型名称")
    private String contractName;

    @ApiModelProperty(value = "是否允许编辑")
    private Integer isEditable;

    @ApiModelProperty(value = "文件名")
    private String fileName;

    @ApiModelProperty(value = "文件url")
    private String fileUrl;

    @ApiModelProperty(value = "附件 id")
    private Long attachmentId;

    @ApiModelProperty(value = "维护人")
    private String createBy;

    @ApiModelProperty(value = "合同签署位置")
    private List<AgreementSignStamperVO> agreementSignStamperList;
}
