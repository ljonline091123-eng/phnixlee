package com.zhaocai.business.bidding.vo.res;

import com.zhaocai.common.core.bean.ValidateGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/5/30 16:43
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TenderNoticeAnswerListVO", description = "招标公告答疑列表VO")
public class TenderNoticeAnswerListVO implements Serializable {
    private static final long serialVersionUID = -6971891261314993088L;

    @ApiModelProperty(value =  "主键id")
    private Long id;

    @ApiModelProperty(value =  "关联业务id")
    private Long busId;

    /** 业务类型（招标公告/招标内容） */
    @ApiModelProperty(value =  "业务类型")
    private Integer busType;

    @ApiModelProperty(value =  "招标公告状态（招标阶段流程状态）0废标11发布(公告)/12发布(报名情况)/1发布文件/2开标/3评标/4二次洽商/5定标报告/6中标公示/7结果发布/8完成")
    @NotNull(message = "招标公告状态不能为空", groups = {ValidateGroup.AddGroup.class})
    private Integer noticeStatus;

    /** 提问内容 */
    @ApiModelProperty(value =  "提问内容")
    private String question;

    /** 答疑内容 */
    @ApiModelProperty(value =  "答疑内容")
    private String content;

    /** 提问用户 */
    @ApiModelProperty(value =  "提问用户")
    private Long questionUser;

    /** 答疑用户 */
    @ApiModelProperty(value =  "答疑用户")
    private Long answerUser;

    @ApiModelProperty(value =  "提问供应商名称")
    private String vendorName;

}
