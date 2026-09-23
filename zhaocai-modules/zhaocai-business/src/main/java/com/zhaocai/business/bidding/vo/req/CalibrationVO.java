package com.zhaocai.business.bidding.vo.req;

import com.zhaocai.business.bidding.vo.res.CalibrationReportListVO;
import io.swagger.annotations.ApiModel;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @author ssy
 * @date 2024/4/9 18:01
 */
@Data
@EqualsAndHashCode(callSuper = false)
@ApiModel(value = "CalibrationVO", description = "定标VO")
public class CalibrationVO extends CalibrationReportListVO {
	private static final long serialVersionUID = -9097760777161008906L;

}
