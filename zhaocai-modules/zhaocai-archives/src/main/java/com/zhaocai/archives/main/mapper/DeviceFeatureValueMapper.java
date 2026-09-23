package com.zhaocai.archives.main.mapper;

import java.util.List;
import com.zhaocai.archives.main.domain.DeviceFeatureValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 设备特征值主Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface DeviceFeatureValueMapper extends BaseMapper<DeviceFeatureValue>
{
    /**
     * 查询设备特征值主
     *
     * @param id 设备特征值主主键
     * @return 设备特征值主
     */
    public DeviceFeatureValue selectDeviceFeatureValueById(String id);

    /**
     * 查询设备特征值主列表
     *
     * @param deviceFeatureValue 设备特征值主
     * @return 设备特征值主集合
     */
    public List<DeviceFeatureValue> selectDeviceFeatureValueList(DeviceFeatureValue deviceFeatureValue);

    /**
     * 新增设备特征值主
     *
     * @param deviceFeatureValue 设备特征值主
     * @return 结果
     */
    public int insertDeviceFeatureValue(DeviceFeatureValue deviceFeatureValue);

    /**
     * 修改设备特征值主
     *
     * @param deviceFeatureValue 设备特征值主
     * @return 结果
     */
    public int updateDeviceFeatureValue(DeviceFeatureValue deviceFeatureValue);

    /**
     * 删除设备特征值主
     *
     * @param id 设备特征值主主键
     * @return 结果
     */
    public int deleteDeviceFeatureValueById(String id);

    /**
     * 批量删除设备特征值主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDeviceFeatureValueByIds(String[] ids);

    long selectDeviceFeatureValueListCount(DeviceFeatureValue deviceFeatureValue);
}
