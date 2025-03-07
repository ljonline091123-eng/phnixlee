package com.zhaocai.archives.dossier.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.domain.DeviceDetails;
import com.zhaocai.archives.dossier.domain.DeviceEigenvalue;
import com.zhaocai.archives.main.domain.DeviceArchives;

import java.util.List;
import java.util.Map;

/**
 * 设备详情Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IDeviceDetailsService extends IService<DeviceDetails> {

    public List<DeviceDetails> selectDeviceDetailsListNoChange(DeviceDetails deviceDetails);

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
     * 批量删除设备详情
     *
     * @param ids 需要删除的设备详情主键集合
     * @return 结果
     */
    public boolean deleteDeviceDetailsByIds(Long[] ids);

    /**
     * 删除设备详情信息
     *
     * @param id 设备详情主键
     * @return 结果
     */
    public int deleteDeviceDetailsById(Long id);

    List<DeviceDetails> initData(DeviceDetails deviceDetails);

    void addTypeByMain(DeviceArchives mtrClass);

    void updateByHostId(DeviceArchives mtrClass);

    void deleteByHostId(String[] histIds, Map<Long,Long> idsMap);

    int addToMain(DeviceDetails deviceDetails);

    int associationToMain(DeviceDetails deviceDetails);

    int unAssociationToMain(DeviceDetails deviceDetails);


    long selectDeviceDetailsListCount(DeviceDetails deviceDetails);

    List<DeviceDetails> getProcessed(String organCode);
}
