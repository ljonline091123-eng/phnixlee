package com.zhaocai.business.agreement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.agreement.domain.AgreementMaterialsList;
import com.zhaocai.business.agreement.vo.res.AgreementMaterialsListVO;
import com.zhaocai.business.agreement.vo.res.AgreementUnderlingMaterialsVO;
import com.zhaocai.business.procurement.domain.MaterialsList;

import java.util.List;

/**
 * 合同物料清单Service接口
 *
 * @author chenming
 * @date 2024-06-19
 */
public interface IAgreementMaterialsListService  extends IService<AgreementMaterialsList> {

    /**
     * 保存合同清单
     * @param agreementMaterialsLists
     * @param agreementId
     */
    void saveAgreementMaterialsList(List<AgreementMaterialsList> agreementMaterialsLists, Long agreementId);

    /**
     * 获取合同清单
     * @param agreementId
     * @return
     */
    List<AgreementMaterialsListVO> listAgreementMaterials(Long agreementId);

    /**
     * 获取底层逻辑平台物料清单
     * @param agreementId
     * @return
     */
    List<AgreementUnderlingMaterialsVO> listUnderlingMaterials(Long agreementId);

    /**
     * 修改合同清单
     * @param agreementMaterialsLists
     * @param agreementId
     */
    void updateAgreementMaterialsList(List<AgreementMaterialsList> agreementMaterialsLists, Long agreementId);

    /**
     * 根据合约拆分 id 获取对应的合同清单列表
     * @param contractSplitId
     * @return
     */
    List<AgreementMaterialsList> listByContractSplitId(Long contractSplitId);

    /**
     * 获取合同清单数据
     * @param agreementId
     * @return
     */
    List<AgreementMaterialsList> listByAgreementId(Long agreementId);

    /**
     * 获取合同清单（易料合同）
     * @param id
     * @return
     */
    List<AgreementMaterialsListVO> listAgreementMaterialsByMarket(Long id);
}
