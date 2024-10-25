package com.zhaocai.business.bidding.vo.res;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;


@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@ApiModel(value = "VendorPortalMsgListVO", description = "消息列表数据VO")
public class VendorPortalMsgListVO<T> extends AdviceObject {



    @ApiModelProperty(value =  "标题")
    private String title;

    @ApiModelProperty(value =  "数据")
    private T data;

    @ApiModelProperty(value =  "消息类别")
    private Integer msgType;

    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PLAN_TYPE,filedName = "msgType")
    @ApiModelProperty(value = "消息类别-文本 ")
    private String msgTypeText;

}
