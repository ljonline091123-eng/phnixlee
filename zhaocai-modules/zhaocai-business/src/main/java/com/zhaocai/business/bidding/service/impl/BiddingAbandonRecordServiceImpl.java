package com.zhaocai.business.bidding.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.BiddingAbandonRecord;
import com.zhaocai.business.bidding.mapper.BiddingAbandonRecordMapper;
import com.zhaocai.business.bidding.service.IBiddingAbandonRecordService;
import com.zhaocai.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 投标单废标记录Service业务层处理
 * 
 * @author WH
 * @date 2024-05-24
 */
@Service
public class BiddingAbandonRecordServiceImpl extends ServiceImpl<BiddingAbandonRecordMapper,BiddingAbandonRecord> implements IBiddingAbandonRecordService
        {
    @Autowired
    private BiddingAbandonRecordMapper biddingAbandonRecordMapper;

    /**
     * 查询投标单废标记录
     * 
     * @param id 投标单废标记录主键
     * @return 投标单废标记录
     */
    @Override
    public BiddingAbandonRecord selectBiddingAbandonRecordById(Long id)
    {
        return biddingAbandonRecordMapper.selectBiddingAbandonRecordById(id);
    }

    /**
     * 查询投标单废标记录列表
     * 
     * @param biddingAbandonRecord 投标单废标记录
     * @return 投标单废标记录
     */
    @Override
    public List<BiddingAbandonRecord> selectBiddingAbandonRecordList(BiddingAbandonRecord biddingAbandonRecord)
    {
        return biddingAbandonRecordMapper.selectBiddingAbandonRecordList(biddingAbandonRecord);
    }

    /**
     * 新增投标单废标记录
     * 
     * @param biddingAbandonRecord 投标单废标记录
     * @return 结果
     */
    @Override
    public int insertBiddingAbandonRecord(BiddingAbandonRecord biddingAbandonRecord)
    {
        biddingAbandonRecord.setCreateTime(DateUtils.getNowDate());
        return biddingAbandonRecordMapper.insertBiddingAbandonRecord(biddingAbandonRecord);
    }

    /**
     * 修改投标单废标记录
     * 
     * @param biddingAbandonRecord 投标单废标记录
     * @return 结果
     */
    @Override
    public int updateBiddingAbandonRecord(BiddingAbandonRecord biddingAbandonRecord)
    {
        biddingAbandonRecord.setUpdateTime(DateUtils.getNowDate());
        return biddingAbandonRecordMapper.updateBiddingAbandonRecord(biddingAbandonRecord);
    }

    /**
     * 批量删除投标单废标记录
     * 
     * @param ids 需要删除的投标单废标记录主键
     * @return 结果
     */
    @Override
    public int deleteBiddingAbandonRecordByIds(Long[] ids)
    {
        return biddingAbandonRecordMapper.deleteBiddingAbandonRecordByIds(ids);
    }

    /**
     * 删除投标单废标记录信息
     * 
     * @param id 投标单废标记录主键
     * @return 结果
     */
    @Override
    public int deleteBiddingAbandonRecordById(Long id)
    {
        return biddingAbandonRecordMapper.deleteBiddingAbandonRecordById(id);
    }
}
