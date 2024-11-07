package com.zhaocai.business.agreement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson.JSONArray;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.AgreementMaterialsList;
import com.zhaocai.business.agreement.domain.MarketMaterialContract;
import com.zhaocai.business.agreement.domain.MarketMaterialList;
import com.zhaocai.business.agreement.mapper.MarketMaterialContractMapper;
import com.zhaocai.business.agreement.service.IMarketMaterialContractService;
import com.zhaocai.business.agreement.service.IMarketMaterialListService;
import com.zhaocai.business.agreement.vo.req.MarketMaterialContractQueryVO;
import com.zhaocai.business.agreement.vo.res.AgreementCreateBaseInfoVO;
import com.zhaocai.business.agreement.vo.res.MarketMaterialContractListVO;
import com.zhaocai.business.bidding.vo.res.VendorBiddingListQuotationListVO;
import com.zhaocai.business.common.cache.DictBizCache;
import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.common.enums.FileZTaskBusinessEnum;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.AmountCalUtil;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.filez.service.IFileZTaskService;
import com.zhaocai.business.manager.http.dto.req.MarketMaterialListQuoteRequestDTO;
import com.zhaocai.business.manager.http.dto.res.MinProjectDetailResponseDTO;
import com.zhaocai.business.manager.http.service.ContractPlanService;
import com.zhaocai.business.procurement.domain.*;
import com.zhaocai.business.procurement.service.IContractPlanningSplitService;
import com.zhaocai.business.procurement.service.IMaterialsListService;
import com.zhaocai.business.procurement.service.IProcurementPlanService;
import com.zhaocai.business.pub.domain.AreaDivision;
import com.zhaocai.business.pub.service.IAreaDivisionService;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 易料采购合同信息Service业务层处理
 *
 * @author lsn
 * @date 2024-10-22
 */
@Service
public class MarketMaterialContractServiceImpl extends ServiceImpl<MarketMaterialContractMapper, MarketMaterialContract> implements IMarketMaterialContractService {

    @Autowired
    private IMarketMaterialListService marketMaterialListService;

    @Autowired
    private ContractPlanService contractPlanService;

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Autowired
    private IMaterialsListService materialsListService;

    @Lazy
    @Autowired
    private IProcurementPlanService procurementPlanService;

    @Autowired
    private IAttachmentService attachmentService;

    @Autowired
    private IFileZTaskService fileZTaskService;

    @Autowired
    private IAreaDivisionService areaDivisionService;

    @Autowired
    private IContractPlanningSplitService contractPlanningSplitService;

    /**
     * 接收采购清单最终报价
     * @param requestDTO
     */
    @Override
    public void saveContract(MarketMaterialListQuoteRequestDTO requestDTO) {
        MarketMaterialContract contract = BeanCopierUtil.copyBean(requestDTO, MarketMaterialContract.class);
        List<MarketMaterialList> list = BeanCopierUtil.copyList(requestDTO.getList(), MarketMaterialList.class);
        //删除易料采购合同清单数据
        marketMaterialListService.remove(new LambdaQueryWrapper<MarketMaterialList>()
                .eq(MarketMaterialList::getContractId, contract.getId()));
        if(!CollectionUtil.isEmpty(list)){
            list.forEach(i->i.setContractId(contract.getId()));
            marketMaterialListService.saveOrUpdateBatch(list);
        }
        super.saveOrUpdate(contract);
    }

    /**
     * 获取可签订的易料采购合同
     * @param queryVO
     * @return
     */
    @Override
    public PageResult<MarketMaterialContractListVO> listMarketMaterialContract(MarketMaterialContractQueryVO queryVO) {
        IPage<MarketMaterialContractListVO> pages = baseMapper.listMarketMaterialContract(queryVO.toMybatisPage(), queryVO);
        return new PageResult<>(pages);
    }

