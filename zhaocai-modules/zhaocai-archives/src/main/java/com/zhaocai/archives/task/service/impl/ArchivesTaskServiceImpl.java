package com.zhaocai.archives.task.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zhaocai.archives.dossier.domain.*;
import com.zhaocai.archives.dossier.service.*;
import com.zhaocai.archives.main.domain.*;
import com.zhaocai.archives.main.service.*;
import com.zhaocai.archives.task.domain.TbMaterialInterfaceLog;
import com.zhaocai.archives.task.service.ArchivesTaskService;
import com.zhaocai.archives.task.service.TbMaterialInterfaceLogService;
import com.zhaocai.archives.utils.KeyUtils;
import com.zhaocai.common.core.constant.SecurityConstants;
import com.zhaocai.common.core.utils.SpringUtils;
import com.zhaocai.system.api.domain.SysDept;
import com.zhaocai.system.api.system.RemoteSystemService;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author liaozhiqin
 * @date 2025/1/17
 */
@Service
public class ArchivesTaskServiceImpl implements ArchivesTaskService {

    @Resource
    private IMaterialTypeService materialTypeService;

    @Resource
    private IMaterialItemService materialItemService;

    @Resource
    private IMaterialEigenvalueService iMaterialEigenvalueService;

    @Resource
    private IMaterialDetailsService iMaterialDetailsService;

    @Resource
    private IDeviceTypeService iDeviceTypeService;

    @Resource
    private IDeviceItemService iDeviceItemService;

    @Resource
    private IDeviceEigenvalueService iDeviceEigenvalueService;

    @Resource
    private IDeviceDetailsService iDeviceDetailsService;

    @Resource
    private ILabourTypeService iLabourTypeService;

    @Resource
    private ILabourItemService iLabourItemService;

    @Resource
    private ILabourEigenvalueService iLabourEigenvalueService;

    @Resource
    private ILabourDetailsService iLabourDetailsService;

    @Resource
    private ISubcontractingTypeService iSubcontractingTypeService;

    @Resource
    private ISubcontractingItemService iSubcontractingItemService;

    @Resource
    private ISubcontractingEigenvalueService iSubcontractingEigenvalueService;

    @Resource
    private ISubcontractingDetailsService iSubcontractingDetailsService;

    @Resource
    private RemoteSystemService remoteSystemService;


    @Resource
    private IMtrClassService mtrClassService;

    @Resource
    private IMtrFeatureService mtrFeatureService;

    @Resource
    private IMtrFeatureValueService mtrFeatureValueService;

    @Resource
    private IDeviceFeatureValueService deviceFeatureValueService;

    @Resource
    private IDeviceFeatureService deviceFeatureService;

    @Resource
    private IDeviceClassService deviceClassService;

    /**
     * base地址
     */
    @Value("${underPlat.baseUrl}")
    private String baseUrl;

    /**
     * 授权码
     */
    @Value("${underPlat.authCode}")
    private String authCode;

    /**
     * 中台地址
     */
    @Value("${dataMiddlePlatform.dataUrl}")
    private String dataUrl;

    /**
     * appId
     */
    @Value("${dataMiddlePlatform.tydtcAppId}")
    private String tydtcAppId;

    /**
     * appKey
     */
    @Value("${dataMiddlePlatform.tydtcAppKey}")
    private String tydtcAppKey;

    /**
     * AppSecret
     */
    @Value("${dataMiddlePlatform.tydtcAppSecret}")
    private String tydtcAppSecret;

    /**
     * 物料新增
     */
    @Value("${dataMiddlePlatform.assetAdd}")
    private String assetAdd;
    /**
     * 物料修改
     */
    @Value("${dataMiddlePlatform.assetModify}")
    private String assetModify;
    /**
     * 物料启用
     */
    @Value("${dataMiddlePlatform.assetEnable}")
    private String assetEnable;
    /**
     * 物料禁用
     */
    @Value("${dataMiddlePlatform.assetDisable}")
    private String assetDisable;
    /**
     * 特征项新增
     */
    @Value("${dataMiddlePlatform.featureAdd}")
    private String featureAdd;
    /**
     * 特征项修改
     */
    @Value("${dataMiddlePlatform.featureModify}")
    private String featureModify;
    /**
     * 特征项启用
     */
    @Value("${dataMiddlePlatform.featureEnable}")
    private String featureEnable;
    /**
     * 特征项禁用
     */
    @Value("${dataMiddlePlatform.featureDisable}")
    private String featureDisable;
    /**
     * 特征值新增
     */
    @Value("${dataMiddlePlatform.featureValAdd}")
    private String featureValAdd;
    /**
     * 特征值修改
     */
    @Value("${dataMiddlePlatform.featureValModify}")
    private String featureValModify;
    /**
     * 特征值启用
     */
    @Value("${dataMiddlePlatform.featureValEnable}")
    private String featureValEnable;
    /**
     * 特征值禁用
     */
    @Value("${dataMiddlePlatform.featureValDisable}")
    private String featureValDisable;


