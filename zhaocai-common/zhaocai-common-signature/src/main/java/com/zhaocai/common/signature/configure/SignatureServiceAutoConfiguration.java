package com.zhaocai.common.signature.configure;

import com.zhaocai.common.signature.common.utils.SpringBeanUtils;
import com.zhaocai.common.signature.controller.QiYueSuoPrivateCallBackController;
import com.zhaocai.common.signature.service.impl.*;
import com.zhaocai.common.signature.service.platform.qysp.*;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({SpringBeanUtils.class,
// 基础服务
        AgreementSignService.class, AgreementSignatureService.class, AgreementSignQysRequestLogService.class, AgreementSignQysService.class,
// 回调
        QiYueSuoPrivateCallBackController.class, QiYueSuoPrivateCallBackService.class,

// 契约锁私有服务
        QiYueSuoPrivateConfiguration.class, QiYueSuoPrivateSignatureService.class,CompanyAuthService.class, PersonAuthService.class, CreateByFileService.class,
        CreateByCategoryService.class,GetSignUrlService.class,DownloadDocumentService.class,CancelContractService.class})
public class SignatureServiceAutoConfiguration {
}
