package com.zhaocai.business.pub.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import com.zhaocai.business.common.exception.BusinessException;
import com.zhaocai.business.pub.service.IOrganizationService;
import com.zhaocai.business.pub.vo.res.OrganizationVO;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.constant.UserConstants;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 组织机构实现类
 *
 * @author chenming
 * @date 2024-07-17
 */
@Service
public class OrganizationServiceImpl implements IOrganizationService {

    @Autowired
    private RemoteSystemService remoteSystemService;

    @Override
    public List<OrganizationVO> getOriganizationTreeList() {
        // 获取所有机构
        List<SysDept> sysDeptList = remoteSystemService.selectDeptList(new SysDept(), SecurityConstants.INNER);

        // 只保留 syncthird 机构
        List<SysDept> filterList = sysDeptList.stream()
                .filter(x ->"syncthird".equals(x.getOrigin()))
                .collect(Collectors.toList());

        return buildOrganizationTree(filterList);
    }

    @Override
    public List<OrganizationVO> getOriganizationTreeUnXList() {
        // 获取所有机构
        List<SysDept> sysDeptList = remoteSystemService.selectDeptList(new SysDept(), SecurityConstants.INNER);

        // 只保留 syncthird 机构
        List<SysDept> filterList = sysDeptList.stream()
                .filter(x ->"syncthird".equals(x.getOrigin()) && !"X".equals(x.getThridOrgType()))
                .collect(Collectors.toList());

        return buildOrganizationTree(filterList);
    }

    @Override
    public List<OrganizationVO> listOrganization4Company() {
//        // 获取所有机构
//        List<SysDept> sysDeptList = remoteSystemService.selectDeptList(new SysDept(), SecurityConstants.INNER);
//
//        // 只保留公司
//        List<SysDept> filterList = sysDeptList.stream()
//                .filter(x -> "syncthird".equals(x.getOrigin()) && !"X".equals(x.getThridOrgType()) && !"BM".equals(x.getThridOrgType()))
//                .collect(Collectors.toList());
//        List<SysDept> sysDeptList = remoteSystemService.getTwoLevelDepts(SecurityConstants.INNER);
        List<SysDept> sysDeptList = remoteSystemService.getThreeLevelDepts(SecurityConstants.INNER);
        return buildOrganizationTree(sysDeptList);
    }

    /**
     * 构建机构树
     * @param rootDept
     * @param sysDeptMap
     * @return
     */
    private OrganizationVO buildOrganizationTree(SysDept rootDept, Map<String, SysDept> sysDeptMap) {
        OrganizationVO rootVO = new OrganizationVO();
        rootVO.setOrganizationCode(rootDept.getThridDeptId());
        rootVO.setOrganizationName(rootDept.getDeptName());
        rootVO.setOrganizationId(rootDept.getDeptId());
        rootVO.setChildren(new ArrayList<>());

        List<OrganizationVO> children = sysDeptMap.values().stream()
                .filter(dept -> rootDept.getThridDeptId().equals(dept.getThridParentId()))
                .map(dept -> buildOrganizationTree(dept, sysDeptMap))
                .collect(Collectors.toList());

        rootVO.setChildren(children);
        return rootVO;
    }

    @Override
    public List<OrganizationVO> listOrganizationGroupOrUnit() {
        // 获取所有机构
        List<SysDept> sysDeptList = remoteSystemService.selectDeptList(new SysDept(), SecurityConstants.INNER);

        // 只保留公司
        List<SysDept> filterList = sysDeptList.stream()
                .filter(x -> "syncthird".equals(x.getOrigin()) && !"X".equals(x.getThridOrgType()) && !"BM".equals(x.getThridOrgType()))
                .collect(Collectors.toList());

        return buildOrganizationTree(filterList);
    }


    /**
     * 构建机构树
     * @param filterList
     * @return
     */
    private List<OrganizationVO> buildOrganizationTree(List<SysDept> filterList) {
        // 获取 root 机构
        List<SysDept> rootList = filterList.stream()
                .filter(dept -> "0".equals(dept.getThridParentId()))
                .collect(Collectors.toList());

        // 构建一个集合
        Map<String, SysDept> sysDeptMap = filterList.stream()
                .collect(Collectors.toMap(SysDept::getThridDeptId, Function.identity()));

        // 构建机构树
        return rootList.stream()
                .map(dept -> buildOrganizationTree(dept, sysDeptMap))
                .collect(Collectors.toList());
    }

    /**
     * 范本选择获取公司
     * @return
     */
    @Override
    public List<OrganizationVO> listOrganizationCalligraphy() {
        // 获取当前用户的二级组织或集团
        String currUserTowLevelThridDeptId =  remoteSystemService.getTwoLevelDeptByDeptId
                (SecurityUtils.getSysUser().getDeptId(),SecurityConstants.INNER).getThridDeptId();
        // 获取所有二级组织及集团
        List<SysDept> sysDeptList = remoteSystemService.getTwoLevelDepts(SecurityConstants.INNER);
        List<OrganizationVO>  organizationTree = null;
        if (!currUserTowLevelThridDeptId.equals(UserConstants.GROUP_DEPT_ID)) {
            sysDeptList = sysDeptList.stream().filter(item -> item.getThridOrgLevel() == NumberConstant.ONE ||
                    item.getThridDeptId().equals(currUserTowLevelThridDeptId)).collect(Collectors.toList());
        }

        organizationTree =  buildOrganizationTree(sysDeptList);
        if (!currUserTowLevelThridDeptId.equals(UserConstants.GROUP_DEPT_ID)) {
            organizationTree.stream().forEach(item -> {
                item.setOrganizationType("2");
            });
        }

        return organizationTree;
    }

}
