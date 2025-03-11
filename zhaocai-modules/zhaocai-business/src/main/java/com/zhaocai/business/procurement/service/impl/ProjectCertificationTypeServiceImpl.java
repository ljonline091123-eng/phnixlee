package com.zhaocai.business.procurement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.procurement.domain.ProjectCertificationType;
import com.zhaocai.business.procurement.mapper.ProjectCertificationTypeMapper;
import com.zhaocai.business.procurement.service.IProjectCertificationTypeService;
import com.zhaocai.business.procurement.vo.res.ProjectCertificationTypeVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author ssy
 * @date 2024/7/13 16:01
 */
@Service
public class ProjectCertificationTypeServiceImpl extends ServiceImpl<ProjectCertificationTypeMapper, ProjectCertificationType> implements IProjectCertificationTypeService {

    @Override
    public List<ProjectCertificationTypeVO> getProjectCertificationTypeTree() {
        //查询全部资质分类数据
        List<ProjectCertificationTypeVO> CertificationTypeList = this.list(new LambdaQueryWrapper<>()).stream().map(ProjectCertificationType ->
                new ProjectCertificationTypeVO(
                        ProjectCertificationType.getId(),
                        ProjectCertificationType.getParentCode(),
                        ProjectCertificationType.getName(),
                        ProjectCertificationType.getCode(),
                        ProjectCertificationType.getType()))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(CertificationTypeList)){
            CertificationTypeList = buildTreeNode(CertificationTypeList, "-1");
        }
        return CertificationTypeList;
    }

    /*
    * 查询资质类型对应的资质类别名
    * */
    @Override
    public String getProjectCertificationTypeName(String codes) {
        if (StringUtils.isBlank(codes)) {
            return null;
        }

        String[] codeArray = codes.split(",");

        // 检查是否提供了三个 code 值
        if (codeArray.length != 3) {
            throw new IllegalArgumentException("codes 必须包含三个值，分别对应 0、1、2 层级");
        }

        // 查询数据库，获取各层级对应的 ProjectCertificationType
        List<ProjectCertificationType> resultList = new ArrayList<>();
        resultList.add(getProjectCertificationTypeByCodeAndType(codeArray[0], "0")); // 0 层级
        resultList.add(getProjectCertificationTypeByCodeAndType(codeArray[1], "1")); // 1 层级
        resultList.add(getProjectCertificationTypeByCodeAndType(codeArray[2], "2")); // 2 层级

        // 提取各层级的 name，并用逗号分隔
        return resultList.stream()
                .map(ProjectCertificationType::getName)
                .collect(Collectors.joining(","));

    }

    /**
     * 根据 code 和 type 查询 ProjectCertificationType
     *
     * @param code 分类编码
     * @param type 层级
     * @return 查询到的 ProjectCertificationType
     */
    @Override
    public ProjectCertificationType getProjectCertificationTypeByCodeAndType(String code, String type) {
        return baseMapper.selectOne(
                new QueryWrapper<ProjectCertificationType>()
                        .eq("code", code)
                        .eq("type", type)
        );
    }

//    @Override
//    public List<VendorClassify> getVendorClassifySubList(Long id) {
//        List<VendorClassify> classifyList = new ArrayList<>();
//        List<VendorClassify> classifies = this.list(new LambdaQueryWrapper<VendorClassify>()
//                .eq(VendorClassify::getParentId, id));
//        if (!CollectionUtils.isEmpty(classifies)){
//            classifyList.addAll(classifies);
//            for (VendorClassify classify : classifies) {
//                List<VendorClassify> parentList = getVendorClassifySubList(classify.getId());
//                if (!CollectionUtils.isEmpty(parentList)){
//                    classifyList.addAll(parentList);
//                }
//            }
//        }
//        return classifyList;
//    }
//
//
    public static List<ProjectCertificationTypeVO> buildTreeNode(List<ProjectCertificationTypeVO> treeNodeList,String rootCode) {
        // 获取根节点
        List<ProjectCertificationTypeVO>  rootList = treeNodeList.stream()
                .filter(item -> item.getParentCode().equals(rootCode)).map(item ->
                        new ProjectCertificationTypeVO(
                        item.getId(), item.getParentCode(), item.getName(), item.getCode(), item.getType()))
                .collect(Collectors.toList());

        // 按照 type 分组，然后在同一层级中按照 parentCode 分组
        Map<String, Map<String, List<ProjectCertificationTypeVO>>> grouped = groupByTypeAndParentCode(treeNodeList);

        // 构建树结构
        recursionFnTree(rootList,grouped);

        return rootList;
    }

    // 按照 type 分组，然后在同一层级中按照 parentCode 分组
    public static Map<String, Map<String, List<ProjectCertificationTypeVO>>> groupByTypeAndParentCode(
            List<ProjectCertificationTypeVO> treeNodeList) {
        // 按照 type 分组
        Map<String, List<ProjectCertificationTypeVO>> typeGrouped = treeNodeList.stream()
                .collect(Collectors.groupingBy(ProjectCertificationTypeVO::getType));

        // 对于每个 type 层的节点列表，再按照 parentCode 分组
        Map<String, Map<String, List<ProjectCertificationTypeVO>>> result = new HashMap<>();
        typeGrouped.forEach((type, nodeList) -> {
            Map<String, List<ProjectCertificationTypeVO>> parentCodeGrouped = nodeList.stream()
                    .collect(Collectors.groupingBy(ProjectCertificationTypeVO::getParentCode));
            result.put(type, parentCodeGrouped);
        });

        return result;
    }

    // 构建树结构
    private static void recursionFnTree(List<ProjectCertificationTypeVO> rootList, Map<String, Map<String, List<ProjectCertificationTypeVO>>> grouped) {
        for (ProjectCertificationTypeVO treeNode : rootList) {
            // 获取当前节点的 type
            String currentType = treeNode.getType();

            // 获取下一层级的分组（type=1 或 type=2）
            String nextType = getNextType(currentType);
            if (nextType == null) {
                continue; // 当前节点是叶子节点，没有子节点
            }

            // 获取下一层级的分组数据
            Map<String, List<ProjectCertificationTypeVO>> nextLevelGrouped = grouped.get(nextType);
            if (nextLevelGrouped == null) {
                continue; // 没有下一层级节点
            }

            // 获取当前节点的子节点列表（根据 parentCode）
            List<ProjectCertificationTypeVO> childList = nextLevelGrouped.get(treeNode.getCode());
            if (CollectionUtil.isNotEmpty(childList)) {
                // 设置子节点
                treeNode.setChildren(childList);

                // 递归构建子树
                recursionFnTree(childList, grouped);
            }
        }
    }


    // 获取下一层级的 type
    private static String getNextType(String currentType) {
        switch (currentType) {
            case "0":
                return "1"; // type=0 的下一层是 type=1
            case "1":
                return "2"; // type=1 的下一层是 type=2
            default:
                return null; // type=2 是叶子节点，没有下一层
        }
    }



}
