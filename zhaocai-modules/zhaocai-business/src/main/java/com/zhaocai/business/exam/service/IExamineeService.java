package com.zhaocai.business.exam.service;

import java.util.List;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.exam.domain.Examinee;

/**
 * 考生管理Service接口
 * 
 * @author xwj
 * @date 2024-11-21
 */
public interface IExamineeService extends IService<Examinee>
{
    /**
     * 查询考生管理
     * 
     * @param id 考生管理主键
     * @return 考生管理
     */
    public Examinee selectExamineeById(Long id);

    //据身份证号查询考生
    Examinee selectExamineeByIdentityCardId(String identityCardId);

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
     */
    public void insertExaminee(Examinee examinee);

    /**
     * 修改考生管理
     * 
     * @param examinee 考生管理
     * @return 结果
     */
    public int updateExaminee(Examinee examinee);

    /**
     * 批量删除考生管理
     * 
     * @param ids 需要删除的考生管理主键集合
     * @return 结果
     */
    public int deleteExamineeByIds(Long[] ids);

    /**
     * 删除考生管理信息
     * 
     * @param id 考生管理主键
     * @return 结果
     */
    public int deleteExamineeById(Long id);
}
