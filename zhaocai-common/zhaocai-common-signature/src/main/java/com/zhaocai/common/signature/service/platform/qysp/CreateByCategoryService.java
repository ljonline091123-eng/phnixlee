package com.zhaocai.common.signature.service.platform.qysp;

import cn.hutool.core.collection.CollectionUtil;
import com.qiyuesuo.sdk.v2.utils.StringUtils;
import com.zhaocai.common.signature.common.enums.SignatureTypeEnum;
import com.zhaocai.common.signature.common.enums.StamperTypeEnum;
import com.zhaocai.common.signature.common.exception.SignatureValidateException;
import com.zhaocai.common.signature.dto.SignatureContact;
import com.zhaocai.common.signature.dto.SignatureCreator;
import com.zhaocai.common.signature.dto.sign.SignatureRequest;
import com.zhaocai.common.signature.dto.SignatureStamper;
import com.zhaocai.common.signature.dto.sign.qysp.CreateByCategoryRequest;
import net.qiyuesuo.v3sdk.model.common.Action;
import net.qiyuesuo.v3sdk.model.common.ActionOperatorInfo;
import net.qiyuesuo.v3sdk.model.common.Signatory;
import net.qiyuesuo.v3sdk.model.common.SignatoryRect;
import net.qiyuesuo.v3sdk.model.contract.request.ContractCreatebycategoryRequest;
import net.qiyuesuo.v3sdk.utils.SdkRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


/**
 * 创建发起电子签约
 *
 * @author chenming
 * @date 2024-09-18
 */
@Service
public class CreateByCategoryService extends AbstractQiYueSuoPrivateRequestDefaultService{

    @Value("${signature.qiyuesuo.private.categoryId}")
    private String categoryId;


    @Override
    protected SdkRequest builderSignRequest(SignatureRequest signatureRequest) {
        CreateByCategoryRequest request = (CreateByCategoryRequest) signatureRequest;

        ContractCreatebycategoryRequest contractRequest = new ContractCreatebycategoryRequest();
        // 基本信息
        contractRequest.setCategoryId(categoryId);
        contractRequest.setSubject(request.getSubject());
        contractRequest.setDocuments(CollectionUtil.newArrayList(request.getDocuments()));

        // 发起方信息
        SignatureCreator signatureCreator = request.getSignatureCreator();
        contractRequest.setTenantName(signatureCreator.getTenantName());
        contractRequest.setCreatorName(signatureCreator.getCreatorName());
        contractRequest.setCreatorContact(signatureCreator.getCreatorContact());

        // 签署方
        // 甲方
        SignatureContact signatureContact = getSignatureContact(request.getSignatureContactList(), SignatureTypeEnum.PARTY_A);
        // 甲方签署信息
        List<SignatureStamper> signatureAStamperList = getSignatureStamper(request.getSignatureStamperList(),SignatureTypeEnum.PARTY_A);
        Signatory signatoryA = builderSignatory(signatureContact,signatureAStamperList);

        // 乙方签署信息
        signatureContact = getSignatureContact(request.getSignatureContactList(), SignatureTypeEnum.PARTY_B);
        List<SignatureStamper> signatureBStamperList = getSignatureStamper(request.getSignatureStamperList(),SignatureTypeEnum.PARTY_B);
        Signatory signatoryB = builderSignatory(signatureContact,signatureBStamperList);

        List<Signatory> signatories = CollectionUtil.newArrayList(signatoryA,signatoryB);
        contractRequest.setSignatories(signatories);

        return contractRequest;
    }

