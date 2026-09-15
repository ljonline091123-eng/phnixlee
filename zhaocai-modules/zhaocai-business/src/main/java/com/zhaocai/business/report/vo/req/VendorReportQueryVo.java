package com.zhaocai.business.report.vo.req;

import com.zhaocai.business.common.annotations.DictCache;
import com.zhaocai.business.common.enums.DictBizEnum;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.sql.Date;

@Data
public class VendorReportQueryVo {

    /** 组织机构id */
    private String id;

    /** 组织机构id */
    private String deptId;

    /**
     * 数据权限范围单位id(第三方部门id)
     * 由后端根据当前登录用户所属单位强制赋值，前端不需要传；为空表示不限制(集团账号)
     */
    private String scopeId;

    /** 供应商分类 */
    private String expenditureBusinessType;

    /** 供应商id */
    private String vendorId;

    /** 甲方id */
    private String partyAOrgId;

    /** 项目编码 */
    private String minAccountCode;

    /** 归属项目部id */
    private String projectDepartmentId;

    /** 甲方名称 */
    private String partyAName;

    /** 合同金额（含税） */
    private BigDecimal contractAmount;

    /** 供应商所在省份 */
    private String enterpriseProvinceCode;

    /** 供应商所在市区 */
    private String enterpriseCityCode;

    /** 供应商所在地 */
    private String enterpriseLocation;

    /** 参与采购任务笔数 */
    private Integer tbiCount;

    /** 中标任务笔数 */
    private Integer tbrCount;

    /** 中标金额（含税） */
    private BigDecimal tbrAmount;

    /** 中标率 */
    private BigDecimal tbrRate;

    /** 投标时间/报名时间 */
    private Date applyTime;

    /** 区域 */
    private String area;

    /** 开始时间 */
    private String startDate;

    /** 结束时间 */
    private String endDate;

    /** 页数 */
    private int pageNum;

    /** 页面条数 */
    private int pageSize;

    @ApiModelProperty(value = "支出业务分类")
    @DictCache(dictBizEnum = DictBizEnum.PROCUREMENT_PLAN_TYPE,filedName = "expenditureBusinessType")
    private String expenditureBusinessTypeText;

}
