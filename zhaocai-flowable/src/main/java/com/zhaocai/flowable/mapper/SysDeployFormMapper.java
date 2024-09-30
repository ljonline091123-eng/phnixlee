package com.zhaocai.flowable.mapper;


import com.zhaocai.flowable.domain.SysDeployForm;
import com.zhaocai.flowable.domain.SysForm;

/**
 * 流程实例关联表单Mapper接口
 * 
 * @author Acecehgnui Xuan
 * @date 2021-03-30
 */
public interface SysDeployFormMapper 
{

    /**
     * 新增流程实例关联表单
     * 
     * @param SysDeployForm 流程实例关联表单
     * @return 结果
     */
    int insertSysDeployForm(SysDeployForm SysDeployForm);

    /**
     * 修改流程实例关联表单
     * 
     * @param SysDeployForm 流程实例关联表单
     * @return 结果
     */
    int updateSysDeployForm(SysDeployForm SysDeployForm);


    int deleteSysDeployFormByDeployIds(String[] deployIds);

    /**
     * 查询流程挂载的表单
     * @param deployId
     */
    SysForm selectSysDeployFormByDeployId(String deployId);

}
