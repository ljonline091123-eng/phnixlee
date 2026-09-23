package com.zhaocai.archives.dossier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.domain.DeviceEigenvalue;
import com.zhaocai.archives.dossier.domain.DeviceItem;
import com.zhaocai.archives.dossier.domain.DeviceType;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.mapper.DeviceEigenvalueMapper;
import com.zhaocai.archives.dossier.mapper.DeviceItemMapper;
import com.zhaocai.archives.dossier.service.IDeviceItemService;
import com.zhaocai.archives.dossier.service.IDeviceTypeService;
import com.zhaocai.archives.main.domain.DeviceClass;
import com.zhaocai.archives.main.domain.DeviceFeature;
import com.zhaocai.archives.main.service.IDeviceClassService;
import com.zhaocai.archives.main.service.IDeviceFeatureService;
import com.zhaocai.archives.utils.KeyUtils;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 设备特征项Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class DeviceItemServiceImpl extends ServiceImpl<DeviceItemMapper, DeviceItem> implements IDeviceItemService {
    @Autowired
    private DeviceItemMapper deviceItemMapper;

    @Resource
    private IDeviceTypeService deviceTypeService;

    @Resource
    private IDeviceFeatureService iDeviceFeatureService;

    @Resource
    private IDeviceClassService iDeviceClassService;

    @Autowired
    private DeviceEigenvalueMapper deviceEigenvalueMapper;

    /**
     * 查询设备特征项
     *
     * @param id 设备特征项主键
     * @return 设备特征项
     */
    @Override
    public DeviceItem selectDeviceItemById(Long id) {
        DeviceItem deviceItem = deviceItemMapper.selectDeviceItemById(id);
//        if ("N".equals(deviceItem.getIsMain())) {
//            deviceItem.setItemCode(deviceItem.getItemCode() + "-" + deviceItem.getOrganCode().substring(0, 4));
//        }
        return deviceItem;
    }

    /**
     * 查询设备特征项列表
     *
     * @param deviceItem 设备特征项
     * @return 设备特征项
     */
    @Override
    public List<DeviceItem> selectDeviceItemListNoChange(DeviceItem deviceItem) {
        return deviceItemMapper.selectDeviceItemList(deviceItem);
    }


    /**
     * 查询设备特征项列表
     *
     * @param deviceItem 设备特征项
     * @return 设备特征项
     */
    @Override
    public List<DeviceItem> selectDeviceItemList(DeviceItem deviceItem) {
        if (deviceItem == null) {
            deviceItem = new DeviceItem();
        }
        deviceItem.setDelFlag("0");
        List<DeviceItem> deviceItems = deviceItemMapper.selectDeviceItemList(deviceItem);
        deviceItems.forEach(item -> {
            if ("N".equals(item.getIsMain())) {
                item.setItemCode(item.getItemCode() + "-" + item.getOrganCode().substring(0, 4));
            }
        });
        Map<Long, Long> idsMap = new HashMap<>();
        if (MaterialType.ALL.equals(deviceItem.getQueryType()) || MaterialType.UNTREATED.equals(deviceItem.getQueryType())) {
            //查询未处理的项数据
            DeviceItem materialItem1 = new DeviceItem();
            materialItem1.setState(3L);
            materialItem1.setDelFlag("0");
            materialItem1.setIsMain("N");
            materialItem1.setMainId("0");
            materialItem1.setOrganCode(deviceItem.getOrganCode());
            List<DeviceItem> materialItems1 = this.selectDeviceItemListNoChange(materialItem1);
            materialItems1.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });

            DeviceEigenvalue eigenvalue = new DeviceEigenvalue();
            eigenvalue.setState(3L);
            eigenvalue.setDelFlag("0");
            eigenvalue.setIsMain("N");
            eigenvalue.setMainId("0");
            eigenvalue.setTypeId(deviceItem.getTypeId());
            eigenvalue.setOrganCode(deviceItem.getOrganCode());
            List<DeviceEigenvalue> list = deviceEigenvalueMapper.selectDeviceEigenvalueList(eigenvalue);
            list.forEach(item -> {
                idsMap.put(item.getItemId(), item.getItemId());
            });
        }
        if (MaterialType.ALL.equals(deviceItem.getQueryType()) || MaterialType.PROCESSED.equals(deviceItem.getQueryType())) {
            List<DeviceEigenvalue> eigenvalues = deviceEigenvalueMapper.getProcessed(deviceItem.getOrganCode());
            eigenvalues.forEach(item -> {
                idsMap.put(item.getItemId(), item.getItemId());
            });
            List<DeviceItem> processed = baseMapper.getProcessed(deviceItem.getOrganCode());
            //查询已处理的项数据
            processed.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (StringUtils.isEmpty(deviceItem.getQueryType())) {
            return deviceItems;
        } else {
            deviceItems.removeIf(item -> !idsMap.containsKey(item.getId()));
        }
        return deviceItems;
    }

    /**
     * 新增设备特征项
     *
     * @param deviceItem 设备特征项
     * @return 结果
     */
    @Override
    public synchronized int insertDeviceItem(DeviceItem deviceItem) {
        if (deviceItem == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(deviceItem.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (deviceItem.getTypeId() == null) {
            throw new RuntimeException("机构编码不能为空");
        }
        DeviceItem materialItem1 = new DeviceItem();
        materialItem1.setTypeId(deviceItem.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(deviceItem.getOrganCode());
        materialItem1.setItemCode(deviceItem.getItemCode());
        materialItem1.setIsMain("N");
        List<DeviceItem> materialItems = baseMapper.selectDeviceItemList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            throw new RuntimeException("特征编码已存在");
        }
        if (deviceItem.getId() == null) {
            deviceItem.setId(KeyUtils.generateId());
            deviceItem.setCreateId(SecurityUtils.getUserId());
            deviceItem.setCreateBy(SecurityUtils.getUsername());
            deviceItem.setCreateTime(DateUtils.getNowDate());
            deviceItem.setState(0L);
            deviceItem.setIsMain("N");
//            materialItem.setDeptId(SecurityUtils.getSysUser().getDeptId());
        }
        deviceItem.setCreateTime(DateUtils.getNowDate());
        return deviceItemMapper.insertDeviceItem(deviceItem);
    }

    /**
     * 修改设备特征项
     *
     * @param deviceItem 设备特征项
     * @return 结果
     */
    @Override
    public int updateDeviceItem(DeviceItem deviceItem) {
        if (deviceItem == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(deviceItem.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (deviceItem.getTypeId() == null) {
            throw new RuntimeException("类型不能为空");
        }
//        String qc = "-"+deviceItem.getOrganCode().substring(0, 4);
//        deviceItem.setItemCode(deviceItem.getItemCode().replace(qc, ""));
        DeviceItem materialItem1 = new DeviceItem();
        materialItem1.setTypeId(deviceItem.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(deviceItem.getOrganCode());
        materialItem1.setItemCode(deviceItem.getItemCode());
        materialItem1.setIsMain("N");
        List<DeviceItem> materialItems = baseMapper.selectDeviceItemList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            for (DeviceItem item : materialItems) {
                if (!deviceItem.getTypeId().equals(item.getTypeId())) {
                    throw new RuntimeException("特征编码已存在");
                }
            }
        }
        deviceItem.setUpdateTime(DateUtils.getNowDate());
        deviceItem.setUpdateId(SecurityUtils.getUserId());
        deviceItem.setUpdateBy(SecurityUtils.getUsername());
        deviceItem.setUpdateTime(DateUtils.getNowDate());
        return deviceItemMapper.updateDeviceItem(deviceItem);
    }

    /**
     * 批量删除设备特征项
     *
     * @param ids 需要删除的设备特征项主键
     * @return 结果
     */
    @Override
    public boolean deleteDeviceItemByIds(Long[] ids) {
        if (ids == null) {
            throw new RuntimeException("参数为空");
        }
        List<DeviceItem> deviceItems = deviceItemMapper.selectBatchIds(Arrays.asList(ids));
        if (deviceItems != null && !deviceItems.isEmpty()) {
            for (DeviceItem deviceItem : deviceItems) {
                if ("Y".equals(deviceItem.getIsMain())) {
                    throw new RuntimeException("主库数据不允许删除");
                }
                if (deviceItem.getState() != 0L) {
                    throw new RuntimeException("流程中数据不允许删除");
                }
            }
            deviceEigenvalueMapper.updateDelByItemId(ids);
        }
        return this.removeBatchByIds(Arrays.asList(ids));
    }

    /**
     * 删除设备特征项信息
     *
     * @param id 设备特征项主键
     * @return 结果
     */
    @Override
    public int deleteDeviceItemById(Long id) {
        return deviceItemMapper.deleteDeviceItemById(id);
    }

    @Override
    public List<DeviceItem> initData(DeviceItem deviceItem) {
        List<DeviceItem> materialItems = deviceItemMapper.selectDeviceItemList(deviceItem);
        List<DeviceFeature> mtrFeatures = iDeviceFeatureService.selectDeviceFeatureList(null);
        DeviceType type1 = new DeviceType();
        type1.setOrganCode(deviceItem.getOrganCode());
        type1.setIsMain("Y");
        List<DeviceType> materialTypes = deviceTypeService.selectDeviceTypeList(type1);
        if (materialTypes == null || materialTypes.isEmpty()) {
            throw new RuntimeException("请先配置设备类型");
        }
        Map<String, Long> params = new HashMap<>();
        materialTypes.forEach(item -> {
            if ("Y".equals(item.getIsMain())) {
                params.put(item.getHostId(), item.getId());
            }
        });
        Map<String, DeviceFeature> mtrMap = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(mtrFeature -> {
                mtrMap.put(mtrFeature.getId(), mtrFeature);
            });
        }
        if (materialItems == null || materialItems.isEmpty()) {
            //初始化-最开始无数据情况
            if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
                mtrFeatures.forEach(mtrFeature -> {
                    DeviceItem bean = new DeviceItem();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeature.getDeviceClassId()));
                    bean.setItemName(mtrFeature.getFeatureName());
                    bean.setItemCode(mtrFeature.getFeatureCode());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(deviceItem.getOrganCode());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
                    bean.setHostId(mtrFeature.getId());
                    materialItems.add(bean);
                });
                this.saveBatch(materialItems);
            }
        } else {
            //有数据的情况，对比主库数据，新增主库存在副库不存在的数据，且非本副库新增至主库数据。（关联数据是否还需要将主库数据同步到副库）
            Map<String, Long> items = new HashMap<>();
            materialItems.forEach(item -> {
                if ("Y".equals(item.getIsMain())) {
                    items.put(item.getHostId(), item.getId());
                }
            });
            List<DeviceItem> addList = new ArrayList<>();
            for (String key : mtrMap.keySet()) {
                if (!items.containsKey(key)) {
                    DeviceFeature mtrFeature = mtrMap.get(key);
                    DeviceItem bean = new DeviceItem();
                    bean.setId(KeyUtils.generateId());
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeature.getDeviceClassId()));
                    bean.setItemName(mtrFeature.getFeatureName());
                    bean.setItemCode(mtrFeature.getFeatureCode());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(deviceItem.getOrganCode());
                    bean.setHostId(mtrFeature.getId());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
                    materialItems.add(bean);
                    addList.add(bean);
                }
            }
            if (!addList.isEmpty()) {
                this.saveBatch(addList);
            }
        }
        return materialItems;
    }


    @Override
    public void addTypeByMain(DeviceFeature mtrFeature) {
        DeviceType materialType = new DeviceType();
        materialType.setHostId(mtrFeature.getDeviceClassId());
        List<DeviceType> materialTypes = deviceTypeService.selectDeviceTypeList(materialType);
        //已存在数据排除
        DeviceItem type1 = new DeviceItem();
        type1.setHostId(mtrFeature.getId());
        type1.setDelFlag("0");
        List<DeviceItem> materialTypes1 = baseMapper.selectDeviceItemList(type1);
        Map<String, Long> map = new HashMap<>();
        if (materialTypes1 != null && !materialTypes1.isEmpty()) {
            materialTypes1.forEach(materialType1 -> {
                map.put(materialType1.getOrganCode(), materialType1.getTypeId());
            });
        }
        List<DeviceItem> addList = new ArrayList<>();
        if (materialTypes != null && !materialTypes.isEmpty()) {
            for (DeviceType type : materialTypes) {
                if (map.containsKey(type.getOrganCode()) && (type.getId() + "").equals(map.get(type.getOrganCode()) + "")) {
                    continue;
                }
                DeviceItem bean = new DeviceItem();
                bean.setId(KeyUtils.generateId());
                bean.setTypeId(type.getId());
                bean.setItemName(mtrFeature.getFeatureName());
                bean.setItemCode(mtrFeature.getFeatureCode());
                bean.setCreateId(SecurityUtils.getUserId());
                bean.setIsMain("Y");
                bean.setState(3L);
                bean.setOrganCode(type.getOrganCode());
                bean.setCreateBy(SecurityUtils.getUsername());
                bean.setCreateTime(DateUtils.getNowDate());
                bean.setHostId(mtrFeature.getId());
                addList.add(bean);
            }
        }
        if (!addList.isEmpty()) {
            this.saveBatch(addList);
        }

    }

    @Override
    public void updateByHostId(DeviceFeature mtrFeature) {
        DeviceItem item = new DeviceItem();
        item.setHostId(mtrFeature.getId());
        item.setDelFlag("0");
        List<DeviceItem> materialItems = baseMapper.selectDeviceItemList(item);
        if (materialItems != null && !materialItems.isEmpty()) {
            materialItems.forEach(bean -> {
                bean.setItemName(mtrFeature.getFeatureName());
                bean.setItemCode(mtrFeature.getFeatureCode());
            });
        }
        this.updateBatchById(materialItems);
    }


    @Override
    public void deleteByHostId(String[] histIds, Map<Long, Long> idsMap) {
        if (histIds != null) {
            QueryWrapper<DeviceItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("del_flag", "0");
            queryWrapper.in("host_id", Arrays.asList(histIds));
            List<DeviceItem> materialTypes = this.list(queryWrapper);
            //查询新增至主库时不同级或者编码重复的数据
            QueryWrapper<DeviceItem> qw = new QueryWrapper<>();
            qw.eq("del_flag", "0");
            qw.in("main_id", Arrays.asList(histIds));
            qw.eq("is_main","Y");
            materialTypes.addAll(this.list(qw));
            List<DeviceItem> newDeviceDetails = new ArrayList<>();
            List<DeviceItem> upDeviceDetails = new ArrayList<>();
            if (idsMap != null && !idsMap.isEmpty()) {
                materialTypes.forEach(item -> {
                    if (idsMap.containsKey(item.getId())) {
                        item.setHostId("0");
                        item.setIsMain("N");
                        item.setHostId("0");
                        upDeviceDetails.add(item);
                    } else {
                        newDeviceDetails.add(item);
                    }
                });
            }
            List<Long> collect = newDeviceDetails.stream().map(DeviceItem::getId).collect(Collectors.toList());
            this.removeBatchByIds(collect);
            this.updateBatchById(upDeviceDetails);
        }
    }


    /**
     * 新增至主库
     *
     * @param materialItem
     * @return
     */
    @Override
    public synchronized int addToMain(DeviceItem materialItem) {
        if (materialItem.getId() == null) {
            throw new BusinessException("请选择要新增至主库的数据");
        }
        if (materialItem.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        DeviceItem materialType1 = this.selectDeviceItemById(materialItem.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法再次新增");
        }
        DeviceClass mtrClass = iDeviceClassService.selectDeviceClassById(materialItem.getHostId());
        //查询编码是否已在主库存在
        DeviceFeature mtrClass1 = new DeviceFeature();
        mtrClass1.setDeviceClassId(materialItem.getHostId());
        mtrClass1.setValid(0L);
        mtrClass1.setFeatureCode(materialType1.getItemCode());
        if (!StringUtils.isEmpty(materialItem.getItemCode())) {
            mtrClass1.setFeatureCode(materialItem.getItemCode());
        }
        List<DeviceFeature> mtrClasses = iDeviceFeatureService.selectDeviceFeatureList(mtrClass1);
        String key = KeyUtils.generateId() + "";
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            return -1;
        } else {
            if (!StringUtils.isEmpty(materialItem.getItemCode())) {
                DeviceFeature aClass = new DeviceFeature();
                aClass.setId(key);
                aClass.setDeviceClassId(mtrClass.getId());
                aClass.setFeatureName(materialType1.getItemName());
                aClass.setSonId(materialType1.getId());
                aClass.setCreateBy(SecurityUtils.getUsername());
                aClass.setCreateTime(DateUtils.getNowDate());
                aClass.setCreateId(SecurityUtils.getUserId() + "");
                aClass.setFeatureCode(materialItem.getItemCode());
                iDeviceFeatureService.insertDeviceFeature(aClass);
                materialType1.setMainId(aClass.getId());
                materialType1.setIsMain("Y");
                return baseMapper.updateDeviceItem(materialType1);
            } else {
                //不存在时 新增至主库
                materialType1.setIsMain("Y");
                materialType1.setHostId(key);
                int i = baseMapper.updateDeviceItem(materialType1);
                if (i > 0) {
                    DeviceFeature aClass = new DeviceFeature();
                    aClass.setId(key);
                    aClass.setDeviceClassId(mtrClass.getId());
                    aClass.setFeatureCode(materialType1.getItemCode());
                    aClass.setFeatureName(materialType1.getItemName());
                    aClass.setSonId(materialType1.getId());
                    aClass.setCreateBy(SecurityUtils.getUsername());
                    aClass.setCreateTime(DateUtils.getNowDate());
                    aClass.setCreateId(SecurityUtils.getUserId() + "");
                    iDeviceFeatureService.insertDeviceFeature(aClass);
                }
                return i;
            }
        }
    }


    /**
     * 关联至主库
     *
     * @param materialItem
     * @return
     */
    @Override
    public synchronized int associationToMain(DeviceItem materialItem) {
        if (materialItem.getId() == null) {
            throw new BusinessException("请选择要关联至主库的数据");
        }
        if (materialItem.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        DeviceItem materialType1 = this.selectDeviceItemById(materialItem.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法进行关联");
        }
        if (!"0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据已关联至主库，无法再次关联");
        }
        materialType1.setMainId(materialItem.getHostId());
        return baseMapper.updateDeviceItem(materialType1);
    }


    /**
     * 取消关联至主库
     *
     * @param materialItem
     * @return
     */
    @Override
    public synchronized int unAssociationToMain(DeviceItem materialItem) {
        if (materialItem.getId() == null) {
            throw new BusinessException("请选择要取消关联至主库的数据");
        }
        DeviceItem materialType1 = this.selectDeviceItemById(materialItem.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据为主库同步数据，无法进行操作");
        }
        if ("0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据尚未关联至主库，无需取消关联");
        }
        materialType1.setMainId("0");
        return baseMapper.updateDeviceItem(materialType1);
    }

    @Override
    public long selectDeviceItemListCount(DeviceItem deviceItem) {
        return baseMapper.selectDeviceItemListCount(deviceItem);
    }

    @Override
    public List<DeviceItem> getProcessed(String organCode) {
        return baseMapper.getProcessed(organCode);
    }


}
