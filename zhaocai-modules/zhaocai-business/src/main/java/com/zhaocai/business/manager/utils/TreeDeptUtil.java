package com.zhaocai.business.manager.utils;


import cn.hutool.core.collection.CollectionUtil;
import com.zhaocai.business.manager.http.dto.PlatDept;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 生成树节点
 *
 * @author ssy
 * @date 2024/07/09
 */
public class TreeDeptUtil {

	public static List<PlatDept> buildTreeNode(List<PlatDept> treeNodeList, String rootId) {
		// 获取跟节点
		List<PlatDept>  rootList = treeNodeList.stream()
			.filter(item -> item.getParentId().equals(rootId))
			.collect(Collectors.toList());

		for (PlatDept platDept : treeNodeList) {
			if (platDept.getParentId().equals(rootId)){
				PlatDept platDept1 = new PlatDept();
				platDept1 = platDept;
			}
		}

		// 根据 parentId 分组
		Map<String, List<PlatDept>> treeNodeMap = treeNodeList.stream()
			.collect(Collectors.groupingBy(PlatDept::getParentId));

		// 构建树结构
		recursionFnTree(rootList, treeNodeMap);

		return rootList;
	}

	/**
	 * 构建树结构
	 * @param rootList
	 * @param treeNodeMap
	 */
	private static void recursionFnTree(List<PlatDept> rootList, Map<String, List<PlatDept>> treeNodeMap) {
		List<PlatDept> childList;
		for (PlatDept treeNode : rootList) {
			childList = treeNodeMap.get(treeNode.getDeptId());
			if (CollectionUtil.isNotEmpty(childList)) {
				treeNode.setChildren(childList);

				recursionFnTree(childList, treeNodeMap);
			}
		}
	}
}
