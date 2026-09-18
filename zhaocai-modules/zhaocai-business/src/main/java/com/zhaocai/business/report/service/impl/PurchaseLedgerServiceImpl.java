package com.zhaocai.business.report.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.AgreementStateEnum;
import com.zhaocai.business.common.enums.DeptTypeEnum;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.business.pub.vo.res.DictListVO;
import com.zhaocai.business.report.domain.PurchaseLedger;
import com.zhaocai.business.report.mapper.PurchaseLedgerMapper;
import com.zhaocai.business.report.service.IPurchaseLedgerService;
import com.zhaocai.business.report.util.ReportScopeUtil;
import com.zhaocai.business.report.vo.req.PurchaseLedgerQueryVo;
import com.zhaocai.business.report.vo.res.PurchaseLedgerListVo;
import com.zhaocai.business.report.vo.res.PurchaseLedgerSummaryVo;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 采购台账报表服务
 *
 * 口径（2026-09-15 与业务确认，勿改动）：
 * 待招采   = 计划 state=3 的拆分项，未被"state≠2 且已发布非废标公告"方案占用；
 * 待开标   = 公告 notice_status ∈ {11,12,13,1}；
 * 待定标   = 公告 notice_status ∈ {2,3,5,6}；
 * 已完成   = 公告 notice_status ∈ {7,8}；
 * 异常/终止= 公告 notice_status=0 且公告 state=3（真正发布过又被废标，方案作废连带的废标不统计）。
 *
 * 数据权限：报表不受顶部"单位-项目"选择框限制，默认范围为登录用户所属组织及其下级，
 * 由 ReportScopeUtil 在后端强制收敛；前端传参只做范围内缩小。
 *
 * @author claude
 */
@Service
public class PurchaseLedgerServiceImpl extends ServiceImpl<PurchaseLedgerMapper, PurchaseLedger> implements IPurchaseLedgerService {

    @Autowired
    private ISysDictDataService dictDataService;

    @Autowired
    private RemoteSystemService remoteSystemService;

    /**
     * 刷新物化表：清空后从视图全量灌入（数据量小，全量重建即可）
     * 删+插在同一事务：插入失败则回滚，物化表保留上一次的完整数据
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean handlePurchaseLedgerReport() {
        System.out.println("进入采购台账报表定时刷新方法");
        baseMapper.deleteAll();
        int count = baseMapper.insertFromView();
        System.out.println("采购台账报表刷新完成，共 " + count + " 行");
        return true;
    }

    /**
     * 汇总视图查询：平铺聚合 → 组装 单位→部门→项目行 三层树
     */
    @Override
    public List<PurchaseLedgerSummaryVo> getSummary(PurchaseLedgerQueryVo queryVo) {
        return buildSummaryTree(getSummaryFlat(queryVo), queryVo.getId());
    }

