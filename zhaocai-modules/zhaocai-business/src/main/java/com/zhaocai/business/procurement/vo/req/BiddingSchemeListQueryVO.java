package com.zhaocai.business.procurement.vo.req;

import com.baomidou.mybatisplus.annotation.TableField;
import com.zhaocai.common.core.bean.PageRecive;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 * @author ssy
 * @date 2024/6/6 19:08
 */
@Data
@ApiModel(value = "列表查询参数")
public class BiddingSchemeListQueryVO extends PageRecive {
    private static final long serialVersionUID = 1188362470189395967L;

    @ApiModelProperty(value =  "采购方案编号")
    private String procurementSchemeCode;

    @ApiModelProperty(value =  "采购方案名称")
    private String procurementSchemeName;

    @ApiModelProperty(value = "采购分类")
    private Integer procurementPlanType;

    @ApiModelProperty(value =  "采购经办人")
    private Long procurementOfficer;

    @ApiModelProperty(value = "采购经办人名称")
    private String procurementOfficerName;

    @ApiModelProperty(value = "采购方式（公开招标|邀请招标|询价采购|单一来源）")
    private Integer procurementType;

    /**
     * 是否为领导，领导可看到所有采购方案
     */
    @ApiModelProperty(hidden = true)
    private Integer isLeader;

    @ApiModelProperty(value = "状态")
    private Integer state;

    @ApiModelProperty(value =  "招标公告状态（招标阶段流程状态）")
    private Integer noticeStatus;

    @ApiModelProperty(value = "项目编号")
    private String projectCode;

    @ApiModelProperty(value = "财务确认人员id")
    private String financeConfirmId;

    /** 类型 */
    @TableField(exist = false)
    private String type;

    /** 项目编号 */
    @TableField(exist = false)
    private List<String> projectCodeList;
}
