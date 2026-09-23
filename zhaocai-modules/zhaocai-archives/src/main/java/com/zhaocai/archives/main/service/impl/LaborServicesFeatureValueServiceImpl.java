package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.ILabourEigenvalueService;
import com.zhaocai.archives.main.domain.LaborServicesFeatureValue;
import com.zhaocai.archives.main.mapper.LaborServicesFeatureValueMapper;
import com.zhaocai.archives.main.service.ILaborServicesFeatureValueService;
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
 * 劳务特征值主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class LaborServicesFeatureValueServiceImpl extends ServiceImpl<LaborServicesFeatureValueMapper, LaborServicesFeatureValue> implements ILaborServicesFeatureValueService {
    @Autowired
    private LaborServicesFeatureValueMapper laborServicesFeatureValueMapper;

    @Resource
    private ILabourEigenvalueService iLabourEigenvalueService;

    /**
     * 查询劳务特征值主
     *
     * @param id 劳务特征值主主键
     * @return 劳务特征值主
     */
    @Override
    public LaborServicesFeatureValue selectLaborServicesFeatureValueById(String id) {
        return laborServicesFeatureValueMapper.selectLaborServicesFeatureValueById(id);
    }

    /**
     * 查询劳务特征值主列表
     *
     * @param laborServicesFeatureValue 劳务特征值主
     * @return 劳务特征值主
     */
    @Override
    public List<LaborServicesFeatureValue> selectLaborServicesFeatureValueList(LaborServicesFeatureValue laborServicesFeatureValue) {
        if (laborServicesFeatureValue == null) {
            laborServicesFeatureValue = new LaborServicesFeatureValue();
        }
        laborServicesFeatureValue.setValid(0L);
        return laborServicesFeatureValueMapper.selectLaborServicesFeatureValueList(laborServicesFeatureValue);
    }

    /**
     * 新增劳务特征值主
     *
     * @param laborServicesFeatureValue 劳务特征值主
     * @return 结果
     */
    @Override
    public synchronized int insertLaborServicesFeatureValue(LaborServicesFeatureValue laborServicesFeatureValue) {
        if (laborServicesFeatureValue == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(laborServicesFeatureValue.getLaborServicesFeatureId())) {
            throw new RuntimeException("特征项id不能为空");
        }
        LaborServicesFeatureValue mtrFeatureValue1 = new LaborServicesFeatureValue();
        mtrFeatureValue1.setFeatureValueCode(laborServicesFeatureValue.getFeatureValueCode());
        mtrFeatureValue1.setValid(0L);
        mtrFeatureValue1.setLaborServicesFeatureId(laborServicesFeatureValue.getLaborServicesFeatureId());
        List<LaborServicesFeatureValue> mtrFeatureValues = baseMapper.selectLaborServicesFeatureValueList(mtrFeatureValue1);
        if (mtrFeatureValues != null && !mtrFeatureValues.isEmpty()) {
            throw new RuntimeException("当前特征值编号已存在");
        }
        if (laborServicesFeatureValue.getId() == null) {
            laborServicesFeatureValue.setId(KeyUtils.generateId() + "");
            laborServicesFeatureValue.setCreateTime(DateUtils.getNowDate());
            laborServicesFeatureValue.setCreateId(SecurityUtils.getUserId() + "");
            laborServicesFeatureValue.setCreateBy(SecurityUtils.getUsername());
            laborServicesFeatureValue.setValid(0L);
        }
        int i = laborServicesFeatureValueMapper.insertLaborServicesFeatureValue(laborServicesFeatureValue);
//        if (i > 0) {
//            iLabourEigenvalueService.addTypeByMain(laborServicesFeatureValue);
//        }
        return i;
    }

    /**
     * 修改劳务特征值主
     *
     * @param laborServicesFeatureValue 劳务特征值主
     * @return 结果
     */
    @Override
    public int updateLaborServicesFeatureValue(LaborServicesFeatureValue laborServicesFeatureValue) {
        if (laborServicesFeatureValue == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(laborServicesFeatureValue.getLaborServicesFeatureId())) {
            throw new RuntimeException("特征项id不能为空");
        }
        LaborServicesFeatureValue mtrFeature1 = new LaborServicesFeatureValue();
        mtrFeature1.setFeatureValueCode(laborServicesFeatureValue.getFeatureValueCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setLaborServicesFeatureId(laborServicesFeatureValue.getLaborServicesFeatureId());
        List<LaborServicesFeatureValue> mtrFeatures = baseMapper.selectLaborServicesFeatureValueList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            for (LaborServicesFeatureValue mtrFeature2 : mtrFeatures) {
                if (!laborServicesFeatureValue.getId().equals(mtrFeature2.getId())) {
                    throw new RuntimeException("当前编号已存在");
                }
            }
        }
        laborServicesFeatureValue.setUpdateTime(DateUtils.getNowDate());
        laborServicesFeatureValue.setUpdateBy(SecurityUtils.getUsername());
        int i = laborServicesFeatureValueMapper.updateLaborServicesFeatureValue(laborServicesFeatureValue);
        if (i > 0) {
            iLabourEigenvalueService.updateByHostId(laborServicesFeatureValue);
        }
        return i;
    }

    /**
     * 批量删除劳务特征值主
     *
     * @param ids 需要删除的劳务特征值主主键
     * @return 结果
     */
    @Override
    public boolean deleteLaborServicesFeatureValueByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<LaborServicesFeatureValue> mtrFeatures = this.listByIds(Arrays.asList(ids));
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
            iLabourEigenvalueService.deleteByHostId(ids,map);
        }
        return b;
    }

    /**
     * 删除劳务特征值主信息
     *
     * @param id 劳务特征值主主键
     * @return 结果
     */
    @Override
    public int deleteLaborServicesFeatureValueById(String id) {
        return laborServicesFeatureValueMapper.deleteLaborServicesFeatureValueById(id);
    }

    @Override
    public long selectLaborServicesFeatureValueListCount(LaborServicesFeatureValue laborServicesFeatureValue) {
        return baseMapper.selectLaborServicesFeatureValueListCount(laborServicesFeatureValue);
    }
}
