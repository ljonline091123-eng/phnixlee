package com.zhaocai.business.agreement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.agreement.domain.AgreementDailyWage;
import org.apache.ibatis.annotations.Param;

/**
 * 合同-计日工Mapper接口
 *
 * @author chenming
 * @date 2024-06-26
 */
public interface AgreementDailyWageMapper extends BaseMapper<AgreementDailyWage> {

    /**
     * 根据合同删除
     *
     * @param agreementId
     */
    void deleteByAgreementId(@Param("agreementId") Long agreementId);
}
