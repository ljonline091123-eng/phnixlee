package com.zhaocai.archives.dossier.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.common.exception.BusinessException;
import com.zhaocai.archives.dossier.domain.LabourEigenvalue;
import com.zhaocai.archives.dossier.domain.LabourItem;
import com.zhaocai.archives.dossier.domain.LabourType;
import com.zhaocai.archives.dossier.domain.MaterialType;
import com.zhaocai.archives.dossier.mapper.LabourEigenvalueMapper;
import com.zhaocai.archives.dossier.mapper.LabourItemMapper;
import com.zhaocai.archives.dossier.service.ILabourItemService;
import com.zhaocai.archives.dossier.service.ILabourTypeService;
import com.zhaocai.archives.main.domain.LaborServicesClass;
import com.zhaocai.archives.main.domain.LaborServicesFeature;
import com.zhaocai.archives.main.service.ILaborServicesClassService;
import com.zhaocai.archives.main.service.ILaborServicesFeatureService;
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
 * 劳务特征项Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class LabourItemServiceImpl extends ServiceImpl<LabourItemMapper, LabourItem> implements ILabourItemService {
    @Autowired
    private LabourItemMapper labourItemMapper;

    @Autowired
    private LabourEigenvalueMapper labourEigenvalueMapper;

    @Resource
    private ILabourTypeService iLabourTypeService;

    @Resource
    private ILaborServicesFeatureService iLaborServicesFeatureService;


    @Resource
    private ILaborServicesClassService laborServicesClassService;

    /**
     * 查询劳务特征项
     *
     * @param id 劳务特征项主键
     * @return 劳务特征项
     */
    @Override
    public LabourItem selectLabourItemById(Long id) {
        LabourItem labourItem = labourItemMapper.selectLabourItemById(id);
//        if ("N".equals(labourItem.getIsMain())) {
//            labourItem.setItemCode(labourItem.getItemCode() + "-" + labourItem.getOrganCode().substring(0, 4));
//        }
        return labourItem;
    }


    /**
     * 查询劳务特征项列表
     *
     * @param labourItem 劳务特征项
     * @return 劳务特征项
     */
    @Override
    public List<LabourItem> selectLabourItemListNoChange(LabourItem labourItem) {
        return labourItemMapper.selectLabourItemList(labourItem);
    }


    /**
     * 查询劳务特征项列表
     *
     * @param labourItem 劳务特征项
     * @return 劳务特征项
     */
    @Override
    public List<LabourItem> selectLabourItemList(LabourItem labourItem) {
        if (labourItem == null) {
            labourItem = new LabourItem();
        }
        labourItem.setDelFlag("0");
        List<LabourItem> labourItems = labourItemMapper.selectLabourItemList(labourItem);
        labourItems.forEach(item -> {
            if ("N".equals(item.getIsMain())) {
                item.setItemCode(item.getItemCode() + "-" + item.getOrganCode().substring(0, 4));
            }
        });
        Map<Long, Long> idsMap = new HashMap<>();
        if (MaterialType.ALL.equals(labourItem.getQueryType()) || MaterialType.UNTREATED.equals(labourItem.getQueryType())) {
            //查询未处理的项数据
            LabourItem materialItem1 = new LabourItem();
            materialItem1.setState(3L);
            materialItem1.setDelFlag("0");
            materialItem1.setIsMain("N");
            materialItem1.setMainId("0");
            materialItem1.setOrganCode(labourItem.getOrganCode());
            List<LabourItem> materialItems1 = this.selectLabourItemListNoChange(materialItem1);
            materialItems1.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });

            LabourEigenvalue eigenvalue = new LabourEigenvalue();
            eigenvalue.setState(3L);
            eigenvalue.setDelFlag("0");
            eigenvalue.setIsMain("N");
            eigenvalue.setMainId("0");
            eigenvalue.setTypeId(labourItem.getTypeId());
            eigenvalue.setOrganCode(labourItem.getOrganCode());
            List<LabourEigenvalue> list = labourEigenvalueMapper.selectLabourEigenvalueList(eigenvalue);
            list.forEach(item -> {
                idsMap.put(item.getItemId(), item.getItemId());
            });
        }
        if (MaterialType.ALL.equals(labourItem.getQueryType()) || MaterialType.PROCESSED.equals(labourItem.getQueryType())) {
            List<LabourEigenvalue> eigenvalues = labourEigenvalueMapper.getProcessed(labourItem.getOrganCode());
            eigenvalues.forEach(item -> {
                idsMap.put(item.getItemId(), item.getItemId());
            });
            List<LabourItem> processed = baseMapper.getProcessed(labourItem.getOrganCode());
            //查询已处理的项数据
            processed.forEach(item -> {
                idsMap.put(item.getId(), item.getId());
            });
        }
        if (StringUtils.isEmpty(labourItem.getQueryType())) {
            return labourItems;
        } else {
            labourItems.removeIf(item -> !idsMap.containsKey(item.getId()));
        }
        return labourItems;

    }

    /**
     * 新增劳务特征项
     *
     * @param labourItem 劳务特征项
     * @return 结果
     */
    @Override
    public synchronized int insertLabourItem(LabourItem labourItem) {
        if (labourItem == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(labourItem.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (labourItem.getTypeId() == null) {
            throw new RuntimeException("类型不能为空");
        }
        LabourItem materialItem1 = new LabourItem();
        materialItem1.setTypeId(labourItem.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(labourItem.getOrganCode());
        materialItem1.setItemCode(labourItem.getItemCode());
        materialItem1.setIsMain("N");
        List<LabourItem> materialItems = baseMapper.selectLabourItemList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            throw new RuntimeException("特征编码已存在");
        }
        if (labourItem.getId() == null) {
            labourItem.setId(KeyUtils.generateId());
            labourItem.setCreateId(SecurityUtils.getUserId());
            labourItem.setCreateBy(SecurityUtils.getUsername());
            labourItem.setCreateTime(DateUtils.getNowDate());
            labourItem.setState(0L);
            labourItem.setIsMain("N");
//            labourItem.setDeptId(SecurityUtils.getSysUser().getDeptId());
        }
        labourItem.setCreateTime(DateUtils.getNowDate());
        return labourItemMapper.insertLabourItem(labourItem);
    }

    /**
     * 修改劳务特征项
     *
     * @param labourItem 劳务特征项
     * @return 结果
     */
    @Override
    public int updateLabourItem(LabourItem labourItem) {
        if (labourItem == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(labourItem.getOrganCode())) {
            throw new RuntimeException("机构编码不能为空");
        }
        if (labourItem.getTypeId() == null) {
            throw new RuntimeException("类型不能为空");
        }
//        String qc = "-"+labourItem.getOrganCode().substring(0, 4);
//        labourItem.setItemCode(labourItem.getItemCode().replace(qc, ""));
        LabourItem materialItem1 = new LabourItem();
        materialItem1.setTypeId(labourItem.getTypeId());
        materialItem1.setDelFlag("0");
        materialItem1.setOrganCode(labourItem.getOrganCode());
        materialItem1.setItemCode(labourItem.getItemCode());
        materialItem1.setIsMain("N");
        List<LabourItem> materialItems = baseMapper.selectLabourItemList(materialItem1);
        if (materialItems != null && !materialItems.isEmpty()) {
            for (LabourItem item : materialItems) {
                if (!labourItem.getTypeId().equals(item.getTypeId())) {
                    throw new RuntimeException("特征编码已存在");
                }
            }
        }
        labourItem.setUpdateTime(DateUtils.getNowDate());
        labourItem.setUpdateId(SecurityUtils.getUserId());
        labourItem.setUpdateBy(SecurityUtils.getUsername());
        labourItem.setUpdateTime(DateUtils.getNowDate());
        return labourItemMapper.updateLabourItem(labourItem);
    }

    /**
     * 批量删除劳务特征项
     *
     * @param ids 需要删除的劳务特征项主键
     * @return 结果
     */
    @Override
    public boolean deleteLabourItemByIds(Long[] ids) {
        if (ids == null) {
            throw new RuntimeException("参数为空");
        }
        List<LabourItem> materialItems = labourItemMapper.selectBatchIds(Arrays.asList(ids));
        if (materialItems != null && !materialItems.isEmpty()) {
            for (LabourItem materialItem : materialItems) {
                if ("Y".equals(materialItem.getIsMain())) {
                    throw new RuntimeException("主库数据不允许删除");
                }
                if (materialItem.getState() != 0L) {
                    throw new RuntimeException("流程中数据不允许删除");
                }
            }
            labourEigenvalueMapper.updateDelByItemId(ids);
        }
        return this.removeBatchByIds(Arrays.asList(ids));
    }

    /**
     * 删除劳务特征项信息
     *
     * @param id 劳务特征项主键
     * @return 结果
     */
    @Override
    public int deleteLabourItemById(Long id) {
        return labourItemMapper.deleteLabourItemById(id);
    }

    @Override
    public List<LabourItem> initData(LabourItem labourItem) {
        List<LabourItem> materialItems = labourItemMapper.selectLabourItemList(labourItem);
        List<LaborServicesFeature> mtrFeatures = iLaborServicesFeatureService.selectLaborServicesFeatureList(null);
        LabourType type1 = new LabourType();
        type1.setOrganCode(labourItem.getOrganCode());
        type1.setIsMain("Y");
        List<LabourType> materialTypes = iLabourTypeService.selectLabourTypeList(type1);
        if (materialTypes == null || materialTypes.isEmpty()) {
            throw new RuntimeException("请先配置劳务类型");
        }
        Map<String, Long> params = new HashMap<>();
        materialTypes.forEach(item -> {
            if ("Y".equals(item.getIsMain())) {
                params.put(item.getHostId(), item.getId());
            }
        });
        Map<String, LaborServicesFeature> mtrMap = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(mtrFeature -> {
                mtrMap.put(mtrFeature.getId(), mtrFeature);
            });
        }
        if (materialItems == null || materialItems.isEmpty()) {
            //初始化-最开始无数据情况
            if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
                mtrFeatures.forEach(mtrFeature -> {
                    LabourItem bean = new LabourItem();
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeature.getLaborServicesClassId()));
                    bean.setItemName(mtrFeature.getFeatureName());
                    bean.setItemCode(mtrFeature.getFeatureCode());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(labourItem.getOrganCode());
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
            List<LabourItem> addList = new ArrayList<>();
            for (String key : mtrMap.keySet()) {
                if (!items.containsKey(key)) {
                    LaborServicesFeature mtrFeature = mtrMap.get(key);
                    LabourItem bean = new LabourItem();
                    bean.setId(KeyUtils.generateId());
                    bean.setId(KeyUtils.generateId());
                    bean.setTypeId(params.get(mtrFeature.getLaborServicesClassId()));
                    bean.setItemName(mtrFeature.getFeatureName());
                    bean.setItemCode(mtrFeature.getFeatureCode());
                    bean.setCreateId(SecurityUtils.getUserId());
                    bean.setIsMain("Y");
                    bean.setState(3L);
                    bean.setOrganCode(labourItem.getOrganCode());
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
    public void addTypeByMain(LaborServicesFeature mtrFeature) {
        LabourType materialType = new LabourType();
        materialType.setHostId(mtrFeature.getLaborServicesClassId());
        List<LabourType> materialTypes = iLabourTypeService.selectLabourTypeList(materialType);
        //已存在数据排除
        LabourItem type1 = new LabourItem();
        type1.setHostId(mtrFeature.getId());
        type1.setDelFlag("0");
        List<LabourItem> materialTypes1 = baseMapper.selectLabourItemList(type1);
        Map<String, Long> map = new HashMap<>();
        if (materialTypes1 != null && !materialTypes1.isEmpty()) {
            materialTypes1.forEach(materialType1 -> {
                map.put(materialType1.getOrganCode(), materialType1.getTypeId());
            });
        }
        List<LabourItem> addList = new ArrayList<>();
        if (materialTypes != null && !materialTypes.isEmpty()) {
            for (LabourType type : materialTypes) {
                if (map.containsKey(type.getOrganCode()) && (type.getId() + "").equals(map.get(type.getOrganCode()) + "")) {
                    continue;
                }
                LabourItem bean = new LabourItem();
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
    public void updateByHostId(LaborServicesFeature mtrFeature) {
        LabourItem item = new LabourItem();
        item.setHostId(mtrFeature.getId());
        item.setDelFlag("0");
        List<LabourItem> materialItems = baseMapper.selectLabourItemList(item);
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
            QueryWrapper<LabourItem> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("del_flag", "0");
            queryWrapper.in("host_id", Arrays.asList(histIds));
            List<LabourItem> materialTypes = this.list(queryWrapper);
            //查询新增至主库时不同级或者编码重复的数据
            QueryWrapper<LabourItem> qw = new QueryWrapper<>();
            qw.eq("del_flag", "0");
            qw.in("main_id", Arrays.asList(histIds));
            qw.eq("is_main","Y");
            materialTypes.addAll(this.list(qw));
            List<LabourItem> newDeviceDetails = new ArrayList<>();
            List<LabourItem> upDeviceDetails = new ArrayList<>();
            if (idsMap != null && !idsMap.isEmpty()) {
                materialTypes.forEach(item -> {
                    if (idsMap.containsKey(item.getId())) {
                        item.setHostId("0");
                        item.setIsMain("N");
                        item.setMainId("N");
                        upDeviceDetails.add(item);
                    } else {
                        newDeviceDetails.add(item);
                    }
                });
            }
            this.updateBatchById(upDeviceDetails);
            List<Long> collect = newDeviceDetails.stream().map(LabourItem::getId).collect(Collectors.toList());
            this.removeBatchByIds(collect);
        }
    }

    /**
     * 新增至主库
     *
     * @param materialItem
     * @return
     */
    @Override
    public synchronized int addToMain(LabourItem materialItem) {
        if (materialItem.getId() == null) {
            throw new BusinessException("请选择要新增至主库的数据");
        }
        if (materialItem.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        LabourItem materialType1 = this.selectLabourItemById(materialItem.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法再次新增");
        }
        LaborServicesClass mtrClass = laborServicesClassService.selectLaborServicesClassById(materialItem.getHostId());
        //查询编码是否已在主库存在
        LaborServicesFeature mtrClass1 = new LaborServicesFeature();
        mtrClass1.setLaborServicesClassId(materialItem.getHostId());
        mtrClass1.setValid(0L);
        mtrClass1.setFeatureCode(materialType1.getItemCode());
        if (!StringUtils.isEmpty(materialItem.getItemCode())) {
            mtrClass1.setFeatureCode(materialItem.getItemCode());
        }
        List<LaborServicesFeature> mtrClasses = iLaborServicesFeatureService.selectLaborServicesFeatureList(mtrClass1);
        String key = KeyUtils.generateId() + "";
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            return -1;
        } else {
            if (!StringUtils.isEmpty(materialItem.getItemCode())) {
                //已存在变为关联数据
                LaborServicesFeature aClass = new LaborServicesFeature();
                aClass.setId(key);
                aClass.setLaborServicesClassId(mtrClass.getId());
                aClass.setFeatureCode(materialItem.getItemCode());
                aClass.setFeatureName(materialType1.getItemName());
                aClass.setSonId(materialType1.getId());
                aClass.setCreateBy(SecurityUtils.getUsername());
                aClass.setCreateTime(DateUtils.getNowDate());
                aClass.setCreateId(SecurityUtils.getUserId() + "");
                iLaborServicesFeatureService.insertLaborServicesFeature(aClass);
                materialType1.setMainId(key);
                materialType1.setIsMain("Y");
                return baseMapper.updateLabourItem(materialType1);
            } else {
                //不存在时 新增至主库
                materialType1.setIsMain("Y");
                materialType1.setHostId(key);
                int i = baseMapper.updateLabourItem(materialType1);
                if (i > 0) {
                    LaborServicesFeature aClass = new LaborServicesFeature();
                    aClass.setId(key);
                    aClass.setLaborServicesClassId(mtrClass.getId());
                    aClass.setFeatureCode(materialType1.getItemCode());
                    aClass.setFeatureName(materialType1.getItemName());
                    aClass.setSonId(materialType1.getId());
                    aClass.setCreateBy(SecurityUtils.getUsername());
                    aClass.setCreateTime(DateUtils.getNowDate());
                    aClass.setCreateId(SecurityUtils.getUserId() + "");
                    iLaborServicesFeatureService.insertLaborServicesFeature(aClass);
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
    public synchronized int associationToMain(LabourItem materialItem) {
        if (materialItem.getId() == null) {
            throw new BusinessException("请选择要关联至主库的数据");
        }
        if (materialItem.getHostId() == null) {
            throw new BusinessException("请选择主库数据");
        }
        LabourItem materialType1 = this.selectLabourItemById(materialItem.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据已新增至主库，无法进行关联");
        }
        if (!"0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据已关联至主库，无法再次关联");
        }
        materialType1.setMainId(materialItem.getHostId());
        return baseMapper.updateLabourItem(materialType1);
    }


    /**
     * 取消关联至主库
     *
     * @param materialItem
     * @return
     */
    @Override
    public synchronized int unAssociationToMain(LabourItem materialItem) {
        if (materialItem.getId() == null) {
            throw new BusinessException("请选择要取消关联至主库的数据");
        }
        LabourItem materialType1 = this.selectLabourItemById(materialItem.getId());
        if ("Y".equals(materialType1.getIsMain())) {
            throw new BusinessException("当前数据为主库同步数据，无法进行操作");
        }
        if ("0".equals(materialType1.getMainId())) {
            throw new BusinessException("当前数据尚未关联至主库，无需取消关联");
        }
        materialType1.setMainId("0");
        return baseMapper.updateLabourItem(materialType1);
    }

    @Override
    public long selectLabourItemListCount(LabourItem labourItem) {
        return baseMapper.selectLabourItemListCount(labourItem);
    }

    @Override
    public List<LabourItem> getProcessed(String organCode) {
        return baseMapper.getProcessed(organCode);
    }


}
