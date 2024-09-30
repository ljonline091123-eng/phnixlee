package com.zhaocai.common.signature.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.common.signature.domain.AgreementSignature;
import org.apache.ibatis.annotations.Delete;

/**
 * 合同签章信息Mapper接口
 *
 * @author chenming
 * @date 2024-08-28
 */
public interface AgreementSignatureMapper extends BaseMapper<AgreementSignature> {

    /**
     * 根据合同签章信息删除相关信息
     * @param signId
     */
    @Delete("delete from tb_agreement_signature where sign_id = #{signId} ")
    void deleteBySignId(Long signId);
}
