package com.zhaocai.archives.main.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.main.domain.DeviceFeature;

import java.util.List;

/**
 * 设备特征项主Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IDeviceFeatureService extends IService<DeviceFeature> {
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
     * 批量删除设备特征项主
     *
     * @param ids 需要删除的设备特征项主主键集合
     * @return 结果
     */
    public boolean deleteDeviceFeatureByIds(String[] ids);

    /**
     * 删除设备特征项主信息
     *
     * @param id 设备特征项主主键
     * @return 结果
     */
    public int deleteDeviceFeatureById(String id);

    long selectDeviceFeatureListCount(DeviceFeature deviceFeature);
}
