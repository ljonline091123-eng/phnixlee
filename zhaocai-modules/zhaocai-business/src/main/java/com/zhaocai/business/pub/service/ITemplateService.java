package com.zhaocai.business.pub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.pub.domain.Template;
import com.zhaocai.business.pub.vo.req.TemplateListQueryVO;
import com.zhaocai.business.pub.vo.req.TemplateSaveRequestVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.pub.vo.res.TemplateListVO;
import com.zhaocai.business.pub.vo.res.TemplateVO;
import com.zhaocai.common.core.bean.PageResult;

/**
 * 模板管理Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface ITemplateService  extends IService<Template> {

    /**
     * 模板列表查询
     * @param queryVO
     * @return
     */
    PageResult<TemplateListVO> listPage(TemplateListQueryVO queryVO);

    /**
     * 模板列表查询
     * @param queryVO
     * @return
     */
    PageResult<TemplateListVO> fanListPage(TemplateListQueryVO queryVO);

    /**
     * 采购方案选择招标文件模板切换
     * @param queryVO
     * @return
     */
    PageResult<TemplateListVO> switchListPage(TemplateListQueryVO queryVO);

    /**
     * 保存模板信息
     * @param requestVO
     */
    void saveTemplate(TemplateSaveRequestVO requestVO);

    /**
     * 删除模板
     * @param id
     */
    void deleteTemplate(Long id);

    /**
     * 详情
     * @param id
     * @return
     */
    TemplateVO detail(Long id);

    /**
     * 获取模板的附件相关信息
     * @param templateId
     * @return
     */
    AttachmentVO getTemplateAttachmentInfo(Long templateId);
}