    /**
     * 获取创建合同的基本信息
     * @param queryVO
     * @return
     */
    @Override
    public AgreementCreateBaseInfoVO getAgreementCreateInfo(MarketMaterialContractQueryVO queryVO) {
        // 易料采购合同
        MarketMaterialContract contract = super.getById(queryVO.getId());
        ValidateUtils.isNullException(contract,"该易料采购合同不存在");

        // 采购计划信息
        ProcurementPlan planInfo = procurementPlanService.getById(contract.getPlanId());
        ValidateUtils.isNullException(planInfo,"该易料采购合同对应的采购计划不存在");

        AgreementCreateBaseInfoVO baseInfoVO = new AgreementCreateBaseInfoVO();
        baseInfoVO.setMarketMaterialContractId(queryVO.getId());
        baseInfoVO.setVendorId(Long.valueOf(contract.getVendorId()));
        baseInfoVO.setBelongAccountingItem(contract.getBelongAccountingItem());
        baseInfoVO.setBelongAccountingItemCode(contract.getBelongAccountingItemCode());
        baseInfoVO.setBusinessType(Integer.valueOf(contract.getExpenditureBusinessType()));
        baseInfoVO.setExpenditureBusinessType(DictBizCache.getValue(DictBizEnum.PROCUREMENT_PLAN_TYPE,String.valueOf(contract.getExpenditureBusinessType())));
        baseInfoVO.setPriceType(planInfo.getPriceType());

        // 查询项目详情
        MinProjectDetailResponseDTO projectDetail = contractPlanService.getMinProjectDetail(contract.getBelongAccountingItemCode());
        baseInfoVO.setBelongAccountingItem(projectDetail.getMinAccountFullName());
        baseInfoVO.setBelongOrganizationId(projectDetail.getBelongingOrgId());
        baseInfoVO.setBelongOrganizationName(getDeptName(projectDetail.getBelongingOrgId()));
        baseInfoVO.setAgreementPerformAddress(contract.getAgreementPerformAddress());
        baseInfoVO.setAgreementPerformCountry("中国");
        //baseInfoVO.setAgreementPerformDistrict(contract.getAgreementPerformDistrict());
        baseInfoVO.setAgreementPerformDistrict(getAgreementPerformDistrict(contract.getAgreementPerformDistrict()));
        baseInfoVO.setPartyAOrgId(projectDetail.getManagementOrgId());
        baseInfoVO.setPartyAName(getDeptName(projectDetail.getManagementOrgId()));

        // 供应商
        baseInfoVO.setPartyBName(contract.getPartyBName());
        baseInfoVO.setPartyBLegalName(contract.getPartyBLegalName());
        baseInfoVO.setPartyBLegalPhone(contract.getPartyBLegalPhone());
        baseInfoVO.setPartyBLegalIdCard(contract.getPartyBLegalIdCard());
        baseInfoVO.setPartyBResponsibleName(contract.getPartyBResponsibleName());
        baseInfoVO.setPartyBResponsibleIdCard(contract.getPartyBResponsibleIdCard());
        baseInfoVO.setPartyBResponsiblePhone(contract.getPartyBLegalPhone());

        // 物料清单
        List<MarketMaterialList> list = marketMaterialListService.list(new LambdaQueryWrapper<MarketMaterialList>().eq(MarketMaterialList::getContractId, queryVO.getId()));

        // 处理清单数据
        BigDecimal totalAmountIncTax = BigDecimal.ZERO;
        BigDecimal totalAmountExcTax = BigDecimal.ZERO;

        List <VendorBiddingListQuotationListVO> quotationList = new ArrayList<>();
        for (MarketMaterialList marketMaterial : list) {
            // 获取合同清单对应的采购计划的清单信息
            MaterialsList materials = materialsListService.getById(marketMaterial.getRequireId());
            if (materials == null) {
                throw new ParamValidateException(String.format("易料采购合同清单对应的采购计划清单未找到,请确认",marketMaterial.getQuoteName()));
            }
            VendorBiddingListQuotationListVO vo =  BeanCopierUtil.copyBean(materials, VendorBiddingListQuotationListVO.class);
            vo.setCostAccount(materials.getCostAccountName());
            vo.setMaterialsListId(Long.valueOf(marketMaterial.getRequireId()));
            vo.setOfferGoodsCode(materials.getCode());
            vo.setGoodsName(materials.getName());
            vo.setOfferBrand(materials.getOfferBrand());
            vo.setOfferPrice(materials.getOfferPrice());
            vo.setSkuId(marketMaterial.getSkuId());
            vo.setCount(marketMaterial.getQuantity().setScale(2, RoundingMode.HALF_UP));
            vo.setSignCount(marketMaterial.getQuantity().setScale(2, RoundingMode.HALF_UP));
            vo.setNotTaxUnitPrice(marketMaterial.getNoTaxPrice().setScale(2, RoundingMode.HALF_UP));
            vo.setSignUnitPriceExclTax(marketMaterial.getNoTaxPrice().setScale(2, RoundingMode.HALF_UP));
            vo.setTaxRate(marketMaterial.getTaxRate().setScale(2, RoundingMode.HALF_UP));
            vo.setSignTaxRate(marketMaterial.getTaxRate().setScale(2, RoundingMode.HALF_UP));

            BigDecimal taxUnitPrice = marketMaterial.getNoTaxPrice().multiply(marketMaterial.getTaxRate().divide(BigDecimal.valueOf(100))).add(marketMaterial.getNoTaxPrice());
            vo.setTaxUnitPrice(taxUnitPrice.setScale(2, RoundingMode.HALF_UP));
            vo.setSignUnitPriceInclTax(taxUnitPrice.setScale(2, RoundingMode.HALF_UP));
            // 含税金额 = 含税单价 * 数量
            BigDecimal taxPrice = AmountCalUtil.calTotalAmountInclTax(vo.getCount(), vo.getTaxUnitPrice());
            vo.setTaxPrice(taxPrice.setScale(2, RoundingMode.HALF_UP));
            vo.setSignAmountInclTax(taxPrice.setScale(2, RoundingMode.HALF_UP));
            // 不含税金额 = 含税金额 / (1 * 税率%)
            BigDecimal notTaxPrice = AmountCalUtil.calTotalAmountExclTax(taxPrice, vo.getTaxRate());
            vo.setNotTaxPrice(notTaxPrice.setScale(2, RoundingMode.HALF_UP));
            vo.setSignAmountExclTax(notTaxPrice.setScale(2, RoundingMode.HALF_UP));
            // 税额 = 含税金额 - 不含税金额
            BigDecimal taxAmount = AmountCalUtil.calTaxAmount(taxPrice, notTaxPrice);
            vo.setTaxAmount(taxAmount);
            totalAmountIncTax = NumberUtil.add(totalAmountIncTax,vo.getSignAmountInclTax());
            totalAmountExcTax = NumberUtil.add(totalAmountExcTax,vo.getSignAmountExclTax());
            quotationList.add(vo);
        }

        quotationList.forEach(i->i.setPaymentType(planInfo.getPriceType() == null ? "" : planInfo.getPriceType().toString()));
        // 购买材料，设置价款类型、交易标的物类型
        baseInfoVO.setBiddingListQuotation(quotationList);
        baseInfoVO.setTotalAmountIncTax(totalAmountIncTax.setScale(2, RoundingMode.DOWN));
        baseInfoVO.setTotalAmountExcTax(totalAmountExcTax.setScale(2, RoundingMode.DOWN));
        baseInfoVO.setSubjectMatterName(planInfo.getSubjectMatterName());
        baseInfoVO.setSubjectMatterType(planInfo.getSubjectMatterType());

        // 合同附件 (前端选择)
//        long attachmentId = agreementCreateAttachmentHandle(scheme.getId());
//        baseInfoVO.setAttachmentId(attachmentId);

        return baseInfoVO;
    }

