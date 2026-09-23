package com.zhaocai.business.report.service;

import com.zhaocai.business.report.vo.req.PurchaseLedgerQueryVo;
import com.zhaocai.business.report.vo.res.PurchaseLedgerListVo;
import com.zhaocai.business.report.vo.res.PurchaseLedgerSummaryVo;

import java.util.List;
import java.util.Map;

/**
 * 采购台账报表服务
 *
 * @author claude
 */
public interface IPurchaseLedgerService {

    /** 刷新物化表（sys_job 调用）：清空后从视图全量灌入 */
    Boolean handlePurchaseLedgerReport();

    /** 汇总视图聚合查询（组织 × 项目 × 需求类型） */
    List<PurchaseLedgerSummaryVo> getSummary(PurchaseLedgerQueryVo queryVo);

    /** 汇总视图导出 */
    Map<String, Object> summaryExport(PurchaseLedgerQueryVo queryVo);

    /** 明细宽表查询（分页） */
    Map<String, Object> getLedgerList(PurchaseLedgerQueryVo queryVo);

    /** 明细宽表导出（完整结果，不限分页） */
    Map<String, Object> ledgerExport(PurchaseLedgerQueryVo queryVo);
}
