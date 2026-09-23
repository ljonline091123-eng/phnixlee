package com.zhaocai.archives.dossier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.domain.DeviceDetails;
import com.zhaocai.archives.dossier.domain.DeviceType;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.mapper.DeviceDetailsMapper;
import com.zhaocai.archives.dossier.service.IDeviceDetailsService;
import com.zhaocai.archives.dossier.service.IDeviceTypeService;
import com.zhaocai.archives.main.domain.DeviceArchives;
import com.zhaocai.archives.main.domain.DeviceClass;
import com.zhaocai.archives.main.service.IDeviceArchivesService;
import com.zhaocai.archives.main.service.IDeviceClassService;
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
 * 设备详情Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class DeviceDetailsServiceImpl extends ServiceImpl<DeviceDetailsMapper, DeviceDetails> implements IDeviceDetailsService {
    @Autowired
    private DeviceDetailsMapper deviceDetailsMapper;

    @Resource
    private IDeviceArchivesService iDeviceArchivesService;

    @Resource
    private IDeviceTypeService iDeviceTypeService;

    @Resource
    private IDeviceClassService iDeviceClassService;


    /**
     * 查询设备详情
     *
     * @param id 设备详情主键
     * @return 设备详情
     */
    @Override
    public DeviceDetails selectDeviceDetailsById(Long id) {
        DeviceDetails deviceDetails = deviceDetailsMapper.selectDeviceDetailsById(id);
//        if ("N".equals(deviceDetails.getIsMain())) {
//            deviceDetails.setDeviceCode(deviceDetails.getDeviceCode() + "-" + deviceDetails.getOrganCode().substring(0, 4));
//        }
        return deviceDetails;
    }

    /**
     * 查询设备详情列表
     *
     * @param deviceDetails 设备详情
     * @return 设备详情
     */
    @Override
    public List<DeviceDetails> selectDeviceDetailsListNoChange(DeviceDetails deviceDetails) {
        return deviceDetailsMapper.selectDeviceDetailsList(deviceDetails);
    }


    /**
     * 查询设备详情列表
     *
     * @param deviceDetails 设备详情
     * @return 设备详情
     */
    @Override
    public List<DeviceDetails> selectDeviceDetailsList(DeviceDetails deviceDetails) {
        if (deviceDetails == null) {
            deviceDetails = new DeviceDetails();
        }
        deviceDetails.setDelFlag("0");
        List<DeviceDetails> deviceDetails1 = deviceDetailsMapper.selectDeviceDetailsList(deviceDetails);
        deviceDetails1.forEach(item -> {
            if ("N".equals(item.getIsMain())) {
                item.setDeviceCode(item.getDeviceCode() + "-" + item.getOrganCode().substring(0, 4));
            }
        });
        Map<Long, Long> idsMap = new HashMap<>();
        if (MaterialType.ALL.equals(deviceDetails.getQueryType()) || MaterialType.UNTREATED.equals(deviceDetails.getQueryType())) {
            //查询未处理的项数据
            DeviceDetails details = new DeviceDetails();
            details.setState(3L);
            details.setDelFlag("0");
            details.setIsMain("N");
            details.setMainId("0");
            details.setOrganCode(deviceDetails.getOrganCode());
            List<DeviceDetails> list = baseMapper.selectDeviceDetailsList(details);
            list.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (MaterialType.ALL.equals(deviceDetails.getQueryType()) || MaterialType.PROCESSED.equals(deviceDetails.getQueryType())) {
            List<DeviceDetails> details = baseMapper.getProcessed(deviceDetails.getOrganCode());
            details.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (StringUtils.isEmpty(deviceDetails.getQueryType())) {
            return deviceDetails1;
        } else {
            deviceDetails1.removeIf(item -> !idsMap.containsKey(item.getId()));
        }
        return deviceDetails1;
    }

    /**
     * 新增设备详情
     *
     * @param deviceDetails 设备详情
     * @return 结果
     */
    @Override
    public synchronized int insertDeviceDetails(DeviceDetails deviceDetails) {
        if (deviceDetails == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(deviceDetails.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (deviceDetails.getTypeId() == null) {
            throw new RuntimeException("材料类型不能为空");
        }
        DeviceDetails materialItem1 = new DeviceDetails();
        materialItem1.setTypeId(deviceDetails.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setIsMain("N");
        materialItem1.setOrganCode(deviceDetails.getOrganCode());
        materialItem1.setDeviceCode(deviceDetails.getDeviceCode());
        List<DeviceDetails> materialItems = baseMapper.selectDeviceDetailsList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            throw new RuntimeException("特征编码已存在");
        }
        if (deviceDetails.getId() == null) {
            deviceDetails.setId(KeyUtils.generateId());
            deviceDetails.setCreateId(SecurityUtils.getUserId());
            deviceDetails.setIsMain("N");
            deviceDetails.setState(0L);
            deviceDetails.setCreateBy(SecurityUtils.getUsername());
            deviceDetails.setCreateTime(DateUtils.getNowDate());
        }
        return deviceDetailsMapper.insertDeviceDetails(deviceDetails);
    }

    /**
     * 修改设备详情
     *
     * @param deviceDetails 设备详情
     * @return 结果
     */
    @Override
    public int updateDeviceDetails(DeviceDetails deviceDetails) {
        if (deviceDetails == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(deviceDetails.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (deviceDetails.getTypeId() == null) {
            throw new RuntimeException("材料类型不能为空");
        }
//        String qc = "-"+deviceDetails.getOrganCode().substring(0, 4);
//        deviceDetails.setDeviceCode(deviceDetails.getDeviceCode().replace(qc, ""));
        DeviceDetails materialItem1 = new DeviceDetails();
        materialItem1.setTypeId(deviceDetails.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(deviceDetails.getOrganCode());
        materialItem1.setDeviceCode(deviceDetails.getDeviceCode());
        materialItem1.setIsMain("N");
        List<DeviceDetails> materialItems = baseMapper.selectDeviceDetailsList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            for (DeviceDetails type : materialItems) {
                if (!type.getId().equals(deviceDetails.getId())) {
                    throw new RuntimeException("编码已存在,请刷新后重试");
                }
            }
        }
        deviceDetails.setUpdateId(SecurityUtils.getUserId());
        deviceDetails.setUpdateBy(SecurityUtils.getUsername());
        deviceDetails.setUpdateTime(DateUtils.getNowDate());
        return deviceDetailsMapper.updateDeviceDetails(deviceDetails);
    }

    /**
     * 批量删除设备详情
     *
     * @param ids 需要删除的设备详情主键
     * @return 结果
     */
    @Override
    public boolean deleteDeviceDetailsByIds(Long[] ids) {
        if (ids == null) {
            throw new RuntimeException("参数为空");
        }
        List<DeviceDetails> materialDetails = deviceDetailsMapper.selectBatchIds(Arrays.asList(ids));
        if (materialDetails != null && !materialDetails.isEmpty()) {
            for (DeviceDetails materialDetail : materialDetails) {
                if ("Y".equals(materialDetail.getIsMain())) {
                    throw new RuntimeException("主库数据不能删除");
                }
                if (materialDetail.getState() != null && materialDetail.getState() != 0L) {
                    throw new RuntimeException("已提交数据不能删除");
                }
            }
        }
        return this.removeBatchByIds(Arrays.asList(ids));
    }

    /**
     * 删除设备详情信息
     *
     * @param id 设备详情主键
     * @return 结果
     */
    @Override
    public int deleteDeviceDetailsById(Long id) {
        return deviceDetailsMapper.deleteDeviceDetailsById(id);
    }

    @Override
    public List<DeviceDetails> initData(DeviceDetails deviceDetails) {
        List<DeviceDetails> materialDetailsList = deviceDetailsMapper.selectDeviceDetailsList(deviceDetails);
        List<DeviceArchives> mtrArchivesList = iDeviceArchivesService.selectDeviceArchivesList(null);
        DeviceType type1 = new DeviceType();
        type1.setOrganCode(deviceDetails.getOrganCode());
        type1.setIsMain("Y");
        List<DeviceType> materialTypes = iDeviceTypeService.selectDeviceTypeList(type1);
        if (materialTypes == null || materialTypes.isEmpty()) {
            throw new RuntimeException("请先配置材料类型");
        }
        Map<String, DeviceType> params = new HashMap<>();
        materialTypes.forEach(item -> {
            if ("Y".equals(item.getIsMain())) {
                params.put(item.getHostId(), item);
            }
        });
        Map<String, DeviceArchives> mtrMap = new HashMap<>();
        if (mtrArchivesList != null && !mtrArchivesList.isEmpty()) {
            mtrArchivesList.forEach(mtrFeature -> {
                mtrMap.put(mtrFeature.getId(), mtrFeature);
            });
        }
        if (materialDetailsList == null || materialDetailsList.isEmpty()) {
            //初始化-最开始无数据情况
            if (mtrArchivesList != null && !mtrArchivesList.isEmpty()) {
                mtrArchivesList.forEach(mtrFeature -> {
                    DeviceDetails bean = new DeviceDetails();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeature.getDeviceClassId()) == null ? -1L : params.get(mtrFeature.getDeviceClassId()).getId());
                    bean.setTypeName(params.get(mtrFeature.getDeviceClassId()) == null ? "" : params.get(mtrFeature.getDeviceClassId()).getDeviceName());
                    bean.setUnit(mtrFeature.getMeasureUnit());
                    bean.setDeviceName(mtrFeature.getDeviceName());
                    bean.setDeviceCode(mtrFeature.getDeviceCode());
                    bean.setFeature(mtrFeature.getFeature());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(deviceDetails.getOrganCode());
                    bean.setHostId(mtrFeature.getId());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
                    bean.setDelFlag(mtrFeature.getFeature());
                    materialDetailsList.add(bean);
                });
                this.saveBatch(materialDetailsList);
            }
        } else {
            //有数据的情况，对比主库数据，新增主库存在副库不存在的数据，且非本副库新增至主库数据。（关联数据是否还需要将主库数据同步到副库）
            Map<String, Long> items = new HashMap<>();
            materialDetailsList.forEach(item -> {
                if ("Y".equals(item.getIsMain())) {
                    items.put(item.getHostId(), item.getId());
                }
            });
            List<DeviceDetails> addList = new ArrayList<>();
            for (String key : mtrMap.keySet()) {
                if (!items.containsKey(key)) {
                    DeviceArchives mtrArchives = mtrMap.get(key);
                    DeviceDetails bean = new DeviceDetails();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrArchives.getDeviceClassId()) == null ? -1L : params.get(mtrArchives.getDeviceClassId()).getId());
                    bean.setTypeName(params.get(mtrArchives.getDeviceClassId()) == null ? "" : params.get(mtrArchives.getDeviceClassId()).getDeviceName());
                    bean.setUnit(mtrArchives.getMeasureUnit());
                    bean.setDeviceName(mtrArchives.getDeviceName());
                    bean.setDeviceCode(mtrArchives.getDeviceCode());
                    bean.setFeature(mtrArchives.getFeature());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(deviceDetails.getOrganCode());
                    bean.setHostId(mtrArchives.getId());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
                    bean.setDelFlag(mtrArchives.getFeature());
                    materialDetailsList.add(bean);
                    addList.add(bean);
                }
            }
            if (!addList.isEmpty()) {
                this.saveBatch(addList);
            }
        }
        return materialDetailsList;
    }


    @Override
    public void addTypeByMain(DeviceArchives mtrArchives) {
        DeviceType materialType = new DeviceType();
        materialType.setHostId(mtrArchives.getDeviceClassId());
        List<DeviceType> materialTypes = iDeviceTypeService.selectDeviceTypeList(materialType);
        //已存在数据排除
        DeviceDetails type1 = new DeviceDetails();
        type1.setHostId(mtrArchives.getId());
        type1.setDelFlag("0");
        List<DeviceDetails> materialTypes1 = baseMapper.selectDeviceDetailsList(type1);
        Map<String, Long> map = new HashMap<>();
        if (materialTypes1 != null && !materialTypes1.isEmpty()) {
            materialTypes1.forEach(materialType1 -> {
                map.put(materialType1.getOrganCode(), materialType1.getTypeId());
            });
        }
        List<DeviceDetails> addList = new ArrayList<>();
        if (materialTypes != null && !materialTypes.isEmpty()) {
            for (DeviceType type : materialTypes) {
                if (map.containsKey(type.getOrganCode()) && (type.getId() + "").equals(map.get(type.getOrganCode()) + "")) {
                    continue;
                }
                DeviceDetails bean = new DeviceDetails();
                bean.setId(KeyUtils.generateId());
                bean.setTypeId(type.getId());
                bean.setTypeName(type.getDeviceName());
                bean.setUnit(mtrArchives.getMeasureUnit());
                bean.setDeviceName(mtrArchives.getDeviceName());
                bean.setDeviceCode(mtrArchives.getDeviceCode());
                bean.setFeature(mtrArchives.getFeature());
                bean.setCreateId(SecurityUtils.getUserId());
                bean.setIsMain("Y");
                bean.setState(3L);
                bean.setOrganCode(type.getOrganCode());
                bean.setHostId(mtrArchives.getId());
                bean.setCreateBy(SecurityUtils.getUsername());
                bean.setCreateTime(DateUtils.getNowDate());
                bean.setDelFlag(mtrArchives.getFeature());
                addList.add(bean);
            }
        }
        if (!addList.isEmpty()) {
            this.saveBatch(addList);
        }
    }


    @Override
    public void updateByHostId(DeviceArchives mtrArchives) {
        DeviceDetails materialDetails = new DeviceDetails();
        materialDetails.setHostId(mtrArchives.getId());
        materialDetails.setDelFlag("0");
        List<DeviceDetails> materialDetails1 = baseMapper.selectDeviceDetailsList(materialDetails);
        if (materialDetails1 != null && !materialDetails1.isEmpty()) {
            materialDetails1.forEach(bean -> {
                bean.setUnit(mtrArchives.getMeasureUnit());
                bean.setDeviceName(mtrArchives.getDeviceName());
                bean.setDeviceCode(mtrArchives.getDeviceCode());
                bean.setFeature(mtrArchives.getFeature());
            });
        }
        this.updateBatchById(materialDetails1);
    }

    @Override
    public void deleteByHostId(String[] histIds, Map<Long, Long> idsMap) {
        if (histIds != null) {
            //查询新增至主库数据和主库同步数据
            QueryWrapper<DeviceDetails> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("del_flag", "0");
            queryWrapper.in("host_id", Arrays.asList(histIds));
            List<DeviceDetails> eigenvalues = this.list(queryWrapper);
            //查询新增至主库时不同级或者编码重复的数据
            QueryWrapper<DeviceDetails> qw = new QueryWrapper<>();
            qw.eq("del_flag", "0");
            qw.in("main_id", Arrays.asList(histIds));
            qw.eq("is_main","Y");
            eigenvalues.addAll(this.list(qw));
            List<DeviceDetails> newDeviceDetails = new ArrayList<>();
            List<DeviceDetails> upDeviceDetails = new ArrayList<>();
            if (idsMap != null && !idsMap.isEmpty()) {
                eigenvalues.forEach(item -> {
                    if (idsMap.containsKey(item.getId())) {
                        item.setHostId("0");
                        item.setIsMain("N");
                        item.setMainId("0");
                        upDeviceDetails.add(item);
                    } else {
                        newDeviceDetails.add(item);
                    }
                });
            }
            this.updateBatchById(upDeviceDetails);
            List<Long> collect = newDeviceDetails.stream().map(DeviceDetails::getId).collect(Collectors.toList());
            this.removeBatchByIds(collect);
        }
    }


    /**
     * 新增至主库
     *
     * @param materialDetails
     * @return
     */
    @Override
    public synchronized int addToMain(DeviceDetails materialDetails) {
        if (materialDetails.getId() == null) {
            throw new BusinessException("请选择要新增至主库的数据");
        }
        if (materialDetails.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        DeviceDetails materialType1 = this.selectDeviceDetailsById(materialDetails.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法再次新增");
        }
        DeviceClass mtrClass = iDeviceClassService.selectDeviceClassById(materialDetails.getHostId());
        //查询编码是否已在主库存在
        DeviceArchives mtrClass1 = new DeviceArchives();
        mtrClass1.setDeviceClassId(materialDetails.getHostId());
        mtrClass1.setValid(0L);
        mtrClass1.setDeviceCode(materialType1.getDeviceCode());
        if (!StringUtils.isEmpty(materialDetails.getDeviceCode())) {
            mtrClass1.setDeviceCode(materialDetails.getDeviceCode());
        }
        List<DeviceArchives> mtrClasses = iDeviceArchivesService.selectDeviceArchivesList(mtrClass1);
        String key = KeyUtils.generateId() + "";
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            return -1;
        } else {
            if (!StringUtils.isEmpty(materialDetails.getDeviceCode())) {
                DeviceArchives aClass = new DeviceArchives();
                aClass.setId(key);
                aClass.setDeviceClassId(mtrClass.getId());
                aClass.setDeviceCode(materialDetails.getDeviceCode());
                aClass.setDeviceName(materialType1.getDeviceName());
                aClass.setFeature(materialType1.getFeature());
                aClass.setMeasureUnit(materialType1.getUnit());
                aClass.setSonId(materialType1.getId());
                aClass.setCreateBy(SecurityUtils.getUsername());
                aClass.setCreateId(SecurityUtils.getUserId() + "");
                aClass.setCreateTime(DateUtils.getNowDate());
                iDeviceArchivesService.insertDeviceArchives(aClass);
                materialType1.setMainId(aClass.getId());
                materialType1.setIsMain("Y");
                return baseMapper.updateDeviceDetails(materialType1);
            } else {
                //不存在时 新增至主库
                materialType1.setIsMain("Y");
                materialType1.setHostId(key);
                int i = baseMapper.updateDeviceDetails(materialType1);
                if (i > 0) {
                    DeviceArchives aClass = new DeviceArchives();
                    aClass.setId(key);
                    aClass.setDeviceClassId(mtrClass.getId());
                    aClass.setDeviceCode(materialType1.getDeviceCode());
                    aClass.setDeviceName(materialType1.getDeviceName());
                    aClass.setFeature(materialType1.getFeature());
                    aClass.setMeasureUnit(materialType1.getUnit());
                    aClass.setSonId(materialType1.getId());
                    aClass.setCreateBy(SecurityUtils.getUsername());
                    aClass.setCreateId(SecurityUtils.getUserId() + "");
                    aClass.setCreateTime(DateUtils.getNowDate());
                    iDeviceArchivesService.insertDeviceArchives(aClass);
                }
                return i;
            }
        }
    }


    /**
     * 关联至主库
     *
     * @param materialDetails
     * @return
     */
    @Override
    public synchronized int associationToMain(DeviceDetails materialDetails) {
        if (materialDetails.getId() == null) {
            throw new BusinessException("请选择要关联至主库的数据");
        }
        if (materialDetails.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        DeviceDetails materialType1 = this.selectDeviceDetailsById(materialDetails.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法进行关联");
        }
        if (!"0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据已关联至主库，无法再次关联");
        }
        materialType1.setMainId(materialDetails.getHostId());
        return baseMapper.updateDeviceDetails(materialType1);
    }


    /**
     * 取消关联至主库
     *
     * @param materialDetails
     * @return
     */
    @Override
    public synchronized int unAssociationToMain(DeviceDetails materialDetails) {
        if (materialDetails.getId() == null) {
            throw new BusinessException("请选择要取消关联至主库的数据");
        }
        DeviceDetails materialType1 = this.selectDeviceDetailsById(materialDetails.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据为主库同步数据，无法进行操作");
        }
        if ("0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据尚未关联至主库，无需取消关联");
        }
        materialType1.setMainId("0");
        return baseMapper.updateDeviceDetails(materialType1);
    }

    @Override
    public long selectDeviceDetailsListCount(DeviceDetails deviceDetails) {
        return baseMapper.selectDeviceDetailsListCount(deviceDetails);
    }

    @Override
    public List<DeviceDetails> getProcessed(String organCode) {
        return baseMapper.getProcessed(organCode);
    }


}
