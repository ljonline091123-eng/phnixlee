package com.zhaocai.business.agreement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.*;
import com.zhaocai.business.agreement.dto.AgreementMaterialsInfoDTO;
import com.zhaocai.business.agreement.mapper.AgreementMapper;
import com.zhaocai.business.agreement.service.*;
import com.zhaocai.business.agreement.vo.req.*;
import com.zhaocai.business.agreement.vo.res.*;
import com.zhaocai.business.bidding.domain.BiddingListQuotation;
import com.zhaocai.business.bidding.domain.BiddingOpenPeople;
import com.zhaocai.business.bidding.domain.TenderNotice;
import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import com.zhaocai.business.bidding.service.IBiddingListQuotationService;
import com.zhaocai.business.bidding.service.IBiddingResultService;
import com.zhaocai.business.bidding.vo.res.VendorBiddingListQuotationListVO;
import com.zhaocai.business.bidding.vo.res.VendorBiddingListQuotationVO;
import com.zhaocai.business.common.cache.DictBizCache;
import com.zhaocai.business.common.conver.ProcurementPlanTypeConver;
import com.zhaocai.business.common.enums.*;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.exception.ResultCode;
import com.zhaocai.business.common.interceptor.RequestParamLoggingInterceptor;
import com.zhaocai.business.common.utils.AmountCalUtil;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.filez.service.IFileZTaskService;
import com.zhaocai.business.manager.http.dto.req.*;
import com.zhaocai.business.manager.http.dto.res.*;
import com.zhaocai.business.manager.http.service.ContractPlanService;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.process.service.IBPMProcessService;
import com.zhaocai.business.process.service.IPBMOverrideService;
import com.zhaocai.business.procurement.domain.*;
import com.zhaocai.business.procurement.service.*;
import com.zhaocai.business.procurement.vo.res.ProcurementSchemeBiddingVendorVO;
import com.zhaocai.business.pub.domain.AreaDivision;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.service.*;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.domain.VendorContact;
import com.zhaocai.business.vendor.service.IVendorContactService;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.business.vendor.vo.req.VendorAgreementListQueryVO;
import com.zhaocai.business.vendor.vo.res.DownloadAgreementVO;
import com.zhaocai.business.vendor.vo.res.VendorAgreementDetailVO;
import com.zhaocai.business.vendor.vo.res.VendorAgreementListVO;
import com.zhaocai.business.vendor.vo.res.VendorAgreementVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.core.web.domain.BaseEntity;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.common.signature.common.enums.SignatureTypeEnum;
import com.zhaocai.common.signature.dto.SignatureContact;
import com.zhaocai.common.signature.dto.SignatureCreator;
import com.zhaocai.common.signature.dto.sign.SignatureResponse;
import com.zhaocai.common.signature.dto.SignatureStamper;
import com.zhaocai.common.signature.dto.command.*;
import com.zhaocai.common.signature.service.SignatureCommandFactory;
import com.zhaocai.common.signature.service.command.CancelAgreementCommand;
import com.zhaocai.common.signature.service.command.CreateAgreementDocumentCommand;
import com.zhaocai.common.signature.service.command.DownloadDocumentCommand;
import com.zhaocai.common.signature.service.command.GetSignUrlCommand;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.system.RemoteSystemService;
import net.qiyuesuo.v3sdk.model.contract.response.ContractSignurlV3Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 合同基本信息Service业务层处理
 *
 * @author chenming
 * @date 2024-05-24
 */
@Service
public class AgreementServiceImpl extends ServiceImpl<AgreementMapper,Agreement> implements IAgreementService {

    @Autowired
    private IProcurementSchemeService procurementSchemeService;

    @Autowired
    private IVendorService vendorService;

    @Autowired
    private IBiddingListQuotationService biddingListQuotationService;

    @Autowired
    private IAgreementDepositService agreementDepositService;

    @Autowired
    private IAgreementMaterialsListService agreementMaterialsListService;

    @Autowired
    private IAgreementPaymentItemService agreementPaymentItemService;

    @Autowired
    private IAgreementPaymentListService agreementPaymentListService;

    @Autowired
    private IAgreementDailyWageService agreementDailyWageService;

    @Autowired
    private IAgreementEquipmentSupplyService agreementEquipmentSupplyService;

    @Autowired
    private IAgreementMachineShiftService agreementMachineShiftService;

    @Autowired
    private IAgreementMaterialSupplyService agreementMaterialSupplyService;

    @Autowired
    private IAttachmentService attachmentService;

    @Autowired
    private IBusinessCodeService businessCodeService;

    @Autowired
    private IContractPlanningService contractPlanningService;

    @Autowired
    private ContractPlanService contractPlanService;

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Autowired
    private ISystemUserService systemUserService;

    @Autowired
    private IAreaDivisionService areaDivisionService;

    @Autowired
    private IAgreementFileService agreementFileZService;

    @Autowired
    private IFileZTaskService fileZTaskService;

    @Autowired
    private IBPMProcessService processService;

    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Autowired
    private IMaterialsListService materialsListService;

    @Autowired
    private IBiddingResultService biddingResultService;

    @Autowired
    private IContractPlanningSplitService contractPlanningSplitService;

    @Autowired
    private IVendorContactService vendorContactService;

    @Autowired
    private ISysFileService sysFileService;

    @Autowired
    private IProcurementSchemeBiddingService procurementSchemeBiddingService;

    @Autowired
    private IAgreementSignStamperService agreementSignStamperService;
    @Autowired
    private IMinProjectService minProjectService;

    @Autowired
    private IMarketMaterialContractService marketMaterialContractService;

    @Lazy
    @Autowired
    private IProcurementPlanService procurementPlanService;


    @Override
    public PageResult<AgreementListVO> listPage(AgreementListQueryVO queryVO) {
        IPage<AgreementListVO> ipage = baseMapper.selectPageList(queryVO.toMybatisPage(),queryVO);
        for (AgreementListVO agreement : ipage.getRecords()) {
            agreement.setIsOperate(getAgreementIsOperate(agreement.getAgreementState(),agreement.getCreateById(),agreement.getSignatureUserId()));
            if (null != agreement.getMarketMaterialContractId()) {
                agreement.setProcurementTypeText("易料采购");
            }
        }
        return new PageResult<>(ipage);
    }

    @Override
    public Boolean checkAgreementCreateInfo(AgreementCreateInfoRequestVO requestVO) {
        // 采购方案
        ProcurementScheme scheme = procurementSchemeService.getById(requestVO.getSchemeId());
        ValidateUtils.isNullException(scheme,"该采购方案不存在，请确认");

        // 合约规划
        ContractPlanning contractPlanning = contractPlanningService.getByContractSplitId(requestVO.getSplitId());
        ValidateUtils.isNullException(contractPlanning,"该采购方案对应的合约规划不存在，请确认");

        // 供应商
        Vendor biddingVendor = vendorService.getById(requestVO.getVendorId());
        ValidateUtils.isNullException(biddingVendor,"该采购方案的中标供应商不存在，请确认");

        if (StringUtils.isBlank(contractPlanning.getProjectName())) {
            throw new ParamValidateException("合同对应的归属最小核算项目为空，请确认");
        }

        // 校验最小核算项目相关数据
        MinProjectDetailResponseDTO projectDetail = contractPlanService.getMinProjectDetail(contractPlanning.getProjectCode());
        if (projectDetail == null) {
            throw new ParamValidateException("获取最小核算项目失败");
        }
        if (StringUtils.isBlank(projectDetail.getBelongingOrgId())) {
            throw new ParamValidateException("最小核算项目所属的归属本级组织id为空，请确认");
        }
        if (StringUtils.isBlank(getDeptName(projectDetail.getBelongingOrgId()))) {
            throw new ParamValidateException("最小核算项目所属的归属本级组织为空，请确认");
        }
        if (StringUtils.isBlank(projectDetail.getPrjAddrInfo())) {
            throw new ParamValidateException("最小核算项目的合同履行地为空");
        }
        if (StringUtils.isBlank(projectDetail.getPrjAddr())) {
            throw new ParamValidateException("最小核算项目的行政区划为空");
        }
        if (StringUtils.isBlank(getAgreementPerformDistrict(projectDetail.getPrjAddr()))) {
            throw new ParamValidateException("获取最小核算项目的行政区划名称为空");
        }
        if (StringUtils.isBlank(projectDetail.getManagementOrgId())) {
            throw new ParamValidateException("最小核算项目的归属管理组织id为空");
        }
        if (StringUtils.isBlank(getDeptName(projectDetail.getManagementOrgId()))) {
            throw new ParamValidateException("获取最小核算项目的归属管理组织名称为空");
        }

        // 校验清单数量
        checkAgreementMaterials(requestVO.getAgreementMaterialsList(),requestVO.getSchemeId(),requestVO.getSplitId(),requestVO.getVendorId());

        return true;
    }

