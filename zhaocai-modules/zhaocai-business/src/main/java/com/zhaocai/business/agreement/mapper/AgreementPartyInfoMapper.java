package com.zhaocai.business.agreement.mapper;

import java.util.List;
import com.zhaocai.business.agreement.domain.AgreementPartyInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 合同签约方信息Mapper接口
 *
 * @author zs
 * @date 2024-12-27
 */
public interface AgreementPartyInfoMapper extends BaseMapper<AgreementPartyInfo>{

    /**
     * 根据合同id删除合同签约方信息
     * @param agreementId
     */
    void deleteByAgreementId(@Param("agreementId") Long agreementId);
}
