package com.zhaocai.archives.api.controller;

import com.zhaocai.archives.api.domain.*;
import com.zhaocai.archives.dossier.domain.*;
import com.zhaocai.archives.dossier.service.*;
import com.zhaocai.archives.main.domain.*;
import com.zhaocai.archives.main.service.*;
import com.zhaocai.common.core.web.controller.BaseController;
import com.zhaocai.common.core.web.page.TableDataInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

/**
 * 远程调用接口
 *
 * @author lzq
 * @date 2025-01-24
 */
@RestController
@RequestMapping("/api/material")
public class MaterialController extends BaseController {
    @Resource
    private IDeviceTypeService deviceTypeService;

    @Resource
    private IMaterialTypeService materialTypeService;

    @Resource
    private ILabourTypeService labourTypeService;

    @Resource
    private ISubcontractingTypeService subcontractingTypeService;

    @Resource
    private IMtrClassService mtrClassService;

    @Resource
    private IMajorSubcontractingClassService majorSubcontractingClassService;


    @Resource
    private ILaborServicesClassService laborServicesClassService;

    @Resource
    private IDeviceClassService deviceClassService;

    @Resource
    private IDeviceDetailsService deviceDetailsService;

    @Resource
    private ILabourDetailsService labourDetailsService;

    @Resource
    private IMaterialDetailsService materialDetailsService;

    @Resource
    private ISubcontractingDetailsService subcontractingDetailsService;
    @Resource
    private IDeviceArchivesService deviceArchivesService;

    @Resource
    private ILaborServicesArchivesService laborServicesArchivesService;

    @Resource
    private IMajorSubcontractingArchivesService majorSubcontractingArchivesService;

    @Resource
    private IMtrArchivesService mtrArchivesService;

    @Resource
    private IDeviceItemService deviceItemService;

    @Resource
    private ILabourItemService labourItemService;

    @Resource
    private IMaterialItemService materialItemService;

    @Resource
    private ISubcontractingItemService subcontractingItemService;

    @Resource
    private IDeviceFeatureService deviceFeatureService;
    @Resource
    private ILaborServicesFeatureService laborServicesFeatureService;
    @Resource
    private IMajorSubcontractingFeatureService majorSubcontractingFeatureService;
    @Resource
    private IMtrFeatureService mtrFeatureService;

    @Resource
    private IDeviceEigenvalueService deviceEigenvalueService;
    @Resource
    private ILabourEigenvalueService labourEigenvalueService;
    @Resource
    private IMaterialEigenvalueService materialEigenvalueService;
    @Resource
    private ISubcontractingEigenvalueService subcontractingEigenvalueService;

    @Resource
    private IDeviceFeatureValueService deviceFeatureValueService;

    @Resource
    private ILaborServicesFeatureValueService laborServicesFeatureValueService;

    @Resource
    private IMajorSubcontractingFeatureValueService majorSubcontractingFeatureValueService;

    @Resource
    private IMtrFeatureValueService mtrFeatureValueService;

