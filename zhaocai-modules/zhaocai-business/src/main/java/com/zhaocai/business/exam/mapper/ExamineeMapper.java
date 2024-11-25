package com.zhaocai.business.exam.mapper;

import java.util.List;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.exam.domain.Examinee;
import com.zhaocai.business.vendor.domain.VendorCertification;

/**
 * 考生管理Mapper接口
 * 
 * @author xwj
 * @date 2024-11-21
 */
public interface ExamineeMapper extends BaseMapper<Examinee>
{
    /**
     * 查询考生管理
     * 
     * @param id 考生管理主键
     * @return 考生管理
     */
    public Examinee selectExamineeById(Long id);

    /**
     * 查询考生管理列表
     * 
     * @param examinee 考生管理
     * @return 考生管理集合
     */
    public List<Examinee> selectExamineeList(Examinee examinee);

    /**
     * 新增考生管理
     * 
     * @param examinee 考生管理
     * @return 结果
     */
    public int insertExaminee(Examinee examinee);

    /**
     * 修改考生管理
     * 
     * @param examinee 考生管理
     * @return 结果
     */
    public int updateExaminee(Examinee examinee);

    /**
     * 删除考生管理
     * 
     * @param id 考生管理主键
     * @return 结果
     */
    public int deleteExamineeById(Long id);

    /**
     * 批量删除考生管理
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteExamineeByIds(Long[] ids);
}
