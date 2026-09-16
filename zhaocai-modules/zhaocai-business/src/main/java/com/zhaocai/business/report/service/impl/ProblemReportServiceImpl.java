package com.zhaocai.business.report.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.DeptTypeEnum;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.business.report.domain.ProblemReport;
import com.zhaocai.business.report.mapper.ProblemReportMapper;
import com.zhaocai.business.report.service.IProblemReportService;
import com.zhaocai.business.report.util.ReportScopeUtil;
import com.zhaocai.business.report.vo.EvaluationBadReportVo;
import com.zhaocai.business.report.vo.ProblemReportVo;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 问题报表
 *
 * 与成控(prod)的差异：
 * 1. 报表不受顶部"单位-项目"选择框限制：组织范围一律由后端按登录用户所属组织收敛(ReportScopeUtil)；
 * 2. tab2 不再读平台同步的 tb_vendor_evaluation_report，改为直查本地供应商评价表 tb_vendor_evaluate
 *    (已确认 + 不合格)，并把"评价项目""评价合同"两列换成"评价周期"。
 */
@Service
public class ProblemReportServiceImpl extends ServiceImpl<ProblemReportMapper, ProblemReport> implements IProblemReportService {

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Autowired
    private ISysDictDataService sysDictDataService;

    @Override
    public Map<String, Object> getProblemReport(ProblemReportVo queryVO) {
        List<ProblemReportVo> resultList = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        // 组织范围由后端收敛：不在权限范围内的组织回落到本人所属组织
        String orgId = resolveScopeOrgId(queryVO);
        // 集团或公司端
        if (null != orgId) {
            List<ProblemReportVo> deptList = baseMapper.selectByDepartment(queryVO);
            if (CollectionUtil.isNotEmpty(deptList)) {
                List<ProblemReportVo> list = baseMapper.select(queryVO);
                List<ProblemReportVo> projectList = list.stream()
                        .collect(Collectors.groupingBy(ProblemReportVo::getMinAccountCode))
                        .entrySet()
                        .stream()
                        .map(entry -> {
                            List<ProblemReportVo> childList = entry.getValue();
                            ProblemReportVo vo = new ProblemReportVo();
                            vo.setMinAccountCode(entry.getKey());
                            vo.setId(entry.getKey());
                            if (!childList.isEmpty()) {
                                vo.setMinAccountFullName(childList.get(0).getMinAccountFullName());
                                vo.setNum(BigDecimal.valueOf(childList.size()));
                                vo.setProjectDepartmentId(childList.get(0).getProjectDepartmentId());
                                vo.setParentId(childList.get(0).getProjectDepartmentId());
                            }
                            vo.setChildren(childList);
                            return vo;
                        }).collect(Collectors.toList());
                for (ProblemReportVo problemReportVo : resultList) {
                    problemReportVo.setChildren(projectList.stream().filter(i ->
                                    i.getProjectDepartmentId() != null && i.getProjectDepartmentId().equals(problemReportVo.getId()))
                            .collect(Collectors.toList()));
                }
            }
            map.put("total", deptList.size());
            map.put("list", resultList);
        } else {
            map.put("total", 0);
            List<ProblemReportVo> list = baseMapper.select(queryVO);
            if (CollectionUtil.isNotEmpty(list)) {
                ProblemReportVo dept = new ProblemReportVo();
                dept.setId(list.get(0).getProjectDepartmentId());
                dept.setDeptName(list.get(0).getProjectDepartment());
                dept.setNum(BigDecimal.valueOf(list.size()));
                ProblemReportVo project = new ProblemReportVo();
                project.setMinAccountCode(list.get(0).getMinAccountCode());
                project.setId(list.get(0).getMinAccountCode());
                project.setMinAccountFullName(list.get(0).getMinAccountFullName());
                project.setNum(BigDecimal.valueOf(list.size()));
                project.setParentId(list.get(0).getProjectDepartmentId());
                project.setChildren(list);
                List<ProblemReportVo> deptList = new ArrayList<>();
                deptList.add(project);
                dept.setChildren(deptList);
                resultList.add(dept);
                map.put("total", 1);
            }
            map.put("list", resultList);
        }
        return map;
    }

