package com.zhaocai.archives.main.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.archives.main.domain.MtrClass;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 材料分类主Mapper接口
 *
 * @author lzq
 * @date 2025-01-06
 */
public interface MtrClassMapper extends BaseMapper<MtrClass> {
    /**
     * 查询材料分类主
     *
     * @param id 材料分类主主键
     * @return 材料分类主
     */
    public MtrClass selectMtrClassById(String id);

    /**
     * 查询材料分类主列表
     *
     * @param mtrClass 材料分类主
     * @return 材料分类主集合
     */
    public List<MtrClass> selectMtrClassList(MtrClass mtrClass);

    /**
     * 新增材料分类主
     *
     * @param mtrClass 材料分类主
     * @return 结果
     */
    public int insertMtrClass(MtrClass mtrClass);

    /**
     * 修改材料分类主
     *
     * @param mtrClass 材料分类主
     * @return 结果
     */
    public int updateMtrClass(MtrClass mtrClass);

    /**
     * 删除材料分类主
     *
     * @param id 材料分类主主键
     * @return 结果
     */
    public int deleteMtrClassById(String id);

    /**
     * 批量删除材料分类主
     *
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMtrClassByIds(String[] ids);

    public Integer getMaxCode(@Param("upCode") String upCode, @Param("upId") String upId);

    long selectMtrClassListCount(MtrClass mtrClass);

    int getMaterialJoinNoMy(String id);
}