    /**
     * 处理新增合同时的附件<br>
     * 1. 为合同选定的合同模板新增一个附件
     * 2. 为合同附件增加一个联想文档任务
     * @param attachmentVO
     * @return
     */
    @Override
    public long agreementCreateAttachmentHandle(AttachmentVO attachmentVO) {
        ValidateUtils.isNullException(attachmentVO,"该合同没有选择模板，请确认");

        // 新增附件
        AttachmentRequestVO attachmentRequestVO = new AttachmentRequestVO(attachmentVO.getFileName(),attachmentVO.getFileUrl());
        long attachmentId = attachmentService.addAttachment(attachmentRequestVO, AttachmentTypeEnum.AGREEMENT_ORIGINAL,null);

        // 新增任务
        fileZTaskService.addInitialFileZTask(FileZTaskBusinessEnum.AGREEMENT_CREATE,attachmentId);

        return attachmentId;
    }

    /**
     * 校验创建合同基本信息
     * @param requestVO
     * @return
     */
    @Override
    public Boolean checkAgreementCreateInfo(MarketMaterialContractQueryVO requestVO) {
        // 易料合同
        MarketMaterialContract marketContract = super.getById(requestVO.getId());
        ValidateUtils.isNullException(marketContract,"该易料合同不存在，请确认");

        // 合同清单
        List<MarketMaterialList> list = marketMaterialListService.list(new LambdaQueryWrapper<MarketMaterialList>()
                .eq(MarketMaterialList::getContractId, marketContract.getId()));
        ValidateUtils.isNullException(list,"该易料合同关联的合同清单不存在，请确认");

        // 采购计划
        ProcurementPlan plan = procurementPlanService.getById(marketContract.getPlanId());
        ValidateUtils.isNullException(plan,"该易料合同关联的采购计划不存在，请确认");

        if (StringUtils.isBlank(marketContract.getBelongAccountingItemCode())) {
            throw new ParamValidateException("合同对应的归属最小核算项目为空，请确认");
        }

        // 校验最小核算项目相关数据
        MinProjectDetailResponseDTO projectDetail = contractPlanService.getMinProjectDetail(marketContract.getBelongAccountingItemCode());
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
        checkAgreementMaterials(list);
        return true;
    }