    @Override
    public AgreementCreateBaseInfoVO getAgreementCreateInfo(AgreementCreateInfoRequestVO requestVO) {
        // 采购方案
        ProcurementScheme scheme = procurementSchemeService.getById(requestVO.getSchemeId());
        ValidateUtils.isNullException(scheme,"该采购方案不存在");

        // 合约规划
        ContractPlanning contractPlanning = contractPlanningService.getByContractSplitId(requestVO.getSplitId());
        ValidateUtils.isNullException(contractPlanning,"该采购方案对应的合约规划不存在");

        AgreementCreateBaseInfoVO baseInfoVO = new AgreementCreateBaseInfoVO();
        baseInfoVO.setSchemeId(requestVO.getSchemeId());
        baseInfoVO.setSplitId(requestVO.getSplitId());
        baseInfoVO.setVendorId(requestVO.getVendorId());
        baseInfoVO.setBelongAccountingItem(contractPlanning.getProjectName());
        baseInfoVO.setBelongAccountingItemCode(contractPlanning.getProjectCode());
        baseInfoVO.setBusinessType(contractPlanning.getContractPlanningCategory());
        baseInfoVO.setExpenditureBusinessType(contractPlanning.getContractPlanningCategoryName());
        baseInfoVO.setPriceType(scheme.getPriceType());

        // 查询项目详情
        MinProjectDetailResponseDTO projectDetail = contractPlanService.getMinProjectDetail(contractPlanning.getProjectCode());
        baseInfoVO.setBelongOrganizationId(projectDetail.getBelongingOrgId());
        baseInfoVO.setBelongOrganizationName(getDeptName(projectDetail.getBelongingOrgId()));

        baseInfoVO.setAgreementPerformAddress(projectDetail.getPrjAddrInfo());
        baseInfoVO.setAgreementPerformCountry("中国");
        baseInfoVO.setAgreementPerformDistrict(getAgreementPerformDistrict(projectDetail.getPrjAddr()));
        baseInfoVO.setPartyAOrgId(projectDetail.getManagementOrgId());
        baseInfoVO.setPartyAName(getDeptName(projectDetail.getManagementOrgId()));

        // 供应商
        Vendor biddingVendor = vendorService.getById(requestVO.getVendorId());
        ValidateUtils.isNullException(biddingVendor,"未获取到该采购方案的中标供应商");
        baseInfoVO.setPartyBName(biddingVendor.getEnterpriseName());
        baseInfoVO.setPartyBLegalName(biddingVendor.getLegalRepresentative());
        baseInfoVO.setPartyBLegalPhone(biddingVendor.getLegalPhone());
        baseInfoVO.setPartyBLegalIdCard(biddingVendor.getLegalIdCard());

        // 物料清单
        VendorBiddingListQuotationVO listQuotation = biddingListQuotationService.getVendorBiddingListQuotation(requestVO.getSchemeId(),requestVO.getSplitId(),requestVO.getVendorId());

        // 购买材料，设置价款类型、交易标的物类型
        if (ProcurementPlanTypeEnum.PURCHASE_MATERIALS.equalsType(scheme.getProcurementType())) {
            for (VendorBiddingListQuotationListVO listVO : listQuotation.getVendorBiddingListQuotationList()) {
                listVO.setPaymentType(scheme.getPriceType() == null ? "" : scheme.getPriceType().toString());
            }
            baseInfoVO.setSubjectMatterType(scheme.getSubjectMatterType());
        }

        // 处理清单数据
        BigDecimal totalAmountIncTax = BigDecimal.ZERO;
        BigDecimal totalAmountExcTax = BigDecimal.ZERO;
        Map<Long,AgreementMaterialsRequestVO> materialsRequestMap = requestVO.getAgreementMaterialsList().stream()
                .collect(Collectors.toMap(AgreementMaterialsRequestVO::getMaterialsListId,val -> val));
        for (VendorBiddingListQuotationListVO quotationList : listQuotation.getVendorBiddingListQuotationList()) {
            AgreementMaterialsRequestVO materialsRequestVO = materialsRequestMap.get(quotationList.getMaterialsListId());
            if (materialsRequestVO == null) {
                throw new ParamValidateException(String.format("提交的清单中少了清单:%s,请确认",quotationList.getMaterialsName()));
            }
            quotationList.setBrand(contractPlanning.getBrand());
            quotationList.setSignCount(materialsRequestVO.getSignCount());
            quotationList.setSignTaxRate(quotationList.getTaxRate());
            quotationList.setSignUnitPriceInclTax(materialsRequestVO.getSignUnitPriceInclTax());
            quotationList.setSignUnitPriceExclTax(AmountCalUtil.calUnitPriceExclTax(materialsRequestVO.getSignUnitPriceInclTax(),quotationList.getTaxRate()));
            quotationList.setSignAmountInclTax(AmountCalUtil.calTotalAmountInclTax(materialsRequestVO.getSignCount(),materialsRequestVO.getSignUnitPriceInclTax()));
            quotationList.setSignAmountExclTax(AmountCalUtil.calTotalAmountExclTax(quotationList.getSignAmountInclTax(),quotationList.getTaxRate()));

            totalAmountIncTax = NumberUtil.add(totalAmountIncTax,quotationList.getSignAmountInclTax());
            totalAmountExcTax = NumberUtil.add(totalAmountExcTax,quotationList.getSignAmountExclTax());
        }

        baseInfoVO.setBiddingListQuotation(listQuotation.getVendorBiddingListQuotationList());
        baseInfoVO.setTotalAmountIncTax(totalAmountIncTax.setScale(2, RoundingMode.DOWN));
        baseInfoVO.setTotalAmountExcTax(totalAmountExcTax.setScale(2, RoundingMode.DOWN));
        baseInfoVO.setSubjectMatterName(listQuotation.getSubjectMatterName());

        // 合同附件
        long attachmentId = agreementCreateAttachmentHandle(scheme.getId());
        baseInfoVO.setAttachmentId(attachmentId);

        return baseInfoVO;
    }

