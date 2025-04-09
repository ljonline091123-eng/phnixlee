package com.zhaocai.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.constant.UserConstants;
import com.zhaocai.common.core.exception.ServiceException;
import com.zhaocai.common.core.text.Convert;
import com.zhaocai.common.core.utils.SpringUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.datascope.annotation.DataScope;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.OrgInfoQueryDTO;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.domain.SysRole;
import com.zhaocai.system.api.domain.SysUser;
import com.zhaocai.system.domain.vo.TreeSelect;
import com.zhaocai.system.mapper.SysDeptMapper;
import com.zhaocai.system.mapper.SysRoleMapper;
import com.zhaocai.system.service.ISysDeptService;
import com.zhaocai.system.utils.KeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 部门管理 服务实现
 *
 * @author ruoyi
 */
@Service
@Slf4j
public class SysDeptServiceImpl extends ServiceImpl<SysDeptMapper, SysDept> implements ISysDeptService {
    @Autowired
    private SysDeptMapper deptMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    /**
     * 查询部门管理数据
     *
     * @param dept 部门信息
     * @return 部门信息集合
     */
    @Override
    @DataScope(deptAlias = "d")
    public List<SysDept> selectDeptList(SysDept dept) {
        return deptMapper.selectDeptList(dept);
    }

    /**
     * 获取部门层级列表
     *
     * @param deptId
     * @return 部门树信息集合
     */
    @Override
    public List<SysDept> selectChildrenDept(String kyeVal, String group, Long deptId) {
        System.out.println(group);
        List<SysDept> list = deptMapper.selectChildrenById(kyeVal, group, deptId);
        return list;
    }

    /**
     * 查询部门树结构信息
     *
     * @param dept 部门信息
     * @return 部门树信息集合
     */
    @Override
    public List<TreeSelect> selectDeptTreeList(SysDept dept) {
        List<SysDept> depts = SpringUtils.getAopProxy(this).selectDeptList(dept);
        return buildDeptTreeSelect(depts);
    }

    /**
     * 查询所有部门树结构信息（不用数据过滤）
     *
     * @param dept 部门信息
     * @return 部门树信息集合
     */
    @Override
    public List<TreeSelect> selectAllDeptTreeList(SysDept dept) {
        List<SysDept> depts = deptMapper.selectAllDeptList(dept);
        return buildDeptTreeSelect(depts);
    }

    /**
     * 查询部门树结构信息
     *
     * @param thridDeptId 部门信息
     * @return 部门树信息集合
     */
    @Override
    public List<TreeSelect> selectDeptTreeList(String thridDeptId) {
        SysDept dept = findSecondDept(thridDeptId);
        List<SysDept> depts = SpringUtils.getAopProxy(this).selectDeptList(dept);
        return buildDeptTreeSelect(depts);
    }


    @Override
    public List<TreeSelect> selectDeptTreeListCond(SysDept dept) {
        /**
         * 0826 张贵荣王宁确认deptId==1000000000为集团账号，其余为非集团
         * 如果是集团的，则查询所有的组织机构
         * 如果是非集团的人应查询当前登陆人所在的二级及以下的组织，不要把所有的组织全部查询出来
         * */
        List<SysDept> depts;
        if (UserConstants.GROUP_DEPT_ID.equals(dept.getThridDeptId())) {
            depts = SpringUtils.getAopProxy(this).selectDeptList(dept);
        } else {
            OrgInfoQueryDTO queryDTO = new OrgInfoQueryDTO();
            queryDTO.setThridOrgId(dept.getThridDeptId());
            queryDTO.setQueryOrg(NumberConstant.ONE + "");
            depts = this.selectOrgInfoList(queryDTO);
        }
        return buildDeptTreeSelect(depts);
    }

