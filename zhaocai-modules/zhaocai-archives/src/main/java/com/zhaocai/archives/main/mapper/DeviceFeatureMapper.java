package com.zhaocai.archives.main.mapper;

import java.util.List;
import com.zhaocai.archives.main.domain.DeviceFeature;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 设备特征项主Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface DeviceFeatureMapper extends BaseMapper<DeviceFeature>
{
    /**
     * 查询设备特征项主
     *
     * @param id 设备特征项主主键
     * @return 设备特征项主
     */
    public DeviceFeature selectDeviceFeatureById(String id);

    /**
     * 查询设备特征项主列表
     *
     * @param deviceFeature 设备特征项主
     * @return 设备特征项主集合
     */
    public List<DeviceFeature> selectDeviceFeatureList(DeviceFeature deviceFeature);

    /**
     * 新增设备特征项主
     *
     * @param deviceFeature 设备特征项主
     * @return 结果
     */
    public int insertDeviceFeature(DeviceFeature deviceFeature);

    /**
     * 修改设备特征项主
     *
     * @param deviceFeature 设备特征项主
     * @return 结果
     */
    public int updateDeviceFeature(DeviceFeature deviceFeature);

    /**
     * 删除设备特征项主
     *
     * @param id 设备特征项主主键
     * @return 结果
     */
    public int deleteDeviceFeatureById(String id);

    /**
     * 批量删除设备特征项主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDeviceFeatureByIds(String[] ids);

    long selectDeviceFeatureListCount(DeviceFeature deviceFeature);
}
