package com.zhaocai.business.pub.controller;

import com.zhaocai.business.common.base.BladeController;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.common.core.web.bean.ResultData;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiModelProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * 附件
 *
 * @author chenming
 * @date 2024-06-26
 */
@Api(value = "附件")
@RestController
@RequestMapping("/attachment")
public class AttachmentController extends BladeController {

    @Autowired
    private IAttachmentService attachmentService;

    @PostMapping("/addAttachment")
    @ApiModelProperty(value = "保存附件信息")
    public ResultData<Long> addAttachment(@RequestBody AttachmentRequestVO requestVO) {
        return ResultData.data(attachmentService.saveAttachment(requestVO));
    }
}