    private Signatory builderSignatory(SignatureContact signatureContact, List<SignatureStamper> signatureStamperList) {
        Signatory signatory = new Signatory();
        // 基本信息
        signatory.setTenantType("COMPANY");
        signatory.setTenantName(signatureContact.getSignatureName());
        signatory.setReceiverName(signatureContact.getContactName());
        signatory.setContact(signatureContact.getContactPhone());
        // 无先后顺序
        signatory.setSerialNo(1L);

        // 签署人信息
        ActionOperatorInfo operatorInfo = new ActionOperatorInfo();
        operatorInfo.setOperatorName(signatureContact.getContactName());
        operatorInfo.setOperatorContact(signatureContact.getContactPhone());
        List<ActionOperatorInfo> operatorInfoList = CollectionUtil.newArrayList(operatorInfo);


        /*
         * 盖章
         */
        // 基本信息
        Action corporateAction = new Action();
        corporateAction.setType("CORPORATE");
        corporateAction.setSerialNo(1L);
        corporateAction.setName("组织签章");
        corporateAction.setActionOperators(operatorInfoList);

        // 签署位置
        List<SignatoryRect> corporateLocations = new ArrayList<>();
        // 公章
        corporateLocations.add(getSignatoryRect(signatureStamperList,StamperTypeEnum.QYS_COMPANY));
        // 骑缝章
        corporateLocations.add(getSignatoryRect(signatureStamperList,StamperTypeEnum.QYS_ACROSS_PAGE));
        corporateAction.setLocations(corporateLocations);

        /*
         * 签名
         */
        Action personalAction = new Action();
        personalAction.setType("PERSONAL");
        personalAction.setSerialNo(1L);
        personalAction.setName("个人签字");
        personalAction.setActionOperators(operatorInfoList);
        // 签署位置
        List<SignatoryRect> personalLocations = new ArrayList<>();
        // 公章
        personalLocations.add(getSignatoryRect(signatureStamperList,StamperTypeEnum.QYS_PERSONAL));
        personalAction.setLocations(personalLocations);

        List<Action> actionList = CollectionUtil.newArrayList(corporateAction,personalAction);
        signatory.setActions(actionList);

        return signatory;
    }

    private SignatoryRect getSignatoryRect(List<SignatureStamper> signatureStamperList,StamperTypeEnum stamperTypeEnum) {
        SignatoryRect signatoryRect = null;
        SignatureStamper stamper = getSignatureStamper(signatureStamperList, stamperTypeEnum);
        if (stamper != null) {
            signatoryRect = new SignatoryRect();
            signatoryRect.setRectType(stamper.getPlatformSignType());
            if (StringUtils.isNotBlank(stamper.getKeyWord())) {
                signatoryRect.setKeyword(stamper.getKeyWord());
            }
            if (stamper.getSignPage() != null) {
                signatoryRect.setPage(Long.valueOf(stamper.getSignPage()));
            }
            if (stamper.getOffsetX() != null) {
                signatoryRect.setOffsetX(stamper.getOffsetX().doubleValue());
            }
            if (stamper.getOffsetY() != null) {
                signatoryRect.setOffsetY(stamper.getOffsetY().doubleValue());
            }
        }
        return signatoryRect;
    }

    /**
     * 获取公章
     * @param signatureStamperList
     * @param stamperTypeEnum
     * @return
     */
    private SignatureStamper getSignatureStamper(List<SignatureStamper> signatureStamperList, StamperTypeEnum stamperTypeEnum) {
        for (SignatureStamper stamper : signatureStamperList) {
            if (stamperTypeEnum.equalsType(stamper.getSignType())) {
                stamper.setPlatformSignType(stamperTypeEnum.getPlatformType());
                return stamper;
            }
        }

        return null;
    }

    /**
     * 获取签署方的公章信息
     * @param signatureStamperList
     * @param typeEnum
     * @return
     */
    private List<SignatureStamper> getSignatureStamper(List<SignatureStamper> signatureStamperList, SignatureTypeEnum typeEnum) {
        List<SignatureStamper> stamperList = new ArrayList<>();
        for (SignatureStamper stamper : signatureStamperList) {
            if (typeEnum.equalsType(stamper.getType())) {
                stamperList.add(stamper);
            }
        }

        if (CollectionUtil.isEmpty(stamperList)) {
            throw new  SignatureValidateException(String.format("签署方[%s]的签署位置信息不能为空",typeEnum.getName()));
        }

        return stamperList;
    }

    /**
     * 获取签署方信息
     * @param signatureContactList
     * @param typeEnum
     * @return
     */
    private SignatureContact getSignatureContact(List<SignatureContact> signatureContactList, SignatureTypeEnum typeEnum) {
        SignatureContact signatureContact = null;
        for (SignatureContact contact : signatureContactList) {
            if (typeEnum.equalsType(contact.getSignatureType())) {
                signatureContact = contact;
                break;
            }
        }

        return Optional.ofNullable(signatureContact)
                .orElseThrow(() -> new SignatureValidateException(String.format("签署方[%s]信息不能为空",typeEnum.getName())));
    }
}
