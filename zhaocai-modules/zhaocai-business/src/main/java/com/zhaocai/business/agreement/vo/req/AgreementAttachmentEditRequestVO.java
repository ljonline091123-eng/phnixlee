package com.zhaocai.business.agreement.vo.req;

import com.zhaocai.business.agreement.domain.Agreement;
import com.zhaocai.business.agreement.vo.res.AgreementPaymentItemVO;
import com.zhaocai.business.agreement.vo.res.AgreementVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;
@Data
public class AgreementAttachmentEditRequestVO {

    @ApiModelProperty(value = "合同基本信息")
    private Agreement agreement;

    @ApiModelProperty(value = "合同款项信息")
    private AgreementPaymentItemVO agreementPaymentItem;

}
