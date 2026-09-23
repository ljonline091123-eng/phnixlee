package com.zhaocai.business.receipt.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.receipt.domain.*;
import com.zhaocai.business.receipt.enums.*;
import com.zhaocai.business.receipt.service.*;
import com.zhaocai.business.receipt.vo.req.*;
import com.zhaocai.business.receipt.vo.res.query.ReceiptContractSettlementQueryVO;
import com.zhaocai.business.vendor.domain.Vendor;
import com.zhaocai.business.vendor.service.IVendorService;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhaocai.business.receipt.mapper.ReceiptContractSettlementMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

/**
 * 推送结算单 Service业务层处理
 *
 * @author cff
 * @date 2024-09-07
 */
@Service
public class ReceiptContractSettlementServiceImpl extends ServiceImpl<ReceiptContractSettlementMapper, ReceiptContractSettlement> implements IReceiptContractSettlementService {
    @Autowired
    private IVendorService vendorService;

    @Autowired
    private ReceiptContractSettlementMapper pushContractSettlementMapper;

    /**
     * 计日工明细
     */
    @Autowired
    private IReceiptContractSettleDatallService iPushContractSettleDatallService;

    /**
     * 押金、保证金信息明细
     */
    @Autowired
    private IReceiptContractSettleDepositService iPushContractSettleDepositService;

    /**
     * 供应商评价
     */
    @Autowired
    private IReceiptContractSettleEvaluateService iPushContractSettleEvaluateService;

    /**
     * 清单结算明细
     */
    @Autowired
    private IReceiptContractSettleListService iPushContractSettleListService;

    /**
     * 机械台班明细
     */
    @Autowired
    private IReceiptContractSettleMechanicalService iPushContractSettleMechanicalService;

    /**
     * 其他款项结算明细
     */
    @Autowired
    private IReceiptContractSettleOtherPaymentService iPushContractSettleOtherPaymentService;

    /**
     * 零星包工明细
     */
    @Autowired
    private IReceiptContractSettleSporadicWorkService iPushContractSettleSporadicWorkService;

    /**
     * 修改结算单状态
     *
     * @param settlementVO 修改结算单状态
     * @return 结果
     */
    @Override
    public boolean modifyContractsettlementState(ReceiptContractSettlementVO settlementVO) {
        return baseMapper.modifyContractsettlementState(settlementVO);
    }

    @Override
    public ReceiptContractSettlementVO detail(Long id) {
        //该结算单详情
        ReceiptContractSettlement receiptContractSettlement = super.getById(id);
        ValidateUtils.isNullException(receiptContractSettlement,"该结算单不存在");
        ReceiptContractSettlementVO settlementVO=new ReceiptContractSettlementVO();
        BeanUtils.copyBeanProp(settlementVO,receiptContractSettlement);
        //明细列表
        receiptContractSettlementDetail(receiptContractSettlement, settlementVO);

        //操作状态
        settlementVO.setStateName(StringUtils.isNotEmpty(settlementVO.getState())? OperationalStateEnum.getValueByCode(settlementVO.getState())!=null
                ?OperationalStateEnum.getValueByCode(settlementVO.getState()):settlementVO.getState():null);
        return settlementVO;
    }

