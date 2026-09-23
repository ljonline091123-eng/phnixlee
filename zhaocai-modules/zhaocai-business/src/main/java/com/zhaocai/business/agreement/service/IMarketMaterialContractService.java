package com.zhaocai.business.agreement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.agreement.domain.AgreementMaterialsList;
import com.zhaocai.business.agreement.domain.MarketMaterialContract;
import com.zhaocai.business.agreement.domain.MarketMaterialList;
import com.zhaocai.business.agreement.vo.req.MarketMaterialContractQueryVO;
import com.zhaocai.business.agreement.vo.res.AgreementCreateBaseInfoVO;
import com.zhaocai.business.agreement.vo.res.MarketMaterialContractListVO;
import com.zhaocai.business.manager.http.dto.req.MarketMaterialListQuoteRequestDTO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.common.core.bean.PageResult;

import java.util.List;

/**
 * 易料采购合同信息Service接口
 *
 * @author lsn
 * @date 2024-10-22
 */
public interface IMarketMaterialContractService extends IService<MarketMaterialContract> {

    /**
     * 接收采购清单最终报价
     *
     * @param requestDTO
     */
    void saveContract(MarketMaterialListQuoteRequestDTO requestDTO);

    /**
     * 获取可签订的易料采购合同
     *
     * @param queryVO
     * @return
     */
    PageResult<MarketMaterialContractListVO> listMarketMaterialContract(MarketMaterialContractQueryVO queryVO);

    /**
     * 获取创建合同的基本信息
     *
     * @param queryVO
     * @return
     */
    AgreementCreateBaseInfoVO getAgreementCreateInfo(MarketMaterialContractQueryVO queryVO);

    /**
     * 处理新增合同创建附件
     * @param attachmentVO
     * @return
     */
    long agreementCreateAttachmentHandle(AttachmentVO attachmentVO);

    /**
     * 校验创建合同基本信息
     * @param requestVO
     * @return
     */
    Boolean checkAgreementCreateInfo(MarketMaterialContractQueryVO requestVO);

    /**
     * 校验清单数据[新增]
     * @param list
     */
    void checkAgreementMaterials(List<MarketMaterialList> list);

    /**
     * 校验清单数据[修改]
     * @param materialsList
     * @param materialsListsOld
     */
    void checkAgreementMaterialsByUpdate(List<AgreementMaterialsList> materialsList, List<AgreementMaterialsList> materialsListsOld);
}
