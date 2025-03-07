package com.zhaocai.archives.task.mapper;

/**
 * @author hyt
 * @date 2023/7/20 11:55
 */

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.task.domain.TbMaterialInterfaceLog;

import java.util.List;

/**
 * 接口访问记录Mapper接口
 *
 * @author ruoyi
 * @date 2023-07-20
 */
public interface TbMaterialInterfaceLogMapper extends BaseMapper<TbMaterialInterfaceLog> {
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

    List<TbMaterialInterfaceLog> selectLikeList(TbMaterialInterfaceLog tInterfaceLog);

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
