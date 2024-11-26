package com.zhaocai.business.manager.http.service;

import cn.hutool.core.collection.CollectionUtil;
import com.alibaba.fastjson2.JSON;
import com.zhaocai.business.common.conver.ProcurementPlanTypeConver;
import com.zhaocai.business.common.enums.ProcurementPlanTypeEnum;
import com.zhaocai.business.common.enums.RentModeEnum;
import com.zhaocai.business.common.utils.ValidationUtils;
import com.zhaocai.business.manager.http.common.config.UnderlingPlatformUrlEnum;
import com.zhaocai.business.manager.http.dto.req.*;
import com.zhaocai.business.manager.http.dto.res.ContractPlanListDTO;
import com.zhaocai.business.manager.http.dto.res.ContractPlanMaterialListDTO;
import com.zhaocai.business.manager.http.dto.res.MinProjectDetailResponseDTO;
import com.zhaocai.business.manager.http.dto.res.UpdatePlanQuantityAmountResponseDTO;
import com.zhaocai.business.procurement.domain.MaterialsList;
import com.zhaocai.business.procurement.vo.req.ContractPlanMaterialListQueryVO;
import com.zhaocai.business.procurement.vo.req.ContractPlanningListQueryVO;
import com.zhaocai.business.procurement.vo.res.ContractMaterialsListVO;
import com.zhaocai.business.procurement.vo.res.ContractPlanningListVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.Constants;
import com.zhaocai.common.core.utils.NumberUtil;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 合约规划服务
 *
 * @author chenming
 * @date 2024-07-09
 */
@Slf4j
@Service
public class ContractPlanService {

    /**
     * 获取合约规划
     * @param queryVO
     * @return
     */
    public PageResult<ContractPlanningListVO> getContractPlanningList(ContractPlanningListQueryVO queryVO) {
        // 校验参数
        ValidationUtils.validateObject(queryVO);

        // 构建请求参数
        ContractPlanListReqDTO reqDTO = new ContractPlanListReqDTO(queryVO);
        reqDTO.setPageNum(1);
        reqDTO.setPageSize(10000);

        // 发送请求
        PageResult<ContractPlanListDTO> pageList =  UnderlingRestTemplateService.pageForObject(UnderlingPlatformUrlEnum.CONTRACT_PLAN_LIST,
                ContractPlanListDTO.class,reqDTO,"total","list");

        PageResult<ContractPlanningListVO> pageResult = new PageResult<>();
        pageResult.setTotal(pageList.getTotal());

        // 从header获取token标识
        String token = SecurityUtils.getMasterControlToken();

        if (CollectionUtil.isNotEmpty(pageList.getRows())) {
            // 异步处理结果集转换
            List<CompletableFuture<ContractPlanningListVO>> futureList = pageList.getRows().stream()
                    .map(dto -> CompletableFuture.supplyAsync(() -> {
                        ContractPlanningListVO listVO = new ContractPlanningListVO();
                        listVO.setContractPlanningId(dto.getConPlanId());
                        listVO.setContractPlanningCode(dto.getConPlanCode());
                        listVO.setContractPlanningName(dto.getConPlanName());
                        listVO.setContractPlanningCategory(ProcurementPlanTypeConver.converToProcurementPlanType(dto.getConPlanType()));
                        listVO.setPlannedAmountInclTax(dto.getTaxAmount());
                        listVO.setPlanningBalance(dto.getSurplusAmount());
                        listVO.setIncurredPlannedAmount(dto.getUsedAmount());
                        listVO.setBiddingMethodCode(dto.getBidMode());
                        listVO.setBiddingMethodName(dto.getBidModeName());
                        listVO.setBidResponsibleOrg(dto.getBidResponsibleOrg());
                        listVO.setBidResponsibleOrgName(dto.getBidResponsibleOrgName());
                        listVO.setBiddingTime(dto.getBidDate());
                        listVO.setBrand(dto.getBrand());

                        // 设置查询条件并发起异步请求
                        ContractPlanMaterialListRequestDTO requestDTO = new ContractPlanMaterialListRequestDTO();
                        requestDTO.setProjectId(queryVO.getProjectId());
                        requestDTO.setConPlanId(dto.getConPlanId());
                        requestDTO.setAuthorization(token);

                        // 使用异步方法查询数据
                        List<ContractPlanMaterialListDTO> list = UnderlingRestTemplateService.listForObject(
                                UnderlingPlatformUrlEnum.LIST_BY_PROJECT_CONTRACT,
                                ContractPlanMaterialListDTO.class,
                                requestDTO
                        );

                        // 计算总剩余可用量
                        listVO.setSurplusQuantity(
                                list.stream()
                                        .map(ContractPlanMaterialListDTO::getSurplusQuantity)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add)
                        );
//                        listVO.setSurplusQuantity(BigDecimal.ONE);

                        return listVO;
                    })).collect(Collectors.toList());

            // 等待所有异步任务完成并收集结果
//            List<ContractPlanningListVO> resultList = futureList.stream()
//                    .map(CompletableFuture::join)
//                    .collect(Collectors.toList());
            List<ContractPlanningListVO> resultList = futureList.stream()
                    .map(future -> {
                        try {
                            return future.join();
                        } catch (Exception e) {
                            // 记录日志，或者返回一个默认值
                            log.error("Future execution failed", e);
                            return null; // 根据需求处理失败的 future
                        }
                    })
                    .filter(Objects::nonNull) // 过滤掉可能的 null 值
                    .collect(Collectors.toList());


            /* 过滤为0的 */
            resultList = resultList.stream().filter(obj -> obj.getSurplusQuantity().compareTo(BigDecimal.valueOf(0.01))>0).collect(Collectors.toList());

            /* 分页逻辑 */
            pageResult.setTotal(resultList.size());
            int totalItems = resultList.size();
            int totalPages = (int) Math.ceil((double) totalItems / queryVO.getPageSize());
            /* 页码超出范围 */
            if (queryVO.getPageNumber() > totalPages || queryVO.getPageNumber() < 1) {
                pageResult.setRows(new ArrayList<>());
            } else {
                int fromIndex = (queryVO.getPageNumber() - 1) * queryVO.getPageSize();
                int toIndex = Math.min(fromIndex + queryVO.getPageSize(), totalItems);
                resultList = resultList.subList(fromIndex, toIndex);
                pageResult.setRows(resultList);
            }
        }

