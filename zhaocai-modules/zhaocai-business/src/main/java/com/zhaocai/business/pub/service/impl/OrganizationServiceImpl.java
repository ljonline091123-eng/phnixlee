package com.zhaocai.business.pub.service.impl;

import com.zhaocai.business.manager.http.service.UnderlingSystemService;
import com.zhaocai.business.pub.service.IOrganizationService;
import com.zhaocai.business.pub.vo.res.OrganizationVO;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    @Autowired
    private UnderlingSystemService underlingSystemService;

    @Override
    public List<OrganizationVO> getOriganizationTreeList() {
        // 获取所有机构
        List<SysDept> sysDeptList = remoteSystemService.selectDeptList(new SysDept(), SecurityConstants.INNER);

        // 只保留 syncthird 机构
        List<SysDept> filterList = sysDeptList.stream()
                .filter(x -> "syncthird".equals(x.getOrigin()))
                .collect(Collectors.toList());

        return buildOrganizationTree(filterList);
    }

    @Override
    public List<OrganizationVO> getOriganizationTreeUnXList() {
        // 获取所有机构
        List<SysDept> sysDeptList = remoteSystemService.selectDeptList(new SysDept(), SecurityConstants.INNER);

        // 只保留 syncthird 机构
        List<SysDept> filterList = sysDeptList.stream()
                .filter(x -> "syncthird".equals(x.getOrigin()) && !"X".equals(x.getThridOrgType()))
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
     *
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
     *
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
     *
     * @return
     */
    @Override
    public List<OrganizationVO> listOrganizationCalligraphy() {
        List<OrganizationVO> organizationTree = new ArrayList<>();
        List<SysDept> deptList = remoteSystemService.selectDeptList(new SysDept(), SecurityConstants.INNER);
        List<SysDept> collect = deptList.stream().filter(dept -> "0".equals(dept.getParentId() + "")).collect(Collectors.toList());
        String thridDeptId = "1000000000";
        if (collect != null && !collect.isEmpty()) {
            thridDeptId = collect.get(0).getThridDeptId();
        }
        List<OrganizationVO> children = buildOrganizationTreeByOrg(thridDeptId, deptList);
        if (deptList != null && !deptList.isEmpty()) {
            OrganizationVO rootVO = new OrganizationVO();
            rootVO.setOrganizationCode(deptList.get(0).getThridDeptId());
            rootVO.setOrganizationName(deptList.get(0).getDeptName());
            rootVO.setOrganizationId(deptList.get(0).getDeptId());
            rootVO.setChildren(children);
            organizationTree.add(0, rootVO);
        }
        return organizationTree;
    }

    private String getOrgByUserId(String s) {
        String result = null;
        /* 根据组织获取对应的二级单位 */
        String orgTwo = underlingSystemService.getL2OrgByOrgId(s);
        /* 获取三级单位 */
        String orgThree = underlingSystemService.getL3OrgByOrgId(s);
        if (orgThree != null) {
            result = orgThree;
        } else if (orgTwo != null) {
            /* 赋值使用二级单位 */
            result = orgTwo;
        }
        return result;
    }

    /**
     * 构建机构树(不从集团层开始)
     *
     * @param org
     * @param deptList
     * @return
     */
    private List<OrganizationVO> buildOrganizationTreeByOrg(String org, List<SysDept> deptList) {
        // 获取 root 机构
        List<SysDept> rootList = deptList.stream()
                .filter(dept -> org.equals(dept.getThridParentId()))
                .collect(Collectors.toList());

        // 构建一个集合
        Map<String, SysDept> sysDeptMap = deptList.stream()
                .collect(Collectors.toMap(SysDept::getThridDeptId, Function.identity()));

        // 构建机构树
        return rootList.stream()
                .map(dept -> buildOrganizationTree(dept, sysDeptMap))
                .collect(Collectors.toList());
    }

}
