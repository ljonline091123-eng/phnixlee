package com.zhaocai.business.agreement.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.zhaocai.business.agreement.domain.AgreementMachineShift;
import com.zhaocai.business.agreement.vo.res.AgreementMachineShiftVO;

import java.util.List;

/**
 * 合同-机械台班Service接口
 *
 * @author chenming
 * @date 2024-06-26
 */
public interface IAgreementMachineShiftService  extends IService<AgreementMachineShift> {

    /**
     * 保存合同-机械台班
     * @param agreementMachineShifts
     * @param agreementId
     */
    void saveAgreementMachineShift(List<AgreementMachineShift> agreementMachineShifts, Long agreementId);

    /**
     * 获取合同-机械台班
     * @param agreementId
     * @return
     */
    List<AgreementMachineShiftVO> listByAgreementId(Long agreementId);

    /**
     * 修改合同-机械台班
     * @param agreementMachineShifts
     * @param agreementId
     */
    void updateAgreementMachineShift(List<AgreementMachineShift> agreementMachineShifts, Long agreementId);
}
