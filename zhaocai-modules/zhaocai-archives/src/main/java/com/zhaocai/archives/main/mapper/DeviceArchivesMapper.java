package com.zhaocai.archives.main.mapper;

import java.util.List;
import com.zhaocai.archives.main.domain.DeviceArchives;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;

/**
 * 设备档案主Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface DeviceArchivesMapper extends BaseMapper<DeviceArchives>
{
    /**
     * 查询设备档案主
     *
     * @param id 设备档案主主键
     * @return 设备档案主
     */
    public DeviceArchives selectDeviceArchivesById(String id);

    /**
     * 查询设备档案主列表
     *
     * @param deviceArchives 设备档案主
     * @return 设备档案主集合
     */
    public List<DeviceArchives> selectDeviceArchivesList(DeviceArchives deviceArchives);

    /**
     * 新增设备档案主
     *
     * @param deviceArchives 设备档案主
     * @return 结果
     */
    public int insertDeviceArchives(DeviceArchives deviceArchives);

    /**
     * 修改设备档案主
     *
     * @param deviceArchives 设备档案主
     * @return 结果
     */
    public int updateDeviceArchives(DeviceArchives deviceArchives);

    /**
     * 删除设备档案主
     *
     * @param id 设备档案主主键
     * @return 结果
     */
    public int deleteDeviceArchivesById(String id);

    /**
     * 批量删除设备档案主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteDeviceArchivesByIds(String[] ids);

    long selectDeviceArchivesListCount(DeviceArchives deviceArchives);
}
