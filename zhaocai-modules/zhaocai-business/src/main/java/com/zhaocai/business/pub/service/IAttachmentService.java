package com.zhaocai.business.pub.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.business.vendor.vo.res.DownloadAgreementVO;

import java.io.IOException;
import java.util.List;

/**
 * 附件Service接口
 *
 * @author WH
 * @date 2024-05-24
 */
public interface IAttachmentService extends IService<Attachment> {

    //修改附件的文件名和文件URL
    void  ModifyFileNameAndFileURL(Long attachmentId) throws IOException;

    //文档中台——获取预览word文件URL
    String  viewWordFileURL(String fileName, String fileUrl);

    //文档中台——获取预览word文件URL+加上水印
    String  viewWordFileURLWithWaterMarK(String fileName, String fileUrl, String waterMarkContent);

    //文档中台——获取预览PDF文件URL
    String  viewPDFFileURL(String fileName, String fileUrl);


    //文档中台——根据文件URL获取预览PDF文件URL（不用下载在本地）
    String  previewPdfUrlByFileUrl(String fileName, String fileUrl);

    //文档中台——获取预览图片URL
    String  viewImageURL(String fileName, String fileUrl);


    //文档中台——获取编辑word文档的URL-（开启限制编辑按钮）
    String  editWordURLWithLimitEdit(Long attachmentId, String fileName, String fileUrl);

    //文档中台——获取编辑word文档的URL
    String  editWordURL(Long attachmentId, String fileName, String fileUrl);

    //文档中台——获取编辑word文档的URL+ 加水印
    String  editWordURLWithWaterMark(Long attachmentId, String fileName, String fileUrl, String waterMarkContent);

    //文档中台-office转PDF
    String  convertOfficeToPdf(String fileName, String fileUrl, String waterMarkContent);

    /*  把传入的attachmentId附件转换为PDF文件（带水印），更新fileUrl
    1.若传入的attachmentId存在，更新传入的attachmentId附件（pdf文件）的fileUrl，返回更新的attachmentId；
    2.若传入的attachmentId不存在，新增attachment，返回新增的attachmentId
    */
    Long ConverToPDFAndUpdateFileUrl(Long busnessId, AttachmentTypeEnum busnessType, Long attachmentId, String watermarkText, String targetFileName, String fileUrl) throws IOException;

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

    //更新文件名和文件URL
    void updateFileNameANDFileUrl(Long id, String fileUrl, String fileName);

    /**
     * 更新附件业务信息
     * @param id
     * @param attachmentTypeEnum
     * @param businessId
     */
    void updateBusiness(Long id, AttachmentTypeEnum attachmentTypeEnum, Long businessId);

    /**
     * 更新附件业务信息
     * @param id
     * @param fileName
     * @param businessId
     */
    void updateBusiness(Long id,  Long businessId,String fileUrl,String fileName);

    /**
     * 获取附件文件流
     * @param attachmentId
     * @param agreementName
     * @return
     */
    DownloadAgreementVO getAttachmentInputStream(long attachmentId,String agreementName);


}