    /**
     * 明细列表
     * @param receiptContractSettlement
     * @param settlementVO
     */
    private void receiptContractSettlementDetail(ReceiptContractSettlement receiptContractSettlement, ReceiptContractSettlementVO settlementVO) {
        //计日工明细
        ReceiptContractSettleDatallVO contractSettleDatallVO=new ReceiptContractSettleDatallVO();
        contractSettleDatallVO.setParntId(String.valueOf(receiptContractSettlement.getId()));
        List<ReceiptContractSettleDatallVO> contractSettleDatallerList=iPushContractSettleDatallService.selectList(contractSettleDatallVO);
        contractSettleDatallerEnum(contractSettleDatallerList);
        settlementVO.setContractSettleDatallerList(contractSettleDatallerList);

        //押金、保证金信息明细
        ReceiptContractSettleDepositVO contractSettleDepositVO=new ReceiptContractSettleDepositVO();
        contractSettleDepositVO.setParntId(String.valueOf(receiptContractSettlement.getId()));
        List<ReceiptContractSettleDepositVO> contractSettleDepositList=iPushContractSettleDepositService.selectList(contractSettleDepositVO);
        contractSettleDepositEnum(contractSettleDepositList);
        settlementVO.setContractSettleDepositList(contractSettleDepositList);

        //供应商评价
        ReceiptContractSettleEvaluateVO contractSettleEvaluateVO=new ReceiptContractSettleEvaluateVO();
        contractSettleEvaluateVO.setParntId(String.valueOf(receiptContractSettlement.getId()));
        List<ReceiptContractSettleEvaluateVO> contractSettleEvaluateList=iPushContractSettleEvaluateService.selectList(contractSettleEvaluateVO);
        settlementVO.setContractSettleEvaluateList(contractSettleEvaluateList);

        //清单结算明细
        ReceiptContractSettleListVO contractSettleListVO=new ReceiptContractSettleListVO();
        contractSettleListVO.setParntId(String.valueOf(receiptContractSettlement.getId()));
        List<ReceiptContractSettleListVO> contractSettleLists=iPushContractSettleListService.selectList(contractSettleListVO);
        contractSettleListEnum(contractSettleLists);
        settlementVO.setContractSettleLists(contractSettleLists);

        //机械台班明细
        ReceiptContractSettleMechanicalVO contractSettleMechanicalVO=new ReceiptContractSettleMechanicalVO();
        contractSettleMechanicalVO.setParntId(String.valueOf(receiptContractSettlement.getId()));
        List<ReceiptContractSettleMechanicalVO> contractSettleMechanicalTableList=iPushContractSettleMechanicalService.selectList(contractSettleMechanicalVO);
        contractSettleMechanicalEnum(contractSettleMechanicalTableList);
        settlementVO.setContractSettleMechanicalTableList(contractSettleMechanicalTableList);

        //其他款项结算明细
        ReceiptContractSettleOtherPaymentVO otherPaymentVO=new ReceiptContractSettleOtherPaymentVO();
        otherPaymentVO.setParntId(String.valueOf(receiptContractSettlement.getId()));
        List<ReceiptContractSettleOtherPaymentVO> contractSettleOtherPaymentList=iPushContractSettleOtherPaymentService.selectList(otherPaymentVO);
        contractSettleOtherPaymentEnum(contractSettleOtherPaymentList);
        settlementVO.setContractSettleOtherPaymentList(contractSettleOtherPaymentList);

        //零星包工明细
        ReceiptContractSettleSporadicWorkVO sporadicWorkVO=new ReceiptContractSettleSporadicWorkVO();
        sporadicWorkVO.setParntId(String.valueOf(receiptContractSettlement.getId()));
        List<ReceiptContractSettleSporadicWorkVO> contractSettleSporadicWorkList=iPushContractSettleSporadicWorkService.selectList(sporadicWorkVO);
        contractSettleSporadicWork(contractSettleSporadicWorkList);
        settlementVO.setContractSettleSporadicWorkList(contractSettleSporadicWorkList);
    }

    /**
     * 零星包工枚举转换
     * @param contractSettleSporadicWorkList
     */
    private void contractSettleSporadicWork(List<ReceiptContractSettleSporadicWorkVO> contractSettleSporadicWorkList) {
        if(!CollectionUtils.isEmpty(contractSettleSporadicWorkList)) {
            contractSettleSporadicWorkList.stream().map(tem -> {
                //成本归集方式
                tem.setCostCollectionMode(StringUtils.isNotEmpty(tem.getCostCollectionMode())? CostCollectionMethodEnum.getValueByCode(tem.getCostCollectionMode())!=null
                        ?CostCollectionMethodEnum.getValueByCode(tem.getCostCollectionMode()):tem.getCostCollectionMode():null);
                return tem;
            }).collect(Collectors.toList());
        }
    }

    /**
     * 其他款项结算枚举转换
     * @param contractSettleOtherPaymentList
     */
    private void contractSettleOtherPaymentEnum(List<ReceiptContractSettleOtherPaymentVO> contractSettleOtherPaymentList) {
        if(!CollectionUtils.isEmpty(contractSettleOtherPaymentList)) {
            contractSettleOtherPaymentList.stream().map(tem -> {
                //成本归集方式
                tem.setCostCollectionMode(StringUtils.isNotEmpty(tem.getCostCollectionMode())? CostCollectionMethodEnum.getValueByCode(tem.getCostCollectionMode())!=null
                        ?CostCollectionMethodEnum.getValueByCode(tem.getCostCollectionMode()):tem.getCostCollectionMode():null);
                return tem;
            }).collect(Collectors.toList());
        }
    }

