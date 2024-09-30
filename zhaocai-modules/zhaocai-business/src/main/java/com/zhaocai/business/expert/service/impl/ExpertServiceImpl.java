package com.zhaocai.business.expert.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.expert.domain.Expert;
import com.zhaocai.business.expert.mapper.ExpertMapper;
import com.zhaocai.business.expert.service.IExpertService;
import com.zhaocai.business.expert.vo.req.ExpertVO;
import com.zhaocai.business.expert.vo.req.query.ExpertQueryVO;
import com.zhaocai.business.expert.vo.req.query.ExpertRandomDrawVO;
import com.zhaocai.business.expert.vo.res.ExpertInfoVO;
import com.zhaocai.business.expert.vo.res.ExpertListVO;
import com.zhaocai.business.expert.vo.res.TPIExpertInfoVO;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.common.core.bean.PageResult;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.system.api.system.RemoteUserService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 专家Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Service
public class ExpertServiceImpl extends ServiceImpl<ExpertMapper,Expert> implements IExpertService {

    @Autowired
    private RemoteUserService remoteUserService;
    @Autowired
    private IAttachmentService attachmentService;

    @Override
    public List<TPIExpertInfoVO> getTPIExpertInfo() {
        List<TPIExpertInfoVO> list = new ArrayList<>();
        TPIExpertInfoVO vo1 = new TPIExpertInfoVO();
        vo1.setExpertId(461234648744L);
        vo1.setExpertName("张明");
        vo1.setExpertPhone("13565485522");
        vo1.setOrganizationId(97413131L);
        vo1.setBelongOrganization("直属三公司");
        vo1.setDepartmentId(1356548552L);
        vo1.setDepartment("商务部");
        list.add(vo1);

        TPIExpertInfoVO vo2 = new TPIExpertInfoVO();
        vo2.setExpertId(45678624136L);
        vo2.setExpertName("李霞");
        vo2.setExpertPhone("13651256325");
        vo2.setOrganizationId(7845236456L);
        vo2.setBelongOrganization("直属三公司");
        vo2.setDepartmentId(76574342L);
        vo2.setDepartment("工程建设部");
        list.add(vo2);

        TPIExpertInfoVO vo4 = new TPIExpertInfoVO();
        vo4.setExpertId(6756345325L);
        vo4.setExpertName("赵特");
        vo4.setExpertPhone("19155256663");
        vo4.setOrganizationId(534653242234L);
        vo4.setBelongOrganization("直属三公司");
        vo4.setDepartmentId(49785464L);
        vo4.setDepartment("技术部");
        list.add(vo4);

        TPIExpertInfoVO vo5 = new TPIExpertInfoVO();
        vo5.setExpertId(634563453L);
        vo5.setExpertName("钱经");
        vo5.setExpertPhone("15112547854");
        vo5.setOrganizationId(786542344234L);
        vo5.setBelongOrganization("直属三公司");
        vo5.setDepartmentId(1231245464575L);
        vo5.setDepartment("工程建设部");
        list.add(vo5);

        return list;
    }

    @Override
    public PageResult<ExpertListVO> page(ExpertQueryVO queryDTO) {
        queryDTO.setWorkYearCompareDate(DateUtils.getNowDate());
        if (!CollectionUtils.isEmpty(queryDTO.getNotIncludeExpertIdList())){
            queryDTO.setNotIncludeExpertIds(
                    queryDTO.getNotIncludeExpertIdList().stream().map(Object::toString).collect(Collectors.joining(",")));
        }
        queryDTO.setNoticeStatus(TenderNoticeStatusEnum.EVALUATION_BID.getState());
        if (StringUtils.isNotEmpty(queryDTO.getDeptIds())){
            List<Long> deptIdList = new ArrayList<>();
            String[] deptArr = queryDTO.getDeptIds().split(",");
            for (String deptIdStr : deptArr) {
                deptIdList.add(Long.valueOf(deptIdStr));
            }
            queryDTO.setDeptIdList(deptIdList);
        }

        IPage<ExpertListVO> iPage = new Page<>();
        if (ObjectUtils.isEmpty(queryDTO.getDrawVO())){
            iPage = baseMapper.page(queryDTO.toMybatisPage(), queryDTO);
        } else {
            //查询所有符合条件的数据
            List<ExpertListVO> expertList = baseMapper.listExpert(queryDTO);

            //如果抽取条件不等于空
            if (!ObjectUtils.isEmpty(queryDTO.getDrawVO())){
                expertList = new ArrayList<>(randomDraw(expertList, queryDTO.getDrawVO()));
            }

            //封装分页数据(根据page的分页参数计算当前分页的数据)
            List<ExpertListVO> subList = expertList.stream()
                    .skip((long) (queryDTO.getPageNumber() - 1) * queryDTO.getPageSize())
                    .limit(queryDTO.getPageSize())
                    .collect(Collectors.toList());
            iPage.setRecords(subList);
            iPage.setTotal(expertList.size());
            iPage.setPages(expertList.size() / queryDTO.getPageSize() + 1);
        }
        return new PageResult<>(iPage);
    }

