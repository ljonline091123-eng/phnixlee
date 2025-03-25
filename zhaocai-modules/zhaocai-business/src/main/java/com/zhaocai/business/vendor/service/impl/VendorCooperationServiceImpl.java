package com.zhaocai.business.vendor.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.csp.sentinel.util.StringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhaocai.business.agreement.domain.Agreement;
import com.zhaocai.business.agreement.service.IAgreementService;
import com.zhaocai.business.common.enums.ProcurementPlanTypeEnum;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.manager.http.dto.res.ContractListDTO;
import com.zhaocai.business.manager.http.service.PerformanceEvaluationService;
import com.zhaocai.business.pub.service.IOrganizationService;
import com.zhaocai.business.pub.vo.res.OrganizationVO;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.domain.VendorPerformanceEvaluation;
import com.zhaocai.business.vendor.mapper.VendorCooperationMapper;
import com.zhaocai.business.vendor.service.IVendorCooperationService;
import com.zhaocai.business.vendor.service.IVendorPerformanceEvaluationService;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.business.vendor.vo.req.VendorCooperationAgreementListQueryVO;
import com.zhaocai.business.vendor.vo.req.VendorCooperationListQueryVO;
import com.zhaocai.business.vendor.vo.req.VendorCooperativePartnerListQueryVO;
import com.zhaocai.business.vendor.vo.res.VendorCooperationAgreementVO;
import com.zhaocai.business.vendor.vo.res.VendorCooperationListVO;
import com.zhaocai.business.vendor.vo.res.VendorCooperativePartnerListVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 供应商合作记录服务
 *
 * @author chenming
 * @date 2024-06-25
 */
@Service
public class VendorCooperationServiceImpl implements IVendorCooperationService {

    @Autowired
    private VendorCooperationMapper vendorCooperationMapper;

    @Autowired
    private IAgreementService agreementService;

    @Autowired
    private PerformanceEvaluationService performanceEvaluationService;

    @Autowired
    private IVendorPerformanceEvaluationService vendorPerformanceEvaluationService;

    @Autowired
    private IOrganizationService organizationService;

    @Autowired
    private IVendorService vendorService;

    @Override
    public PageResult<VendorCooperationListVO> listPage(VendorCooperationListQueryVO queryVO) {
        IPage<VendorCooperationListVO> page = vendorCooperationMapper.selectVendorCooperationList(queryVO.toMybatisPage(),queryVO);
        page.getRecords().forEach(x -> {
            if (x.getCooperationAmount() == null) {
                x.setCooperationAmount(BigDecimal.ZERO);
            }

            if (x.getExcellentNum() == null) {
                x.setExcellentNum(0L);
            }
        });
        return new PageResult<>(page);
    }

    @Override
    public PageResult<VendorCooperativePartnerListVO> listVendorCooperativePartner(VendorCooperativePartnerListQueryVO queryVO) {
        IPage<VendorCooperativePartnerListVO> pages = vendorCooperationMapper.selectVendorCooperativePartner(queryVO.toMybatisPage(),queryVO);
        return new PageResult<>(pages);
    }