    /**
     * 查询设备分类列表
     */
    @GetMapping("/deviceTypeList")
    public TableDataInfo deviceTypeList(DeviceType deviceType) {
        startPage();
        List<DeviceType> list = deviceTypeService.selectDeviceTypeList(deviceType);
        List<DeviceClassVo> listVo = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(item -> {
                DeviceClassVo deviceClassVo = new DeviceClassVo();
                deviceClassVo.setId(item.getId() + "");
                deviceClassVo.setDeviceClassCode(item.getDeviceCode());
                deviceClassVo.setDeviceClassName(item.getDeviceName());
                deviceClassVo.setDeviceClassType(item.getDeviceType());
                deviceClassVo.setMeasureUnit(item.getUnit());
                deviceClassVo.setSubjectMatterCode(item.getSubjectMatterCode());
                deviceClassVo.setSubjectMatterName(item.getSubjectMatterName());
                deviceClassVo.setSubjectMatter(Integer.parseInt(item.getIsTransaction()));
                deviceClassVo.setClassLevel(item.getDeviceLevel());
                deviceClassVo.setClassLevelCd(item.getDeviceLevelCd());
                deviceClassVo.setParentId(item.getUpId() + "");
                deviceClassVo.setCreateId(item.getCreateId() + "");
                deviceClassVo.setCreateBy(item.getCreateBy());
                deviceClassVo.setCreateTime(item.getCreateTime());
                deviceClassVo.setUpdateBy(item.getUpdateBy());
                deviceClassVo.setUpdateTime(item.getUpdateTime());
                deviceClassVo.setValid(Long.valueOf(item.getDelFlag()));
                deviceClassVo.setIsMain(item.getIsMain());
                deviceClassVo.setState(item.getState());
                deviceClassVo.setWfProcessId(item.getWfProcessId());
                deviceClassVo.setWfBatch(item.getWfBatch());
                deviceClassVo.setMainId(item.getMainId());
                deviceClassVo.setHostId(item.getHostId());
                deviceClassVo.setOrganCode(item.getOrganCode());
                listVo.add(deviceClassVo);
            });
        }
        TableDataInfo dataTable = getDataTable(listVo);
        dataTable.setTotal(deviceTypeService.selectDeviceTypeListCount(deviceType));
        return dataTable;
    }

    /**
     * 查询材料分类列表
     */
    // @RequiresPermissions("archives:materialType:list")
    @GetMapping("/materialTypeList")
    public TableDataInfo materialTypeList(MaterialType materialType) {
        startPage();
        List<MaterialType> list = materialTypeService.selectMaterialTypeList(materialType);
        List<MtrClassVo> listVo = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(item -> {
                MtrClassVo deviceClassVo = new MtrClassVo();
                deviceClassVo.setId(item.getId() + "");
                deviceClassVo.setMtrClassCode(item.getMaterialCode());
                deviceClassVo.setMtrClassName(item.getMaterialName());
                deviceClassVo.setMtrClassType(item.getMaterialType());
                deviceClassVo.setMeasureUnit(item.getUnit());
                deviceClassVo.setSubjectMatter(Integer.parseInt(item.getIsTransaction()));
                deviceClassVo.setClassLevel(item.getMaterialLevel());
                deviceClassVo.setClassLevelCd(item.getMaterialLevelCd());
                deviceClassVo.setParentId(item.getUpId() + "");
                deviceClassVo.setCreateId(item.getCreateId() + "");
                deviceClassVo.setCreateBy(item.getCreateBy());
                deviceClassVo.setCreateTime(item.getCreateTime());
                deviceClassVo.setUpdateBy(item.getUpdateBy());
                deviceClassVo.setUpdateTime(item.getUpdateTime());
                deviceClassVo.setValid(Long.valueOf(item.getDelFlag()));
                deviceClassVo.setIsMain(item.getIsMain());
                deviceClassVo.setState(item.getState());
                deviceClassVo.setWfProcessId(item.getWfProcessId());
                deviceClassVo.setWfBatch(item.getWfBatch());
                deviceClassVo.setMainId(item.getMainId());
                deviceClassVo.setHostId(item.getHostId());
                deviceClassVo.setOrganCode(item.getOrganCode());
                listVo.add(deviceClassVo);
            });
        }
        TableDataInfo dataTable = getDataTable(listVo);
        dataTable.setTotal(materialTypeService.selectMaterialTypeListCount(materialType));
        return dataTable;
    }

    /**
     * 查询劳务分类列表
     */
    // @RequiresPermissions("archives:labourType:list")
    @GetMapping("/labourTypeList")
    public TableDataInfo labourTypeList(LabourType labourType) {
        startPage();
        List<LabourType> list = labourTypeService.selectLabourTypeList(labourType);
        List<LaborServicesClassVo> listVo = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(item -> {
                LaborServicesClassVo deviceClassVo = new LaborServicesClassVo();
                deviceClassVo.setId(item.getId() + "");
                deviceClassVo.setLaborServicesClassCode(item.getLabourCode());
                deviceClassVo.setLaborServicesClassName(item.getLabourName());
                deviceClassVo.setLaborServicesClassType(item.getLabourType());
                deviceClassVo.setMeasureUnit(item.getUnit());
                deviceClassVo.setSubjectMatter(Integer.parseInt(item.getIsTransaction()));
                deviceClassVo.setParentId(item.getUpId() + "");
                deviceClassVo.setCreateId(item.getCreateId() + "");
                deviceClassVo.setCreateBy(item.getCreateBy());
                deviceClassVo.setCreateTime(item.getCreateTime());
                deviceClassVo.setUpdateBy(item.getUpdateBy());
                deviceClassVo.setUpdateTime(item.getUpdateTime());
                deviceClassVo.setValid(Long.valueOf(item.getDelFlag()));
                deviceClassVo.setIsMain(item.getIsMain());
                deviceClassVo.setState(item.getState());
                deviceClassVo.setWfProcessId(item.getWfProcessId());
                deviceClassVo.setWfBatch(item.getWfBatch());
                deviceClassVo.setMainId(item.getMainId());
                deviceClassVo.setHostId(item.getHostId());
                deviceClassVo.setOrganCode(item.getOrganCode());
                listVo.add(deviceClassVo);
            });
        }
        TableDataInfo dataTable = getDataTable(listVo);
        dataTable.setTotal(labourTypeService.selectLabourTypeListCount(labourType));
        return dataTable;
    }


    /**
     * 查询专业分包分类列表
     */
    // @RequiresPermissions("archives:subcontractingType:list")
    @GetMapping("/subcontractingTypeList")
    public TableDataInfo subcontractingTypeList(SubcontractingType subcontractingType) {
        startPage();
        List<SubcontractingType> list = subcontractingTypeService.selectSubcontractingTypeList(subcontractingType);
        List<MajorSubcontractingClassVo> listVo = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(item -> {
                MajorSubcontractingClassVo deviceClassVo = new MajorSubcontractingClassVo();
                deviceClassVo.setId(item.getId() + "");
                deviceClassVo.setMajorSubcontractingClassCode(item.getSubcontractingCode());
                deviceClassVo.setMajorSubcontractingClassName(item.getSubcontractingName());
                deviceClassVo.setMajorSubcontractingClassType(item.getSubcontractingType());
                deviceClassVo.setMeasureUnit(item.getUnit());
                deviceClassVo.setSubjectMatter(Integer.parseInt(item.getIsTransaction()));
                deviceClassVo.setParentId(item.getUpId() + "");
                deviceClassVo.setCreateId(item.getCreateId() + "");
                deviceClassVo.setCreateBy(item.getCreateBy());
                deviceClassVo.setCreateTime(item.getCreateTime());
                deviceClassVo.setUpdateBy(item.getUpdateBy());
                deviceClassVo.setUpdateTime(item.getUpdateTime());
                deviceClassVo.setValid(Long.valueOf(item.getDelFlag()));
                deviceClassVo.setIsMain(item.getIsMain());
                deviceClassVo.setState(item.getState());
                deviceClassVo.setWfProcessId(item.getWfProcessId());
                deviceClassVo.setWfBatch(item.getWfBatch());
                deviceClassVo.setMainId(item.getMainId());
                deviceClassVo.setHostId(item.getHostId());
                deviceClassVo.setOrganCode(item.getOrganCode());
                listVo.add(deviceClassVo);
            });
        }
        TableDataInfo dataTable = getDataTable(listVo);
        dataTable.setTotal(subcontractingTypeService.selectSubcontractingTypeListCount(subcontractingType));
        return dataTable;
    }


    /**
     * 查询设备分类主列表
     */
    // @RequiresPermissions("archives:deviceClass:list")
    @GetMapping("/deviceClassList")
    public TableDataInfo deviceClassList(DeviceClass deviceClass) {
        startPage();
        List<DeviceClass> list = deviceClassService.selectDeviceClassList(deviceClass);
        TableDataInfo dataTable = getDataTable(list);
        dataTable.setTotal(deviceClassService.selectDeviceClassListCount(deviceClass));
        return dataTable;
    }


    /**
     * 查询劳务分类主列表
     */
    // @RequiresPermissions("archives:laborClass:list")
    @GetMapping("/laborServicesClassList")
    public TableDataInfo laborServicesClassList(LaborServicesClass laborServicesClass) {
        startPage();
        List<LaborServicesClass> list = laborServicesClassService.selectLaborServicesClassList(laborServicesClass);
        TableDataInfo dataTable = getDataTable(list);
        dataTable.setTotal(laborServicesClassService.selectLaborServicesClassListCount(laborServicesClass));
        return dataTable;
    }


    /**
     * 查询专业分包分类主列表
     */
    // @RequiresPermissions("archives:majorClass:list")
    @GetMapping("/majorSubcontractingClassList")
    public TableDataInfo majorSubcontractingClassList(MajorSubcontractingClass majorSubcontractingClass) {
        startPage();
        List<MajorSubcontractingClass> list = majorSubcontractingClassService.selectMajorSubcontractingClassList(majorSubcontractingClass);
        TableDataInfo dataTable = getDataTable(list);
        dataTable.setTotal(majorSubcontractingClassService.selectMajorSubcontractingClassListCount(majorSubcontractingClass));
        return dataTable;
    }


    /**
     * 查询材料分类主列表
     */
    // @RequiresPermissions("archives:mtrClass:list")
    @GetMapping("/mtrClassList")
    public TableDataInfo mtrClassList(MtrClass mtrClass) {
        startPage();
        List<MtrClass> list = mtrClassService.selectMtrClassList(mtrClass);
        TableDataInfo dataTable = getDataTable(list);
        dataTable.setTotal(mtrClassService.selectMtrClassListCount(mtrClass));
        return dataTable;
    }

    /**
     * 查询设备详情列表
     */
    // @RequiresPermissions("archives:deviceDetails:list")
    @GetMapping("/deviceDetailsList")
    public TableDataInfo deviceDetailsList(DeviceDetails deviceDetails) {
        startPage();
        List<DeviceDetails> list = deviceDetailsService.selectDeviceDetailsListNoChange(deviceDetails);
        List<DeviceArchivesVo> voList = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(item -> {
                DeviceArchivesVo deviceArchivesVo = new DeviceArchivesVo();
                deviceArchivesVo.setId(item.getId() + "");
                deviceArchivesVo.setDeviceClassId(item.getTypeId() + "");
                deviceArchivesVo.setDeviceCode(item.getDeviceCode());
                deviceArchivesVo.setDeviceName(item.getDeviceName());
                deviceArchivesVo.setFeature(item.getFeature());
                deviceArchivesVo.setMeasureUnit(item.getUnit());
                deviceArchivesVo.setCreateId(item.getCreateId() + "");
                deviceArchivesVo.setCreateBy(item.getCreateBy());
                deviceArchivesVo.setCreateTime(item.getCreateTime());
                deviceArchivesVo.setUpdateBy(item.getUpdateBy());
                deviceArchivesVo.setUpdateTime(item.getUpdateTime());
                deviceArchivesVo.setValid(Long.valueOf(item.getDelFlag()));
                deviceArchivesVo.setDeptId(item.getDeptId());
                deviceArchivesVo.setIsMain(item.getIsMain());
                deviceArchivesVo.setState(item.getState());
                deviceArchivesVo.setWfProcessId(item.getWfProcessId());
                deviceArchivesVo.setWfBatch(item.getWfBatch());
                deviceArchivesVo.setMainId(item.getMainId());
                deviceArchivesVo.setHostId(item.getHostId());
                deviceArchivesVo.setOrganCode(item.getOrganCode());
                voList.add(deviceArchivesVo);
            });
        }
        TableDataInfo dataTable = getDataTable(voList);
        dataTable.setTotal(deviceDetailsService.selectDeviceDetailsListCount(deviceDetails));
        return dataTable;
    }


    /**
     * 查询劳务详情列表
     */
    // @RequiresPermissions("archives:labourDetails:list")
    @GetMapping("/labourDetailsList")
    public TableDataInfo labourDetailsList(LabourDetails labourDetails) {
        startPage();
        List<LabourDetails> list = labourDetailsService.selectLabourDetailsListNoChange(labourDetails);
        List<LaborServicesArchivesVo> voList = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(item -> {
                LaborServicesArchivesVo deviceArchivesVo = new LaborServicesArchivesVo();
                deviceArchivesVo.setId(item.getId() + "");
                deviceArchivesVo.setLaborServicesClassId(item.getTypeId() + "");
                deviceArchivesVo.setLaborServicesCode(item.getLabourCode());
                deviceArchivesVo.setLaborServicesName(item.getLabourName());
                deviceArchivesVo.setFeature(item.getItemAndEigenvalue());
                deviceArchivesVo.setMeasureUnit(item.getUnit());
                deviceArchivesVo.setCreateId(item.getCreateId() + "");
                deviceArchivesVo.setCreateBy(item.getCreateBy());
                deviceArchivesVo.setCreateTime(item.getCreateTime());
                deviceArchivesVo.setUpdateBy(item.getUpdateBy());
                deviceArchivesVo.setUpdateTime(item.getUpdateTime());
                deviceArchivesVo.setValid(Long.valueOf(item.getDelFlag()));
                deviceArchivesVo.setMetrologicalRules(item.getMeasurementRules());
                deviceArchivesVo.setBasicJob(item.getWorkContent());
                deviceArchivesVo.setDeptId(item.getDeptId());
                deviceArchivesVo.setIsMain(item.getIsMain());
                deviceArchivesVo.setState(item.getState());
                deviceArchivesVo.setWfProcessId(item.getWfProcessId());
                deviceArchivesVo.setWfBatch(item.getWfBatch());
                deviceArchivesVo.setMainId(item.getMainId());
                deviceArchivesVo.setHostId(item.getHostId());
                deviceArchivesVo.setOrganCode(item.getOrganCode());
                voList.add(deviceArchivesVo);
            });
        }
        TableDataInfo dataTable = getDataTable(voList);
        dataTable.setTotal(labourDetailsService.selectLabourDetailsListCount(labourDetails));
        return dataTable;
    }

    /**
     * 查询材料详情列表
     */
    // @RequiresPermissions("archives:materialDetails:list")
    @GetMapping("/materialDetailsList")
    public TableDataInfo materialDetailsList(MaterialDetails materialDetails) {
        startPage();
        List<MaterialDetails> list = materialDetailsService.selectMaterialDetailsListNoChange(materialDetails);
        List<MtrArchivesVo> voList = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(item -> {
                MtrArchivesVo deviceArchivesVo = new MtrArchivesVo();
                deviceArchivesVo.setId(item.getId() + "");
                deviceArchivesVo.setMtrClassId(item.getTypeId() + "");
                deviceArchivesVo.setMtrCode(item.getMaterialCode());
                deviceArchivesVo.setMtrName(item.getMaterialName());
                deviceArchivesVo.setMeasureUnit(item.getUnit());
                deviceArchivesVo.setSpecs(item.getMaterialSpecifications());
                deviceArchivesVo.setCreateId(item.getCreateId() + "");
                deviceArchivesVo.setCreateBy(item.getCreateBy());
                deviceArchivesVo.setCreateTime(item.getCreateTime());
                deviceArchivesVo.setUpdateBy(item.getUpdateBy());
                deviceArchivesVo.setUpdateTime(item.getUpdateTime());
                deviceArchivesVo.setValid(Long.valueOf(item.getDelFlag()));
                deviceArchivesVo.setDeptId(item.getDeptId());
                deviceArchivesVo.setIsMain(item.getIsMain());
                deviceArchivesVo.setState(item.getState());
                deviceArchivesVo.setWfProcessId(item.getWfProcessId());
                deviceArchivesVo.setWfBatch(item.getWfBatch());
                deviceArchivesVo.setMainId(item.getMainId());
                deviceArchivesVo.setHostId(item.getHostId());
                deviceArchivesVo.setOrganCode(item.getOrganCode());
                voList.add(deviceArchivesVo);
            });
        }
        TableDataInfo dataTable = getDataTable(voList);
        dataTable.setTotal(materialDetailsService.selectMaterialDetailsListCount(materialDetails));
        return dataTable;
    }


    /**
     * 查询专业分包详情列表
     */
    // @RequiresPermissions("archives:subcontractingDetails:list")
    @GetMapping("/subcontractingDetailsList")
    public TableDataInfo subcontractingDetailsList(SubcontractingDetails subcontractingDetails) {
        startPage();
        List<SubcontractingDetails> list = subcontractingDetailsService.selectSubcontractingDetailsListNoChange(subcontractingDetails);
        List<MajorSubcontractingArchivesVo> voList = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(item -> {
                MajorSubcontractingArchivesVo deviceArchivesVo = new MajorSubcontractingArchivesVo();
                deviceArchivesVo.setId(item.getId() + "");
                deviceArchivesVo.setMajorSubcontractingClassId(item.getTypeId() + "");
                deviceArchivesVo.setMajorSubcontractingCode(item.getSubcontractingCode());
                deviceArchivesVo.setMajorSubcontractingName(item.getSubcontractingName());
                deviceArchivesVo.setFeature(item.getItemAndEigenvalue());
                deviceArchivesVo.setMeasureUnit(item.getUnit());
                deviceArchivesVo.setMetrologicalRules(item.getMeasurementRules());
                deviceArchivesVo.setBasicJob(item.getWorkContent());
                deviceArchivesVo.setCreateId(item.getCreateId() + "");
                deviceArchivesVo.setCreateBy(item.getCreateBy());
                deviceArchivesVo.setCreateTime(item.getCreateTime());
                deviceArchivesVo.setUpdateBy(item.getUpdateBy());
                deviceArchivesVo.setUpdateTime(item.getUpdateTime());
                deviceArchivesVo.setValid(Long.valueOf(item.getDelFlag()));
                deviceArchivesVo.setDeptId(item.getDeptId());
                deviceArchivesVo.setIsMain(item.getIsMain());
                deviceArchivesVo.setState(item.getState());
                deviceArchivesVo.setWfProcessId(item.getWfProcessId());
                deviceArchivesVo.setWfBatch(item.getWfBatch());
                deviceArchivesVo.setMainId(item.getMainId());
                deviceArchivesVo.setHostId(item.getHostId());
                deviceArchivesVo.setOrganCode(item.getOrganCode());
                voList.add(deviceArchivesVo);
            });
        }
        TableDataInfo dataTable = getDataTable(voList);
        dataTable.setTotal(subcontractingDetailsService.selectSubcontractingDetailsListCount(subcontractingDetails));
        return dataTable;
    }


    /**
     * 查询设备档案主列表
     */
    // @RequiresPermissions("archives:deviceArchives:list")
    @GetMapping("/deviceArchivesList")
    public TableDataInfo deviceArchivesList(DeviceArchives deviceArchives) {
        startPage();
        List<DeviceArchives> list = deviceArchivesService.selectDeviceArchivesList(deviceArchives);
        TableDataInfo dataTable = getDataTable(list);
        dataTable.setTotal(deviceArchivesService.selectDeviceArchivesListCount(deviceArchives));
        return dataTable;
    }


    /**
     * 查询劳务档案主列表
     */
    // @RequiresPermissions("archives:laborArchives:list")
    @GetMapping("/laborServicesArchivesList")
    public TableDataInfo laborServicesArchivesList(LaborServicesArchives laborServicesArchives) {
        startPage();
        List<LaborServicesArchives> list = laborServicesArchivesService.selectLaborServicesArchivesList(laborServicesArchives);
        TableDataInfo dataTable = getDataTable(list);
        dataTable.setTotal(laborServicesArchivesService.selectLaborServicesArchivesListCount(laborServicesArchives));
        return dataTable;
    }


    /**
     * 查询专业分包档案主列表
     */
    // @RequiresPermissions("archives:majorArchives:list")
    @GetMapping("/majorSubcontractingArchivesList")
    public TableDataInfo majorSubcontractingArchivesList(MajorSubcontractingArchives majorSubcontractingArchives) {
        startPage();
        List<MajorSubcontractingArchives> list = majorSubcontractingArchivesService.selectMajorSubcontractingArchivesList(majorSubcontractingArchives);
        TableDataInfo dataTable = getDataTable(list);
        dataTable.setTotal(majorSubcontractingArchivesService.selectMajorSubcontractingArchivesListCount(majorSubcontractingArchives));
        return dataTable;
    }


    /**
     * 查询材料档案主列表
     */
    // @RequiresPermissions("archives:mtrArchives:list")
    @GetMapping("/mtrArchivesList")
    public TableDataInfo mtrArchivesList(MtrArchives mtrArchives) {
        startPage();
        List<MtrArchives> list = mtrArchivesService.selectMtrArchivesList(mtrArchives);
        TableDataInfo dataTable = getDataTable(list);
        dataTable.setTotal(mtrArchivesService.selectMtrArchivesListCount(mtrArchives));
        return dataTable;
    }


    /**
     * 查询设备特征项列表
     */
    // @RequiresPermissions("archives:deviceItem:list")
    @GetMapping("/deviceItemList")
    public TableDataInfo deviceItemList(DeviceItem deviceItem) {
        startPage();
        List<DeviceItem> list = deviceItemService.selectDeviceItemListNoChange(deviceItem);
        List<DeviceFeatureVo> voList = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(item -> {
                DeviceFeatureVo deviceFeatureVo = new DeviceFeatureVo();
                deviceFeatureVo.setId(item.getId() + "");
                deviceFeatureVo.setDeviceClassId(item.getTypeId() + "");
                deviceFeatureVo.setFeatureCode(item.getItemCode());
                deviceFeatureVo.setFeatureName(item.getItemName());
                deviceFeatureVo.setCreateId(item.getCreateId() + "");
                deviceFeatureVo.setCreateBy(item.getCreateBy());
                deviceFeatureVo.setCreateTime(item.getCreateTime());
                deviceFeatureVo.setUpdateBy(item.getUpdateBy());
                deviceFeatureVo.setUpdateTime(item.getUpdateTime());
                deviceFeatureVo.setValid(Long.valueOf(item.getDelFlag()));
                deviceFeatureVo.setIsMain(item.getIsMain());
                deviceFeatureVo.setState(item.getState());
                deviceFeatureVo.setWfProcessId(item.getWfProcessId());
                deviceFeatureVo.setWfBatch(item.getWfBatch());
                deviceFeatureVo.setMainId(item.getMainId());
                deviceFeatureVo.setHostId(item.getHostId());
                deviceFeatureVo.setOrganCode(item.getOrganCode());
                deviceFeatureVo.setDeptId(item.getDeptId());
                voList.add(deviceFeatureVo);
            });
        }
        TableDataInfo dataTable = getDataTable(voList);
        dataTable.setTotal(deviceItemService.selectDeviceItemListCount(deviceItem));
        return dataTable;
    }


    /**
     * 查询劳务特征项列表
     */
    // @RequiresPermissions("archives:labourItem:list")
    @GetMapping("/labourItemList")
    public TableDataInfo labourItemList(LabourItem labourItem) {
        startPage();
        List<LabourItem> list = labourItemService.selectLabourItemListNoChange(labourItem);
        List<LaborServicesFeatureVo> voList = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(item -> {
                LaborServicesFeatureVo deviceFeatureVo = new LaborServicesFeatureVo();
                deviceFeatureVo.setId(item.getId() + "");
                deviceFeatureVo.setLaborServicesClassId(item.getTypeId() + "");
                deviceFeatureVo.setFeatureCode(item.getItemCode());
                deviceFeatureVo.setFeatureName(item.getItemName());
                deviceFeatureVo.setCreateId(item.getCreateId() + "");
                deviceFeatureVo.setCreateBy(item.getCreateBy());
                deviceFeatureVo.setCreateTime(item.getCreateTime());
                deviceFeatureVo.setUpdateBy(item.getUpdateBy());
                deviceFeatureVo.setUpdateTime(item.getUpdateTime());
                deviceFeatureVo.setValid(Long.valueOf(item.getDelFlag()));
                deviceFeatureVo.setIsMain(item.getIsMain());
                deviceFeatureVo.setState(item.getState());
                deviceFeatureVo.setWfProcessId(item.getWfProcessId());
                deviceFeatureVo.setWfBatch(item.getWfBatch());
                deviceFeatureVo.setMainId(item.getMainId());
                deviceFeatureVo.setHostId(item.getHostId());
                deviceFeatureVo.setOrganCode(item.getOrganCode());
                deviceFeatureVo.setDeptId(item.getDeptId());
                voList.add(deviceFeatureVo);
            });
        }
        TableDataInfo dataTable = getDataTable(voList);
        dataTable.setTotal(labourItemService.selectLabourItemListCount(labourItem));
        return dataTable;
    }


    /**
     * 查询材料特征项列表
     */
    // @RequiresPermissions("archives:materialItem:list")
    @GetMapping("/materialItemList")
    public TableDataInfo materialItemList(MaterialItem materialItem) {
        startPage();
        List<MaterialItem> list = materialItemService.selectMaterialItemListNoChange(materialItem);
        List<MtrFeatureVo> voList = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(item -> {
                MtrFeatureVo deviceFeatureVo = new MtrFeatureVo();
                deviceFeatureVo.setId(item.getId() + "");
                deviceFeatureVo.setMtrClassId(item.getTypeId() + "");
                deviceFeatureVo.setFeatureCode(item.getItemCode());
                deviceFeatureVo.setFeatureName(item.getItemName());
                deviceFeatureVo.setCreateId(item.getCreateId() + "");
                deviceFeatureVo.setCreateBy(item.getCreateBy());
                deviceFeatureVo.setCreateTime(item.getCreateTime());
                deviceFeatureVo.setUpdateBy(item.getUpdateBy());
                deviceFeatureVo.setUpdateTime(item.getUpdateTime());
                deviceFeatureVo.setValid(Long.valueOf(item.getDelFlag()));
                deviceFeatureVo.setIsMain(item.getIsMain());
                deviceFeatureVo.setState(item.getState());
                deviceFeatureVo.setWfProcessId(item.getWfProcessId());
                deviceFeatureVo.setWfBatch(item.getWfBatch());
                deviceFeatureVo.setMainId(item.getMainId());
                deviceFeatureVo.setHostId(item.getHostId());
                deviceFeatureVo.setOrganCode(item.getOrganCode());
                deviceFeatureVo.setDeptId(item.getDeptId());
                voList.add(deviceFeatureVo);
            });
        }
        TableDataInfo dataTable = getDataTable(voList);
        dataTable.setTotal(materialItemService.selectMaterialItemListCount(materialItem));
        return dataTable;
    }


    /**
     * 查询专业分包特征项列表
     */
    // @RequiresPermissions("archives:subcontractingItem:list")
    @GetMapping("/subcontractingItemList")
    public TableDataInfo subcontractingItemList(SubcontractingItem subcontractingItem) {
        startPage();
        List<SubcontractingItem> list = subcontractingItemService.selectSubcontractingItemListNoChange(subcontractingItem);
        List<MajorSubcontractingFeatureVo> voList = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(item -> {
                MajorSubcontractingFeatureVo deviceFeatureVo = new MajorSubcontractingFeatureVo();
                deviceFeatureVo.setId(item.getId() + "");
                deviceFeatureVo.setMajorSubcontractingClassId(item.getTypeId() + "");
                deviceFeatureVo.setFeatureCode(item.getItemCode());
                deviceFeatureVo.setFeatureName(item.getItemName());
                deviceFeatureVo.setCreateId(item.getCreateId() + "");
                deviceFeatureVo.setCreateBy(item.getCreateBy());
                deviceFeatureVo.setCreateTime(item.getCreateTime());
                deviceFeatureVo.setUpdateBy(item.getUpdateBy());
                deviceFeatureVo.setUpdateTime(item.getUpdateTime());
                deviceFeatureVo.setValid(Long.valueOf(item.getDelFlag()));
                deviceFeatureVo.setIsMain(item.getIsMain());
                deviceFeatureVo.setState(item.getState());
                deviceFeatureVo.setWfProcessId(item.getWfProcessId());
                deviceFeatureVo.setWfBatch(item.getWfBatch());
                deviceFeatureVo.setMainId(item.getMainId());
                deviceFeatureVo.setHostId(item.getHostId());
                deviceFeatureVo.setOrganCode(item.getOrganCode());
                deviceFeatureVo.setDeptId(item.getDeptId());
                voList.add(deviceFeatureVo);
            });
        }
        TableDataInfo dataTable = getDataTable(voList);
        dataTable.setTotal(subcontractingItemService.selectSubcontractingItemListCount(subcontractingItem));
        return dataTable;
    }


    /**
     * 查询设备特征项主列表
     */
    // @RequiresPermissions("archives:deviceFeature:list")
    @GetMapping("/deviceFeatureList")
    public TableDataInfo deviceFeatureList(DeviceFeature deviceFeature) {
        startPage();
        List<DeviceFeature> list = deviceFeatureService.selectDeviceFeatureList(deviceFeature);
        TableDataInfo dataTable = getDataTable(list);
        dataTable.setTotal(deviceFeatureService.selectDeviceFeatureListCount(deviceFeature));
        return dataTable;
    }


    /**
     * 查询劳务特征项主列表
     */
    // @RequiresPermissions("archives:laborFeature:list")
    @GetMapping("/laborServicesFeatureList")
    public TableDataInfo laborServicesFeatureList(LaborServicesFeature laborServicesFeature) {
        startPage();
        List<LaborServicesFeature> list = laborServicesFeatureService.selectLaborServicesFeatureList(laborServicesFeature);
        TableDataInfo dataTable = getDataTable(list);
        dataTable.setTotal(laborServicesFeatureService.selectLaborServicesFeatureListCount(laborServicesFeature));
        return dataTable;
    }


    /**
     * 查询专业分包特征项主列表
     */
    // @RequiresPermissions("archives:majorFeature:list")
    @GetMapping("/majorSubcontractingFeatureList")
    public TableDataInfo majorSubcontractingFeatureList(MajorSubcontractingFeature majorSubcontractingFeature) {
        startPage();
        List<MajorSubcontractingFeature> list = majorSubcontractingFeatureService.selectMajorSubcontractingFeatureList(majorSubcontractingFeature);
        TableDataInfo dataTable = getDataTable(list);
        dataTable.setTotal(majorSubcontractingFeatureService.selectMajorSubcontractingFeatureListCount(majorSubcontractingFeature));
        return dataTable;
    }


    /**
     * 查询材料特征项主列表
     */
    // @RequiresPermissions("archives:mtrFeature:list")
    @GetMapping("/mtrFeatureList")
    public TableDataInfo mtrFeatureList(MtrFeature mtrFeature) {
        startPage();
        List<MtrFeature> list = mtrFeatureService.selectMtrFeatureList(mtrFeature);
        TableDataInfo dataTable = getDataTable(list);
        dataTable.setTotal(mtrFeatureService.selectMtrFeatureListCount(mtrFeature));
        return dataTable;
    }


    /**
     * 查询设备特征值列表
     */
    // @RequiresPermissions("archives:deviceEigenvalue:list")
    @GetMapping("/deviceEigenvalueList")
    public TableDataInfo deviceEigenvalueList(DeviceEigenvalue deviceEigenvalue) {
        startPage();
        List<DeviceEigenvalue> list = deviceEigenvalueService.selectDeviceEigenvalueListNoChange(deviceEigenvalue);
        List<DeviceFeatureValueVo> voList = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(item -> {
                DeviceFeatureValueVo deviceFeatureValueVo = new DeviceFeatureValueVo();
                deviceFeatureValueVo.setId(item.getId() + "");
                deviceFeatureValueVo.setDeviceFeatureId(item.getItemId() + "");
                deviceFeatureValueVo.setFeatureValueName(item.getEigenvalueName());
                deviceFeatureValueVo.setFeatureValueCode(item.getEigenvalueCode());
                deviceFeatureValueVo.setCreateId(item.getCreateId() + "");
                deviceFeatureValueVo.setCreateBy(item.getCreateBy());
                deviceFeatureValueVo.setCreateTime(item.getCreateTime());
                deviceFeatureValueVo.setUpdateBy(item.getUpdateBy());
                deviceFeatureValueVo.setUpdateTime(item.getUpdateTime());
                deviceFeatureValueVo.setValid(Long.valueOf(item.getDelFlag()));
                deviceFeatureValueVo.setIsMain(item.getIsMain());
                deviceFeatureValueVo.setState(item.getState());
                deviceFeatureValueVo.setWfProcessId(item.getWfProcessId());
                deviceFeatureValueVo.setWfBatch(item.getWfBatch());
                deviceFeatureValueVo.setMainId(item.getMainId());
                deviceFeatureValueVo.setHostId(item.getHostId());
                deviceFeatureValueVo.setOrganCode(item.getOrganCode());
                deviceFeatureValueVo.setDeptId(item.getDeptId());
                voList.add(deviceFeatureValueVo);
            });
        }
        TableDataInfo dataTable = getDataTable(voList);
        dataTable.setTotal(deviceEigenvalueService.selectDeviceEigenvalueListCount(deviceEigenvalue));
        return dataTable;
    }


    /**
     * 查询劳务特征值列表
     */
    // @RequiresPermissions("archives:labourEigenvalue:list")
    @GetMapping("/labourEigenvalueList")
    public TableDataInfo labourEigenvalueList(LabourEigenvalue labourEigenvalue) {
        startPage();
        List<LabourEigenvalue> list = labourEigenvalueService.selectLabourEigenvalueListNoChange(labourEigenvalue);
        List<LaborServicesFeatureValueVo> voList = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(item -> {
                LaborServicesFeatureValueVo deviceFeatureValueVo = new LaborServicesFeatureValueVo();
                deviceFeatureValueVo.setId(item.getId() + "");
                deviceFeatureValueVo.setLaborServicesFeatureId(item.getItemId() + "");
                deviceFeatureValueVo.setFeatureValueName(item.getEigenvalueName());
                deviceFeatureValueVo.setFeatureValueCode(item.getEigenvalueCode());
                deviceFeatureValueVo.setCreateId(item.getCreateId() + "");
                deviceFeatureValueVo.setCreateBy(item.getCreateBy());
                deviceFeatureValueVo.setCreateTime(item.getCreateTime());
                deviceFeatureValueVo.setUpdateBy(item.getUpdateBy());
                deviceFeatureValueVo.setUpdateTime(item.getUpdateTime());
                deviceFeatureValueVo.setValid(Long.valueOf(item.getDelFlag()));
                deviceFeatureValueVo.setIsMain(item.getIsMain());
                deviceFeatureValueVo.setState(item.getState());
                deviceFeatureValueVo.setWfProcessId(item.getWfProcessId());
                deviceFeatureValueVo.setWfBatch(item.getWfBatch());
                deviceFeatureValueVo.setMainId(item.getMainId());
                deviceFeatureValueVo.setHostId(item.getHostId());
                deviceFeatureValueVo.setOrganCode(item.getOrganCode());
                deviceFeatureValueVo.setDeptId(item.getDeptId());
                voList.add(deviceFeatureValueVo);
            });
        }
        TableDataInfo dataTable = getDataTable(voList);
        dataTable.setTotal(labourEigenvalueService.selectLabourEigenvalueListCount(labourEigenvalue));
        return dataTable;
    }


    /**
     * 查询材料特征值列表
     */
    // @RequiresPermissions("archives:materialEigenvalue:list")
    @GetMapping("/materialEigenvalueList")
    public TableDataInfo materialEigenvalueList(MaterialEigenvalue materialEigenvalue) {
        startPage();
        List<MaterialEigenvalue> list = materialEigenvalueService.selectMaterialEigenvalueListNoChange(materialEigenvalue);
        List<MtrFeatureValueVo> voList = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(item -> {
                MtrFeatureValueVo deviceFeatureValueVo = new MtrFeatureValueVo();
                deviceFeatureValueVo.setId(item.getId() + "");
                deviceFeatureValueVo.setMtrFeatureId(item.getItemId() + "");
                deviceFeatureValueVo.setFeatureValueName(item.getEigenvalueName());
                deviceFeatureValueVo.setFeatureValueCode(item.getEigenvalueCode());
                deviceFeatureValueVo.setCreateId(item.getCreateId() + "");
                deviceFeatureValueVo.setCreateBy(item.getCreateBy());
                deviceFeatureValueVo.setCreateTime(item.getCreateTime());
                deviceFeatureValueVo.setUpdateBy(item.getUpdateBy());
                deviceFeatureValueVo.setUpdateTime(item.getUpdateTime());
                deviceFeatureValueVo.setValid(Long.valueOf(item.getDelFlag()));
                deviceFeatureValueVo.setIsMain(item.getIsMain());
                deviceFeatureValueVo.setState(item.getState());
                deviceFeatureValueVo.setWfProcessId(item.getWfProcessId());
                deviceFeatureValueVo.setWfBatch(item.getWfBatch());
                deviceFeatureValueVo.setMainId(item.getMainId());
                deviceFeatureValueVo.setHostId(item.getHostId());
                deviceFeatureValueVo.setOrganCode(item.getOrganCode());
                deviceFeatureValueVo.setDeptId(item.getDeptId());
                voList.add(deviceFeatureValueVo);
            });
        }
        TableDataInfo dataTable = getDataTable(voList);
        dataTable.setTotal(materialEigenvalueService.selectMaterialEigenvalueListCount(materialEigenvalue));
        return dataTable;
    }


    /**
     * 查询专业分包特征值列表
     */
    // @RequiresPermissions("archives:subcontractingEigenvalue:list")
    @GetMapping("/subcontractingEigenvalueList")
    public TableDataInfo subcontractingEigenvalueList(SubcontractingEigenvalue subcontractingEigenvalue) {
        startPage();
        List<SubcontractingEigenvalue> list = subcontractingEigenvalueService.selectSubcontractingEigenvalueListNoChange(subcontractingEigenvalue);
        List<MajorSubcontractingFeatureValueVo> voList = new ArrayList<>();
        if (list != null && !list.isEmpty()) {
            list.forEach(item -> {
                MajorSubcontractingFeatureValueVo deviceFeatureValueVo = new MajorSubcontractingFeatureValueVo();
                deviceFeatureValueVo.setId(item.getId() + "");
                deviceFeatureValueVo.setMajorSubcontractingFeatureId(item.getItemId() + "");
                deviceFeatureValueVo.setFeatureValueName(item.getEigenvalueName());
                deviceFeatureValueVo.setFeatureValueCode(item.getEigenvalueCode());
                deviceFeatureValueVo.setCreateId(item.getCreateId() + "");
                deviceFeatureValueVo.setCreateBy(item.getCreateBy());
                deviceFeatureValueVo.setCreateTime(item.getCreateTime());
                deviceFeatureValueVo.setUpdateBy(item.getUpdateBy());
                deviceFeatureValueVo.setUpdateTime(item.getUpdateTime());
                deviceFeatureValueVo.setValid(Long.valueOf(item.getDelFlag()));
                deviceFeatureValueVo.setIsMain(item.getIsMain());
                deviceFeatureValueVo.setState(item.getState());
                deviceFeatureValueVo.setWfProcessId(item.getWfProcessId());
                deviceFeatureValueVo.setWfBatch(item.getWfBatch());
                deviceFeatureValueVo.setMainId(item.getMainId());
                deviceFeatureValueVo.setHostId(item.getHostId());
                deviceFeatureValueVo.setOrganCode(item.getOrganCode());
                deviceFeatureValueVo.setDeptId(item.getDeptId());
                voList.add(deviceFeatureValueVo);
            });
        }
        TableDataInfo dataTable = getDataTable(voList);
        dataTable.setTotal(subcontractingEigenvalueService.selectSubcontractingEigenvalueListCount(subcontractingEigenvalue));
        return dataTable;
    }


    /**
     * 查询设备特征值主列表
     */
    // @RequiresPermissions("archives:featureValue:list")
    @GetMapping("/deviceFeatureValueList")
    public TableDataInfo deviceFeatureValueList(DeviceFeatureValue deviceFeatureValue) {
        startPage();
        List<DeviceFeatureValue> list = deviceFeatureValueService.selectDeviceFeatureValueList(deviceFeatureValue);
        TableDataInfo dataTable = getDataTable(list);
        dataTable.setTotal(deviceFeatureValueService.selectDeviceFeatureValueListCount(deviceFeatureValue));
        return dataTable;
    }


    /**
     * 查询劳务特征值主列表
     */
    // @RequiresPermissions("archives:laborValue:list")
    @GetMapping("/laborServicesFeatureValueList")
    public TableDataInfo laborServicesFeatureValueList(LaborServicesFeatureValue laborServicesFeatureValue) {
        startPage();
        List<LaborServicesFeatureValue> list = laborServicesFeatureValueService.selectLaborServicesFeatureValueList(laborServicesFeatureValue);
        TableDataInfo dataTable = getDataTable(list);
        dataTable.setTotal(laborServicesFeatureValueService.selectLaborServicesFeatureValueListCount(laborServicesFeatureValue));
        return dataTable;
    }


    /**
     * 查询专业分包特征值主列表
     */
    // @RequiresPermissions("archives:majorValue:list")
    @GetMapping("/majorSubcontractingFeatureValueList")
    public TableDataInfo majorSubcontractingFeatureValueList(MajorSubcontractingFeatureValue majorSubcontractingFeatureValue) {
        startPage();
        List<MajorSubcontractingFeatureValue> list = majorSubcontractingFeatureValueService.selectMajorSubcontractingFeatureValueList(majorSubcontractingFeatureValue);
        TableDataInfo dataTable = getDataTable(list);
        dataTable.setTotal(majorSubcontractingFeatureValueService.selectMajorSubcontractingFeatureValueListCount(majorSubcontractingFeatureValue));
        return dataTable;
    }


    /**
     * 查询材料特征值主列表
     */
    // @RequiresPermissions("archives:mtrValue:list")
    @GetMapping("/mtrFeatureValueList")
    public TableDataInfo mtrFeatureValueList(MtrFeatureValue mtrFeatureValue) {
        startPage();
        List<MtrFeatureValue> list = mtrFeatureValueService.selectMtrFeatureValueList(mtrFeatureValue);
        TableDataInfo dataTable = getDataTable(list);
        dataTable.setTotal(mtrFeatureValueService.selectMtrFeatureValueListCount(mtrFeatureValue));
        return dataTable;
    }


}
