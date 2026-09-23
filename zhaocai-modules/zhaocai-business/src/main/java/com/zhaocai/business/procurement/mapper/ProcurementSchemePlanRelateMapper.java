package com.zhaocai.business.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.procurement.domain.ProcurementScheme;
import com.zhaocai.business.procurement.domain.ProcurementSchemePlanRelate;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 采购方案-采购计划对应关系Mapper接口
 *
 * @author WH
 * @date 2024-05-29
 */
public interface ProcurementSchemePlanRelateMapper extends BaseMapper<ProcurementSchemePlanRelate> {

    /**
     * 获取与采购计划关联的采购方案
     *
     * @param planId
     * @return
     */
    List<ProcurementScheme> selectProcurementSchemeByRelatePlanId(@Param("planId") Long planId);
}
