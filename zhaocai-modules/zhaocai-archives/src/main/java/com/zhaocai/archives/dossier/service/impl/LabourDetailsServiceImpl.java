package com.zhaocai.archives.dossier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.domain.DeviceType;
import com.zhaocai.archives.dossier.domain.LabourDetails;
import com.zhaocai.archives.dossier.domain.LabourType;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.mapper.LabourDetailsMapper;
import com.zhaocai.archives.dossier.service.ILabourDetailsService;
import com.zhaocai.archives.dossier.service.ILabourTypeService;
import com.zhaocai.archives.main.domain.LaborServicesArchives;
import com.zhaocai.archives.main.domain.LaborServicesClass;
import com.zhaocai.archives.main.service.ILaborServicesArchivesService;
import com.zhaocai.archives.main.service.ILaborServicesClassService;
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
 * 劳务详情Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class LabourDetailsServiceImpl extends ServiceImpl<LabourDetailsMapper, LabourDetails> implements ILabourDetailsService {
    @Autowired
    private LabourDetailsMapper labourDetailsMapper;

    @Resource
    private ILaborServicesArchivesService iLaborServicesArchivesService;

    @Resource
    private ILaborServicesClassService laborServicesClassService;

    @Resource
    private ILabourTypeService iLabourTypeService;

    /**
     * 查询劳务详情
     *
     * @param id 劳务详情主键
     * @return 劳务详情
     */
    @Override
    public LabourDetails selectLabourDetailsById(Long id) {
        return labourDetailsMapper.selectLabourDetailsById(id);
    }


    /**
     * 查询劳务详情列表
     *
     * @param labourDetails 劳务详情
     * @return 劳务详情
     */
    @Override
    public List<LabourDetails> selectLabourDetailsListNoChange(LabourDetails labourDetails) {
        return labourDetailsMapper.selectLabourDetailsList(labourDetails);

    }

    /**
     * 查询劳务详情列表
     *
     * @param labourDetails 劳务详情
     * @return 劳务详情
     */
    @Override
    public List<LabourDetails> selectLabourDetailsList(LabourDetails labourDetails) {
        if (labourDetails == null) {
            labourDetails = new LabourDetails();
        }
        labourDetails.setDelFlag("0");
        List<LabourDetails> labourDetails1 = labourDetailsMapper.selectLabourDetailsList(labourDetails);
        labourDetails1.forEach(item -> {
            if ("N".equals(item.getIsMain())) {
                item.setLabourCode(item.getLabourCode() + "-" + item.getOrganCode().substring(0, 4));
            }
        });
        Map<Long, Long> idsMap = new HashMap<>();
        if (MaterialType.ALL.equals(labourDetails.getQueryType()) || MaterialType.UNTREATED.equals(labourDetails.getQueryType())) {
            //查询未处理的项数据
            LabourDetails details = new LabourDetails();
            details.setState(3L);
            details.setDelFlag("0");
            details.setIsMain("N");
            details.setMainId("0");
            details.setOrganCode(labourDetails.getOrganCode());
            List<LabourDetails> list = baseMapper.selectLabourDetailsList(details);
            list.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (MaterialType.ALL.equals(labourDetails.getQueryType()) || MaterialType.PROCESSED.equals(labourDetails.getQueryType())) {
            List<LabourDetails> details = baseMapper.getProcessed(labourDetails.getOrganCode());
            details.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (StringUtils.isEmpty(labourDetails.getQueryType())) {
            return labourDetails1;
        } else {
            labourDetails1.removeIf(item -> !idsMap.containsKey(item.getId()));
        }
        return labourDetails1;

    }

    /**
     * 新增劳务详情
     *
     * @param labourDetails 劳务详情
     * @return 结果
     */
    @Override
    public synchronized int insertLabourDetails(LabourDetails labourDetails) {
        if (labourDetails == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(labourDetails.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (labourDetails.getTypeId() == null) {
            throw new RuntimeException("类型不能为空");
        }
        if (labourDetails.getLabourCode() == null) {
            throw new RuntimeException("编码为空，请先初始化");
        }
        LabourDetails materialItem1 = new LabourDetails();
        materialItem1.setTypeId(labourDetails.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(labourDetails.getOrganCode());
        materialItem1.setIsMain("N");
        materialItem1.setLabourCode(labourDetails.getLabourCode());
        List<LabourDetails> materialItems = baseMapper.selectLabourDetailsList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            throw new RuntimeException("特征编码已存在");
        }
        if (labourDetails.getId() == null) {
            labourDetails.setId(KeyUtils.generateId());
        }
        labourDetails.setCreateId(SecurityUtils.getUserId());
        labourDetails.setIsMain("N");
        labourDetails.setState(0L);
        labourDetails.setCreateBy(SecurityUtils.getUsername());
        labourDetails.setCreateTime(DateUtils.getNowDate());
        return labourDetailsMapper.insertLabourDetails(labourDetails);
    }

    /**
     * 修改劳务详情
     *
     * @param labourDetails 劳务详情
     * @return 结果
     */
    @Override
    public int updateLabourDetails(LabourDetails labourDetails) {
        if (labourDetails == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(labourDetails.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (labourDetails.getTypeId() == null) {
            throw new RuntimeException("类型不能为空");
        }
        String qc = "-" + labourDetails.getOrganCode().substring(0, 4);
        labourDetails.setLabourCode(labourDetails.getLabourCode().replace(qc, ""));
        LabourDetails materialItem1 = new LabourDetails();
        materialItem1.setTypeId(labourDetails.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(labourDetails.getOrganCode());
        materialItem1.setLabourCode(labourDetails.getLabourCode());
        materialItem1.setIsMain("N");
        List<LabourDetails> materialItems = baseMapper.selectLabourDetailsList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            for (LabourDetails type : materialItems) {
                if (!type.getId().equals(labourDetails.getId())) {
                    throw new RuntimeException("编码已存在,请刷新后重试");
                }
            }
        }
        labourDetails.setUpdateId(SecurityUtils.getUserId());
        labourDetails.setUpdateBy(SecurityUtils.getUsername());
        labourDetails.setUpdateTime(DateUtils.getNowDate());
        return labourDetailsMapper.updateLabourDetails(labourDetails);
    }

    /**
     * 批量删除劳务详情
     *
     * @param ids 需要删除的劳务详情主键
     * @return 结果
     */
    @Override
    public boolean deleteLabourDetailsByIds(Long[] ids) {
        if (ids == null) {
            throw new RuntimeException("参数为空");
        }
        List<LabourDetails> materialDetails = labourDetailsMapper.selectBatchIds(Arrays.asList(ids));
        if (materialDetails != null && !materialDetails.isEmpty()) {
            for (LabourDetails materialDetail : materialDetails) {
                if (materialDetail.getIsMain().equals("Y")) {
                    throw new RuntimeException("主库数据不能删除");
                }
                if (materialDetail.getState() != 0L) {
                    throw new RuntimeException("已提交数据不能删除");
                }
            }
        }
        return this.removeBatchByIds(Arrays.asList(ids));
    }

    /**
     * 删除劳务详情信息
     *
     * @param id 劳务详情主键
     * @return 结果
     */
    @Override
    public int deleteLabourDetailsById(Long id) {
        return labourDetailsMapper.deleteLabourDetailsById(id);
    }

    @Override
    public List<LabourDetails> initData(LabourDetails labourDetails) {
        List<LabourDetails> materialDetailsList = labourDetailsMapper.selectLabourDetailsList(labourDetails);
        List<LaborServicesArchives> mtrArchivesList = iLaborServicesArchivesService.selectLaborServicesArchivesList(null);
        LabourType type1 = new LabourType();
        type1.setOrganCode(labourDetails.getOrganCode());
        type1.setIsMain("Y");
        List<LabourType> materialTypes = iLabourTypeService.selectLabourTypeList(type1);
        if (materialTypes == null || materialTypes.isEmpty()) {
            throw new RuntimeException("请先配置材料类型");
        }
        Map<String, LabourType> params = new HashMap<>();
        materialTypes.forEach(item -> {
            if ("Y".equals(item.getIsMain())) {
                params.put(item.getHostId(), item);
            }
        });
        Map<String, LaborServicesArchives> mtrMap = new HashMap<>();
        if (mtrArchivesList != null && !mtrArchivesList.isEmpty()) {
            mtrArchivesList.forEach(mtrFeature -> {
                mtrMap.put(mtrFeature.getId(), mtrFeature);
            });
        }
        if (materialDetailsList == null || materialDetailsList.isEmpty()) {
            //初始化-最开始无数据情况
            if (mtrArchivesList != null && !mtrArchivesList.isEmpty()) {
                mtrArchivesList.forEach(mtrFeature -> {
                    LabourDetails bean = new LabourDetails();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeature.getLaborServicesClassId()) == null ? -1L : params.get(mtrFeature.getLaborServicesClassId()).getId());
                    bean.setTypeName(params.get(mtrFeature.getLaborServicesClassId()) == null ? "" : params.get(mtrFeature.getLaborServicesClassId()).getLabourName());
                    bean.setMeasurementRules(mtrFeature.getMetrologicalRules());
                    bean.setUnit(mtrFeature.getMeasureUnit());
                    bean.setLabourName(mtrFeature.getLaborServicesName());
                    bean.setLabourCode(mtrFeature.getLaborServicesCode());
                    bean.setWorkContent(mtrFeature.getBasicJob());
                    bean.setItemAndEigenvalue(mtrFeature.getFeature());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(labourDetails.getOrganCode());
                    bean.setHostId(mtrFeature.getId());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
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
            List<LabourDetails> addList = new ArrayList<>();
            for (String key : mtrMap.keySet()) {
                if (!items.containsKey(key)) {
                    LaborServicesArchives mtrFeature = mtrMap.get(key);
                    LabourDetails bean = new LabourDetails();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeature.getLaborServicesClassId()) == null ? -1L : params.get(mtrFeature.getLaborServicesClassId()).getId());
                    bean.setTypeName(params.get(mtrFeature.getLaborServicesClassId()) == null ? "" : params.get(mtrFeature.getLaborServicesClassId()).getLabourName());
                    bean.setMeasurementRules(mtrFeature.getMetrologicalRules());
                    bean.setUnit(mtrFeature.getMeasureUnit());
                    bean.setLabourName(mtrFeature.getLaborServicesName());
                    bean.setLabourCode(mtrFeature.getLaborServicesCode());
                    bean.setWorkContent(mtrFeature.getBasicJob());
                    bean.setItemAndEigenvalue(mtrFeature.getFeature());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(labourDetails.getOrganCode());
                    bean.setHostId(mtrFeature.getId());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
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
    public void addTypeByMain(LaborServicesArchives mtrArchives) {
        LabourType materialType = new LabourType();
        materialType.setHostId(mtrArchives.getLaborServicesClassId());
        List<LabourType> materialTypes = iLabourTypeService.selectLabourTypeList(materialType);
        //已存在数据排除
        LabourDetails type1 = new LabourDetails();
        type1.setHostId(mtrArchives.getId());
        type1.setDelFlag("0");
        List<LabourDetails> materialTypes1 = baseMapper.selectLabourDetailsList(type1);
        Map<String, Long> map = new HashMap<>();
        if (materialTypes1 != null && !materialTypes1.isEmpty()) {
            materialTypes1.forEach(materialType1 -> {
                map.put(materialType1.getOrganCode(), materialType1.getTypeId());
            });
        }
        List<LabourDetails> addList = new ArrayList<>();
        if (materialTypes != null && !materialTypes.isEmpty()) {
            for (LabourType type : materialTypes) {
                if (map.containsKey(type.getOrganCode()) && (type.getId() + "").equals(map.get(type.getOrganCode()) + "")) {
                    continue;
                }
                LabourDetails bean = new LabourDetails();
                bean.setId(KeyUtils.generateId());
                bean.setTypeId(type.getId());
                bean.setTypeName(type.getLabourName());
                bean.setMeasurementRules(mtrArchives.getMetrologicalRules());
                bean.setUnit(mtrArchives.getMeasureUnit());
                bean.setLabourName(mtrArchives.getLaborServicesName());
                bean.setLabourCode(mtrArchives.getLaborServicesCode());
                bean.setWorkContent(mtrArchives.getBasicJob());
                bean.setItemAndEigenvalue(mtrArchives.getFeature());
                bean.setCreateId(SecurityUtils.getUserId());
                bean.setIsMain("Y");
                bean.setState(3L);
                bean.setOrganCode(type.getOrganCode());
                bean.setHostId(mtrArchives.getId());
                bean.setCreateBy(SecurityUtils.getUsername());
                bean.setCreateTime(DateUtils.getNowDate());
                addList.add(bean);
            }
        }
        if (!addList.isEmpty()) {
            this.saveBatch(addList);
        }
    }


    @Override
    public void updateByHostId(LaborServicesArchives mtrArchives) {
        LabourDetails materialDetails = new LabourDetails();
        materialDetails.setHostId(mtrArchives.getId());
        materialDetails.setDelFlag("0");
        List<LabourDetails> materialDetails1 = baseMapper.selectLabourDetailsList(materialDetails);
        if (materialDetails1 != null && !materialDetails1.isEmpty()) {
            materialDetails1.forEach(bean -> {
                bean.setMeasurementRules(mtrArchives.getMetrologicalRules());
                bean.setUnit(mtrArchives.getMeasureUnit());
                bean.setLabourName(mtrArchives.getLaborServicesName());
                bean.setLabourCode(mtrArchives.getLaborServicesCode());
                bean.setWorkContent(mtrArchives.getBasicJob());
                bean.setItemAndEigenvalue(mtrArchives.getFeature());
            });
        }
        this.updateBatchById(materialDetails1);
    }

    @Override
    public void deleteByHostId(String[] histIds, Map<Long, Long> idsMap) {
        if (histIds != null) {
            QueryWrapper<LabourDetails> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("del_flag", "0");
            queryWrapper.in("host_id", Arrays.asList(histIds));
            List<LabourDetails> eigenvalues = this.list(queryWrapper);
            //查询新增至主库时不同级或者编码重复的数据
            QueryWrapper<LabourDetails> qw = new QueryWrapper<>();
            qw.eq("del_flag", "0");
            qw.in("main_id", Arrays.asList(histIds));
            qw.eq("is_main","Y");
            eigenvalues.addAll(this.list(qw));
            List<LabourDetails> newDeviceDetails = new ArrayList<>();
            List<LabourDetails> upDeviceDetails = new ArrayList<>();
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
            List<Long> collect = newDeviceDetails.stream().map(LabourDetails::getId).collect(Collectors.toList());
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
    public synchronized int addToMain(LabourDetails materialDetails) {
        if (materialDetails.getId() == null) {
            throw new BusinessException("请选择要新增至主库的数据");
        }
        if (materialDetails.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        LabourDetails materialType1 = this.selectLabourDetailsById(materialDetails.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法再次新增");
        }
        LaborServicesClass mtrClass = laborServicesClassService.selectLaborServicesClassById(materialDetails.getHostId());
        if (mtrClass == null) {
            throw new BusinessException("未查询到当前主库数据，请检查后重试");
        }
        //查询编码是否已在主库存在
        LaborServicesArchives mtrClass1 = new LaborServicesArchives();
        mtrClass1.setLaborServicesClassId(materialDetails.getHostId());
        mtrClass1.setValid(0L);
        mtrClass1.setLaborServicesCode(materialType1.getLabourCode());
        List<LaborServicesArchives> mtrClasses = iLaborServicesArchivesService.selectLaborServicesArchivesList(mtrClass1);
        String key = KeyUtils.generateId() + "";
        boolean pd = true;
        LabourType materialType2 = iLabourTypeService.selectLabourTypeByIdNoChange(materialType1.getTypeId());
        if (materialType2 != null && mtrClass.getLaborServicesClassCode().equals(materialType2.getLabourCode())) {
            pd = false;
        }
        if ((mtrClasses != null && !mtrClasses.isEmpty()) || pd) {
            //存在时修改编号，并关联主表id
            LaborServicesArchives aClass = new LaborServicesArchives();
            aClass.setLaborServicesClassId(materialDetails.getHostId());
            aClass = iLaborServicesArchivesService.initDetails(aClass);
            aClass.setLaborServicesName(materialType1.getLabourName());
//                    aClass.setSpecs(materialType1.getMaterialSpecifications());
            aClass.setMetrologicalRules(materialType1.getMeasurementRules());
            aClass.setMeasureUnit(materialType1.getUnit());
            aClass.setBasicJob(materialType1.getWorkContent());
            aClass.setFeature(materialType1.getItemAndEigenvalue());
            aClass.setSonId(materialType1.getId());
            aClass.setCreateBy(SecurityUtils.getUsername());
            aClass.setCreateId(SecurityUtils.getUserId() + "");
            aClass.setCreateTime(DateUtils.getNowDate());
            iLaborServicesArchivesService.insertLaborServicesArchives(aClass);
            materialType1.setMainId(key);
            materialType1.setIsMain("Y");
            return baseMapper.updateLabourDetails(materialType1);
        } else {
            //不存在时 新增至主库
            materialType1.setIsMain("Y");
            materialType1.setHostId(key);
            int i = baseMapper.updateLabourDetails(materialType1);
            if (i > 0) {
                LaborServicesArchives aClass = new LaborServicesArchives();
                aClass.setId(key);
                aClass.setLaborServicesClassId(mtrClass.getId());
                aClass.setLaborServicesCode(materialType1.getLabourCode());
                aClass.setLaborServicesName(materialType1.getLabourName());
//                    aClass.setSpecs(materialType1.getMaterialSpecifications());
                aClass.setMetrologicalRules(materialType1.getMeasurementRules());
                aClass.setMeasureUnit(materialType1.getUnit());
                aClass.setBasicJob(materialType1.getWorkContent());
                aClass.setFeature(materialType1.getItemAndEigenvalue());
                aClass.setSonId(materialType1.getId());
                aClass.setCreateBy(SecurityUtils.getUsername());
                aClass.setCreateId(SecurityUtils.getUserId() + "");
                aClass.setCreateTime(DateUtils.getNowDate());
                iLaborServicesArchivesService.insertLaborServicesArchives(aClass);
            }
            return i;
        }
    }


    /**
     * 关联至主库
     *
     * @param materialDetails
     * @return
     */
    @Override
    public synchronized int associationToMain(LabourDetails materialDetails) {
        if (materialDetails.getId() == null) {
            throw new BusinessException("请选择要关联至主库的数据");
        }
        if (materialDetails.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        LabourDetails materialType1 = this.selectLabourDetailsById(materialDetails.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法进行关联");
        }
        if (!"0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据已关联至主库，无法再次关联");
        }
        materialType1.setMainId(materialDetails.getHostId());
        return baseMapper.updateLabourDetails(materialType1);
    }


    /**
     * 取消关联至主库
     *
     * @param materialDetails
     * @return
     */
    @Override
    public synchronized int unAssociationToMain(LabourDetails materialDetails) {
        if (materialDetails.getId() == null) {
            throw new BusinessException("请选择要取消关联至主库的数据");
        }
        LabourDetails materialType1 = this.selectLabourDetailsById(materialDetails.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据为主库同步数据，无法进行操作");
        }
        if ("0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据尚未关联至主库，无需取消关联");
        }
        materialType1.setMainId("0");
        return baseMapper.updateLabourDetails(materialType1);
    }


    @Override
    public LabourDetails initDetails(LabourDetails materialDetails) {
        if (StringUtils.isEmpty(materialDetails.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (materialDetails.getTypeId() == null) {
            throw new RuntimeException("类型id不能为空");
        }
        LabourType subcontractingType = iLabourTypeService.selectLabourTypeById(materialDetails.getTypeId());
        if (subcontractingType == null) {
            throw new BusinessException("请选择正确的类型");
        }
        String materialCode = subcontractingType.getLabourCode();
        Integer maxCode = baseMapper.getMaxCode(materialDetails.getTypeId(), materialDetails.getOrganCode(), subcontractingType.getLabourCode());
        if (maxCode != null) {
            maxCode += 1;
            //根据规则，长度大于8的流水号有3位
            if (maxCode < 100 && maxCode >= 10) {
                materialCode = materialCode + "0" + maxCode;
            } else if (maxCode < 10) {
                materialCode = materialCode + "00" + maxCode;
            } else {
                materialCode = materialCode + maxCode;
            }
        } else {
            materialCode = materialCode + "001";
        }
        LabourDetails materialType1 = new LabourDetails();
        materialType1.setId(KeyUtils.generateId());
        materialType1.setTypeId(subcontractingType.getId());
        materialType1.setLabourCode(materialCode);
        materialType1.setState(0L);
        materialType1.setIsMain("N");
        materialType1.setCreateId(SecurityUtils.getUserId());
        materialType1.setCreateBy(SecurityUtils.getUsername());
        materialType1.setCreateTime(DateUtils.getNowDate());
        return materialType1;
    }

    @Override
    public long selectLabourDetailsListCount(LabourDetails labourDetails) {
        return baseMapper.selectLabourDetailsListCount(labourDetails);
    }

    @Override
    public List<LabourDetails> getProcessed(String organCode) {
        return baseMapper.getProcessed(organCode);
    }

}
