package com.zhaocai.business.bidding.vo.req;

import com.zhaocai.common.core.bean.ValidateGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/5/30 16:02
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TenderNoticeAnswerVO", description = "招标公告/招标内容答疑对象VO")
public class TenderNoticeAnswerVO implements Serializable {
    private static final long serialVersionUID = -4580946068259143369L;

    @ApiModelProperty(value =  "主键id")
    @NotNull(message = "主键id不能为空", groups = {ValidateGroup.UpdateGroup.class})
    private Long id;

    @ApiModelProperty(value =  "关联业务id")
    @NotNull(message = "关联业务id不能为空", groups = {ValidateGroup.AddGroup.class})
    private Long busId;

    @ApiModelProperty(value =  "提问内容")
    @NotBlank(message = "提问内容不能为空", groups = {ValidateGroup.AddGroup.class})
    private String question;

    @ApiModelProperty(value =  "答疑内容")
    @NotBlank(message = "答疑内容不能为空", groups = {ValidateGroup.UpdateGroup.class})
    private String content;

    @ApiModelProperty(value =  "提问供应商id")
//    @NotNull(message = "供应商id不能为空", groups = {ValidateGroup.AddGroup.class})
    private Long vendorId;

    @ApiModelProperty(value =  "提问供应商名称")
//    @NotBlank(message = "供应商id不能为空", groups = {ValidateGroup.AddGroup.class})
    private String vendorName;

}
