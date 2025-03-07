package com.zhaocai.archives.dossier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.domain.SubcontractingDetails;
import com.zhaocai.archives.dossier.domain.SubcontractingType;
import com.zhaocai.archives.dossier.mapper.SubcontractingDetailsMapper;
import com.zhaocai.archives.dossier.service.ISubcontractingDetailsService;
import com.zhaocai.archives.dossier.service.ISubcontractingTypeService;
import com.zhaocai.archives.main.domain.MajorSubcontractingArchives;
import com.zhaocai.archives.main.domain.MajorSubcontractingClass;
import com.zhaocai.archives.main.service.IMajorSubcontractingArchivesService;
import com.zhaocai.archives.main.service.IMajorSubcontractingClassService;
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
 * 专业分包详情Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class SubcontractingDetailsServiceImpl extends ServiceImpl<SubcontractingDetailsMapper, SubcontractingDetails> implements ISubcontractingDetailsService {
    @Autowired
    private SubcontractingDetailsMapper subcontractingDetailsMapper;


    @Resource
    private IMajorSubcontractingArchivesService iMajorSubcontractingArchivesService;

    @Resource
    private IMajorSubcontractingClassService iMajorSubcontractingClassService;

    @Resource
    private ISubcontractingTypeService iSubcontractingTypeService;

    /**
     * 查询专业分包详情
     *
     * @param id 专业分包详情主键
     * @return 专业分包详情
     */
    @Override
    public SubcontractingDetails selectSubcontractingDetailsById(Long id) {
        SubcontractingDetails subcontractingDetails = subcontractingDetailsMapper.selectSubcontractingDetailsById(id);
        if (subcontractingDetails == null) {
            return null;
        }
        if ("N".equals(subcontractingDetails.getIsMain())) {
            subcontractingDetails.setSubcontractingCode(subcontractingDetails.getSubcontractingCode() + "-" + subcontractingDetails.getOrganCode().substring(0, 4));
        }
        return subcontractingDetails;
    }


    /**
     * 查询专业分包详情列表
     *
     * @param subcontractingDetails 专业分包详情
     * @return 专业分包详情
     */
    @Override
    public List<SubcontractingDetails> selectSubcontractingDetailsListNoChange(SubcontractingDetails subcontractingDetails) {
        return subcontractingDetailsMapper.selectSubcontractingDetailsList(subcontractingDetails);
    }


    /**
     * 查询专业分包详情列表
     *
     * @param subcontractingDetails 专业分包详情
     * @return 专业分包详情
     */
    @Override
    public List<SubcontractingDetails> selectSubcontractingDetailsList(SubcontractingDetails subcontractingDetails) {
        if (subcontractingDetails == null) {
            subcontractingDetails = new SubcontractingDetails();
        }
        subcontractingDetails.setDelFlag("0");
        List<SubcontractingDetails> subcontractingDetails1 = subcontractingDetailsMapper.selectSubcontractingDetailsList(subcontractingDetails);
        subcontractingDetails1.forEach(materialItem1 -> {
            if ("N".equals(materialItem1.getIsMain())) {
                materialItem1.setSubcontractingCode(materialItem1.getSubcontractingCode() + "-" + materialItem1.getOrganCode().substring(0, 4));
            }
        });
        Map<Long, Long> idsMap = new HashMap<>();
        if (MaterialType.ALL.equals(subcontractingDetails.getQueryType()) || MaterialType.UNTREATED.equals(subcontractingDetails.getQueryType())) {
            //查询未处理的项数据
            SubcontractingDetails details = new SubcontractingDetails();
            details.setState(3L);
            details.setDelFlag("0");
            details.setIsMain("N");
            details.setMainId("0");
            details.setOrganCode(subcontractingDetails.getOrganCode());
            List<SubcontractingDetails> list = baseMapper.selectSubcontractingDetailsList(details);
            list.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (MaterialType.ALL.equals(subcontractingDetails.getQueryType()) || MaterialType.PROCESSED.equals(subcontractingDetails.getQueryType())) {
            List<SubcontractingDetails> details = baseMapper.getProcessed(subcontractingDetails.getOrganCode());
            details.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (StringUtils.isEmpty(subcontractingDetails.getQueryType())) {
            return subcontractingDetails1;
        } else {
            subcontractingDetails1.removeIf(item -> !idsMap.containsKey(item.getId()));
        }
        return subcontractingDetails1;
    }

    /**
     * 新增专业分包详情
     *
     * @param subcontractingDetails 专业分包详情
     * @return 结果
     */
    @Override
    public synchronized int insertSubcontractingDetails(SubcontractingDetails subcontractingDetails) {
        if (subcontractingDetails == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(subcontractingDetails.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (subcontractingDetails.getTypeId() == null) {
            throw new RuntimeException("材料类型不能为空");
        }
        if (subcontractingDetails.getSubcontractingCode() == null) {
            throw new RuntimeException("编码为空，请先初始化");
        }
        SubcontractingDetails materialItem1 = new SubcontractingDetails();
        materialItem1.setTypeId(subcontractingDetails.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(subcontractingDetails.getOrganCode());
        materialItem1.setSubcontractingCode(subcontractingDetails.getSubcontractingCode());
        materialItem1.setIsMain("N");
        List<SubcontractingDetails> materialItems = baseMapper.selectSubcontractingDetailsList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            throw new RuntimeException("特征编码已存在");
        }
        if (subcontractingDetails.getId() == null) {
            subcontractingDetails.setId(KeyUtils.generateId());
        }
        subcontractingDetails.setCreateId(SecurityUtils.getUserId());
        subcontractingDetails.setIsMain("N");
        subcontractingDetails.setState(0L);
        subcontractingDetails.setCreateBy(SecurityUtils.getUsername());
        subcontractingDetails.setCreateTime(DateUtils.getNowDate());
        return subcontractingDetailsMapper.insertSubcontractingDetails(subcontractingDetails);
    }

    /**
     * 修改专业分包详情
     *
     * @param subcontractingDetails 专业分包详情
     * @return 结果
     */
    @Override
    public int updateSubcontractingDetails(SubcontractingDetails subcontractingDetails) {
        if (subcontractingDetails == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(subcontractingDetails.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (subcontractingDetails.getTypeId() == null) {
            throw new RuntimeException("材料类型不能为空");
        }
        String qc = "-" + subcontractingDetails.getOrganCode().substring(0, 4);
        subcontractingDetails.setSubcontractingCode(subcontractingDetails.getSubcontractingCode().replace(qc, ""));
        SubcontractingDetails materialItem1 = new SubcontractingDetails();
        materialItem1.setTypeId(subcontractingDetails.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(subcontractingDetails.getOrganCode());
        materialItem1.setSubcontractingCode(subcontractingDetails.getSubcontractingCode());
        materialItem1.setIsMain("N");
        List<SubcontractingDetails> materialItems = baseMapper.selectSubcontractingDetailsList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            for (SubcontractingDetails type : materialItems) {
                if (!type.getId().equals(subcontractingDetails.getId())) {
                    throw new RuntimeException("编码已存在,请刷新后重试");
                }
            }
        }
        subcontractingDetails.setUpdateId(SecurityUtils.getUserId());
        subcontractingDetails.setUpdateBy(SecurityUtils.getUsername());
        subcontractingDetails.setUpdateTime(DateUtils.getNowDate());
        return subcontractingDetailsMapper.updateSubcontractingDetails(subcontractingDetails);
    }

    /**
     * 批量删除专业分包详情
     *
     * @param ids 需要删除的专业分包详情主键
     * @return 结果
     */
    @Override
    public boolean deleteSubcontractingDetailsByIds(Long[] ids) {
        if (ids == null) {
            throw new RuntimeException("参数为空");
        }
        List<SubcontractingDetails> materialDetails = subcontractingDetailsMapper.selectBatchIds(Arrays.asList(ids));
        if (materialDetails != null && !materialDetails.isEmpty()) {
            for (SubcontractingDetails materialDetail : materialDetails) {
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
     * 删除专业分包详情信息
     *
     * @param id 专业分包详情主键
     * @return 结果
     */
    @Override
    public int deleteSubcontractingDetailsById(Long id) {
        return subcontractingDetailsMapper.deleteSubcontractingDetailsById(id);
    }

    @Override
    public List<SubcontractingDetails> initData(SubcontractingDetails subcontractingDetails) {
        List<SubcontractingDetails> materialDetailsList = subcontractingDetailsMapper.selectSubcontractingDetailsList(subcontractingDetails);
        List<MajorSubcontractingArchives> mtrArchivesList = iMajorSubcontractingArchivesService.selectMajorSubcontractingArchivesList(null);
        SubcontractingType type1 = new SubcontractingType();
        type1.setOrganCode(subcontractingDetails.getOrganCode());
        type1.setIsMain("Y");
        List<SubcontractingType> materialTypes = iSubcontractingTypeService.selectSubcontractingTypeList(type1);
        if (materialTypes == null || materialTypes.isEmpty()) {
            throw new RuntimeException("请先配置材料类型");
        }
        Map<String, SubcontractingType> params = new HashMap<>();
        materialTypes.forEach(item -> {
            if ("Y".equals(item.getIsMain())) {
                params.put(item.getHostId(), item);
            }
        });
        Map<String, MajorSubcontractingArchives> mtrMap = new HashMap<>();
        if (mtrArchivesList != null && !mtrArchivesList.isEmpty()) {
            mtrArchivesList.forEach(mtrFeature -> {
                mtrMap.put(mtrFeature.getId(), mtrFeature);
            });
        }
        if (materialDetailsList == null || materialDetailsList.isEmpty()) {
            //初始化-最开始无数据情况
            if (mtrArchivesList != null && !mtrArchivesList.isEmpty()) {
                mtrArchivesList.forEach(mtrFeature -> {
                    SubcontractingDetails bean = new SubcontractingDetails();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeature.getMajorSubcontractingClassId()) == null ? -1L : params.get(mtrFeature.getMajorSubcontractingClassId()).getId());
                    bean.setTypeName(params.get(mtrFeature.getMajorSubcontractingClassId()) == null ? "" : params.get(mtrFeature.getMajorSubcontractingClassId()).getSubcontractingName());
                    bean.setMeasurementRules(mtrFeature.getMetrologicalRules());
                    bean.setWorkContent(mtrFeature.getBasicJob());
                    bean.setUnit(mtrFeature.getMeasureUnit());
                    bean.setSubcontractingCode(mtrFeature.getMajorSubcontractingCode());
                    bean.setSubcontractingName(mtrFeature.getMajorSubcontractingName());
                    bean.setItemAndEigenvalue(mtrFeature.getFeature());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(subcontractingDetails.getOrganCode());
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
            List<SubcontractingDetails> addList = new ArrayList<>();
            for (String key : mtrMap.keySet()) {
                if (!items.containsKey(key)) {
                    MajorSubcontractingArchives mtrFeature = mtrMap.get(key);
                    SubcontractingDetails bean = new SubcontractingDetails();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeature.getMajorSubcontractingClassId()) == null ? -1L : params.get(mtrFeature.getMajorSubcontractingClassId()).getId());
                    bean.setTypeName(params.get(mtrFeature.getMajorSubcontractingClassId()) == null ? "" : params.get(mtrFeature.getMajorSubcontractingClassId()).getSubcontractingName());
                    bean.setMeasurementRules(mtrFeature.getMetrologicalRules());
                    bean.setWorkContent(mtrFeature.getBasicJob());
                    bean.setUnit(mtrFeature.getMeasureUnit());
                    bean.setSubcontractingCode(mtrFeature.getMajorSubcontractingCode());
                    bean.setSubcontractingName(mtrFeature.getMajorSubcontractingName());
                    bean.setItemAndEigenvalue(mtrFeature.getFeature());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(subcontractingDetails.getOrganCode());
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
    public void addTypeByMain(MajorSubcontractingArchives mtrArchives) {
        SubcontractingType materialType = new SubcontractingType();
        materialType.setHostId(mtrArchives.getMajorSubcontractingClassId());
        List<SubcontractingType> materialTypes = iSubcontractingTypeService.selectSubcontractingTypeList(materialType);
        //已存在数据排除
        SubcontractingDetails type1 = new SubcontractingDetails();
        type1.setHostId(mtrArchives.getId());
        type1.setDelFlag("0");
        List<SubcontractingDetails> materialTypes1 = baseMapper.selectSubcontractingDetailsList(type1);
        Map<String, Long> map = new HashMap<>();
        if (materialTypes1 != null && !materialTypes1.isEmpty()) {
            materialTypes1.forEach(materialType1 -> {
                map.put(materialType1.getOrganCode(), materialType1.getTypeId());
            });
        }
        List<SubcontractingDetails> addList = new ArrayList<>();
        if (materialTypes != null && !materialTypes.isEmpty()) {
            for (SubcontractingType type : materialTypes) {
                if (map.containsKey(type.getOrganCode()) && (type.getId() + "").equals(map.get(type.getOrganCode()) + "")) {
                    continue;
                }
                SubcontractingDetails bean = new SubcontractingDetails();
                bean.setId(KeyUtils.generateId());
                bean.setTypeId(type.getId());
                bean.setTypeName(type.getSubcontractingName());
                bean.setMeasurementRules(mtrArchives.getMetrologicalRules());
                bean.setWorkContent(mtrArchives.getBasicJob());
                bean.setUnit(mtrArchives.getMeasureUnit());
                bean.setSubcontractingCode(mtrArchives.getMajorSubcontractingCode());
                bean.setSubcontractingName(mtrArchives.getMajorSubcontractingName());
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
    public void updateByHostId(MajorSubcontractingArchives mtrArchives) {
        SubcontractingDetails materialDetails = new SubcontractingDetails();
        materialDetails.setHostId(mtrArchives.getId());
        materialDetails.setDelFlag("0");
        List<SubcontractingDetails> materialDetails1 = baseMapper.selectSubcontractingDetailsList(materialDetails);
        if (materialDetails1 != null && !materialDetails1.isEmpty()) {
            materialDetails1.forEach(bean -> {
                bean.setMeasurementRules(mtrArchives.getMetrologicalRules());
                bean.setWorkContent(mtrArchives.getBasicJob());
                bean.setUnit(mtrArchives.getMeasureUnit());
                bean.setSubcontractingCode(mtrArchives.getMajorSubcontractingCode());
                bean.setSubcontractingName(mtrArchives.getMajorSubcontractingName());
                bean.setItemAndEigenvalue(mtrArchives.getFeature());
            });
        }
        this.updateBatchById(materialDetails1);
    }

    @Override
    public void deleteByHostId(String[] histIds, Map<Long, Long> idsMap) {
        if (histIds != null) {
            QueryWrapper<SubcontractingDetails> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("del_flag", "0");
            queryWrapper.in("host_id", Arrays.asList(histIds));
            List<SubcontractingDetails> eigenvalues = this.list(queryWrapper);
            //查询新增至主库时不同级或者编码重复的数据
            QueryWrapper<SubcontractingDetails> qw = new QueryWrapper<>();
            qw.eq("del_flag", "0");
            qw.in("main_id", Arrays.asList(histIds));
            qw.eq("is_main","Y");
            eigenvalues.addAll(this.list(qw));
            List<SubcontractingDetails> newDeviceDetails = new ArrayList<>();
            List<SubcontractingDetails> upDeviceDetails = new ArrayList<>();
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
            List<Long> collect = newDeviceDetails.stream().map(SubcontractingDetails::getId).collect(Collectors.toList());
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
    public synchronized int addToMain(SubcontractingDetails materialDetails) {
        if (materialDetails.getId() == null) {
            throw new BusinessException("请选择要新增至主库的数据");
        }
        if (materialDetails.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        SubcontractingDetails materialType1 = this.selectSubcontractingDetailsById(materialDetails.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法再次新增");
        }
        MajorSubcontractingClass mtrClass = iMajorSubcontractingClassService.selectMajorSubcontractingClassById(materialDetails.getHostId());
        if (mtrClass == null) {
            throw new BusinessException("未查询到当前主库数据，请检查后重试");
        }
        //查询编码是否已在主库存在
        MajorSubcontractingArchives mtrClass1 = new MajorSubcontractingArchives();
        mtrClass1.setMajorSubcontractingClassId(materialDetails.getHostId());
        mtrClass1.setValid(0L);
        mtrClass1.setMajorSubcontractingCode(materialType1.getSubcontractingCode());
        List<MajorSubcontractingArchives> mtrClasses = iMajorSubcontractingArchivesService.selectMajorSubcontractingArchivesList(mtrClass1);
        String key = KeyUtils.generateId() + "";
        boolean pd = true;
        SubcontractingType materialType2 = iSubcontractingTypeService.selectSubcontractingTypeByIdNoChange(materialType1.getTypeId());
        if (materialType2 != null && mtrClass.getMajorSubcontractingClassCode().equals(materialType2.getSubcontractingCode())) {
            pd = false;
        }
        if ((mtrClasses != null && !mtrClasses.isEmpty()) || pd) {
            MajorSubcontractingArchives aClass = new MajorSubcontractingArchives();
            aClass.setMajorSubcontractingClassId(materialDetails.getHostId());
            aClass = iMajorSubcontractingArchivesService.initDetails(aClass);
            aClass.setMajorSubcontractingName(materialType1.getSubcontractingName());
            aClass.setFeature(materialType1.getItemAndEigenvalue());
            aClass.setMeasureUnit(materialType1.getUnit());
            aClass.setMetrologicalRules(materialType1.getMeasurementRules());
            aClass.setBasicJob(materialType1.getWorkContent());
            aClass.setSonId(materialType1.getId());
            aClass.setCreateBy(SecurityUtils.getUsername());
            aClass.setCreateId(SecurityUtils.getUserId() + "");
            aClass.setCreateTime(DateUtils.getNowDate());
            iMajorSubcontractingArchivesService.insertMajorSubcontractingArchives(aClass);
            materialType1.setMainId(aClass.getId());
            materialType1.setIsMain("Y");
            return baseMapper.updateSubcontractingDetails(materialType1);
        } else {
            //不存在时 新增至主库
            materialType1.setIsMain("Y");
            materialType1.setHostId(key);
            int i = baseMapper.updateSubcontractingDetails(materialType1);
            if (i > 0) {
                MajorSubcontractingArchives aClass = new MajorSubcontractingArchives();
                aClass.setId(key);
                aClass.setMajorSubcontractingClassId(mtrClass.getId());
                aClass.setMajorSubcontractingCode(materialType1.getSubcontractingCode());
                aClass.setMajorSubcontractingName(materialType1.getSubcontractingName());
                aClass.setFeature(materialType1.getItemAndEigenvalue());
                aClass.setMeasureUnit(materialType1.getUnit());
                aClass.setMetrologicalRules(materialType1.getMeasurementRules());
                aClass.setBasicJob(materialType1.getWorkContent());
                aClass.setSonId(materialType1.getId());
                aClass.setCreateBy(SecurityUtils.getUsername());
                aClass.setCreateId(SecurityUtils.getUserId() + "");
                aClass.setCreateTime(DateUtils.getNowDate());
                iMajorSubcontractingArchivesService.insertMajorSubcontractingArchives(aClass);
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
    public synchronized int associationToMain(SubcontractingDetails materialDetails) {
        if (materialDetails.getId() == null) {
            throw new BusinessException("请选择要关联至主库的数据");
        }
        if (materialDetails.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        SubcontractingDetails materialType1 = this.selectSubcontractingDetailsById(materialDetails.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法进行关联");
        }
        if (!"0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据已关联至主库，无法再次关联");
        }
        materialType1.setMainId(materialDetails.getHostId());
        return baseMapper.updateSubcontractingDetails(materialType1);
    }


    /**
     * 取消关联至主库
     *
     * @param materialDetails
     * @return
     */
    @Override
    public synchronized int unAssociationToMain(SubcontractingDetails materialDetails) {
        if (materialDetails.getId() == null) {
            throw new BusinessException("请选择要取消关联至主库的数据");
        }
        SubcontractingDetails materialType1 = this.selectSubcontractingDetailsById(materialDetails.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据为主库同步数据，无法进行操作");
        }
        if ("0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据尚未关联至主库，无需取消关联");
        }
        materialType1.setMainId("0");
        return baseMapper.updateSubcontractingDetails(materialType1);
    }

    @Override
    public SubcontractingDetails initDetails(SubcontractingDetails subcontractingDetails) {
        if (StringUtils.isEmpty(subcontractingDetails.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (subcontractingDetails.getTypeId() == null) {
            throw new RuntimeException("类型id不能为空");
        }
        SubcontractingType subcontractingType = iSubcontractingTypeService.selectSubcontractingTypeById(subcontractingDetails.getTypeId());
        if (subcontractingType == null) {
            throw new BusinessException("请选择正确的类型");
        }
        String materialCode = subcontractingType.getSubcontractingCode();
        Integer maxCode = baseMapper.getMaxCode(subcontractingDetails.getTypeId(), subcontractingDetails.getOrganCode(), subcontractingType.getSubcontractingCode());
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
        SubcontractingDetails materialType1 = new SubcontractingDetails();
        materialType1.setId(KeyUtils.generateId());
        materialType1.setTypeId(subcontractingType.getId());
        materialType1.setSubcontractingCode(materialCode);
        materialType1.setState(0L);
        materialType1.setIsMain("N");
        materialType1.setCreateId(SecurityUtils.getUserId());
        materialType1.setCreateBy(SecurityUtils.getUsername());
        materialType1.setCreateTime(DateUtils.getNowDate());
        return materialType1;
    }

    @Override
    public long selectSubcontractingDetailsListCount(SubcontractingDetails subcontractingDetails) {
        return baseMapper.selectSubcontractingDetailsListCount(subcontractingDetails);
    }

    @Override
    public List<SubcontractingDetails> getProcessed(String organCode) {
        return baseMapper.getProcessed(organCode);
    }


}
