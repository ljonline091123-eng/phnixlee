package com.zhaocai.archives.dossier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.domain.SubcontractingDetails;
import com.zhaocai.archives.dossier.domain.SubcontractingEigenvalue;
import com.zhaocai.archives.dossier.domain.SubcontractingItem;
import com.zhaocai.archives.dossier.mapper.SubcontractingEigenvalueMapper;
import com.zhaocai.archives.dossier.service.ISubcontractingEigenvalueService;
import com.zhaocai.archives.dossier.service.ISubcontractingItemService;
import com.zhaocai.archives.dossier.service.ISubcontractingTypeService;
import com.zhaocai.archives.main.domain.MajorSubcontractingFeature;
import com.zhaocai.archives.main.domain.MajorSubcontractingFeatureValue;
import com.zhaocai.archives.main.service.IMajorSubcontractingFeatureService;
import com.zhaocai.archives.main.service.IMajorSubcontractingFeatureValueService;
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
 * 专业分包特征值Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class SubcontractingEigenvalueServiceImpl extends ServiceImpl<SubcontractingEigenvalueMapper, SubcontractingEigenvalue> implements ISubcontractingEigenvalueService {
    @Autowired
    private SubcontractingEigenvalueMapper subcontractingEigenvalueMapper;

    @Resource
    private IMajorSubcontractingFeatureValueService iMajorSubcontractingFeatureValueService;

    @Resource
    private ISubcontractingItemService iSubcontractingItemService;

    @Resource
    private IMajorSubcontractingFeatureService iMajorSubcontractingFeatureService;

    @Resource
    private ISubcontractingTypeService iSubcontractingTypeService;

    /**
     * 查询专业分包特征值
     *
     * @param id 专业分包特征值主键
     * @return 专业分包特征值
     */
    @Override
    public SubcontractingEigenvalue selectSubcontractingEigenvalueById(Long id) {
        SubcontractingEigenvalue subcontractingEigenvalue = subcontractingEigenvalueMapper.selectSubcontractingEigenvalueById(id);
//        if ("N".equals(subcontractingEigenvalue.getIsMain())) {
//            String thStr = subcontractingEigenvalue.getOrganCode().substring(0, 4);
//            String xStr = iSubcontractingItemService.selectSubcontractingItemById(subcontractingEigenvalue.getItemId()).getItemCode().replace(thStr, "");
//            String typeStr = iSubcontractingTypeService.selectSubcontractingTypeById(subcontractingEigenvalue.getTypeId()).getSubcontractingCode().replace(thStr, "");
//            subcontractingEigenvalue.setEigenvalueCode(typeStr + xStr + subcontractingEigenvalue.getEigenvalueCode() + "-" + thStr);
//        }
        return subcontractingEigenvalue;
    }

    /**
     * 查询专业分包特征值列表
     *
     * @param subcontractingEigenvalue 专业分包特征值
     * @return 专业分包特征值
     */
    @Override
    public List<SubcontractingEigenvalue> selectSubcontractingEigenvalueListNoChange(SubcontractingEigenvalue subcontractingEigenvalue) {
        return subcontractingEigenvalueMapper.selectSubcontractingEigenvalueList(subcontractingEigenvalue);
    }


    /**
     * 查询专业分包特征值列表
     *
     * @param subcontractingEigenvalue 专业分包特征值
     * @return 专业分包特征值
     */
    @Override
    public List<SubcontractingEigenvalue> selectSubcontractingEigenvalueList(SubcontractingEigenvalue subcontractingEigenvalue) {
        if (subcontractingEigenvalue == null) {
            subcontractingEigenvalue = new SubcontractingEigenvalue();
        }
        subcontractingEigenvalue.setDelFlag("0");
        List<SubcontractingEigenvalue> subcontractingEigenvalues = subcontractingEigenvalueMapper.selectSubcontractingEigenvalueList(subcontractingEigenvalue);
        if (subcontractingEigenvalues != null && !subcontractingEigenvalues.isEmpty()) {
            String thStr = subcontractingEigenvalues.get(0).getOrganCode().substring(0, 4);
            SubcontractingItem item = iSubcontractingItemService.selectSubcontractingItemById(subcontractingEigenvalues.get(0).getItemId());
            String xStr = item.getItemCode().replace("-" + thStr, "");
            String typeStr = iSubcontractingTypeService.selectSubcontractingTypeById(item.getTypeId()).getSubcontractingCode().replace("-" + thStr, "");
            for (SubcontractingEigenvalue eigenvalue : subcontractingEigenvalues) {
                if ("N".equals(eigenvalue.getIsMain())) {
                    eigenvalue.setEigenvalueCode(typeStr + xStr + eigenvalue.getEigenvalueCode() + "-" + thStr);
                }
            }
        }
        Map<Long, Long> idsMap = new HashMap<>();
        if (MaterialType.ALL.equals(subcontractingEigenvalue.getQueryType()) || MaterialType.UNTREATED.equals(subcontractingEigenvalue.getQueryType())) {
            SubcontractingEigenvalue eigenvalue = new SubcontractingEigenvalue();
            eigenvalue.setState(3L);
            eigenvalue.setDelFlag("0");
            eigenvalue.setIsMain("N");
            eigenvalue.setMainId("0");
            eigenvalue.setTypeId(subcontractingEigenvalue.getTypeId());
            eigenvalue.setOrganCode(subcontractingEigenvalue.getOrganCode());
            List<SubcontractingEigenvalue> list = baseMapper.selectSubcontractingEigenvalueList(eigenvalue);
            list.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (MaterialType.ALL.equals(subcontractingEigenvalue.getQueryType()) || MaterialType.PROCESSED.equals(subcontractingEigenvalue.getQueryType())) {
            List<SubcontractingEigenvalue> eigenvalues1 = baseMapper.getProcessed(subcontractingEigenvalue.getOrganCode());
            eigenvalues1.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (StringUtils.isEmpty(subcontractingEigenvalue.getQueryType())) {
            return subcontractingEigenvalues;
        } else if (subcontractingEigenvalues != null) {
            subcontractingEigenvalues.removeIf(item -> !idsMap.containsKey(item.getId()));
        }
        return subcontractingEigenvalues;
    }

    /**
     * 新增专业分包特征值
     *
     * @param subcontractingEigenvalue 专业分包特征值
     * @return 结果
     */
    @Override
    public synchronized int insertSubcontractingEigenvalue(SubcontractingEigenvalue subcontractingEigenvalue) {
        if (subcontractingEigenvalue != null) {
            if (subcontractingEigenvalue.getItemId() == null) {
                throw new RuntimeException("未获取到特征项id");
            }
            if (StringUtils.isEmpty(subcontractingEigenvalue.getOrganCode())) {
                throw new RuntimeException("未获取到机构代码");
            }
            SubcontractingEigenvalue materialItem1 = new SubcontractingEigenvalue();
            materialItem1.setItemId(subcontractingEigenvalue.getItemId());
            materialItem1.setDelFlag("0");
            materialItem1.setOrganCode(subcontractingEigenvalue.getOrganCode());
            materialItem1.setEigenvalueCode(subcontractingEigenvalue.getEigenvalueCode());
            materialItem1.setIsMain("N");
            List<SubcontractingEigenvalue> materialItems = baseMapper.selectSubcontractingEigenvalueList(materialItem1);
            if (materialItems != null && !materialItems.isEmpty()) {
                throw new RuntimeException("编码已存在");
            }
            if (subcontractingEigenvalue.getId() == null) {
                subcontractingEigenvalue.setId(KeyUtils.generateId());
                subcontractingEigenvalue.setState(0L);
                subcontractingEigenvalue.setCreateId(SecurityUtils.getUserId());
                subcontractingEigenvalue.setCreateBy(SecurityUtils.getUsername());
                subcontractingEigenvalue.setIsMain("N");
                subcontractingEigenvalue.setCreateTime(DateUtils.getNowDate());
            }
        } else {
            throw new RuntimeException("未获取到特征值信息");
        }
        return subcontractingEigenvalueMapper.insertSubcontractingEigenvalue(subcontractingEigenvalue);
    }

    /**
     * 修改专业分包特征值
     *
     * @param subcontractingEigenvalue 专业分包特征值
     * @return 结果
     */
    @Override
    public int updateSubcontractingEigenvalue(SubcontractingEigenvalue subcontractingEigenvalue) {
        if (subcontractingEigenvalue == null) {
            throw new RuntimeException("未获取到特征值信息");
        }
        if (subcontractingEigenvalue.getItemId() == null) {
            throw new RuntimeException("未获取到特征项id");
        }
        if (StringUtils.isEmpty(subcontractingEigenvalue.getOrganCode())) {
            throw new RuntimeException("未获取到机构代码");
        }
//        String qc = "-"+subcontractingEigenvalue.getOrganCode().substring(0, 4);
//        subcontractingEigenvalue.setEigenvalueCode(subcontractingEigenvalue.getEigenvalueCode().replace(qc, ""));
        SubcontractingEigenvalue materialItem1 = new SubcontractingEigenvalue();
        materialItem1.setTypeId(subcontractingEigenvalue.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(subcontractingEigenvalue.getOrganCode());
        materialItem1.setEigenvalueCode(subcontractingEigenvalue.getEigenvalueCode());
        materialItem1.setIsMain("N");
        List<SubcontractingEigenvalue> materialItems = baseMapper.selectSubcontractingEigenvalueList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            for (SubcontractingEigenvalue type : materialItems) {
                if (!type.getId().equals(subcontractingEigenvalue.getId())) {
                    throw new RuntimeException("编码已存在,请刷新后重试");
                }
            }
        }
        subcontractingEigenvalue.setUpdateId(SecurityUtils.getUserId());
        subcontractingEigenvalue.setUpdateBy(SecurityUtils.getUsername());
        subcontractingEigenvalue.setUpdateTime(DateUtils.getNowDate());
        return subcontractingEigenvalueMapper.updateSubcontractingEigenvalue(subcontractingEigenvalue);
    }

    /**
     * 批量删除专业分包特征值
     *
     * @param ids 需要删除的专业分包特征值主键
     * @return 结果
     */
    @Override
    public boolean deleteSubcontractingEigenvalueByIds(Long[] ids) {
        if (ids == null) {
            throw new RuntimeException("参数为空");
        }
        List<SubcontractingEigenvalue> eigenvalues = subcontractingEigenvalueMapper.selectBatchIds(Arrays.asList(ids));
        for (SubcontractingEigenvalue eigenvalue : eigenvalues) {
            if (eigenvalue.getIsMain().equals("Y")) {
                throw new RuntimeException("主库同步数据不允许删除");
            }
            if (eigenvalue.getState() != 0L) {
                throw new RuntimeException("流程中数据不允许删除");
            }
        }
        return this.removeBatchByIds(Arrays.asList(ids));
    }

    /**
     * 删除专业分包特征值信息
     *
     * @param id 专业分包特征值主键
     * @return 结果
     */
    @Override
    public int deleteSubcontractingEigenvalueById(Long id) {
        return subcontractingEigenvalueMapper.deleteSubcontractingEigenvalueById(id);
    }

    @Override
    public List<SubcontractingEigenvalue> initData(SubcontractingEigenvalue subcontractingEigenvalue) {
        List<SubcontractingEigenvalue> eigenvalues = subcontractingEigenvalueMapper.selectSubcontractingEigenvalueList(subcontractingEigenvalue);
        List<MajorSubcontractingFeatureValue> mtrFeatureValues = iMajorSubcontractingFeatureValueService.selectMajorSubcontractingFeatureValueList(null);
        SubcontractingItem item1 = new SubcontractingItem();
        item1.setOrganCode(subcontractingEigenvalue.getOrganCode());
        item1.setIsMain("Y");
        List<SubcontractingItem> materialItems = iSubcontractingItemService.selectSubcontractingItemList(item1);
        if (materialItems == null || materialItems.isEmpty()) {
            throw new RuntimeException("请先配置特征项");
        }
        Map<String, SubcontractingItem> params = new HashMap<>();
        materialItems.forEach(item -> {
            if ("Y".equals(item.getIsMain())) {
                params.put(item.getHostId(), item);
            }
        });
        Map<String, MajorSubcontractingFeatureValue> mtrMap = new HashMap<>();
        if (mtrFeatureValues != null && !mtrFeatureValues.isEmpty()) {
            mtrFeatureValues.forEach(mtrFeatureValue -> {
                mtrMap.put(mtrFeatureValue.getId(), mtrFeatureValue);
            });
        }
        if (eigenvalues.isEmpty()) {
            //初始化-最开始无数据情况
            if (mtrFeatureValues != null && !mtrFeatureValues.isEmpty()) {
                mtrFeatureValues.forEach(mtrFeatureValue -> {
                    SubcontractingEigenvalue bean = new SubcontractingEigenvalue();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeatureValue.getMajorSubcontractingFeatureId()) == null ? -1L : params.get(mtrFeatureValue.getMajorSubcontractingFeatureId()).getTypeId());
                    bean.setItemId(params.get(mtrFeatureValue.getMajorSubcontractingFeatureId()) == null ? -1L : params.get(mtrFeatureValue.getMajorSubcontractingFeatureId()).getId());
                    bean.setEigenvalueName(mtrFeatureValue.getFeatureValueName());
                    bean.setEigenvalueCode(mtrFeatureValue.getFeatureValueCode());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(subcontractingEigenvalue.getOrganCode());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
                    bean.setHostId(mtrFeatureValue.getId());
                    bean.setDelFlag(mtrFeatureValue.getValid() + "");
                    eigenvalues.add(bean);
                });
                this.saveBatch(eigenvalues);
            }
        } else {
            //有数据的情况，对比主库数据，新增主库存在副库不存在的数据，且非本副库新增至主库数据。（关联数据是否还需要将主库数据同步到副库）
            Map<String, Long> map = new HashMap<>();
            eigenvalues.forEach(item -> {
                if ("Y".equals(item.getIsMain())) {
                    map.put(item.getHostId(), item.getId());
                }
            });
            List<SubcontractingEigenvalue> addList = new ArrayList<>();
            for (String key : mtrMap.keySet()) {
                if (!map.containsKey(key)) {
                    MajorSubcontractingFeatureValue mtrFeatureValue = mtrMap.get(key);
                    SubcontractingEigenvalue bean = new SubcontractingEigenvalue();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeatureValue.getMajorSubcontractingFeatureId()) == null ? -1L : params.get(mtrFeatureValue.getMajorSubcontractingFeatureId()).getTypeId());
                    bean.setItemId(params.get(mtrFeatureValue.getMajorSubcontractingFeatureId()) == null ? -1L : params.get(mtrFeatureValue.getMajorSubcontractingFeatureId()).getId());
                    bean.setEigenvalueName(mtrFeatureValue.getFeatureValueName());
                    bean.setEigenvalueCode(mtrFeatureValue.getFeatureValueCode());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(subcontractingEigenvalue.getOrganCode());
                    bean.setHostId(mtrFeatureValue.getId());
                    bean.setCreateBy(SecurityUtils.getUsername());
                    bean.setCreateTime(DateUtils.getNowDate());
                    bean.setDelFlag(mtrFeatureValue.getValid() + "");
                    eigenvalues.add(bean);
                    addList.add(bean);
                }
            }
            if (!addList.isEmpty()) {
                this.saveBatch(addList);
            }
        }
        return eigenvalues;
    }

    @Override
    public void addTypeByMain(MajorSubcontractingFeatureValue mtrFeatureValue) {
        SubcontractingItem materialItem = new SubcontractingItem();
        materialItem.setHostId(mtrFeatureValue.getMajorSubcontractingFeatureId());
        List<SubcontractingItem> materialItems = iSubcontractingItemService.selectSubcontractingItemList(materialItem);
        //已存在数据排除
        SubcontractingEigenvalue type1 = new SubcontractingEigenvalue();
        type1.setHostId(mtrFeatureValue.getId());
        type1.setDelFlag("0");
        List<SubcontractingEigenvalue> materialTypes1 = baseMapper.selectSubcontractingEigenvalueList(type1);
        Map<String, Long> map = new HashMap<>();
        if (materialTypes1 != null && !materialTypes1.isEmpty()) {
            materialTypes1.forEach(materialType1 -> {
                map.put(materialType1.getOrganCode(), materialType1.getItemId());
            });
        }
        List<SubcontractingEigenvalue> eigenvalues = new ArrayList<>();
        if (materialItems != null && !materialItems.isEmpty()) {
            for (SubcontractingItem item : materialItems) {
                if (map.containsKey(item.getOrganCode()) && (item.getId() + "").equals(map.get(item.getOrganCode()) + "")) {
                    continue;
                }
                SubcontractingEigenvalue bean = new SubcontractingEigenvalue();
                bean.setId(KeyUtils.generateId());
                bean.setTypeId(item.getTypeId());
                bean.setItemId(item.getId());
                bean.setEigenvalueName(mtrFeatureValue.getFeatureValueName());
                bean.setEigenvalueCode(mtrFeatureValue.getFeatureValueCode());
                bean.setCreateId(SecurityUtils.getUserId());
                bean.setIsMain("Y");
                bean.setState(3L);
                bean.setOrganCode(item.getOrganCode());
                bean.setHostId(mtrFeatureValue.getId());
                bean.setCreateBy(SecurityUtils.getUsername());
                bean.setCreateTime(DateUtils.getNowDate());
                bean.setDelFlag(mtrFeatureValue.getValid() + "");
                eigenvalues.add(bean);
            }
        }
        if (!eigenvalues.isEmpty()) {
            this.saveBatch(eigenvalues);
        }
    }


    @Override
    public void updateByHostId(MajorSubcontractingFeatureValue mtrFeatureValue) {
        SubcontractingEigenvalue eigenvalue = new SubcontractingEigenvalue();
        eigenvalue.setHostId(mtrFeatureValue.getId());
        eigenvalue.setDelFlag("0");
        List<SubcontractingEigenvalue> eigenvalues = baseMapper.selectSubcontractingEigenvalueList(eigenvalue);
        if (eigenvalues != null && !eigenvalues.isEmpty()) {
            eigenvalues.forEach(bean -> {
                bean.setEigenvalueName(mtrFeatureValue.getFeatureValueName());
                bean.setEigenvalueCode(mtrFeatureValue.getFeatureValueCode());
            });
        }
        this.updateBatchById(eigenvalues);
    }

    @Override
    public void deleteByHostId(String[] histIds, Map<Long, Long> idsMap) {
        if (histIds != null) {
            QueryWrapper<SubcontractingEigenvalue> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("del_flag", "0");
            queryWrapper.in("host_id", Arrays.asList(histIds));
            List<SubcontractingEigenvalue> eigenvalues = this.list(queryWrapper);
            //查询新增至主库时不同级或者编码重复的数据
            QueryWrapper<SubcontractingEigenvalue> qw = new QueryWrapper<>();
            qw.eq("del_flag", "0");
            qw.in("main_id", Arrays.asList(histIds));
            qw.eq("is_main","Y");
            eigenvalues.addAll(this.list(qw));
            List<SubcontractingEigenvalue> newDeviceDetails = new ArrayList<>();
            List<SubcontractingEigenvalue> upDeviceDetails = new ArrayList<>();
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
            List<Long> collect = newDeviceDetails.stream().map(SubcontractingEigenvalue::getId).collect(Collectors.toList());
            this.removeBatchByIds(collect);
        }
    }

    /**
     * 新增至主库
     *
     * @param materialEigenvalue
     * @return
     */
    @Override
    public synchronized int addToMain(SubcontractingEigenvalue materialEigenvalue) {
        if (materialEigenvalue.getId() == null) {
            throw new BusinessException("请选择要新增至主库的数据");
        }
        if (materialEigenvalue.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        SubcontractingEigenvalue materialType1 = this.selectSubcontractingEigenvalueById(materialEigenvalue.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法再次新增");
        }
        MajorSubcontractingFeature mtrClass = iMajorSubcontractingFeatureService.selectMajorSubcontractingFeatureById(materialEigenvalue.getHostId());
        //查询编码是否已在主库存在
        MajorSubcontractingFeatureValue mtrClass1 = new MajorSubcontractingFeatureValue();
        mtrClass1.setMajorSubcontractingFeatureId(materialEigenvalue.getHostId());
        mtrClass1.setValid(0L);
        mtrClass1.setFeatureValueCode(materialType1.getEigenvalueCode());
        if (!StringUtils.isEmpty(materialEigenvalue.getEigenvalueCode())) {
            mtrClass1.setFeatureValueCode(materialEigenvalue.getEigenvalueCode());
        }
        List<MajorSubcontractingFeatureValue> mtrClasses = iMajorSubcontractingFeatureValueService.selectMajorSubcontractingFeatureValueList(mtrClass1);
        String key = KeyUtils.generateId() + "";
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            return -1;
        } else {
            if (!StringUtils.isEmpty(materialEigenvalue.getEigenvalueCode())) {
                MajorSubcontractingFeatureValue aClass = new MajorSubcontractingFeatureValue();
                aClass.setId(key);
                aClass.setMajorSubcontractingFeatureId(mtrClass.getId());
                aClass.setFeatureValueCode(materialEigenvalue.getEigenvalueCode());
                aClass.setFeatureValueName(materialType1.getEigenvalueName());
                aClass.setSonId(materialType1.getId());
                aClass.setCreateBy(SecurityUtils.getUsername());
                aClass.setCreateTime(DateUtils.getNowDate());
                aClass.setCreateId(SecurityUtils.getUserId() + "");
                iMajorSubcontractingFeatureValueService.insertMajorSubcontractingFeatureValue(aClass);
                materialType1.setMainId(aClass.getId());
                materialType1.setIsMain("Y");
                return baseMapper.updateSubcontractingEigenvalue(materialType1);
            } else {
                //不存在时 新增至主库
                materialType1.setIsMain("Y");
                materialType1.setHostId(key);
                int i = baseMapper.updateSubcontractingEigenvalue(materialType1);
                if (i > 0) {
                    MajorSubcontractingFeatureValue aClass = new MajorSubcontractingFeatureValue();
                    aClass.setId(key);
                    aClass.setMajorSubcontractingFeatureId(mtrClass.getId());
                    aClass.setFeatureValueCode(materialType1.getEigenvalueCode());
                    aClass.setFeatureValueName(materialType1.getEigenvalueName());
                    aClass.setSonId(materialType1.getId());
                    aClass.setCreateBy(SecurityUtils.getUsername());
                    aClass.setCreateTime(DateUtils.getNowDate());
                    aClass.setCreateId(SecurityUtils.getUserId() + "");
                    iMajorSubcontractingFeatureValueService.insertMajorSubcontractingFeatureValue(aClass);
                }
                return i;
            }
        }
    }


    /**
     * 关联至主库
     *
     * @param materialEigenvalue
     * @return
     */
    @Override
    public synchronized int associationToMain(SubcontractingEigenvalue materialEigenvalue) {
        if (materialEigenvalue.getId() == null) {
            throw new BusinessException("请选择要关联至主库的数据");
        }
        if (materialEigenvalue.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        SubcontractingEigenvalue materialType1 = this.selectSubcontractingEigenvalueById(materialEigenvalue.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法进行关联");
        }
        if (!"0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据已关联至主库，无法再次关联");
        }
        materialType1.setMainId(materialEigenvalue.getHostId());
        return baseMapper.updateSubcontractingEigenvalue(materialType1);
    }


    /**
     * 取消关联至主库
     *
     * @param materialEigenvalue
     * @return
     */
    @Override
    public synchronized int unAssociationToMain(SubcontractingEigenvalue materialEigenvalue) {
        if (materialEigenvalue.getId() == null) {
            throw new BusinessException("请选择要取消关联至主库的数据");
        }
        SubcontractingEigenvalue materialType1 = this.selectSubcontractingEigenvalueById(materialEigenvalue.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据为主库同步数据，无法进行操作");
        }
        if ("0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据尚未关联至主库，无需取消关联");
        }
        materialType1.setMainId("0");
        return baseMapper.updateSubcontractingEigenvalue(materialType1);
    }

    @Override
    public long selectSubcontractingEigenvalueListCount(SubcontractingEigenvalue subcontractingEigenvalue) {
        return baseMapper.selectSubcontractingEigenvalueListCount(subcontractingEigenvalue);
    }

    @Override
    public List<SubcontractingEigenvalue> getProcessed(String organCode) {
        return baseMapper.getProcessed(organCode);
    }


}
