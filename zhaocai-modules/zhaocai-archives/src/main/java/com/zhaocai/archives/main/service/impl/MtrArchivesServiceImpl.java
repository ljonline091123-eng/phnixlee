package com.zhaocai.archives.main.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.dossier.service.IMaterialDetailsService;
import com.zhaocai.archives.main.domain.MtrArchives;
import com.zhaocai.archives.main.domain.MtrClass;
import com.zhaocai.archives.main.mapper.MtrArchivesMapper;
import com.zhaocai.archives.main.service.IMtrArchivesService;
import com.zhaocai.archives.main.service.IMtrClassService;
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
 * 材料档案主Service业务层处理
 *
 * @author lzq
 * @date 2025-01-06
 */
@Service
public class MtrArchivesServiceImpl extends ServiceImpl<MtrArchivesMapper, MtrArchives> implements IMtrArchivesService {
    @Autowired
    private MtrArchivesMapper mtrArchivesMapper;

    @Resource
    private IMaterialDetailsService iMaterialDetailsService;

    @Resource
    private IMtrClassService mtrClassService;

    /**
     * 查询材料档案主
     *
     * @param id 材料档案主主键
     * @return 材料档案主
     */
    @Override
    public MtrArchives selectMtrArchivesById(String id) {
        return mtrArchivesMapper.selectMtrArchivesById(id);
    }

    /**
     * 查询材料档案主列表
     *
     * @param mtrArchives 材料档案主
     * @return 材料档案主
     */
    @Override
    public List<MtrArchives> selectMtrArchivesList(MtrArchives mtrArchives) {
        if (mtrArchives == null) {
            mtrArchives = new MtrArchives();
        }
        mtrArchives.setValid(0L);
        List<MtrArchives> mtrArchives1 = mtrArchivesMapper.selectMtrArchivesList(mtrArchives);
        MtrClass aClass = new MtrClass();
        if (mtrArchives.getMtrClassId() != null) {
            aClass = mtrClassService.selectMtrClassById(mtrArchives.getMtrClassId());
        }
        String aClassName = aClass.getMtrClassName();
        if (mtrArchives1 != null && !mtrArchives1.isEmpty()) {
            mtrArchives1.forEach(item -> {
                item.setMtrClassName(aClassName);
            });
        }
        return mtrArchives1;
    }

    /**
     * 新增材料档案主
     *
     * @param mtrArchives 材料档案主
     * @return 结果
     */
    @Override
    public synchronized int insertMtrArchives(MtrArchives mtrArchives) {
        if (mtrArchives == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(mtrArchives.getMtrClassId())) {
            throw new RuntimeException("类型id不能为空");
        }
        MtrArchives mtrFeature1 = new MtrArchives();
        mtrFeature1.setMtrCode(mtrArchives.getMtrCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setMtrClassId(mtrArchives.getMtrClassId());
        List<MtrArchives> mtrFeatures = baseMapper.selectMtrArchivesList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            throw new RuntimeException("当前编号已存在");
        }
        if (mtrArchives.getId() == null) {
            mtrArchives.setId(KeyUtils.generateId() + "");
            mtrArchives.setCreateTime(DateUtils.getNowDate());
            mtrArchives.setCreateId(SecurityUtils.getUserId() + "");
            mtrArchives.setCreateBy(SecurityUtils.getUsername());
            mtrArchives.setValid(0L);
        }
        int i = baseMapper.insertMtrArchives(mtrArchives);
        if (i > 0) {
            iMaterialDetailsService.addByMain(mtrArchives);
        }
        return i;
    }


    /**
     * 修改材料档案主
     *
     * @param mtrArchives 材料档案主
     * @return 结果
     */
    @Override
    public int updateMtrArchives(MtrArchives mtrArchives) {
        if (mtrArchives == null) {
            throw new RuntimeException("参数为空");
        }
        if (StringUtils.isEmpty(mtrArchives.getMtrClassId())) {
            throw new RuntimeException("类型id不能为空");
        }
        MtrArchives mtrFeature1 = new MtrArchives();
        mtrFeature1.setMtrCode(mtrArchives.getMtrCode());
        mtrFeature1.setValid(0L);
        mtrFeature1.setMtrClassId(mtrArchives.getMtrClassId());
        List<MtrArchives> mtrFeatures = baseMapper.selectMtrArchivesList(mtrFeature1);
        if (mtrFeatures != null && !mtrFeatures.isEmpty()) {
            for (MtrArchives mtrFeature2 : mtrFeatures) {
                if (!mtrArchives.getId().equals(mtrFeature2.getId())) {
                    throw new RuntimeException("当前编号已存在");
                }
            }
        }
        mtrArchives.setUpdateTime(DateUtils.getNowDate());
        mtrArchives.setUpdateBy(SecurityUtils.getUsername());
        int i = baseMapper.updateMtrArchives(mtrArchives);
        if (i > 0) {
            iMaterialDetailsService.updateByHostId(mtrArchives);
        }
        return i;
    }

    /**
     * 批量删除材料档案主
     *
     * @param ids 需要删除的材料档案主主键
     * @return 结果
     */
    @Override
    public boolean deleteMtrArchivesByIds(String[] ids) {
        if (ids == null) {
            throw new RuntimeException("id不能为空");
        }
        List<MtrArchives> mtrFeatures = this.listByIds(Arrays.asList(ids));
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
            iMaterialDetailsService.deleteByHostId(ids, map);
        }
        return b;
    }

    /**
     * 删除材料档案主信息
     *
     * @param id 材料档案主主键
     * @return 结果
     */
    @Override
    public int deleteMtrArchivesById(String id) {
        return mtrArchivesMapper.deleteMtrArchivesById(id);
    }

    @Override
    public MtrArchives initCode(MtrClass mtrClass) {
        String code = mtrArchivesMapper.getMaxCode(mtrClass.getId());
        MtrArchives mtrArchives = new MtrArchives();
        mtrArchives.setMtrCode(code + 1);
        mtrArchives.setId(KeyUtils.generateId() + "");
        mtrArchives.setCreateBy(SecurityUtils.getUsername());
        mtrArchives.setMtrClassId(mtrClass.getId());
        mtrArchives.setCreateId(SecurityUtils.getUserId() + "");
        mtrArchives.setCreateTime(DateUtils.getNowDate());
        return mtrArchives;
    }

    @Override
    public long selectMtrArchivesListCount(MtrArchives mtrArchives) {
        return baseMapper.selectMtrArchivesListCount(mtrArchives);
    }
}
