package com.zhaocai.archives.task.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.task.domain.TbMaterialInterfaceLog;


import java.util.List;

/**
 * 接口访问记录Service接口
 *
 * @author hyt
 * @date 2023/7/20 11:56
 */
public interface TbMaterialInterfaceLogService extends IService<TbMaterialInterfaceLog> {
    /**
     * 查询接口访问记录
     *
     * @param id 接口访问记录主键
     * @return 接口访问记录
     */
    TbMaterialInterfaceLog selectTInterfaceLogById(Long id);

    /**
     * 查询接口访问记录列表
     *
     * @param tInterfaceLog 接口访问记录
     * @return 接口访问记录集合
     */
    List<TbMaterialInterfaceLog> selectTInterfaceLogList(TbMaterialInterfaceLog tInterfaceLog);

    /**
     * 新增接口访问记录
     *
     * @param tInterfaceLog 接口访问记录
     * @return 结果
     */
    int insertTInterfaceLog(TbMaterialInterfaceLog tInterfaceLog);

    /**
     * 修改接口访问记录
     *
     * @param tInterfaceLog 接口访问记录
     * @return 结果
     */
    int updateTInterfaceLog(TbMaterialInterfaceLog tInterfaceLog);

    /**
     * 批量删除接口访问记录
     *
     * @param ids 需要删除的接口访问记录主键集合
     * @return 结果
     */
    int deleteTInterfaceLogByIds(Long[] ids);

    /**
     * 删除接口访问记录信息
     *
     * @param id 接口访问记录主键
     * @return 结果
     */
    int deleteTInterfaceLogById(Long id);
}
