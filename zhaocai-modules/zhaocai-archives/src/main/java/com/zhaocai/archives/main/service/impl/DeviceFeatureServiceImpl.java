package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.IDeviceItemService;
import com.zhaocai.archives.main.domain.DeviceFeature;
import com.zhaocai.archives.main.mapper.DeviceFeatureMapper;
import com.zhaocai.archives.main.service.IDeviceFeatureService;
import com.zhaocai.archives.utils.KeyUtils;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 设备特征项主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class DeviceFeatureServiceImpl extends ServiceImpl<DeviceFeatureMapper, DeviceFeature> implements IDeviceFeatureService {
    @Autowired
    private DeviceFeatureMapper deviceFeatureMapper;

    @Resource
    private IDeviceItemService iDeviceItemService;

    /**
     * 查询设备特征项主
     *
     * @param id 设备特征项主主键
     * @return 设备特征项主
     */
    @Override
    public DeviceFeature selectDeviceFeatureById(String id) {
        return deviceFeatureMapper.selectDeviceFeatureById(id);
    }

    /**
     * 查询设备特征项主列表
     *
     * @param deviceFeature 设备特征项主
     * @return 设备特征项主
     */
    @Override
    public List<DeviceFeature> selectDeviceFeatureList(DeviceFeature deviceFeature) {
        if (deviceFeature == null) {
            deviceFeature = new DeviceFeature();
        }
        deviceFeature.setValid(0L);
        return deviceFeatureMapper.selectDeviceFeatureList(deviceFeature);
    }

    /**
     * 新增设备特征项主
     *
     * @param deviceFeature 设备特征项主
     * @return 结果
     */
    @Override
    public synchronized int insertDeviceFeature(DeviceFeature deviceFeature) {
        if (deviceFeature == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(deviceFeature.getDeviceClassId())) {
            throw new RuntimeException("类型id不能为空");
        }
        DeviceFeature mtrFeature1 = new DeviceFeature();
        mtrFeature1.setFeatureCode(deviceFeature.getFeatureCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setDeviceClassId(deviceFeature.getDeviceClassId());
        List<DeviceFeature> mtrFeatures = baseMapper.selectDeviceFeatureList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            throw new RuntimeException("当前特征项编号已存在");
        }
        if (deviceFeature.getId() == null) {
            deviceFeature.setId(KeyUtils.generateId() + "");
            deviceFeature.setCreateTime(DateUtils.getNowDate());
            deviceFeature.setCreateId(SecurityUtils.getUserId() + "");
            deviceFeature.setCreateBy(SecurityUtils.getUsername());
            deviceFeature.setValid(0L);
        }
        int i = deviceFeatureMapper.insertDeviceFeature(deviceFeature);
//        if (i > 0) {
//            iDeviceItemService.addTypeByMain(deviceFeature);
//        }
        return i;
    }

    /**
     * 修改设备特征项主
     *
     * @param deviceFeature 设备特征项主
     * @return 结果
     */
    @Override
    public int updateDeviceFeature(DeviceFeature deviceFeature) {
        if (deviceFeature == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(deviceFeature.getDeviceClassId())) {
            throw new RuntimeException("类型id不能为空");
        }
        DeviceFeature mtrFeature1 = new DeviceFeature();
        mtrFeature1.setFeatureCode(deviceFeature.getFeatureCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setDeviceClassId(deviceFeature.getDeviceClassId());
        List<DeviceFeature> mtrFeatures = baseMapper.selectDeviceFeatureList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            for (DeviceFeature mtrFeature2 : mtrFeatures) {
                if (!deviceFeature.getId().equals(mtrFeature2.getId())) {
                    throw new RuntimeException("当前特征项编号已存在");
                }
            }
        }
        deviceFeature.setUpdateTime(DateUtils.getNowDate());
        deviceFeature.setUpdateBy(SecurityUtils.getUsername());
        deviceFeature.setIsTb("2");
        int i = deviceFeatureMapper.updateDeviceFeature(deviceFeature);
        if (i > 0) {
            iDeviceItemService.updateByHostId(deviceFeature);
        }
        return i;
    }

    /**
     * 批量删除设备特征项主
     *
     * @param ids 需要删除的设备特征项主主键
     * @return 结果
     */
    @Override
    public boolean deleteDeviceFeatureByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<DeviceFeature> mtrFeatures = this.listByIds(Arrays.asList(ids));
        Map<Long, Long> map = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(item -> {
                item.setValid(System.currentTimeMillis() / 1000L);
                item.setIsTb("3");
                if (item.getSonId() != null) {
                    map.put(item.getSonId(), item.getSonId());
                }
            });
        }
        boolean b = this.updateBatchById(mtrFeatures);
        if (b) {
            iDeviceItemService.deleteByHostId(ids,map);
        }
        return b;
    }

    /**
     * 删除设备特征项主信息
     *
     * @param id 设备特征项主主键
     * @return 结果
     */
    @Override
    public int deleteDeviceFeatureById(String id) {
        return deviceFeatureMapper.deleteDeviceFeatureById(id);
    }

    @Override
    public long selectDeviceFeatureListCount(DeviceFeature deviceFeature) {
        return baseMapper.selectDeviceFeatureListCount(deviceFeature);
    }
}
