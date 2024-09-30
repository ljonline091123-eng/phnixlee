package com.zhaocai.business.vendor.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.vendor.domain.VendorPerformanceEvaluation;
import org.apache.ibatis.annotations.Param;

/**
 * 供应商履约评价Mapper接口
 *
 * @author WH
 * @date 2024-07-12
 */
public interface VendorPerformanceEvaluationMapper extends BaseMapper<VendorPerformanceEvaluation> {

    /**
     * 根据供应商删除相关记录
     *
     * @param vendorId
     */
    Integer deleteByVendorId(@Param("vendorId") Long vendorId);
}
