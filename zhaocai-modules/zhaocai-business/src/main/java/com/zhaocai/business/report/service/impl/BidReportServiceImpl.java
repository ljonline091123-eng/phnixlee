package com.zhaocai.business.report.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.DeptTypeEnum;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.business.report.domain.BidReport;
import com.zhaocai.business.report.mapper.BidReportMapper;
import com.zhaocai.business.report.service.IBidReportService;
import com.zhaocai.business.report.util.ReportScopeUtil;
import com.zhaocai.business.report.vo.VBidCountVo;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 招标率统计报表
 *
 * 与成控(prod)的差异：
 * 1. 项目业态取值改为本地字典 project_format(DictBizEnum.UNDERLING_PROJECT_FORMAT)，不再走平台接口；
 * 2. 去掉“非招标总数”(nBidTotalNum)——中湘大连没有支出合同模块；
 * 3. 报表不受顶部“单位-项目”选择框限制：组织范围一律由后端按登录用户所属组织收敛(ReportScopeUtil)。
 */
@Service
public class BidReportServiceImpl extends ServiceImpl<BidReportMapper, BidReport> implements IBidReportService {

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Autowired
    private ISysDictDataService sysDictDataService;

    /**
     * 获取当前单位及下一层数据
     *
     * @param queryVo
     * @return
     */
    @Override
    public List<VBidCountVo> getInitialInfo(VBidCountVo queryVo) {
        List<VBidCountVo> resultList = new ArrayList<>();
        // 组织范围由后端收敛：不在权限范围内的组织回落到本人所属组织
        List<SysDept> deptList = getScopeDeptList(queryVo);
        // 集团或公司端
        if (CollectionUtil.isNotEmpty(deptList)) {
            // 获取组织统计数据
            queryVo.setDeptId(String.valueOf(deptList.get(0).getDeptId()));
            List<VBidCountVo> list = baseMapper.getBidCountByDept(queryVo);
            if (CollectionUtil.isNotEmpty(list)) {
                VBidCountVo rootVo = new VBidCountVo();
                rootVo = addInfo(rootVo, list);
                rootVo.setId(deptList.get(0).getThridDeptId());
                rootVo.setDeptName(deptList.get(0).getDeptName());
                rootVo.setParentId(deptList.get(0).getThridParentId());
                rootVo.setType("G");
                // 只有本级时也要返回空的 children，前端固定取 tableData[0].children
                rootVo.setChildren(deptList.size() > 1 ? getNextData(deptList, list, queryVo) : new ArrayList<>());
                resultList.add(rootVo);
            }
        } else if (StringUtils.isNotEmpty(queryVo.getMinAccountCode())) {         // 项目端
            List<VBidCountVo> vo = baseMapper.select(queryVo);
            if (CollectionUtil.isNotEmpty(vo)) {
                vo.get(0).setType("X");
                handlePrjStateName(vo);
                resultList.addAll(vo);
            }
        }
        return resultList;
    }

    /**
     * 获取下一层数据
     *
     * @param queryVo
     * @return
     */
    @Override
    public List<VBidCountVo> getBidCountNext(VBidCountVo queryVo) {
        List<VBidCountVo> resultList = new ArrayList<>();
        // 组织范围由后端收敛：不在权限范围内的组织回落到本人所属组织
        List<SysDept> deptList = getScopeDeptList(queryVo);
        if (CollectionUtil.isNotEmpty(deptList) && deptList.size() > 1) {
            // 行级过滤固定用根组织的 deptId
            queryVo.setDeptId(String.valueOf(deptList.get(0).getDeptId()));
            List<VBidCountVo> list = baseMapper.getBidCountByDept(queryVo);
            if (CollectionUtil.isNotEmpty(list)) {
                resultList = getNextData(deptList, list, queryVo);
            }
        }
        return resultList;
    }

