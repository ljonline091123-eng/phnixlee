package com.zhaocai.business.agreement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.agreement.domain.MarketMaterialContract;
import com.zhaocai.business.agreement.domain.MarketMaterialList;
import com.zhaocai.business.agreement.mapper.MarketMaterialContractMapper;
import com.zhaocai.business.agreement.service.IMarketMaterialContractService;
import com.zhaocai.business.agreement.service.IMarketMaterialListService;
import com.zhaocai.business.agreement.vo.req.MarketMaterialContractQueryVO;
import com.zhaocai.business.agreement.vo.res.AgreementCreateBaseInfoVO;
import com.zhaocai.business.agreement.vo.res.MarketMaterialContractListVO;
import com.zhaocai.business.bidding.vo.res.VendorBiddingListQuotationListVO;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.utils.AmountCalUtil;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.manager.http.dto.req.MarketMaterialListQuoteRequestDTO;
import com.zhaocai.business.manager.http.dto.res.MinProjectDetailResponseDTO;
import com.zhaocai.business.manager.http.service.ContractPlanService;
import com.zhaocai.business.procurement.domain.MaterialsList;
import com.zhaocai.business.procurement.domain.ProcurementPlan;
import com.zhaocai.business.procurement.service.IMaterialsListService;
import com.zhaocai.business.procurement.service.IProcurementPlanService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        baseInfoVO.setVendorId(Long.valueOf(contract.getVendorId()));
        baseInfoVO.setBelongAccountingItem(contract.getBelongAccountingItem());
        baseInfoVO.setBelongAccountingItemCode(contract.getBelongAccountingItemCode());
        baseInfoVO.setBusinessType(planInfo.getProcurementType());
        baseInfoVO.setExpenditureBusinessType(contract.getExpenditureBusinessType());
        baseInfoVO.setPriceType(planInfo.getPriceType());

        // 查询项目详情
        MinProjectDetailResponseDTO projectDetail = contractPlanService.getMinProjectDetail(contract.getBelongAccountingItem());
        baseInfoVO.setBelongOrganizationId(projectDetail.getBelongingOrgId());
        baseInfoVO.setBelongOrganizationName(getDeptName(projectDetail.getBelongingOrgId()));
        baseInfoVO.setAgreementPerformAddress(contract.getAgreementPerformAddress());
        baseInfoVO.setAgreementPerformCountry("中国");
        baseInfoVO.setAgreementPerformDistrict(contract.getAgreementPerformDistrict());
        baseInfoVO.setPartyAOrgId(projectDetail.getManagementOrgId());
        baseInfoVO.setPartyAName(getDeptName(projectDetail.getManagementOrgId()));

        // 供应商
        baseInfoVO.setPartyBName(contract.getPartyBName());
        baseInfoVO.setPartyBLegalName(contract.getPartyBLegalName());
        baseInfoVO.setPartyBLegalPhone(contract.getPartyBLegalPhone());
        baseInfoVO.setPartyBLegalIdCard(contract.getPartyBLegalIdCard());

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
            vo.setMaterialsListId(Long.valueOf(marketMaterial.getRequireId()));
            vo.setMaterialsCode(marketMaterial.getQuoteNo());
            vo.setMaterialsName(marketMaterial.getQuoteName());
            vo.setSpecification(marketMaterial.getCategory());
            vo.setUnitMeasurement(marketMaterial.getUnitName());
            vo.setCount(marketMaterial.getQuantity());
            vo.setSignCount(marketMaterial.getQuantity());
            vo.setTaxUnitPrice(marketMaterial.getPrice());
            vo.setSignUnitPriceInclTax(marketMaterial.getPrice());
            vo.setNotTaxUnitPrice(marketMaterial.getNoTaxPrice());
            vo.setSignUnitPriceExclTax(marketMaterial.getNoTaxPrice());
            vo.setTaxRate(marketMaterial.getTaxRate());
            vo.setSignTaxRate(marketMaterial.getTaxRate());

            // 含税金额 = 含税单价 * 数量
            BigDecimal taxPrice = AmountCalUtil.calTotalAmountInclTax(vo.getCount(), vo.getTaxUnitPrice());
            vo.setTaxPrice(taxPrice);
            vo.setSignAmountInclTax(taxPrice);
            // 不含税金额 = 含税金额 / (1 * 税率%)
            BigDecimal notTaxPrice = AmountCalUtil.calTotalAmountExclTax(taxPrice, vo.getTaxRate());
            vo.setNotTaxPrice(notTaxPrice);
            vo.setSignAmountExclTax(notTaxPrice);
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

}
