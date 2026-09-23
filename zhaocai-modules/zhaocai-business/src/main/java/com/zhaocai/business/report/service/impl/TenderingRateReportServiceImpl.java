package com.zhaocai.business.report.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.ReportEnum;
import com.zhaocai.business.report.mapper.TenderingRateReportMapper;
import com.zhaocai.business.report.service.ITenderingRateReportService;
import com.zhaocai.business.report.vo.TenderingRateReportVo;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.uuid.UUID;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.domain.SysDictData;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class TenderingRateReportServiceImpl extends ServiceImpl<TenderingRateReportMapper, TenderingRateReportVo> implements ITenderingRateReportService {

    @Autowired
    private RemoteSystemService remoteSystemService;

    /**
     * 招标率报表
     * @param tenderingRate
     * @return
     */
    @Override
    public List<TenderingRateReportVo> tenderingRateReport(TenderingRateReportVo tenderingRate) {
        List<TenderingRateReportVo> resultList = new ArrayList<>();
        // 获取投标已完成，根据项目code、采购需求类型和采购方式进行汇总的信息
        List<TenderingRateReportVo> allItems = baseMapper.getTenderingRateReportResult(tenderingRate);
        // 获取组织信息
        List<SysDept> depts = remoteSystemService.selectDeptList(new SysDept(), SecurityConstants.INNER);
        if(null != tenderingRate.getDeptId()){
            List<SysDept> selectDept = depts.stream().filter(i -> i.getDeptId().equals(tenderingRate.getDeptId())).collect(Collectors.toList());
            if(null != selectDept && !selectDept.isEmpty()){
                depts = depts.stream().filter(i -> null != i.getAncestors() && i.getAncestors().startsWith(selectDept.get(0).getAncestors()) || i.getDeptId() == selectDept.get(0).getDeptId()).collect(Collectors.toList());
            }
        }
        if(StringUtils.isNotEmpty(depts)){
            List<TenderingRateReportVo> leftList = new ArrayList<>();
            // 获取最末级组织
            List<SysDept> leftDepts = getLeafDept(depts);
            if(null != leftDepts && !leftDepts.isEmpty()) {
                // 构建末级组织统计
                leftDepts.forEach(i -> {
                    TenderingRateReportVo reportVo = new TenderingRateReportVo();
                    reportVo.setId(String.valueOf(i.getDeptId()));
                    reportVo.setDeptId(i.getDeptId());
                    reportVo.setDeptName(i.getDeptName());
                    reportVo.setParentId(String.valueOf(i.getParentId()));
                    // todo 根据组织查询项目信息 目前缺失组织与项目联系
                    leftList.add(reportVo);
                });
            }
            // 构造项目
            List<TenderingRateReportVo> projectList = this.createProject(allItems, leftList.get(0));
            // todo 将项目信息先放入到第一个最末级组织下
            leftList.get(0).setChildren(projectList);
            // todo 统计最末级组织数据
            leftList.set(0, this.getTenderingRateCountByDept(leftList.get(0), projectList));
            // 组织数据向上汇总(除去最末级)
            List<Long> leftIds = leftList.stream().map(TenderingRateReportVo::getDeptId).collect(Collectors.toList());
            List<SysDept> noLeafDept = depts.stream().filter(left -> !leftIds.contains(left.getDeptId())).collect(Collectors.toList());
            List<TenderingRateReportVo> finalResultList = new ArrayList<>();
            noLeafDept.forEach(dept->{
                TenderingRateReportVo reportVo = new TenderingRateReportVo();
                reportVo.setDeptId(dept.getDeptId());
                reportVo.setDeptName(dept.getDeptName());
                reportVo.setParentId(String.valueOf(dept.getParentId()));
                reportVo.setId(String.valueOf(dept.getDeptId()));
                finalResultList.add(this.getTenderingRateCountByDept(reportVo,leftList));
            });
            resultList.addAll(leftList);
            resultList.addAll(finalResultList);
        }
        // 构建组织机构树
        List<TenderingRateReportVo> result;
        if(null != tenderingRate.getDeptId()){
            result = createDeptTree(resultList,tenderingRate.getDeptId());
        }else{
            result = createDeptTree(resultList,0L);
        }
        return result;
    }

    /**
     * 构建项目
     * @param allItems
     * @param parentInfo
     * @return
     */
    private List<TenderingRateReportVo> createProject(List<TenderingRateReportVo> allItems,TenderingRateReportVo parentInfo) {
        List<TenderingRateReportVo> resultList = new ArrayList<>();
        // 获取采购需求类型
        List<SysDictData> planTypes = this.getDictData("procurement_plan_type");
        List<TenderingRateReportVo> distinctItems = allItems.stream().filter(i->null != i.getProjectCode())
                .collect(Collectors.toMap(TenderingRateReportVo::getProjectCode, Function.identity(), (existing, replacement) -> existing))
                .values().stream().collect(Collectors.toList());
        if(StringUtils.isNotEmpty(distinctItems)){
            distinctItems.forEach(i->{
                TenderingRateReportVo reportVo = new TenderingRateReportVo();
                UUID id = UUID.randomUUID();
                reportVo.setId(String.valueOf(id));
                reportVo.setParentId(parentInfo.getId());
                reportVo.setDeptName(parentInfo.getDeptName());
                reportVo.setProjectCode(i.getProjectCode());
                reportVo.setProjectName(i.getProjectName());
                List<TenderingRateReportVo> projectItems = allItems.stream().filter(item->null != item.getProjectCode() && item.getProjectCode().equals(i.getProjectCode())).collect(Collectors.toList());
                reportVo.setChildren(this.createType(planTypes,projectItems,reportVo));
                reportVo.setProcurementCount(projectItems.stream().map(TenderingRateReportVo::getCount).reduce(BigDecimal.ZERO,BigDecimal::add));
                reportVo.setOpenBidCount(projectItems.stream().filter(item->null != item.getProcurementType() && item.getProcurementType().equals(ReportEnum.OPEN_BID)).map(TenderingRateReportVo::getCount).reduce(BigDecimal.ZERO,BigDecimal::add));
                reportVo.setInviteBidCount(projectItems.stream().filter(item->null != item.getProcurementType() && item.getProcurementType().equals(ReportEnum.INVITE_BID)).map(TenderingRateReportVo::getCount).reduce(BigDecimal.ZERO,BigDecimal::add));
                reportVo.setEnquiryProcurementCount(projectItems.stream().filter(item->null != item.getProcurementType() && item.getProcurementType().equals(ReportEnum.ENQUIRY_PROCUREMENT)).map(TenderingRateReportVo::getCount).reduce(BigDecimal.ZERO,BigDecimal::add));
                reportVo.setOnlySourceCount(projectItems.stream().filter(item->null != item.getProcurementType() && item.getProcurementType().equals(ReportEnum.ONLY_SOURCE)).map(TenderingRateReportVo::getCount).reduce(BigDecimal.ZERO,BigDecimal::add));
                reportVo.setOpenBidTotalCount(reportVo.getOpenBidCount());
                reportVo.setNoOpenBidTotalCount(reportVo.getProcurementCount().subtract(reportVo.getOpenBidCount()));
                if(reportVo.getProcurementCount().compareTo(BigDecimal.valueOf(0))!=0){
                    reportVo.setOpenBidRate(reportVo.getOpenBidCount().divide(reportVo.getProcurementCount(),4).multiply(ReportEnum.PERCENT));
                    reportVo.setNoOpenBidRate(reportVo.getNoOpenBidTotalCount().divide(reportVo.getProcurementCount(),4).multiply(ReportEnum.PERCENT));
                }
                resultList.add(reportVo);
            });
        }
        return resultList;
    }

    /**
     * 构建采购需求类型
     * @param planTypes
     * @param projectItems
     * @param parentInfo
     * @return
     */
    private List<TenderingRateReportVo> createType(List<SysDictData> planTypes, List<TenderingRateReportVo> projectItems, TenderingRateReportVo parentInfo) {
        List<TenderingRateReportVo> resultList = new ArrayList<>();
        planTypes.forEach(i->{
            TenderingRateReportVo reportVo = new TenderingRateReportVo();
            UUID id = UUID.randomUUID();
            reportVo.setId(String.valueOf(id));
            reportVo.setParentId(parentInfo.getId());
            reportVo.setDeptName(parentInfo.getDeptName());
            reportVo.setProjectCode(parentInfo.getProjectCode());
            reportVo.setProjectName(parentInfo.getProjectName());
            reportVo.setProcurementTypeCode(i.getDictValue());
            reportVo.setProcurementTypeName(i.getDictLabel());
            List<TenderingRateReportVo> typrItems = projectItems.stream().filter(item -> null != item.getProcurementTypeCode() && item.getProcurementTypeCode().equals(i.getDictValue())).collect(Collectors.toList());
            reportVo.setProcurementCount(typrItems.stream().map(TenderingRateReportVo::getCount).reduce(BigDecimal.ZERO,BigDecimal::add));
            reportVo.setOpenBidCount(typrItems.stream().filter(item->null != item.getProcurementType() && item.getProcurementType().equals(ReportEnum.OPEN_BID)).map(TenderingRateReportVo::getCount).reduce(BigDecimal.ZERO,BigDecimal::add));
            reportVo.setInviteBidCount(typrItems.stream().filter(item->null != item.getProcurementType() && item.getProcurementType().equals(ReportEnum.INVITE_BID)).map(TenderingRateReportVo::getCount).reduce(BigDecimal.ZERO,BigDecimal::add));
            reportVo.setEnquiryProcurementCount(typrItems.stream().filter(item->null != item.getProcurementType() && item.getProcurementType().equals(ReportEnum.ENQUIRY_PROCUREMENT)).map(TenderingRateReportVo::getCount).reduce(BigDecimal.ZERO,BigDecimal::add));
            reportVo.setOnlySourceCount(typrItems.stream().filter(item->null != item.getProcurementType() && item.getProcurementType().equals(ReportEnum.ONLY_SOURCE)).map(TenderingRateReportVo::getCount).reduce(BigDecimal.ZERO,BigDecimal::add));
            reportVo.setOpenBidTotalCount(reportVo.getOpenBidCount());
            reportVo.setNoOpenBidTotalCount(reportVo.getProcurementCount().subtract(reportVo.getOpenBidCount()));
            if(reportVo.getProcurementCount().compareTo(BigDecimal.valueOf(0))!=0){
                reportVo.setOpenBidRate(reportVo.getOpenBidCount().divide(reportVo.getProcurementCount(),4).multiply(ReportEnum.PERCENT));
                reportVo.setNoOpenBidRate(reportVo.getNoOpenBidTotalCount().divide(reportVo.getProcurementCount(),4).multiply(ReportEnum.PERCENT));
            }
            resultList.add(reportVo);
        });
        return resultList;
    }

    /**
     * 构造部门树
     * @param resultList
     * @param dept
     * @return
     */
    private List<TenderingRateReportVo> createDeptTree(List<TenderingRateReportVo> resultList, Long dept) {
        List<TenderingRateReportVo> childrenList = resultList.stream().filter(i -> i.getParentId().equals(String.valueOf(dept))).collect(Collectors.toList());
        if (CollUtil.isNotEmpty(childrenList)) {
            for (TenderingRateReportVo map : childrenList) {
                map.setChildren(createDeptTree(resultList, map.getDeptId()));
            }
        }else {
            List<TenderingRateReportVo> deptList = resultList.stream().filter(i -> i.getDeptId().equals(dept)).collect(Collectors.toList());
            if(CollUtil.isNotEmpty(deptList)){
                return deptList.get(0).getChildren();
            }
        }
        return childrenList;
    }

    /**
     * 构建组织的数据统计
     * @param reportVo
     * @param projectList
     * @return
     */
    private TenderingRateReportVo getTenderingRateCountByDept(TenderingRateReportVo reportVo, List<TenderingRateReportVo> projectList) {
        reportVo.setProcurementCount(projectList.stream().filter(i->null != i.getProcurementCount()).map(TenderingRateReportVo::getProcurementCount).reduce(BigDecimal.ZERO,BigDecimal::add));
        reportVo.setOpenBidCount(projectList.stream().filter(i->null != i.getOpenBidCount()).map(TenderingRateReportVo::getOpenBidCount).reduce(BigDecimal.ZERO,BigDecimal::add));
        reportVo.setInviteBidCount(projectList.stream().filter(i->null != i.getInviteBidCount()).map(TenderingRateReportVo::getInviteBidCount).reduce(BigDecimal.ZERO,BigDecimal::add));
        reportVo.setEnquiryProcurementCount(projectList.stream().filter(i->null != i.getEnquiryProcurementCount()).map(TenderingRateReportVo::getEnquiryProcurementCount).reduce(BigDecimal.ZERO,BigDecimal::add));
        reportVo.setOnlySourceCount(projectList.stream().filter(i->null != i.getOnlySourceCount()).map(TenderingRateReportVo::getOnlySourceCount).reduce(BigDecimal.ZERO,BigDecimal::add));
        reportVo.setOpenBidTotalCount(reportVo.getOpenBidCount());
        reportVo.setNoOpenBidTotalCount(reportVo.getProcurementCount().subtract(reportVo.getOpenBidCount()));
        if(reportVo.getProcurementCount().compareTo(BigDecimal.valueOf(0))!=0){
            reportVo.setOpenBidRate(reportVo.getOpenBidCount().divide(reportVo.getProcurementCount(),4).multiply(ReportEnum.PERCENT));
            reportVo.setNoOpenBidRate(reportVo.getNoOpenBidTotalCount().divide(reportVo.getProcurementCount(),4).multiply(ReportEnum.PERCENT));
        }
        return reportVo;
    }

    /**
     * 根据字典类型获取字典数据
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
     * @param depts
     * @return
     */
    private List<SysDept> getLeafDept(List<SysDept> depts) {
        List<Long> parentIds = depts.stream().filter(d -> d.getParentId() != null).map(SysDept::getParentId).collect(Collectors.toList());
        return depts.stream().filter(d -> !parentIds.contains(d.getDeptId())).collect(Collectors.toList());
    }
}