    /**
     * 根据条件随机抽取专家
     * */
    private List<ExpertListVO> randomDraw(List<ExpertListVO> expertList, ExpertRandomDrawVO drawVO){
        List<ExpertListVO> drawExpertList = new ArrayList<>();
        // 使用洗牌算法打乱列表中的元素
        Collections.shuffle(expertList);
        List<ExpertListVO> techExpertList = expertList.stream().filter(item -> item.getExpertType() == 1).collect(Collectors.toList());
        List<ExpertListVO> econExpertList = expertList.stream().filter(item -> item.getExpertType() == 2).collect(Collectors.toList());
        Integer econExpertNum = drawVO.getEconExpertNum();
        Integer techExpertNum = drawVO.getTechExpertNum();

        //获取抽取人数
        for (int i = 0; i < Math.min(techExpertList.size(), techExpertNum); i++) {
            drawExpertList.add(techExpertList.get(i));
        }
        for (int j = 0; j < Math.min(econExpertList.size(), econExpertNum); j++) {
            drawExpertList.add(econExpertList.get(j));
        }
        return drawExpertList;
    }

    @Override
    public ExpertInfoVO getInfo(Long id) {
        Expert expert = this.getById(id);
        ExpertInfoVO vo = BeanCopierUtil.copyBean(expert, ExpertInfoVO.class);

        List<AttachmentVO> resumeAttachList = attachmentService.listAttachment(AttachmentTypeEnum.EXPERT_RESUME, expert.getId());
        vo.setResumeAttachList(resumeAttachList);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean add(ExpertVO expertVO) {
        long count = this.count(new LambdaQueryWrapper<Expert>()
                .eq(Expert::getUserId, expertVO.getUserId()));
        //如果该用户已经是某个专家了
        if (count > 0){
            throw new ParamValidateException("该用户已经成为专家，不允许重复设置");
        }

        //新增专家信息
        Expert expert = BeanCopierUtil.copyBean(expertVO, Expert.class);
        expert.setExpertState(NumberConstant.ONE);
        boolean res = this.save(expert);

        //保存招标文件附件
        attachmentService.addAttachment(expertVO.getResumeAttachList(), AttachmentTypeEnum.EXPERT_RESUME, expert.getId());

        if (res){
            //提交审批信息
            //folw.submit()
        }

        /*//创建专家账号
        BusinessUser businessUser = new BusinessUser();
        businessUser.setUserName(expert.getExpertPhone());
        businessUser.setNickName(expert.getExpertName());
        businessUser.setUserType(UserTypeEnum.EXPERT);
        R<Long> r = remoteUserService.addBusinessUser(businessUser, SecurityConstants.INNER);
        if (R.SUCCESS != r.getCode()) {
            throw new BusinessException(r.getMsg());
        }
        expert.setUserId(r.getData());
        //直接设置专家用户id
        expert.setUserId(expertVO.getUserId());
        this.updateById(expert);*/
        return res;
    }

    @Override
    public boolean delete(List<Long> ids) {
        return this.removeByIds(ids);
    }

    @Override
    public boolean updateStatus(Long id, Integer state) {
        LambdaUpdateWrapper<Expert> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(Expert::getExpertState, state);
        updateWrapper.eq(Expert::getId, id);
        return update(updateWrapper);
    }

}
