package com.zhaocai.business.report.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.pub.service.ISysDictDataService;
import com.zhaocai.business.report.mapper.VBidCountMapper;
import com.zhaocai.business.report.service.IVBidCountService;
import com.zhaocai.business.report.vo.VBidCountVo;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class VBidCountServiceImpl extends ServiceImpl<VBidCountMapper, VBidCountVo> implements IVBidCountService {

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Autowired
    private ISysDictDataService sysDictDataService;

    /**
     * 招标率报表
     * @param vBidCountVo
     * @return
     */
    @Override
    public List<VBidCountVo> bidCountReport(VBidCountVo vBidCountVo) {
        List<VBidCountVo> resultList = new ArrayList<>();

        List<VBidCountVo> bidCountList = baseMapper.select(vBidCountVo);
        bidCountList = this.handleDict(bidCountList);
        // 集团或公司端
        if(null != vBidCountVo.getId() && !CollectionUtils.isEmpty(bidCountList)){
            // 获取组织结构(本级及以下)
            if(null != vBidCountVo.getDeptId()){
                vBidCountVo.setId(vBidCountVo.getDeptId());
            }
            String thridDeptId = vBidCountVo.getId();
            List<SysDept> deptList = remoteSystemService.getDeptByThridDeptId(thridDeptId, SecurityConstants.INNER);
            if(!CollectionUtils.isEmpty(deptList)){
                // 将组织机构数据汇总
                for (SysDept sysDept : deptList) {
                    if(!sysDept.getThridOrgType().equals("BM")){
                        Set<String> idSet = deptList.parallelStream()
                                .filter(i -> null != i.getAncestors() && i.getAncestors().contains(sysDept.getAncestors() + "," + sysDept.getDeptId()))
                                .map(SysDept::getThridDeptId)
                                .collect(Collectors.toSet());
                        List<VBidCountVo> list = bidCountList.parallelStream().filter(i->idSet.contains(i.getProjectDepartmentId())
                                || i.getProjectDepartmentId().equals(sysDept.getThridDeptId()) ).collect(Collectors.toList());
                        if(!CollectionUtils.isEmpty(list)){
                            VBidCountVo vo = this.getBidCount(sysDept, list);
                            if (sysDept.getThridOrgType().equals("X")) {
                                vo.setChildren(this.getProject(list, sysDept));
                            }
                            resultList.add(vo);
                        }
                    }
                }
                // 构建组织树
                List<VBidCountVo> tree = resultList.parallelStream().filter(i -> i.getId().equals(vBidCountVo.getId())).collect(Collectors.toList());
                if(!CollectionUtils.isEmpty(tree)){
                    tree.get(0).setChildren(this.createDeptTree(resultList, vBidCountVo.getId()));
                }
                return tree;
            }
        }
        // 项目端
        if(null != vBidCountVo.getMinAccountCode() && !CollectionUtils.isEmpty(bidCountList)){
            // 获取组织结构(本级及以下)
            List<SysDept> deptList = remoteSystemService.getDeptByThridDeptId(bidCountList.get(0).getProjectDepartmentId(), SecurityConstants.INNER);
            if(!CollectionUtils.isEmpty(deptList)){
                VBidCountVo vo = this.getBidCount(deptList.get(0), bidCountList);
                vo.setChildren(this.getProject(bidCountList, deptList.get(0)));
                resultList.add(vo);
            }
        }
        return resultList;
    }

    /**
     * 招标率报表(懒加载)
     * @param vBidCountVo
     * @return
     */
    @Override
    public List<VBidCountVo> bidCountReportLazy(VBidCountVo vBidCountVo) {
        List<VBidCountVo> resultList = new ArrayList<>();

        List<VBidCountVo> bidCountList = baseMapper.select(vBidCountVo);
        bidCountList = this.handleDict(bidCountList);
        if(null != vBidCountVo.getId() && !CollectionUtils.isEmpty(bidCountList)) {
            // 获取本级组织
            if(null != vBidCountVo.getDeptId() && vBidCountVo.getLevel().equals("0")){
                vBidCountVo.setId(vBidCountVo.getDeptId());
            }
            List<SysDept> deptList = remoteSystemService.getDeptByThridDeptId(vBidCountVo.getId(), SecurityConstants.INNER);
            // 组织第0层
            if (!CollectionUtils.isEmpty(deptList) && vBidCountVo.getLevel().equals("0")) {
                SysDept sysDept = deptList.get(0);
                Set<String> idSet = deptList.stream()
                        .filter(i -> null != i.getAncestors() && i.getAncestors().contains(sysDept.getAncestors() + "," + sysDept.getDeptId()))
                        .map(SysDept::getThridDeptId)
                        .collect(Collectors.toSet());
                List<VBidCountVo> list = bidCountList.stream().filter(i -> idSet.contains(i.getProjectDepartmentId())
                        || i.getProjectDepartmentId().equals(sysDept.getThridDeptId()) ).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(list)) {
                    VBidCountVo vo = this.getBidCount(sysDept, list);
                    resultList.add(vo);
                }
                // 组织中项目部层
            } else if (!CollectionUtils.isEmpty(deptList) && deptList.get(0).getThridOrgType().equals("X")) {
                List<VBidCountVo> list = bidCountList.stream().filter(i -> i.getProjectDepartmentId().equals(vBidCountVo.getId())).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(list)) {
                    list.forEach(i -> {
                        i.setId(i.getMinAccountCode());
                        i.setParentId(vBidCountVo.getId());
                        i.setType("X");
                    });
                    resultList = list;
                }
            } else {
                // 获取组织下一级
                List<SysDept> nextDeptList = deptList.stream().filter(i -> i.getThridParentId().equals(i.getThridDeptId())).collect(Collectors.toList());
                // 将组织机构数据汇总
                for (SysDept sysDept : nextDeptList) {
                    Set<String> idSet = deptList.stream()
                            .filter(i -> null != i.getAncestors() && i.getAncestors().contains(sysDept.getAncestors() + "," + sysDept.getDeptId()))
                            .map(SysDept::getThridDeptId)
                            .collect(Collectors.toSet());
                    List<VBidCountVo> list = bidCountList.stream().filter(i -> idSet.contains(i.getProjectDepartmentId())
                            || i.getProjectDepartmentId().equals(sysDept.getThridDeptId()) ).collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(list)) {
                        VBidCountVo vo = this.getBidCount(sysDept, list);
                        resultList.add(vo);
                    }
                }
            }
        }
        if(null != vBidCountVo.getMinAccountCode() && !CollectionUtils.isEmpty(bidCountList)) {
            // 获取组织结构(本级及以下)
            List<SysDept> deptList = remoteSystemService.getDeptByThridDeptId(bidCountList.get(0).getProjectDepartmentId(), SecurityConstants.INNER);
            if(!CollectionUtils.isEmpty(deptList)){
                VBidCountVo vo = this.getBidCount(deptList.get(0), bidCountList);
                vo.setChildren(this.getProject(bidCountList, deptList.get(0)));
                resultList.add(vo);
            }
        }
        return resultList;
    }

    /**
     * 导出报表
     * @param vBidCountVo
     * @return
     */
    @Override
    public List<VBidCountVo> bidCountReportExport(VBidCountVo vBidCountVo) {
        List<VBidCountVo> resultList = new ArrayList<>();

        List<VBidCountVo> bidCountList = baseMapper.select(vBidCountVo);
        bidCountList = this.handleDict(bidCountList);
        // 集团或公司端
        if(null != vBidCountVo.getId() && !CollectionUtils.isEmpty(bidCountList)){
            // 获取组织结构(本级及以下)
            if(null != vBidCountVo.getDeptId()){
                vBidCountVo.setId(vBidCountVo.getDeptId());
            }
            String thridDeptId = vBidCountVo.getId();
            List<SysDept> deptList = remoteSystemService.getDeptByThridDeptId(thridDeptId, SecurityConstants.INNER);
            if(!CollectionUtils.isEmpty(deptList)){
                // 将组织机构数据汇总
                for (SysDept sysDept : deptList) {
                    if(!sysDept.getThridOrgType().equals("BM")){
                        Set<String> idSet = deptList.parallelStream()
                                .filter(i -> null != i.getAncestors() && i.getAncestors().contains(sysDept.getAncestors() + "," + sysDept.getDeptId()))
                                .map(SysDept::getThridDeptId)
                                .collect(Collectors.toSet());
                        List<VBidCountVo> list = bidCountList.parallelStream().filter(i->idSet.contains(i.getProjectDepartmentId())
                                || i.getProjectDepartmentId().equals(sysDept.getThridDeptId()) ).collect(Collectors.toList());
                        if(!CollectionUtils.isEmpty(list)){
                            VBidCountVo vo = this.getBidCount(sysDept, list);
                            resultList.add(vo);
                            if (sysDept.getThridOrgType().equals("X")) {
                                resultList.addAll(this.getProject(list, sysDept));
                            }
                        }
                    }
                }
            }
        }
        // 项目端
        if(null != vBidCountVo.getMinAccountCode() && !CollectionUtils.isEmpty(bidCountList)){
            // 获取组织结构(本级及以下)
            List<SysDept> deptList = remoteSystemService.getDeptByThridDeptId(bidCountList.get(0).getProjectDepartmentId(), SecurityConstants.INNER);
            if(!CollectionUtils.isEmpty(deptList)){
                VBidCountVo vo = this.getBidCount(deptList.get(0), bidCountList);
                resultList.add(vo);
                resultList.addAll(this.getProject(bidCountList, deptList.get(0)));
            }
        }
        return resultList;
    }

    /**
     * 处理字典
     * @param bidCountList
     * @return
     */
    private List<VBidCountVo> handleDict(List<VBidCountVo> bidCountList) {
        Map<String, String> projectTypeList = sysDictDataService.listDictMap(DictBizEnum.UNDERLING_PROJECT_FORMAT.getName());
        bidCountList.parallelStream().forEach(i -> {
            //项目业态
            i.setPrjStateName(StringUtils.isNotEmpty(i.getPrjState())?projectTypeList.get(i.getPrjState()):null);
        });
        return bidCountList;
    }

    /**
     * 获取组织及以下所有项目编码
     * @param thridDeptId
     * @return
     */
    @Override
    public List<String> getBidCountProjectCode(String thridDeptId) {
        List<String> list = new ArrayList<>();
        List<VBidCountVo> bidCountList = baseMapper.select(new VBidCountVo());
        if(!CollectionUtils.isEmpty(bidCountList)){
            // 获取组织结构(本级及以下)
            List<SysDept> deptList = remoteSystemService.getDeptByThridDeptId(thridDeptId, SecurityConstants.INNER);
            if(!CollectionUtils.isEmpty(deptList)) {
                SysDept sysDept = deptList.get(0);
                if (sysDept.getThridOrgType().equals("X")) {
                    list = bidCountList.parallelStream().filter(i -> i.getProjectDepartmentId().equals(sysDept.getThridDeptId()))
                            .map(VBidCountVo::getMinAccountCode)
                            .collect(Collectors.toList());

                } else {
                    Set<String> ids = deptList.parallelStream().filter(i -> null != i.getAncestors()
                                    && i.getAncestors().contains(sysDept.getAncestors() + "," + sysDept.getDeptId()))
                            .map(SysDept::getThridDeptId)
                            .collect(Collectors.toSet());
                    list = bidCountList.parallelStream().filter(i -> ids.contains(i.getProjectDepartmentId())
                            || i.getProjectDepartmentId().equals(sysDept.getThridDeptId()))
                            .map(VBidCountVo::getMinAccountCode)
                            .collect(Collectors.toList());
                }
            }
        }
        return list;
    }

    @Override
    public String getExportTitle(VBidCountVo vBidCountVo) {
        String title = "";
        // 集团或公司端
        if(null != vBidCountVo.getId()){
            SysDept dept = remoteSystemService.getByThridDeptId(vBidCountVo.getId(), SecurityConstants.INNER);
            if(null != dept && !StringUtils.isEmpty(dept.getDeptName())){
                title = dept.getDeptName();
            }
        }
        // 项目端
        if(null != vBidCountVo.getMinAccountCode()){
            List<VBidCountVo> bidCountList = baseMapper.select(vBidCountVo);
            if(!CollectionUtils.isEmpty(bidCountList)){
                title = bidCountList.get(0).getMinAccountFullName();
            }
        }
        return title;
    }

    /**
     * 获取招标率报表（左树右表）
     * @param vBidCountVo
     * @return
     */
    @Override
    public List<VBidCountVo> getBidCountReport(VBidCountVo vBidCountVo) {
        List<VBidCountVo> resultList = new ArrayList<>();
        // 集团或公司端
        if(null != vBidCountVo.getId()){
            // 获取组织结构(本级及以下)
            if(null != vBidCountVo.getDeptId()){
                vBidCountVo.setId(vBidCountVo.getDeptId());
            }
            List<VBidCountVo> bidCountList = baseMapper.select(vBidCountVo);
            if(!CollectionUtils.isEmpty(bidCountList)){
                bidCountList = this.handleDict(bidCountList);
                // 按部门编号分组
                resultList = bidCountList.stream()
                        .collect(Collectors.groupingBy(VBidCountVo::getProjectDepartmentId))
                        .entrySet()
                        .stream()
                        .map(entry -> {
                            List<VBidCountVo> projectList = entry.getValue();
                            VBidCountVo vo = new VBidCountVo();
                            vo = this.getCountData(vo,projectList);
                            vo.setId(entry.getKey());
                            // 获取 projectList 中第一个对象的 projectDepartmentName
                            if (!projectList.isEmpty()) {
                                vo.setDeptName(projectList.get(0).getProjectDepartmentName());
                            }
                            vo.setType("G");
                            vo.setChildren(projectList);
                            return vo;
                        }).collect(Collectors.toList());
            }
        } else if(null != vBidCountVo.getMinAccountCode()){         // 项目端
            List<VBidCountVo> bidCountList = baseMapper.select(vBidCountVo);
            if(!CollectionUtils.isEmpty(bidCountList)){
                VBidCountVo vo = new VBidCountVo();
                vo = this.getCountData(vo,bidCountList);
                vo.setId(bidCountList.get(0).getProjectDepartmentId());
                vo.setDeptName(bidCountList.get(0).getProjectDepartmentName());
                vo.setType("X");
                vo.setChildren(bidCountList);
                resultList.add(vo);
            }
        }
        return resultList;
    }

    /**
     * 获取汇总数据
     * @param vo
     * @param projectList
     * @return
     */
    private VBidCountVo getCountData(VBidCountVo vo, List<VBidCountVo> projectList) {
        // 初始化BigDecimal变量
        BigDecimal cgNum = BigDecimal.ZERO;
        BigDecimal gkNum = BigDecimal.ZERO;
        BigDecimal yqNum = BigDecimal.ZERO;
        BigDecimal xjNum = BigDecimal.ZERO;
        BigDecimal dyNum = BigDecimal.ZERO;
        BigDecimal gkTotalNum = BigDecimal.ZERO;
        BigDecimal ngkTotalNum = BigDecimal.ZERO;
        BigDecimal nBidTotalNum = BigDecimal.ZERO;
        // 一次性遍历列表，计算所有需要的值
        for (VBidCountVo item : projectList) {
            cgNum = cgNum.add(item.getCgNum());
            gkNum = gkNum.add(item.getGkNum());
            yqNum = yqNum.add(item.getYqNum());
            xjNum = xjNum.add(item.getXjNum());
            dyNum = dyNum.add(item.getDyNum());
            gkTotalNum = gkTotalNum.add(item.getGkTotalNum());
            ngkTotalNum = ngkTotalNum.add(item.getNgkTotalNum());
            nBidTotalNum = nBidTotalNum.add(item.getNBidTotalNum());
        }
        // 设置计算结果
        vo.setCgNum(cgNum);
        vo.setGkNum(gkNum);
        vo.setYqNum(yqNum);
        vo.setXjNum(xjNum);
        vo.setDyNum(dyNum);
        vo.setGkTotalNum(gkTotalNum);
        vo.setNgkTotalNum(ngkTotalNum);
        vo.setNBidTotalNum(nBidTotalNum);
        // 计算比率
        if (cgNum.compareTo(BigDecimal.ZERO) > 0) {
            vo.setGkRatio(gkNum.divide(cgNum, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
        }
        return vo;
    }

    /**
     * 创建组织树
     * @param resultList
     * @param thridParentId
     * @return
     */
    private List<VBidCountVo> createDeptTree(List<VBidCountVo> resultList, String thridParentId) {
        List<VBidCountVo> childenList = resultList.stream().filter(i -> i.getParentId().equals(thridParentId)).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(childenList)) {
            for (VBidCountVo map : childenList) {
                map.setChildren(createDeptTree(resultList, map.getId()));
            }
        } else {
            List<VBidCountVo> children = resultList.stream().filter(i -> i.getId().equals(thridParentId)).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(children)) {
                List<VBidCountVo> selfChildren = new ArrayList<>(children.get(0).getChildren());
                selfChildren.addAll(childenList);
                return selfChildren;
            }
        }
        return childenList;
    }

    /**
     * 项目信息
     * @param list
     * @param sysDept
     * @return
     */
    private List<VBidCountVo> getProject(List<VBidCountVo> list, SysDept sysDept) {
        list.forEach(i->{
            i.setId(i.getMinAccountCode());
            i.setParentId(sysDept.getThridDeptId());
            i.setType("X");
        });
        return list;
    }

    /**
     * 获取组织统计数据
     * @param sysDept
     * @param list
     * @return
     */
    private VBidCountVo getBidCount(SysDept sysDept, List<VBidCountVo> list) {
        VBidCountVo vo = new VBidCountVo();
        vo.setId(sysDept.getThridDeptId());
        vo.setDeptName(sysDept.getDeptName());
        vo.setParentId(sysDept.getThridParentId());
        vo.setType("G");
        // 初始化BigDecimal变量
        BigDecimal cgNum = BigDecimal.ZERO;
        BigDecimal gkNum = BigDecimal.ZERO;
        BigDecimal yqNum = BigDecimal.ZERO;
        BigDecimal xjNum = BigDecimal.ZERO;
        BigDecimal dyNum = BigDecimal.ZERO;
        BigDecimal gkTotalNum = BigDecimal.ZERO;
        BigDecimal ngkTotalNum = BigDecimal.ZERO;
        BigDecimal nBidTotalNum = BigDecimal.ZERO;
        // 一次性遍历列表，计算所有需要的值
        for (VBidCountVo item : list) {
            cgNum = cgNum.add(item.getCgNum());
            gkNum = gkNum.add(item.getGkNum());
            yqNum = yqNum.add(item.getYqNum());
            xjNum = xjNum.add(item.getXjNum());
            dyNum = dyNum.add(item.getDyNum());
            gkTotalNum = gkTotalNum.add(item.getGkTotalNum());
            ngkTotalNum = ngkTotalNum.add(item.getNgkTotalNum());
            nBidTotalNum = nBidTotalNum.add(item.getNBidTotalNum());
        }
        // 设置计算结果
        vo.setCgNum(cgNum);
        vo.setGkNum(gkNum);
        vo.setYqNum(yqNum);
        vo.setXjNum(xjNum);
        vo.setDyNum(dyNum);
        vo.setGkTotalNum(gkTotalNum);
        vo.setNgkTotalNum(ngkTotalNum);
        vo.setNBidTotalNum(nBidTotalNum);
        // 计算比率
        if (cgNum.compareTo(BigDecimal.ZERO) > 0) {
            vo.setGkRatio(gkNum.divide(cgNum, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP));
        }
        return vo;
    }


}
