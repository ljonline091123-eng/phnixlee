package com.zhaocai.business.agreement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.agreement.domain.AgreementSignStamper;

/**
 * 合同签章签署位置Mapper接口
 *
 * @author chenming
 * @date 2024-09-14
 */
public interface AgreementSignStamperMapper extends BaseMapper<AgreementSignStamper> {

    /**
     * 根据 templateId 删除记录
     * @param templateId
     */
    void deleteByTemplateId(Long templateId);
}