    /**
     * 机械台班枚举转换
     * @param contractSettleMechanicalTableList
     */
    private void contractSettleMechanicalEnum(List<ReceiptContractSettleMechanicalVO> contractSettleMechanicalTableList) {
        if(!CollectionUtils.isEmpty(contractSettleMechanicalTableList)) {
            contractSettleMechanicalTableList.stream().map(tem -> {
                //成本归集方式
                tem.setCostCollectionMode(StringUtils.isNotEmpty(tem.getCostCollectionMode())? CostCollectionMethodEnum.getValueByCode(tem.getCostCollectionMode())!=null
                        ?CostCollectionMethodEnum.getValueByCode(tem.getCostCollectionMode()):tem.getCostCollectionMode():null);
                return tem;
            }).collect(Collectors.toList());
        }
    }

    /**
     * 清单结算枚举转换
     * @param contractSettleLists
     */
    private void contractSettleListEnum(List<ReceiptContractSettleListVO> contractSettleLists) {
        if(!CollectionUtils.isEmpty(contractSettleLists)) {
            contractSettleLists.stream().map(tem -> {
                //成本归集方式
                tem.setCostCollectionMode(StringUtils.isNotEmpty(tem.getCostCollectionMode())? CostCollectionMethodEnum.getValueByCode(tem.getCostCollectionMode())!=null
                        ?CostCollectionMethodEnum.getValueByCode(tem.getCostCollectionMode()):tem.getCostCollectionMode():null);
                //租赁方式
                tem.setRentMode(StringUtils.isNotEmpty(tem.getRentMode())? RentalMethodEnum.getValueByCode(tem.getRentMode())!=null
                        ?RentalMethodEnum.getValueByCode(tem.getRentMode()):tem.getRentMode():null);
                return tem;
            }).collect(Collectors.toList());
        }
    }

    /**
     * 押金、保证金枚举转换
     * @param contractSettleDepositList
     */
    private void contractSettleDepositEnum(List<ReceiptContractSettleDepositVO> contractSettleDepositList) {
        if(!CollectionUtils.isEmpty(contractSettleDepositList)) {
            contractSettleDepositList.stream().map(tem -> {
                //押金、保证金基数
                tem.setBaseType(StringUtils.isNotEmpty(tem.getBaseType())? BaseEnum.getValueByCode(tem.getBaseType())!=null
                        ?BaseEnum.getValueByCode(tem.getBaseType()):tem.getBaseType():null);
                //押金、保证金方式
                tem.setDepositMode(StringUtils.isNotEmpty(tem.getDepositMode())? DepositSecurityDepositMethodEnum.getValueByCode(tem.getDepositMode())!=null
                        ?DepositSecurityDepositMethodEnum.getValueByCode(tem.getDepositMode()):tem.getDepositMode():null);
                //押金、保证金类型
                tem.setDepositType(StringUtils.isNotEmpty(tem.getDepositType())? DepositSecurityDepositMethodTypeEnum.getValueByCode(tem.getDepositType())!=null
                        ?DepositSecurityDepositMethodTypeEnum.getValueByCode(tem.getDepositType()):tem.getDepositType():null);
                return tem;
            }).collect(Collectors.toList());
        }
    }

    /**
     * 计日工明细枚举
     * @param contractSettleDatallerList
     */
    private void contractSettleDatallerEnum(List<ReceiptContractSettleDatallVO> contractSettleDatallerList) {
        if(!CollectionUtils.isEmpty(contractSettleDatallerList)) {
            contractSettleDatallerList.stream().map(tem -> {
                //成本归集方式
                tem.setCostCollectionMode(StringUtils.isNotEmpty(tem.getCostCollectionMode())? CostCollectionMethodEnum.getValueByCode(tem.getCostCollectionMode())!=null
                        ?CostCollectionMethodEnum.getValueByCode(tem.getCostCollectionMode()):tem.getCostCollectionMode():null);
                //工种名称
                tem.setWorkType(StringUtils.isNotEmpty(tem.getWorkType())? JobTitleEnum.getValueByCode(tem.getWorkType())!=null
                        ?JobTitleEnum.getValueByCode(tem.getWorkType()):tem.getWorkType():null);
                return tem;
            }).collect(Collectors.toList());
        }
    }

