package com.zhaocai.business.vendor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.vendor.domain.VendorPerformanceEvaluation;

import java.util.List;

/**
 * 供应商履约评价Service接口
 *
 * @author WH
 * @date 2024-07-12
 */
public interface IVendorPerformanceEvaluationService  extends IService<VendorPerformanceEvaluation> {

    /**
     * 同步供应商履约评价记录<br/>
     * 全量数据同步
     */
    void syncVendorPerformanceEvaluationService();

    /**
     * 获取和同步指定供应商履约评价记录
     * @param vendorId
     * @return
     */
    List<VendorPerformanceEvaluation> getAndSyncVendorPerformanceList(Long vendorId);
}
