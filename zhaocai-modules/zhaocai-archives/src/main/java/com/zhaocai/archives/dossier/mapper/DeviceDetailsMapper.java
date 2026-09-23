package com.zhaocai.archives.dossier.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.dossier.domain.DeviceDetails;

import java.util.List;

/**
 * 设备详情Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface DeviceDetailsMapper extends BaseMapper<DeviceDetails> {
    /**
     * 查询设备详情
     *
     * @param id 设备详情主键
     * @return 设备详情
     */
    public DeviceDetails selectDeviceDetailsById(Long id);

    /**
     * 查询设备详情列表
     *
     * @param deviceDetails 设备详情
     * @return 设备详情集合
     */
    public List<DeviceDetails> selectDeviceDetailsList(DeviceDetails deviceDetails);

    public long selectDeviceDetailsListCount(DeviceDetails deviceDetails);

    /**
     * 新增设备详情
     *
     * @param deviceDetails 设备详情
     * @return 结果
     */
    public int insertDeviceDetails(DeviceDetails deviceDetails);

    /**
     * 修改设备详情
     *
     * @param deviceDetails 设备详情
     * @return 结果
     */
    public int updateDeviceDetails(DeviceDetails deviceDetails);

    /**
     * 删除设备详情
     *
     * @param id 设备详情主键
     * @return 结果
     */
    public int deleteDeviceDetailsById(Long id);

    /**
     * 批量删除设备详情
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDeviceDetailsByIds(Long[] ids);

    List<DeviceDetails> getProcessed(String organCode);
}
