package com.zhaocai.business.report.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.DeptTypeEnum;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.procurement.domain.MinProject;
import com.zhaocai.business.procurement.service.IMinProjectService;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.business.pub.vo.res.DictListVO;
import com.zhaocai.business.report.domain.VendorReport;
import com.zhaocai.business.report.mapper.VendorReportMapper;
import com.zhaocai.business.report.service.IVendorReportService;
import com.zhaocai.business.report.util.ReportScopeUtil;
import com.zhaocai.business.report.vo.req.VendorReportQueryVo;
import com.zhaocai.business.report.vo.res.VendorReportListVo;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 供应商报表
 *
 * 与成控(prod)的差异：
 * 1. "项目评分汇总"改为展示供应商评价(tb_vendor_evaluate)确认状态的"合格n次，不合格m次"，并按合格次数降序(见 XML)；
 * 2. 报表不受顶部"单位-项目"选择框限制：组织范围一律由后端按登录用户所属组织收敛(ReportScopeUtil)，
 *    前端未传"合作单位"时默认查本人所属单位及下级单位的数据，不再全量返回。
 */
@Service
public class VendorReportServiceImpl extends ServiceImpl<VendorReportMapper, VendorReport> implements IVendorReportService {

    @Autowired
    private ISysDictDataService dictDataService;

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Autowired
    private IMinProjectService minProjectService;

    @Override
    public Map<String, Object> getVendorReport(VendorReportQueryVo vo) {
        List<VendorReportListVo> result = new ArrayList<>();
        Map<String, Object> map = new HashMap<>();
        // 组织范围由后端收敛：不在权限范围内的单位回落到本人所属单位
        String orgId = resolveScopeOrgId(vo);
        if (StringUtils.isEmpty(orgId)) {
            // 取不到登录用户所属单位时按无数据返回，避免越权全量查询
            map.put("total", 0);
            map.put("list", result);
            return map;
        }
        List<VendorReportListVo> list = baseMapper.select(vo);
        if (CollectionUtil.isNotEmpty(list)) {
            map.put("total", list.size());
            result = getPage(list, vo.getPageNum(), vo.getPageSize());
            List<DictListVO> rentalType = dictDataService.listDictByType(DictBizEnum.PROCUREMENT_PLAN_TYPE.getName());
            Map<String, String> rentalTypeMap = rentalType.stream()
                    .collect(Collectors.toMap(
                            DictListVO::getDictValue,  // 键为 code
                            DictListVO::getDictLabel   // 值为 name
                    ));
            result.forEach(i -> {
                i.setExpenditureBusinessTypeText(rentalTypeMap.get(i.getExpenditureBusinessType()));
            });
        } else {
            map.put("total", 0);
        }
        map.put("list", result);
        return map;
    }

    @Override
    public Boolean handleVendorReport() {
        System.out.println("进入供应商报表定时刷新方法");
        List<VendorReport> list = baseMapper.selectAll();
        return super.saveOrUpdateBatch(BeanCopierUtil.copyList(list, VendorReport.class));
    }

    @Override
    public Map<String, Object> vendorReportExport(VendorReportQueryVo vo) {
        Map<String, Object> map = new HashMap<>();
        List<VendorReportListVo> list = new ArrayList<>();
        String title = "";
        // 组织范围由后端收敛：不在权限范围内的单位回落到本人所属单位
        String orgId = resolveScopeOrgId(vo);
        if (StringUtils.isEmpty(orgId)) {
            map.put("title", title);
            map.put("list", list);
            return map;
        }
        list = baseMapper.select(vo);
        if (CollectionUtil.isNotEmpty(list)) {
            List<DictListVO> rentalType = dictDataService.listDictByType(DictBizEnum.PROCUREMENT_PLAN_TYPE.getName());
            Map<String, String> rentalTypeMap = rentalType.stream()
                    .collect(Collectors.toMap(
                            DictListVO::getDictValue,  // 键为 code
                            DictListVO::getDictLabel   // 值为 name
                    ));
            list.forEach(i -> {
                i.setExpenditureBusinessTypeText(rentalTypeMap.get(i.getExpenditureBusinessType()));
            });
            if (null != vo.getId()) {
                SysDept dept = remoteSystemService.getByThridDeptId(vo.getId(), SecurityConstants.INNER);
                title = dept == null ? "" : dept.getDeptName();
            } else {
                MinProject project = minProjectService.getOne(new LambdaQueryWrapper<MinProject>()
                        .eq(MinProject::getMinAccountCode, list.get(0).getMinAccountCode()));
                title = project == null ? "" : project.getMinAccountFullName();
            }
        }
        map.put("title", title);
        map.put("list", list);
        return map;
    }

    /**
     * 校验并返回最终生效的单位id(第三方部门id)，同时把 vo.id / vo.scopeId 修正好
     * - vo.id：前端"合作单位"筛选值，未传时默认取登录用户所属单位
     * - vo.scopeId：数据权限上限，集团账号为 null(不限制)，其他账号为本人所属单位
     *
     * @param vo 查询参数
     * @return 生效的单位id，取不到返回 null
     */
    private String resolveScopeOrgId(VendorReportQueryVo vo) {
        // 前端"合作单位"筛选传的是第三方部门id
        if (null != vo.getDeptId()) {
            vo.setId(vo.getDeptId());
        }
        // 数据权限上限：为空表示不限制(集团账号)
        vo.setScopeId(ReportScopeUtil.getScopeThridDeptId());
        String defaultOrgId = ReportScopeUtil.getDefaultOrgId();
        String orgId = StringUtils.isNotEmpty(vo.getId()) ? vo.getId() : defaultOrgId;
        if (StringUtils.isEmpty(orgId)) {
            if (ReportScopeUtil.needScope()) {
                // 非集团账号又取不到本人所属单位，直接不给数据
                return null;
            }
            vo.setId(null);
            return orgId;
        }
        // 非本人所属单位时要校验一次，越权则回落到本人所属单位
        if (!orgId.equals(defaultOrgId)) {
            List<SysDept> deptList = remoteSystemService.getDeptAndNextDept(orgId, DeptTypeEnum.ALL_DEPT_TYPE.getType(), SecurityConstants.INNER);
            if (CollectionUtil.isEmpty(deptList) || !ReportScopeUtil.inScope(deptList.get(0))) {
                orgId = defaultOrgId;
            }
        }
        if (StringUtils.isEmpty(orgId)) {
            return null;
        }
        vo.setId(orgId);
        return orgId;
    }

    /**
     * 手工分页(前端传 pageNum/pageSize，缺省时按第1页10条)
     *
     * @param taskList
     * @param page
     * @param pageSize
     * @return
     */
    private List<VendorReportListVo> getPage(List<VendorReportListVo> taskList, int page, int pageSize) {
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

}
