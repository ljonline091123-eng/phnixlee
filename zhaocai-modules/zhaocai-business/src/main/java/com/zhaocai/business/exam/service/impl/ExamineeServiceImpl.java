package com.zhaocai.business.exam.service.impl;

import java.util.List;
import java.util.stream.Collectors;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.exam.vo.ExaminationRoomOptionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.zhaocai.business.exam.mapper.ExamineeMapper;
import com.zhaocai.business.exam.domain.Examinee;
import com.zhaocai.business.exam.service.IExamineeService;

/**
 * 考生管理Service业务层处理
 * 
 * @author xwj
 * @date 2024-11-21
 */
@Slf4j
@Service
public class ExamineeServiceImpl extends ServiceImpl<ExamineeMapper, Examinee> implements IExamineeService
{
    @Autowired
    private ExamineeMapper examineeMapper;


    /**
     * 查询考生
     * 
     * @param id 考生管理主键
     * @return 考生管理
     */
    @Override
    public Examinee selectExamineeById(Long id)
    {
        return examineeMapper.selectExamineeById(id);
    }

    /**
     * 据身份证号查询考生
     *
     * @param identityCardId 考生管理主键
     * @return 考生管理
     */
    @Override
    public Examinee selectExamineeByIdentityCardId(String identityCardId)
    {
        Examinee examinee = super.getOne(new LambdaQueryWrapper<Examinee>()
                .eq(Examinee::getIdentityCardId,identityCardId));
        return examinee;
    }

    /**
     * 查询所有考场
     *
     * @return 考场列表
     */
    @Override
    public List<ExaminationRoomOptionVO> getExaminationRoomList()
    {
        // 查询所有考试房间
        List<Examinee> examineeList = super.list();

        // 提取所有不同的考试房间
        List<String> examinationRooms = examineeList.stream()
                .map(Examinee::getExaminationRoom)
                .distinct()
                .collect(Collectors.toList());

        // 转换为所需的格式
        List<ExaminationRoomOptionVO> options = examinationRooms.stream()
                .map(room -> new ExaminationRoomOptionVO(room, room))
                .collect(Collectors.toList());

        return options;


    }

    /**
     * 据考场号查询考生管理列表
     *
     * @param examinationRoom 考生管理
     * @return 考生管理
     */
    @Override
    public List<Examinee> selectExamineeListByExaminationRoom(String examinationRoom)
    {
        List<Examinee> examineeList;
        examineeList = super.list(new LambdaQueryWrapper<Examinee>()
                .eq(Examinee::getExaminationRoom, examinationRoom)
                .orderByAsc(Examinee::getSeatNumber));
        return examineeList;
    }

    /**
     * 查询考生管理列表
     * 
     * @param examinee 考生管理
     * @return 考生管理
     */
    @Override
    public List<Examinee> selectExamineeList(Examinee examinee)
    {
        return examineeMapper.selectExamineeList(examinee);
    }

    /**
     * 新增考生管理
     * 
     * @param examinee 考生管理
     */
    @Override
    public void insertExaminee(Examinee examinee)
    {
        super.save(examinee);
        //return examineeMapper.insertExaminee(examinee);
    }

    /**
     * 修改考生管理
     * 
     * @param examinee 考生管理
     * @return 结果
     */
    @Override
    public int updateExaminee(Examinee examinee)
    {
        return examineeMapper.updateExaminee(examinee);
    }

    /**
     * 批量删除考生管理
     * 
     * @param ids 需要删除的考生管理主键
     * @return 结果
     */
    @Override
    public int deleteExamineeByIds(Long[] ids)
    {
        return examineeMapper.deleteExamineeByIds(ids);
    }

    /**
     * 删除考生管理信息
     * 
     * @param id 考生管理主键
     * @return 结果
     */
    @Override
    public int deleteExamineeById(Long id)
    {
        return examineeMapper.deleteExamineeById(id);
    }
}
