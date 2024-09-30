package com.zhaocai.business.bidding.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.bidding.domain.BiddingMarkTemplate;
import com.zhaocai.business.bidding.vo.req.BiddingMarkTemplateVO;
import com.zhaocai.business.bidding.vo.req.query.BiddingMarkTemplateQueryVO;
import com.zhaocai.business.bidding.vo.res.BiddingMarkTemplateDetailVO;
import com.zhaocai.business.bidding.vo.res.BiddingMarkTemplateListVO;
import com.zhaocai.common.core.bean.PageResult;

/**
 * 评分模板Service接口
 *
 * @author WH
 * @date 2024-06-18
 */
public interface IBiddingMarkTemplateService  extends IService<BiddingMarkTemplate> {

    /**
     * 详情
     *
     * @param id 主键id
     * @return 结果
     */
    BiddingMarkTemplateDetailVO detail(Long id);

    /**
     * 分页查询
     */
    PageResult<BiddingMarkTemplateListVO> page(BiddingMarkTemplateQueryVO queryVO);

    /**
     * 分页查询
     */
    PageResult<BiddingMarkTemplateListVO> fanListPage(BiddingMarkTemplateQueryVO queryVO);

    /**
     * 分页查询
     */
    PageResult<BiddingMarkTemplateListVO> switchListPage(BiddingMarkTemplateQueryVO queryVO);

    /**
     * 新增或修改评分模板
     *
     * @param biddingMarkTemplateVO 评分模板
     * @return 结果
     */
    boolean addOrUpdate(BiddingMarkTemplateVO biddingMarkTemplateVO);

    /**
     * 删除评分模板
     *
     * @param id 主键id
     * @return 结果
     */
    boolean delete(Long id);

    /**
     * 修改启用状态
     *
     * @param id
     * @param state
     * @return
     */
    boolean updateStatus(Long id, Integer state);

}