    /**
     * 汇总视图导出（保持平铺明细，不随页面改树）
     */
    @Override
    public Map<String, Object> summaryExport(PurchaseLedgerQueryVo queryVo) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", "采购台账");
        map.put("list", getSummaryFlat(queryVo));
        return map;
    }

    /**
     * 平铺聚合查询（组织 × 项目 × 采购需求类型），并翻译需求类型名称
     */
    private List<PurchaseLedgerSummaryVo> getSummaryFlat(PurchaseLedgerQueryVo queryVo) {
        if (!resolveScopeOrgId(queryVo)) {
            return new ArrayList<>();
        }
        List<PurchaseLedgerSummaryVo> list = baseMapper.selectSummary(queryVo);
        if (CollectionUtil.isNotEmpty(list)) {
            Map<String, String> dictMap = getDictMap(DictBizEnum.PROCUREMENT_PLAN_TYPE);
            list.forEach(vo -> vo.setDemandTypeText(dictMap.getOrDefault(
                    vo.getDemandType() == null ? "" : String.valueOf(vo.getDemandType()), "")));
        }
        return list;
    }

    /**
     * 把平铺汇总行组装成 单位 → 部门 → 项目 → 采购需求类型 四级树
     *
     * 组织骨架：getDeptAndNextDept(scopeOrgId, 含公司、部门,不含项目部) 取根单位 + 直属部门。
     * 台账叶子行（项目×需求类型）按 org_id（管理组织=部门）归组：先按项目合并成项目节点，
     * 需求类型行挂在项目下；管理组织不在骨架里（如项目部）则回落到根单位，保证数据不丢。
     * 各级节点五状态 = 其下所有叶子行计数向上求和。
     *
     * @param flatList    平铺汇总行（组织×项目×需求类型）
     * @param scopeOrgId  生效范围组织id(第三方部门id)，getSummaryFlat 已把 queryVo.id 修正为它
     */
    private List<PurchaseLedgerSummaryVo> buildSummaryTree(List<PurchaseLedgerSummaryVo> flatList, String scopeOrgId) {
        if (CollectionUtil.isEmpty(flatList) || StringUtils.isEmpty(scopeOrgId)) {
            return flatList;
        }
        List<SysDept> deptList = remoteSystemService.getDeptAndNextDept(
                scopeOrgId, DeptTypeEnum.NOT_X_DEPT_TYPE.getType(), SecurityConstants.INNER);
        if (CollectionUtil.isEmpty(deptList)) {
            return flatList;
        }
        SysDept unitDept = deptList.get(0);

        // 单位节点（根）
        PurchaseLedgerSummaryVo unitNode = new PurchaseLedgerSummaryVo();
        unitNode.setId(unitDept.getThridDeptId());
        unitNode.setType("U");
        unitNode.setOrgId(unitDept.getThridDeptId());
        unitNode.setOrgName(unitDept.getDeptName());

        // 部门节点：部门 thrid_dept_id -> 节点
        Map<String, PurchaseLedgerSummaryVo> deptNodeMap = new LinkedHashMap<>();
        for (int i = 1; i < deptList.size(); i++) {
            SysDept dept = deptList.get(i);
            PurchaseLedgerSummaryVo deptNode = new PurchaseLedgerSummaryVo();
            deptNode.setId(dept.getThridDeptId());
            deptNode.setType("D");
            deptNode.setOrgId(dept.getThridDeptId());
            deptNode.setOrgName(dept.getDeptName());
            deptNodeMap.put(dept.getThridDeptId(), deptNode);
        }

        // 叶子行(项目×需求类型) → 需求类型节点(T)；按 部门 → 项目 两级归组
        // 项目节点 key=orgId_projectCode；管理组织不在部门骨架里的回落到根单位
        Map<String, PurchaseLedgerSummaryVo> projectNodeMap = new LinkedHashMap<>();
        Map<String, List<PurchaseLedgerSummaryVo>> deptProjectList = new LinkedHashMap<>(); // 组织id -> 项目节点，null=回落到单位
        for (PurchaseLedgerSummaryVo row : flatList) {
            row.setType("T");
            row.setId(row.getOrgId() + "_"
                    + (row.getProjectCode() == null ? "" : row.getProjectCode()) + "_"
                    + (row.getDemandType() == null ? "" : row.getDemandType()));

            String parentOrgId = deptNodeMap.containsKey(row.getOrgId()) ? row.getOrgId() : null;
            String projectKey = (parentOrgId == null ? "" : parentOrgId) + "_"
                    + (row.getProjectCode() == null ? "" : row.getProjectCode());
            PurchaseLedgerSummaryVo projectNode = projectNodeMap.get(projectKey);
            if (projectNode == null) {
                projectNode = new PurchaseLedgerSummaryVo();
                projectNode.setId(projectKey);
                projectNode.setType("P");
                projectNode.setOrgId(parentOrgId);
                projectNode.setProjectCode(row.getProjectCode());
                projectNode.setProjectName(row.getProjectName());
                projectNodeMap.put(projectKey, projectNode);
                deptProjectList.computeIfAbsent(parentOrgId, k -> new ArrayList<>()).add(projectNode);
            }
            projectNode.getChildren().add(row);
        }

        // 项目求和后挂到部门（无项目的部门不展示），部门求和后挂到单位；回落到单位的项目直接挂单位
        for (PurchaseLedgerSummaryVo deptNode : deptNodeMap.values()) {
            List<PurchaseLedgerSummaryVo> projects = deptProjectList.get(deptNode.getOrgId());
            if (CollectionUtil.isNotEmpty(projects)) {
                for (PurchaseLedgerSummaryVo projectNode : projects) {
                    sumNode(projectNode);
                    deptNode.getChildren().add(projectNode);
                }
                sumNode(deptNode);
                unitNode.getChildren().add(deptNode);
            }
        }
        List<PurchaseLedgerSummaryVo> unitProjects = deptProjectList.get(null);
        if (CollectionUtil.isNotEmpty(unitProjects)) {
            for (PurchaseLedgerSummaryVo projectNode : unitProjects) {
                sumNode(projectNode);
                unitNode.getChildren().add(projectNode);
            }
        }
        sumNode(unitNode);

        List<PurchaseLedgerSummaryVo> result = new ArrayList<>();
        result.add(unitNode);
        return result;
    }

    /** 五状态计数向上求和（子节点已含各自计数） */
    private void sumNode(PurchaseLedgerSummaryVo node) {
        long pending = 0L, preopen = 0L, preaward = 0L, completed = 0L, exception = 0L;
        for (PurchaseLedgerSummaryVo child : node.getChildren()) {
            pending += count(child.getPending());
            preopen += count(child.getPreopen());
            preaward += count(child.getPreaward());
            completed += count(child.getCompleted());
            exception += count(child.getException());
        }
        node.setPending(pending);
        node.setPreopen(preopen);
        node.setPreaward(preaward);
        node.setCompleted(completed);
        node.setException(exception);
    }

    private long count(Long v) {
        return v == null ? 0L : v;
    }

    /**
     * 明细宽表查询（分页）
     */
    @Override
    public Map<String, Object> getLedgerList(PurchaseLedgerQueryVo queryVo) {
        Map<String, Object> map = new HashMap<>();
        if (!resolveScopeOrgId(queryVo)) {
            map.put("total", 0);
            map.put("list", new ArrayList<>());
            return map;
        }
        List<PurchaseLedgerListVo> list = baseMapper.selectPageList(queryVo);
        if (CollectionUtil.isNotEmpty(list)) {
            fillDict(list);
            map.put("total", list.size());
            map.put("list", getPage(list, queryVo.getPageNum(), queryVo.getPageSize()));
        } else {
            map.put("total", 0);
            map.put("list", list);
        }
        return map;
    }

    /**
     * 明细宽表导出（完整结果，不受分页限制）
     */
    @Override
    public Map<String, Object> ledgerExport(PurchaseLedgerQueryVo queryVo) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", "采购台账");
        if (!resolveScopeOrgId(queryVo)) {
            map.put("list", new ArrayList<>());
            return map;
        }
        map.put("list", baseMapper.selectPageList(queryVo));
        if (CollectionUtil.isNotEmpty((List<PurchaseLedgerListVo>) map.get("list"))) {
            fillDict((List<PurchaseLedgerListVo>) map.get("list"));
        }
        return map;
    }

    /**
     * 明细行字段翻译：需求类型/采购方式/五类状态/合同状态
     */
    private void fillDict(List<PurchaseLedgerListVo> list) {
        Map<String, String> demandMap = getDictMap(DictBizEnum.PROCUREMENT_PLAN_TYPE);
        Map<String, String> methodMap = getDictMap(DictBizEnum.PROCUREMENT_TYPE);
        list.forEach(vo -> {
            vo.setDemandTypeText(demandMap.getOrDefault(
                    vo.getDemandType() == null ? "" : String.valueOf(vo.getDemandType()), ""));
            vo.setMethodText(methodMap.getOrDefault(
                    vo.getMethod() == null ? "" : String.valueOf(vo.getMethod()), ""));
            vo.setStatusText(statusText(vo.getStatus()));
            vo.setContractStateText(contractStateText(vo.getContractState()));
        });
    }

    /**
     * 五类台账状态翻译
     */
    private String statusText(String status) {
        if (StringUtils.isEmpty(status)) {
            return "";
        }
        switch (status) {
            case "pending":
                return "待招采";
            case "preopen":
                return "待开标";
            case "preaward":
                return "待定标";
            case "completed":
                return "已完成";
            case "exception":
                return "异常/终止";
            default:
                return status;
        }
    }

    /**
     * 合同状态翻译（物化表存多个数字逗号拼接，逐个翻译再拼接）
     */
    private String contractStateText(String contractState) {
        if (StringUtils.isEmpty(contractState)) {
            return null;
        }
        String[] codes = contractState.split(",");
        StringBuilder sb = new StringBuilder();
        for (String code : codes) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            String text = null;
            try {
                Integer state = Integer.valueOf(code.trim());
                for (AgreementStateEnum e : AgreementStateEnum.values()) {
                    if (e.equalsState(state)) {
                        text = e.getDesc();
                        break;
                    }
                }
            } catch (NumberFormatException ignored) {
            }
            sb.append(text == null ? code : text);
        }
        return sb.toString();
    }

    /**
     * 校验并收敛组织范围，同时把 scopeId / id 修正为最终生效值
     *
     * @return false 表示取不到登录用户所属组织（非集团账号），按无数据返回避免越权
     */
    private boolean resolveScopeOrgId(PurchaseLedgerQueryVo queryVo) {
        // 数据权限上限：为空表示不限制(集团账号)
        queryVo.setScopeId(ReportScopeUtil.getScopeThridDeptId());
        String defaultOrgId = ReportScopeUtil.getDefaultOrgId();
        // 前端筛选树与左树均传第三方部门id(deptId/id)
        String orgId = StringUtils.isNotEmpty(queryVo.getId()) ? queryVo.getId()
                : (StringUtils.isNotEmpty(queryVo.getDeptId()) ? queryVo.getDeptId() : defaultOrgId);
        if (StringUtils.isEmpty(orgId)) {
            if (ReportScopeUtil.needScope()) {
                // 非集团账号又取不到本人所属单位，直接不给数据
                return false;
            }
            queryVo.setId(null);
            return true;
        }
        // 非本人所属组织时要校验一次，越权则回落到本人所属组织
        if (!orgId.equals(defaultOrgId)) {
            List<SysDept> deptList = remoteSystemService.getDeptAndNextDept(orgId, DeptTypeEnum.NOT_BM_DEPT_TYPE.getType(), SecurityConstants.INNER);
            if (CollectionUtil.isEmpty(deptList) || !ReportScopeUtil.inScope(deptList.get(0))) {
                orgId = defaultOrgId;
            }
        }
        if (StringUtils.isEmpty(orgId)) {
            return !ReportScopeUtil.needScope();
        }
        queryVo.setId(orgId);
        return true;
    }

    /**
     * 取字典 Map（code -> label）
     */
    private Map<String, String> getDictMap(DictBizEnum dictBizEnum) {
        List<DictListVO> dictList = dictDataService.listDictByType(dictBizEnum.getName());
        return dictList.stream().collect(Collectors.toMap(
                DictListVO::getDictValue, DictListVO::getDictLabel, (a, b) -> a));
    }

    /**
     * Java 内部分页（照供应商报表模式：明细先查全量再分页）
     */
    private List<PurchaseLedgerListVo> getPage(List<PurchaseLedgerListVo> list, int page, int pageSize) {
        int currentPage = page < 1 ? 1 : page;
        int currentSize = pageSize < 1 ? 10 : pageSize;
        int total = list.size();
        int fromIndex = (currentPage - 1) * currentSize;
        if (fromIndex >= total) {
            return new ArrayList<>();
        }
        int toIndex = Math.min(fromIndex + currentSize, total);
        return list.subList(fromIndex, toIndex);
    }
}