    @Override
    public List<ProblemReportVo> getInitialInfo(ProblemReportVo queryVo) {
        List<ProblemReportVo> resultList = new ArrayList<>();
        // 组织范围由后端收敛：不在权限范围内的组织回落到本人所属组织
        String orgId = resolveScopeOrgId(queryVo);
        // 集团或公司端
        if (null != orgId) {
            // 获取当前单位及下一层的组织
            List<SysDept> deptList = remoteSystemService.getDeptAndNextDept(queryVo.getId(), DeptTypeEnum.ALL_DEPT_TYPE.getType(), SecurityConstants.INNER);
            if (CollectionUtil.isNotEmpty(deptList)) {
                // 获取组织统计数据
                queryVo.setDeptId(String.valueOf(deptList.get(0).getDeptId()));
                List<ProblemReportVo> list = baseMapper.getProblemByDept(queryVo);
                if (CollectionUtil.isNotEmpty(list)) {
                    ProblemReportVo rootVo = new ProblemReportVo();
                    rootVo.setNum(list.stream().map(ProblemReportVo::getNum).reduce(BigDecimal.ZERO, BigDecimal::add));
                    rootVo.setId(deptList.get(0).getThridDeptId());
                    rootVo.setDeptName(deptList.get(0).getDeptName());
                    rootVo.setParentId(deptList.get(0).getThridParentId());
                    // 只有本级时也要返回空的 children，前端固定取 tableData[0].children
                    rootVo.setChildren(deptList.size() > 1 ? getNextData(deptList, list, queryVo) : new ArrayList<>());
                    resultList.add(rootVo);
                }
            }
        } else if (null != queryVo.getMinAccountCode()) {         // 项目端
            List<ProblemReportVo> vo = baseMapper.select(queryVo);
            if (CollectionUtil.isNotEmpty(vo)) {
                resultList.addAll(vo);
            }
        }
        return resultList;
    }

    private List<ProblemReportVo> getNextData(List<SysDept> deptList, List<ProblemReportVo> list, ProblemReportVo queryVo) {
        List<ProblemReportVo> resultList = new ArrayList<>();
        for (int i = 1; i < deptList.size(); i++) {
            SysDept dept = deptList.get(i);
            List<ProblemReportVo> bidList = list.stream().filter(item ->
                            item.getId().equals(dept.getThridDeptId()) || item.getAncestors().contains(String.valueOf(dept.getDeptId())))
                    .collect(Collectors.toList());
            if (CollectionUtil.isNotEmpty(bidList)) {
                ProblemReportVo vo = new ProblemReportVo();
                if ("X".equals(dept.getThridOrgType())) {
                    // 项目部层获取下面所有
                    vo = bidList.get(0);
                    queryVo.setProjectDepartmentId(dept.getThridDeptId());
                    vo.setChildren(baseMapper.select(queryVo));
                } else {
                    // 公司层获取统计数据
                    vo.setNum(bidList.stream().map(ProblemReportVo::getNum).reduce(BigDecimal.ZERO, BigDecimal::add));
                }
                vo.setId(dept.getThridDeptId());
                vo.setDeptName(dept.getDeptName());
                vo.setParentId(dept.getThridParentId());
                resultList.add(vo);
            }
        }
        return resultList;
    }

    @Override
    public List<ProblemReportVo> getProblemNext(ProblemReportVo queryVo) {
        List<ProblemReportVo> resultList = new ArrayList<>();
        // 组织范围由后端收敛：不在权限范围内的组织回落到本人所属组织
        String orgId = resolveScopeOrgId(queryVo);
        if (StringUtils.isEmpty(orgId)) {
            return resultList;
        }
        // 获取当前单位及下一层的组织
        List<SysDept> deptList = remoteSystemService.getDeptAndNextDept(queryVo.getId(), DeptTypeEnum.ALL_DEPT_TYPE.getType(), SecurityConstants.INNER);
        if (CollectionUtil.isNotEmpty(deptList)) {
            queryVo.setDeptId(String.valueOf(deptList.get(0).getDeptId()));
            List<ProblemReportVo> list = baseMapper.getProblemByDept(queryVo);
            if (CollectionUtil.isNotEmpty(list) && deptList.size() > 1) {
                resultList = getNextData(deptList, list, queryVo);
            }
        }
        return resultList;
    }