    @Override
    public List<VendorCooperationAgreementVO> listDetail(VendorCooperationAgreementListQueryVO queryVO) {
        Vendor vendor = vendorService.getById(queryVO.getVendorId());
        ValidateUtils.isNullException(vendor,"该供应商不存在，请确认");

        // 获取合同数据
        List<Agreement> agreementList = agreementService.list(new LambdaQueryWrapper<Agreement>()
                .eq(Agreement::getVendorId,queryVO.getVendorId())
                .eq(queryVO.getExpenditureBusinessType() != null,Agreement::getExpenditureBusinessType,queryVO.getExpenditureBusinessType())
                .orderByDesc(Agreement::getAgreementSignDate));

        // 获取合同收付款信息
        List<ContractListDTO> contractList = performanceEvaluationService.listContractList(queryVO.getVendorId(),queryVO.getExpenditureBusinessType());
         Map<String,ContractListDTO> contractListMap = contractList.stream()
                .collect(Collectors.toMap(ContractListDTO::getConCode, Function.identity(),(v1,v2) -> v2));

         // 获取供应商的履约评价
        List<VendorPerformanceEvaluation> performanceEvaluationList = vendorPerformanceEvaluationService.getAndSyncVendorPerformanceList(queryVO.getVendorId());
        Map<String,VendorPerformanceEvaluation> performanceEvaluationMap = performanceEvaluationList.stream()
                .collect(Collectors.toMap(VendorPerformanceEvaluation::getAgreementCode, Function.identity(),(v1,v2) -> v2));
        // 拼凑结果数据
        Map<String,List<VendorCooperationAgreementVO>> agreementMap = agreementList.stream()
                .collect(Collectors.groupingBy(
                        Agreement::getPartyAOrgId,
                        Collectors.mapping(agreement -> {
                            VendorCooperationAgreementVO cooperationAgreement = new VendorCooperationAgreementVO();
                            cooperationAgreement.setVendorName(vendor.getEnterpriseName());
                            cooperationAgreement.setAgreementName(agreement.getAgreementName());
                            cooperationAgreement.setTotalAmountIncTax(agreement.getTotalAmountIncTax());
                            cooperationAgreement.setAgreementSignDate(agreement.getAgreementSignDate());
                            cooperationAgreement.setAgreementId(agreement.getId());
                            cooperationAgreement.setPartyAName(agreement.getPartyAName());
                            cooperationAgreement.setPartyAContactName(agreement.getPartyAContactName());
                            cooperationAgreement.setPartyAContactPhone(agreement.getPartyAContactPhone());
                            cooperationAgreement.setExpenditureBusinessType(agreement.getExpenditureBusinessType());
                            cooperationAgreement.setExpenditureBusinessTypeText(ProcurementPlanTypeEnum.getValueByCode(agreement.getExpenditureBusinessType()));

                            // 处理合同收付款
                            ContractListDTO listDTO = contractListMap.get(agreement.getAgreementCode());
                            if (listDTO != null) {
                                cooperationAgreement.setSettledAmount(listDTO.getTotalTaxSettleAmount());
                                cooperationAgreement.setPaidAmount(listDTO.getTotalActualPaymentAmount());
                                cooperationAgreement.setUnpaidAmount(NumberUtil.subtract(listDTO.getTotalTaxSettleAmount(),listDTO.getTotalActualPaymentAmount()));
                            } else {
                                cooperationAgreement.setSettledAmount(BigDecimal.ZERO);
                                cooperationAgreement.setPaidAmount(BigDecimal.ZERO);
                                cooperationAgreement.setUnpaidAmount(BigDecimal.ZERO);
                            }

                            // 处理供应商履约评价
                            VendorPerformanceEvaluation performanceEvaluation = performanceEvaluationMap.get(agreement.getAgreementCode());
                            if (performanceEvaluation != null) {
                                cooperationAgreement.setExcellentNum(performanceEvaluation.getExcellentNum());
                                cooperationAgreement.setGoodNum(performanceEvaluation.getGoodNum());
                                cooperationAgreement.setQualifiedNum(performanceEvaluation.getQualifiedNum());
                                cooperationAgreement.setBadNum(performanceEvaluation.getBadNum());
                            } else {
                                cooperationAgreement.setExcellentNum(BigDecimal.ZERO);
                                cooperationAgreement.setGoodNum(BigDecimal.ZERO);
                                cooperationAgreement.setQualifiedNum(BigDecimal.ZERO);
                                cooperationAgreement.setBadNum(BigDecimal.ZERO);
                            }

                            return  cooperationAgreement;
                        },Collectors.toList())
                ));

        // 获取机构数据
        List<OrganizationVO> organizationList = organizationService.getOriganizationTreeUnXList();

        // 构建结果集
        List<VendorCooperationAgreementVO> resultList = new ArrayList<>();
        AtomicInteger numberId = new AtomicInteger(1000);
        for (OrganizationVO organization : organizationList) {
            VendorCooperationAgreementVO cooperationAgreement = buildCooperationAgreement(organization,agreementMap,numberId);
            resultList.add(cooperationAgreement);
        }

        // 过滤数据掉非空数据
        processAgreements(resultList);

        // 将子集金额累加到父类金额上面
        processAgreementAmount(resultList);

        return resultList;
    }

    /**
     * 将子集金额累加到父类金额上面
     * @param resultList
     */
    private void processAgreementAmount(List<VendorCooperationAgreementVO> resultList) {
        if (CollectionUtil.isNotEmpty(resultList)) {
            for (VendorCooperationAgreementVO agreementVO : resultList) {
                processAgreementAmount(agreementVO);
            }
        }
    }

