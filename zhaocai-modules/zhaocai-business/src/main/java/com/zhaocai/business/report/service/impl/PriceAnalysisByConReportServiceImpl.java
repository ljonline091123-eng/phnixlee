package com.zhaocai.business.report.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.report.mapper.PriceAnalysisByConReportMapper;
import com.zhaocai.business.report.service.IPriceAnalysisByConReportService;
import com.zhaocai.business.report.vo.PriceAnalysisByConReportVo;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
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
public class PriceAnalysisByConReportServiceImpl extends ServiceImpl<PriceAnalysisByConReportMapper, PriceAnalysisByConReportVo> implements IPriceAnalysisByConReportService {

    @Autowired
    private RemoteSystemService remoteSystemService;

    /**
     * 价格分析报表
     * @param priceAnalysis
     * @return
     */
    @Override
    public List<PriceAnalysisByConReportVo> priceAnalysisReport(PriceAnalysisByConReportVo priceAnalysis) {
        List<PriceAnalysisByConReportVo> resultList = new ArrayList<>();
        // 1、判断是项目端还是集团端公司端 2、公司端/集团端查询对应公司及下级公司所有的项目对应的合同清单，项目端查询项目下所有的合同清单 3、根据成本科目、组织（公司端/集团端构建组织树）、项目统计数据
        // 公司端/集团端
        if (null != priceAnalysis.getId()) {
            // 2、公司端/集团端查询对应公司及下级公司所有的项目对应的合同清单
            // 获取组织结构(本级及以下)
            if (null != priceAnalysis.getDeptId()) {
                priceAnalysis.setId(priceAnalysis.getDeptId());
            }
            // 查询公司及下级公司
            List<SysDept> deptList = remoteSystemService.getDeptByThridDeptId(priceAnalysis.getId(), SecurityConstants.INNER);
            if (!CollectionUtils.isEmpty(deptList)) {
                // 查询公司及下级公司所有的项目
                priceAnalysis.setDeptIds(deptList.stream().map(SysDept::getThridDeptId).collect(Collectors.toList()));
                // 查询公司及下级公司所有的项目对应的合同清单
                List<PriceAnalysisByConReportVo> list = this.getContractList(priceAnalysis);
                if (!CollectionUtils.isEmpty(list)) {
                    // 根据物料/清单进行汇总
                    List<PriceAnalysisByConReportVo> result = list.stream()
                            .collect(Collectors.groupingBy(PriceAnalysisByConReportVo::getSubjectDtlCode))
                            .entrySet()
                            .stream()
                            .map(entry -> {
                                List<PriceAnalysisByConReportVo> subjectList = entry.getValue();
                                // 创建物料/清单级别汇总对象
                                PriceAnalysisByConReportVo vo = new PriceAnalysisByConReportVo();
                                vo = this.getSummaryPrice(vo,subjectList);
                                vo = this.addSubjectInfo(vo,subjectList.get(0));
                                vo.setChildren(this.createDeptLevel(subjectList, deptList, vo, priceAnalysis));
                                return vo;
                            }).collect(Collectors.toList());
                    resultList.addAll(result);
                }
            }
        } else if (null != priceAnalysis.getMinAccountCode()) { // 项目端
            // 查询项目对应的合同清单
            List<PriceAnalysisByConReportVo> list = this.getContractList(priceAnalysis);
            if (!CollectionUtils.isEmpty(list)) {
                // 查询项目对应的组织机构
                List<SysDept> deptList = remoteSystemService.getDeptByThridDeptId(list.get(0).getBelongingOrgId(), SecurityConstants.INNER);
                if(!CollectionUtils.isEmpty(deptList)){
                    // 根据物料/清单进行汇总
                    List<PriceAnalysisByConReportVo> result = list.parallelStream()
                            .collect(Collectors.groupingBy(PriceAnalysisByConReportVo::getSubjectDtlCode))
                            .entrySet()
                            .stream()
                            .map(entry -> {
                                List<PriceAnalysisByConReportVo> subjectList = entry.getValue();
                                // 创建物料/清单级别汇总对象
                                PriceAnalysisByConReportVo vo = new PriceAnalysisByConReportVo();
                                vo = this.getSummaryPrice(vo,subjectList);
                                vo = this.addSubjectInfo(vo,subjectList.get(0));
                                vo.setChildren(this.createDeptLevelByproject(subjectList, vo, priceAnalysis, deptList.get(0)));
                                return vo;
                            }).collect(Collectors.toList());
                    resultList.addAll(result);
                }
            }
        }
        return resultList;
    }