    /**
     * 校验清单数量
     * @param list
     */
    @Override
    public void checkAgreementMaterials(List<MarketMaterialList> list) {
        BigDecimal requestTotalAmount = BigDecimal.ZERO;
        MaterialsList materials;
        BigDecimal surplusCount;
        Map<Long, BigDecimal> totalAmount = new HashMap<>();
        for (MarketMaterialList materialsList : list) {
            // 校验剩余数量(和采购计划清单做对比)
            materials = materialsListService.getById(materialsList.getRequireId());
            if (materials == null) {
                throw new ParamValidateException(String.format("易料合同对应的合同清单不存在对应的采购计划清单:%s,请确认",materialsList.getGoodsName()));
            }

            surplusCount = NumberUtil.subtract(materials.getCount(),materials.getUsedCount());
            if (NumberUtil.compare(surplusCount,materialsList.getQuantity()) < 0) {
                throw new ParamValidateException(String.format("易料合同对应的合同清单[%s]数量[%s]超过剩余使用量[%s]，请重新输入",materialsList.getGoodsName(),
                        NumberUtil.decimalFormat(materialsList.getQuantity(),4),
                        NumberUtil.decimalFormat(surplusCount,4)));
            }
            // 汇总合约拆分金额
            if (null == totalAmount.get(materials.getContractSplitId())) {
                requestTotalAmount = NumberUtil.add(BigDecimal.ZERO,AmountCalUtil.calTotalAmountInclTax(materialsList.getQuantity(),materialsList.getPrice()));
            } else {
                requestTotalAmount = NumberUtil.add(totalAmount.get(materials.getContractSplitId()),AmountCalUtil.calTotalAmountInclTax(materialsList.getQuantity(),materialsList.getPrice()));
            }
            totalAmount.put(materials.getContractSplitId(), requestTotalAmount);
        }

        // 校验总金额（和合约规划拆分做对比）
        totalAmount.forEach((contractSplitId, amount) -> {
            ContractPlanningSplit contractPlanningSplit = contractPlanningSplitService.getById(contractSplitId);
            BigDecimal totalSurplusAmount = NumberUtil.subtract(contractPlanningSplit.getTotalPlanAmount(),contractPlanningSplit.getTotalUsedAmount());
            if (totalSurplusAmount.compareTo(amount) < 0) {
                throw new ParamValidateException(String.format("易料合同对应的合同清单总金额[%s]大于剩余可用金额[%s]",NumberUtil.decimalFormat(amount,4),NumberUtil.decimalFormat(totalSurplusAmount,4)));
            }
        });
    }

