package com.zhaocai.business.bidding.vo.res;

import com.zhaocai.business.common.base.AdviceObject;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@ApiModel(value = "VendorPortalMsgListVO", description = "消息列表数据VO")
public class VendorPortalMsgListVO<T> extends AdviceObject {

    /** 消息标题 */
    @ApiModelProperty(value = "标题")
    private String title;

    /** 需要的数据对象 */
    @ApiModelProperty(value = "数据")
    private T data;

    /**
     * 类型枚举值
     *  {@link com.zhaocai.business.bidding.enums.VendorMsgStatusEnum}
     */
    @ApiModelProperty(value = "消息类别")
    private Integer msgType;

    /**
     * 转换字典值。
     * {@link com.zhaocai.business.common.cache.DictBizCache#getValue (字典类型, 值) }
     */
    @ApiModelProperty(value = "消息类别-文本 ")
    private String msgTypeText;

}
