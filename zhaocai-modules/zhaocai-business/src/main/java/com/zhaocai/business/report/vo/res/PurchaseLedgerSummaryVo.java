package com.zhaocai.business.report.vo.res;

import com.zhaocai.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 采购台账-汇总视图行（单位 → 部门 → 项目 → 采购需求类型 四级树，五类状态逐层向上汇总）
 *
 * 层级说明（2026-09-17 与业务确认）：
 *   U=单位（根，聚合全部部门）；D=部门（聚合该部门下所有项目）；P=项目（聚合该项目下所有需求类型）；
 *   T=采购需求类型（叶子，项目×需求类型，五类状态计数在叶子行）。同一个项目有多种需求类型时，
 *   在项目节点下显示多行并列数据。children 仅 U/D/P 节点有值，T 节点为叶子。
 *
 * @author claude
 */
@Data
public class PurchaseLedgerSummaryVo {

    /** 节点唯一 id（U/D 用 thrid_dept_id，P 用 orgId_projectCode，T 用 orgId_projectCode_demandType），供前端树 row-key */
    @ApiModelProperty(value = "节点唯一id")
    private String id;

    /** 节点类型：U=单位 D=部门 P=项目 T=采购需求类型 */
    @ApiModelProperty(value = "节点类型：U=单位 D=部门 P=项目 T=采购需求类型")
    private String type;

    /** 子节点（仅 U/D 有值） */
    @ApiModelProperty(value = "子节点")
    private List<PurchaseLedgerSummaryVo> children = new ArrayList<>();

    @ApiModelProperty(value = "组织机构id")
    private String orgId;

    @Excel(name = "组织机构")
    @ApiModelProperty(value = "组织机构名称")
    private String orgName;

    @Excel(name = "项目编号")
    @ApiModelProperty(value = "项目编号")
    private String projectCode;

    @Excel(name = "项目名称")
    @ApiModelProperty(value = "项目名称")
    private String projectName;

    @ApiModelProperty(value = "采购需求类型")
    private Integer demandType;

    @Excel(name = "采购需求类型")
    @ApiModelProperty(value = "采购需求类型名称")
    private String demandTypeText;

    @Excel(name = "待招采")
    @ApiModelProperty(value = "待招采")
    private Long pending = 0L;

    @Excel(name = "待开标")
    @ApiModelProperty(value = "待开标")
    private Long preopen = 0L;

    @Excel(name = "待定标")
    @ApiModelProperty(value = "待定标")
    private Long preaward = 0L;

    @Excel(name = "已完成")
    @ApiModelProperty(value = "已完成")
    private Long completed = 0L;

    @Excel(name = "异常/终止")
    @ApiModelProperty(value = "异常/终止")
    private Long exception = 0L;

}
