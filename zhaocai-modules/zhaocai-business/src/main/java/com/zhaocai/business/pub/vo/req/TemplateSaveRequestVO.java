package com.zhaocai.business.pub.vo.req;

import com.zhaocai.business.agreement.domain.AgreementSignStamper;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * 模板对象 tb_template
 *
 * @author WH
 * @date 2024-06-25
 */
@Data
public class TemplateSaveRequestVO {

    @ApiModelProperty(value = "模板id")
    private Long id;

    @ApiModelProperty(value = "模板名称")
    private String templateName;

    @ApiModelProperty(value = "模板分类")
    private Integer templateType;

    @ApiModelProperty(value = "使用单位编码")
    private String usingUnitNo;

    @ApiModelProperty(value = "合同类型")
    private String contractType;

    @ApiModelProperty(value = "合同类型名称")
    private String contractName;

    @ApiModelProperty(value = "使用单位名称")
    private String usingUnitName;

    @ApiModelProperty(value = "是否允许编辑")
    private Integer isEditable;

    @ApiModelProperty(value = "附件id")
    private Long attachmentId;

    @ApiModelProperty(value = "合同签订位置")
    private List<AgreementSignStamper> agreementSignStamperList;
}
