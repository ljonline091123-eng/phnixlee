package com.zhaocai.flowable.mapper;

import org.apache.ibatis.annotations.Param;

public interface ProcumentMapper {

    void updateProcurementPlanStatus(@Param("planId") Long planId);
}
