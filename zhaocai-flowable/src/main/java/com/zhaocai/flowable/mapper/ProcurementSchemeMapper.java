package com.zhaocai.flowable.mapper;

import org.apache.ibatis.annotations.Param;

public interface ProcurementSchemeMapper {

    void updateProcurementSchemeState(@Param("procurementSchemeCode") String procurementSchemeCode);

}
