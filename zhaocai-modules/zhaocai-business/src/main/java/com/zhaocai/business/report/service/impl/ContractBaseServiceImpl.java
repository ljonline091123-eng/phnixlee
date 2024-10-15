package com.zhaocai.business.report.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.report.domain.ContractBase;
import com.zhaocai.business.report.mapper.ContractBaseMapper;
import com.zhaocai.business.report.service.*;
import com.zhaocai.business.report.vo.ContractBaseReportVo;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 合同基础信息(支出合同) Service服务
 */
@Service
public class ContractBaseServiceImpl extends ServiceImpl<ContractBaseMapper, ContractBase> implements IContractBaseService {

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Autowired
    private IContractListLaborService laborService;

    @Autowired
    private IContractListLeasedDeviceService leasedDeviceService;

    @Autowired
    private IContractListLeasedMaterialsService leasedMaterialsService;

    @Autowired
    private IContractListMaterialsService materialsService;

    @Autowired
    private IContractListOtherService otherService;

    @Autowired
    private IContractListSpecialtyService specialtyService;

    @Autowired
    private UnderlingSystemService underlingSystemService;

    /**
     * 获取合同台账报表
     *
     * @param contractBaseReportVo
     * @return
     */
    @Override
    public List<ContractBaseReportVo> contractLedgerReport(ContractBaseReportVo contractBaseReportVo, String callType) {
        List<ContractBaseReportVo> resultList = new ArrayList<>();
        // 1、判断是项目端还是集团端公司端 2、公司端/集团端查询对应公司及下级公司所有的项目对应的合同，项目端查询项目下所有的合同 3、统计数据 4、公司端/集团端构建组织树
        // 公司端/集团端
        if (null != contractBaseReportVo.getId()) {
            // 2、公司端/集团端查询对应公司及下级公司所有的项目对应的合同，项目端查询项目下所有的合同
            // 获取组织结构(本级及以下)
            if (null != contractBaseReportVo.getDeptId()) {
                contractBaseReportVo.setId(contractBaseReportVo.getDeptId());
            }
            // 查询公司及下级公司
            List<SysDept> deptList = remoteSystemService.getDeptByThridDeptId(contractBaseReportVo.getId(), SecurityConstants.INNER);
            if (!CollectionUtils.isEmpty(deptList)) {
                // 查询公司及下级公司所有的项目
                contractBaseReportVo.setDeptIds(deptList.stream().map(SysDept::getThridDeptId).collect(Collectors.toList()));
                // 查询公司及下级公司所有的项目对应的合同
                List<ContractBaseReportVo> contractBaseList;
                if (null != callType && callType.equals("Export")) {
                    contractBaseList = baseMapper.contractLedgerExportList(contractBaseReportVo);
                } else {
                    contractBaseList = baseMapper.contractLedgerList(contractBaseReportVo);
                }
                if (!CollectionUtils.isEmpty(contractBaseList)) {
                    for (SysDept sysDept : deptList) {
                        if (sysDept.getThridOrgType().equals("X")) {
                            // 项目部汇总
                            List<ContractBaseReportVo> list = contractBaseList.stream().filter(i -> i.getBelongingOrgId().equals(sysDept.getThridDeptId())).collect(Collectors.toList());
                            if (!CollectionUtils.isEmpty(list)) {
                                resultList = this.getProject(resultList, list, sysDept, callType);
                            }
                        } else {
                            // 公司汇总
                            List<String> ids = deptList.stream().filter(i -> null != i.getAncestors()
                                    && i.getAncestors().contains(sysDept.getAncestors() + "," + sysDept.getDeptId())).map(SysDept::getThridDeptId).collect(Collectors.toList());
                            List<ContractBaseReportVo> list = contractBaseList.stream().filter(i -> ids.contains(i.getBelongingOrgId())
                                    || i.getBelongingOrgId().equals(sysDept.getThridDeptId())).collect(Collectors.toList());
                            if (!CollectionUtils.isEmpty(list)) {
                                ContractBaseReportVo vo = this.getContractBase(sysDept, list, callType);
                                resultList.add(vo);
                            }
                        }
                    }
                    // 构建组织树
                    List<ContractBaseReportVo> tree = resultList.stream().filter(i -> i.getId().equals(contractBaseReportVo.getId())).collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(tree)) {
                        tree.get(0).setChildren(this.createDeptTree(resultList, contractBaseReportVo.getId()));
                    }
                    return tree;
                }
            }
        } else if (null != contractBaseReportVo.getMinAccountCode()) { // 项目端
            // 查询项目对应的合同
            List<ContractBaseReportVo> contractBaseList = baseMapper.contractLedgerList(contractBaseReportVo);
            if (!CollectionUtils.isEmpty(contractBaseList)) {
                // 创建项目汇总对象
                ContractBaseReportVo vo = new ContractBaseReportVo();
                vo.setId(contractBaseList.get(0).getProjectId());
                vo.setDeptName(contractBaseList.get(0).getMinAccountFullName());
                vo.setType("X");
                vo.setContractNumber(contractBaseList.size());
                vo.setNtaxChangedAmount(contractBaseList.stream().map(ContractBaseReportVo::getNtaxChangedAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                if (null != callType && callType.equals("Export")) {
                    vo.setSettledAmount(contractBaseList.stream().map(ContractBaseReportVo::getSettledAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                    vo.setUnsettledAmount(contractBaseList.stream().map(ContractBaseReportVo::getUnsettledAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                    vo.setPaidAmount(contractBaseList.stream().map(ContractBaseReportVo::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                    vo.setUnpaidAmount(contractBaseList.stream().map(ContractBaseReportVo::getUnpaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                }
                // 按类型分组
                List<ContractBaseReportVo> typeSummaries = contractBaseList.stream()
                        .collect(Collectors.groupingBy(ContractBaseReportVo::getConType))
                        .entrySet()
                        .stream()
                        .map(typeEntry -> {
                            String type = typeEntry.getKey();
                            List<ContractBaseReportVo> typeContracts = typeEntry.getValue();
                            typeContracts.forEach(i -> i.setParentId(vo.getId() + type));
                            // 汇总类型级别的数量和金额
                            int typeCount = typeContracts.size();
                            BigDecimal typeAmount = typeContracts.stream().map(ContractBaseReportVo::getNtaxChangedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                            // 创建类型汇总对象
                            ContractBaseReportVo typeVo = new ContractBaseReportVo();
                            typeVo.setId(vo.getId() + type);
                            typeVo.setConTypeName(StringUtils.isNotEmpty(type)?
                                    underlingSystemService.listDictMap(DictBizEnum.UNDERLING_CONTRACT_TYPE.getName()).get(type):null);
                            typeVo.setDeptName(type);
                            typeVo.setParentId(vo.getId());
                            typeVo.setType("X");
                            typeVo.setContractNumber(typeCount);
                            typeVo.setNtaxChangedAmount(typeAmount);
                            if (null != callType && callType.equals("Export")) {
                                typeVo.setSettledAmount(typeContracts.stream().map(ContractBaseReportVo::getSettledAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                                typeVo.setUnsettledAmount(typeContracts.stream().map(ContractBaseReportVo::getUnsettledAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                                typeVo.setPaidAmount(typeContracts.stream().map(ContractBaseReportVo::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                                typeVo.setUnpaidAmount(typeContracts.stream().map(ContractBaseReportVo::getUnpaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                                typeContracts = this.getContractLedgerDetails(typeContracts);
                            }
                            typeVo.setChildren(typeContracts);
                            return typeVo;
                        }).collect(Collectors.toList());
                vo.setChildren(typeSummaries);
                resultList.add(vo);
            }
        }
        return resultList;
    }

    /**
     * 构建合同台账组织树
     *
     * @param resultList
     * @param id
     * @return
     */
    private List<ContractBaseReportVo> createDeptTree(List<ContractBaseReportVo> resultList, String id) {
        List<ContractBaseReportVo> childenList = resultList.stream().filter(i -> i.getParentId().equals(id)).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(childenList)) {
            for (ContractBaseReportVo map : childenList) {
                map.setChildren(createDeptTree(resultList, map.getId()));
            }
        } else {
            List<ContractBaseReportVo> children = resultList.stream().filter(i -> i.getId().equals(id)).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(children)) {
                return children.get(0).getChildren();
            }
        }
        return childenList;
    }

    /**
     * 获取合同台账详情
     *
     * @param contractId
     * @return
     */
    @Override
    public List<Object> contractLedgerDetails(String contractId) {
        List<Object> result = new ArrayList<>();
        ContractBase contract = baseMapper.selectById(contractId);
        if (contract.getConType().equals("A")) { // A-劳务分包
            result = laborService.getDetailsByContractId(contractId);
        } else if (contract.getConType().equals("B")) { // B-专业分包
            result = specialtyService.getDetailsByContractId(contractId);
        } else if (contract.getConType().equals("C")) { // C-购买材料
            result = materialsService.getDetailsByContractId(contractId);
        } else if (contract.getConType().equals("D")) { // D-租赁材料
            result = leasedMaterialsService.getDetailsByContractId(contractId);
        } else if (contract.getConType().equals("G")) { // G-设备租赁（机械）
            result = leasedDeviceService.getDetailsByContractId(contractId);
        } else if (contract.getConType().equals("Z")) { // Z-其他
            result = otherService.getDetailsByContractId(contractId);
        }
        return result;
    }

    /**
     * 合同台账导出
     *
     * @param contractBaseReportVo
     * @return
     */
    @Override
    public List<ContractBaseReportVo> contractLedgerExport(ContractBaseReportVo contractBaseReportVo) {
        List<ContractBaseReportVo> result = new ArrayList<>();
        List<ContractBaseReportVo> list = this.contractLedgerReport(contractBaseReportVo, "Export");
        this.flattenData(list.get(0), result);
        return result;
    }

    /**
     * 递归方法，将嵌套的数据扁平化
     *
     * @param node
     * @param result
     */
    private void flattenData(ContractBaseReportVo node, List<ContractBaseReportVo> result) {
        result.add(node);
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            for (ContractBaseReportVo child : node.getChildren()) {
                flattenData(child, result);
            }
        }
    }

    /**
     * 合同台账导出获取物料明细信息
     *
     * @param resultList
     * @return
     */
    private List<ContractBaseReportVo> getContractLedgerDetails(List<ContractBaseReportVo> resultList) {
        for (ContractBaseReportVo contractBaseReportVo : resultList) {
            List<Object> list = contractLedgerDetails(contractBaseReportVo.getUniqueId());
            if (!CollectionUtils.isEmpty(list)) {
                contractBaseReportVo.setChildren(BeanCopierUtil.copyList(list, ContractBaseReportVo.class));
            }
            resultList.add(contractBaseReportVo);
        }
        return resultList;
    }

    /**
     * 获取合同台账导出表头
     *
     * @param contractBaseReportVo
     * @return
     */
    @Override
    public String getExportTitle(ContractBaseReportVo contractBaseReportVo) {
        String title = "";
        // 集团或公司端
        if (null != contractBaseReportVo.getId()) {
            SysDept dept = remoteSystemService.getByThridDeptId(contractBaseReportVo.getId(), SecurityConstants.INNER);
            if (null != dept && !StringUtils.isEmpty(dept.getDeptName())) {
                title = dept.getDeptName();
            }
        }
        // 项目端
        if (null != contractBaseReportVo.getMinAccountCode()) {
            List<ContractBaseReportVo> contractBaseList = baseMapper.contractLedgerList(contractBaseReportVo);
            if (!CollectionUtils.isEmpty(contractBaseList)) {
                title = contractBaseList.get(0).getMinAccountFullName();
            }
        }
        return title;
    }

    /**
     * 汇总项目层级（合同台账,无项目管理部）
     *
     * @param resultList
     * @param list
     * @param sysDept
     * @return
     */
    private List<ContractBaseReportVo> getProject(List<ContractBaseReportVo> resultList, List<ContractBaseReportVo> list, SysDept sysDept, String callType) {
        // 汇总项目及以下数据
        List<ContractBaseReportVo> result = list.stream()
                .collect(Collectors.groupingBy(ContractBaseReportVo::getProjectId))
                .entrySet()
                .stream()
                .map(entry -> {
                    String projectId = entry.getKey();
                    List<ContractBaseReportVo> projectContracts = entry.getValue();
                    // 汇总项目级别的数量和金额
                    int projectCount = projectContracts.size();
                    BigDecimal projectAmount = projectContracts.stream().map(ContractBaseReportVo::getNtaxChangedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    // 按类型分组
                    List<ContractBaseReportVo> typeSummaries = projectContracts.stream()
                            .collect(Collectors.groupingBy(ContractBaseReportVo::getConType))
                            .entrySet()
                            .stream()
                            .map(typeEntry -> {
                                String type = typeEntry.getKey();
                                List<ContractBaseReportVo> typeContracts = typeEntry.getValue();
                                typeContracts.forEach(i -> i.setParentId(projectId + type));
                                // 汇总类型级别的数量和金额
                                int typeCount = typeContracts.size();
                                BigDecimal typeAmount = typeContracts.stream().map(ContractBaseReportVo::getNtaxChangedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                                // 创建类型汇总对象
                                ContractBaseReportVo typeVo = new ContractBaseReportVo();
                                typeVo.setId(projectId + type);
                                typeVo.setConTypeName(StringUtils.isNotEmpty(type)?
                                        underlingSystemService.listDictMap(DictBizEnum.UNDERLING_CONTRACT_TYPE.getName()).get(type):null);
                                typeVo.setDeptName(type);
                                typeVo.setParentId(projectId);
                                typeVo.setType("X");
                                typeVo.setContractNumber(typeCount);
                                typeVo.setNtaxChangedAmount(typeAmount);
                                if (null != callType && callType.equals("Export")) {
                                    typeVo.setSettledAmount(typeContracts.stream().map(ContractBaseReportVo::getSettledAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                                    typeVo.setUnsettledAmount(typeContracts.stream().map(ContractBaseReportVo::getUnsettledAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                                    typeVo.setPaidAmount(typeContracts.stream().map(ContractBaseReportVo::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                                    typeVo.setUnpaidAmount(typeContracts.stream().map(ContractBaseReportVo::getUnpaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                                    typeContracts = this.getContractLedgerDetails(typeContracts);
                                }
                                typeVo.setChildren(typeContracts);
                                return typeVo;
                            }).collect(Collectors.toList());
                    // 创建项目汇总对象
                    ContractBaseReportVo projectVo = new ContractBaseReportVo();
                    projectVo.setId(projectId);
                    projectVo.setDeptName(projectContracts.get(0).getMinAccountFullName());
                    projectVo.setParentId(sysDept.getThridParentId());
                    projectVo.setType("X");
                    projectVo.setContractNumber(projectCount);
                    projectVo.setNtaxChangedAmount(projectAmount);
                    if (null != callType && callType.equals("Export")) {
                        projectVo.setSettledAmount(projectContracts.stream().map(ContractBaseReportVo::getSettledAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                        projectVo.setUnsettledAmount(projectContracts.stream().map(ContractBaseReportVo::getUnsettledAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                        projectVo.setPaidAmount(projectContracts.stream().map(ContractBaseReportVo::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                        projectVo.setUnpaidAmount(projectContracts.stream().map(ContractBaseReportVo::getUnpaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                    }
                    projectVo.setChildren(typeSummaries);
                    return projectVo;
                }).collect(Collectors.toList());
        // 找到项目管理部上级公司
        ContractBaseReportVo gs = resultList.stream()
                .filter(i -> i.getId().equals(sysDept.getThridParentId())).findFirst().orElse(null);
        if (gs != null) {
            // 判断 children 是否有值
            if (gs.getChildren() == null) {
                gs.setChildren(result);
            } else {
                gs.getChildren().addAll(result);
            }
        }
        return resultList;
    }

    /**
     * 汇总项目层级（合同台账,有项目管理部）
     *
     * @param resultList
     * @param list
     * @param sysDept
     * @return
     */
    private List<ContractBaseReportVo> getProject1(List<ContractBaseReportVo> resultList, List<ContractBaseReportVo> list, SysDept sysDept, String callType) {
        ContractBaseReportVo xmb = this.getContractBase(sysDept, list, callType);
        // 汇总项目及以下数据
        List<ContractBaseReportVo> result = list.stream()
                .collect(Collectors.groupingBy(ContractBaseReportVo::getProjectId))
                .entrySet()
                .stream()
                .map(entry -> {
                    String projectId = entry.getKey();
                    List<ContractBaseReportVo> projectContracts = entry.getValue();
                    // 汇总项目级别的数量和金额
                    int projectCount = projectContracts.size();
                    BigDecimal projectAmount = projectContracts.stream().map(ContractBaseReportVo::getNtaxChangedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    // 按类型分组
                    List<ContractBaseReportVo> typeSummaries = projectContracts.stream()
                            .collect(Collectors.groupingBy(ContractBaseReportVo::getConType))
                            .entrySet()
                            .stream()
                            .map(typeEntry -> {
                                String type = typeEntry.getKey();
                                List<ContractBaseReportVo> typeContracts = typeEntry.getValue();
                                // 汇总类型级别的数量和金额
                                int typeCount = typeContracts.size();
                                BigDecimal typeAmount = typeContracts.stream().map(ContractBaseReportVo::getNtaxChangedAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                                typeContracts.forEach(i -> i.setParentId(projectId + type));
                                // 创建类型汇总对象
                                ContractBaseReportVo typeVo = new ContractBaseReportVo();
                                typeVo.setId(projectId + type);
                                typeVo.setConTypeName(StringUtils.isNotEmpty(type)?
                                        underlingSystemService.listDictMap(DictBizEnum.UNDERLING_CONTRACT_TYPE.getName()).get(type):null);
                                typeVo.setDeptName(type);
                                typeVo.setParentId(projectId);
                                typeVo.setType("X");
                                typeVo.setContractNumber(typeCount);
                                typeVo.setNtaxChangedAmount(typeAmount);
                                if (null != callType && callType.equals("Export")) {
                                    typeVo.setSettledAmount(typeContracts.stream().map(ContractBaseReportVo::getSettledAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                                    typeVo.setUnsettledAmount(typeContracts.stream().map(ContractBaseReportVo::getUnsettledAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                                    typeVo.setPaidAmount(typeContracts.stream().map(ContractBaseReportVo::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                                    typeVo.setUnpaidAmount(typeContracts.stream().map(ContractBaseReportVo::getUnpaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                                    typeContracts = this.getContractLedgerDetails(typeContracts);
                                }
                                typeVo.setChildren(typeContracts);
                                return typeVo;
                            }).collect(Collectors.toList());
                    // 创建项目汇总对象
                    ContractBaseReportVo projectVo = new ContractBaseReportVo();
                    projectVo.setId(projectId);
                    projectVo.setDeptName(projectContracts.get(0).getMinAccountFullName());
                    projectVo.setParentId(sysDept.getThridDeptId());
                    projectVo.setType("X");
                    projectVo.setContractNumber(projectCount);
                    projectVo.setNtaxChangedAmount(projectAmount);
                    if (null != callType && callType.equals("Export")) {
                        projectVo.setSettledAmount(projectContracts.stream().map(ContractBaseReportVo::getSettledAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                        projectVo.setUnsettledAmount(projectContracts.stream().map(ContractBaseReportVo::getUnsettledAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                        projectVo.setPaidAmount(projectContracts.stream().map(ContractBaseReportVo::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                        projectVo.setUnpaidAmount(projectContracts.stream().map(ContractBaseReportVo::getUnpaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                    }
                    projectVo.setChildren(typeSummaries);
                    return projectVo;
                }).collect(Collectors.toList());
        xmb.setChildren(result);
        resultList.add(xmb);
        return resultList;
    }

    /**
     * 汇总公司层级（合同台账）
     *
     * @param sysDept
     * @param list
     * @return
     */
    private ContractBaseReportVo getContractBase(SysDept sysDept, List<ContractBaseReportVo> list, String callType) {
        ContractBaseReportVo vo = new ContractBaseReportVo();
        vo.setId(sysDept.getThridDeptId());
        vo.setDeptName(sysDept.getDeptName());
        vo.setParentId(sysDept.getThridParentId());
        vo.setType("G");
        vo.setContractNumber(list.size());
        vo.setNtaxChangedAmount(list.stream().map(ContractBaseReportVo::getNtaxChangedAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        if (null != callType && callType.equals("Export")) {
            vo.setSettledAmount(list.stream().map(ContractBaseReportVo::getSettledAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            vo.setUnsettledAmount(list.stream().map(ContractBaseReportVo::getUnsettledAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            vo.setPaidAmount(list.stream().map(ContractBaseReportVo::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
            vo.setUnpaidAmount(list.stream().map(ContractBaseReportVo::getUnpaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
        }
        return vo;
    }
}
