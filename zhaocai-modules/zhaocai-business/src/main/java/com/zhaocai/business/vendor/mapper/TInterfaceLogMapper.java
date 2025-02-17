package com.zhaocai.business.vendor.mapper;

/**
 * @author hyt
 * @date 2023/7/20 11:55
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.vendor.domain.TInterfaceLog;

import java.util.List;

/**
 * 接口访问记录Mapper接口
 *
 * @author ruoyi
 * @date 2023-07-20
 */
public interface TInterfaceLogMapper extends BaseMapper<TInterfaceLog> {
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

    List<TInterfaceLog> selectLikeList(TInterfaceLog tInterfaceLog);

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
     * 删除接口访问记录
     *
     * @param id 接口访问记录主键
     * @return 结果
     */
    int deleteTInterfaceLogById(Long id);

    /**
     * 批量删除接口访问记录
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    int deleteTInterfaceLogByIds(Long[] ids);
}