package com.zhaocai.business.pub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.vendor.vo.res.DownloadAgreementVO;

import java.io.InputStream;
import java.util.List;

/**
 * 附件Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface IAttachmentService extends IService<Attachment> {

    /**
     * 新增附件
     * @param attachmentList    附件列表
     * @param businessType      业务类型
     * @param businessId        业务 id
     */
    void addAttachment(List<AttachmentRequestVO> attachmentList, AttachmentTypeEnum businessType, Long businessId);

    /**
     * 获取指定业务的附件
     * @param businessType
     * @param businessId
     * @return
     */
    List<AttachmentVO> listAttachment(AttachmentTypeEnum businessType, Long businessId);

    /**
     * 获取指定id的附件
     * @param attachmentId
     * @return
     */
    AttachmentVO getAttachmentById(Long attachmentId);

    /**
     * 新增附件
     * @param requestVO
     * @param businessType
     * @param businessId
     * @return
     */
    Long addAttachment(AttachmentRequestVO requestVO, AttachmentTypeEnum businessType, Long businessId);

    /**
     * 根据业务代码删除
     * @param businessId
     */
    void deleteByBusinessId(AttachmentTypeEnum businessType, Long businessId);

    /**
     * 保存附件信息
     * @param requestVO
     * @return
     */
    Long saveAttachment(AttachmentRequestVO requestVO);

    /**
     * 更新附件业务信息
     * @param id
     * @param attachmentTypeEnum
     * @param businessId
     */
    void updateBusiness(Long id, AttachmentTypeEnum attachmentTypeEnum, Long businessId);

    /**
     * 获取附件文件流
     * @param attachmentId
     * @param agreementName
     * @return
     */
    DownloadAgreementVO getAttachmentInputStream(long attachmentId,String agreementName);


}
