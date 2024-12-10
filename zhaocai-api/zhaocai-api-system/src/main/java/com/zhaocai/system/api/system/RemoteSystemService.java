package com.zhaocai.system.api.system;

import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.constant.ServiceNameConstants;
import com.zhaocai.common.core.web.bean.ResultData;
import com.zhaocai.common.core.web.domain.AjaxResult;
import com.zhaocai.system.api.domain.OrgInfoQueryDTO;
import com.zhaocai.system.api.domain.SetConfigValueDTO;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.domain.SysDictData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(contextId = "remoteSystemService", value = ServiceNameConstants.SYSTEM_SERVICE)
public interface RemoteSystemService {

    /**
     * 获取部门列表
     */
    @PostMapping("/dept/selectDeptList")
    List<SysDept> selectDeptList(@RequestBody SysDept dept, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 获取字典数据
     */
    @PostMapping("/dict/data/selectDictDataList")
    List<SysDictData> selectDictDataList(@RequestBody SysDictData dictData, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 获取部门列表
     */
    @PostMapping("/dept/addList")
    boolean addList(@RequestBody List<SysDept> depts, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 删除同步的部门
     */
    @PostMapping("/dept/deleteSyncDept")
    Boolean deleteSyncDept(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据参数键名查询参数值
     */
    @GetMapping("/config/configKeyStr/{configKey}")
    String configKeyStr(@PathVariable("configKey") String configKey);

    /**
     * 设置配置参数值
     */
    @PostMapping("/config/setConfigValueByKey")
    void setConfigValueByKey(@RequestBody SetConfigValueDTO setConfigValue, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据第三方部门 id 获取组织机构信息
     */
    @GetMapping("/dept/getByThridDeptId")
    SysDept getByThridDeptId(@RequestParam(value = "thridDeptId") String thridDeptId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据第三方部门 id 获取组织机构信息
     */
    @GetMapping("/dept/getSwitchByThridDeptId")
    List<SysDept> getSwitchByThridDeptId(@RequestParam(value = "thridDeptId") String thridDeptId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 获取组织机构信息
     */
    @PostMapping("/dept/selectOrgInfoList")
    List<SysDept> getOrgInfoList(@RequestBody OrgInfoQueryDTO queryDTO, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 同步第三方部门数据
     *
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/syncPlatformData/syncDept")
    ResultData<Boolean> syncDept(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 同步第三方用户数据
     *
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/syncPlatformData/syncUser")
    ResultData<Boolean> syncUser(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 结构化同步的部门数据
     *
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/dept/structDept")
    Boolean structDept(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 初始化同步的用户角色
     *
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/role/initUserRole")
    Integer initUserRole(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping("/dept/findSecondDept")
    SysDept findSecondDept(@RequestParam(value = "thridDeptId") String thridDeptId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);


    /**
     * 查询部门树结构信息
     *
     * @param thridDeptId
     * @return
     */
    @GetMapping("/dept/getBySwitchListThridDeptId")
    List<SysDept> getBySwitchListThridDeptId(@RequestParam(value = "thridDeptId") String thridDeptId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping(value = "/dept/{deptId}")
    AjaxResult getInfo(@PathVariable("deptId") Long deptId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    @GetMapping(value = "/dept/selectChildrenDept")
    List<SysDept> selectChildrenDept(@RequestParam("kyeVal") String kyeVal, @RequestParam("group") String group
            , @RequestParam("deptId") Long deptId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);


    @GetMapping("/dept/getTwoLevelDeptByDeptId")
    SysDept getTwoLevelDeptByDeptId(@RequestParam("deptId") Long deptId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

     @GetMapping("/dept/getTwoLevelDepts")
     List<SysDept> getTwoLevelDepts(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

     @GetMapping("/dept/getThreeLevelDepts")
     List<SysDept> getThreeLevelDepts(@RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据第三方部门 id 获取组织机构信息(本部门及以下部门，含项目部)
     */
    @GetMapping("/dept/getDeptByThridDeptId")
    List<SysDept> getDeptByThridDeptId(@RequestParam(value = "thridDeptId") String thridDeptId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 根据第三方部门 id 获取组织机构信息(本部门及以下部门，不含部门、项目部)
     */
    @GetMapping("/dept/getDeptByThridDeptIdNoBM")
    List<SysDept> getDeptByThridDeptIdNoBM(@RequestParam(value = "thridDeptId") String thridDeptId, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);


}