        return pageResult;
    }

    /**
     * 获取合约的物料清单
     * @param queryVO
     * @return
     */
    public List<ContractMaterialsListVO> getContractMaterialsList(ContractPlanMaterialListQueryVO queryVO) {
        ValidationUtils.validateObject(queryVO);

        ContractPlanMaterialListRequestDTO requestDTO = BeanCopierUtil.copyBean(queryVO,ContractPlanMaterialListRequestDTO.class);

        List<ContractPlanMaterialListDTO> list = UnderlingRestTemplateService.listForObject(UnderlingPlatformUrlEnum.LIST_BY_PROJECT_CONTRACT,ContractPlanMaterialListDTO.class,requestDTO);

        /* 排序一下 根据 物料编码 */
        list.stream().sorted(Comparator.comparing(ContractPlanMaterialListDTO::getSubjectDtlCode).reversed()).collect(Collectors.toList());

        if (CollectionUtil.isNotEmpty(list)) {
            return list.stream()
                    .map(val -> new ContractMaterialsListVO(val,queryVO.getProcurementType()))
                    .collect(Collectors.toList());
        }

        return Collections.emptyList();
    }

    /**
     * 回写合约拆分数据
     * @param contractPlanningId
     * @param projectId
     * @param projectCode
     * @param procurementId
     * @param materialsLists
     */
    public void updatePlanQuantityAmount(String contractPlanningId, String projectId, String projectCode, Long procurementId,Integer procurementPlanType,
                                         List<MaterialsList> materialsLists,String operateFlag) {
        UpdatePlanQuantityAmountRequestDTO requestDTO = new UpdatePlanQuantityAmountRequestDTO(projectId,projectCode);

        // 构造清单写合约规划
        List<UpdatePlanQuantityAmount4ContractAmount> contractAmountUpdates = buildContractAmount(contractPlanningId,procurementId,materialsLists,operateFlag);
        requestDTO.setContractAmountUpdates(contractAmountUpdates);

        // 构造成本科目的已发生金额
        List<UpdatePlanQuantityAmount4SubjectAmount> subjectAmountUpdates = buildSubjectAmount(procurementId,materialsLists,operateFlag);
        requestDTO.setSubjectAmountUpdates(subjectAmountUpdates);

        // 构造清单回写数据
        List<UpdatePlanQuantityAmount4SubjectDtlQuantity> subjectDtlQuantityUpdates = buildSubjectDtlQuantity(procurementId,procurementPlanType,materialsLists,operateFlag);
        requestDTO.setSubjectDtlQuantityUpdates(subjectDtlQuantityUpdates);

        log.info("[回写合约拆分数据] - 开始发送请求...");
        UpdatePlanQuantityAmountResponseDTO responseDTO = UnderlingRestTemplateService.postForObject(UnderlingPlatformUrlEnum.UPDATE_PLAN_QUANTITY_AMOUNT,
                UpdatePlanQuantityAmountResponseDTO.class,requestDTO);
        log.info("[回写合约拆分数据] - 请求结果为:{}", JSON.toJSONString(responseDTO));
    }

    /**
     * 查询最小核算项目详细信息
     * @param projectCode
     * @return
     */
    public MinProjectDetailResponseDTO getMinProjectDetail(String projectCode) {
        MinProjectDetailRequestDTO reqDTO = new MinProjectDetailRequestDTO(projectCode);

        return UnderlingRestTemplateService.getForObject(UnderlingPlatformUrlEnum.GET_MIN_PROJECT,MinProjectDetailResponseDTO.class,reqDTO);
    }

    /**
     * 构造清单回写数据
     *
     * @param procurementId
     * @param materialsLists
     * @param operateFlag
     * @return
     */
    private List<UpdatePlanQuantityAmount4SubjectDtlQuantity> buildSubjectDtlQuantity(Long procurementId, Integer procurementPlanType, List<MaterialsList> materialsLists,
                                                                                      String operateFlag) {
        UpdatePlanQuantityAmount4SubjectDtlQuantity subjectDtlQuantity;
        Map<String,UpdatePlanQuantityAmount4SubjectDtlQuantity> subjectDtlQuantityMap = new HashMap<>();
        for (MaterialsList materialsList : materialsLists) {
            String key = materialsList.getMaterialsUniqueId();
            subjectDtlQuantity = subjectDtlQuantityMap.get(key);
            if (subjectDtlQuantity == null) {
                subjectDtlQuantity = new UpdatePlanQuantityAmount4SubjectDtlQuantity();
                subjectDtlQuantity.setBusinessId(procurementId.toString());
                subjectDtlQuantity.setSubjectDtlUniqueId(materialsList.getMaterialsUniqueId());
                subjectDtlQuantity.setSubjectDtlCode(materialsList.getMaterialsCode());
                subjectDtlQuantity.setSubjectId(materialsList.getCostAccountId());
                subjectDtlQuantity.setTimeStamp(System.currentTimeMillis());
                subjectDtlQuantity.setReduceRentQuantity(BigDecimal.ZERO);
                subjectDtlQuantity.setAddRentQuantity(BigDecimal.ZERO);

                if (ProcurementPlanTypeEnum.isRent(procurementPlanType)) {
                    subjectDtlQuantity.setRentMode(materialsList.getRentMode());
                    if (!RentModeEnum.isWork(materialsList.getRentMode())) {
                        subjectDtlQuantity.setRentQuantity(materialsList.getRentQuantity());
                        subjectDtlQuantity.setRentTime(materialsList.getRentTime());
                    }
                }

                if (Constants.CONTRACT_UPDATE_ADD_FLAG.equals(operateFlag)) {
                    subjectDtlQuantity.setAddQuantity(materialsList.getCount());
                    subjectDtlQuantity.setReduceQuantity(BigDecimal.ZERO);
                } else {
                    subjectDtlQuantity.setAddQuantity(BigDecimal.ZERO);
                    subjectDtlQuantity.setReduceQuantity(materialsList.getCount());
                }
            } else {
                if (ProcurementPlanTypeEnum.isRent(procurementPlanType) && !RentModeEnum.isWork(materialsList.getRentMode())) {
                    subjectDtlQuantity.setRentQuantity(NumberUtil.add(subjectDtlQuantity.getRentQuantity(),materialsList.getRentQuantity()));
                    subjectDtlQuantity.setRentTime(NumberUtil.add(subjectDtlQuantity.getRentTime(),materialsList.getRentTime()));
                }

                if (Constants.CONTRACT_UPDATE_ADD_FLAG.equals(operateFlag)) {
                    subjectDtlQuantity.setAddQuantity(NumberUtil.add(subjectDtlQuantity.getAddQuantity(),materialsList.getCount()));
                } else {
                    subjectDtlQuantity.setReduceQuantity(NumberUtil.add(subjectDtlQuantity.getReduceQuantity(),materialsList.getCount()));
                }
            }

            subjectDtlQuantityMap.put(key ,subjectDtlQuantity);
        }
        return new ArrayList<>(subjectDtlQuantityMap.values());
    }

    /**
     * 构造回写合约规划
     *
     * @param contractPlanningId
     * @param procurementId
     * @param materialsLists
     * @param operateFlag
     * @return
     */
    private List<UpdatePlanQuantityAmount4ContractAmount> buildContractAmount(String contractPlanningId, Long procurementId, List<MaterialsList> materialsLists,
                                                                              String operateFlag) {
        UpdatePlanQuantityAmount4ContractAmount contractAmount = new UpdatePlanQuantityAmount4ContractAmount();
        contractAmount.setConPlanId(contractPlanningId);
        contractAmount.setBusinessId(procurementId.toString());
        contractAmount.setTimeStamp(System.currentTimeMillis());

        // 计算回写金额
        BigDecimal updateAmount = BigDecimal.ZERO;
        for (MaterialsList materialsList : materialsLists) {
            updateAmount = updateAmount.add(materialsList.getAmountInclTax());
        }

        if (Constants.CONTRACT_UPDATE_ADD_FLAG.equals(operateFlag)) {
            contractAmount.setAddAmount(updateAmount);
            contractAmount.setReduceAmount(BigDecimal.ZERO);
        } else {
            contractAmount.setAddAmount(BigDecimal.ZERO);
            contractAmount.setReduceAmount(updateAmount);
        }


        List<UpdatePlanQuantityAmount4ContractAmount> contractAmountUpdates = new ArrayList<>();
        contractAmountUpdates.add(contractAmount);

        return contractAmountUpdates;
    }

    /**
     * 构造成本科目的已发生金额
     *
     * @param procurementId
     * @param materialsLists
     * @param operateFlag
     * @return
     */
    private List<UpdatePlanQuantityAmount4SubjectAmount> buildSubjectAmount(Long procurementId, List<MaterialsList> materialsLists, String operateFlag) {
        UpdatePlanQuantityAmount4SubjectAmount subjectAmount;
        Map<String,UpdatePlanQuantityAmount4SubjectAmount> subjectAmountMap = new HashMap<>();
        for (MaterialsList materialsList : materialsLists) {
            subjectAmount = subjectAmountMap.get(materialsList.getCostAccountId());
            if (subjectAmount == null) {
                subjectAmount = new UpdatePlanQuantityAmount4SubjectAmount();
                subjectAmount.setSubjectId(materialsList.getCostAccountId());
                subjectAmount.setTimeStamp(System.currentTimeMillis());
                subjectAmount.setBusinessId(procurementId.toString());

                if (Constants.CONTRACT_UPDATE_ADD_FLAG.equals(operateFlag)) {
                    subjectAmount.setAddAmount(materialsList.getAmountInclTax());
                    subjectAmount.setReduceAmount(BigDecimal.ZERO);
                } else {
                    subjectAmount.setAddAmount(BigDecimal.ZERO);
                    subjectAmount.setReduceAmount(materialsList.getAmountInclTax());
                }
            } else {
                if (Constants.CONTRACT_UPDATE_ADD_FLAG.equals(operateFlag)) {
                    subjectAmount.setAddAmount(NumberUtil.add(subjectAmount.getAddAmount(),materialsList.getAmountInclTax()));
                } else {
                    subjectAmount.setReduceAmount(NumberUtil.add(subjectAmount.getReduceAmount(),materialsList.getAmountInclTax()));
                }
            }

            subjectAmountMap.put(materialsList.getCostAccountId(),subjectAmount);
        }

        return new ArrayList<>(subjectAmountMap.values());
    }
}