    /**
     * 构建前端所需要树结构
     *
     * @param depts 部门列表
     * @return 树结构列表
     */
    @Override
    public List<SysDept> buildDeptTree(List<SysDept> depts) {
        List<SysDept> returnList = new ArrayList<SysDept>();
        List<Long> tempList = depts.stream().map(SysDept::getDeptId).collect(Collectors.toList());
        for (SysDept dept : depts) {
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(dept.getParentId())) {
                recursionFn(depts, dept);
                returnList.add(dept);
            }
        }
        if (returnList.isEmpty()) {
            returnList = depts;
        }
        return returnList;
    }

    /**
     * 构建前端所需要下拉树结构
     *
     * @param depts 部门列表
     * @return 下拉树结构列表
     */
    @Override
    public List<TreeSelect> buildDeptTreeSelect(List<SysDept> depts) {
        List<SysDept> deptTrees = buildDeptTree(depts);
        return deptTrees.stream().map(TreeSelect::new).collect(Collectors.toList());
    }

    /**
     * 根据角色ID查询部门树信息
     *
     * @param roleId 角色ID
     * @return 选中部门列表
     */
    @Override
    public List<Long> selectDeptListByRoleId(Long roleId) {
        SysRole role = roleMapper.selectRoleById(roleId);
        return deptMapper.selectDeptListByRoleId(roleId, role.isDeptCheckStrictly());
    }

    /**
     * 根据部门ID查询信息
     *
     * @param deptId 部门ID
     * @return 部门信息
     */
    @Override
    public SysDept selectDeptById(Long deptId) {
        return deptMapper.selectDeptById(deptId);
    }

    /**
     * 根据ID查询所有子部门（正常状态）
     *
     * @param deptId 部门ID
     * @return 子部门数
     */
    @Override
    public int selectNormalChildrenDeptById(Long deptId) {
        return deptMapper.selectNormalChildrenDeptById(deptId);
    }

    /**
     * 是否存在子节点
     *
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    public boolean hasChildByDeptId(Long deptId) {
        int result = deptMapper.hasChildByDeptId(deptId);
        return result > 0;
    }

    /**
     * 查询部门是否存在用户
     *
     * @param deptId 部门ID
     * @return 结果 true 存在 false 不存在
     */
    @Override
    public boolean checkDeptExistUser(Long deptId) {
        int result = deptMapper.checkDeptExistUser(deptId);
        return result > 0;
    }

    /**
     * 校验部门名称是否唯一
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public boolean checkDeptNameUnique(SysDept dept) {
        Long deptId = StringUtils.isNull(dept.getDeptId()) ? -1L : dept.getDeptId();
        SysDept info = deptMapper.checkDeptNameUnique(dept.getDeptName(), dept.getParentId());
        if (StringUtils.isNotNull(info) && info.getDeptId().longValue() != deptId.longValue()) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 校验部门是否有数据权限
     *
     * @param deptId 部门id
     */
    @Override
    public void checkDeptDataScope(Long deptId) {
        if (!SysUser.isAdmin(SecurityUtils.getUserId())) {
            SysDept dept = new SysDept();
            dept.setDeptId(deptId);
            List<SysDept> depts = SpringUtils.getAopProxy(this).selectDeptList(dept);
            if (StringUtils.isEmpty(depts)) {
                throw new ServiceException("没有权限访问部门数据！");
            }
        }
    }

    /**
     * 新增保存部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public int insertDept(SysDept dept) {
        SysDept info = deptMapper.selectDeptById(dept.getParentId());
        // 如果父节点不为正常状态,则不允许新增子节点
        if (!UserConstants.DEPT_NORMAL.equals(info.getStatus())) {
            throw new ServiceException("部门停用，不允许新增");
        }
        dept.setAncestors(info.getAncestors() + "," + dept.getParentId());

        dept.setThridDeptId(KeyUtils.generateId() + "");
        dept.setOrigin("syncthird");
        if (info != null) {
            dept.setThridParentId(info.getThridDeptId());
            dept.setThridOrgLevel(info.getThridOrgLevel() + 1);
        } else {
            dept.setThridOrgLevel(1);
        }
        return deptMapper.insertDept(dept);
    }

    @Override
    public boolean insertDepts(List<SysDept> depts, boolean saveFlag) {
        if (depts.size() > 0) {
            if (saveFlag) {
//                return this.saveBatch(depts, depts.size());
                int res = deptMapper.insertBatchSomeColumn(depts);
                return true;
            } else {
                this.updateBatchById(depts, depts.size());
/*                UpdateBatchWrapper wrapper = new UpdateBatchWrapper<SysDept>().setUpdateFields(
                        SysDept::getThridDeptId,
                        SysDept::getAncestors,
                        SysDept::getDeptName,
                        SysDept::getOrderNum,
                        SysDept::getPhone,
                        SysDept::getEmail,
                        SysDept::getLeader,
                        SysDept::getOrigin,
                        SysDept::getStatus,
                        SysDept::getThridParentId,
                        SysDept::getThridOrgType);
                int res = deptMapper.updateBatchColumnById(depts, wrapper);*/
                return true;
            }
        }


        return false;
    }

    @Override
    public Boolean deleteSyncDept() {
        return baseMapper.deleteSyncDept();
    }

    @Override
    public Boolean structDept() {
        log.info("结构化的部门数据。。。。");

        //获取到需要结构化的部门数据
        List<SysDept> depts = baseMapper.findSyncThridDept();
        if (CollectionUtils.isEmpty(depts)) {
            return false;
        }

        String rootId = "0";
        //获取根节点部门
        List<SysDept> rootList = depts.stream()
                .filter(item -> item.getThridParentId().equals(rootId))
                .peek(item2 -> item2.setAncestors("0"))
                .collect(Collectors.toList());

        // 根据 parentId 分组
        Map<String, List<SysDept>> treeNodeMap = depts.stream()
                .collect(Collectors.groupingBy(SysDept::getThridParentId));

        //定义一个全新的部门集合
        List<SysDept> deptNew = new ArrayList<>(rootList);
        // 填充下一个节点数据
        deptNew = fillNodeData(treeNodeMap, rootList, deptNew);
        //更新部门信息表
        this.updateBatchById(deptNew, deptNew.size());
        return true;
    }

    private static List<SysDept> fillNodeData(Map<String, List<SysDept>> treeNodeMap, List<SysDept> rootList, List<SysDept> deptNew) {
        List<SysDept> childList;
        for (SysDept sysDept : rootList) {
            childList = treeNodeMap.get(sysDept.getThridDeptId());
            if (!CollectionUtils.isEmpty(childList)) {
                //如果有子集合，子集合要填充属性
                for (SysDept childDept : childList) {
                    childDept.setParentId(sysDept.getDeptId());
                    childDept.setAncestors(sysDept.getAncestors() + "," + sysDept.getDeptId());
                }
                deptNew.addAll(childList);
                //继续往下面填充子集合
                deptNew = fillNodeData(treeNodeMap, childList, deptNew);
            }
        }
        return deptNew;

    }

    /**
     * 修改保存部门信息
     *
     * @param dept 部门信息
     * @return 结果
     */
    @Override
    public int updateDept(SysDept dept) {
        SysDept newParentDept = deptMapper.selectDeptById(dept.getParentId());
        SysDept oldDept = deptMapper.selectDeptById(dept.getDeptId());
        if (StringUtils.isNotNull(newParentDept) && StringUtils.isNotNull(oldDept)) {
            String newAncestors = newParentDept.getAncestors() + "," + newParentDept.getDeptId();
            String oldAncestors = oldDept.getAncestors();
            dept.setAncestors(newAncestors);
            updateDeptChildren(dept.getDeptId(), newAncestors, oldAncestors);
        }
        if (newParentDept != null) {
            dept.setThridParentId(newParentDept.getThridDeptId());
            dept.setThridOrgLevel(newParentDept.getThridOrgLevel() + 1);
        } else {
            dept.setThridOrgLevel(1);
        }
        int result = deptMapper.updateDept(dept);
        if (UserConstants.DEPT_NORMAL.equals(dept.getStatus()) && StringUtils.isNotEmpty(dept.getAncestors())
                && !StringUtils.equals("0", dept.getAncestors())) {
            // 如果该部门是启用状态，则启用该部门的所有上级部门
            updateParentDeptStatusNormal(dept);
        }
        return result;
    }

    /**
     * 修改该部门的父级部门状态
     *
     * @param dept 当前部门
     */
    private void updateParentDeptStatusNormal(SysDept dept) {
        String ancestors = dept.getAncestors();
        Long[] deptIds = Convert.toLongArray(ancestors);
        deptMapper.updateDeptStatusNormal(deptIds);
    }

    /**
     * 修改子元素关系
     *
     * @param deptId       被修改的部门ID
     * @param newAncestors 新的父ID集合
     * @param oldAncestors 旧的父ID集合
     */
    public void updateDeptChildren(Long deptId, String newAncestors, String oldAncestors) {
        List<SysDept> children = deptMapper.selectChildrenDeptById(deptId);
        for (SysDept child : children) {
            child.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
        }
        if (children.size() > 0) {
            deptMapper.updateDeptChildren(children);
        }
    }

    /**
     * 删除部门管理信息
     *
     * @param deptId 部门ID
     * @return 结果
     */
    @Override
    public int deleteDeptById(Long deptId) {
        return deptMapper.deleteDeptById(deptId);
    }

    @Override
    public SysDept getByThridDeptId(String thridDeptId) {
        return deptMapper.findThridDeptCond(thridDeptId);
    }

    @Override
    public List<SysDept> getListByThridDeptId(List<String> thridDeptIds) {
        return deptMapper.findListThridDeptCond(thridDeptIds);
    }

    @Override
    public List<SysDept> getBySwitchListThridDeptId(String thridDeptId) {
        return deptMapper.getBySwitchListThridDeptId(thridDeptId);
    }


    @Override
    public SysDept findSecondDept(String thridDeptId) {
        return deptMapper.findSecondDept(thridDeptId);
    }


    @Override
    public List<SysDept> selectOrgInfoList(OrgInfoQueryDTO queryDTO) {
        String thridOrgId = queryDTO.getThridOrgId();
        String queryOrg = queryDTO.getQueryOrg();
        List<SysDept> sysDepts = new ArrayList<>();
        if (StringUtils.isNotEmpty(thridOrgId)) {
            SysDept sysDept = deptMapper.selectOne(new LambdaQueryWrapper<SysDept>()
                    .eq(SysDept::getThridDeptId, thridOrgId));
            if (!ObjectUtils.isEmpty(sysDept)) {
                String ancestors = sysDept.getAncestors() + "," + sysDept.getDeptId();
                sysDepts = deptMapper.findOrgInfoList(ancestors, queryOrg);
                sysDepts.add(0, sysDept);
            }
        }
        return sysDepts;
    }

    /**
     * 递归列表
     */
    private void recursionFn(List<SysDept> list, SysDept t) {
        // 得到子节点列表
        List<SysDept> childList = getChildList(list, t);
        t.setChildren(childList);
        for (SysDept tChild : childList) {
            if (hasChild(list, tChild)) {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<SysDept> getChildList(List<SysDept> list, SysDept t) {
        List<SysDept> tlist = new ArrayList<SysDept>();
        Iterator<SysDept> it = list.iterator();
        while (it.hasNext()) {
            SysDept n = (SysDept) it.next();
            if (StringUtils.isNotNull(n.getParentId()) && n.getParentId().longValue() == t.getDeptId().longValue()) {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<SysDept> list, SysDept t) {
        return getChildList(list, t).size() > 0 ? true : false;
    }

    @Override
    public SysDept getTwoLevelDeptByDeptId(Long deptId) {
        return deptMapper.getTwoLevelDeptByDeptId(deptId);
    }

    @Override
    public SysDept getDeptByDeptId(Long deptId) {
        return deptMapper.getDeptByDeptId(deptId);
    }

    @Override
    public List<SysDept> getTwoLevelDepts() {
        return deptMapper.getTwoLevelDepts();
    }

    @Override
    public List<SysDept> getThreeLevelDepts() {
        return deptMapper.getThreeLevelDepts();
    }

    @Override
    public List<SysDept> getDeptByThridDeptId(String thridDeptId) {
        List<SysDept> sysDepts = new ArrayList<>();
        SysDept sysDept = deptMapper.selectOne(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getThridDeptId, thridDeptId));
        if (!ObjectUtils.isEmpty(sysDept)) {
            String ancestors = sysDept.getAncestors() + "," + sysDept.getDeptId();
            sysDepts = super.list(new LambdaQueryWrapper<SysDept>()
                    .likeRight(SysDept::getAncestors, ancestors));
            sysDepts.add(0, sysDept);
        }
        return sysDepts;
    }

    @Override
    public List<TreeSelect> getDeptTreeByThridDeptId(String thridDeptId) {
        List<SysDept> sysDepts = new ArrayList<>();
        SysDept sysDept = deptMapper.selectOne(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getThridDeptId, thridDeptId));
        if (!ObjectUtils.isEmpty(sysDept)) {
            String ancestors = sysDept.getAncestors() + "," + sysDept.getDeptId();
            sysDepts = super.list(new LambdaQueryWrapper<SysDept>()
                    .likeRight(SysDept::getAncestors, ancestors));
            sysDepts.add(0, sysDept);
        }
        return buildDeptTreeSelect(sysDepts);
    }

    /**
     * 根据第三方部门 id 获取组织机构信息(本部门及以下部门，不含项目部、部门)
     *
     * @param thridDeptId
     * @return
     */
    @Override
    public List<SysDept> getDeptByThridDeptIdNoBM(String thridDeptId) {
        List<SysDept> sysDepts = new ArrayList<>();
        SysDept sysDept = deptMapper.selectOne(new LambdaQueryWrapper<SysDept>()
                .eq(SysDept::getThridDeptId, thridDeptId));
        if (!ObjectUtils.isEmpty(sysDept)) {
            String ancestors = sysDept.getAncestors() + "," + sysDept.getDeptId();
            List<String> thridOrgTypes = Arrays.asList("X", "BM");
            sysDepts = super.list(new LambdaQueryWrapper<SysDept>()
                    .likeRight(SysDept::getAncestors, ancestors)
                    .notIn(SysDept::getThridOrgType, thridOrgTypes));
            sysDepts.add(0, sysDept);
        }
        return sysDepts;
    }

    @Override
    public List<TreeSelect> getDeptTree() {
        SysUser sysUser = SecurityUtils.getSysUser();
        if (sysUser != null) {
            SysDept dept1 = sysUser.getDept();
            SysDept dept = new SysDept();
            dept.setThridOrgLevel(1);
            dept.setDelFlag("0");
            List<SysDept> sysDepts = this.selectDeptList(dept);
            dept.setThridOrgLevel(2);
            sysDepts.addAll(this.selectDeptList(dept));
            Map<Long, SysDept> map1 = new HashMap<>();
            sysDepts.forEach(item -> {
                map1.put(item.getDeptId(), item);
            });
            String[] split = dept1.getAncestors().split(",");
            List<SysDept> listDept = new ArrayList<>();
            if (split.length == 1) {
                listDept.addAll(sysDepts);
            } else {
                for (String s : split) {
                    if (map1.get(Long.parseLong(s)) != null) {
                        listDept.add(map1.get(Long.parseLong(s)));
                    }
                }
                if (map1.get(dept1.getDeptId()) != null) {
                    listDept.add(map1.get(dept1.getDeptId()));
                }
            }
            Map<Long, TreeSelect> map = new HashMap<>();
            listDept.forEach(item -> {
                TreeSelect treeSelect = new TreeSelect();
                treeSelect.setId(item.getDeptId());
                treeSelect.setLabel(item.getSimpleName());
                treeSelect.setParentId(item.getParentId());
                treeSelect.setThridDeptId(item.getThridDeptId());
                treeSelect.setChildren(new ArrayList<>());
                map.put(item.getDeptId(), treeSelect);
            });

            // 构建树形结构
            List<TreeSelect> list = new ArrayList<>();
            for (SysDept type : listDept) {
                TreeSelect treeVo = map.get(type.getDeptId());
                if (type.getParentId() == null || "0".equals(type.getParentId() + "")) {
                    // 根节点，直接添加
                    list.add(treeVo);
                } else {
                    // 非根节点，找到父节点并添加到其子节点列表中
                    TreeSelect treeSelect = map.get(type.getParentId());
                    if (treeSelect != null) {
                        treeSelect.getChildren().add(treeVo);
                    }
                }
            }
            return list;
        }
        return new ArrayList<>();
    }


}
