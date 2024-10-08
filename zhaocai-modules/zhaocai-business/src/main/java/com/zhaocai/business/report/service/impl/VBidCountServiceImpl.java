package com.zhaocai.business.report.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.manager.http.service.UnderlingSystemService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VBidCountServiceImpl extends ServiceImpl<VBidCountMapper, VBidCountVo> implements IVBidCountService {

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Autowired
    private UnderlingSystemService underlingSystemService;

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
                    if(sysDept.getThridOrgType().equals("X")){
                        // 项目部汇总
                        List<VBidCountVo> list = bidCountList.stream().filter(i->i.getBelongingOrgId().equals(sysDept.getThridDeptId())).collect(Collectors.toList());
                        if(!CollectionUtils.isEmpty(list)){
                            VBidCountVo vo = this.getBidCount(sysDept, list);
                            vo.setChildren(this.getProject(list, sysDept));
                            resultList.add(vo);
                        }
                    } else {
                        // 公司汇总
                        List<String> ids = deptList.stream().filter(i->null != i.getAncestors()
                                        && i.getAncestors().contains(sysDept.getAncestors() + "," + sysDept.getDeptId()))
                                .map(SysDept::getThridDeptId)
                                .collect(Collectors.toList());
                        List<VBidCountVo> list = bidCountList.stream().filter(i->ids.contains(i.getManagementOrgId())
                                || i.getManagementOrgId().equals(sysDept.getThridDeptId()) ).collect(Collectors.toList());
                        if(!CollectionUtils.isEmpty(list)){
                            VBidCountVo vo = this.getBidCount(sysDept, list);
                            resultList.add(vo);
                        }
                    }
                }
                // 构建组织树
                List<VBidCountVo> tree = resultList.stream().filter(i -> i.getId().equals(vBidCountVo.getId())).collect(Collectors.toList());
                if(!CollectionUtils.isEmpty(tree)){
                    tree.get(0).setChildren(this.createDeptTree(resultList, vBidCountVo.getId()));
                }
                return tree;
            }
        }
        // 项目端
        if(null != vBidCountVo.getMinAccountCode() && !CollectionUtils.isEmpty(bidCountList)){
            // 获取组织结构(本级及以下)
            List<SysDept> deptList = remoteSystemService.getDeptByThridDeptId(bidCountList.get(0).getBelongingOrgId(), SecurityConstants.INNER);
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
                List<String> ids = deptList.stream().filter(i -> null != i.getAncestors()
                                && i.getAncestors().contains(sysDept.getAncestors() + "," +  sysDept.getDeptId()))
                        .map(SysDept::getThridDeptId)
                        .collect(Collectors.toList());
                List<VBidCountVo> list = bidCountList.stream().filter(i -> ids.contains(i.getManagementOrgId())
                        || i.getManagementOrgId().equals(sysDept.getThridDeptId()) ).collect(Collectors.toList());
                if (!CollectionUtils.isEmpty(list)) {
                    VBidCountVo vo = this.getBidCount(sysDept, list);
                    resultList.add(vo);
                }
                // 组织中项目部层
            } else if (!CollectionUtils.isEmpty(deptList) && deptList.get(0).getThridOrgType().equals("X")) {
                List<VBidCountVo> list = bidCountList.stream().filter(i -> i.getBelongingOrgId().equals(vBidCountVo.getId())).collect(Collectors.toList());
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
                    List<String> ids = deptList.stream().filter(i -> null != i.getAncestors()
                                    && i.getAncestors().contains(sysDept.getAncestors() + "," + sysDept.getDeptId()))
                            .map(SysDept::getThridDeptId)
                            .collect(Collectors.toList());
                    List<VBidCountVo> list = bidCountList.stream().filter(i -> ids.contains(i.getManagementOrgId())
                            || i.getManagementOrgId().equals(sysDept.getThridDeptId()) ).collect(Collectors.toList());
                    if (!CollectionUtils.isEmpty(list)) {
                        VBidCountVo vo = this.getBidCount(sysDept, list);
                        resultList.add(vo);
                    }
                }
            }
        }
        if(null != vBidCountVo.getMinAccountCode() && !CollectionUtils.isEmpty(bidCountList)) {
            // 获取组织结构(本级及以下)
            List<SysDept> deptList = remoteSystemService.getDeptByThridDeptId(bidCountList.get(0).getBelongingOrgId(), SecurityConstants.INNER);
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
                    if(sysDept.getThridOrgType().equals("X")){
                        // 项目部汇总
                        List<VBidCountVo> list = bidCountList.stream().filter(i->i.getBelongingOrgId().equals(sysDept.getThridDeptId())).collect(Collectors.toList());
                        if(!CollectionUtils.isEmpty(list)){
                            VBidCountVo vo = this.getBidCount(sysDept, list);
                            resultList.add(vo);
                            resultList.addAll(this.getProject(list, sysDept));
                        }
                    } else {
                        // 公司汇总
                        List<String> ids = deptList.stream().filter(i->null != i.getAncestors()
                                        && i.getAncestors().contains(sysDept.getAncestors() + "," + sysDept.getDeptId()))
                                .map(SysDept::getThridDeptId)
                                .collect(Collectors.toList());
                        List<VBidCountVo> list = bidCountList.stream().filter(i->ids.contains(i.getManagementOrgId())
                                || i.getManagementOrgId().equals(sysDept.getThridDeptId()) ).collect(Collectors.toList());
                        if(!CollectionUtils.isEmpty(list)){
                            VBidCountVo vo = this.getBidCount(sysDept, list);
                            resultList.add(vo);
                        }
                    }
                }
            }
        }
        // 项目端
        if(null != vBidCountVo.getMinAccountCode() && !CollectionUtils.isEmpty(bidCountList)){
            // 获取组织结构(本级及以下)
            List<SysDept> deptList = remoteSystemService.getDeptByThridDeptId(bidCountList.get(0).getBelongingOrgId(), SecurityConstants.INNER);
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
        bidCountList.stream().map(i -> {
            //项目业态
            i.setPrjStateName(StringUtils.isNotEmpty(i.getPrjState())?
                    underlingSystemService.listDictMap(DictBizEnum.UNDERLING_PROJECT_FORMAT.getName()).get(i.getPrjState()):null);
            return i;
        }).collect(Collectors.toList());
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
                    list = bidCountList.stream().filter(i -> i.getBelongingOrgId().equals(sysDept.getThridDeptId()))
                            .map(VBidCountVo::getMinAccountCode)
                            .collect(Collectors.toList());

                } else {
                    List<String> ids = deptList.stream().filter(i -> null != i.getAncestors()
                                    && i.getAncestors().contains(sysDept.getAncestors() + "," + sysDept.getDeptId()))
                            .map(SysDept::getThridDeptId)
                            .collect(Collectors.toList());
                    list = bidCountList.stream().filter(i -> ids.contains(i.getManagementOrgId())
                            || i.getManagementOrgId().equals(sysDept.getThridDeptId()))
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
                return children.get(0).getChildren();
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
        vo.setCgNum(list.stream().map(VBidCountVo::getCgNum).reduce(BigDecimal.ZERO,BigDecimal::add));
        vo.setGkNum(list.stream().map(VBidCountVo::getGkNum).reduce(BigDecimal.ZERO,BigDecimal::add));
        vo.setYqNum(list.stream().map(VBidCountVo::getYqNum).reduce(BigDecimal.ZERO,BigDecimal::add));
        vo.setXjNum(list.stream().map(VBidCountVo::getXjNum).reduce(BigDecimal.ZERO,BigDecimal::add));
        vo.setDyNum(list.stream().map(VBidCountVo::getDyNum).reduce(BigDecimal.ZERO,BigDecimal::add));
        vo.setGkTotalNum(list.stream().map(VBidCountVo::getGkTotalNum).reduce(BigDecimal.ZERO,BigDecimal::add));
        vo.setNgkTotalNum(list.stream().map(VBidCountVo::getNgkTotalNum).reduce(BigDecimal.ZERO,BigDecimal::add));
        vo.setnBidTotalNum(list.stream().map(VBidCountVo::getnBidTotalNum).reduce(BigDecimal.ZERO,BigDecimal::add));
        if(vo.getCgNum().compareTo(BigDecimal.valueOf(0))>0){
            vo.setGkRatio((vo.getGkNum().divide(vo.getCgNum(),4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100)).setScale(2,RoundingMode.HALF_UP)));
        }
        return vo;
    }


}
