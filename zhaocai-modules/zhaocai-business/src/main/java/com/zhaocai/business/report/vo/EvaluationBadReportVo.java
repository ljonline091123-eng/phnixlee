package com.zhaocai.business.report.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.zhaocai.common.core.annotation.Excel;
import com.zhaocai.common.core.bean.PageRecive;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 问题报表-tab2 供应商评价不合格记录查询对象
 *
 * 与成控(prod)的差异：
 * 1. 数据源由 tb_vendor_evaluation_report(平台同步的支出合同结算评价) 改为本地供应商评价表 tb_vendor_evaluate；
 * 2. 判定条件由"六个评分项中存在差评"改为 is_qualified = 'N'(不合格) + evaluate_status = '1'(已确认)；
 * 3. 去掉"评价项目""评价合同"两个字段，新增"评价周期"。
 *
 * @author claude
 */
@Data
public class EvaluationBadReportVo extends PageRecive {

    /** 评价记录id */
    private String id;

    /** 供应商名称 */
    @Excel(name = "供应商名称")
    private String vendorName;

    /** 评价类型(字典 evaluate_type) */
    private String evaluateType;

    /** 评价类型名称 */
    @Excel(name = "评价类型")
    private String typeName;

    /** 评价分数(tb_vendor_evaluate.evaluate_fraction) */
    @Excel(name = "评价分数")
    private BigDecimal evaluateFraction;

    /** 评分周期 */
    private Date evaluateTime;

    /** 评分周期文字(季度时存字典 evaluate_time 的 value) */
    private String evaluateTimeTxt;

    /** 评价周期 */
    @Excel(name = "评价周期")
    private String evaluateTimeTxtName;

    /** 评价人员 */
    @Excel(name = "评价人员")
    private String createBy;

    /** 评价时间 */
    @Excel(name = "评价时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    /** 页数 */
    private int pageNum;

    /** 页面条数 */
    private int pageSize;
}
