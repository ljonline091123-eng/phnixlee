package com.zhaocai.system.controller;

import com.zhaocai.common.core.constant.UserConstants;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.common.log.annotation.Log;
import com.zhaocai.common.log.enums.BusinessType;
import com.zhaocai.common.security.annotation.InnerAuth;
import com.zhaocai.common.security.annotation.RequiresPermissions;
import com.zhaocai.common.security.utils.SecurityUtils;
import com.zhaocai.system.api.domain.OrgInfoQueryDTO;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.domain.vo.TreeSelect;
import com.zhaocai.system.service.ISysDeptService;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 部门信息
 *
 * @author ruoyi
 */
@RestController
@RequestMapping("/dept")
public class SysDeptController extends BaseController
{
    @Autowired
    private ISysDeptService deptService;

    /**
     * 获取部门列表
     */
    @RequiresPermissions("system:dept:list")
    @GetMapping("/list")
    public AjaxResult list(SysDept dept)
    {
        List<SysDept> depts = deptService.selectDeptList(dept);
        return success(depts);
    }

    /**
     * 获取部门层级列表
     */
    @InnerAuth
    @GetMapping("/selectChildrenDept")
    public List<SysDept> selectChildrenDept(@RequestParam String kyeVal,@RequestParam String group,@RequestParam Long deptId)
    {
        List<SysDept> depts = deptService.selectChildrenDept(kyeVal,group,deptId);
        return depts;
    }

    @InnerAuth
    @PostMapping("/selectDeptList")
    public List<SysDept> selectDeptList(@RequestBody SysDept dept)
    {
        List<SysDept> depts = deptService.selectDeptList(dept);
        return depts;
    }


    /**
     * 查询部门树结构信息
     */
    @InnerAuth
    @GetMapping("/selectDeptTreeList")
    public List<TreeSelect> selectDeptTreeList(@RequestParam String thridDeptId)
    {
        if (StringUtils.isEmpty(thridDeptId)) {
            throw new RuntimeException("第三方部门 id 不能为空");
        }
        List<TreeSelect> treeList = deptService.selectDeptTreeList(thridDeptId);
        return treeList;
    }


    /**
     * 查询部门列表（排除节点）
     */
    @RequiresPermissions("system:dept:list")
    @GetMapping("/list/exclude/{deptId}")
    public AjaxResult excludeChild(@PathVariable(value = "deptId", required = false) Long deptId)
    {
        List<SysDept> depts = deptService.selectDeptList(new SysDept());
        depts.removeIf(d -> d.getDeptId().intValue() == deptId || ArrayUtils.contains(StringUtils.split(d.getAncestors(), ","), deptId + ""));
        return success(depts);
    }

    /**
     * 根据部门编号获取详细信息
     */
    @RequiresPermissions("system:dept:query")
    @GetMapping(value = "/{deptId}")
    public AjaxResult getInfo(@PathVariable Long deptId)
    {
        deptService.checkDeptDataScope(deptId);
        return success(deptService.selectDeptById(deptId));
    }

