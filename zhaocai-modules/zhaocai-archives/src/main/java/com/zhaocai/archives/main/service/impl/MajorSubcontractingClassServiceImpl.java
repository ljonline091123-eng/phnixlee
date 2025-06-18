package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.ISubcontractingTypeService;
import com.zhaocai.archives.dossier.tree.SubcontractingTypeTree;
import com.zhaocai.archives.main.domain.*;
import com.zhaocai.archives.main.mapper.MajorSubcontractingClassMapper;
import com.zhaocai.archives.main.service.IMajorSubcontractingArchivesService;
import com.zhaocai.archives.main.service.IMajorSubcontractingClassService;
import com.zhaocai.archives.main.service.IMajorSubcontractingFeatureService;
import com.zhaocai.archives.main.service.IMajorSubcontractingFeatureValueService;
import com.zhaocai.archives.utils.KeyUtils;
import com.zhaocai.common.core.exception.ServiceException;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.PageUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.security.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;

/**
 * 专业分包分类主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class MajorSubcontractingClassServiceImpl extends ServiceImpl<MajorSubcontractingClassMapper, MajorSubcontractingClass> implements IMajorSubcontractingClassService {
    @Autowired
    private MajorSubcontractingClassMapper majorSubcontractingClassMapper;

    @Resource
    private ISubcontractingTypeService iSubcontractingTypeService;
    @Autowired
    private IMajorSubcontractingFeatureService iMajorSubcontractingFeatureService;
    @Autowired
    private IMajorSubcontractingFeatureValueService iMajorSubcontractingFeatureValueService;
    @Autowired
    private IMajorSubcontractingArchivesService iMajorSubcontractingArchivesService;

    /**
     * 查询专业分包分类主
     *
     * @param id 专业分包分类主主键
     * @return 专业分包分类主
     */
    @Override
    public MajorSubcontractingClass selectMajorSubcontractingClassById(String id) {
        MajorSubcontractingClass aClass = majorSubcontractingClassMapper.selectMajorSubcontractingClassById(id);
        if (aClass != null) {
            if (aClass.getParentId() != null && !"0".equals(aClass.getParentId())) {
                MajorSubcontractingClass aClass1 = majorSubcontractingClassMapper.selectMajorSubcontractingClassById(aClass.getParentId());
                aClass.setBelongingLevel(aClass1.getMajorSubcontractingClassName());
            } else {
                aClass.setBelongingLevel("顶级");
            }
        }
        return aClass;
    }

    /**
     * 查询专业分包分类主列表
     *
     * @param majorSubcontractingClass 专业分包分类主
     * @return 专业分包分类主
     */
    @Override
    public List<MajorSubcontractingClass> selectMajorSubcontractingClassList(MajorSubcontractingClass majorSubcontractingClass) {
        if (majorSubcontractingClass == null) {
            majorSubcontractingClass = new MajorSubcontractingClass();
        }
        majorSubcontractingClass.setValid(0L);
        return majorSubcontractingClassMapper.selectMajorSubcontractingClassList(majorSubcontractingClass);
    }

    /**
     * 新增专业分包分类主
     *
     * @param majorSubcontractingClass 专业分包分类主
     * @return 结果
     */
    @Override
    public synchronized int insertMajorSubcontractingClass(MajorSubcontractingClass majorSubcontractingClass) {
        if (majorSubcontractingClass.getId() == null) {
            throw new RuntimeException("请先初始化");
        }
        if (majorSubcontractingClass.getParentId() == null) {
            throw new RuntimeException("父级不能为空");
        }
        //判断code是否已存在
        MajorSubcontractingClass mtrClass1 = new MajorSubcontractingClass();
        mtrClass1.setValid(0L);
        mtrClass1.setMajorSubcontractingClassCode(majorSubcontractingClass.getMajorSubcontractingClassCode());
        List<MajorSubcontractingClass> mtrClasses = this.selectMajorSubcontractingClassList(mtrClass1);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            throw new RuntimeException("编码已存在");
        }
        majorSubcontractingClass.setCreateId(SecurityUtils.getUserId() + "");
        majorSubcontractingClass.setCreateBy(SecurityUtils.getUsername());
        majorSubcontractingClass.setCreateTime(DateUtils.getNowDate());
        majorSubcontractingClass.setValid(0L);
        int i = majorSubcontractingClassMapper.insertMajorSubcontractingClass(majorSubcontractingClass);
        if (i > 0) {
            iSubcontractingTypeService.addTypeByMain(majorSubcontractingClass);
        }
        return i;
    }

    /**
     * 修改专业分包分类主
     *
     * @param majorSubcontractingClass 专业分包分类主
     * @return 结果
     */
    @Override
    public int updateMajorSubcontractingClass(MajorSubcontractingClass majorSubcontractingClass) {
        //判断code是否已存在
        MajorSubcontractingClass mtrClass1 = new MajorSubcontractingClass();
        mtrClass1.setValid(0L);
        mtrClass1.setMajorSubcontractingClassCode(majorSubcontractingClass.getMajorSubcontractingClassCode());
        List<MajorSubcontractingClass> mtrClasses = this.selectMajorSubcontractingClassList(mtrClass1);
        if (mtrClasses != null && !mtrClasses.isEmpty()) {
            for (MajorSubcontractingClass mtrClass2 : mtrClasses) {
                if (!majorSubcontractingClass.getId().equals(mtrClass2.getId())) {
                    throw new RuntimeException("编码已存在");
                }
            }
        }
        majorSubcontractingClass.setUpdateTime(DateUtils.getNowDate());
        int i = majorSubcontractingClassMapper.updateMajorSubcontractingClass(majorSubcontractingClass);
        if (i > 0) {
            iSubcontractingTypeService.updateByHostId(majorSubcontractingClass);
        }
        return i;
    }

    /**
     * 批量删除专业分包分类主
     *
     * @param ids 需要删除的专业分包分类主主键
     * @return 结果
     */
    @Override
    public boolean deleteMajorSubcontractingClassByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<MajorSubcontractingClass> mtrFeatures = this.listByIds(Arrays.asList(ids));
        Map<Long, Long> map = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(item -> {
                int i = baseMapper.getMaterialJoinNoMy(item.getId());
                if (i > 0) {
                    throw new RuntimeException("当前分类下存在数据，无法进行删除");
                }
                item.setValid(System.currentTimeMillis() / 1000L);
                if (item.getSonId() != null) {
                    map.put(item.getSonId(), item.getSonId());
                }
            });
        }
        boolean b = this.updateBatchById(mtrFeatures);
        if (b) {
            iSubcontractingTypeService.deleteByHostId(ids, map);
        }
        return b;
    }

    /**
     * 删除专业分包分类主信息
     *
     * @param id 专业分包分类主主键
     * @return 结果
     */
    @Override
    public int deleteMajorSubcontractingClassById(String id) {
        return majorSubcontractingClassMapper.deleteMajorSubcontractingClassById(id);
    }

    @Override
    public List<SubcontractingTypeTree> getMajorSubcontractingClassTree() {
        MajorSubcontractingClass majorSubcontractingClass = new MajorSubcontractingClass();
        majorSubcontractingClass.setValid(0L);
        PageUtils.clearPage();
        List<MajorSubcontractingClass> select = baseMapper.selectMajorSubcontractingClassList(majorSubcontractingClass);
        Map<String, SubcontractingTypeTree> map = new HashMap<>();
        select.forEach(item -> {
            SubcontractingTypeTree materialTypeTree = new SubcontractingTypeTree();
            materialTypeTree.setId(item.getId() + "");
            materialTypeTree.setLabel(item.getMajorSubcontractingClassName());
            materialTypeTree.setType(item.getMajorSubcontractingClassType());
            materialTypeTree.setCode(item.getMajorSubcontractingClassCode());
            materialTypeTree.setChildren(new ArrayList<>());
            map.put(item.getId(), materialTypeTree);
        });

        // 构建树形结构
        List<SubcontractingTypeTree> list = new ArrayList<>();
        for (MajorSubcontractingClass type : select) {
            SubcontractingTypeTree treeVo = map.get(type.getId());
            if (StringUtils.isEmpty(type.getParentId()) || "0".equals(type.getParentId())) {
                // 根节点，直接添加
                list.add(treeVo);
            } else {
                // 非根节点，找到父节点并添加到其子节点列表中
                SubcontractingTypeTree materialTypeTree = map.get(type.getParentId());
                if (materialTypeTree != null) {
                    materialTypeTree.getChildren().add(treeVo);
                }
            }
        }
        return list;
    }

    @Override
    public MajorSubcontractingClass initCode(MajorSubcontractingClass majorSubcontractingClass) {
        if (majorSubcontractingClass.getId() == null) {
            throw new RuntimeException("id不能为空");
        }
        String materialCode = "";
        MajorSubcontractingClass type = new MajorSubcontractingClass();
        if ("0".equals(majorSubcontractingClass.getId())) {
            String[] strs = {"A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T", "U", "V", "W", "X", "Y", "Z"};
            MajorSubcontractingClass aClassx = new MajorSubcontractingClass();
            aClassx.setParentId("0");
            List<MajorSubcontractingClass> mtrClasses = baseMapper.selectMajorSubcontractingClassList(aClassx);
            Map<String, String> map = new HashMap<>();
            for (MajorSubcontractingClass mtrClass1 : mtrClasses) {
                String s = retainEnglishLetters(mtrClass1.getMajorSubcontractingClassCode());
                map.put(s, s);
            }
            for (String s : strs) {
                if (StringUtils.isEmpty(map.get(s))) {
                    materialCode = s + "1";
                    break;
                }
            }
        } else {
            type = baseMapper.selectMajorSubcontractingClassById(majorSubcontractingClass.getId());
            materialCode = type.getMajorSubcontractingClassCode();
            Integer maxCode = baseMapper.getMaxCode(materialCode, type.getId());
            if (maxCode != null) {
                maxCode += 1;
                //根据规则，长度大于8的流水号有3位
                if (materialCode.length() >= 8) {
                    if (maxCode < 100 && maxCode >= 10) {
                        materialCode = materialCode + "0" + maxCode;
                    } else if (maxCode < 10) {
                        materialCode = materialCode + "00" + maxCode;
                    } else {
                        materialCode = materialCode + maxCode;
                    }
                } else {
                    if (maxCode < 10) {
                        materialCode = materialCode + "0" + maxCode;
                    } else {
                        materialCode = materialCode + maxCode;
                    }
                }
            } else {
                if (materialCode.length() >= 8) {
                    materialCode = materialCode + "001";
                } else {
                    materialCode = materialCode + "01";
                }
            }
        }
        MajorSubcontractingClass aClass = new MajorSubcontractingClass();
        aClass.setId(KeyUtils.generateId() + "");
        aClass.setParentId(majorSubcontractingClass.getId());
        aClass.setMajorSubcontractingClassCode(materialCode);
        aClass.setCreateId(SecurityUtils.getUserId() + "");
        aClass.setCreateBy(SecurityUtils.getUsername());
        aClass.setCreateTime(DateUtils.getNowDate());
        return aClass;
    }

    @Override
    public long selectMajorSubcontractingClassListCount(MajorSubcontractingClass majorSubcontractingClass) {
        return baseMapper.selectMajorSubcontractingClassListCount(majorSubcontractingClass);
    }

    /**
     * 保留字符串中的英文字符
     *
     * @param input 输入字符串
     * @return 只包含英文字符的字符串
     */
    public static String retainEnglishLetters(String input) {
        if (input == null || input.isEmpty()) {
            return input; // 处理空字符串或 null 的情况
        }
        return input.replaceAll("[^a-zA-Z]", ""); // 保留 a-z 和 A-Z 范围内的字符
    }




    @Override
    public  String importData(List<MajorSubcontractingClassExcelData> userList, boolean updateSupport, String operName) {
        if (StringUtils.isNull(userList) || userList.size() == 0) {
            throw new ServiceException("导入数据不能为空！");
        }
        MajorSubcontractingClass mtrClass = new MajorSubcontractingClass();
        mtrClass.setValid(0L);
        List<MajorSubcontractingClass> mtrClasses = majorSubcontractingClassMapper.selectMajorSubcontractingClassList(mtrClass);
        Map<String, MajorSubcontractingClass> map = new HashMap<>();
        mtrClasses.forEach(item -> {
            map.put(item.getMajorSubcontractingClassCode(), item);
        });
        MajorSubcontractingFeature mtrFeature = new MajorSubcontractingFeature();
        mtrFeature.setValid(0L);
        Map<String, MajorSubcontractingFeature> featureMap = new HashMap<>();
        List<MajorSubcontractingFeature> mtrFeatures = iMajorSubcontractingFeatureService.selectMajorSubcontractingFeatureList(mtrFeature);
        mtrFeatures.forEach(item -> {
            featureMap.put(item.getMajorSubcontractingClassId() + "|" + item.getFeatureCode(), item);
        });
        MajorSubcontractingFeatureValue mtrFeatureValue = new MajorSubcontractingFeatureValue();
        mtrFeatureValue.setValid(0L);
        Map<String, MajorSubcontractingFeatureValue> valMap = new HashMap<>();
        List<MajorSubcontractingFeatureValue> mtrFeatureValues = iMajorSubcontractingFeatureValueService.selectMajorSubcontractingFeatureValueList(mtrFeatureValue);
        mtrFeatureValues.forEach(item -> {
            valMap.put(item.getMajorSubcontractingFeatureId() + "|" + item.getFeatureValueCode(), item);
        });
        MajorSubcontractingArchives mtrArchives1 = new MajorSubcontractingArchives();
        mtrArchives1.setValid(0L);
        Map<String, MajorSubcontractingArchives> mtrArchivesMap = new HashMap<>();
        List<MajorSubcontractingArchives> mtrArchives = iMajorSubcontractingArchivesService.selectMajorSubcontractingArchivesList(mtrArchives1);
        mtrArchives.forEach(item -> {
            mtrArchivesMap.put(item.getMajorSubcontractingClassId() + "|" + item.getMajorSubcontractingCode(), item);
        });
        List<MajorSubcontractingClass> addMtrClasses = new ArrayList<>();
        List<MajorSubcontractingFeature> mtrFeatureList = new ArrayList<>();
        List<MajorSubcontractingFeatureValue> mtrFeatureValueList = new ArrayList<>();
        List<MajorSubcontractingArchives> mtrArchivesList = new ArrayList<>();
        int errIndex = 1;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (MajorSubcontractingClassExcelData data : userList) {
            errIndex++;
            //判空
            if (StringUtils.isEmpty(data.getParentCode())) {
                failureMsg.append("<br/>第" + errIndex + "行，父级专业分包分类编码为空");
            }
            if (StringUtils.isEmpty(data.getMajorSubcontractingClassCode())) {
                failureMsg.append("<br/>第" + errIndex + "行，专业分包分类编码为空");
            }
            if (StringUtils.isEmpty(data.getMajorSubcontractingClassName())) {
                failureMsg.append("<br/>第" + errIndex + "行，专业分包分类名称为空");
            }
            if (!StringUtils.isEmpty(failureMsg.toString())) {
                continue;
            }
            if ("0".equals(data.getParentCode())) {
                if (map.containsKey(data.getMajorSubcontractingCode())) {
                    String id = map.get(data.getMajorSubcontractingCode()).getId();
                    String name = map.get(data.getMajorSubcontractingCode()).getMajorSubcontractingClassName();
                    //判断是否有特征值、特征项、详细档案
                    if (!StringUtils.isEmpty(data.getFeatureCode()) || !StringUtils.isEmpty(data.getFeatureValueCode()) || !StringUtils.isEmpty(data.getMajorSubcontractingCode())) {
                        if (!StringUtils.isEmpty(data.getFeatureCode())) {
                            if (featureMap.containsKey(id + "|" + data.getFeatureCode())) {
                                if (!StringUtils.isEmpty(data.getFeatureValueCode())) {
                                    String id1 = featureMap.get(id + "|" + data.getFeatureCode()).getId();
                                    if (valMap.containsKey(id1 + "|" + data.getFeatureValueCode())) {
                                        failureMsg.append("<br/>第" + errIndex + "行，特征值已存在");
                                    } else {
                                        MajorSubcontractingFeatureValue mtrFeatureValue1 = new MajorSubcontractingFeatureValue();
                                        mtrFeatureValue1.setId(KeyUtils.generateId() + "");
                                        mtrFeatureValue1.setMajorSubcontractingFeatureId(id1);
                                        mtrFeatureValue1.setFeatureValueName(data.getFeatureValueCode());
                                        mtrFeatureValue1.setFeatureValueCode(data.getFeatureValueCode());
                                        mtrFeatureValue1.setCreateId(SecurityUtils.getUserId() + "");
                                        mtrFeatureValue1.setCreateBy(SecurityUtils.getUsername());
                                        mtrFeatureValue1.setCreateTime(DateUtils.getNowDate());
                                        mtrFeatureValueList.add(mtrFeatureValue1);
                                        valMap.put(id1 + "|" + data.getFeatureValueCode(), mtrFeatureValue1);
                                    }
                                } else {
                                    failureMsg.append("<br/>第" + errIndex + "行，特征项已存在");
                                }
                            } else {
                                MajorSubcontractingFeature mtrFeature1 = new MajorSubcontractingFeature();
                                mtrFeature1.setId(KeyUtils.generateId() + "");
                                mtrFeature1.setMajorSubcontractingClassId(id);
                                mtrFeature1.setFeatureCode(data.getFeatureCode());
                                mtrFeature1.setFeatureName(data.getFeatureName());
                                mtrFeature1.setCreateId(SecurityUtils.getUserId() + "");
                                mtrFeature1.setCreateBy(SecurityUtils.getUsername());
                                mtrFeature1.setCreateTime(DateUtils.getNowDate());
                                mtrFeatureList.add(mtrFeature1);
                                featureMap.put(id + "|" + data.getFeatureCode(), mtrFeature1);
                                if (!StringUtils.isEmpty(data.getFeatureValueCode())) {
                                    MajorSubcontractingFeatureValue mtrFeatureValue1 = new MajorSubcontractingFeatureValue();
                                    mtrFeatureValue1.setId(KeyUtils.generateId() + "");
                                    mtrFeatureValue1.setMajorSubcontractingFeatureId(mtrFeature1.getId());
                                    mtrFeatureValue1.setFeatureValueName(data.getFeatureValueCode());
                                    mtrFeatureValue1.setFeatureValueCode(data.getFeatureValueCode());
                                    mtrFeatureValue1.setCreateId(SecurityUtils.getUserId() + "");
                                    mtrFeatureValue1.setCreateBy(SecurityUtils.getUsername());
                                    mtrFeatureValue1.setCreateTime(DateUtils.getNowDate());
                                    mtrFeatureValueList.add(mtrFeatureValue1);
                                    valMap.put(mtrFeature1.getId() + "|" + data.getFeatureValueCode(), mtrFeatureValue1);
                                }
                            }
                        }
                        if (!StringUtils.isEmpty(data.getMajorSubcontractingCode())) {
                            if (mtrArchivesMap.containsKey(id + "|" + data.getMajorSubcontractingCode())) {
                                failureMsg.append("<br/>第" + errIndex + "行，具体档案已存在");
                            } else {
                                MajorSubcontractingArchives mtrArchives2 = new MajorSubcontractingArchives();
                                mtrArchives2.setId(KeyUtils.generateId() + "");
                                mtrArchives2.setMajorSubcontractingClassId(id);
                                mtrArchives2.setMajorSubcontractingCode(data.getMajorSubcontractingCode());
                                mtrArchives2.setMajorSubcontractingName(data.getMajorSubcontractingName());
                                mtrArchives2.setMajorSubcontractingClassName(name);
                                mtrArchives2.setMeasureUnit(data.getUnit());
                                mtrArchives2.setSpecs(data.getSpecs());
                                mtrArchives2.setFeature(data.getFeature());
                                mtrArchives2.setMetrologicalRules(data.getMetrologicalRules());
                                mtrArchives2.setBasicJob(data.getBasicJob());
                                mtrArchives2.setCreateId(SecurityUtils.getUserId() + "");
                                mtrArchives2.setCreateBy(SecurityUtils.getUsername());
                                mtrArchives2.setCreateTime(DateUtils.getNowDate());
                                mtrArchivesList.add(mtrArchives2);
                                mtrArchivesMap.put(id + "|" + data.getMajorSubcontractingCode(), mtrArchives2);
                            }
                        }
                    } else {
                        failureMsg.append("<br/>第" + errIndex + "行，材料分类已存在");
                    }
                } else {
                    MajorSubcontractingClass mtrClass1 = new MajorSubcontractingClass();
                    mtrClass1.setValid(0L);
                    mtrClass1.setId(KeyUtils.generateId() + "");
                    mtrClass1.setParentId(data.getParentCode());
                    mtrClass1.setMajorSubcontractingClassCode(data.getMajorSubcontractingClassCode());
                    mtrClass1.setMajorSubcontractingClassName(data.getMajorSubcontractingClassName());
                    mtrClass1.setMeasureUnit(data.getMeasureUnit());
                    mtrClass1.setCreateId(SecurityUtils.getUserId() + "");
                    mtrClass1.setCreateBy(SecurityUtils.getUsername());
                    mtrClass1.setCreateTime(DateUtils.getNowDate());
                    addMtrClasses.add(mtrClass1);
                    map.put(mtrClass1.getMajorSubcontractingClassCode(), mtrClass1);
                }
            } else {
                if (map.containsKey(data.getMajorSubcontractingClassCode())) {
                    String id = map.get(data.getMajorSubcontractingClassCode()).getId();
                    String name = map.get(data.getMajorSubcontractingClassCode()).getMajorSubcontractingClassName();
                    //判断是否有特征值、特征项、详细档案
                    if (!StringUtils.isEmpty(data.getFeatureCode()) || !StringUtils.isEmpty(data.getFeatureValueCode()) || !StringUtils.isEmpty(data.getMajorSubcontractingCode())) {
                        if (!StringUtils.isEmpty(data.getFeatureCode())) {
                            if (featureMap.containsKey(id + "|" + data.getFeatureCode())) {
                                if (!StringUtils.isEmpty(data.getFeatureValueCode())) {
                                    String id1 = featureMap.get(id + "|" + data.getFeatureCode()).getId();
                                    if (valMap.containsKey(id1 + "|" + data.getFeatureValueCode())) {
                                        failureMsg.append("<br/>第" + errIndex + "行，特征值已存在");
                                    } else {
                                        MajorSubcontractingFeatureValue mtrFeatureValue1 = new MajorSubcontractingFeatureValue();
                                        mtrFeatureValue1.setId(KeyUtils.generateId() + "");
                                        mtrFeatureValue1.setMajorSubcontractingFeatureId(id1);
                                        mtrFeatureValue1.setFeatureValueName(data.getFeatureValueCode());
                                        mtrFeatureValue1.setFeatureValueCode(data.getFeatureValueCode());
                                        mtrFeatureValue1.setCreateId(SecurityUtils.getUserId() + "");
                                        mtrFeatureValue1.setCreateBy(SecurityUtils.getUsername());
                                        mtrFeatureValue1.setCreateTime(DateUtils.getNowDate());
                                        mtrFeatureValueList.add(mtrFeatureValue1);
                                        valMap.put(id1 + "|" + data.getFeatureValueCode(), mtrFeatureValue1);
                                    }
                                } else {
                                    failureMsg.append("<br/>第" + errIndex + "行，特征项已存在");
                                }
                            } else {
                                MajorSubcontractingFeature mtrFeature1 = new MajorSubcontractingFeature();
                                mtrFeature1.setId(KeyUtils.generateId() + "");
                                mtrFeature1.setMajorSubcontractingClassId(id);
                                mtrFeature1.setFeatureCode(data.getFeatureCode());
                                mtrFeature1.setFeatureName(data.getFeatureName());
                                mtrFeature1.setCreateId(SecurityUtils.getUserId() + "");
                                mtrFeature1.setCreateBy(SecurityUtils.getUsername());
                                mtrFeature1.setCreateTime(DateUtils.getNowDate());
                                mtrFeatureList.add(mtrFeature1);
                                featureMap.put(id + "|" + data.getFeatureCode(), mtrFeature1);

                                if (!StringUtils.isEmpty(data.getFeatureValueCode())) {
                                    MajorSubcontractingFeatureValue mtrFeatureValue1 = new MajorSubcontractingFeatureValue();
                                    mtrFeatureValue1.setId(KeyUtils.generateId() + "");
                                    mtrFeatureValue1.setMajorSubcontractingFeatureId(mtrFeature1.getId());
                                    mtrFeatureValue1.setFeatureValueName(data.getFeatureValueCode());
                                    mtrFeatureValue1.setFeatureValueCode(data.getFeatureValueCode());
                                    mtrFeatureValue1.setCreateId(SecurityUtils.getUserId() + "");
                                    mtrFeatureValue1.setCreateBy(SecurityUtils.getUsername());
                                    mtrFeatureValue1.setCreateTime(DateUtils.getNowDate());
                                    mtrFeatureValueList.add(mtrFeatureValue1);
                                    valMap.put(mtrFeature1.getId() + "|" + data.getFeatureValueCode(), mtrFeatureValue1);
                                }
                            }
                        }
                        if (!StringUtils.isEmpty(data.getMajorSubcontractingCode())) {
                            if (mtrArchivesMap.containsKey(id + "|" + data.getMajorSubcontractingCode())) {
                                failureMsg.append("<br/>第" + errIndex + "行，具体档案已存在");
                            } else {
                                MajorSubcontractingArchives mtrArchives2 = new MajorSubcontractingArchives();
                                mtrArchives2.setId(KeyUtils.generateId() + "");
                                mtrArchives2.setMajorSubcontractingClassId(id);
                                mtrArchives2.setMajorSubcontractingCode(data.getMajorSubcontractingCode());
                                mtrArchives2.setMajorSubcontractingName(data.getMajorSubcontractingName());
                                mtrArchives2.setMajorSubcontractingClassName(name);
                                mtrArchives2.setMeasureUnit(data.getUnit());
                                mtrArchives2.setSpecs(data.getSpecs());
                                mtrArchives2.setFeature(data.getFeature());
                                mtrArchives2.setMetrologicalRules(data.getMetrologicalRules());
                                mtrArchives2.setBasicJob(data.getBasicJob());
                                mtrArchives2.setCreateId(SecurityUtils.getUserId() + "");
                                mtrArchives2.setCreateBy(SecurityUtils.getUsername());
                                mtrArchives2.setCreateTime(DateUtils.getNowDate());
                                mtrArchivesList.add(mtrArchives2);
                                mtrArchivesMap.put(id + "|" + data.getMajorSubcontractingCode(), mtrArchives2);
                            }
                        }
                    } else {
                        failureMsg.append("<br/>第" + errIndex + "行，材料分类已存在");
                    }
                } else {
                    MajorSubcontractingClass mtrClass1 = new MajorSubcontractingClass();
                    mtrClass1.setValid(0L);
                    mtrClass1.setId(KeyUtils.generateId() + "");
                    mtrClass1.setParentId(map.get(data.getParentCode()).getId());
                    mtrClass1.setMajorSubcontractingClassCode(data.getMajorSubcontractingClassCode());
                    mtrClass1.setMajorSubcontractingClassName(data.getMajorSubcontractingClassName());
                    mtrClass1.setMeasureUnit(data.getMeasureUnit());
                    mtrClass1.setCreateId(SecurityUtils.getUserId() + "");
                    mtrClass1.setCreateBy(SecurityUtils.getUsername());
                    mtrClass1.setCreateTime(DateUtils.getNowDate());
                    addMtrClasses.add(mtrClass1);
                    map.put(mtrClass1.getMajorSubcontractingClassCode(), mtrClass1);
                    if (!StringUtils.isEmpty(data.getFeatureCode())) {
                        MajorSubcontractingFeature mtrFeature1 = new MajorSubcontractingFeature();
                        mtrFeature1.setId(KeyUtils.generateId() + "");
                        mtrFeature1.setMajorSubcontractingClassId(mtrClass1.getId());
                        mtrFeature1.setFeatureCode(data.getFeatureCode());
                        mtrFeature1.setFeatureName(data.getFeatureName());
                        mtrFeature1.setCreateId(SecurityUtils.getUserId() + "");
                        mtrFeature1.setCreateBy(SecurityUtils.getUsername());
                        mtrFeature1.setCreateTime(DateUtils.getNowDate());
                        mtrFeatureList.add(mtrFeature1);
                        featureMap.put(mtrClass1.getId() + "|" + data.getFeatureCode(), mtrFeature1);
                        if (!StringUtils.isEmpty(data.getFeatureValueCode())) {
                            MajorSubcontractingFeatureValue mtrFeatureValue1 = new MajorSubcontractingFeatureValue();
                            mtrFeatureValue1.setId(KeyUtils.generateId() + "");
                            mtrFeatureValue1.setMajorSubcontractingFeatureId(mtrFeature1.getId());
                            mtrFeatureValue1.setFeatureValueName(data.getFeatureValueCode());
                            mtrFeatureValue1.setFeatureValueCode(data.getFeatureValueCode());
                            mtrFeatureValue1.setCreateId(SecurityUtils.getUserId() + "");
                            mtrFeatureValue1.setCreateBy(SecurityUtils.getUsername());
                            mtrFeatureValue1.setCreateTime(DateUtils.getNowDate());
                            mtrFeatureValueList.add(mtrFeatureValue1);
                            valMap.put(mtrFeature1.getId() + "|" + data.getFeatureValueCode(), mtrFeatureValue1);
                        }
                    }
                    if (!StringUtils.isEmpty(data.getMajorSubcontractingCode())) {
                        MajorSubcontractingArchives mtrArchives2 = new MajorSubcontractingArchives();
                        mtrArchives2.setId(KeyUtils.generateId() + "");
                        mtrArchives2.setMajorSubcontractingClassId(mtrClass1.getId());
                        mtrArchives2.setMajorSubcontractingCode(data.getMajorSubcontractingCode());
                        mtrArchives2.setMajorSubcontractingName(data.getMajorSubcontractingName());
                        mtrArchives2.setMajorSubcontractingClassName(data.getMajorSubcontractingClassName());
                        mtrArchives2.setMeasureUnit(data.getUnit());
                        mtrArchives2.setSpecs(data.getSpecs());
                        mtrArchives2.setFeature(data.getFeature());
                        mtrArchives2.setMetrologicalRules(data.getMetrologicalRules());
                        mtrArchives2.setBasicJob(data.getBasicJob());
                        mtrArchives2.setCreateId(SecurityUtils.getUserId() + "");
                        mtrArchives2.setCreateBy(SecurityUtils.getUsername());
                        mtrArchives2.setCreateTime(DateUtils.getNowDate());
                        mtrArchivesList.add(mtrArchives2);
                        mtrArchivesMap.put(mtrClass1.getId() + "|" + data.getMajorSubcontractingCode(), mtrArchives2);
                    }
                }
            }
        }
        if (StringUtils.isEmpty(failureMsg.toString())) {
            if (!addMtrClasses.isEmpty()) {
                for (MajorSubcontractingClass mtrClass1 : addMtrClasses) {
                    majorSubcontractingClassMapper.insertMajorSubcontractingClass(mtrClass1);
                }
            }
            if (!mtrFeatureList.isEmpty()) {
                for (MajorSubcontractingFeature mtrClass1 : mtrFeatureList) {
                    iMajorSubcontractingFeatureService.insertMajorSubcontractingFeature(mtrClass1);
                }
            }
            if (!mtrFeatureValueList.isEmpty()) {
                for (MajorSubcontractingFeatureValue mtrClass1 : mtrFeatureValueList) {
                    iMajorSubcontractingFeatureValueService.insertMajorSubcontractingFeatureValue(mtrClass1);
                }
            }
            if (!mtrArchivesList.isEmpty()) {
                for (MajorSubcontractingArchives mtrClass1 : mtrArchivesList) {
                    iMajorSubcontractingArchivesService.insertMajorSubcontractingArchives(mtrClass1);
                }
            }
        } else {
            failureMsg.insert(0, "很抱歉，导入失败！错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        successMsg.insert(0, "恭喜您，数据已全部导入成功！");
        return successMsg.toString();
    }


}
