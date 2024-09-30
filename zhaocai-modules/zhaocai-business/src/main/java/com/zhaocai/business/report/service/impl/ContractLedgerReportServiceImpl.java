package com.zhaocai.business.report.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.report.mapper.ContractLedgerReportMapper;
import com.zhaocai.business.report.service.IContractLedgerReportService;
import com.zhaocai.business.report.vo.ContractLedgerReportVo;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.domain.SysDictData;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ContractLedgerReportServiceImpl extends ServiceImpl<ContractLedgerReportMapper, ContractLedgerReportVo> implements IContractLedgerReportService {

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Override
    public List<ContractLedgerReportVo> contractLedgerReport(ContractLedgerReportVo contractLedger) {
        List<ContractLedgerReportVo> resultList = new ArrayList<>();
        // 获取组织信息
        List<SysDept> depts = remoteSystemService.selectDeptList(new SysDept(), SecurityConstants.INNER);
        if (null != contractLedger.getDeptId()) {
            List<SysDept> selectDept = depts.stream().filter(i -> i.getDeptId().equals(contractLedger.getDeptId())).collect(Collectors.toList());
            if (null != selectDept && !selectDept.isEmpty()) {
                depts = depts.stream().filter(i -> null != i.getAncestors() && i.getAncestors().startsWith(selectDept.get(0).getAncestors()) || i.getDeptId() == selectDept.get(0).getDeptId()).collect(Collectors.toList());
            }
        }
        // 获取采购需求类型
        List<SysDictData> planTypes = this.getDictData("procurement_plan_type");
        // 获取所有成本科目及合同信息、供应商信息等
        List<ContractLedgerReportVo> allItems = baseMapper.getContractLedgerReportResult(contractLedger);
        // 构建合同与成本子项的金额统计
        List<ContractLedgerReportVo> contractList = this.getContractLedgerByContract(allItems);
        if (null != depts && !depts.isEmpty()) {
            // 构建组织最末级的数据统计
            List<ContractLedgerReportVo> leftList = new ArrayList<>();
            // 获取最末级组织
            List<SysDept> leftDepts = getLeafDept(depts);
            if (null != leftDepts && !leftDepts.isEmpty()) {
                // 构建末级组织统计
                leftDepts.forEach(i -> {
                    ContractLedgerReportVo reportVo = new ContractLedgerReportVo();
                    reportVo.setId(String.valueOf(i.getDeptId()));
                    reportVo.setDeptId(i.getDeptId());
                    reportVo.setDeptName(i.getDeptName());
                    reportVo.setParentId(String.valueOf(i.getParentId()));
                    // todo 根据组织查询方案信息 目前缺失组织与方案信息的关联
                    leftList.add(reportVo);
                });
                // 构建采购需求与合同、供应商、物料的统计
                if (null != planTypes && !planTypes.isEmpty()) {
                    List<ContractLedgerReportVo> planTypeList = new ArrayList<>();
                    planTypes.forEach(type -> {
                        ContractLedgerReportVo reportVo = new ContractLedgerReportVo();
                        UUID id = UUID.randomUUID();
                        reportVo.setId(String.valueOf(id));
                        reportVo.setParentId(leftList.get(0).getId());
                        reportVo.setProcurementTypeCode(type.getDictValue());
                        reportVo.setProcurementTypeName(type.getDictLabel());
                        reportVo.setDeptName(leftList.get(0).getDeptName());
                        List<ContractLedgerReportVo> items = contractList.stream().filter(i -> null != i.getProcurementTypeCode() && i.getProcurementTypeCode().equals(type.getDictValue())).collect(Collectors.toList());
                        items.forEach(i -> {
                            i.setParentId(reportVo.getId());
                            i.setDeptName(leftList.get(0).getDeptName());
                            i.setProcurementTypeName(type.getDictLabel());
                            if (StringUtils.isNotEmpty(i.getChildren())) {
                                i.getChildren().forEach(item -> {
                                    item.setDeptName(leftList.get(0).getDeptName());
                                    item.setProcurementTypeName(type.getDictLabel());
                                });
                            }
                        });
                        reportVo.setChildren(items);
                        reportVo.setCostItemAmount(items.stream().filter(i -> null != i.getCostItemAmount()).map(ContractLedgerReportVo::getCostItemAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                        planTypeList.add(reportVo);
                    });
                    // todo 将采购需求统计信息先放入到第一个最末级组织下
                    leftList.get(0).setChildren(planTypeList);
                    // todo 统计最末级组织数据
                    leftList.get(0).setCostItemAmount(planTypeList.stream().filter(i -> null != i.getCostItemAmount()).map(ContractLedgerReportVo::getCostItemAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                }
            }
            List<SysDept> finalDepts = depts;
            resultList.addAll(leftList);
            List<Long> leftIds = leftList.stream().map(ContractLedgerReportVo::getDeptId).collect(Collectors.toList());
            List<SysDept> noLeafDept = depts.stream().filter(left -> !leftIds.contains(left.getDeptId())).collect(Collectors.toList());
            noLeafDept.forEach(dept -> {
                ContractLedgerReportVo reportVo = new ContractLedgerReportVo();
                reportVo.setId(String.valueOf(dept.getDeptId()));
                reportVo.setDeptId(dept.getDeptId());
                reportVo.setDeptName(dept.getDeptName());
                reportVo.setParentId(String.valueOf(dept.getParentId()));
                List<Long> deptIds = finalDepts.stream().filter(i -> null != i.getAncestors() && i.getAncestors().startsWith(dept.getAncestors()))
                        .map(SysDept::getDeptId).collect(Collectors.toList());
                List<ContractLedgerReportVo> deptList = leftList.stream().filter(left -> deptIds.contains(left.getDeptId())).collect(Collectors.toList());
                reportVo.setCostItemAmount(deptList.stream().filter(i -> null != i.getCostItemAmount()).map(ContractLedgerReportVo::getCostItemAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                resultList.add(reportVo);
            });
        }
        // 构建组织机构树
        if (null != contractLedger.getDeptId()) {
            return createDeptTree(resultList, contractLedger.getDeptId());
        }
        return createDeptTree(resultList, 0L);
    }

    private List<ContractLedgerReportVo> createDeptTree(List<ContractLedgerReportVo> resultList, Long dept) {
        List<ContractLedgerReportVo> childrenList = resultList.stream().filter(i -> i.getParentId().equals(String.valueOf(dept))).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(childrenList)) {
            for (ContractLedgerReportVo map : childrenList) {
                map.setChildren(createDeptTree(resultList, map.getDeptId()));
            }
        } else {
            List<ContractLedgerReportVo> deptList = resultList.stream().filter(i -> i.getDeptId().equals(dept)).collect(Collectors.toList());
            if (CollUtil.isNotEmpty(deptList)) {
                return deptList.get(0).getChildren();
            }
        }
        return childrenList;
    }

    private List<ContractLedgerReportVo> getContractLedgerByContract(List<ContractLedgerReportVo> allItems) {
        List<ContractLedgerReportVo> resultList = new ArrayList<>();
        List<ContractLedgerReportVo> distinctItems = allItems.stream()
                .collect(Collectors.toMap(ContractLedgerReportVo::getContractId, Function.identity(), (existing, replacement) -> existing))
                .values().stream().collect(Collectors.toList());
        if (StringUtils.isNotEmpty(distinctItems)) {
            distinctItems.forEach(i -> {
                ContractLedgerReportVo reportVo = new ContractLedgerReportVo();
                BeanUtils.copyProperties(i, reportVo);
                UUID id = UUID.randomUUID();
                reportVo.setId(String.valueOf(id));
                reportVo.setCostItemName(null);
                reportVo.setCostItemBrand(null);
                reportVo.setCostItemSpecification(null);
                reportVo.setCostItemUnit(null);
                reportVo.setCostItemUnitPrice(null);
                reportVo.setCostItemCount(null);
                List<ContractLedgerReportVo> items = allItems.stream().filter(item -> null != item.getContractId() && i.getContractId() == item.getContractId()).distinct().collect(Collectors.toList());
                items.forEach(item -> {
                    item.setParentId(reportVo.getId());
                });
                reportVo.setChildren(items);
                reportVo.setCostItemAmount(items.stream().filter(item -> null != item.getCostItemAmount()).map(ContractLedgerReportVo::getCostItemAmount).reduce(BigDecimal.ZERO, BigDecimal::add));
                resultList.add(reportVo);
            });
        }
        return resultList;
    }

    /**
     * 根据字典类型获取字典数据
     *
     * @param s
     * @return
     */
    private List<SysDictData> getDictData(String s) {
        SysDictData dictData = new SysDictData();
        dictData.setDictType(s);
        List<SysDictData> dataList = remoteSystemService.selectDictDataList(dictData, SecurityConstants.INNER);
        return dataList;
    }

    /**
     * 获取最末级部门
     *
     * @param depts
     * @return
     */
    private List<SysDept> getLeafDept(List<SysDept> depts) {
        List<Long> parentIds = depts.stream().filter(d -> d.getParentId() != null).map(SysDept::getParentId).collect(Collectors.toList());
        return depts.stream().filter(d -> !parentIds.contains(d.getDeptId())).collect(Collectors.toList());
    }
}
