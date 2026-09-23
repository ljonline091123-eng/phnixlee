package com.zhaocai.archives.dossier.mapper;

import java.util.List;
import com.zhaocai.archives.dossier.domain.DeviceItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.dossier.domain.MaterialType;

/**
 * 设备特征项Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface DeviceItemMapper extends BaseMapper<DeviceItem>
{
    /**
     * 查询设备特征项
     *
     * @param id 设备特征项主键
     * @return 设备特征项
     */
    public DeviceItem selectDeviceItemById(Long id);

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
     * 删除设备特征项
     *
     * @param id 设备特征项主键
     * @return 结果
     */
    public int deleteDeviceItemById(Long id);

    /**
     * 批量删除设备特征项
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDeviceItemByIds(Long[] ids);

    long selectDeviceItemListCount(DeviceItem deviceItem);


    List<DeviceItem> getProcessed(String organCode);
}
