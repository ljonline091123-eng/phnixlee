package com.zhaocai.business.bidding.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.bidding.domain.BiddingMarkTemplate;
import com.zhaocai.business.bidding.vo.req.query.BiddingMarkTemplateQueryVO;
import com.zhaocai.business.bidding.vo.res.BiddingMarkTemplateListVO;
import org.apache.ibatis.annotations.Param;

/**
 * 评分模板Mapper接口
 *
 * @author WH
 * @date 2024-06-18
 */
public interface BiddingMarkTemplateMapper extends BaseMapper<BiddingMarkTemplate> {

    /**
     * 分页查询招标公告列表
     *
     * @param queryVO 查询参数
     * @return 招标公告集合
     */
    IPage<BiddingMarkTemplateListVO> page(Page toMybatisPage, @Param("queryVO") BiddingMarkTemplateQueryVO queryVO);

    /**
     * 复用模板列表
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<BiddingMarkTemplateListVO> multiplexList(Page mybatisPage, @Param("queryVO") BiddingMarkTemplateQueryVO queryVO);


    /**
     * 列表查询
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<BiddingMarkTemplateListVO> switchList(Page mybatisPage,@Param("ids") String[] ids, @Param("queryVO") BiddingMarkTemplateQueryVO queryVO);


    /**
     * 列表查询
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<BiddingMarkTemplateListVO> selectList(Page mybatisPage, @Param("queryVO") BiddingMarkTemplateQueryVO queryVO);

}
