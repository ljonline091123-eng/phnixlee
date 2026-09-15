package com.zhaocai.business.report.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.ReportEnum;
import com.zhaocai.business.report.mapper.ManagePageReportMapper;
import com.zhaocai.business.report.service.IManagePageReportService;
import com.zhaocai.business.report.util.ReportScopeUtil;
import com.zhaocai.business.report.vo.ManagePageReportVo;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.system.api.domain.OrgInfoQueryDTO;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ManagePageReportServiceImpl extends ServiceImpl<ManagePageReportMapper, ManagePageReportVo> implements IManagePageReportService {

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Override
    public Map<String, Object> managePageReport(ManagePageReportVo managePage) {
        List<ManagePageReportVo> resultList = new ArrayList<>();
        // todo 组织和项目未关联，手动关联到103
        if(null != managePage.getDeptId()){
            if(this.isReturnView(managePage.getDeptId())){
                // 获取所有项目、合同等信息
                List<ManagePageReportVo> allItems = baseMapper.getManagePageReportResult(managePage);
                // 通过项目进行统计
                List<ManagePageReportVo> distinctItems = allItems.stream().filter(i->null != i.getProjectCode())
                        .collect(Collectors.toMap(ManagePageReportVo::getProjectCode, Function.identity(), (existing, replacement) -> existing))
                        .values().stream().collect(Collectors.toList());
                distinctItems.forEach(i->{
                    List<ManagePageReportVo> projectItem = allItems.stream().filter(item->null != item.getProjectCode() && item.getProjectCode().equals(i.getProjectCode())).collect(Collectors.toList());
                    BigDecimal contractAmount = projectItem.stream().filter(item->null !=item.getTotalAmountIncTax()).map(ManagePageReportVo::getTotalAmountIncTax).reduce(BigDecimal.ZERO, BigDecimal::add);
                    i.setContractAmount(contractAmount.divide(BigDecimal.valueOf(10000),2));
                    long planCount = projectItem.stream()
                            .collect(Collectors.toMap(ManagePageReportVo::getPlanId, Function.identity(), (existing, replacement) -> existing))
                            .values().stream().count();
                    i.setPlanCount(BigDecimal.valueOf(planCount));
                    long bidCount = projectItem.stream().filter(item->null != item.getNoticeStatus() && item.getNoticeStatus().equals(ReportEnum.BID_RESULT_RELEASE))
                            .collect(Collectors.toMap(ManagePageReportVo::getSchemeId, Function.identity(), (existing, replacement) -> existing))
                            .values().stream().count();
                    i.setBidCount(BigDecimal.valueOf(bidCount));
                    List<ManagePageReportVo> signList = projectItem.stream().filter(item->null != item.getAgreementState() && item.getAgreementState().equals(ReportEnum.SIGN_CONTRACT_STATUS))
                            .collect(Collectors.toMap(ManagePageReportVo::getContractId, Function.identity(), (existing, replacement) -> existing))
                            .values().stream().collect(Collectors.toList());
                    i.setSignCount(BigDecimal.valueOf(signList.size()));
                    BigDecimal signContractAmount = signList.stream().filter(item->null !=item.getTotalAmountIncTax()).map(ManagePageReportVo::getTotalAmountIncTax).reduce(BigDecimal.ZERO, BigDecimal::add);
                    i.setSignContractAmount(signContractAmount);
                    //项目业态
                    i.setProjectBusinessName(i.getProjectBusinessCode());
                    //工程类型名称
                    i.setEngineerTypeName(i.getEngineerTypeCode());
                    //结构类型名称
                    i.setStructureTypeName(i.getStructureTypeCode());
                });
                resultList.addAll(distinctItems);
            }
        }
        if(null != managePage.getContractStartAmount() && null != managePage.getContractEndAmount()){
            resultList = resultList.stream().filter(i->
                    i.getContractAmount().compareTo(managePage.getContractStartAmount())>=0
                    && i.getContractAmount().compareTo(managePage.getSignContractAmount())<=0)
                    .collect(Collectors.toList());
        }
        Map<String, Object> map = new HashMap<>();
        map.put("total",resultList.size());
        map.put("list",getPage(resultList,managePage.getPageNum(),managePage.getPageSize()));
        return map;
    }

    @Override
    public List<SysDept> managePageReportDept(ManagePageReportVo managePage) {
        SysDept dept = new SysDept();
        if(null != managePage.getDeptName()){
            dept.setDeptName(managePage.getDeptName());
        }
        OrgInfoQueryDTO queryDTO = new OrgInfoQueryDTO();
        queryDTO.setThridOrgId(managePage.getThridOrgId());
        List<SysDept> depts = remoteSystemService.getOrgInfoList(queryDTO, SecurityConstants.INNER);

        /*List<SysDept> depts = remoteSystemService.selectDeptList(dept, SecurityConstants.INNER);
        if(null != managePage.getThridOrgId()){
            List<SysDept> selectDept = depts.stream().filter(i -> managePage.getThridOrgId().equals(i.getThridDeptId())).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(selectDept)) {
                String selectDeptAnc = selectDept.get(0).getAncestors() + "," + selectDept.get(0).getDeptId();
                depts = depts.stream().filter(i ->
                                (null != i.getAncestors() && i.getAncestors().startsWith(selectDeptAnc))
                                && ("F".equals(i.getThridOrgType()) || "A".equals(i.getThridOrgType())))
                        .collect(Collectors.toList());
                depts.add(selectDept.get(0));
            }
        }*/
        return depts;
    }

    @Override
    public List<SysDept> getOrgList(String orgId) {
        // 报表组织树按登录用户数据权限收敛：
        // 集团账号按传入单位(未传时取登录用户所属单位)取树，其他账号一律取本人所属单位及下级
        String scopeOrgId = ReportScopeUtil.clampOrgId(StringUtils.isNotEmpty(orgId) ? orgId : ReportScopeUtil.getDefaultOrgId());
        if (StringUtils.isEmpty(scopeOrgId)) {
            return new ArrayList<>();
        }
        OrgInfoQueryDTO queryDTO = new OrgInfoQueryDTO();
        queryDTO.setThridOrgId(scopeOrgId);
        List<SysDept> depts = remoteSystemService.getOrgInfoList(queryDTO, SecurityConstants.INNER);
        return depts;
    }

    private List<ManagePageReportVo> getPage(List<ManagePageReportVo> taskList, int page, int pageSize) {
        int total = taskList.size();
        int fromIndex = (page - 1) * pageSize;
        int toIndex = Math.min(fromIndex + pageSize, total);
        return taskList.subList(fromIndex, toIndex);
    }

    private boolean isReturnView(Long deptId) {
        // 获取组织信息
        List<SysDept> depts = remoteSystemService.selectDeptList(new SysDept(), SecurityConstants.INNER);
        List<SysDept> dept = depts.stream().filter(i-> i.getDeptId().equals(deptId)).collect(Collectors.toList());
        if(StringUtils.isNotEmpty(dept)){
            List<Long> deptList = depts.stream().filter(i -> i.getAncestors().startsWith(dept.get(0).getAncestors()) || i.getDeptId().equals(dept.get(0).getDeptId())).map(SysDept::getDeptId).collect(Collectors.toList());
            /*if(deptList.contains(103L)){
                return true;
            }*/
            return true;
        }
        return false;
    }
}