    /**
     * 价格分析报表导出
     * @param priceAnalysisByConReportVo
     * @return
     */
    @Override
    public List<PriceAnalysisByConReportVo> priceAnalysisExport(PriceAnalysisByConReportVo priceAnalysisByConReportVo) {
        List<PriceAnalysisByConReportVo> result = new ArrayList<>();
        List<PriceAnalysisByConReportVo> list = this.priceAnalysisReport(priceAnalysisByConReportVo);
        this.flattenData(list.get(0), result);
        return result;
    }

    /**
     * 递归方法，将嵌套的数据扁平化
     *
     * @param node
     * @param result
     */
    private void flattenData(PriceAnalysisByConReportVo node, List<PriceAnalysisByConReportVo> result) {
        result.add(node);
        if (node.getChildren() != null && !node.getChildren().isEmpty()) {
            for (PriceAnalysisByConReportVo child : node.getChildren()) {
                flattenData(child, result);
            }
        }
    }

    /**
     * 获取导出表头和忽略导出字段
     * @param priceAnalysis
     * @return
     */
    @Override
    public Map<String, Object> getExportTitle(PriceAnalysisByConReportVo priceAnalysis) {
        Map<String, Object> map = new HashMap<>();
        String title = "";
        List<String> column = new ArrayList<>();

        if (null != priceAnalysis.getConType()) {
            if (priceAnalysis.getConType().equals("A")) { // A-劳务分包
                title = "劳务分包";
                column = Arrays.asList("brand", "rentMode", "rentUnit", "rentTime", "metrologicalRules", "basicJob", "number");
            } else if (priceAnalysis.getConType().equals("B")) { // B-专业分包
                title = "专业分包";
                column = Arrays.asList("brand", "rentMode", "rentUnit", "rentTime", "number");
            } else if (priceAnalysis.getConType().equals("C")) { // C-购买材料
                title = "物资采购";
                column = Arrays.asList("brand", "rentMode", "rentUnit", "rentTime", "metrologicalRules", "basicJob", "number");
            } else if (priceAnalysis.getConType().equals("D")) { // D-租赁材料
                title = "租赁材料";
                column = Arrays.asList("metrologicalRules", "basicJob");
            } else if (priceAnalysis.getConType().equals("G")) { // G-设备租赁（机械）
                title = "租赁设备（机械）";
                column = Arrays.asList("metrologicalRules", "basicJob");
            } else if (priceAnalysis.getConType().equals("Z")) { // Z-其他
                title = "其他";
                column = Arrays.asList("brand", "rentMode", "rentUnit", "rentTime", "metrologicalRules", "basicJob", "number");
            }
        }

        map.put("title", title);
        map.put("column", column);
        return map;
    }

    /**
     * 构建组织层级（项目端）
     * @param subjectList
     * @param parentVo
     * @param priceAnalysis
     * @return
     */
    private List<PriceAnalysisByConReportVo> createDeptLevelByproject(List<PriceAnalysisByConReportVo> subjectList, PriceAnalysisByConReportVo parentVo, PriceAnalysisByConReportVo priceAnalysis,SysDept sysDept) {
        List<PriceAnalysisByConReportVo> result = new ArrayList<>();
        // 创建项目管理部汇总对象
        PriceAnalysisByConReportVo vo = new PriceAnalysisByConReportVo();
        vo = this.getSummaryPrice(vo,subjectList);
        PriceAnalysisByConReportVo projectVo = BeanCopierUtil.copyBean(vo, PriceAnalysisByConReportVo.class);
        vo.setId(sysDept.getThridDeptId());
        vo.setProjectId(parentVo.getId());
        vo.setDeptName(sysDept.getDeptName());
        // 创建项目汇总对象
        projectVo.setId(subjectList.get(0).getMinAccountCode());
        projectVo.setProjectId(vo.getId());
        projectVo.setMinAccountFullName(subjectList.get(0).getMinAccountFullName());
        projectVo.setChildren(this.addContractInfo(subjectList, projectVo));
        List<PriceAnalysisByConReportVo> list = new ArrayList<>();
        list.add(projectVo);
        vo.setChildren(list);
        result.add(vo);
        return result;
    }

