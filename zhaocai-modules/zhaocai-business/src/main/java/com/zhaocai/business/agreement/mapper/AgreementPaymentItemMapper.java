package com.zhaocai.business.agreement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.agreement.domain.AgreementPaymentItem;
import org.apache.ibatis.annotations.Param;

/**
 * 合同款项信息Mapper接口
 *
 * @author chenming
 * @date 2024-05-24
 */
public interface AgreementPaymentItemMapper extends BaseMapper<AgreementPaymentItem> {

    /**
     * 删除合同款项数据
     *
     * @param agreementId
     */
    void deleteByAgreementId(@Param("agreementId") Long agreementId);
}