    @Override
    public boolean synchronizeMasterData() {
        try {
            SysDept dept = new SysDept();
            dept.setThridOrgLevel(1);
            List<SysDept> sysDepts = remoteSystemService.selectDeptList(dept, SecurityConstants.INNER);
            dept.setThridOrgLevel(2);
            sysDepts.addAll(remoteSystemService.selectDeptList(dept, SecurityConstants.INNER));
            List<String> organCodes = sysDepts.stream().map(SysDept::getThridDeptId).collect(Collectors.toList());
            if (!organCodes.isEmpty()) {
                for (String organCode : organCodes) {
                    synchronizeMaterialData(organCode);
                    synchronizeDeviceData(organCode);
                    synchronizeLabourData(organCode);
                    synchronizeSubcontractingData(organCode);
                }
            }
            return true;
        } catch (Exception e) {
            System.out.println("同步数据失败------" + e.getMessage());
            return false;
        }
    }


    /**
     * 同步材料数据
     *
     * @param organCode
     */
    private void synchronizeMaterialData(String organCode) {
        MaterialType materialType = new MaterialType();
        materialType.setOrganCode(organCode);
        List<MaterialType> list = materialTypeService.initData(materialType);
        if (list != null && !list.isEmpty()) {
            MaterialItem item = new MaterialItem();
            item.setOrganCode(organCode);
            List<MaterialItem> materialItems = materialItemService.initData(item);
            if (materialItems != null && !materialItems.isEmpty()) {
                MaterialEigenvalue materialEigenvalue = new MaterialEigenvalue();
                materialEigenvalue.setOrganCode(organCode);
                iMaterialEigenvalueService.initData(materialEigenvalue);
            }
            MaterialDetails details = new MaterialDetails();
            details.setOrganCode(organCode);
            iMaterialDetailsService.initData(details);
        }
    }

    /**
     * 同步设备数据
     *
     * @param organCode
     */
    private void synchronizeDeviceData(String organCode) {
        DeviceType type = new DeviceType();
        type.setOrganCode(organCode);
        List<DeviceType> list = iDeviceTypeService.initData(type);
        if (list != null && !list.isEmpty()) {
            DeviceItem item = new DeviceItem();
            item.setOrganCode(organCode);
            List<DeviceItem> items = iDeviceItemService.initData(item);
            if (items != null && !items.isEmpty()) {
                DeviceEigenvalue eigenvalue = new DeviceEigenvalue();
                eigenvalue.setOrganCode(organCode);
                iDeviceEigenvalueService.initData(eigenvalue);
            }
            DeviceDetails details = new DeviceDetails();
            details.setOrganCode(organCode);
            iDeviceDetailsService.initData(details);
        }
    }

    /**
     * 同步劳务数据
     *
     * @param organCode
     */
    private void synchronizeLabourData(String organCode) {
        LabourType type = new LabourType();
        type.setOrganCode(organCode);
        List<LabourType> list = iLabourTypeService.initData(type);
        if (list != null && !list.isEmpty()) {
            LabourItem item = new LabourItem();
            item.setOrganCode(organCode);
            List<LabourItem> items = iLabourItemService.initData(item);
            if (items != null && !items.isEmpty()) {
                LabourEigenvalue eigenvalue = new LabourEigenvalue();
                eigenvalue.setOrganCode(organCode);
                iLabourEigenvalueService.initData(eigenvalue);
            }
            LabourDetails details = new LabourDetails();
            details.setOrganCode(organCode);
            iLabourDetailsService.initData(details);
        }
    }

