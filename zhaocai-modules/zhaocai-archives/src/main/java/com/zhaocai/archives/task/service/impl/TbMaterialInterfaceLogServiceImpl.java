package com.zhaocai.archives.task.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.archives.task.domain.TbMaterialInterfaceLog;
import com.zhaocai.archives.task.mapper.TbMaterialInterfaceLogMapper;
import com.zhaocai.archives.task.service.TbMaterialInterfaceLogService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * 接口访问记录Service业务层处理
 *
 * @author hyt
 * @date 2023/7/20 11:58
 */
@Service
public class TbMaterialInterfaceLogServiceImpl extends ServiceImpl<TbMaterialInterfaceLogMapper, TbMaterialInterfaceLog> implements TbMaterialInterfaceLogService {

    @Resource
    private TbMaterialInterfaceLogMapper tInterfaceLogMapper;

    /**
     * 查询接口访问记录
     *
     * @param id 接口访问记录主键
     * @return 接口访问记录
     */
    @Override
    public TbMaterialInterfaceLog selectTInterfaceLogById(Long id) {
        return tInterfaceLogMapper.selectTInterfaceLogById(id);
    }

    /**
     * 查询接口访问记录列表
     *
     * @param tInterfaceLog 接口访问记录
     * @return 接口访问记录
     */
    @Override
    public List<TbMaterialInterfaceLog> selectTInterfaceLogList(TbMaterialInterfaceLog tInterfaceLog) {
        return tInterfaceLogMapper.selectLikeList(tInterfaceLog);
    }

    /**
     * 新增接口访问记录
     *
     * @param tInterfaceLog 接口访问记录
     * @return 结果
     */
    @Override
    public int insertTInterfaceLog(TbMaterialInterfaceLog tInterfaceLog) {
        return tInterfaceLogMapper.insertTInterfaceLog(tInterfaceLog);
    }

    /**
     * 修改接口访问记录
     *
     * @param tInterfaceLog 接口访问记录
     * @return 结果
     */
    @Override
    public int updateTInterfaceLog(TbMaterialInterfaceLog tInterfaceLog) {
        return tInterfaceLogMapper.updateTInterfaceLog(tInterfaceLog);
    }

    /**
     * 批量删除接口访问记录
     *
     * @param ids 需要删除的接口访问记录主键
     * @return 结果
     */
    @Override
    public int deleteTInterfaceLogByIds(Long[] ids) {
        return tInterfaceLogMapper.deleteTInterfaceLogByIds(ids);
    }

    /**
     * 删除接口访问记录信息
     *
     * @param id 接口访问记录主键
     * @return 结果
     */
    @Override
    public int deleteTInterfaceLogById(Long id) {
        return tInterfaceLogMapper.deleteTInterfaceLogById(id);
    }
}
