package com.zhaocai.business.report.vo.res;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.base.AdviceObject;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.common.core.annotation.Excel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Date;

@Data
public class VendorReportListVo extends AdviceObject {

    /** 组织机构id */
    private String id;

    /** 组织机构id */
    private String deptId;

    /** 供应商分类 */
    private String expenditureBusinessType;

    @ApiModelProperty(value = "支出业务分类")
    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PLAN_TYPE,filedName = "expenditureBusinessType")
    @Excel(name = "供应商分类")
    private String expenditureBusinessTypeText;

    /** 供应商所在地 */
    @Excel(name = "供应商所在地")
    private String enterpriseLocation;

    /** 供应商id */
    private String vendorId;

    /** 供应商名称 */
    @Excel(name = "供应商名称")
    private String vendorName;

    /** 甲方id */
    private String partyAOrgId;

    /** 项目编码 */
    private String minAccountCode;

    /** 归属项目部id */
    private String projectDepartmentId;

    /** 甲方名称 */
    @Excel(name = "组织机构名称")
    private String partyAName;

    /** 项目评分汇总(供应商评价确认状态的合格/不合格次数文本，格式：合格n次，不合格m次) */
    @Excel(name = "项目评分汇总")
    private String evaluationTypeName;

    /** 合格次数(供应商评价确认状态，列表只展示上面的文本，此字段用于定时任务刷数据) */
    private Integer qualifiedCount;

    /** 不合格次数(供应商评价确认状态，列表只展示上面的文本，此字段用于定时任务刷数据) */
    private Integer unqualifiedCount;

    /** 供应商所在省份 */
    private String enterpriseProvinceCode;

    /** 供应商所在市区 */
    private String enterpriseCityCode;

    /** 参与采购任务笔数 */
    @Excel(name = "参与采购任务笔数")
    private Integer tbiCount;

    /** 中标任务笔数 */
    @Excel(name = "中标任务笔数")
    private Integer tbrCount;

    /** 中标率 */
    @Excel(name = "中标率")
    private BigDecimal tbrRate;

    /** 中标金额（含税） */
    @Excel(name = "中标金额（含税元）")
    private BigDecimal tbrAmount;

    /** 合同金额（含税） */
    @Excel(name = "合同金额（含税元）")
    private BigDecimal contractAmount;

    /** 投标时间/报名时间 */
    private Date applyTime;

    /** 区域 */
    private String area;

    /** 开始时间 */
    private String startTime;

    /** 结束时间 */
    private String endTime;
}