    /**
     * 同步分包数据
     *
     * @param organCode
     */
    private void synchronizeSubcontractingData(String organCode) {
        SubcontractingType type = new SubcontractingType();
        type.setOrganCode(organCode);
        List<SubcontractingType> list = iSubcontractingTypeService.initData(type);
        if (list != null && !list.isEmpty()) {
            SubcontractingItem item = new SubcontractingItem();
            item.setOrganCode(organCode);
            List<SubcontractingItem> items = iSubcontractingItemService.initData(item);
            if (items != null && !items.isEmpty()) {
                SubcontractingEigenvalue eigenvalue = new SubcontractingEigenvalue();
                eigenvalue.setOrganCode(organCode);
                iSubcontractingEigenvalueService.initData(eigenvalue);
            }
            SubcontractingDetails details = new SubcontractingDetails();
            details.setOrganCode(organCode);
            iSubcontractingDetailsService.initData(details);
        }
    }


    /**
     * 推送数据到中台
     *
     * @return
     */
    @Override
    public boolean pushMiddlePlatform() {
        try {
            pushMtrClass();
            pushDeviceClass();
            pushMtrFeature();
            pushDeviceFeature();
            pushMtrFeatureValue();
            pushDeviceFeatureValue();
            return true;
        } catch (Exception e) {
            System.out.println("推送数据到中台失败------" + e.getMessage());
            return false;
        }
    }


