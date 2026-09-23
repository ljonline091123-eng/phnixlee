package com.zhaocai.business.bidding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhaocai.business.bidding.domain.BiddingAbandonRecord;

import java.util.List;

/**
 * 投标单废标记录Mapper接口
 * 
 * @author WH
 * @date 2024-05-24
 */
public interface BiddingAbandonRecordMapper extends BaseMapper<BiddingAbandonRecord>
{
    /**
     * 查询投标单废标记录
     * 
     * @param id 投标单废标记录主键
     * @return 投标单废标记录
     */
    public BiddingAbandonRecord selectBiddingAbandonRecordById(Long id);

    /**
     * 查询投标单废标记录列表
     * 
     * @param biddingAbandonRecord 投标单废标记录
     * @return 投标单废标记录集合
     */
    public List<BiddingAbandonRecord> selectBiddingAbandonRecordList(BiddingAbandonRecord biddingAbandonRecord);

    /**
     * 新增投标单废标记录
     * 
     * @param biddingAbandonRecord 投标单废标记录
     * @return 结果
     */
    public int insertBiddingAbandonRecord(BiddingAbandonRecord biddingAbandonRecord);

    /**
     * 修改投标单废标记录
     * 
     * @param biddingAbandonRecord 投标单废标记录
     * @return 结果
     */
    public int updateBiddingAbandonRecord(BiddingAbandonRecord biddingAbandonRecord);

    /**
     * 删除投标单废标记录
     * 
     * @param id 投标单废标记录主键
     * @return 结果
     */
    public int deleteBiddingAbandonRecordById(Long id);

    /**
     * 批量删除投标单废标记录
     * 
     * @param ids 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteBiddingAbandonRecordByIds(Long[] ids);
}