    /**
     * 结算单列表
     * @param queryDTO
     * @return
     */
    @Override
    public PageResult<ReceiptContractSettlementVO> contractsettlementListPage(ReceiptContractSettlementQueryVO queryDTO) {
        queryDTO.setSupplierId(String.valueOf(getVendor(SecurityUtils.getUserId()).getId()));
        IPage<ReceiptContractSettlementVO> pages = baseMapper.selectListPge(queryDTO.toMybatisPage(),queryDTO);
        if(!CollectionUtils.isEmpty(pages.getRecords())){
            pages.getRecords().stream()
                    .map(tem -> {
                        //支出业务分类
                        tem.setConType(StringUtils.isNotEmpty(tem.getConType())?ConTypeEnum.getValueByCode(tem.getConType())!=null
                                ?ConTypeEnum.getValueByCode(tem.getConType()):tem.getConType():null);
                        //发票类型
                        tem.setInvoiceType(StringUtils.isNotEmpty(tem.getInvoiceType())?ConTypeEnum.getValueByCode(tem.getInvoiceType())!=null
                                ?ConTypeEnum.getValueByCode(tem.getInvoiceType()):tem.getInvoiceType():null);
                        //流程状态
                        tem.setProcStatus(StringUtils.isNotEmpty(tem.getProcStatus())?ConTypeEnum.getValueByCode(tem.getProcStatus())!=null
                                ?ConTypeEnum.getValueByCode(tem.getProcStatus()):tem.getProcStatus():null);
                        //结算类型
                        tem.setSettleType(StringUtils.isNotEmpty(tem.getSettleType())?ConTypeEnum.getValueByCode(tem.getSettleType())!=null
                                ?ConTypeEnum.getValueByCode(tem.getSettleType()):tem.getSettleType():null);
                        //操作状态
                        tem.setStateName(StringUtils.isNotEmpty(tem.getState())? OperationalStateEnum.getValueByCode(tem.getState())!=null
                                ?OperationalStateEnum.getValueByCode(tem.getState()):tem.getState():null);
                        return tem;
                    })
                    .collect(Collectors.toList());
        }
        return new PageResult<>(pages);
    }
    private Vendor getVendor(Long userId){
        return vendorService.getByLoginUser(userId);
    }
    /**
     *
     * @param pushContractSettlementVO 新增推送结算单
     * @return
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW,rollbackFor = Exception.class)
    public boolean insertPushContractSettlement(ReceiptContractSettlementVO pushContractSettlementVO) {
        ReceiptContractSettlementVO purchaseVO=new ReceiptContractSettlementVO();
        purchaseVO.setThirdId(String.valueOf(pushContractSettlementVO.getId()));
        pushContractSettlementVO.setThirdId(String.valueOf(pushContractSettlementVO.getId()));
        pushContractSettlementVO.setId(null);
        boolean flat=this.save(pushContractSettlementVO);
        if(flat){
            List<ReceiptContractSettlementVO> selectList=pushContractSettlementMapper.selectList(purchaseVO);

            //计日工明细
            List<ReceiptContractSettleDatallVO> detailVos=pushContractSettlementVO.getContractSettleDatallerList();
            insertPushContractSettleDatall(selectList, detailVos);

            //押金、保证金信息明细
            List<ReceiptContractSettleDepositVO> depositList=pushContractSettlementVO.getContractSettleDepositList();
            insertPushContractSettleDeposit(selectList,depositList);

            //供应商评价
            List<ReceiptContractSettleEvaluateVO> evaluateList=pushContractSettlementVO.getContractSettleEvaluateList();
            insertPushContractSettleEvaluate(selectList,evaluateList);

            //清单结算明细
            List<ReceiptContractSettleListVO> settleListList=pushContractSettlementVO.getContractSettleLists();
            insertPushContractSettle(selectList,settleListList);

            //机械台班明细
            List<ReceiptContractSettleMechanicalVO> mechanicalList=pushContractSettlementVO.getContractSettleMechanicalTableList();
            insertPushContractSettleMechanical(selectList,mechanicalList);

            //其他款项结算明细
            List<ReceiptContractSettleOtherPaymentVO> otherPaymentList=pushContractSettlementVO.getContractSettleOtherPaymentList();
            insertOtherPayment(selectList,otherPaymentList);

            //零星包工明细
            List<ReceiptContractSettleSporadicWorkVO> sporadicWorkList=pushContractSettlementVO.getContractSettleSporadicWorkList();
            insertSporadicWorks(selectList,sporadicWorkList);
        }
        return flat;
    }

    /**
     * 计日工明细
     * @param selectList
     * @param sporadicWorkVOList
     */
    private void insertSporadicWorks(List<ReceiptContractSettlementVO> selectList, List<ReceiptContractSettleSporadicWorkVO> sporadicWorkVOList) {
        if (!CollectionUtils.isEmpty(sporadicWorkVOList)){
            if(!CollectionUtils.isEmpty(selectList)){
                ReceiptContractSettlement pushPurchaseDetail=new ReceiptContractSettlement();
                BeanUtils.copyBeanProp(pushPurchaseDetail,selectList.get(0));
                String id=String.valueOf(pushPurchaseDetail.getId());
                List<ReceiptContractSettleSporadicWork> purchaseDetailList = sporadicWorkVOList.stream()
                        .map(tem -> {
                            ReceiptContractSettleSporadicWork detail=new ReceiptContractSettleSporadicWork();
                            BeanUtils.copyBeanProp(detail,tem);
                            detail.setThirdId(String.valueOf(tem.getId()));
                            detail.setParntId(id);
                            detail.setId(null);
                            return detail;
                        })
                        .collect(Collectors.toList());
                iPushContractSettleSporadicWorkService.insertSporadicWorksSaveBatch(purchaseDetailList);
            }
        }
    }

