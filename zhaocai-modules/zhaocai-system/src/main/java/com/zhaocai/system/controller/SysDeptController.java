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
     * 获取一二级组织机构
     *
     * @param
     * @return
     */
    @GetMapping("/getDeptTree")
    public AjaxResult getDeptTree() {
        return success(deptService.getDeptTree());
    }



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
     * 查询所有部门树结构信息
     */
    @GetMapping("/deptTree")
    public AjaxResult selectDeptTreeList(SysDept dept)
    {
        List<TreeSelect> treeList = deptService.selectAllDeptTreeList(dept);
        return success(treeList);
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

    /**
     * 根据第三方部门 id 获取组织机构信息(本单位和下一层单位)
     * @param thridDeptId
     * @param type 1=含公司、部门、项目部 2=含公司,不含部门、项目部 3=含公司、项目部,不含部门 4=含公司、部门,不含项目部
     * @return
     */
    @InnerAuth
    @GetMapping("/getDeptAndNextDept")
    public List<SysDept> getDeptAndNextDept(@RequestParam Object thridDeptId, @RequestParam Object type) {
        String thridDeptIdNew = String.valueOf(thridDeptId);
        String typeNew = String.valueOf(type);
        if (StringUtils.isEmpty(thridDeptIdNew)) {
            throw new RuntimeException("第三方部门 id 不能为空");
        }
        if (StringUtils.isEmpty(typeNew)) {
            typeNew = "1";
        }
        return deptService.getDeptAndNextDept(thridDeptIdNew, typeNew);
    }

    /**
     * 根据第三方部门 id 获取组织机构信息(本部门及以下部门，含项目部) 树结构
     * @param thridDeptId
     * @return
     */
    @GetMapping("/getDeptTreeByThridDeptId")
    public AjaxResult getDeptTreeByThridDeptId(@RequestParam Object thridDeptId) {
        String thridDeptIdNew = String.valueOf(thridDeptId);
        if (StringUtils.isEmpty(thridDeptIdNew)) {
            throw new RuntimeException("第三方部门 id 不能为空");
        }
        return success(deptService.getDeptTreeByThridDeptId(thridDeptIdNew));
    }

    /**
     * 根据单位id查询对应的二级单位
     * @param deptId
     * @return
     */
    @GetMapping("/getTwoLevelDeptByDeptId1")
    public SysDept getTwoLevelDeptByDeptId1(@RequestParam Long deptId) {
        if (null == deptId) {
            throw new RuntimeException("部门 id 不能为空");
        }
        return deptService.getTwoLevelDeptByDeptId(deptId);
    }

    /**
     * 根据单位id查询对应的部门
     * @param deptId
     * @return
     */
    @GetMapping("/getDeptByDeptId")
    public SysDept getDeptByDeptId(@RequestParam Long deptId) {
        if (null == deptId) {
            throw new RuntimeException("部门 id 不能为空");
        }
        return deptService.getDeptByDeptId(deptId);
    }

    /**
     * 根据第三方部门 id 获取组织机构信息(本部门及以下部门，不含项目部、部门)
     * @param thridDeptId
     * @return
     */
    @InnerAuth
    @GetMapping("/getDeptByThridDeptIdNoBM")
    public List<SysDept> getDeptByThridDeptIdNoBM(@RequestParam Object thridDeptId) {
        String thridDeptIdNew = String.valueOf(thridDeptId);
        if (StringUtils.isEmpty(thridDeptIdNew)) {
            throw new RuntimeException("第三方部门 id 不能为空");
        }
        return deptService.getDeptByThridDeptIdNoBM(thridDeptIdNew);
    }

    /**
     * 根据部门code组装出父级公司名称， ***集团 / ***公司 / ***公司
     */
    @InnerAuth
    @GetMapping("/getDeptNameLoop")
    public String getDeptNameLoop(@RequestParam Object thridDeptId,@RequestParam String deptName) {
        String s =thridDeptId + "";
        SysDept sysDept = deptService.getByThridDeptId(s);
        if (sysDept == null || (deptName!=null&&deptName.length()>900) ) {
            /* 找不到 或者死循环了 就直接返回 */
            return deptName;
        }else{
            /* 集团是顶级不用再递归了 */
            if(sysDept.getThridParentId().equals(UserConstants.GROUP_DEPT_ID)
                    ||sysDept.getThridParentId().equals("0")
                    ||sysDept.getThridParentId().equals("")
                    ||sysDept.getThridParentId()==null){
                /* 是本身层级就不在拼接了 */
                if(deptName!=null&&deptName.equals(sysDept.getDeptName())){
                    return deptName;
                }
                /* 拼接返回 */
                return sysDept.getDeptName() + (StringUtils.isEmpty(deptName)||  "null".equals(deptName)? "" : " / " + deptName);
            }else{
                /* 递归拼接 */
                return getDeptNameLoop(sysDept.getThridParentId(),sysDept.getDeptName() + (StringUtils.isEmpty(deptName)||  "null".equals(deptName)? "" : " / " + deptName));
            }
        }
    }
}
