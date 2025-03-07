package com.zhaocai.archives.dossier.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.archives.dossier.domain.DeviceItem;
import com.zhaocai.archives.main.domain.DeviceFeature;

import java.util.List;
import java.util.Map;

/**
 * 设备特征项Service接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface IDeviceItemService extends IService<DeviceItem> {
    /**
     * 查询设备特征项
     *
     * @param id 设备特征项主键
     * @return 设备特征项
     */
    public DeviceItem selectDeviceItemById(Long id);


    public List<DeviceItem> selectDeviceItemListNoChange(DeviceItem deviceItem);

    /**
     * 查询设备特征项列表
     *
     * @param deviceItem 设备特征项
     * @return 设备特征项集合
     */
    public List<DeviceItem> selectDeviceItemList(DeviceItem deviceItem);

    /**
     * 新增设备特征项
     *
     * @param deviceItem 设备特征项
     * @return 结果
     */
    public int insertDeviceItem(DeviceItem deviceItem);

    /**
     * 修改设备特征项
     *
     * @param deviceItem 设备特征项
     * @return 结果
     */
    public int updateDeviceItem(DeviceItem deviceItem);

    /**
     * 批量删除设备特征项
     *
     * @param ids 需要删除的设备特征项主键集合
     * @return 结果
     */
    public boolean deleteDeviceItemByIds(Long[] ids);

    /**
     * 删除设备特征项信息
     *
     * @param id 设备特征项主键
     * @return 结果
     */
    public int deleteDeviceItemById(Long id);

    List<DeviceItem> initData(DeviceItem deviceItem);

    void addTypeByMain(DeviceFeature mtrClass);

    void updateByHostId(DeviceFeature mtrClass);

    void deleteByHostId(String[] histIds, Map<Long, Long> idsMap);


    int addToMain(DeviceItem deviceItem);

    int associationToMain(DeviceItem deviceItem);

    int unAssociationToMain(DeviceItem deviceItem);


    long selectDeviceItemListCount(DeviceItem deviceItem);

    List<DeviceItem> getProcessed(String organCode);
}