    @Override
    public void checkAgreementMaterialsByUpdate(List<AgreementMaterialsList> materialsList, List<AgreementMaterialsList> materialsListsOld) {
        // 上一次合同清单的列表及合约拆分汇总金额
        Map<Long,AgreementMaterialsList> materialsOldMap = materialsListsOld.stream()
                .collect(Collectors.toMap(AgreementMaterialsList::getMaterialsListId,val -> val));
        Map<Long, BigDecimal> materialsAmountOldMap = materialsListsOld.stream().collect(Collectors.groupingBy(
                AgreementMaterialsList::getContractSplitId,
                Collectors.reducing(BigDecimal.ZERO, AgreementMaterialsList::getSignAmountInclTax, BigDecimal::add)
                ));
        // 本次合同清单的合约拆分汇总金额
        Map<Long, BigDecimal> materialsAmountMap = materialsList.stream().collect(Collectors.groupingBy(
                AgreementMaterialsList::getContractSplitId,
                Collectors.reducing(BigDecimal.ZERO, AgreementMaterialsList::getSignAmountInclTax, BigDecimal::add)
        ));

        // 校验数量
        MaterialsList materials;
        AgreementMaterialsList materialsOld;
        BigDecimal surplusCount;
        for (AgreementMaterialsList materialsVO : materialsList) {
            // 校验剩余数量(和采购计划清单做对比)
            materials = materialsListService.getById(materialsVO.getMaterialsListId());
            materialsOld = materialsOldMap.get(materialsVO.getMaterialsListId());
            if (materials == null) {
                throw new ParamValidateException(String.format("易料合同对应的合同清单不存在对应的采购计划清单:%s,请确认",materialsVO.getGoodsName()));
            }
            surplusCount = NumberUtil.subtract(materials.getCount(), materials.getUsedCount());
            surplusCount = NumberUtil.add(surplusCount, materialsOld.getSignCount());
            if (NumberUtil.compare(surplusCount,materialsVO.getSignCount()) < 0) {
                throw new ParamValidateException(String.format("易料合同对应的合同清单[%s]数量[%s]超过剩余使用量[%s]，请重新输入",materialsVO.getGoodsName(),
                        NumberUtil.decimalFormat(materialsVO.getSignCount(),4),
                        NumberUtil.decimalFormat(surplusCount,4)));
            }
        }

        // 校验总金额（和合约规划拆分做对比）
        materialsAmountMap.forEach((contractSplitId, amount) -> {
            ContractPlanningSplit contractPlanningSplit = contractPlanningSplitService.getById(contractSplitId);
            BigDecimal totalSurplusAmount = NumberUtil.subtract(contractPlanningSplit.getTotalPlanAmount(),contractPlanningSplit.getTotalUsedAmount());
            totalSurplusAmount = NumberUtil.add(totalSurplusAmount, materialsAmountOldMap.get(contractPlanningSplit));
            if (totalSurplusAmount.compareTo(amount) < 0) {
                throw new ParamValidateException(String.format("易料合同对应的合同清单总金额[%s]大于剩余可用金额[%s]",NumberUtil.decimalFormat(amount,4),NumberUtil.decimalFormat(totalSurplusAmount,4)));
            }
        });
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
            List<String> addrList = new ArrayList<>();
            try {
                addrList = JSONArray.parseArray(prjAddr, String.class);
            } catch (Exception e) {
                // 处理解析异常，例如记录日志
                e.printStackTrace();
                addrList.add(prjAddr); // 如果解析失败，直接使用原始字符串
            }
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

}
