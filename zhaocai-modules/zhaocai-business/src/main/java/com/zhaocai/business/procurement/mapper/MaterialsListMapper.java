package com.zhaocai.business.procurement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.procurement.domain.MaterialsList;
import com.zhaocai.business.procurement.dto.MaterialsListDTO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 采购物料清单Mapper接口
 * 
 * @author WH
 * @date 2024-05-24
 */
public interface MaterialsListMapper extends BaseMapper<MaterialsList> {

    /**
     * 删除物料清单
     * @param planId
     */
    void deleteByPlanId(@Param("planId") Long planId);

    /**
     * 根据采购计划物料清单
     *
     * @param planId
     * @return
     */
    List<MaterialsListDTO> selectMaterialsListByPlanId(@Param("planId") Long planId);
}