    /**
     * 新增部门
     */
    @RequiresPermissions("system:dept:add")
    @Log(title = "部门管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysDept dept)
    {
        if (!deptService.checkDeptNameUnique(dept))
        {
            return error("新增部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        dept.setCreateBy(SecurityUtils.getUsername());
        return toAjax(deptService.insertDept(dept));
    }

    /**
     * 新增多个部门
     */
    @InnerAuth
    @Log(title = "新增多个部门", businessType = BusinessType.INSERT)
    @PostMapping("/addList")
    public boolean addList(@RequestBody List<SysDept> depts, boolean savaFlag) {
        return deptService.insertDepts(depts, savaFlag);
    }

    /**
     * 删除同步部门数据
     */
    @PostMapping("/deleteSyncDept")
    public Boolean deleteSyncDept() {
        return deptService.deleteSyncDept();
    }

    /**
     * 结构化同步的部门数据
     */
    @GetMapping("/structDept")
    public Boolean structDept(){
        return deptService.structDept();
    }


    /**
     * 修改部门
     */
    @RequiresPermissions("system:dept:edit")
    @Log(title = "部门管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysDept dept)
    {
        Long deptId = dept.getDeptId();
        deptService.checkDeptDataScope(deptId);
        if (!deptService.checkDeptNameUnique(dept))
        {
            return error("修改部门'" + dept.getDeptName() + "'失败，部门名称已存在");
        }
        else if (dept.getParentId().equals(deptId))
        {
            return error("修改部门'" + dept.getDeptName() + "'失败，上级部门不能是自己");
        }
        else if (StringUtils.equals(UserConstants.DEPT_DISABLE, dept.getStatus()) && deptService.selectNormalChildrenDeptById(deptId) > 0)
        {
            return error("该部门包含未停用的子部门！");
        }
        dept.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(deptService.updateDept(dept));
    }

    /**
     * 删除部门
     */
    @RequiresPermissions("system:dept:remove")
    @Log(title = "部门管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{deptId}")
    public AjaxResult remove(@PathVariable Long deptId)
    {
        if (deptService.hasChildByDeptId(deptId))
        {
            return warn("存在下级部门,不允许删除");
        }
        if (deptService.checkDeptExistUser(deptId))
        {
            return warn("部门存在用户,不允许删除");
        }
        deptService.checkDeptDataScope(deptId);
        return toAjax(deptService.deleteDeptById(deptId));
    }

    /**
     * 根据第三方部门 id 获取组织机构信息
     * @param thridDeptId
     * @return
     */
    @InnerAuth
    @GetMapping("/getByThridDeptId")
    public SysDept getByThridDeptId(@RequestParam String thridDeptId) {
        if (StringUtils.isEmpty(thridDeptId)) {
            throw new RuntimeException("第三方部门 id 不能为空");
        }
        return deptService.getByThridDeptId(thridDeptId);
    }


    /**
     * 根据第三方部门 id 获取组织机构信息
     * @param thridDeptId
     * @return
     */
    @InnerAuth
    @GetMapping("/getBySwitchListThridDeptId")
    public List<SysDept> getBySwitchListThridDeptId(@RequestParam String thridDeptId) {
        if (StringUtils.isEmpty(thridDeptId)) {
            throw new RuntimeException("第三方部门 id 不能为空");
        }
        return deptService.getBySwitchListThridDeptId(thridDeptId);
    }

    /**
     * 获取组织机构信息
     * @return
     */
    @PostMapping("/selectOrgInfoList")
    @InnerAuth
    public List<SysDept> selectOrgInfoList(@RequestBody OrgInfoQueryDTO queryDTO){
        queryDTO.setQueryOrg("1");
        return deptService.selectOrgInfoList(queryDTO);
    }

    @InnerAuth
    @GetMapping("/findSecondDept")
    public SysDept findSecondDept(@RequestParam String thridDeptId) {
        if (StringUtils.isEmpty(thridDeptId)) {
            throw new RuntimeException("第三方部门 id 不能为空");
        }
        return deptService.findSecondDept(thridDeptId);
    }

    @InnerAuth
    @GetMapping("/getTwoLevelDeptByDeptId")
    public SysDept getTwoLevelDeptByDeptId(@RequestParam Long deptId) {
        if (null == deptId) {
            throw new RuntimeException("部门 id 不能为空");
        }
        return deptService.getTwoLevelDeptByDeptId(deptId);
    }

    @GetMapping("/getTwoLevelDepts")
    @InnerAuth
    public List<SysDept> getTwoLevelDepts() {
        return deptService.getTwoLevelDepts();
    }

    @GetMapping("/getThreeLevelDepts")
    @InnerAuth
    public List<SysDept> getThreeLevelDepts() {
        return deptService.getThreeLevelDepts();
    }

    /**
     * 根据第三方部门 id 获取组织机构信息(本部门及以下部门，含项目部)
     * @param thridDeptId
     * @return
     */
    @InnerAuth
    @GetMapping("/getDeptByThridDeptId")
    public List<SysDept> getDeptByThridDeptId(@RequestParam Object thridDeptId) {
        String thridDeptIdNew = String.valueOf(thridDeptId);
        if (StringUtils.isEmpty(thridDeptIdNew)) {
            throw new RuntimeException("第三方部门 id 不能为空");
        }
        return deptService.getDeptByThridDeptId(thridDeptIdNew);
    }

}
