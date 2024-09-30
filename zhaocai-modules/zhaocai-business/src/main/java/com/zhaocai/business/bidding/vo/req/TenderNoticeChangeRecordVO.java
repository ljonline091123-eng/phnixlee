package com.zhaocai.business.bidding.vo.req;

import com.zhaocai.common.core.bean.ValidateGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * @author ssy
 * @date 2024/5/31 10:03
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "TenderNoticeChangeRecordVO", description = "招标公告变更记录VO")
public class TenderNoticeChangeRecordVO implements Serializable {
    private static final long serialVersionUID = 4313216303078906763L;

    @ApiModelProperty(value =  "招标公告id")
    @NotNull(message = "招标公告id不能为空", groups = {ValidateGroup.AddGroup.class})
    private Long noticeId;

    @ApiModelProperty(value =  "变更类型")
    @NotNull(message = "变更类型不能为空", groups = {ValidateGroup.AddGroup.class})
    private Integer type;

    @ApiModelProperty(value =  "变更前信息")
    private String updateBefore;

    @ApiModelProperty(value =  "变更后信息")
    private String updateAfter;

}
