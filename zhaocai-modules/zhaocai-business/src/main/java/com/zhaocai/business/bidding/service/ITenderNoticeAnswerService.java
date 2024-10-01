package com.zhaocai.business.bidding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.bidding.domain.TenderNoticeAnswer;
import com.zhaocai.business.bidding.vo.req.TenderNoticeAnswerVO;
import com.zhaocai.business.bidding.vo.req.query.TenderNoticeAnswerQueryVO;
import com.zhaocai.business.bidding.vo.res.TenderNoticeAnswerListVO;

import java.util.List;

/**
 * 招标公告/招标内容 答疑Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface ITenderNoticeAnswerService  extends IService<TenderNoticeAnswer> {
    /**
     * 查询招标公告答疑列表
     *
     * @param queryVO 招标公告答疑
     * @return
     */
    List<TenderNoticeAnswerListVO> getListNotice(TenderNoticeAnswerQueryVO queryVO);
    /**
     * 查询招标公告答疑列表
     *
     * @param queryVO 招标公告答疑
     * @return
     */
    List<TenderNoticeAnswerListVO> getList(TenderNoticeAnswerQueryVO queryVO);

    /**
     * 新增招标公告答疑
     *
     * @param tenderNoticeAnswerVO 招标公告答疑
     * @return 结果
     */
    boolean addNotice(TenderNoticeAnswerVO tenderNoticeAnswerVO);

    /**
     * 新增招标公告文件答疑
     *
     * @param tenderNoticeAnswerVO 招标公告答疑
     * @return 结果
     */
    boolean add(TenderNoticeAnswerVO tenderNoticeAnswerVO);

    /**
     * 修改招标公告答疑
     *
     * @param tenderNoticeAnswerVO 招标公告答疑
     * @return 结果
     */
    boolean editNotice(TenderNoticeAnswerVO tenderNoticeAnswerVO);

    /**
     * 修改招标公告文件答疑
     *
     * @param tenderNoticeAnswerVO 招标公告答疑
     * @return 结果
     */
    boolean edit(TenderNoticeAnswerVO tenderNoticeAnswerVO);

}