    @Override
    public Boolean handleProblemReport() {
        System.out.println("进入问题报表-异常报表定时刷新方法");
        List<ProblemReport> list = baseMapper.selectAll();
        return super.saveOrUpdateBatch(list);
    }

    @Override
    public Map<String, Object> problemReportExport(ProblemReportVo queryVo) {
        List<ProblemReportVo> resultList = new ArrayList<>();
        String title = "";
        // 组织范围由后端收敛：不在权限范围内的组织回落到本人所属组织
        String orgId = resolveScopeOrgId(queryVo);
        // 集团或公司端
        if (StringUtils.isNotEmpty(orgId)) {
            // 获取当前单位及下级所有单位
            List<SysDept> deptList = remoteSystemService.getDeptByThridDeptId(queryVo.getId(), SecurityConstants.INNER);
            if (CollectionUtil.isNotEmpty(deptList)) {
                title = deptList.get(0).getDeptName();
                // 对 deptList 排序
                deptList = deptList.stream()
                        .sorted(Comparator.comparing(sysDept -> sysDept.getAncestors() + "," + sysDept.getDeptId()))
                        .collect(Collectors.toList());

                // 获取组织统计数据
                queryVo.setDeptId(String.valueOf(deptList.get(0).getDeptId()));
                List<ProblemReportVo> list = baseMapper.getProblemByDept(queryVo);
                if (CollectionUtil.isNotEmpty(list)) {
                    for (SysDept sysDept : deptList) {
                        List<ProblemReportVo> bidList = list.stream().filter(item ->
                                        item.getId().equals(sysDept.getThridDeptId()) || item.getAncestors().contains(String.valueOf(sysDept.getDeptId())))
                                .collect(Collectors.toList());
                        if (CollectionUtil.isNotEmpty(bidList)) {
                            List<ProblemReportVo> children = new ArrayList<>();
                            ProblemReportVo vo = new ProblemReportVo();
                            vo.setNum(bidList.stream().map(ProblemReportVo::getNum).reduce(BigDecimal.ZERO, BigDecimal::add));
                            if ("X".equals(sysDept.getThridOrgType())) {
                                // 项目部层获取下面所有
                                vo.setFourDeptName(sysDept.getDeptName());
                                queryVo.setProjectDepartmentId(sysDept.getThridDeptId());
                                children = baseMapper.select(queryVo);
                            } else {
                                // 公司层获取统计数据
                                if (null != sysDept.getThridOrgLevel() && sysDept.getThridOrgLevel().equals(1)) {
                                    vo.setOneDeptName(sysDept.getDeptName());
                                } else if (null != sysDept.getThridOrgLevel() && sysDept.getThridOrgLevel().equals(2)) {
                                    vo.setTwoDeptName(sysDept.getDeptName());
                                } else {
                                    vo.setThreeDeptName(sysDept.getDeptName());
                                }
                            }
                            resultList.add(vo);
                            resultList.addAll(children);
                        }
                    }
                }
            }
        } else if (null != queryVo.getMinAccountCode()) {         // 项目端
            List<ProblemReportVo> vo = baseMapper.select(queryVo);
            if (CollectionUtil.isNotEmpty(vo)) {
                resultList.addAll(vo);
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("list", resultList);
        return map;
    }

    /**
     * 供应商评价不合格记录(tab2)
     *
     * @param queryVO
     * @return
     */
    @Override
    public Map<String, Object> getEvaluationBadReport(EvaluationBadReportVo queryVO) {
        List<EvaluationBadReportVo> list = baseMapper.selectEvaluationBad(queryVO);
        handleEvaluationBadName(list);
        Map<String, Object> map = new HashMap<>();
        map.put("total", list.size());
        map.put("list", getPage(list, queryVO.getPageNum(), queryVO.getPageSize()));
        return map;
    }

    /**
     * 供应商评价不合格记录导出(tab2)
     *
     * @param queryVO
     * @return
     */
    @Override
    public Map<String, Object> evaluationBadReportExport(EvaluationBadReportVo queryVO) {
        List<EvaluationBadReportVo> list = baseMapper.selectEvaluationBad(queryVO);
        handleEvaluationBadName(list);
        Map<String, Object> map = new HashMap<>();
        map.put("title", "");
        map.put("list", list);
        return map;
    }

    /**
     * 翻译评价类型/评价周期(口径与"供应商评价"菜单一致)
     *
     * @param list
     */
    private void handleEvaluationBadName(List<EvaluationBadReportVo> list) {
        if (CollectionUtil.isEmpty(list)) {
            return;
        }
        Map<String, String> evaluateTypeMap = sysDictDataService.listDictMap(DictBizEnum.evaluate_type.getName());
        Map<String, String> evaluateTimeMap = sysDictDataService.listDictMap(DictBizEnum.evaluate_time.getName());
        list.forEach(i -> {
            i.setTypeName(evaluateTypeMap.get(i.getEvaluateType()));
            if ("2".equals(i.getEvaluateType())) {
                // 季度：2025年 + 一季度/二季度...
                String year = DateUtils.parseDateToStr(DateUtils.YYYY, i.getEvaluateTime());
                i.setEvaluateTimeTxtName(year + "年" + evaluateTimeMap.get(i.getEvaluateTimeTxt()));
            } else if ("3".equals(i.getEvaluateType())) {
                // 年度
                i.setEvaluateTimeTxtName(DateUtils.parseDateToStr(DateUtils.YYYY, i.getEvaluateTime()));
            } else {
                // 月度
                i.setEvaluateTimeTxtName(DateUtils.parseDateToStr(DateUtils.YYYY_MM, i.getEvaluateTime()));
            }
        });
    }

    /**
     * 手工分页(前端传 pageNum/pageSize，缺省时按第1页10条)
     *
     * @param taskList
     * @param page
     * @param pageSize
     * @return
     */
    private List<EvaluationBadReportVo> getPage(List<EvaluationBadReportVo> taskList, int page, int pageSize) {
        int currentPage = page < 1 ? 1 : page;
        int currentSize = pageSize < 1 ? 10 : pageSize;
        int total = taskList.size();
        int fromIndex = (currentPage - 1) * currentSize;
        if (fromIndex >= total) {
            return new ArrayList<>();
        }
        int toIndex = Math.min(fromIndex + currentSize, total);
        return taskList.subList(fromIndex, toIndex);
    }

    /**
     * 校验并返回最终生效的组织id(第三方部门id)，同时把 queryVo.id 修正为该值
     * 前端传的组织不在登录用户数据权限范围内时，回落到登录用户所属组织
     *
     * @param queryVo 查询参数
     * @return 生效的组织id，取不到返回 null
     */
    private String resolveScopeOrgId(ProblemReportVo queryVo) {
        // 前端"组织机构"筛选传的是第三方部门id，先并入 id 再统一校验，避免绕过数据权限
        if (StringUtils.isNotEmpty(queryVo.getDeptId())) {
            queryVo.setId(queryVo.getDeptId());
        }
        String defaultOrgId = ReportScopeUtil.getDefaultOrgId();
        String orgId = StringUtils.isNotEmpty(queryVo.getId()) ? queryVo.getId() : defaultOrgId;
        if (StringUtils.isEmpty(orgId)) {
            return null;
        }
        // 非本人所属组织时要校验一次，越权则回落到本人所属组织
        if (!orgId.equals(defaultOrgId)) {
            List<SysDept> deptList = remoteSystemService.getDeptAndNextDept(orgId, DeptTypeEnum.ALL_DEPT_TYPE.getType(), SecurityConstants.INNER);
            if (CollectionUtil.isEmpty(deptList) || !ReportScopeUtil.inScope(deptList.get(0))) {
                orgId = defaultOrgId;
            }
        }
        if (StringUtils.isEmpty(orgId)) {
            return null;
        }
        queryVo.setId(orgId);
        return orgId;
    }
}
