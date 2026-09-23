package com.zhaocai.business.vendor.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.vendor.domain.TInterfaceLog;

import java.util.List;

/**
 * 接口访问记录Service接口
 *
 * @author hyt
 * @date 2023/7/20 11:56
 */
public interface ITInterfaceLogService extends IService<TInterfaceLog> {
    /**
     * 查询接口访问记录
     *
     * @param id 接口访问记录主键
     * @return 接口访问记录
     */
    TInterfaceLog selectTInterfaceLogById(Long id);

    /**
     * 查询接口访问记录列表
     *
     * @param tInterfaceLog 接口访问记录
     * @return 接口访问记录集合
     */
    List<TInterfaceLog> selectTInterfaceLogList(TInterfaceLog tInterfaceLog);

    /**
     * 新增接口访问记录
     *
     * @param tInterfaceLog 接口访问记录
     * @return 结果
     */
    int insertTInterfaceLog(TInterfaceLog tInterfaceLog);

    /**
     * 修改接口访问记录
     *
     * @param tInterfaceLog 接口访问记录
     * @return 结果
     */
    int updateTInterfaceLog(TInterfaceLog tInterfaceLog);

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