    /**
     * 押金、保证金信息明细
     * @param selectList
     * @param otherPaymentList
     */
    private void insertOtherPayment(List<ReceiptContractSettlementVO> selectList, List<ReceiptContractSettleOtherPaymentVO> otherPaymentList) {
        if (!CollectionUtils.isEmpty(otherPaymentList)){
            if(!CollectionUtils.isEmpty(selectList)){
                ReceiptContractSettlement pushPurchaseDetail=new ReceiptContractSettlement();
                BeanUtils.copyBeanProp(pushPurchaseDetail,selectList.get(0));
                String id=String.valueOf(pushPurchaseDetail.getId());
                List<ReceiptContractSettleOtherPayment> purchaseDetailList = otherPaymentList.stream()
                        .map(tem -> {
                            ReceiptContractSettleOtherPayment detail=new ReceiptContractSettleOtherPayment();
                            BeanUtils.copyBeanProp(detail,tem);
                            detail.setThirdId(String.valueOf(tem.getId()));
                            detail.setParntId(id);
                            detail.setId(null);
                            return detail;
                        })
                        .collect(Collectors.toList());
                iPushContractSettleOtherPaymentService.insertOtherPaymentListSaveBatch(purchaseDetailList);
            }
        }
    }

    /**
     * 供应商评价
     * @param selectList
     * @param mechanicalVOList
     */
    private void insertPushContractSettleMechanical(List<ReceiptContractSettlementVO> selectList, List<ReceiptContractSettleMechanicalVO> mechanicalVOList) {
        if (!CollectionUtils.isEmpty(mechanicalVOList)){
            if(!CollectionUtils.isEmpty(selectList)){
                ReceiptContractSettlement pushPurchaseDetail=new ReceiptContractSettlement();
                BeanUtils.copyBeanProp(pushPurchaseDetail,selectList.get(0));
                String id=String.valueOf(pushPurchaseDetail.getId());
                List<ReceiptContractSettleMechanical> purchaseDetailList = mechanicalVOList.stream()
                        .map(tem -> {
                            ReceiptContractSettleMechanical detail=new ReceiptContractSettleMechanical();
                            BeanUtils.copyBeanProp(detail,tem);
                            detail.setThirdId(String.valueOf(tem.getId()));
                            detail.setParntId(id);
                            detail.setId(null);
                            return detail;
                        })
                        .collect(Collectors.toList());
                iPushContractSettleMechanicalService.insertPushContractSettleMechanicalSaveBatch(purchaseDetailList);
            }
        }
    }