    /**
     * 推送设备特征值
     *
     * @return
     */
    public boolean pushDeviceFeatureValue() {
        List<DeviceClass> mtrClasses2 = deviceClassService.selectDeviceClassList(null);
        List<DeviceFeature> mtrClasses1 = deviceFeatureService.selectDeviceFeatureList(null);
        List<DeviceFeatureValue> upMtrClasses = new ArrayList<>();
        QueryWrapper<DeviceFeatureValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("is_tb", MtrClass.YTS);
        queryWrapper.orderByAsc("feature_value_code");
        List<DeviceFeatureValue> mtrClasses = deviceFeatureValueService.list(queryWrapper);
        Map<String, DeviceFeature> mtrMap = new HashMap();
        mtrClasses1.forEach(mtrClass -> {
            mtrMap.put(mtrClass.getId(), mtrClass);
        });
        Map<String, DeviceClass> mtrMap2 = new HashMap();
        mtrClasses2.forEach(mtrClass -> {
            mtrMap2.put(mtrClass.getId(), mtrClass);
        });
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        List<DeviceClass> mtrClasses1 = deviceClassService.selectDeviceClassList(null);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            for (DeviceFeatureValue mtrClass : mtrClasses) {
//            for (int i = 0; i < 1; i++) {
//                MtrClass mtrClass = mtrClasses.get(i);
                //TODO 推送数据到中台
                Map<String, Object> map = new HashMap();
                map.put("internal_id", mtrClass.getId());
                map.put("internal_ref_id", mtrClass.getDeviceFeatureId());
//                map.put("internal_parent_id", mtrClass.getParentId());
                map.put("feature_val_code", mtrClass.getFeatureValueCode());
                map.put("feature_val_name", mtrClass.getFeatureValueName());
                map.put("feature_id", mtrClass.getDeviceFeatureId());
                DeviceFeature feature = mtrMap.get(mtrClass.getDeviceFeatureId());
                if (feature != null) {
                    map.put("feature_name", feature.getFeatureName());
                    map.put("feature_code", feature.getFeatureCode());
                    DeviceClass aClass = mtrMap2.get(feature.getDeviceClassId());
                    if (aClass != null) {
                        map.put("mtr_class_name", aClass.getDeviceClassName());
                        map.put("mtr_class_id", aClass.getId());
                        map.put("mtr_class_code", aClass.getDeviceClassCode());
                    }
                }
//                map.put("ext1",);
                //TODO 测试 -999
                map.put("report_status", "-999");
                map.put("report_time", dateFormat.format(new Date()));
                Map<String, Object> mapMap = new HashMap<>();
                String pjUrl = "";
                String logType = "";
                if (MtrClass.WTS.equals(mtrClass.getIsTb())) {
                    pjUrl = featureValAdd;
                    logType = "sb_add";
                    mapMap.put("data", map);
                } else if (MtrClass.XGTS.equals(mtrClass.getIsTb())) {
                    pjUrl = featureValModify;
                    logType = "sb_edit";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                    mapMap.put("values", map);
                } else if (MtrClass.SCTS.equals(mtrClass.getIsTb())) {
                    pjUrl = featureValDisable;
                    logType = "sb_disable";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                } else if (MtrClass.QYTS.equals(mtrClass.getIsTb())) {
                    pjUrl = featureValEnable;
                    logType = "sb_enable";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                }
                JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(mapMap));
                boolean b = this.sendToDataCenter(jsonObject, pjUrl, logType);
                if (b) {
                    mtrClass.setIsTb(MtrClass.YTS);
                    upMtrClasses.add(mtrClass);
                }
            }
        }
        if (!upMtrClasses.isEmpty()) {
            deviceFeatureValueService.updateBatchById(upMtrClasses);
        }
        return true;
    }

    /**
     * 推送材料特征值
     *
     * @return
     */
    public boolean pushMtrFeatureValue() {
        List<MtrClass> mtrClasses2 = mtrClassService.selectMtrClassListNoChange(null);
        List<MtrFeature> mtrClasses1 = mtrFeatureService.selectMtrFeatureList(null);
        List<MtrFeatureValue> upMtrClasses = new ArrayList<>();
        QueryWrapper<MtrFeatureValue> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("is_tb", MtrClass.YTS);
        queryWrapper.orderByAsc("feature_value_code");
        List<MtrFeatureValue> mtrClasses = mtrFeatureValueService.list(queryWrapper);
        Map<String, MtrFeature> mtrMap = new HashMap();
        mtrClasses1.forEach(mtrClass -> {
            mtrMap.put(mtrClass.getId(), mtrClass);
        });
        Map<String, MtrClass> mtrMap2 = new HashMap();
        mtrClasses2.forEach(mtrClass -> {
            mtrMap2.put(mtrClass.getId(), mtrClass);
        });
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        List<DeviceClass> mtrClasses1 = deviceClassService.selectDeviceClassList(null);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            for (MtrFeatureValue mtrClass : mtrClasses) {
//            for (int i = 0; i < 1; i++) {
//                MtrClass mtrClass = mtrClasses.get(i);
                //TODO 推送数据到中台
                Map<String, Object> map = new HashMap();
                map.put("internal_id", mtrClass.getId());
                map.put("internal_ref_id", mtrClass.getMtrFeatureId());
//                map.put("internal_parent_id", mtrClass.getParentId());
                map.put("feature_val_code", mtrClass.getFeatureValueCode());
                map.put("feature_val_name", mtrClass.getFeatureValueName());
                map.put("feature_id", mtrClass.getMtrFeatureId());
                MtrFeature feature = mtrMap.get(mtrClass.getMtrFeatureId()) == null ? null : mtrMap.get(mtrClass.getMtrFeatureId());
                if (feature != null) {
                    map.put("feature_name", feature.getFeatureName());
                    map.put("feature_code", feature.getFeatureCode());
                    MtrClass aClass = mtrMap2.get(feature.getMtrClassId());
                    if (aClass != null) {
                        map.put("mtr_class_name", aClass.getMtrClassCode());
                        map.put("mtr_class_id", aClass.getId());
                        map.put("mtr_class_code", aClass.getMtrClassCode());
                    }
                }
//                map.put("ext1",);
                //TODO 测试 -999
                map.put("report_status", "-999");
                map.put("report_time", dateFormat.format(new Date()));
                Map<String, Object> mapMap = new HashMap<>();
                String pjUrl = "";
                String logType = "";
                if (MtrClass.WTS.equals(mtrClass.getIsTb())) {
                    pjUrl = featureValAdd;
                    logType = "cl_add";
                    mapMap.put("data", map);
                } else if (MtrClass.XGTS.equals(mtrClass.getIsTb())) {
                    pjUrl = featureValModify;
                    logType = "cl_edit";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                    mapMap.put("values", map);
                } else if (MtrClass.SCTS.equals(mtrClass.getIsTb())) {
                    pjUrl = featureValDisable;
                    logType = "cl_disable";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                } else if (MtrClass.QYTS.equals(mtrClass.getIsTb())) {
                    pjUrl = featureValEnable;
                    logType = "cl_enable";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                }
                JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(mapMap));
                boolean b = this.sendToDataCenter(jsonObject, pjUrl, logType);
                if (b) {
                    mtrClass.setIsTb(MtrClass.YTS);
                    upMtrClasses.add(mtrClass);
                }
            }
        }
        if (!upMtrClasses.isEmpty()) {
            mtrFeatureValueService.updateBatchById(upMtrClasses);
        }
        return true;
    }

    /**
     * 推送材料类型
     *
     * @return
     */
    public boolean pushMtrClass() {
        List<MtrClass> upMtrClasses = new ArrayList<>();
        QueryWrapper<MtrClass> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("is_tb", MtrClass.YTS);
        queryWrapper.orderByAsc("mtr_class_code");
        List<MtrClass> mtrClasses = mtrClassService.list(queryWrapper);
        Map<String, MtrClass> mtrMap = new HashMap();
        mtrClasses.forEach(mtrClass -> {
            mtrMap.put(mtrClass.getId(), mtrClass);
        });
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        List<DeviceClass> mtrClasses1 = deviceClassService.selectDeviceClassList(null);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            for (MtrClass mtrClass : mtrClasses) {
//            for (int i = 0; i < 1; i++) {
//                MtrClass mtrClass = mtrClasses.get(i);
                //TODO 推送数据到中台
                Map<String, Object> map = new HashMap();
                map.put("internal_id", mtrClass.getId());
//                map.put("internal_ref_id", mtrClass.getId());
                map.put("internal_parent_id", mtrClass.getParentId());
                map.put("asset_class_code", mtrClass.getMtrClassCode());
                map.put("asset_class_name", mtrClass.getMtrClassName());
                map.put("asset_class_level", StringUtils.isEmpty(mtrClass.getClassLevel()) ? null : mtrClass.getClassLevel());
                map.put("asset_class_level_cd", StringUtils.isEmpty(mtrClass.getClassLevelCd()) ? null : mtrClass.getClassLevelCd());
                map.put("belg_pre_asset_class_name", mtrMap.get(mtrClass.getParentId()) == null ? null : mtrMap.get(mtrClass.getParentId()).getMtrClassName());
                map.put("belg_pre_asset_class_id", mtrClass.getParentId());
                map.put("belg_pre_asset_class_code", mtrMap.get(mtrClass.getParentId()) == null ? null : mtrMap.get(mtrClass.getParentId()).getMtrClassCode());
                map.put("is_subject_matter", mtrClass.getSubjectMatter());
                map.put("is_subject_matter_cd", mtrClass.getSubjectMatter() == 1 ? "true" : "false");
//                map.put("source_remark", mtrClass.getSubjectMatter() == 1 ? "true" : "false");
//                map.put("standard_tax_rate", "");
//                map.put("remark", "");
                map.put("measure_unit", mtrClass.getMeasureUnit());
//                map.put("map_asset_class_code",);
//                map.put("map_asset_class_name",);
//                map.put("ext1",);
                //TODO 测试 -999
                map.put("report_status", "-999");
//                map.put("report_status",);
                map.put("report_time", dateFormat.format(new Date()));
                Map<String, Object> mapMap = new HashMap<>();

                String pjUrl = "";
                String logType = "";
                if (MtrClass.WTS.equals(mtrClass.getIsTb())) {
                    pjUrl = assetAdd;
                    logType = "cl_add";
                    mapMap.put("data", map);
                } else if (MtrClass.XGTS.equals(mtrClass.getIsTb())) {
                    pjUrl = assetModify;
                    logType = "cl_edit";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                    mapMap.put("values", map);
                } else if (MtrClass.SCTS.equals(mtrClass.getIsTb())) {
                    pjUrl = assetDisable;
                    logType = "cl_disable";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                } else if (MtrClass.QYTS.equals(mtrClass.getIsTb())) {
                    pjUrl = assetEnable;
                    logType = "cl_enable";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                }
                JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(mapMap));
                boolean b = this.sendToDataCenter(jsonObject, pjUrl, logType);
                if (b) {
                    mtrClass.setIsTb(MtrClass.YTS);
                    upMtrClasses.add(mtrClass);
                }
            }
        }
        if (!upMtrClasses.isEmpty()) {
            mtrClassService.updateBatchById(upMtrClasses);
        }
        return true;
    }

    /**
     * 推送材料特征项
     *
     * @return
     */
    public boolean pushMtrFeature() {
        List<MtrClass> mtrClasses1 = mtrClassService.selectMtrClassListNoChange(null);
        List<MtrFeature> upMtrClasses = new ArrayList<>();
        QueryWrapper<MtrFeature> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("is_tb", MtrClass.YTS);
        queryWrapper.orderByAsc("feature_code");
        List<MtrFeature> mtrClasses = mtrFeatureService.list(queryWrapper);
        Map<String, MtrClass> mtrMap = new HashMap();
        mtrClasses1.forEach(mtrClass -> {
            mtrMap.put(mtrClass.getId(), mtrClass);
        });
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        List<DeviceClass> mtrClasses1 = deviceClassService.selectDeviceClassList(null);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            for (MtrFeature mtrClass : mtrClasses) {
//            for (int i = 0; i < 1; i++) {
//                MtrClass mtrClass = mtrClasses.get(i);
                Map<String, Object> map = new HashMap();
                map.put("internal_id", mtrClass.getId());
                map.put("internal_ref_id", mtrClass.getMtrClassId());
//                map.put("internal_parent_id", mtrClass.getParentId());
                map.put("feature_code", mtrClass.getFeatureCode());
                map.put("feature_name", mtrClass.getFeatureName());
                map.put("mtr_class_name", mtrMap.get(mtrClass.getMtrClassId()) == null ? null : mtrMap.get(mtrClass.getMtrClassId()).getMtrClassName());
                map.put("mtr_class_id", mtrClass.getMtrClassId());
                map.put("mtr_class_code", mtrMap.get(mtrClass.getMtrClassId()) == null ? null : mtrMap.get(mtrClass.getMtrClassId()).getMtrClassCode());
//                map.put("ext1",);
                //TODO 测试 -999
                map.put("report_status", "-999");
                map.put("report_time", dateFormat.format(new Date()));
                Map<String, Object> mapMap = new HashMap<>();

                String pjUrl = "";
                String logType = "";
                if (MtrClass.WTS.equals(mtrClass.getIsTb())) {
                    pjUrl = featureAdd;
                    logType = "cl_add";
                    mapMap.put("data", map);
                } else if (MtrClass.XGTS.equals(mtrClass.getIsTb())) {
                    pjUrl = featureModify;
                    logType = "cl_edit";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                    mapMap.put("values", map);
                } else if (MtrClass.SCTS.equals(mtrClass.getIsTb())) {
                    pjUrl = featureDisable;
                    logType = "cl_disable";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                } else if (MtrClass.QYTS.equals(mtrClass.getIsTb())) {
                    pjUrl = featureEnable;
                    logType = "cl_enable";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                }
                JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(mapMap));
                boolean b = this.sendToDataCenter(jsonObject, pjUrl, logType);
                if (b) {
                    mtrClass.setIsTb(MtrClass.YTS);
                    upMtrClasses.add(mtrClass);
                }
            }
        }
        if (!upMtrClasses.isEmpty()) {
            mtrFeatureService.updateBatchById(upMtrClasses);
        }
        return true;
    }


    /**
     * 推送设备特征项
     *
     * @return
     */
    public boolean pushDeviceFeature() {
        List<DeviceClass> mtrClasses1 = deviceClassService.selectDeviceClassList(null);
        List<DeviceFeature> upMtrClasses = new ArrayList<>();
        QueryWrapper<DeviceFeature> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("is_tb", MtrClass.YTS);
        queryWrapper.orderByAsc("feature_code");
        List<DeviceFeature> mtrClasses = deviceFeatureService.list(queryWrapper);
        Map<String, DeviceClass> mtrMap = new HashMap();
        mtrClasses1.forEach(mtrClass -> {
            mtrMap.put(mtrClass.getId(), mtrClass);
        });
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
//        List<DeviceClass> mtrClasses1 = deviceClassService.selectDeviceClassList(null);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            for (DeviceFeature mtrClass : mtrClasses) {
//            for (int i = 0; i < 1; i++) {
//                MtrClass mtrClass = mtrClasses.get(i);
                Map<String, Object> map = new HashMap();
                map.put("internal_id", mtrClass.getId());
                map.put("internal_ref_id", mtrClass.getDeviceClassId());
//                map.put("internal_parent_id", mtrClass.getParentId());
                map.put("feature_code", mtrClass.getFeatureCode());
                map.put("feature_name", mtrClass.getFeatureName());
                map.put("mtr_class_name", mtrMap.get(mtrClass.getDeviceClassId()) == null ? null : mtrMap.get(mtrClass.getDeviceClassId()).getDeviceClassName());
                map.put("mtr_class_id", mtrClass.getDeviceClassId());
                map.put("mtr_class_code", mtrMap.get(mtrClass.getDeviceClassId()) == null ? null : mtrMap.get(mtrClass.getDeviceClassId()).getDeviceClassCode());
//                map.put("ext1",);
                //TODO 测试 -999
                map.put("report_status", "-999");
                map.put("report_time", dateFormat.format(new Date()));
                Map<String, Object> mapMap = new HashMap<>();

                String pjUrl = "";
                String logType = "";
                if (MtrClass.WTS.equals(mtrClass.getIsTb())) {
                    pjUrl = featureAdd;
                    logType = "sb_add";
                    mapMap.put("data", map);
                } else if (MtrClass.XGTS.equals(mtrClass.getIsTb())) {
                    pjUrl = featureModify;
                    logType = "sb_edit";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                    mapMap.put("values", map);
                } else if (MtrClass.SCTS.equals(mtrClass.getIsTb())) {
                    pjUrl = featureDisable;
                    logType = "sb_disable";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                } else if (MtrClass.QYTS.equals(mtrClass.getIsTb())) {
                    pjUrl = featureEnable;
                    logType = "sb_enable";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                }
                JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(mapMap));
                boolean b = this.sendToDataCenter(jsonObject, pjUrl, logType);
                if (b) {
                    mtrClass.setIsTb(MtrClass.YTS);
                    upMtrClasses.add(mtrClass);
                }
            }
        }
        if (!upMtrClasses.isEmpty()) {
            deviceFeatureService.updateBatchById(upMtrClasses);
        }
        return true;
    }


    /**
     * 推送设备类型
     *
     * @return
     */
    public boolean pushDeviceClass() {
        List<DeviceClass> upMtrClasses = new ArrayList<>();
        QueryWrapper<DeviceClass> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne("is_tb", MtrClass.YTS);
        queryWrapper.orderByAsc("device_class_code");
        List<DeviceClass> mtrClasses = deviceClassService.list(queryWrapper);
        Map<String, DeviceClass> mtrMap = new HashMap();
        mtrClasses.forEach(mtrClass -> {
            mtrMap.put(mtrClass.getId(), mtrClass);
        });
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            for (DeviceClass mtrClass : mtrClasses) {
//            for (int i = 0; i < 1; i++) {
//                DeviceClass mtrClass = mtrClasses.get(i);
                Map<String, Object> map = new HashMap();
                map.put("internal_id", mtrClass.getId());
//                map.put("internal_ref_id", mtrClass.getId());
                map.put("internal_parent_id", mtrClass.getParentId());
                map.put("asset_class_code", mtrClass.getDeviceClassCode());
                map.put("asset_class_name", mtrClass.getDeviceClassName());
                map.put("asset_class_level", StringUtils.isEmpty(mtrClass.getClassLevel()) ? null : mtrClass.getClassLevel());
                map.put("asset_class_level_cd", StringUtils.isEmpty(mtrClass.getClassLevelCd()) ? null : mtrClass.getClassLevelCd());
                map.put("belg_pre_asset_class_name", mtrMap.get(mtrClass.getParentId()) == null ? null : mtrMap.get(mtrClass.getParentId()).getDeviceClassName());
                map.put("belg_pre_asset_class_id", mtrClass.getParentId());
                map.put("belg_pre_asset_class_code", mtrMap.get(mtrClass.getParentId()) == null ? null : mtrMap.get(mtrClass.getParentId()).getDeviceClassCode());
                map.put("is_subject_matter", mtrClass.getSubjectMatter());
                map.put("is_subject_matter_cd", mtrClass.getSubjectMatter() == 1 ? "true" : "false");
//                map.put("source_remark", mtrClass.getSubjectMatter() == 1 ? "true" : "false");
//                map.put("standard_tax_rate", "");
//                map.put("remark", "");
                map.put("measure_unit", mtrClass.getMeasureUnit());
                map.put("map_asset_class_code", StringUtils.isEmpty(mtrClass.getSubjectMatterCode()) ? null : mtrClass.getSubjectMatterCode());
                map.put("map_asset_class_name", StringUtils.isEmpty(mtrClass.getSubjectMatterName()) ? null : mtrClass.getSubjectMatterName());
//                map.put("ext1",);
                //TODO 测试 -999
                map.put("report_status", "-999");
//                map.put("report_status",);
                map.put("report_time", dateFormat.format(new Date()));
                Map<String, Object> mapMap = new HashMap<>();

                String pjUrl = "";
                String logType = "";
                if (MtrClass.WTS.equals(mtrClass.getIsTb())) {
                    pjUrl = assetAdd;
                    logType = "sb_add";
                    mapMap.put("data", map);
                } else if (MtrClass.XGTS.equals(mtrClass.getIsTb())) {
                    pjUrl = assetModify;
                    logType = "sb_edit";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                    mapMap.put("values", map);
                } else if (MtrClass.SCTS.equals(mtrClass.getIsTb())) {
                    pjUrl = assetDisable;
                    logType = "sb_disable";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                } else if (MtrClass.QYTS.equals(mtrClass.getIsTb())) {
                    pjUrl = assetEnable;
                    logType = "sb_enable";
                    Map<String, Object> map1 = new HashMap<>();
                    map1.put("internal_id", "=");
                    mapMap.put("conditions", map1);
                    Map<String, Object> map2 = new HashMap<>();
                    map2.put("internal_id", mtrClass.getId());
                    mapMap.put("conditionValues", map2);
                }
                JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(mapMap));
                boolean b = this.sendToDataCenter(jsonObject, pjUrl, logType);
                if (b) {
                    mtrClass.setIsTb(MtrClass.YTS);
                    upMtrClasses.add(mtrClass);
                }
            }
        }
        if (!upMtrClasses.isEmpty()) {
            deviceClassService.updateBatchById(upMtrClasses);
        }
        return true;
    }


    private boolean sendToDataCenter(JSONObject dataObject, String pjUrl, String logType) {
        boolean flag = false;
        JSONObject data = new JSONObject();
        Long start = System.currentTimeMillis();
        //发送的数据
        String sendData = dataObject.toJSONString();

        //加密数据格式tyStamp+sendData+tydtcAppSecret md5加密
        String tySign = DigestUtils.md5Hex(start + sendData + tydtcAppSecret);

        //地址
//        String url = baseUrl + "/ckzt" +pjUrl+ "?tyStamp=" + start + "&tySign=" + tySign;
        String url = baseUrl + "/ckzt" + pjUrl + "?tyStamp=" + start + "&tySign=" + tySign;
        String receive = "";
        Long end = System.currentTimeMillis();
        try {
            //获取token
            String token = getToken();
            if (StringUtils.isNotEmpty(token)) {
                HttpResponse response = HttpRequest.post(url)
                        .header("TYDTC_APP_TOKEN", token)
                        .header("Content-Type", "application/json;charset=UTF-8")
                        .body(sendData)
                        .timeout(TIME_OUT)
                        .execute();
                end = System.currentTimeMillis();
                if (response != null && response.isOk()) {
                    receive = response.body();
                    if (StringUtils.isNotEmpty(receive)) {
                        JSONObject object = JSONObject.parseObject(receive);
                        if (object != null && object.containsKey("code") && object.getInteger("code") == 200) {
                            //成功的
                            flag = true;
                            data = object;
                        } else {
                            flag = false;
                            data = object;
                        }
                    }
                } else {
                    receive = String.valueOf(response);
                }
            } else {
                receive = "获取token失败!";
                flag = false;
            }
        } catch (Exception e) {
            receive = e.getMessage();
            flag = false;
        }

        try {
            //保存访问的日志
            saveLog(start, url, sendData, receive, end, flag, logType,pjUrl);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (receive.contains("Read timed out")) {
            data = new JSONObject(Integer.parseInt("-10000"));
        }
//        System.out.println("返回结果:" + data);
        return flag;
    }

    /**
     * 超时时间
     */
    private static final Integer TIME_OUT = 30000;

    public String getToken() {
        String result = HttpRequest.get(dataUrl + "/token")
                .header("TYDTC_APP_ID", tydtcAppId)
                .header("TYDTC_APP_KEY", tydtcAppKey)
                .timeout(TIME_OUT)
                .execute()
                .body();
        if (StringUtils.isNotEmpty(result)) {
            JSONObject object = JSONObject.parseObject(result);
            if (object != null && object.containsKey("code") && object.getInteger("code") == 200) {
                if (object.containsKey("data")) {
                    return object.getString("data");
                }
            }
        }
        return null;
    }

    private void saveLog(Long start, String url, String sendData, String receive, Long end, boolean flag, String logType,String pjUrl) {
        TbMaterialInterfaceLog log = new TbMaterialInterfaceLog();
        log.setId(KeyUtils.generateId());
        log.setSendTime(new Date(start));
        log.setSendData(sendData);
        log.setSendUrl(url);
        log.setReceiveData(receive);
        log.setReceiveTime(new Date(end));
        log.setLogType(logType);
        log.setFlag(flag + "");
        //设置业务id
        JSONObject jsonObject = JSONObject.parseObject(sendData);
        String businessId;
        if (sendData.contains("data")) {
            businessId = jsonObject.getJSONObject("data").getString("internal_id");
        } else if (sendData.contains("values")) {
            businessId = jsonObject.getJSONObject("values").getString("internal_id");
        } else {
            businessId = jsonObject.getJSONObject("conditionValues").getString("internal_id");
        }
        log.setBusinessId(businessId);
        log.setRemark(pjUrl);
        SpringUtils.getBean(TbMaterialInterfaceLogService.class).insertTInterfaceLog(log);
    }


}
