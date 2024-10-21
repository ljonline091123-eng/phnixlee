package com.zhaocai.business.procurement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.agreement.vo.req.AgreementSchemeQueryVO;
import com.zhaocai.business.agreement.vo.res.AgreementSchemeListVO;
import com.zhaocai.business.process.service.IProcessBusinessBaseService;
import com.zhaocai.business.procurement.domain.ProcurementScheme;
import com.zhaocai.business.procurement.vo.req.BiddingSchemeListQueryVO;
import com.zhaocai.business.procurement.vo.req.ProcurementSchemeListQueryVO;
import com.zhaocai.business.procurement.vo.req.ProcurementSchemeRequestVO;
import com.zhaocai.business.procurement.vo.res.*;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.web.bean.ResultData;

import java.util.List;

/**
 * 采购方案Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface IProcurementSchemeService  extends IService<ProcurementScheme>, IProcessBusinessBaseService {

    /**
     * 采购方案列表查询
     * @param queryVO
     * @return
     */
    PageResult<ProcurementSchemeListVO> listPage(ProcurementSchemeListQueryVO queryVO);

    /**
     * 招标管理列表查询
     * @param queryVO
     * @return
     */
    PageResult<BiddingSchemeListVO> biddingSchemeListPage(BiddingSchemeListQueryVO queryVO);

    /**
     * 保存采购方案
     * @param requestVO
     */
    Long saveProcurementScheme(ProcurementSchemeRequestVO requestVO);

    /**
     * 提交采购方案
     * @param id
     */
    void submitProcurementScheme(Long id,String detailUrl,String operateComment);

    /**
     * 获取采购方案详情
     * @param id
     * @return
     */
    ProcurementSchemeDetailVO detail(Long id);

    /**
     * 获取[采购方案]对应的[采购计划]下的所有[采购方案]列表。
     * @param id 采购方案id
     * @return
     */
    List<ProcurementSchemeVO> planSchemeDetail(Long id);
    /**
     * 获取采购方案的物料清单
     * @param id
     * @return
     */
    List<MaterialsVO> listMaterials(Long id);

    List<CompMaterialsVO> listCompMaterials(Long id);

    /**
     * 获取采购方案绑定的采购计划
     * @param id
     * @return
     */
    List<ProcurementPlanListVO> listProcurementPlanByScheme(Long id);

    /**
     * 获取采购方案新建数据
     * @param contractSplitIds
     * @return
     */
    ProcurementSchemeCreateVO getProcurementSchemeCreateInfo(List<Long> contractSplitIds);

    /**
     * 获取可签订合同的采购方案
     * @param queryVO
     * @return
     */
    PageResult<AgreementSchemeListVO> listSignAgreementScheme(AgreementSchemeQueryVO queryVO);

    /**
     * 获取采购方案的合同模板附件信息
     * @param id
     * @return
     */
    AttachmentVO getAgreementTemplateAttachmentInfo(Long id);

    /**
     * 通过采购方案id获取最小核算项目信息
     * @param id
     * @return
     */
    List<MinProjectDataVO> selectDataByScheme(Long id);

    /**
     * 作废采购方案
     * @param id
     */
    void cancellationProcurementScheme(Long id);

    /**
     * 撤回采购方案
     * @param id
     */
    void revokeProcurementScheme(Long id);
}
