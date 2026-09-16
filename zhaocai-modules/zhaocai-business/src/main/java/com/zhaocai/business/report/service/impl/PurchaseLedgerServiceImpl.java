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
     * 汇总视图聚合查询
     */
    @Override
    public List<PurchaseLedgerSummaryVo> getSummary(PurchaseLedgerQueryVo queryVo) {
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
     * 汇总视图导出
     */
    @Override
    public Map<String, Object> summaryExport(PurchaseLedgerQueryVo queryVo) {
        Map<String, Object> map = new HashMap<>();
        map.put("title", "采购台账");
        map.put("list", getSummary(queryVo));
        return map;
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
