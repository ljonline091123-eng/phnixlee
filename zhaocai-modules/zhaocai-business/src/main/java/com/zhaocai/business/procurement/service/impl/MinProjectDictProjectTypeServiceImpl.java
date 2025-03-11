package com.zhaocai.business.procurement.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.procurement.domain.MinProjectDictProjectType;
import com.zhaocai.business.procurement.mapper.MinProjectDictProjectTypeMapper;
import com.zhaocai.business.procurement.service.IMinProjectDictProjectTypeService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ssy
 * @date 2024/7/13 16:01
 */
@Service
public class MinProjectDictProjectTypeServiceImpl extends ServiceImpl<MinProjectDictProjectTypeMapper, MinProjectDictProjectType> implements IMinProjectDictProjectTypeService {

//    @Override
//    public List<VendorClassifyTreeVO> getVendorClassifyTree() {
//        //查询全部供应商分类数据
//        List<VendorClassifyTreeVO> classifyTreeVOList = this.list(new LambdaQueryWrapper<>()).stream().map(vendorClassifie ->
//                new VendorClassifyTreeVO(
//                        vendorClassifie.getId(),
//                        vendorClassifie.getParentId(),
//                        vendorClassifie.getName(),
//                        vendorClassifie.getCode(),
//                        vendorClassifie.getLevel()))
//                .collect(Collectors.toList());
//        if (!CollectionUtils.isEmpty(classifyTreeVOList)){
//            classifyTreeVOList = buildTreeNode(classifyTreeVOList, 0L);
//        }
//        return classifyTreeVOList;
//    }

    /*
    * 查询工程类型对应的工程类别名
    * */
    @Override
    public String getDictProjectTypeName(String codes) {
        if (StringUtils.isBlank(codes)) {
            return null;
        }

        List<String> codeList = Arrays.asList(codes.split(","));

        // 查询数据库，根据 code 获取对应的 MinProjectDictProjectType 对象
        List<MinProjectDictProjectType> projectTypeList = baseMapper.selectList(
                new QueryWrapper<MinProjectDictProjectType>()
                        .in("code", codeList)
        );

        // 将查询到的 name 收集起来，并用逗号分隔
        return projectTypeList.stream()
                .map(MinProjectDictProjectType::getName)
                .collect(Collectors.joining(","));

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
//    public static List<VendorClassifyTreeVO> buildTreeNode(List<VendorClassifyTreeVO> treeNodeList,Long rootId) {
//        // 获取跟节点
//        List<VendorClassifyTreeVO>  rootList = treeNodeList.stream()
//                .filter(item -> item.getParentId().equals(rootId)).map(item ->
//                        new VendorClassifyTreeVO(
//                        item.getId(), item.getParentId(), item.getName(), item.getCode(), item.getLevel()))
//                .collect(Collectors.toList());
//
//        // 根据 parentId 分组
//        Map<String,List<VendorClassifyTreeVO>> treeNodeMap = treeNodeList.stream()
//                .collect(Collectors.groupingBy(item -> item.getParentId().toString()));
//
//        // 构建树结构
//        recursionFnTree(rootList,treeNodeMap);
//
//        return rootList;
//    }
//
//    private static void recursionFnTree(List<VendorClassifyTreeVO> rootList, Map<String, List<VendorClassifyTreeVO>> treeNodeMap) {
//        List<VendorClassifyTreeVO> childList;
//        for (VendorClassifyTreeVO treeNode : rootList) {
//            childList = treeNodeMap.get(treeNode.getId().toString());
//            if (CollectionUtil.isNotEmpty(childList)) {
//                treeNode.setChildren(childList);
//
//                recursionFnTree(childList, treeNodeMap);
//            }
//        }
//    }



}