    /**
     * 构建组织层级
     * @param subjectList
     * @param deptList
     * @return
     */
    private List<PriceAnalysisByConReportVo> createDeptLevel(List<PriceAnalysisByConReportVo> subjectList, List<SysDept> deptList, PriceAnalysisByConReportVo parentVo,PriceAnalysisByConReportVo priceAnalysis) {
        List<PriceAnalysisByConReportVo> resultList = new ArrayList<>();
        for (SysDept sysDept : deptList) {
            if(!sysDept.getThridOrgType().equals("BM")){
                List<String> ids = deptList.stream().filter(i -> null != i.getAncestors()
                                && i.getAncestors().contains(sysDept.getAncestors() + "," + sysDept.getDeptId()))
                        .map(SysDept::getThridDeptId).collect(Collectors.toList());
                List<PriceAnalysisByConReportVo> list = subjectList.stream().filter(i -> ids.contains(i.getBelongingOrgId())
                        || i.getBelongingOrgId().equals(sysDept.getThridDeptId())).collect(Collectors.toList());
                if(!CollectionUtils.isEmpty(list)){
                    PriceAnalysisByConReportVo vo = new PriceAnalysisByConReportVo();
                    vo = this.getSummaryPrice(vo, list);
                    vo.setId(sysDept.getThridDeptId());
                    vo.setDeptName(sysDept.getDeptName());
                    vo.setParentId(parentVo.getId());
                    List<PriceAnalysisByConReportVo> childrenList = subjectList.stream().filter(i->i.getBelongingOrgId().equals(sysDept.getThridDeptId())).collect(Collectors.toList());
                    if(!CollectionUtils.isEmpty(childrenList)){
                        vo.setChildren(this.createProjectLevel(childrenList, sysDept));
                    }
                    resultList.add(vo);
                }
            }
        }
        // 构建组织树
        List<PriceAnalysisByConReportVo> tree = resultList.stream().filter(i -> i.getId().equals(priceAnalysis.getId())).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(tree)) {
            tree.get(0).setChildren(this.createDeptTree(resultList, priceAnalysis.getId()));
        }
        return tree;
    }

    /**
     * 创建部门树
     * @param resultList
     * @param id
     * @return
     */
    private List<PriceAnalysisByConReportVo> createDeptTree(List<PriceAnalysisByConReportVo> resultList, String id) {
        List<PriceAnalysisByConReportVo> childenList = resultList.stream().filter(i -> i.getParentId().equals(id)).collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(childenList)) {
            for (PriceAnalysisByConReportVo map : childenList) {
                map.setChildren(createDeptTree(resultList, map.getId()));
            }
        } else {
            List<PriceAnalysisByConReportVo> children = resultList.stream().filter(i -> i.getId().equals(id)).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(children)) {
                return children.get(0).getChildren();
            }
        }
        return childenList;
    }

    /**
     * 构建项目及合同信息
     * @param childrenList
     * @param sysDept
     * @return
     */
    private List<PriceAnalysisByConReportVo> createProjectLevel(List<PriceAnalysisByConReportVo> childrenList, SysDept sysDept) {
        // 根据项目进行汇总
        List<PriceAnalysisByConReportVo> result = childrenList.stream()
                .collect(Collectors.groupingBy(PriceAnalysisByConReportVo::getMinAccountCode))
                .entrySet()
                .stream()
                .map(projectEntry -> {
                    String project = projectEntry.getKey();
                    List<PriceAnalysisByConReportVo> projectList = projectEntry.getValue();
                    // 创建项目级别汇总对象
                    PriceAnalysisByConReportVo vo = new PriceAnalysisByConReportVo();
                    vo = this.getSummaryPrice(vo,projectList);
                    vo.setId(project);
                    vo.setProjectId(sysDept.getThridDeptId());
                    vo.setMinAccountFullName(projectList.get(0).getMinAccountFullName());
                    vo.setChildren(this.addContractInfo(projectList, vo));
                    return vo;
                }).collect(Collectors.toList());
        return result;
    }

    /**
     * 补充合同层信息
     * @param projectList
     * @param parentVo
     * @return
     */
    private List<PriceAnalysisByConReportVo> addContractInfo(List<PriceAnalysisByConReportVo> projectList, PriceAnalysisByConReportVo parentVo) {
        List<PriceAnalysisByConReportVo> result = projectList.parallelStream()
                .map(i -> {
                    PriceAnalysisByConReportVo vo = new PriceAnalysisByConReportVo();
                    vo.setId(i.getConCode());
                    vo.setParentId(parentVo.getId());
                    vo.setQuantity(i.getQuantity());
                    vo.setNtaxPrice(i.getNtaxPrice());
                    vo.setLastPrice(i.getLastPrice());
                    vo.setMinPrice(i.getMinPrice());
                    vo.setMaxPrice(i.getMaxPrice());
                    vo.setAvgPrice(i.getAvgPrice());
                    vo.setConName(i.getConName());
                    vo.setSignDate(i.getSignDate());
                    vo.setInitialPrice(i.getInitialPrice());
                    vo.setConType(i.getConType());
                    return vo;
                })
                .collect(Collectors.toList());

        return result;
    }

    /**
     * 补充物资层信息
     * @param vo
     * @param priceAnalysis
     * @return
     */
    private PriceAnalysisByConReportVo addSubjectInfo(PriceAnalysisByConReportVo vo, PriceAnalysisByConReportVo priceAnalysis) {
        vo.setId(priceAnalysis.getUniqueId());
        vo.setSubjectDtlCode(priceAnalysis.getSubjectDtlCode());
        vo.setSubjectDtlName(priceAnalysis.getSubjectDtlName());
        vo.setSpecs(priceAnalysis.getSpecs());
        vo.setMeasureUnit(priceAnalysis.getMeasureUnit());
        vo.setBrand(priceAnalysis.getBrand());
        vo.setRentMode(priceAnalysis.getRentMode());
        vo.setRentUnit(priceAnalysis.getRentUnit());
        vo.setRentTime(priceAnalysis.getRentTime());
        vo.setMetrologicalRules(priceAnalysis.getMetrologicalRules());
        vo.setBasicJob(priceAnalysis.getBasicJob());
        return vo;
    }

    /**
     * 汇总数量、单价、最新单价、最低单价、最高单价、平均单价
     * @param vo
     * @param subjectList
     * @return
     */
    private PriceAnalysisByConReportVo getSummaryPrice(PriceAnalysisByConReportVo vo, List<PriceAnalysisByConReportVo> subjectList) {
        BigDecimal totalQuantity = BigDecimal.ZERO;
        BigDecimal totalNumber = BigDecimal.ZERO;
        BigDecimal totalNtaxPrice = BigDecimal.ZERO;
        BigDecimal minPrice = null;
        BigDecimal maxPrice = null;
        Date lastCreateTime = null;
        BigDecimal lastPrice = null;
        BigDecimal total = BigDecimal.ZERO;

        for (PriceAnalysisByConReportVo item : subjectList) {
            if (item.getQuantity() != null) {
                totalQuantity = totalQuantity.add(item.getQuantity());
            }
            if (item.getNumber() != null) {
                totalNumber = totalNumber.add(item.getNumber());
            }
            if (item.getNtaxPrice() != null) {
                totalNtaxPrice = totalNtaxPrice.add(item.getNtaxPrice());
            }
            if (item.getMinPrice() != null) {
                if (minPrice == null || item.getMinPrice().compareTo(minPrice) < 0) {
                    minPrice = item.getMinPrice();
                }
                total = total.add(item.getMinPrice());
            }
            if (item.getMaxPrice() != null) {
                if (maxPrice == null || item.getMaxPrice().compareTo(maxPrice) > 0) {
                    maxPrice = item.getMaxPrice();
                }
                total = total.add(item.getMinPrice());
            }
            if (item.getCreateTime() != null) {
                if (lastCreateTime == null || item.getCreateTime().after(lastCreateTime)) {
                    lastCreateTime = item.getCreateTime();
                    lastPrice = item.getLastPrice();
                }
            }
        }

        vo.setQuantity(totalQuantity);
        vo.setNumber(totalNumber);
        vo.setNtaxPrice(totalNtaxPrice);
        vo.setLastPrice(lastPrice);
        vo.setMinPrice(minPrice);
        vo.setMaxPrice(maxPrice);
        vo.setAvgPrice(total.divide(BigDecimal.valueOf(subjectList.size())).setScale(2, RoundingMode.HALF_UP));

        return vo;
    }

    /**
     * 获取项目对应的合同清单
     * @param priceAnalysis
     * @return
     */
    private List<PriceAnalysisByConReportVo> getContractList(PriceAnalysisByConReportVo priceAnalysis) {
        List<PriceAnalysisByConReportVo> result = new ArrayList<>();
        if(null != priceAnalysis.getConType()){
            if (priceAnalysis.getConType().equals("A")) { // A-劳务分包
                result = baseMapper.getVPriceAnalysisLabor(priceAnalysis);
            } else if (priceAnalysis.getConType().equals("B")) { // B-专业分包
                result = baseMapper.getVPriceAnalysisSpecialty(priceAnalysis);
            } else if (priceAnalysis.getConType().equals("C")) { // C-购买材料
                result = baseMapper.getVPriceAnalysisMaterials(priceAnalysis);
            } else if (priceAnalysis.getConType().equals("D")) { // D-租赁材料
                result = baseMapper.getVPriceAnalysisLeasedMaterials(priceAnalysis);
            } else if (priceAnalysis.getConType().equals("G")) { // G-设备租赁（机械）
                result = baseMapper.getVPriceAnalysisLeasedDevice(priceAnalysis);
            } else if (priceAnalysis.getConType().equals("Z")) { // Z-其他
                result = baseMapper.getVPriceAnalysisOther(priceAnalysis);
            }
        }
        return result;
    }
}
