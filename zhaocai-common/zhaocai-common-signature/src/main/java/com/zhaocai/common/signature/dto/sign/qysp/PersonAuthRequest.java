package com.zhaocai.common.signature.dto.sign.qysp;

import com.zhaocai.common.signature.common.enums.QiYueSuoPrivateRequestTypeEnum;
import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import com.zhaocai.common.signature.dto.command.PersonAuthCommandRequest;
import lombok.Getter;
import lombok.Setter;

/**
 * 个人认证授权请求
 *
 * @author chenming
 * @date 2024-09-10
 */
@Getter
@Setter
public class PersonAuthRequest extends SignatureRequest {

    public PersonAuthRequest(PersonAuthCommandRequest commandRequest) {
        super(commandRequest);

        setRequestType(QiYueSuoPrivateRequestTypeEnum.PERSON_AUTH);
    }

    /**
     * 用户 id
     */
    private String userId;

    /**
     * 用户名
     */
    private String userName;

    /**
     * 用户联系方式
     */
    private String userPhone;
}
