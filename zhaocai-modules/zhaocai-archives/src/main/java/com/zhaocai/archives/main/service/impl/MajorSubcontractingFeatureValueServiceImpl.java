package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.ISubcontractingEigenvalueService;
import com.zhaocai.archives.main.domain.MajorSubcontractingFeature;
import com.zhaocai.archives.main.domain.MajorSubcontractingFeatureValue;
import com.zhaocai.archives.main.mapper.MajorSubcontractingFeatureValueMapper;
import com.zhaocai.archives.main.service.IMajorSubcontractingFeatureValueService;
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
 * 专业分包特征值主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class MajorSubcontractingFeatureValueServiceImpl extends ServiceImpl<MajorSubcontractingFeatureValueMapper, MajorSubcontractingFeatureValue> implements IMajorSubcontractingFeatureValueService {
    @Autowired
    private MajorSubcontractingFeatureValueMapper majorSubcontractingFeatureValueMapper;

    @Resource
    private ISubcontractingEigenvalueService iSubcontractingEigenvalueService;

    /**
     * 查询专业分包特征值主
     *
     * @param id 专业分包特征值主主键
     * @return 专业分包特征值主
     */
    @Override
    public MajorSubcontractingFeatureValue selectMajorSubcontractingFeatureValueById(String id) {
        return majorSubcontractingFeatureValueMapper.selectMajorSubcontractingFeatureValueById(id);
    }

    /**
     * 查询专业分包特征值主列表
     *
     * @param majorSubcontractingFeatureValue 专业分包特征值主
     * @return 专业分包特征值主
     */
    @Override
    public List<MajorSubcontractingFeatureValue> selectMajorSubcontractingFeatureValueList(MajorSubcontractingFeatureValue majorSubcontractingFeatureValue) {
        if (majorSubcontractingFeatureValue == null) {
            majorSubcontractingFeatureValue = new MajorSubcontractingFeatureValue();
        }
        majorSubcontractingFeatureValue.setValid(0L);
        return majorSubcontractingFeatureValueMapper.selectMajorSubcontractingFeatureValueList(majorSubcontractingFeatureValue);
    }

    /**
     * 新增专业分包特征值主
     *
     * @param majorSubcontractingFeatureValue 专业分包特征值主
     * @return 结果
     */
    @Override
    public synchronized int insertMajorSubcontractingFeatureValue(MajorSubcontractingFeatureValue majorSubcontractingFeatureValue) {
        if (majorSubcontractingFeatureValue == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(majorSubcontractingFeatureValue.getMajorSubcontractingFeatureId())) {
            throw new RuntimeException("特征项id不能为空");
        }
        MajorSubcontractingFeatureValue mtrFeatureValue1 = new MajorSubcontractingFeatureValue();
        mtrFeatureValue1.setFeatureValueCode(majorSubcontractingFeatureValue.getFeatureValueCode());
        mtrFeatureValue1.setValid(0L);
        mtrFeatureValue1.setMajorSubcontractingFeatureId(majorSubcontractingFeatureValue.getMajorSubcontractingFeatureId());
        List<MajorSubcontractingFeatureValue> mtrFeatureValues = baseMapper.selectMajorSubcontractingFeatureValueList(mtrFeatureValue1);
        if (mtrFeatureValues != null && !mtrFeatureValues.isEmpty()) {
            throw new RuntimeException("当前特征值编号已存在");
        }
        if (majorSubcontractingFeatureValue.getId() == null) {
            majorSubcontractingFeatureValue.setId(KeyUtils.generateId() + "");
            majorSubcontractingFeatureValue.setCreateTime(DateUtils.getNowDate());
            majorSubcontractingFeatureValue.setCreateId(SecurityUtils.getUserId() + "");
            majorSubcontractingFeatureValue.setCreateBy(SecurityUtils.getUsername());
            majorSubcontractingFeatureValue.setValid(0L);
        }
        int i = majorSubcontractingFeatureValueMapper.insertMajorSubcontractingFeatureValue(majorSubcontractingFeatureValue);
//        if (i > 0) {
//            iSubcontractingEigenvalueService.addTypeByMain(majorSubcontractingFeatureValue);
//        }
        return i;
    }

    /**
     * 修改专业分包特征值主
     *
     * @param majorSubcontractingFeatureValue 专业分包特征值主
     * @return 结果
     */
    @Override
    public int updateMajorSubcontractingFeatureValue(MajorSubcontractingFeatureValue majorSubcontractingFeatureValue) {
        if (majorSubcontractingFeatureValue == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(majorSubcontractingFeatureValue.getMajorSubcontractingFeatureId())) {
            throw new RuntimeException("特征项id不能为空");
        }
        MajorSubcontractingFeatureValue mtrFeature1 = new MajorSubcontractingFeatureValue();
        mtrFeature1.setFeatureValueCode(majorSubcontractingFeatureValue.getFeatureValueCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setMajorSubcontractingFeatureId(majorSubcontractingFeatureValue.getMajorSubcontractingFeatureId());
        List<MajorSubcontractingFeatureValue> mtrFeatures = baseMapper.selectMajorSubcontractingFeatureValueList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            for (MajorSubcontractingFeatureValue mtrFeature2 : mtrFeatures) {
                if (!majorSubcontractingFeatureValue.getId().equals(mtrFeature2.getId())) {
                    throw new RuntimeException("当前编号已存在");
                }
            }
        }
        majorSubcontractingFeatureValue.setUpdateTime(DateUtils.getNowDate());
        majorSubcontractingFeatureValue.setUpdateBy(SecurityUtils.getUsername());
        int i = majorSubcontractingFeatureValueMapper.updateMajorSubcontractingFeatureValue(majorSubcontractingFeatureValue);
        if (i > 0) {
            iSubcontractingEigenvalueService.updateByHostId(majorSubcontractingFeatureValue);
        }
        return i;
    }

    /**
     * 批量删除专业分包特征值主
     *
     * @param ids 需要删除的专业分包特征值主主键
     * @return 结果
     */
    @Override
    public boolean deleteMajorSubcontractingFeatureValueByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<MajorSubcontractingFeatureValue> mtrFeatures = this.listByIds(Arrays.asList(ids));
        Map<Long, Long> map = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(item -> {
                item.setValid(System.currentTimeMillis() / 1000L);
                if (item.getSonId() != null) {
                    map.put(item.getSonId(), item.getSonId());
                }
            });
        }
        boolean b = this.updateBatchById(mtrFeatures);
        if (b) {
            iSubcontractingEigenvalueService.deleteByHostId(ids,map);
        }
        return b;
    }

    /**
     * 删除专业分包特征值主信息
     *
     * @param id 专业分包特征值主主键
     * @return 结果
     */
    @Override
    public int deleteMajorSubcontractingFeatureValueById(String id) {
        return majorSubcontractingFeatureValueMapper.deleteMajorSubcontractingFeatureValueById(id);
    }

    @Override
    public MajorSubcontractingFeatureValue initCode(MajorSubcontractingFeature mtrClass) {
        String maxCode = majorSubcontractingFeatureValueMapper.getMaxCode(mtrClass.getId());
        MajorSubcontractingFeatureValue subcontractingFeatureValue = new MajorSubcontractingFeatureValue();
        subcontractingFeatureValue.setFeatureValueCode(maxCode+1);
        subcontractingFeatureValue.setMajorSubcontractingFeatureId(mtrClass.getId());
        subcontractingFeatureValue.setCreateBy(SecurityUtils.getUsername());
        subcontractingFeatureValue.setCreateId(SecurityUtils.getUserId() + "");
        subcontractingFeatureValue.setCreateTime(DateUtils.getNowDate());
        return subcontractingFeatureValue;
    }

    @Override
    public long selectMajorSubcontractingFeatureValueListCount(MajorSubcontractingFeatureValue majorSubcontractingFeatureValue) {
        return baseMapper.selectMajorSubcontractingFeatureValueListCount(majorSubcontractingFeatureValue);
    }
}