    @Override
    public PageResult<VendorAgreementListVO> listVendorAgreement(VendorAgreementListQueryVO queryVO) {
        IPage<VendorAgreementListVO> page = baseMapper.selectVendorAgreementList(queryVO.toMybatisPage(),queryVO);

        VendorContact vendorContact = vendorContactService.getVendorContactByLoginUser(SecurityUtils.getUserId());
        for (VendorAgreementListVO agreementList : page.getRecords()) {
            if (vendorContact.getIsManager() == 1) {
                agreementList.setOperateFlag(1);
            } else {
                agreementList.setOperateFlag(0);
            }
        }

        return new PageResult<>(page);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public AgreementSaveVO saveAgreement(AgreementSaveRequestVO requestVO) {
        AgreementSaveVO agreementSave;

        if (NumberUtil.isNullOrZero(requestVO.getAgreement().getId())) {
            // todo 为易料推送过来的合同先不进行校验
            if (StringUtils.isEmpty(requestVO.getAgreement().getMarketMaterialContractId())) {
                // 校验清单数据
                checkSaveAgreementMaterialsList(requestVO.getAgreementMaterialsLists(),requestVO.getAgreement().getSchemeId(),requestVO.getAgreement().getContractSplitId(),
                        requestVO.getAgreement().getVendorId());
            }
            /* 插入 签订合同 */
            agreementSave = addAgreement(requestVO);
        } else {
            // todo 为易料推送过来的合同先不进行校验
            if (StringUtils.isEmpty(requestVO.getAgreement().getMarketMaterialContractId())) {
                // 修改使用的 校验清单数据方法，和新增校验不一样，该方法查询了该合同上一次签订的单价，校验规则还原了数据再进行校验的。
                checkSaveAgreementMaterialsListByUpdate(requestVO.getAgreementMaterialsLists(), requestVO.getAgreement().getSchemeId(), requestVO.getAgreement().getContractSplitId(), requestVO.getAgreement().getVendorId(), requestVO.getAgreement().getId());
            }
            /* 修改 签订合同 */
            agreementSave = updateAgreement(requestVO);
        }

        return agreementSave;
    }

    @Override
    public AgreementDetailVO detail(Long id) {
        Agreement agreement = super.getById(id);
        ValidateUtils.isNullException(agreement,"合同不存在,请确认");

        AgreementVO agreementVO = BeanCopierUtil.copyBean(agreement,AgreementVO.class);
        agreementVO.setPartyBContactName(agreement.getPartyBLegalName());
        agreementVO.setPartyBContactPhone(agreement.getPartyBLegalPhone());
        agreementVO.setExpenditureBusinessTypeText(DictBizCache.getValue(DictBizEnum.PROCUREMENT_PLAN_TYPE,String.valueOf(agreement.getExpenditureBusinessType())));
        agreementVO.setPaymentCycleText(underlingSystemService.listDictMap(DictBizEnum.UNDERLING_PAYMENT_CYCLE.getName()).get(agreement.getPaymentCycle()));
        agreementVO.setPriceFormText(underlingSystemService.listDictMap(DictBizEnum.UNDERLING_PRICE_FORM.getName()).get(agreement.getPriceForm()));

        //合同标签附件Url
        if (null != agreement.getLabelAttachmentId()){
         Attachment attachment = attachmentService.getById(agreement.getLabelAttachmentId());
         if (attachment != null) {
             agreementVO.setLabelAttachmentUrl(attachment.getFileUrl());
         }
        }

        // 处理支付方式
        if (StringUtils.isNotBlank(agreement.getPaymentWay())) {
            Map<String,String> paymentWayMap = underlingSystemService.listDictMap(DictBizEnum.UNDERLING_PAYMENT_TYPE.getName());
            String[] paymentWayS = agreement.getPaymentWay().split(",");
            agreementVO.setPaymentWayText(Arrays.stream(paymentWayS).map(paymentWayMap::get).collect(Collectors.joining(",")));
        }
        Integer priceType;
        Integer procurementPlanType;

        if(StringUtils.isEmpty(agreement.getMarketMaterialContractId())){
            // 获取采购方案
            ProcurementScheme procurementScheme = procurementSchemeService.getById(agreement.getSchemeId());
            ValidateUtils.isNullException(procurementScheme,"该合同对应的采购方案不存在，请确认");
            agreementVO.setProcurementSchemeCode(procurementScheme.getProcurementSchemeCode());
            agreementVO.setProcurementSchemeName(procurementScheme.getProcurementSchemeName());
            priceType = procurementScheme.getPriceType();
            procurementPlanType = procurementScheme.getProcurementType();
        } else {
            MarketMaterialContract contract = marketMaterialContractService.getById(agreement.getMarketMaterialContractId());
            ValidateUtils.isNullException(contract, "该合同对应的易料采购合同为空");
            ProcurementPlan planInfo = procurementPlanService.getById(contract.getPlanId());
            ValidateUtils.isNullException(planInfo,"该易料采购合同对应的采购计划不存在");
            priceType = planInfo.getPriceType();
            procurementPlanType = Integer.valueOf(contract.getExpenditureBusinessType());
        }

        agreementVO.setPriceType(priceType);
        agreementVO.setAttachmentName(agreement.getAgreementName() + ".docx");
        agreementVO.setIsOperate(getAgreementIsOperate(agreement.getAgreementState(),agreement.getCreateId(),agreement.getSignatureUserId()));

        SysDept sysDept = remoteSystemService.getByThridDeptId(agreement.getPartyAOrgId(), SecurityConstants.INNER);
        agreementVO.setPartyADeptId(sysDept.getDeptId());

        // 合同款项信息
        AgreementPaymentItemVO agreementPaymentItemVO = agreementPaymentItemService.getByAgreementId(id);
        agreementPaymentItemVO.setTotalAmountExcTax(agreement.getTotalAmountExcTax());
        agreementPaymentItemVO.setTotalAmountIncTax(agreement.getTotalAmountIncTax());

        // 结算与付款节点信息
        List<AgreementPaymentListVO> agreementPaymentLists = agreementPaymentListService.listByAgreementId(id);

        List<AgreementMaterialsListVO> materialsLists;
        // 合同清单
        if (StringUtils.isEmpty(agreement.getMarketMaterialContractId())) {
            materialsLists = agreementMaterialsListService.listAgreementMaterials(id);
        } else {
            materialsLists = agreementMaterialsListService.listAgreementMaterialsByMarket(id);
        }
        // 设置价格类型
        // 购买材料，设置价款类型、交易标的物类型
        if (ProcurementPlanTypeEnum.PURCHASE_MATERIALS.equalsType(procurementPlanType)) {
            for (AgreementMaterialsListVO materialsListVO : materialsLists) {
                materialsListVO.setPaymentType(priceType == null ? "" : priceType.toString());
            }
        }
        // 合同保证金
        List<AgreementDepositVO> agreementDeposits = agreementDepositService.listByAgreementId(id);

        // 合同-计日工对象
        List<AgreementDailyWageVO> agreementDailyWageList = agreementDailyWageService.listByAgreementId(id);

        // 合同-机械台班对象
        List<AgreementMachineShiftVO> agreementMachineShifts = agreementMachineShiftService.listByAgreementId(id);

        // 合同-甲供设备清单对象
        List<AgreementEquipmentSupplyVO> agreementEquipmentSupplies = agreementEquipmentSupplyService.listByAgreementId(id);

        // 合同-甲供材料清单对象
        List<AgreementMaterialSupplyVO> agreementMaterialSupplies = agreementMaterialSupplyService.listByAgreementId(id);

        return AgreementDetailVO.builder()
                .agreement(agreementVO)
                .agreementPaymentItem(agreementPaymentItemVO)
                .agreementPaymentLists(agreementPaymentLists)
                .materialsList(materialsLists)
                .agreementDeposits(agreementDeposits)
                .agreementDailyWageList(agreementDailyWageList)
                .agreementMachineShifts(agreementMachineShifts)
                .agreementEquipmentSupplies(agreementEquipmentSupplies)
                .agreementMaterialSupplies(agreementMaterialSupplies)
                .build();
    }

    @Override
    public VendorAgreementDetailVO getVendorAgreementDetail(Long id) {
        Agreement agreement = super.getById(id);
        ValidateUtils.isNullException(agreement,"该合同信息不存在，请确认");

        // 供应商
        Vendor vendor = vendorService.getById(agreement.getVendorId());
        if (!agreement.getVendorId().equals(vendor.getId())) {
            throw new BusinessException(ResultCode.FAILURE,"该合同不是您的，您无权查看");
        }

        VendorAgreementVO agreementVO = BeanCopierUtil.copyBean(agreement,VendorAgreementVO.class);
        agreementVO.setPartyBContactName(agreement.getPartyBLegalName());
        agreementVO.setPartyBContactPhone(agreement.getPartyBLegalPhone());
        agreementVO.setIdentificationNumber(vendor.getSocialCreditCode());

        agreementVO.setCreatePhone(systemUserService.getUserById(agreement.getCreateId()).getPhonenumber());

        // 合同附件文本
        agreementVO.setAttachmentName(agreement.getAgreementName());
        if (AgreementStateEnum.isSign(agreement.getAgreementState())) {
            agreementVO.setFileType("pdf");
        } else {
            agreementVO.setFileType("docx");
        }

        // 合同款项信息
        AgreementPaymentItemVO agreementPaymentItemVO = agreementPaymentItemService.getByAgreementId(id);

        List<AgreementMaterialsListVO> agreementMaterialsList = agreementMaterialsListService.listAgreementMaterials(id);

        return VendorAgreementDetailVO.builder()
                .agreement(agreementVO)
                .agreementPaymentItem(agreementPaymentItemVO)
                .materialsList(agreementMaterialsList)
                .build();
    }

    @Override
    public DownloadAgreementVO getAgreementFileInputStream(Long id) {
        Agreement agreement = super.getById(id);
        ValidateUtils.isNullException(agreement,"该合同不存在，请确认后再操作");
        if (NumberUtil.isNullOrZero(agreement.getWatermarkAttachmentId())) {
            throw new BusinessException("合同文件正在生成中，请稍后重试");
        }

        DownloadAgreementVO agreementVO = null;
        if (AgreementStateEnum.isSign(agreement.getAgreementState())) {
            // 签署状态，从电子签章获取
            SysUser sysUser = systemUserService.getLoginUser();
            DownloadDocumentCommandRequest commandRequest = new DownloadDocumentCommandRequestBuilder()
                    .builder("tb_agreement",agreement.getId(),sysUser.getNickName(),sysUser.getUserName())
                    .build();
            SignatureResponse response = SignatureCommandFactory.getInstance().executeSignCommand(new DownloadDocumentCommand(commandRequest),null);
            if (response.isSuccess()) {
                InputStream inputStream = (InputStream) response.getResponseResult();
                agreementVO = new DownloadAgreementVO();
                agreementVO.setFileStream(inputStream);
                agreementVO.setFileName(agreement.getAgreementName() + ".pdf");
            } else {
                throw new BusinessException(response.getMessage());
            }
        } else {
            agreementVO = attachmentService.getAttachmentInputStream(agreement.getWatermarkAttachmentId(),agreement.getAgreementName());
        }

        return  agreementVO;
    }

    @Override
    public PageResult<AgreementJkpthtListVO> listAgreementJkpthtPage(AgreementJkpthtListQueryVO queryVO) {
        IPage<AgreementJkpthtListVO> pages = baseMapper.selectAgreementJkpthtList(queryVO.toMybatisPage(),queryVO);
        return new PageResult<>(pages);
    }

    @Override
    public AgreementUnderlingDetailVO getAgreementUnderlingDetail(Long id) {
        // 获取合同
        Agreement agreement = super.getById(id);
        ValidateUtils.isNullException(agreement,"该合同不存在");

        // 获取合同协议付款
        AgreementPaymentItemVO paymentItem = agreementPaymentItemService.getByAgreementId(id);
        ValidateUtils.isNullException(paymentItem,"该合同对应的付款协议不存在");

        AgreementUnderlingDetailVO underlingDetailVO;
        // 获取合约规划记录
        if(StringUtils.isEmpty(agreement.getMarketMaterialContractId())){
            ContractPlanning contractPlanning = contractPlanningService.getByContractSplitId(agreement.getContractSplitId());
            ValidateUtils.isNullException(paymentItem,"该合同对应的合约规划记录不存在");

            ProcurementScheme procurementScheme = procurementSchemeService.getById(agreement.getSchemeId());
            ValidateUtils.isNullException(procurementScheme,"该合同对应的采购方案不存在");

            // 构建对象
            underlingDetailVO = new AgreementUnderlingDetailVO(agreement,paymentItem,contractPlanning,procurementScheme);
        } else {
            // 构建对象
            underlingDetailVO = new AgreementUnderlingDetailVO(agreement,paymentItem,new ContractPlanning(),new ProcurementScheme());
        }

        // 获取合同计日工信息
        List<AgreementDailyWageVO> contractDatallerList = agreementDailyWageService.listByAgreementId(id);
        underlingDetailVO.setContractDatallerList(contractDatallerList);

        // 合同押金、保证金信息
        List<AgreementDepositVO> contractDepositList = agreementDepositService.listByAgreementId(id);
        underlingDetailVO.setContractDepositList(contractDepositList);

        // 获取机械台班
        List<AgreementMachineShiftVO> contractMechanicalTableList = agreementMachineShiftService.listByAgreementId(id);
        underlingDetailVO.setContractMechanicalTableList(contractMechanicalTableList);

        // 合同-结算与付款信息节点
        List<AgreementPaymentListVO> contractNodeList = agreementPaymentListService.listByAgreementId(id);
        underlingDetailVO.setContractNodeList(contractNodeList);

        // 甲供设备清单列表
        List<AgreementEquipmentSupplyVO> contractSupplyEquipmentList = agreementEquipmentSupplyService.listByAgreementId(id);
        underlingDetailVO.setContractSupplyEquipmentList(contractSupplyEquipmentList);

        // 甲供材料清单列表
        List<AgreementMaterialSupplyVO> contractSupplyMaterialsList = agreementMaterialSupplyService.listByAgreementId(id);
        underlingDetailVO.setContractSupplyMaterialsList(contractSupplyMaterialsList);

        // 获取合同清单
        List<AgreementUnderlingMaterialsVO> underlingMaterialsList;
        if(StringUtils.isEmpty(agreement.getMarketMaterialContractId())) {
            underlingMaterialsList = agreementMaterialsListService.listUnderlingMaterials(id);
        } else {
            List<AgreementMaterialsList> lists = agreementMaterialsListService.list(new LambdaQueryWrapper<AgreementMaterialsList>()
                    .eq(AgreementMaterialsList::getAgreementId, id));
            underlingMaterialsList = BeanCopierUtil.copyList(lists,AgreementUnderlingMaterialsVO.class);
        }
        switch (agreement.getExpenditureBusinessType()) {
            case 1 :
                // 物资采购
                underlingDetailVO.setContractListMaterialsList(underlingMaterialsList);
                break;
            case 2 :
                // 物资租赁
                underlingDetailVO.setContractListLeasedMaterialsList(underlingMaterialsList);
                break;
            case 3 :
                // 机械租赁
                underlingDetailVO.setContractListLeasedDeviceList(underlingMaterialsList);
                break;
            case 4 :
                // 专业分包
                underlingDetailVO.setContractListSpecialtyList(underlingMaterialsList);
                break;
            case 5 :
                // 劳务分包
                underlingDetailVO.setContractListLaborList(underlingMaterialsList);
                break;
            case 6 :
                // 其他
                underlingDetailVO.setContractListOtherList(underlingMaterialsList);
                break;
        }

        return underlingDetailVO;
    }

    @Override
    public AgreementFileVO getAgreementAttachmentId(Long id) {
        Agreement agreement = this.getById(id);
        ValidateUtils.isNullException(agreement,"该合同不存在");
        long attachmentId = 0L;
        String message = "获取成功";
        if (NumberUtil.isNotNullAndZero(agreement.getWatermarkAttachmentId())) {
            attachmentId = agreement.getWatermarkAttachmentId();
        }else if (NumberUtil.isNotNullAndZero(agreement.getLabelAttachmentId())) {
            attachmentId = agreement.getLabelAttachmentId();
        } else {
            message = "合同附件正在生成中，请稍后再查看";
        }

        return new AgreementFileVO(attachmentId,message);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRED,rollbackFor = Exception.class)
    public void cancellationAgreement(Long id) {
        Agreement agreement = super.getById(id);
        ValidateUtils.isNullException(agreement,"该合同不存在，请确认");
        if (!AgreementStateEnum.isCancel(agreement.getAgreementState())) {
            throw new BusinessException("该状态下的合同不允许作废");
        }

        // 修改合同状态
        super.update(new LambdaUpdateWrapper<Agreement>()
                .set(Agreement::getAgreementState,AgreementStateEnum.CANCELLATION.getState())
                .eq(Agreement::getId,id));

        // 回写清单数量
        List<AgreementMaterialsList> agreementMaterialsLists = agreementMaterialsListService.listByAgreementId(id);
        Map<Long,AgreementMaterialsList> agreementMaterialsListMap = agreementMaterialsLists.stream()
                .collect(Collectors.toMap(AgreementMaterialsList::getMaterialsListId,val -> val));

        // 获取合同对应的拆分合约的物理清单数据
        List<MaterialsList> materialsLists = materialsListService.listMaterialsListByContractSplitIds(CollectionUtil.newArrayList(agreement.getContractSplitId()));
        for (MaterialsList materialsList : materialsLists) {
            AgreementMaterialsList agreementMaterialsList = agreementMaterialsListMap.get(materialsList.getId());
            materialsList.setUsedCount(NumberUtil.subtract(materialsList.getUsedCount(),agreementMaterialsList.getSignCount()));
        }
        materialsListService.updateMaterialsListUsedCount(materialsLists);

        // 设置合约拆分记录为未使用完毕
        contractPlanningSplitService.updateContractPlanningSplitUseSub(agreement.getContractSplitId(),agreementMaterialsLists);
    }

    @Override
    public void submitAgreement(Long id,String detailUrl) {
        Agreement agreement = super.getById(id);
        ValidateUtils.isNullException(agreement,"该合同不存在，请确认");
        if (!(AgreementStateEnum.DRAFT.getState().equals(agreement.getAgreementState()) || AgreementStateEnum.REVOKED.getState().equals(agreement.getAgreementState()))) {
            throw new BusinessException("该状态下的合同不允许提交");
        }

        if (NumberUtil.isNullOrZero(agreement.getLabelAttachmentId())) {
            throw new BusinessException("合同附件正在生成中，请稍后重试");
        }

        // 状态调整为审批中
        super.update(new LambdaUpdateWrapper<Agreement>()
                .set(Agreement::getAgreementState,AgreementStateEnum.IN_APPROVAL.getState())
                .eq(Agreement::getId,id));

        if (AgreementStateEnum.DRAFT.equalsState(agreement.getAgreementState())) {
            // 初次提交，设置水印
            Attachment attachment = attachmentService.getById(agreement.getLabelAttachmentId());
            agreementFileZService.applyWatermark(agreement.getAgreementName(),agreement.getPartyAName(),attachment.getFileUrl(),agreement.getId());
        }

        //接入底层逻辑平台流程
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("businessId", agreement.getId());
        paramMap.put("projectCode", agreement.getBelongAccountingItemCode());
        paramMap.put("businessTitle", "合同审批");
        paramMap.put("businessContent", String.format(ApproveFlowPromptTemplateEnum.CONTRACT_APPROVE.getDesc(), agreement.getAgreementName()));
        paramMap.put("detailUrl", detailUrl);
        UserObj userObj = UserObj.builder().businessType(ProcessKeyEnum.ZHAOCAI_AGREEMENT_SIGN.name()).
                businessId(id.toString())
                .toDoType(ToDoTypeEnum.EXAMINE.name()).build();
        paramMap.put("userObj", JSON.toJSONString(userObj));

        /** 合同类型（contractType），价格(contractMoney)，项目部（parentProjectCode），责任单位（responsibilityDeptId），公司（companyId） */
        paramMap.put("contractType", String.valueOf(agreement.getExpenditureBusinessType()));/* 合同签订流程 合同类型 */
        paramMap.put("contractMoney", agreement.getTotalAmountIncTax());/* 合同签订流程 价格 */

        /* startProcessInstance方法内根据projectCode拿到了层级数据了 */
//        MinProject minProject = minProjectService.getOne(new LambdaQueryWrapper<MinProject>().eq(MinProject::getMinAccountCode, agreement.getBelongAccountingItemCode()).eq(MinProject::getDelFlag, NumberConstant.ZERO));
////        ValidateUtils.isNullException(minProject,"该合同最小核算项目不存在，请确认");
//        SysDept sysDept = remoteSystemService.getByThridDeptId(agreement.getPartyAOrgId(), SecurityConstants.INNER);
//        paramMap.put("parentProjectCode", minProject==null?null:minProject.getBelongingOrgId()==null?null:minProject.getBelongingOrgId());/* 项目部 */
//        paramMap.put("responsibilityDeptId", minProject==null?null:minProject.getDutyUnit()==null?null:minProject.getDutyUnit());/* 责任单位 */
//        paramMap.put("companyId", sysDept==null?null:sysDept.getThridParentId()==null?null:sysDept.getThridParentId());/* 公司 */

        processService.startProcessInstance(ProcessKeyEnum.ZHAOCAI_AGREEMENT_SIGN.getIdentifying(),paramMap);
    }

    @Override
    public void setAgreementFileLabelFinish(Long id, long attachmentId) {
        this.update(new LambdaUpdateWrapper<Agreement>()
                .set(Agreement::getLabelAttachmentId,attachmentId)
                .eq(Agreement::getId,id));
    }

    @Override
    public Boolean checkAgreementUpdate(Long id) {
        Agreement agreement = super.getById(id);
        ValidateUtils.isNullException(agreement,"该合同不存在");
        if (!(AgreementStateEnum.DRAFT.getState().equals(agreement.getAgreementState()) || AgreementStateEnum.REVOKED.getState().equals(agreement.getAgreementState()))) {
            throw new BusinessException("该状态下的合同不允许修改");
        }

        if (NumberUtil.isNullOrZero(agreement.getLabelAttachmentId())) {
            throw new BusinessException("合同附件正在生成中，请稍后重试");
        }

        return true;
    }

    @Override
    public void agreementWatermarkFinish(Long id, long attachmentId) {
        this.update(new LambdaUpdateWrapper<Agreement>()
                .set(Agreement::getWatermarkAttachmentId ,attachmentId)
                .eq(Agreement::getId,id));
    }

    @Override
    public Long getLabelAttachmentId(Long id) {
        Agreement agreement = super.getById(id);
        return agreement.getLabelAttachmentId();
    }

    @Override
    public void processStart(Map<String, Object> variables) {
        String processId = variables.get("processId").toString();
        String businessId = variables.get("businessId").toString();
        Object flagObj = variables.get("completedFlag");
        Integer agreementState =  AgreementStateEnum.IN_APPROVAL.getState();
        if (!ObjectUtils.isEmpty(flagObj) && ProcessStateEnum.COMPLETED.getDesc().equals(flagObj.toString())) {
            agreementState = AgreementStateEnum.APPROVE.getState();
        }
        super.update(new LambdaUpdateWrapper<Agreement>()
                .set(Agreement::getWfProcessId,processId)
                .set(Agreement::getAgreementState,agreementState)
                .eq(Agreement::getId,businessId));
    }

    @Override
    public void processAuditPass(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        super.update(new LambdaUpdateWrapper<Agreement>()
                .set(Agreement::getAgreementState, AgreementStateEnum.APPROVE.getState())
                .eq(Agreement::getId, businessId));
    }

    @Override
    public void revokeAgreement(Long id) {
        Agreement agreement = super.getById(id);
        ValidateUtils.isNullException(agreement,"该合同不存在");
        ValidateUtils.validateStatusNotEquals(AgreementStateEnum.IN_APPROVAL::equalsState,agreement.getAgreementState(),"该状态下的合同不允许撤回");
        // 撤回流程
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("businessId", agreement.getId());
        paramMap.put("processId", agreement.getWfProcessId());
        processService.revokeProcess(ProcessKeyEnum.ZHAOCAI_AGREEMENT_SIGN.getIdentifying(),paramMap);
    }

    @Override
    public AgreementSelectedProcurementInfoVO getAgreementSelectedProcurementInfo(Long schemeId, Long contractSplitId) {
        AgreementSelectedProcurementInfoVO biddingInfoVO = new AgreementSelectedProcurementInfoVO();

        // 获取上限价
        ContractPlanning contractPlanning = contractPlanningService.getByContractSplitId(contractSplitId);
        biddingInfoVO.setUpperLimitPrice(contractPlanning.getPlannedAmountInclTax());

        // 获取采购上限价 = 采购计划清单 * 单价
        List<MaterialsList> materialsLists = materialsListService.listMaterialsListByContractSplitIds(CollectionUtil.newArrayList(contractSplitId));
        BigDecimal procurementUpperLimitPrice = BigDecimal.ZERO;
        for (MaterialsList materialsList : materialsLists) {
            procurementUpperLimitPrice = procurementUpperLimitPrice.add(AmountCalUtil.calTotalAmountInclTax(materialsList.getCount(),materialsList.getUnitPriceInclTax()));
        }
        biddingInfoVO.setProcurementUpperLimitPrice(NumberUtil.round(procurementUpperLimitPrice,2));


        ContractPlanningSplit contractPlanningSplit = contractPlanningSplitService.getById(contractSplitId);
        // 已使用总金额
        biddingInfoVO.setUsedTotalAmount(contractPlanningSplit.getTotalUsedAmount());

        // 剩余总价
        biddingInfoVO.setSurplusTotalAmount(NumberUtil.subtract(contractPlanningSplit.getTotalPlanAmount(),contractPlanningSplit.getTotalUsedAmount(),2));

        // 获取招标供应商
        List<ProcurementSchemeBiddingVendorVO> biddingVendorList = biddingResultService.listBiddingVendorBySchemeId(schemeId);
        biddingInfoVO.setBiddingVendorList(biddingVendorList);

        return biddingInfoVO;
    }



    @Override
    public void pushAgreementToVendor(Long id) {
        Agreement agreement = this.getById(id);
        ValidateUtils.validateStatusNotEquals(AgreementStateEnum.APPROVE::equalsState,agreement.getAgreementState(),"该状态下的合同不允许推送至供应商");

        if (NumberUtil.isNullOrZero(agreement.getWatermarkAttachmentId())) {
            throw new BusinessException("合同水印还没有生成，请联系开发确认");
        }

        boolean updateResult = this.update(new LambdaUpdateWrapper<Agreement>()
                .set(Agreement::getAgreementState,AgreementStateEnum.VENDOR_VERIFY.getState())
                .eq(Agreement::getId,id));

        if (!updateResult) {
            throw new BusinessException("推送合同至供应商失败");
        }
    }

    @Override
    public void vendorAffirmAgreement(Long id) {
        Agreement agreement = this.getById(id);
        ValidateUtils.validateStatusNotEquals(AgreementStateEnum.VENDOR_VERIFY::equalsState,agreement.getAgreementState(),"该状态下的合同不允许确认");
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        if (!vendor.getId().equals(agreement.getVendorId())) {
            throw new BusinessException("该合同不属于贵司，您无权操作");
        }

        Attachment attachment = attachmentService.getById(agreement.getWatermarkAttachmentId());

        // 生成 PDF
        agreementFileZService.agreementConvertToPdf(id,null,agreement.getAgreementName(),attachment.getFileUrl());
    }

    @Override
    public void coverToPdfFinish(Long id, long attachmentId) {
        super.update(new LambdaUpdateWrapper<Agreement>()
                .set(Agreement::getAgreementState, AgreementStateEnum.PUSH_SIGNATURE_PLATFORM.getState())
                .set(Agreement::getConvertPdfAttachmentId,attachmentId)
                .eq(Agreement::getId, id));
    }

    @Override
    public Boolean checkAgreementAffirmState(Long id) {
        Agreement agreement = this.getById(id);
        return NumberUtil.isNotNullAndZero(agreement.getConvertPdfAttachmentId())
                && AgreementStateEnum.PUSH_SIGNATURE_PLATFORM.equalsState(agreement.getAgreementState());
    }

    @Override
    public void pushAgreementToSignPlatform(PushAgreementToSignVO pushAgreementToSign) {
        Agreement agreement = this.getById(pushAgreementToSign.getId());
        ValidateUtils.validateStatusNotEquals(AgreementStateEnum.PUSH_SIGNATURE_PLATFORM::equalsState,agreement.getAgreementState(),"该状态下的合同不允许推送至电子签章平台");
        if (NumberUtil.isNullOrZero(agreement.getConvertPdfAttachmentId())) {
            throw new ParamValidateException("该合同签订合同附件还没有生成，请联系开发确认");
        }
        // 获取合同附件
        Attachment attachment = attachmentService.getById(agreement.getConvertPdfAttachmentId());
        InputStream inputStream = null;
        try{
            inputStream = sysFileService.getFileByFileUrl(attachment.getFileUrl());

            // 获取甲方签订人
            SysUser sysUser = systemUserService.getUserById(pushAgreementToSign.getPartyAUserId());
            SignatureContact signatureContactA = new SignatureContact();
            signatureContactA.setSignatureType(SignatureTypeEnum.PARTY_A.getType());
            signatureContactA.setSignatureId(agreement.getPartyAOrgId());
            signatureContactA.setSignatureName(agreement.getPartyAName());
            signatureContactA.setContactId(sysUser.getUserId());
            signatureContactA.setContactName(sysUser.getNickName());
            signatureContactA.setContactPhone(sysUser.getUserName());

            // 获取乙方签订人
            Vendor vendor = vendorService.getById(agreement.getVendorId());
            VendorContact vendorContact = vendorContactService.getVendorManager(agreement.getVendorId());
            SignatureContact signatureContactB = new SignatureContact();
            signatureContactB.setSignatureType(SignatureTypeEnum.PARTY_B.getType());
            signatureContactB.setSignatureId(vendor.getId().toString());
            signatureContactB.setSignatureName(vendor.getEnterpriseName());
            signatureContactB.setContactId(vendorContact.getId());
            signatureContactB.setContactName(vendorContact.getContactName());
            signatureContactB.setContactPhone(vendorContact.getContactPhone());

            // 发起人信息
            SignatureCreator signatureCreator = new SignatureCreator();
            signatureCreator.setTenantId(agreement.getPartyAOrgId());
            signatureCreator.setTenantName(agreement.getPartyAName());
            signatureCreator.setCreatorContact(sysUser.getUserName());
            signatureCreator.setCreatorName(sysUser.getNickName());

            // 获取合同签署位置
            ProcurementSchemeBidding schemeBidding = procurementSchemeBiddingService.getDomainBySchemeId(agreement.getSchemeId());
            List<AgreementSignStamper> signStampers = agreementSignStamperService.listByTemplateId(schemeBidding.getContractTemplateId());
            if (CollectionUtil.isEmpty(signStampers)) {
                throw new BusinessException("该合同对应的合同模板签章位置信息为空，请确认后重试");
            }
            List<SignatureStamper> signatureStamperList = signStampers.stream()
                    .map(x -> {
                        SignatureStamper signatureStamper = new SignatureStamper();
                        signatureStamper.setType(x.getType());
                        signatureStamper.setSignType(x.getSignType());
                        signatureStamper.setKeyWord(x.getKeyWord());
                        signatureStamper.setSignPage(x.getSignPage());
                        signatureStamper.setOffsetX(x.getOffsetX());
                        signatureStamper.setOffsetY(x.getOffsetY());
                        return signatureStamper;
                    }).collect(Collectors.toList());

            // 构建请求命令
            CreateAgreementDocumentCommandRequest commandRequest = new CreateAgreementDocumentCommandRequestBuilder()
                    .builder("tb_agreement",agreement.getId(),sysUser.getNickName(),sysUser.getUserName())
                    .agreementName(agreement.getAgreementName())
                    .agreementCode(agreement.getAgreementCode())
                    .agreementFileType(FileUtil.getSuffix(attachment.getFileName()))
                    .agreementFile(inputStream)
                    .signatureContactPartyA(signatureContactA)
                    .signatureContactPartyB(signatureContactB)
                    .signatureCreator(signatureCreator)
                    .signatureStamperList(signatureStamperList)
                    .build();

            SignatureCommandFactory.getInstance().executeSignCommand(new CreateAgreementDocumentCommand(commandRequest),null);

            this.update(new LambdaUpdateWrapper<Agreement>()
                    .set(Agreement::getSignatureUserId,pushAgreementToSign.getPartyAUserId())
                    .set(Agreement::getAgreementState,AgreementStateEnum.PARTY_B_TO_SIGN.getState())
                    .eq(Agreement::getId,pushAgreementToSign.getId()));
        } catch (Exception e) {
            throw e;
        } finally {
            IoUtil.close(inputStream);
        }

    }

    @Override
    public String vendorSignAgreement(Long id) {
        Agreement agreement = this.getById(id);
        ValidateUtils.validateStatusNotEquals(AgreementStateEnum.PARTY_B_TO_SIGN::equalsState,agreement.getAgreementState(),"该状态的合同不允许您签署，请确认后重新操作");

        SysUser sysUser = systemUserService.getLoginUser();
        Vendor vendor = vendorService.getByLoginUser(SecurityUtils.getUserId());
        if (!vendor.getId().equals(agreement.getVendorId())) {
            throw new BusinessException("该合同不属于您，您无权操作");
        }

        VendorContact vendorContact = vendorContactService.getVendorContactByLoginUser(SecurityUtils.getUserId());
        if (vendorContact.getIsManager() == 0) {
            throw new ParamValidateException("您不是管理员，无权执行签订操作");
        }
        if (!SignStateEnum.SIGN_SUCCESS.equalsState(vendorContact.getSignState())) {
            throw new ParamValidateException("您还未进行个人认证授权，请先完成个人认证授权");
        }
        if (!SignStateEnum.SIGN_SUCCESS.equalsState(vendor.getSignState())) {
            throw new ParamValidateException("您还未进行企业认证授权，请先完成企业认证授权");
        }

        GetSignUrlCommandRequest commandRequest = new GetSignUrlCommandRequestBuilder().builder("tb_agreement",id,sysUser.getNickName(),sysUser.getUserName())
                .signatureName(vendor.getEnterpriseName())
                .signatureType(SignatureTypeEnum.PARTY_B)
                .build();

        SignatureResponse signatureResponse = SignatureCommandFactory.getInstance().executeSignCommand(new GetSignUrlCommand(commandRequest),null);
        ContractSignurlV3Response response = (ContractSignurlV3Response) signatureResponse.getResponseResult();
        return response.getSignUrl();
    }

    @Override
    public String signAgreement(Long id) {
        Agreement agreement = this.getById(id);
        ValidateUtils.validateStatusNotEquals(AgreementStateEnum.PARTY_A_TO_SIGN::equalsState,agreement.getAgreementState(),"该状态的合同不允许您签署，请确认后重新操作");
        if (!agreement.getSignatureUserId().equals(SecurityUtils.getUserId())) {
            throw new ParamValidateException("您不是该合同的签署人，无权签署该合同");
        }

        SysUser sysUser = systemUserService.getLoginUser();
        GetSignUrlCommandRequest commandRequest = new GetSignUrlCommandRequestBuilder().builder("tb_agreement",id,sysUser.getNickName(),sysUser.getUserName())
                .signatureName(agreement.getPartyAName())
                .signatureType(SignatureTypeEnum.PARTY_A)
                .build();

        SignatureResponse signatureResponse = SignatureCommandFactory.getInstance().executeSignCommand(new GetSignUrlCommand(commandRequest),null);
        ContractSignurlV3Response response = (ContractSignurlV3Response) signatureResponse.getResponseResult();
        return response.getSignUrl();
    }

    @Override
    public void cancelledSignAgreement(CancelledSignAgreementRequestVO requestVO) {
        Agreement agreement = this.getById(requestVO.getId());
        ValidateUtils.validateStatusNotEquals(AgreementStateEnum.SIGN_SUCCESS::equalsState,agreement.getAgreementState(),"未完成签署的合同不允许作废");
        if (!agreement.getSignatureUserId().equals(SecurityUtils.getUserId())) {
            throw new ParamValidateException("该合同不是您签署的，您无权作废");
        }

        SysUser sysUser = systemUserService.getLoginUser();
        CancelAgreementCommandRequest commandRequest = new CancelAgreementCommandRequestBuilder().builder("tb_agreement",agreement.getId(),sysUser.getNickName(),sysUser.getUserName())
                .cancelReason(requestVO.getCancelledReason())
                .build();
        SignatureResponse signatureResponse = SignatureCommandFactory.getInstance().executeSignCommand(new CancelAgreementCommand(commandRequest),null);
        if (signatureResponse.isSuccess()) {
            this.update(new LambdaUpdateWrapper<Agreement>()
                    .set(Agreement::getAgreementState,AgreementStateEnum.CANCEL_SIGN.getState())
                    .set(Agreement::getCancelledReason,requestVO.getCancelledReason())
                    .eq(Agreement::getId,requestVO.getId()));
        } else {
            throw new BusinessException(signatureResponse.getMessage());
        }
    }

    /**
     * 免审登录（易料合同）
     * @param id
     * @return
     */
    @Override
    public boolean avoidSubmitByMarket(Long id) {
        Agreement agreement = super.getById(id);
        ValidateUtils.isNullException(agreement,"该合同不存在");
        return super.update(new LambdaUpdateWrapper<Agreement>()
                .set(Agreement::getAgreementState, AgreementStateEnum.APPROVE.getState())
                .eq(BaseEntity::getId, id));
    }

    /**
     * 审批驳回到发起人
     * @param variables
     */
    @Override
    public void processAuditFreedom(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        super.update(new LambdaUpdateWrapper<Agreement>()
                .set(Agreement::getAgreementState, AgreementStateEnum.DRAFT.getState())
                .eq(Agreement::getId, businessId));
    }

    /**
     * 审批驳回
     * @param variables
     */
    @Override
    public void processAuditReject(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        super.update(new LambdaUpdateWrapper<Agreement>()
                .set(Agreement::getAgreementState, AgreementStateEnum.IN_APPROVAL.getState())
                .eq(Agreement::getId, businessId));

    }
    /**
     * 审批撤销
     * @param variables
     */
    @Override
    public void processAuditRevoke(Map<String, Object> variables) {
        String businessId = variables.get("businessId").toString();
        super.update(new LambdaUpdateWrapper<Agreement>()
                .set(Agreement::getAgreementState, AgreementStateEnum.REVOKED.getState())
                .eq(Agreement::getId, businessId));
    }

    @Override
    public ResultData<BpmInitializeResponseDTO> initialize(BpmInitializeRequestDTO requestDTO) {
        return null;
    }

    @Override
    public ResultData<List<BpmListProcessLogResponseDTO>> listProcessLog(BpmListProcessLogRequestDTO requestDTO) {
        return null;
    }

    @Override
    public String audit(String processKey, Map<String, Object> variables) {
        return null;
    }

    @Override
    public ResultData<List<BpmLoadTaskDefResponseDTO>> loadTaskDef(BpmLoadTaskDefRequestDTO requestDTO) {
        return null;
    }

    /**
     * 获取机构名称
     * @param thridDeptId
     * @return
     */
    private String getDeptName(String thridDeptId) {
        if (StringUtils.isNotBlank(thridDeptId)) {
            SysDept sysDept = remoteSystemService.getByThridDeptId(thridDeptId, SecurityConstants.INNER);
            return Optional.ofNullable(sysDept)
                    .map(SysDept::getDeptName)
                    .orElse("");
        }
        return null;
    }

    /**
     * 获取合同行政区域
     * @param prjAddr
     * @return
     */
    private String getAgreementPerformDistrict(String prjAddr) {
        if (StringUtils.isNotBlank(prjAddr)) {
            List<String> addrList = JSONArray.parseArray(prjAddr,String.class);
            StringBuilder agreementPerformDistrict = new StringBuilder();
            for(String code : addrList) {
                AreaDivision areaDivision = areaDivisionService.getByCode(code);
                if (areaDivision != null) {
                    agreementPerformDistrict.append(areaDivision.getAreaName()).append("/");
                }
            }

            return StringUtils.isNotBlank(agreementPerformDistrict.toString()) ?
                    agreementPerformDistrict.substring(0,agreementPerformDistrict.length() - 1) : "";
        }
        return null;
    }

    /**
     * 获取合同编号
     * @param expenditureBusinessType
     * @param belongOrganizationId
     * @return
     */
    private String getAgreementCode(Integer expenditureBusinessType,String belongOrganizationId) {
        String numberCode = businessCodeService.getBusinessCode(BusinessCodeEnum.AGREEMENT);
        belongOrganizationId = belongOrganizationId.substring(0,4);
        String businessType = ProcurementPlanTypeConver.converFromProcurementPlanType(expenditureBusinessType);

        return "WZ" + belongOrganizationId + businessType + DateUtils.dateTimeNow("yyyyMM") + numberCode;
    }

    /**
     * 处理新增合同时的附件<br>
     * 1. 为合同选定的合同模板新增一个附件
     * 2. 为合同附件增加一个联想文档任务
     * @param schemeId
     * @return
     */
    private long agreementCreateAttachmentHandle(Long schemeId) {
        AttachmentVO agreementAttachment = procurementSchemeService.getAgreementTemplateAttachmentInfo(schemeId);
        ValidateUtils.isNullException(agreementAttachment,"该合同没有选择模板，请确认");

        // 新增附件
        AttachmentRequestVO attachmentRequestVO = new AttachmentRequestVO(agreementAttachment.getFileName(),agreementAttachment.getFileUrl());
        long attachmentId = attachmentService.addAttachment(attachmentRequestVO,AttachmentTypeEnum.AGREEMENT_ORIGINAL,null);

        // 新增任务
        fileZTaskService.addInitialFileZTask(FileZTaskBusinessEnum.AGREEMENT_CREATE,attachmentId);

        return attachmentId;
    }

    /**
     * 新增合同
     * @param requestVO
     * @return
     */
    private AgreementSaveVO addAgreement(AgreementSaveRequestVO requestVO) {
        Agreement agreement = requestVO.getAgreement();
        AgreementMaterialsInfoDTO agreementMaterialsInfo;
        Integer procurementPlanType;
        if (StringUtils.isEmpty(requestVO.getAgreement().getMarketMaterialContractId())) {
            ProcurementScheme procurementScheme = procurementSchemeService.getById(requestVO.getAgreement().getSchemeId());
            ValidateUtils.isNullException(procurementScheme, "该合同对应的采购计划为空");
            procurementPlanType = procurementScheme.getProcurementPlanType();
            agreement.setAgreementCode(getAgreementCode(procurementPlanType,agreement.getBelongOrganizationId()));
        } else {
            MarketMaterialContract contract = marketMaterialContractService.getById(requestVO.getAgreement().getMarketMaterialContractId());
            ValidateUtils.isNullException(contract, "该合同对应的易料采购合同为空");
            procurementPlanType = Integer.valueOf(contract.getExpenditureBusinessType());
            agreement.setAgreementCode(getAgreementCodeByMarket(procurementPlanType,agreement.getBelongOrganizationId()));
            agreement.setProcurementSchemeCode(getProcurementSchemeCodeByMarket());
        }
        // 合同基本信息
        agreement.setAgreementState(AgreementStateEnum.DRAFT.getState());

        /*
         * 处理合同清单数据
         */
        if (StringUtils.isEmpty(requestVO.getAgreement().getMarketMaterialContractId())) {
            agreementMaterialsInfo = handleAgreementMaterials(requestVO.getAgreementMaterialsLists(),agreement.getSchemeId(),agreement.getContractSplitId(),
                    agreement.getVendorId(),agreement.getId());
        } else {
            agreementMaterialsInfo = handleAgreementMaterialsByMarket(requestVO.getAgreementMaterialsLists(),agreement.getMarketMaterialContractId());
        }

        // 合同签订总金额
        agreement.setTotalAmountIncTax(agreementMaterialsInfo.getTotalAmountInclTax());
        agreement.setTotalAmountExcTax(agreementMaterialsInfo.getTotalAmountExclTax());

        // 合同交易标的物
        agreement.setSubjectMatterName(agreementMaterialsInfo.getSubjectMatterName());
        agreement.setSubjectMatterCode(agreementMaterialsInfo.getSubjectMatterCode());

        //agreement.setReporterName(SecurityUtils.getLoginUser().getSysUser().getNickName());
        agreement.setExpenditureBusinessType(procurementPlanType);
        if (agreement.getEntryDate() != null && agreement.getFinishDate() != null) {
            long days = DateUtil.between(agreement.getEntryDate(),agreement.getFinishDate(), DateUnit.DAY);
            agreement.setDuration(days + "");
        }
        if (agreement.getContractStartDate() != null && agreement.getContractEndDate() != null) {
            long days = DateUtil.between(agreement.getContractStartDate(),agreement.getContractEndDate(), DateUnit.DAY);
            agreement.setDuration(days + "");
        }

        SysUser sysUser = systemUserService.getLoginUser();
        agreement.setPartyAContactName(sysUser.getNickName());
        agreement.setPartyAContactPhone(sysUser.getPhonenumber());

        super.save(agreement);

        // 合同款项信息
        agreementPaymentItemService.saveAgreementPaymentItem(requestVO.getAgreementPaymentItem(),agreement.getId());

        // 结算与付款节点信息
        agreementPaymentListService.saveAgreementPaymentList(requestVO.getAgreementPaymentLists(),agreement.getId());

        // 合同清单
        agreementMaterialsListService.saveAgreementMaterialsList(requestVO.getAgreementMaterialsLists(),agreement.getId());

        // 合同保证金
        agreementDepositService.saveAgreementDeposit(requestVO.getAgreementDeposits(),agreement.getId());

        // 合同-计日工
        agreementDailyWageService.saveAgreementDailyWage(requestVO.getAgreementDailyWageList(),agreement.getId());

        // 合同-机械台班
        agreementMachineShiftService.saveAgreementMachineShift(requestVO.getAgreementMachineShifts(),agreement.getId());

        // 合同-甲供设备清单
        agreementEquipmentSupplyService.saveAgreementEquipmentSupply(requestVO.getAgreementEquipmentSupplies(),agreement.getId());

        // 合同-甲供材料清单对象
        agreementMaterialSupplyService.saveAgreementMaterialSupply(requestVO.getAgreementMaterialSupplies(),agreement.getId());

        // 保存附件相关信息
        attachmentService.updateBusiness(agreement.getAttachmentId(),AttachmentTypeEnum.AGREEMENT_ORIGINAL,agreement.getId());

        // 保存合同附件任务
        fileZTaskService.setFileZBusinessId(agreement.getAttachmentId(),FileZTaskBusinessEnum.AGREEMENT_CREATE,agreement.getId(),requestVO.getTemplateEditFlag());

        // 占用合同数据量
        materialsListService.updateMaterialsListUsedCount(agreementMaterialsInfo.getMaterialsLists());

        // 判断合约拆分是否已使用完毕
        if (agreement.getEntryDate() != null && agreement.getFinishDate() != null) {
            contractPlanningSplitService.updateContractPlanningSplitUseAdd(agreement.getContractSplitId(),agreementMaterialsInfo.getMaterialsLists(),requestVO.getAgreementMaterialsLists(),null);
        }

        // 接入联想文档
        agreementFileZService.setAgreementLabel(agreement, requestVO.getAgreementMaterialsLists(), requestVO.getTemplateEditFlag());

        AgreementSaveVO saveVO = new AgreementSaveVO();
        saveVO.setId(agreement.getId());
        saveVO.setProcurementPlanType(procurementPlanType);

        return saveVO;
    }

    /**
     * 获取合同招标编号(易料合同)
     * @return
     */
    private String getProcurementSchemeCodeByMarket() {
        List<Agreement> list = super.list(new LambdaQueryWrapper<Agreement>().isNotNull(Agreement::getMarketMaterialContractId).orderByDesc(BaseEntity::getCreateTime));
        String numberCode = null;
        if(CollectionUtil.isEmpty(list)){
            numberCode = "000000001";
        } else {
            String code = list.get(0).getProcurementSchemeCode();
            if (code != null && code.length() >= 9) {
                String lastNineDigits = code.substring(code.length() - 9);
                int nextNumber = Integer.parseInt(lastNineDigits) + 1;
                numberCode = StrUtil.padPre(String.valueOf(nextNumber), 9, '0');
            } else {
                numberCode = "000000001";
            }
        }
        return "YLCG"  + DateUtils.dateTimeNow("yyyy") + numberCode;
    }

    /**
     * 获取合同编号(易料合同)
     * @param expenditureBusinessType
     * @param belongOrganizationId
     * @return
     */
    private String getAgreementCodeByMarket(Integer expenditureBusinessType, String belongOrganizationId) {
        String numberCode = businessCodeService.getBusinessCode(BusinessCodeEnum.AGREEMENT);
        belongOrganizationId = belongOrganizationId.substring(0,4);
//        String businessType = ProcurementPlanTypeConver.converFromProcurementPlanType(expenditureBusinessType);

        return "WZ" + belongOrganizationId + "Y" + DateUtils.dateTimeNow("yyyyMM") + numberCode;
    }

    /**
     * 处理合同清单数据
     * @param agreementMaterialsLists
     * @param schemeId
     * @param contractSplitId
     * @param vendorId
     */
    private AgreementMaterialsInfoDTO handleAgreementMaterials(List<AgreementMaterialsList> agreementMaterialsLists, Long schemeId, Long contractSplitId, Long vendorId,Long agreementId) {
        // 获取物料清单
        List<MaterialsList> materialsLists = materialsListService.listMaterialsListByContractSplitIds(CollectionUtil.newArrayList(contractSplitId));
        Map<Long,MaterialsList> materialsListMap = materialsLists.stream()
                .collect(Collectors.toMap(MaterialsList::getId,val -> val));

        // 获取供应商的投标物料清单
        List<BiddingListQuotation> biddingListQuotations = biddingListQuotationService.listVendorBiddingListQuotation(schemeId,contractSplitId,vendorId);
        Map<Long,BiddingListQuotation> listQuotationMap = biddingListQuotations.stream()
                .collect(Collectors.toMap(BiddingListQuotation::getMaterialsId,val -> val));

        // 处理合同清单
        BiddingListQuotation biddingListQuotation;
        MaterialsList materialsList;
        BigDecimal totalAmountInclTax = BigDecimal.ZERO;
        BigDecimal totalAmountExclTax = BigDecimal.ZERO;

        for (AgreementMaterialsList agreementMaterialsList : agreementMaterialsLists) {
            // 设置合约拆分 id
            agreementMaterialsList.setContractSplitId(contractSplitId);

            materialsList = materialsListMap.get(agreementMaterialsList.getMaterialsListId());
            biddingListQuotation = listQuotationMap.get(agreementMaterialsList.getMaterialsListId());

            // 设置供应商投标金额
            agreementMaterialsList.setVendorTaxRate(biddingListQuotation.getTaxRate());
            agreementMaterialsList.setVendorUnitPriceInclTax(biddingListQuotation.getTaxUnitPrice());
            agreementMaterialsList.setVendorUnitPriceExclTax(biddingListQuotation.getNotTaxUnitPrice());
            agreementMaterialsList.setVendorAmountInclTax(biddingListQuotation.getTaxPrice());
            agreementMaterialsList.setVendorAmountExclTax(biddingListQuotation.getNotTaxPrice());

            // 如果是新增，则使用供应商投标税率
            if (NumberUtil.isNullOrZero(agreementId)) {
                agreementMaterialsList.setSignTaxRate(biddingListQuotation.getTaxRate());
            }

            // 计算签订合同数据
            agreementMaterialsList.setSignUnitPriceExclTax(AmountCalUtil.calUnitPriceExclTax(agreementMaterialsList.getSignUnitPriceInclTax(),agreementMaterialsList.getSignTaxRate()));
            agreementMaterialsList.setSignAmountInclTax(AmountCalUtil.calTotalAmountInclTax(agreementMaterialsList.getSignCount(),agreementMaterialsList.getSignUnitPriceInclTax()));
            agreementMaterialsList.setSignAmountExclTax(AmountCalUtil.calTotalAmountExclTax(agreementMaterialsList.getSignAmountInclTax(),agreementMaterialsList.getSignTaxRate()));

            // 计算合同总金额
            totalAmountInclTax = NumberUtil.add(totalAmountInclTax,agreementMaterialsList.getSignAmountInclTax());
            totalAmountExclTax = NumberUtil.add(totalAmountExclTax,agreementMaterialsList.getSignAmountExclTax());

            // 设置物料的使用数量
            materialsList.setUsedCount(NumberUtil.add(materialsList.getUsedCount(),agreementMaterialsList.getSignCount()));
        }

        // 计算交易标的物
        String subjectMatterCode = materialsLists.stream()
                .map(MaterialsList::getSubjectMatterCode)
                .distinct()
                .collect(Collectors.joining(","));

        String subjectMatterName = materialsLists.stream()
                .map(MaterialsList::getSubjectMatterName)
                .distinct()
                .collect(Collectors.joining(","));

        return AgreementMaterialsInfoDTO.builder()
                .subjectMatterCode(subjectMatterCode)
                .subjectMatterName(subjectMatterName)
                .totalAmountInclTax(totalAmountInclTax)
                .totalAmountExclTax(totalAmountExclTax)
                .materialsLists(materialsLists)
                .build();
    }

    /**
     * 处理合同清单数据(易料合同)
     * @param agreementMaterialsLists
     * @param contractId
     */
    private AgreementMaterialsInfoDTO handleAgreementMaterialsByMarket(List<AgreementMaterialsList> agreementMaterialsLists, String contractId) {
        // 获取物料清单(采购计划清单)
        List<Long> materialsListIds = agreementMaterialsLists.stream().map(AgreementMaterialsList::getMaterialsListId).collect(Collectors.toList());
        List<MaterialsList> materialsLists = materialsListService.list(new LambdaQueryWrapper<MaterialsList>().in(BaseEntity::getId,materialsListIds));
        Map<Long,MaterialsList> materialsListMap = materialsLists.stream()
                .collect(Collectors.toMap(MaterialsList::getId,val -> val));

        // 获取供应商的投标物料清单
//        List<BiddingListQuotation> biddingListQuotations = biddingListQuotationService.listVendorBiddingListQuotation(schemeId,contractSplitId,vendorId);
//        Map<Long,BiddingListQuotation> listQuotationMap = biddingListQuotations.stream()
//                .collect(Collectors.toMap(BiddingListQuotation::getMaterialsId,val -> val));

        // 处理合同清单
        BiddingListQuotation biddingListQuotation;
        MaterialsList materialsList;
        BigDecimal totalAmountInclTax = BigDecimal.ZERO;
        BigDecimal totalAmountExclTax = BigDecimal.ZERO;

        for (AgreementMaterialsList agreementMaterialsList : agreementMaterialsLists) {
            // 设置合约拆分 id
//            agreementMaterialsList.setContractSplitId(contractSplitId);

            materialsList = materialsListMap.get(agreementMaterialsList.getMaterialsListId());
//            biddingListQuotation = listQuotationMap.get(agreementMaterialsList.getMaterialsListId());
//
//            // 设置供应商投标金额
//            agreementMaterialsList.setVendorTaxRate(biddingListQuotation.getTaxRate());
//            agreementMaterialsList.setVendorUnitPriceInclTax(biddingListQuotation.getTaxUnitPrice());
//            agreementMaterialsList.setVendorUnitPriceExclTax(biddingListQuotation.getNotTaxUnitPrice());
//            agreementMaterialsList.setVendorAmountInclTax(biddingListQuotation.getTaxPrice());
//            agreementMaterialsList.setVendorAmountExclTax(biddingListQuotation.getNotTaxPrice());

            // 如果是新增，则使用供应商投标税率
//            if (NumberUtil.isNullOrZero(agreementId)) {
//                agreementMaterialsList.setSignTaxRate(biddingListQuotation.getTaxRate());
//            }

            // 计算签订合同数据
            agreementMaterialsList.setSignUnitPriceExclTax(AmountCalUtil.calUnitPriceExclTax(agreementMaterialsList.getSignUnitPriceInclTax(),agreementMaterialsList.getSignTaxRate()));
            agreementMaterialsList.setSignAmountInclTax(AmountCalUtil.calTotalAmountInclTax(agreementMaterialsList.getSignCount(),agreementMaterialsList.getSignUnitPriceInclTax()));
            agreementMaterialsList.setSignAmountExclTax(AmountCalUtil.calTotalAmountExclTax(agreementMaterialsList.getSignAmountInclTax(),agreementMaterialsList.getSignTaxRate()));

            // 计算合同总金额
            totalAmountInclTax = NumberUtil.add(totalAmountInclTax,agreementMaterialsList.getSignAmountInclTax());
            totalAmountExclTax = NumberUtil.add(totalAmountExclTax,agreementMaterialsList.getSignAmountExclTax());

            // 设置物料的使用数量
            materialsList.setUsedCount(NumberUtil.add(materialsList.getUsedCount(),agreementMaterialsList.getSignCount()));
        }

        // 计算交易标的物
        String subjectMatterCode = materialsLists.stream()
                .map(MaterialsList::getSubjectMatterCode)
                .distinct()
                .collect(Collectors.joining(","));

        String subjectMatterName = materialsLists.stream()
                .map(MaterialsList::getSubjectMatterName)
                .distinct()
                .collect(Collectors.joining(","));

        return AgreementMaterialsInfoDTO.builder()
                .subjectMatterCode(subjectMatterCode)
                .subjectMatterName(subjectMatterName)
                .totalAmountInclTax(totalAmountInclTax)
                .totalAmountExclTax(totalAmountExclTax)
                .materialsLists(materialsLists)
                .build();
    }

    /**
     * 修改合同
     * @param requestVO
     * @return
     */
    private AgreementSaveVO updateAgreement(AgreementSaveRequestVO requestVO) {
        Agreement agreement = this.getById(requestVO.getAgreement().getId());
        ProcurementScheme procurementScheme = procurementSchemeService.getById(requestVO.getAgreement().getSchemeId());
        ValidateUtils.isNullException(agreement,"该合同不存在，请确认后重试");
        if (!(AgreementStateEnum.DRAFT.getState().equals(agreement.getAgreementState()) || AgreementStateEnum.REVOKED.getState().equals(agreement.getAgreementState()))) {
            throw new BusinessException("该状态下的合同不允许修改");
        }

        if (NumberUtil.isNullOrZero(agreement.getLabelAttachmentId())) {
            throw new BusinessException("合同附件还在生成中，请稍后重试");
        }
        if (!agreement.getCreateId().equals(SecurityUtils.getUserId())) {
            throw new BusinessException("不是您新建的合同，您无权编辑");
        }

        /*
         * 重新结算合同清单数据
         */
        AgreementMaterialsInfoDTO agreementMaterialsInfo;
        if (StringUtils.isEmpty(requestVO.getAgreement().getMarketMaterialContractId())) {
            agreementMaterialsInfo = handleAgreementMaterials(requestVO.getAgreementMaterialsLists(),agreement.getSchemeId(),agreement.getContractSplitId(),
                    agreement.getVendorId(),agreement.getId());
        } else {
            agreementMaterialsInfo = handleAgreementMaterialsByMarket(requestVO.getAgreementMaterialsLists(), String.valueOf(agreement.getId()));
        }

        // 合同签订总金额
        agreement.setTotalAmountIncTax(agreementMaterialsInfo.getTotalAmountInclTax());
        agreement.setTotalAmountExcTax(agreementMaterialsInfo.getTotalAmountExclTax());

        // 修改合同
        Agreement updateAgreement = requestVO.getAgreement();
        this.updateById(updateAgreement);

        // 合同款项信息
        agreementPaymentItemService.updateAgreementPaymentItem(requestVO.getAgreementPaymentItem(),agreement.getId());

        // 结算与付款节点信息
        agreementPaymentListService.updateAgreementPaymentList(requestVO.getAgreementPaymentLists(),agreement.getId());

        // 合同清单
        agreementMaterialsListService.updateAgreementMaterialsList(requestVO.getAgreementMaterialsLists(),agreement.getId());

        // 合同保证金
        agreementDepositService.updateAgreementDeposit(requestVO.getAgreementDeposits(),agreement.getId());

        // 合同-计日工
        agreementDailyWageService.updateAgreementDailyWage(requestVO.getAgreementDailyWageList(),agreement.getId());

        // 合同-机械台班
        agreementMachineShiftService.updateAgreementMachineShift(requestVO.getAgreementMachineShifts(),agreement.getId());

        // 合同-甲供设备清单
        agreementEquipmentSupplyService.updateAgreementEquipmentSupply(requestVO.getAgreementEquipmentSupplies(),agreement.getId());

        // 合同-甲供材料清单对象
        agreementMaterialSupplyService.updateAgreementMaterialSupply(requestVO.getAgreementMaterialSupplies(),agreement.getId());



        // 更新方法 占用合同数据量
        materialsListService.updateMaterialsListUsedCount(agreementMaterialsInfo.getMaterialsLists());

        /* 获取原来的价格，累加现在的价格后再减去上次保存的价格。使更新方法数据保持一致 */
        List<AgreementMaterialsList> agreementMaterialsLists = agreementMaterialsListService.list(new LambdaQueryWrapper<AgreementMaterialsList>().eq(AgreementMaterialsList::getAgreementId, agreement.getId()));
        // 更新方法  合约拆分是否已使用完毕
        if (StringUtils.isEmpty(requestVO.getAgreement().getMarketMaterialContractId())) {
            contractPlanningSplitService.updateContractPlanningSplitUseAdd(agreement.getContractSplitId(),agreementMaterialsInfo.getMaterialsLists(),requestVO.getAgreementMaterialsLists(),agreementMaterialsLists);
        }


        AgreementSaveVO saveVO = new AgreementSaveVO();
        saveVO.setId(agreement.getId());
        if (StringUtils.isEmpty(requestVO.getAgreement().getMarketMaterialContractId())) {
            saveVO.setProcurementPlanType(procurementScheme.getProcurementPlanType());
        } else {
            MarketMaterialContract contract = marketMaterialContractService.getById(requestVO.getAgreement().getMarketMaterialContractId());
            if(null != contract && null != contract.getExpenditureBusinessType()){
                saveVO.setProcurementPlanType(Integer.valueOf(contract.getExpenditureBusinessType()));
            }
        }
        return saveVO;
    }

    /**
     * 计算合同是否可操作
     * @param agreementState
     * @param createId
     * @param signatureUserId
     * @return
     */
    private Integer getAgreementIsOperate(Integer agreementState, Long createId, Long signatureUserId) {
        int isOperate;
        Long loginUserId = SecurityUtils.getUserId();
        if (AgreementStateEnum.PARTY_A_TO_SIGN.equalsState(agreementState) ||
                AgreementStateEnum.SIGN_SUCCESS.equalsState(agreementState)) {
            if (loginUserId.equals(signatureUserId)) {
                isOperate = 1;
            } else {
                isOperate = 0;
            }
        } else {
            if (loginUserId.equals(createId)) {
                isOperate = 1;
            } else {
                isOperate = 0;
            }
        }
        return isOperate;
    }

    /**
     * 校验保存时清单的数据
     *
     * @param agreementMaterialsLists
     * @param schemeId
     * @param contractSplitId
     * @param vendorId
     */
    private void checkSaveAgreementMaterialsList(List<AgreementMaterialsList> agreementMaterialsLists, Long schemeId, Long contractSplitId, Long vendorId) {
        List<AgreementMaterialsRequestVO> agreementMaterialsList = agreementMaterialsLists.stream()
                .map(x -> {
                    AgreementMaterialsRequestVO requestVO = new AgreementMaterialsRequestVO();
                    requestVO.setMaterialsListId(x.getMaterialsListId());/* 物料清单id */
                    requestVO.setSignCount(x.getSignCount());/* 签订数量 */
                    requestVO.setSignUnitPriceInclTax(x.getSignUnitPriceInclTax());/* 签订单价（含税） */
                    return requestVO;
                }).collect(Collectors.toList());
        checkAgreementMaterials(agreementMaterialsList,schemeId,contractSplitId,vendorId);
    }

    /**
     * 校验合同清单数据
     * @param agreementMaterialsList
     * @param schemeId
     * @param splitId
     * @param vendorId
     */
    private void checkAgreementMaterials(List<AgreementMaterialsRequestVO> agreementMaterialsList,Long schemeId,Long splitId,Long vendorId) {
        /* 物料清单id 集合 */
        Map<Long,AgreementMaterialsRequestVO> materialsRequestMap = agreementMaterialsList.stream()
                .collect(Collectors.toMap(AgreementMaterialsRequestVO::getMaterialsListId, val -> val));

        // 获取拆分清单 根据 合约采购拆分id 查询
        List<MaterialsList> materialsLists = materialsListService.listMaterialsListByContractSplitIds(CollectionUtil.newArrayList(splitId));
        // 获取供应商投标清单
        List<VendorBiddingListQuotationListVO> vendorBiddingListQuotationList = biddingListQuotationService.getVendorBiddingListQuotation(schemeId,splitId,vendorId)
                .getVendorBiddingListQuotationList();
        Map<Long,VendorBiddingListQuotationListVO> vendorBiddingMap = vendorBiddingListQuotationList.stream()
                .collect(Collectors.toMap(VendorBiddingListQuotationListVO::getMaterialsListId, val -> val));

        BigDecimal requestTotalAmount = BigDecimal.ZERO;
        BigDecimal surplusCount;
        AgreementMaterialsRequestVO materialsRequestVO;
        VendorBiddingListQuotationListVO listQuotationListVO;
        for (MaterialsList materialsList : materialsLists) {
            // 校验剩余数量
            materialsRequestVO = materialsRequestMap.get(materialsList.getId());
            if (materialsRequestVO == null) {
                throw new ParamValidateException(String.format("提交的清单中少了清单:%s,请确认",materialsList.getMaterialsName()));
            }

            surplusCount = NumberUtil.subtract(materialsList.getCount(),materialsList.getUsedCount());
            if (NumberUtil.compare(surplusCount,materialsRequestVO.getSignCount()) < 0) {
                throw new ParamValidateException(String.format("提交的清单[%s]数量[%s]超过剩余使用量[%s]，请重新输入",materialsList.getMaterialsName(),
                        NumberUtil.decimalFormat(materialsRequestVO.getSignCount(),4),
                        NumberUtil.decimalFormat(surplusCount,4)));
            }

            // 校验单价
            listQuotationListVO = vendorBiddingMap.get(materialsList.getId());
            if (NumberUtil.compare(materialsRequestVO.getSignUnitPriceInclTax(),listQuotationListVO.getTaxUnitPrice()) > 0) {
                throw new ParamValidateException(String.format("提交的清单[%s]含税单价[%s]大于供应商的中标单价[%s]，请重新输入",materialsList.getMaterialsName(),
                        NumberUtil.decimalFormat(materialsRequestVO.getSignUnitPriceInclTax(),4),
                        NumberUtil.decimalFormat(listQuotationListVO.getTaxUnitPrice(),4)));
            }

            requestTotalAmount = NumberUtil.add(requestTotalAmount,AmountCalUtil.calTotalAmountInclTax(materialsRequestVO.getSignCount(),materialsRequestVO.getSignUnitPriceInclTax()));
        }

        // 校验总金额
        ContractPlanningSplit contractPlanningSplit = contractPlanningSplitService.getById(splitId);
        BigDecimal totalSurplusAmount = NumberUtil.subtract(contractPlanningSplit.getTotalPlanAmount(),contractPlanningSplit.getTotalUsedAmount());
        if (totalSurplusAmount.compareTo(requestTotalAmount) < 0) {
            throw new ParamValidateException(String.format("输入的清单总金额[%s]大于剩余可用金额[%s]",NumberUtil.decimalFormat(requestTotalAmount,4),NumberUtil.decimalFormat(totalSurplusAmount,4)));
        }
    }

    /**
     * [修改时] 校验保存时清单的数据
     *
     * @param agreementMaterialsLists
     * @param schemeId
     * @param contractSplitId
     * @param vendorId
     */
    private void checkSaveAgreementMaterialsListByUpdate(List<AgreementMaterialsList> agreementMaterialsLists, Long schemeId, Long contractSplitId, Long vendorId, Long id) {
        List<AgreementMaterialsRequestVO> agreementMaterialsList = agreementMaterialsLists.stream()
                .map(x -> {
                    AgreementMaterialsRequestVO requestVO = new AgreementMaterialsRequestVO();
                    requestVO.setMaterialsListId(x.getMaterialsListId());/* 物料清单id */
                    requestVO.setSignCount(x.getSignCount());/* 签订数量 */
                    requestVO.setSignUnitPriceInclTax(x.getSignUnitPriceInclTax());/* 签订单价（含税） */
                    return requestVO;
                }).collect(Collectors.toList());
        checkAgreementMaterialsByUpdate(agreementMaterialsList,schemeId,contractSplitId,vendorId,id);
    }

    /**
     * [修改时] 校验合同清单数据
     * @param agreementMaterialsList
     * @param schemeId
     * @param splitId
     * @param vendorId
     */
    private void checkAgreementMaterialsByUpdate(List<AgreementMaterialsRequestVO> agreementMaterialsList,Long schemeId,Long splitId,Long vendorId, Long id) {
        /* 物料清单id 集合 */
        Map<Long,AgreementMaterialsRequestVO> materialsRequestMap = agreementMaterialsList.stream()
                .collect(Collectors.toMap(AgreementMaterialsRequestVO::getMaterialsListId, val -> val));

        // 获取拆分清单 根据 合约采购拆分id 查询
        List<MaterialsList> materialsLists = materialsListService.listMaterialsListByContractSplitIds(CollectionUtil.newArrayList(splitId));


        /* 获取原来的价格，累加现在的价格后再减去原来的价格。使验证正常通过 */
        List<AgreementMaterialsList> agreementMaterialsListsOld = agreementMaterialsListService.list(new LambdaQueryWrapper<AgreementMaterialsList>().eq(AgreementMaterialsList::getAgreementId, id));
        if(agreementMaterialsListsOld!=null){
            /* 格式化签订清单数据 */
            AgreementMaterialsInfoDTO agreementMaterialsInfo = handleAgreementMaterials(agreementMaterialsListsOld,schemeId,splitId,vendorId,id);
            if(agreementMaterialsInfo!=null){
                List<MaterialsList> materialsListsOld = agreementMaterialsInfo.getMaterialsLists();
                if(materialsListsOld!=null){
                    for (int i = 0; i < materialsLists.size(); i++) {
                        for (int j = 0; j < materialsListsOld.size(); j++) {
                            /* id一样时 */
                            if(materialsLists.get(i).getId().equals(materialsListsOld.get(j).getId())){
                                /* 减去上次的 签订单价，得到之前的原始单价，为了过下面的校验规则。 */
                                materialsLists.get(i).setUsedCount( NumberUtil.subtract(materialsLists.get(i).getUsedCount(),materialsListsOld.get(j).getUsedCount()));
                            }
                        }
                    }
                }
            }
        }

        // 获取供应商投标清单
        List<VendorBiddingListQuotationListVO> vendorBiddingListQuotationList = biddingListQuotationService.getVendorBiddingListQuotation(schemeId,splitId,vendorId)
                .getVendorBiddingListQuotationList();
        Map<Long,VendorBiddingListQuotationListVO> vendorBiddingMap = vendorBiddingListQuotationList.stream()
                .collect(Collectors.toMap(VendorBiddingListQuotationListVO::getMaterialsListId, val -> val));

        BigDecimal requestTotalAmount = BigDecimal.ZERO;
        BigDecimal surplusCount;
        AgreementMaterialsRequestVO materialsRequestVO;
        VendorBiddingListQuotationListVO listQuotationListVO;
        for (MaterialsList materialsList : materialsLists) {
            // 校验剩余数量
            materialsRequestVO = materialsRequestMap.get(materialsList.getId());
            if (materialsRequestVO == null) {
                throw new ParamValidateException(String.format("提交的清单中少了清单:%s,请确认",materialsList.getMaterialsName()));
            }

            surplusCount = NumberUtil.subtract(materialsList.getCount(),materialsList.getUsedCount());
            if (NumberUtil.compare(surplusCount,materialsRequestVO.getSignCount()) < 0) {
                throw new ParamValidateException(String.format("提交的清单[%s]数量[%s]超过剩余使用量[%s]，请重新输入",materialsList.getMaterialsName(),
                        NumberUtil.decimalFormat(materialsRequestVO.getSignCount(),4),
                        NumberUtil.decimalFormat(surplusCount,4)));
            }

            // 校验供应商提交的单价 与 原物料单价对比
            listQuotationListVO = vendorBiddingMap.get(materialsList.getId());
            if (NumberUtil.compare(materialsRequestVO.getSignUnitPriceInclTax(),listQuotationListVO.getTaxUnitPrice()) > 0) {
                throw new ParamValidateException(String.format("提交的清单[%s]含税单价[%s]大于供应商的中标单价[%s]，请重新输入",materialsList.getMaterialsName(),
                        NumberUtil.decimalFormat(materialsRequestVO.getSignUnitPriceInclTax(),4),
                        NumberUtil.decimalFormat(listQuotationListVO.getTaxUnitPrice(),4)));
            }

            requestTotalAmount = NumberUtil.add(requestTotalAmount,AmountCalUtil.calTotalAmountInclTax(materialsRequestVO.getSignCount(),materialsRequestVO.getSignUnitPriceInclTax()));
        }

        // 校验总金额
        ContractPlanningSplit contractPlanningSplit = contractPlanningSplitService.getById(splitId);

        /* 将这一次的减去 */
        BigDecimal totalSurplusAmount = NumberUtil.subtract(contractPlanningSplit.getTotalPlanAmount(),contractPlanningSplit.getTotalUsedAmount());

        /* 将上一次的加上 */
        if (agreementMaterialsListsOld != null) {
            for (AgreementMaterialsList agreementMaterials : agreementMaterialsListsOld) {
                totalSurplusAmount = NumberUtil.add(totalSurplusAmount,agreementMaterials.getSignAmountInclTax());
            }
        }
        if (totalSurplusAmount.compareTo(requestTotalAmount) < 0) {
            throw new ParamValidateException(String.format("输入的清单总金额[%s]大于剩余可用金额[%s]",NumberUtil.decimalFormat(requestTotalAmount,4),NumberUtil.decimalFormat(totalSurplusAmount,4)));
        }
    }
}
