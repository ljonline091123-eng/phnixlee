package com.zhaocai.business.agreement.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.agreement.domain.AgreementMaterialsList;
import com.zhaocai.business.agreement.vo.res.AgreementMaterialsListVO;
import com.zhaocai.business.agreement.vo.res.AgreementUnderlingMaterialsVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 合同物料清单Mapper接口
 *
 * @author chenming
 * @date 2024-06-19
 */
public interface AgreementMaterialsListMapper extends BaseMapper<AgreementMaterialsList> {

    /**
     * 获取合同清单列表
     *
     * @param agreementId
     * @return
     */
    List<AgreementMaterialsListVO> selectAgreementMaterialsList(@Param("agreementId") Long agreementId);

    /**
     * 获取底层逻辑平台物料清单
     *
     * @param agreementId
     * @return
     */
    List<AgreementUnderlingMaterialsVO> selectUnderlingMaterialsList(@Param("agreementId") Long agreementId);

    /**
     * 根据合同id删除
     *
     * @param agreementId
     */
    void deleteByAgreementId(@Param("agreementId") Long agreementId);

    /**
     * 根据合约拆分 id 获取对应的合同清单列表
     *
     * @param contractSplitId
     * @return
     */
    List<AgreementMaterialsList> selectMaterialsByContractSplitId(@Param("contractSplitId") Long contractSplitId);

    /**
     * 获取合同清单（易料合同）
     * @param id
     * @return
     */
    List<AgreementMaterialsListVO> listAgreementMaterialsByMarket(Long id);
}
