package com.zhaocai.archives.main.service.impl;

import com.zhaocai.common.signature.dto.callback.CallBackData;
import com.zhaocai.common.signature.service.SignatureCallBackBusinessService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 电子签章回调处理类
 *
 * @author chenming
 * @date 2024-09-11
 */
@Slf4j
@Service
public class SignatureCallBackBusinessServiceImpl implements SignatureCallBackBusinessService {


    @Override
    public void personAuthCallBack(CallBackData callBackData) {

    }

    @Override
    public void companyAuthCallBack(CallBackData callBackData) {

    }

    @Override
    public void signCallBack(CallBackData callBackData, Integer signatureType, Long businessId) {

    }


}
