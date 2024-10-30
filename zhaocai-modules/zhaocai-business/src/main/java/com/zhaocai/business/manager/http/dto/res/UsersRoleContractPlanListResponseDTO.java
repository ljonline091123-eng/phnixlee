package com.zhaocai.business.manager.http.dto.res;

import com.zhaocai.business.bidding.vo.res.ContractPlanningNoticeVO;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.List;

/**
 *
 * Time:2024/10/16 下午4:48
 * */
@Data
public class UsersRoleContractPlanListResponseDTO {

    @ApiModelProperty(value = "用户列表")
    private List<UsersRoleListResponseDTO> userList;

    @ApiModelProperty(value = "招标对应合约规划列表")
    private List<ContractPlanningNoticeVO> contractPlanningNoticeVOList;

}
