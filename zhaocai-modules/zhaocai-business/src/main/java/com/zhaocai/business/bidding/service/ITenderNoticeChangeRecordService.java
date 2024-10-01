package com.zhaocai.business.bidding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.bidding.domain.TenderNoticeChangeRecord;
import com.zhaocai.business.bidding.vo.req.TenderNoticeChangeRecordVO;
import com.zhaocai.business.bidding.vo.req.query.TenderNoticeChangeRecordQueryVO;
import com.zhaocai.business.bidding.vo.res.TenderNoticeChangeRecordListVO;

import java.util.List;

/**
 * 招标公告变更记录Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface ITenderNoticeChangeRecordService  extends IService<TenderNoticeChangeRecord> {

    /**
     * 新增招标公告变更记录
     *
     * @param noticeChangeRecordVO 新增招标公告变更记录
     * @return
     */
    boolean add(TenderNoticeChangeRecordVO noticeChangeRecordVO);

    /**
     * 查询招标公告变更记录列表
     *
     * @param queryVO 招标公告变更记录
     * @return
     */
    List<TenderNoticeChangeRecordListVO> getList(TenderNoticeChangeRecordQueryVO queryVO);

}
