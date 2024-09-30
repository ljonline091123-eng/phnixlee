package com.zhaocai.business.pub.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhaocai.business.pub.domain.Template;
import com.zhaocai.business.pub.vo.req.TemplateListQueryVO;
import com.zhaocai.business.pub.vo.res.TemplateListVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模板管理Mapper接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface TemplateMapper extends BaseMapper<Template> {

    /**
     * 列表查询
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<TemplateListVO> selectList(Page mybatisPage, @Param("queryVO") TemplateListQueryVO queryVO);

    /**
     * 复用模板列表
     * @param mybatisPage
     * @param queryVO
     * @return
     */
    IPage<TemplateListVO> multiplexList(Page mybatisPage, @Param("queryVO") TemplateListQueryVO queryVO);

    /**
     * 通用模板列表查询
     * @param ids
     * @return
     */
    IPage<TemplateListVO> switchList(Page mybatisPage,@Param("ids") String[] ids, @Param("queryVO") TemplateListQueryVO queryVO);
}
