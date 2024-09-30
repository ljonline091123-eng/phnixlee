package com.zhaocai.business.agreement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.agreement.domain.AgreementPaymentList;
import org.apache.ibatis.annotations.Param;

/**
 * 合同结算与付款节点信息Mapper接口
 *
 * @author chenming
 * @date 2024-05-24
 */
public interface AgreementPaymentListMapper extends BaseMapper<AgreementPaymentList> {

    /**
     * 根据合同id删除合同结算与付款
     * @param agreementId
     */
    void deleteByAgreementId(@Param("agreementId") Long agreementId);
}