    /**
     * 校验并返回最终生效的组织id(第三方部门id)，同时把 queryVo.id 修正为该值
     * 前端传的组织不在登录用户数据权限范围内时，回落到登录用户所属组织
     *
     * @param queryVo 查询参数
     * @return 生效的组织id，取不到返回 null
     */
    private String resolveScopeOrgId(VBidCountVo queryVo) {
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

    /**
     * 取报表要展示的组织(本级)及其下一层组织
     *
     * @param queryVo 查询参数，方法内会把 id 修正为最终生效的组织
     * @return 组织列表，第一个为根组织；取不到组织时返回空集合
     */
    private List<SysDept> getScopeDeptList(VBidCountVo queryVo) {
        String orgId = resolveScopeOrgId(queryVo);
        if (StringUtils.isEmpty(orgId)) {
            return new ArrayList<>();
        }
        return remoteSystemService.getDeptAndNextDept(orgId, DeptTypeEnum.ALL_DEPT_TYPE.getType(), SecurityConstants.INNER);
    }

    /**
     * 刷新数据库表数据
     *
     * @return
     */
    @Override
    public Boolean handleBidReport() {
        System.out.println("进入招标率报表定时刷新方法");
        List<BidReport> list = baseMapper.selectAll();
        return super.saveOrUpdateBatch(list);
    }

    /**
     * 招标率导出
     *
     * @param queryVo
     * @return
     */
    @Override
    public Map<String, Object> bidReportExport(VBidCountVo queryVo) {
        List<VBidCountVo> resultList = new ArrayList<>();
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
                List<VBidCountVo> list = baseMapper.getBidCountByDept(queryVo);
                if (CollectionUtil.isNotEmpty(list)) {
                    for (SysDept sysDept : deptList) {
                        List<VBidCountVo> bidList = list.stream().filter(item ->
                                        item.getId().equals(sysDept.getThridDeptId()) || item.getAncestors().contains(String.valueOf(sysDept.getDeptId())))
                                .collect(Collectors.toList());
                        if (CollectionUtil.isNotEmpty(bidList)) {
                            VBidCountVo vo = new VBidCountVo();
                            vo.setId(sysDept.getThridDeptId());
                            vo.setDeptName(sysDept.getDeptName());
                            vo.setParentId(sysDept.getThridParentId());
                            if ("X".equals(sysDept.getThridOrgType())) {
                                // 项目部层获取下面所有
                                vo = bidList.get(0);
                                vo.setFourDeptName(sysDept.getDeptName());
                                resultList.add(vo);
                                resultList.addAll(getXInfo(sysDept.getThridDeptId(), queryVo));
                            } else {
                                // 公司层获取统计数据
                                if (null != sysDept.getThridOrgLevel() && sysDept.getThridOrgLevel().equals(1)) {
                                    vo.setOneDeptName(sysDept.getDeptName());
                                } else if (null != sysDept.getThridOrgLevel() && sysDept.getThridOrgLevel().equals(2)) {
                                    vo.setTwoDeptName(sysDept.getDeptName());
                                } else {
                                    vo.setThreeDeptName(sysDept.getDeptName());
                                }
                                vo = addInfo(vo, bidList);
                                resultList.add(vo);
                            }
                        }
                    }
                }
            }

        } else if (StringUtils.isNotEmpty(queryVo.getMinAccountCode())) {         // 项目端
            List<VBidCountVo> vo = baseMapper.select(queryVo);
            if (CollectionUtil.isNotEmpty(vo)) {
                title = vo.get(0).getMinAccountFullName();
                vo.get(0).setType("X");
                handlePrjStateName(vo);
                resultList.addAll(vo);
            }
        }
        Map<String, Object> map = new HashMap<>();
        map.put("title", title);
        map.put("list", resultList);
        return map;
    }

    /**
     * 获取下一层数据
     *
     * @param deptList
     * @param list
     * @param queryVo
     * @return
     */
    private List<VBidCountVo> getNextData(List<SysDept> deptList, List<VBidCountVo> list, VBidCountVo queryVo) {
        List<VBidCountVo> resultList = new ArrayList<>();
        for (int i = 1; i < deptList.size(); i++) {
            SysDept dept = deptList.get(i);
            List<VBidCountVo> bidList = list.stream().filter(item ->
                            item.getId().equals(dept.getThridDeptId()) || item.getAncestors().contains(String.valueOf(dept.getDeptId())))
                    .collect(Collectors.toList());
            if (CollectionUtil.isNotEmpty(bidList)) {
                VBidCountVo vo = new VBidCountVo();
                if ("X".equals(dept.getThridOrgType())) {
                    // 项目部层获取下面所有
                    vo = bidList.get(0);
                    vo.setChildren(getXInfo(dept.getThridDeptId(), queryVo));
                } else {
                    // 公司层获取统计数据
                    vo = addInfo(vo, bidList);
                }
                vo.setId(dept.getThridDeptId());
                vo.setDeptName(dept.getDeptName());
                vo.setParentId(dept.getThridParentId());
                vo.setType("G");
                resultList.add(vo);
            }
        }
        return resultList;
    }

    /**
     * 统计数据
     *
     * @param vo
     * @param bidList
     * @return
     */
    private VBidCountVo addInfo(VBidCountVo vo, List<VBidCountVo> bidList) {
        // 初始化BigDecimal变量
        BigDecimal cgNum = BigDecimal.ZERO;
        BigDecimal gkNum = BigDecimal.ZERO;
        BigDecimal yqNum = BigDecimal.ZERO;
        BigDecimal xjNum = BigDecimal.ZERO;
        BigDecimal dyNum = BigDecimal.ZERO;
        BigDecimal gkTotalNum = BigDecimal.ZERO;
        BigDecimal ngkTotalNum = BigDecimal.ZERO;
        // 一次性遍历列表，计算所有需要的值
        for (VBidCountVo item : bidList) {
            cgNum = cgNum.add(item.getCgNum());
            gkNum = gkNum.add(item.getGkNum());
            yqNum = yqNum.add(item.getYqNum());
            xjNum = xjNum.add(item.getXjNum());
            dyNum = dyNum.add(item.getDyNum());
            gkTotalNum = gkTotalNum.add(item.getGkTotalNum());
            ngkTotalNum = ngkTotalNum.add(item.getNgkTotalNum());
        }
        // 设置计算结果
        vo.setCgNum(cgNum);
        vo.setGkNum(gkNum);
        vo.setYqNum(yqNum);
        vo.setXjNum(xjNum);
        vo.setDyNum(dyNum);
        vo.setGkTotalNum(gkTotalNum);
        vo.setNgkTotalNum(ngkTotalNum);
        // 计算比率
        if (cgNum.compareTo(BigDecimal.ZERO) > 0) {
            vo.setGkRatio(gkNum.divide(cgNum, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
        }
        return vo;
    }

    /**
     * 项目部层获取下面所有
     *
     * @param thridDeptId
     * @return
     */
    private List<VBidCountVo> getXInfo(String thridDeptId, VBidCountVo queryVo) {
        queryVo.setProjectDepartmentId(thridDeptId);
        List<VBidCountVo> list = baseMapper.select(queryVo);
        if (CollectionUtil.isNotEmpty(list)) {
            handlePrjStateName(list);
            list.parallelStream().forEach(i -> i.setType("X"));
        }
        return list;
    }

    /**
     * 项目业态名称翻译(本地字典 project_format)
     *
     * @param list
     */
    private void handlePrjStateName(List<VBidCountVo> list) {
        Map<String, String> projectTypeList = sysDictDataService.listDictMap(DictBizEnum.UNDERLING_PROJECT_FORMAT.getName());
        list.parallelStream().forEach(i ->
                i.setPrjStateName(StringUtils.isNotEmpty(i.getPrjState()) ? projectTypeList.get(i.getPrjState()) : null));
    }

}