    /**
     * 清单结算明细
     * @param selectList
     * @param settleListVOList
     */
    private void insertPushContractSettle(List<ReceiptContractSettlementVO> selectList, List<ReceiptContractSettleListVO> settleListVOList) {
        if (!CollectionUtils.isEmpty(settleListVOList)){
            if(!CollectionUtils.isEmpty(selectList)){
                ReceiptContractSettlement pushPurchaseDetail=new ReceiptContractSettlement();
                BeanUtils.copyBeanProp(pushPurchaseDetail,selectList.get(0));
                String id=String.valueOf(pushPurchaseDetail.getId());
                List<ReceiptContractSettleList> purchaseDetailList = settleListVOList.stream()
                        .map(tem -> {
                            ReceiptContractSettleList detail=new ReceiptContractSettleList();
                            BeanUtils.copyBeanProp(detail,tem);
                            detail.setParntId(id);
                            return detail;
                        })
                        .collect(Collectors.toList());
                iPushContractSettleListService.insertPushContractSettleListSaveBatch(purchaseDetailList);
            }
        }
    }
    /**
     * 机械台班明细
     * @param selectList
     * @param evaluateVOList
     */
    private void insertPushContractSettleEvaluate(List<ReceiptContractSettlementVO> selectList, List<ReceiptContractSettleEvaluateVO> evaluateVOList) {
        if (!CollectionUtils.isEmpty(evaluateVOList)){
            if(!CollectionUtils.isEmpty(selectList)){
                ReceiptContractSettlement pushPurchaseDetail=new ReceiptContractSettlement();
                BeanUtils.copyBeanProp(pushPurchaseDetail,selectList.get(0));
                String id=String.valueOf(pushPurchaseDetail.getId());
                List<ReceiptContractSettleEvaluate> purchaseDetailList = evaluateVOList.stream()
                        .map(tem -> {
                            ReceiptContractSettleEvaluate detail=new ReceiptContractSettleEvaluate();
                            BeanUtils.copyBeanProp(detail,tem);
                            detail.setParntId(id);
                            return detail;
                        })
                        .collect(Collectors.toList());
                iPushContractSettleEvaluateService.insertPushContractSettleEvaluateSaveBatch(purchaseDetailList);
            }
        }
    }
    /**
     * 其他款项结算明细
     * @param selectList
     * @param depositVOList
     */
    private void insertPushContractSettleDeposit(List<ReceiptContractSettlementVO> selectList, List<ReceiptContractSettleDepositVO> depositVOList) {
        if (!CollectionUtils.isEmpty(depositVOList)){
            if(!CollectionUtils.isEmpty(selectList)){
                ReceiptContractSettlement pushPurchaseDetail=new ReceiptContractSettlement();
                BeanUtils.copyBeanProp(pushPurchaseDetail,selectList.get(0));
                String id=String.valueOf(pushPurchaseDetail.getId());
                List<ReceiptContractSettleDeposit> purchaseDetailList = depositVOList.stream()
                        .map(tem -> {
                            ReceiptContractSettleDeposit detail=new ReceiptContractSettleDeposit();
                            BeanUtils.copyBeanProp(detail,tem);
                            detail.setThirdId(String.valueOf(tem.getId()));
                            detail.setParntId(id);
                            detail.setId(null);
                            return detail;
                        })
                        .collect(Collectors.toList());
                iPushContractSettleDepositService.insertPushContractSettleDepositSaveBatch(purchaseDetailList);
            }
        }
    }
    /**
     * 零星包工明细
     * @param selectList
     * @param detailVos
     * @param
     */
    private void insertPushContractSettleDatall(List<ReceiptContractSettlementVO> selectList, List<ReceiptContractSettleDatallVO> detailVos) {
        if (!CollectionUtils.isEmpty(detailVos)){
            if(!CollectionUtils.isEmpty(selectList)){
                ReceiptContractSettlement pushContractSettlement=new ReceiptContractSettlement();
                BeanUtils.copyBeanProp(pushContractSettlement,selectList.get(0));
                String id=String.valueOf(pushContractSettlement.getId());
                List<ReceiptContractSettleDatall> purchaseDetailList = detailVos.stream()
                        .map(tem -> {
                            ReceiptContractSettleDatall detail=new ReceiptContractSettleDatall();
                            BeanUtils.copyBeanProp(detail,tem);
                            detail.setThirdId(String.valueOf(tem.getId()));
                            detail.setParntId(id);
                            detail.setId(null);
                            return detail;
                        })
                        .collect(Collectors.toList());
                iPushContractSettleDatallService.insertPushContractSettleDatallSaveBatch(purchaseDetailList);
            }
        }
    }
}
