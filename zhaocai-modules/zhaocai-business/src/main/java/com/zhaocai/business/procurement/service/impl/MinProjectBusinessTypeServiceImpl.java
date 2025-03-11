package com.zhaocai.business.procurement.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.procurement.domain.MinProjectBusinessType;
import com.zhaocai.business.procurement.mapper.MinProjectBusinessTypeMapper;
import com.zhaocai.business.procurement.service.IMinProjectBusinessTypeService;
import com.zhaocai.business.procurement.vo.res.MinProjectBusinessTypeVO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author ssy
 * @date 2024/7/13 16:01
 */
@Service
public class MinProjectBusinessTypeServiceImpl extends ServiceImpl<MinProjectBusinessTypeMapper, MinProjectBusinessType> implements IMinProjectBusinessTypeService {

    @Override
    public List<MinProjectBusinessTypeVO> getVendorClassifyTree() {
        //查询全部供应商分类数据
        List<MinProjectBusinessTypeVO> BusinessTypeVOList = this.list(new LambdaQueryWrapper<>()).stream().map(MinProjectBusinessType ->
                new MinProjectBusinessTypeVO(
                        MinProjectBusinessType.getId(),
                        MinProjectBusinessType.getParentId(),
                        MinProjectBusinessType.getName(),
                        MinProjectBusinessType.getCode()))
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(BusinessTypeVOList)){
            BusinessTypeVOList = buildTreeNode(BusinessTypeVOList, "-1");
        }
        return BusinessTypeVOList;
    }

    /*
    * 查询工程类型对应的工程类别名
    * */
    @Override
    public String getMinProjectBusinessTypeName(String code) {
        if (StringUtils.isBlank(code)) {
            return null;
        }
        // 存储所有祖先节点的 name 值
        List<String> ancestorNames = new ArrayList<>();

        // 当前节点
        MinProjectBusinessType currentNode = baseMapper.selectOne(
                new QueryWrapper<MinProjectBusinessType>()
                        .eq("code", code));
        if (currentNode == null) {
            return null; // 如果没有找到对应的节点，返回 null
        }

        // 添加当前节点的 name
        ancestorNames.add(currentNode.getName());

        // 递归查询父节点，直到根节点
        while (currentNode.getParentId() != null) {
            currentNode = baseMapper.selectOne(new QueryWrapper<MinProjectBusinessType>()
                            .eq("parent_id", currentNode.getParentId()));
            if (currentNode == null) {
                break; // 如果没有找到父节点，退出循环
            }
            ancestorNames.add(currentNode.getName());
        }

        // 反转列表，从根节点到当前节点
        Collections.reverse(ancestorNames);

        // 用 "/" 拼接所有 name 值
        return String.join("/", ancestorNames);
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
    public static List<MinProjectBusinessTypeVO> buildTreeNode(List<MinProjectBusinessTypeVO> treeNodeList,String rootId) {
        // 获取跟节点
        List<MinProjectBusinessTypeVO>  rootList = treeNodeList.stream()
                .filter(item -> item.getParentId().equals(rootId)).map(item ->
                        new MinProjectBusinessTypeVO(
                        item.getId(), item.getParentId(), item.getName(), item.getCode()))
                .collect(Collectors.toList());

        // 根据 parentId 分组
        Map<String,List<MinProjectBusinessTypeVO>> treeNodeMap = treeNodeList.stream()
                .collect(Collectors.groupingBy(item -> item.getParentId().toString()));

        // 构建树结构
        recursionFnTree(rootList,treeNodeMap);

        return rootList;
    }

    private static void recursionFnTree(List<MinProjectBusinessTypeVO> rootList, Map<String, List<MinProjectBusinessTypeVO>> treeNodeMap) {
        List<MinProjectBusinessTypeVO> childList;
        for (MinProjectBusinessTypeVO treeNode : rootList) {
            childList = treeNodeMap.get(treeNode.getId());
            if (CollectionUtil.isNotEmpty(childList)) {
                treeNode.setChildren(childList);

                recursionFnTree(childList, treeNodeMap);
            }
        }
    }



}
