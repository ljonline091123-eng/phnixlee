package com.zhaocai.business.report.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.report.mapper.PriceAnalysisReportMapper;
import com.zhaocai.business.report.service.IPriceAnalysisReportService;
import com.zhaocai.business.report.vo.PriceAnalysisReportVo;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class PriceAnalysisReportServiceImpl extends ServiceImpl<PriceAnalysisReportMapper, PriceAnalysisReportVo> implements IPriceAnalysisReportService {

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Override
    public List<PriceAnalysisReportVo> priceAnalysisReport(PriceAnalysisReportVo priceAnalysis) {
        List<PriceAnalysisReportVo> resultList = new ArrayList<>();
        // 获取所有成本科目及项目、合同信息、价格信息等
        List<PriceAnalysisReportVo> allItems = baseMapper.getPriceAnalysisReportResult(priceAnalysis);
        // 构建成本科目层
        resultList = this.createCost(allItems);
        return resultList;
    }

    /**
     * 构建成本科目层
     * @param allItems
     * @return
     */
    private List<PriceAnalysisReportVo> createCost(List<PriceAnalysisReportVo> allItems) {
        List<PriceAnalysisReportVo> resultList = new ArrayList<>();
        List<PriceAnalysisReportVo> distinctItems = allItems.stream()
                .collect(Collectors.toMap(PriceAnalysisReportVo::getCostItemCode, Function.identity(), (existing, replacement) -> existing))
                .values().stream().collect(Collectors.toList());
        if(StringUtils.isNotEmpty(distinctItems)){
            distinctItems.forEach(i->{
                PriceAnalysisReportVo reportVo = new PriceAnalysisReportVo();
                BeanUtils.copyProperties(i, reportVo);
                UUID id = UUID.randomUUID();
                reportVo.setId(String.valueOf(id));
                reportVo.setParentId("0");
                reportVo.setAreaName("");
                reportVo.setProjectName("");
                reportVo.setContractCode("");
                reportVo.setContractTime(null);
                reportVo.setTaxUnitPrice(null);
                reportVo.setNotTaxUnitPrice(null);
                List<PriceAnalysisReportVo> costItem = allItems.stream().filter(item -> null != item.getCostItemCode() && item.getCostItemCode().equals(i.getCostItemCode())).collect(Collectors.toList());
                reportVo.setChildren(this.createArea(costItem,reportVo));
                resultList.add(reportVo);
            });
        }
        return resultList;
    }

    /**
     * 构建地区层
     * @param costItem
     * @param parentInfo
     * @return
     */
    private List<PriceAnalysisReportVo> createArea(List<PriceAnalysisReportVo> costItem, PriceAnalysisReportVo parentInfo) {
        List<PriceAnalysisReportVo> resultList = new ArrayList<>();
        List<PriceAnalysisReportVo> distinctItems = costItem.stream().filter(i->null != i.getProjectCode())
                .collect(Collectors.toMap(PriceAnalysisReportVo::getProjectCode, Function.identity(), (existing, replacement) -> existing))
                .values().stream().collect(Collectors.toList());
        if(StringUtils.isNotEmpty(distinctItems)){
            distinctItems.forEach(i->{
                PriceAnalysisReportVo reportVo = new PriceAnalysisReportVo();
                BeanUtils.copyProperties(i, reportVo);
                UUID id = UUID.randomUUID();
                reportVo.setId(String.valueOf(id));
                reportVo.setParentId(parentInfo.getId());
                reportVo.setProjectName("");
                reportVo.setContractCode("");
                reportVo.setContractTime(null);
                reportVo.setTaxUnitPrice(null);
                reportVo.setNotTaxUnitPrice(null);
                // todo 将统计信息先放入到第一个最末级组织下，项目与组织目前没有关联
                List<PriceAnalysisReportVo> deptList = this.getFirstLeafDept(reportVo);
                List<PriceAnalysisReportVo> areaItem = costItem.stream().filter(item -> null != item.getAreaName() && item.getAreaName().equals(i.getAreaName())).collect(Collectors.toList());
                deptList.get(0).setChildren(this.createProject(areaItem,deptList.get(0)));
                reportVo.setChildren(deptList);
                resultList.add(reportVo);
            });
        }
        return resultList;
    }

    /**
     * 构建项目和合同层
     * @param areaItem
     * @param parentInfo
     * @return
     */
    private List<PriceAnalysisReportVo> createProject(List<PriceAnalysisReportVo> areaItem, PriceAnalysisReportVo parentInfo) {
        List<PriceAnalysisReportVo> resultList = new ArrayList<>();
        List<PriceAnalysisReportVo> distinctItems = areaItem.stream()
                .collect(Collectors.toMap(PriceAnalysisReportVo::getProjectCode, Function.identity(), (existing, replacement) -> existing))
                .values().stream().collect(Collectors.toList());
        if(StringUtils.isNotEmpty(distinctItems)){
            distinctItems.forEach(i->{
                PriceAnalysisReportVo reportVo = new PriceAnalysisReportVo();
                BeanUtils.copyProperties(i, reportVo);
                UUID id = UUID.randomUUID();
                reportVo.setId(String.valueOf(id));
                reportVo.setParentId(parentInfo.getId());
                reportVo.setDeptName(parentInfo.getDeptName());
                reportVo.setContractName(null);
                reportVo.setContractTime(null);
                reportVo.setTaxUnitPrice(null);
                reportVo.setNotTaxUnitPrice(null);
                reportVo.setContractCode(null);
                List<PriceAnalysisReportVo> contractItem = areaItem.stream().filter(item -> null != item.getProjectCode() && item.getProjectCode().equals(i.getProjectCode())).collect(Collectors.toList());
                contractItem = contractItem.stream()
                        .collect(Collectors.toMap(PriceAnalysisReportVo::getContractCode, Function.identity(), (existing, replacement) -> existing))
                        .values().stream().collect(Collectors.toList());
                contractItem.forEach(item->{
                    UUID cid = UUID.randomUUID();
                    item.setId(String.valueOf(cid));
                    item.setParentId(i.getId());
                    item.setDeptName(parentInfo.getDeptName());
                });
                reportVo.setChildren(contractItem);
                resultList.add(reportVo);
            });
        }
        return resultList;
    }

    /**
     * 构建第一个最末级部门
     * @return
     */
    private List<PriceAnalysisReportVo> getFirstLeafDept(PriceAnalysisReportVo parentInfo) {
        // 获取组织信息
        List<SysDept> depts = remoteSystemService.selectDeptList(new SysDept(), SecurityConstants.INNER);
        List<Long> parentIds = depts.stream().filter(d -> d.getParentId() != null).map(SysDept::getParentId).collect(Collectors.toList());
        List<SysDept> leftDepts = depts.stream().filter(d -> !parentIds.contains(d.getDeptId())).collect(Collectors.toList());
        PriceAnalysisReportVo reportVo = new PriceAnalysisReportVo();
        UUID id = UUID.randomUUID();
        reportVo.setId(String.valueOf(id));
        reportVo.setParentId(parentInfo.getId());
        reportVo.setDeptId(leftDepts.get(0).getDeptId());
        reportVo.setDeptName(leftDepts.get(0).getDeptName());
        reportVo.setCostItemName(parentInfo.getCostItemName());
        reportVo.setCostItemUnit(parentInfo.getCostItemUnit());
        reportVo.setCostItemSpecification(parentInfo.getCostItemSpecification());
        reportVo.setAreaName(parentInfo.getAreaName());
        List<PriceAnalysisReportVo> resultList = new ArrayList<>();
        resultList.add(reportVo);
        return resultList;
    }
}
