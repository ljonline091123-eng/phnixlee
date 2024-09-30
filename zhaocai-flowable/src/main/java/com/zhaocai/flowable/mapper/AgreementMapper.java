package com.zhaocai.flowable.mapper;

import org.apache.ibatis.annotations.Param;

public interface AgreementMapper {

    void updateAgreementStatus(@Param("agreementId") Long agreementId);

}
