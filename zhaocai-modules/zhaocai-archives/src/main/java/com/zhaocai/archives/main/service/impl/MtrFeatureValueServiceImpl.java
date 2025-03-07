package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.IMaterialEigenvalueService;
import com.zhaocai.archives.main.domain.MtrFeature;
import com.zhaocai.archives.main.domain.MtrFeatureValue;
import com.zhaocai.archives.main.mapper.MtrFeatureValueMapper;
import com.zhaocai.archives.main.service.IMtrFeatureValueService;
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
 * 材料特征值主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class MtrFeatureValueServiceImpl extends ServiceImpl<MtrFeatureValueMapper, MtrFeatureValue> implements IMtrFeatureValueService {
    @Autowired
    private MtrFeatureValueMapper mtrFeatureValueMapper;


    @Resource
    private IMaterialEigenvalueService materialEigenvalueService;

    /**
     * 查询材料特征值主
     *
     * @param id 材料特征值主主键
     * @return 材料特征值主
     */
    @Override
    public MtrFeatureValue selectMtrFeatureValueById(String id) {
        return mtrFeatureValueMapper.selectMtrFeatureValueById(id);
    }

    /**
     * 查询材料特征值主列表
     *
     * @param mtrFeatureValue 材料特征值主
     * @return 材料特征值主
     */
    @Override
    public List<MtrFeatureValue> selectMtrFeatureValueList(MtrFeatureValue mtrFeatureValue) {
        if (mtrFeatureValue == null) {
            mtrFeatureValue = new MtrFeatureValue();
        }
        mtrFeatureValue.setValid(0L);
        return mtrFeatureValueMapper.selectMtrFeatureValueList(mtrFeatureValue);
    }

    /**
     * 新增材料特征值主
     *
     * @param mtrFeatureValue 材料特征值主
     * @return 结果
     */
    @Override
    public synchronized int insertMtrFeatureValue(MtrFeatureValue mtrFeatureValue) {
        if (mtrFeatureValue == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(mtrFeatureValue.getMtrFeatureId())) {
            throw new RuntimeException("特征项id不能为空");
        }
        MtrFeatureValue mtrFeatureValue1 = new MtrFeatureValue();
        mtrFeatureValue1.setFeatureValueCode(mtrFeatureValue.getFeatureValueCode());
        mtrFeatureValue1.setValid(0L);
        mtrFeatureValue1.setMtrFeatureId(mtrFeatureValue.getMtrFeatureId());
        List<MtrFeatureValue> mtrFeatureValues = baseMapper.selectMtrFeatureValueList(mtrFeatureValue1);
        if (mtrFeatureValues != null && !mtrFeatureValues.isEmpty()) {
            throw new RuntimeException("当前特征值编号已存在");
        }
        if (mtrFeatureValue.getId() == null) {
            mtrFeatureValue.setId(KeyUtils.generateId() + "");
            mtrFeatureValue.setCreateTime(DateUtils.getNowDate());
            mtrFeatureValue.setCreateId(SecurityUtils.getUserId() + "");
            mtrFeatureValue.setCreateBy(SecurityUtils.getUsername());
            mtrFeatureValue.setValid(0L);
        }
        int i = mtrFeatureValueMapper.insertMtrFeatureValue(mtrFeatureValue);
        if (i > 0) {
            materialEigenvalueService.addByMain(mtrFeatureValue);
        }
        return i;
    }


    /**
     * 修改材料特征值主
     *
     * @param mtrFeatureValue 材料特征值主
     * @return 结果
     */
    @Override
    public int updateMtrFeatureValue(MtrFeatureValue mtrFeatureValue) {
        if (mtrFeatureValue == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(mtrFeatureValue.getMtrFeatureId())) {
            throw new RuntimeException("特征项id不能为空");
        }
        MtrFeatureValue mtrFeature1 = new MtrFeatureValue();
        mtrFeature1.setFeatureValueCode(mtrFeatureValue.getFeatureValueCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setMtrFeatureId(mtrFeatureValue.getMtrFeatureId());
        List<MtrFeatureValue> mtrFeatures = baseMapper.selectMtrFeatureValueList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            for (MtrFeatureValue mtrFeature2 : mtrFeatures) {
                if (!mtrFeatureValue.getId().equals(mtrFeature2.getId())) {
                    throw new RuntimeException("当前编号已存在");
                }
            }
        }
        mtrFeatureValue.setUpdateTime(DateUtils.getNowDate());
        mtrFeatureValue.setUpdateBy(SecurityUtils.getUsername());
        mtrFeatureValue.setIsTb("2");
        int i = mtrFeatureValueMapper.updateMtrFeatureValue(mtrFeatureValue);
        if (i > 0) {
            materialEigenvalueService.updateByHostId(mtrFeatureValue);
        }
        return i;
    }

    /**
     * 批量删除材料特征值主
     *
     * @param ids 需要删除的材料特征值主主键
     * @return 结果
     */
    @Override
    public boolean deleteMtrFeatureValueByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<MtrFeatureValue> mtrFeatures = this.listByIds(Arrays.asList(ids));
        Map<Long, Long> map = new HashMap<>();
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            mtrFeatures.forEach(item -> {
                item.setValid(System.currentTimeMillis() / 1000L);
                item.setIsTb("3");
                if (item.getSonId() != null) {
                    map.put(item.getSonId(), item.getSonId());
                }
            });
        }
        boolean b = this.updateBatchById(mtrFeatures);
        if (b) {
            materialEigenvalueService.deleteByHostId(ids, map);
        }
        return b;
    }

    /**
     * 删除材料特征值主信息
     *
     * @param id 材料特征值主主键
     * @return 结果
     */
    @Override
    public int deleteMtrFeatureValueById(String id) {
        return mtrFeatureValueMapper.deleteMtrFeatureValueById(id);
    }

    @Override
    public MtrFeatureValue initCode(MtrFeature mtrClass) {
        String code = mtrFeatureValueMapper.getMaxCode(mtrClass.getId());
        MtrFeatureValue mtrFeatureValue = new MtrFeatureValue();
        mtrFeatureValue.setFeatureValueCode(code + 1);
        mtrFeatureValue.setMtrFeatureId(mtrClass.getId());
        mtrFeatureValue.setCreateBy(SecurityUtils.getUsername());
        mtrFeatureValue.setCreateId(SecurityUtils.getUserId() + "");
        mtrFeatureValue.setCreateTime(DateUtils.getNowDate());
        return mtrFeatureValue;
    }

    @Override
    public long selectMtrFeatureValueListCount(MtrFeatureValue mtrFeatureValue) {
        return baseMapper.selectMtrFeatureValueListCount(mtrFeatureValue);
    }
}
