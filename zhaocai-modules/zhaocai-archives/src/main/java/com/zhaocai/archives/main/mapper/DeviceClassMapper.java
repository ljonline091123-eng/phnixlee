package com.zhaocai.archives.main.mapper;

import java.util.List;
import com.zhaocai.archives.main.domain.DeviceClass;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

/**
 * 设备分类主Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface DeviceClassMapper extends BaseMapper<DeviceClass>
{
    /**
     * 查询设备分类主
     *
     * @param id 设备分类主主键
     * @return 设备分类主
     */
    public DeviceClass selectDeviceClassById(String id);

    /**
     * 查询设备分类主列表
     *
     * @param deviceClass 设备分类主
     * @return 设备分类主集合
     */
    public List<DeviceClass> selectDeviceClassList(DeviceClass deviceClass);

    /**
     * 新增设备分类主
     *
     * @param deviceClass 设备分类主
     * @return 结果
     */
    public int insertDeviceClass(DeviceClass deviceClass);

    /**
     * 修改设备分类主
     *
     * @param deviceClass 设备分类主
     * @return 结果
     */
    public int updateDeviceClass(DeviceClass deviceClass);

    /**
     * 删除设备分类主
     *
     * @param id 设备分类主主键
     * @return 结果
     */
    public int deleteDeviceClassById(String id);

    /**
     * 批量删除设备分类主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDeviceClassByIds(String[] ids);

    public Integer getMaxCode(@Param("upCode") String upCode, @Param("upId") String upId);

    long selectDeviceClassListCount(DeviceClass deviceClass);

    int getMaterialJoinNoMy(String id);
}
