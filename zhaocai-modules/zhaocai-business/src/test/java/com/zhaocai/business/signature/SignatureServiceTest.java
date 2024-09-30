package com.zhaocai.business.signature;

import com.zhaocai.business.agreement.service.IAgreementService;
import com.zhaocai.business.base.SpringBaseTest;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class SignatureServiceTest extends SpringBaseTest {

    @Autowired
    private IAgreementService agreementService;

    @Test
    public void createAgreementSignTest() {
        //agreementService.createAgreementSign(1824361767486070786L);
    }
}
