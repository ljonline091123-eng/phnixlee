package com.zhaocai.business.agreement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.agreement.domain.Agreement;
import com.zhaocai.business.agreement.vo.req.*;
import com.zhaocai.business.agreement.vo.res.*;
import com.zhaocai.business.process.service.IProcessBusinessBaseService;
import com.zhaocai.business.vendor.vo.req.VendorAgreementListQueryVO;
import com.zhaocai.business.vendor.vo.res.DownloadAgreementVO;
import com.zhaocai.business.vendor.vo.res.VendorAgreementDetailVO;
import com.zhaocai.business.vendor.vo.res.VendorAgreementListVO;
import com.zhaocai.common.core.bean.PageResult;

/**
 * 合同基本信息Service接口
 *
 * @author chenming
 * @date 2024-05-24
 */
public interface IAgreementService  extends IService<Agreement> , IProcessBusinessBaseService {

    /**
     * 合同列表查询
     * @param queryVO
     * @return
     */
    PageResult<AgreementListVO> listPage(AgreementListQueryVO queryVO);

    /**
     *
     * @param requestVO
     * @return
     */
    Boolean checkAgreementCreateInfo(AgreementCreateInfoRequestVO requestVO);

    /**
     * 获取创建合同时的基本信息
     * @param requestVO
     * @return
     */
    AgreementCreateBaseInfoVO getAgreementCreateInfo(AgreementCreateInfoRequestVO requestVO);

    /**
     * 获取供应商的签订合同
     * @param queryVO
     * @return
     */
    PageResult<VendorAgreementListVO> listVendorAgreement(VendorAgreementListQueryVO queryVO);

    /**
     * 保存合同
     * @param requestVO
     */
    AgreementSaveVO saveAgreement(AgreementSaveRequestVO requestVO);

    /**
     * 合同详情
     * @param id
     * @return
     */
    AgreementDetailVO detail(Long id);

    /**
     * 获取供应商合同详情
     * @param id
     * @return
     */
    VendorAgreementDetailVO getVendorAgreementDetail(Long id);

    /**
     * 获取合同文件 InputStream
     * @param id
     * @return
     */
    DownloadAgreementVO getAgreementFileInputStream(Long id);

    /**
     * 获取合同列表
     * @param queryVO
     * @return
     */
    PageResult<AgreementJkpthtListVO> listAgreementJkpthtPage(AgreementJkpthtListQueryVO queryVO);

    /**
     * 获取底层逻辑
     * @param id
     * @return
     */
    AgreementUnderlingDetailVO getAgreementUnderlingDetail(Long id);

    /**
     * 获取合同的附件id
     * @param id
     * @return
     */
    AgreementFileVO getAgreementAttachmentId(Long id);

    /**
     * 作废合同
     * @param id
     */
    void cancellationAgreement(Long id);

    /**
     * 提交合同
     * @param id
     */
    void submitAgreement(Long id,String detailUrl);

    /**
     * 设置合同文件标签完成
     * @param id
     * @param attachmentId
     */
    void setAgreementFileLabelFinish(Long id, long attachmentId);

    /**
     * 校验合同是否可修改
     * @param id
     * @return
     */
    Boolean checkAgreementUpdate(Long id);

    /**
     * 合同设置完水印
     * @param id
     * @param attachmentId
     */
    void agreementWatermarkFinish(Long id, long attachmentId);

    /**
     * 获取合同标签 id
     * @param id
     * @return
     */
    Long getLabelAttachmentId(Long id);

    /**
     * 撤回合同
     * @param id
     */
    void revokeAgreement(Long id);

    /**
     * 获取合同选定的采购相关信息
     * @param schemeId
     * @param contractSplitId
     * @return
     */
    AgreementSelectedProcurementInfoVO getAgreementSelectedProcurementInfo(Long schemeId, Long contractSplitId);

    /**
     * 推送合同至供应商
     * @param id
     */
    void pushAgreementToVendor(Long id);

    /**
     * 供应商确认合同
     * @param id
     */
    void vendorAffirmAgreement(Long id);

    /**
     * 转换为 pdf 完成
     * @param id
     * @param attachmentId
     */
    void coverToPdfFinish(Long id, long attachmentId);

    /**
     * 校验合同供应商确认是否成功
     * @param id
     * @return
     */
    Boolean checkAgreementAffirmState(Long id);

    /**
     * 推送合同至电子签章平台
     * @param pushAgreementToSign
     */
    void pushAgreementToSignPlatform(PushAgreementToSignVO pushAgreementToSign);

    /**
     * 供应商签订合同
     * @param id
     * @return
     */
    String vendorSignAgreement(Long id);

    /**
     * 签订合同
     * @param id
     * @return
     */
    String signAgreement(Long id);

    /**
     * 作废已签署合同工
     * @param requestVO
     */
    void cancelledSignAgreement(CancelledSignAgreementRequestVO requestVO);

    /**
     * 易料合同免审提交
     * @param id
     * @return
     */
    boolean avoidSubmitByMarket(Long id);
}