    private void processAgreementAmount(VendorCooperationAgreementVO cooperationAgreementVO) {
        if (cooperationAgreementVO.getTotalAmountIncTax() == null) {
            cooperationAgreementVO.setTotalAmountIncTax(BigDecimal.ZERO);
        }
        if (cooperationAgreementVO.getSettledAmount() == null) {
            cooperationAgreementVO.setSettledAmount(BigDecimal.ZERO);
        }
        if (cooperationAgreementVO.getPaidAmount() == null) {
            cooperationAgreementVO.setPaidAmount(BigDecimal.ZERO);
        }
        if (cooperationAgreementVO.getUnpaidAmount() == null) {
            cooperationAgreementVO.setUnpaidAmount(BigDecimal.ZERO);
        }
        if (StringUtil.isEmpty(cooperationAgreementVO.getAgreementName())) {
            cooperationAgreementVO.setChildrenNum(new BigDecimal(0));
        }else{
            cooperationAgreementVO.setChildrenNum(new BigDecimal(1));
        }

        if (CollectionUtil.isNotEmpty(cooperationAgreementVO.getChildren())) {
            for (VendorCooperationAgreementVO children : cooperationAgreementVO.getChildren()) {
                processAgreementAmount(children);

                if (children.getTotalAmountIncTax() != null) {
                    cooperationAgreementVO.setTotalAmountIncTax(cooperationAgreementVO.getTotalAmountIncTax().add(children.getTotalAmountIncTax()));
                }
                if (children.getSettledAmount() != null) {
                    cooperationAgreementVO.setSettledAmount(cooperationAgreementVO.getSettledAmount().add(children.getSettledAmount()));
                }
                if (children.getPaidAmount() != null) {
                    cooperationAgreementVO.setPaidAmount(cooperationAgreementVO.getPaidAmount().add(children.getPaidAmount()));
                }
                if (children.getUnpaidAmount() != null) {
                    cooperationAgreementVO.setUnpaidAmount(cooperationAgreementVO.getUnpaidAmount().add(children.getUnpaidAmount()));
                }
                if(children.getChildrenNum() != null ){
                    cooperationAgreementVO.setChildrenNum(cooperationAgreementVO.getChildrenNum().add(children.getChildrenNum()));
                }
            }
        }
    }

    /**
     * 过滤掉空数据
     * @param resultList
     * @return
     */
    private static void processAgreements(List<VendorCooperationAgreementVO> resultList) {
        Iterator<VendorCooperationAgreementVO> iterator = resultList.iterator();
        while (iterator.hasNext()) {
            VendorCooperationAgreementVO agreement = iterator.next();
            if (StringUtils.isBlank(agreement.getAgreementName()) && CollectionUtil.isEmpty(agreement.getChildren())) {
                // 移除子节点
                iterator.remove();
            } else if(StringUtils.isBlank(agreement.getAgreementName()) && CollectionUtil.isNotEmpty(agreement.getChildren())) {
                // 处理子类
                processAgreements(agreement.getChildren());

                if (CollectionUtil.isEmpty(agreement.getChildren())) {
                    iterator.remove();
                }
            }
        }
    }


    /**
     * 构建结果集
     * @param organization
     * @param agreementMap
     * @param numberId
     * @return
     */
    private VendorCooperationAgreementVO buildCooperationAgreement(OrganizationVO organization, Map<String, List<VendorCooperationAgreementVO>> agreementMap,
                                                                   AtomicInteger numberId) {
        VendorCooperationAgreementVO cooperationAgreement = new VendorCooperationAgreementVO();
        cooperationAgreement.setCooperativePartnerName(organization.getOrganizationName());
        cooperationAgreement.setId(numberId.incrementAndGet());

        List<VendorCooperationAgreementVO> agreementList = agreementMap.get(organization.getOrganizationCode());
        if (CollectionUtil.isNotEmpty(agreementList)) {
            for (VendorCooperationAgreementVO agreementVO : agreementList) {
                agreementVO.setId(numberId.incrementAndGet());
            }
            cooperationAgreement.setChildren(agreementList);
            //cooperationAgreement.setAgreementName(agreementList.size()+"");
        } else {
            cooperationAgreement.setChildren(new ArrayList<>());
        }

        if (CollectionUtil.isNotEmpty(organization.getChildren())) {
            for (OrganizationVO organization1 : organization.getChildren()) {
                cooperationAgreement.addChildren(buildCooperationAgreement(organization1,agreementMap,numberId));
            }
        }
        return cooperationAgreement;
    }

}
