package com.zhaocai.business.bidding.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.zhaocai.business.bidding.domain.*;
import com.zhaocai.business.bidding.enums.BiddingInfoStatusEnum;
import com.zhaocai.business.bidding.enums.TenderNoticeStatusEnum;
import com.zhaocai.business.bidding.mapper.BiddingInfoMapper;
import com.zhaocai.business.bidding.service.*;
import com.zhaocai.business.bidding.vo.req.*;
import com.zhaocai.business.bidding.vo.req.query.BiddingInfoQueryVO;
import com.zhaocai.business.bidding.vo.req.query.BiddingQuotationQueryVO;
import com.zhaocai.business.bidding.vo.res.*;
import com.zhaocai.business.common.cache.DictBizCache;
import com.zhaocai.business.common.enums.AttachmentTypeEnum;
import com.zhaocai.business.common.enums.DictBizEnum;
import com.zhaocai.business.common.enums.SmsTemplateEnum;
import com.zhaocai.business.common.exception.ParamValidateException;
import com.zhaocai.business.common.sms.SmsSenderUtil;
import com.zhaocai.business.common.utils.ValidateUtils;
import com.zhaocai.business.expert.domain.Expert;
import com.zhaocai.business.expert.domain.ExpertScore;
import com.zhaocai.business.expert.service.IExpertScoreService;
import com.zhaocai.business.expert.service.IExpertService;
import com.zhaocai.business.procurement.domain.MaterialsList;
import com.zhaocai.business.procurement.domain.ProcurementScheme;
import com.zhaocai.business.procurement.service.IProcurementSchemeService;
import com.zhaocai.business.procurement.vo.res.CompContractSplitMaterialsVO;
import com.zhaocai.business.procurement.vo.res.CompMaterialsContentVO;
import com.zhaocai.business.procurement.vo.res.CompMaterialsVO;
import com.zhaocai.business.procurement.vo.res.MaterialsVO;
import com.zhaocai.business.pub.domain.Attachment;
import com.zhaocai.business.pub.service.IAttachmentService;
import com.zhaocai.business.pub.vo.req.AttachmentRequestVO;
import com.zhaocai.business.pub.vo.res.AttachmentVO;
import com.zhaocai.common.core.constant.NumberConstant;
import com.zhaocai.common.core.utils.DateUtils;
import com.zhaocai.common.core.utils.StringUtils;
import com.zhaocai.common.core.utils.bean.BeanCopierUtil;
import com.zhaocai.common.security.utils.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.math.BigDecimal.ROUND_DOWN;

/**
 * 投标单信息Service业务层处理
 *
 * @author WH
 * @date 2024-05-24
 */
@Service
@Slf4j
public class BiddingInfoServiceImpl extends ServiceImpl<BiddingInfoMapper,BiddingInfo> implements IBiddingInfoService {

    @Autowired
    private ITenderNoticeService tenderNoticeService;
    @Autowired
    private IBiddingAbandonRecordService biddingAbandonRecordService;
    @Autowired
    private IBiddingListQuotationService biddingListQuotationService;
    @Autowired
    private IAttachmentService attachmentService;
    @Autowired
    private IExpertScoreService expertScoreService;
    @Autowired
    private IProcurementSchemeService procurementSchemeService;
    @Autowired
    private IBiddingEvaluatExpertService biddingEvaluatExpertService;
    @Autowired
    private IExpertService expertService;

    @Autowired
    private SmsSenderUtil smsSenderUtil = SpringUtil.getBean(SmsSenderUtil.class);

    @Override
    public List<BiddingInfoListVO> getList(BiddingInfoQueryVO queryVO) {
//        queryVO.setExcludeBiddingStatus(BiddingInfoStatusEnum.HAVE_ABANDON.getState());
        List<BiddingInfoListVO> biddingInfoList = baseMapper.selectBiddingInfoList(queryVO);
        for (BiddingInfoListVO biddingInfoListVO : biddingInfoList) {
            biddingInfoListVO.setBiddingStatusText(BiddingInfoStatusEnum.getValueByCode(biddingInfoListVO.getBiddingStatus()));
            //投标单开标状态
            if (biddingInfoListVO.getBiddingStatus().compareTo(BiddingInfoStatusEnum.OPENED.getState()) >= 0){
                biddingInfoListVO.setBiddingOpenStatus("已开标");
            } else {
                biddingInfoListVO.setBiddingOpenStatus("待开标");
            }
//            biddingInfoListVO.setTaxPrice(biddingInfoListVO.getTaxPrice().setScale(2, ROUND_DOWN));
//            biddingInfoListVO.setNotTaxPrice(biddingInfoListVO.getNotTaxPrice().setScale(2, ROUND_DOWN));
//            biddingInfoListVO.setTaxPricePattern(NumberUtil.decimalFormat("#,###.00", biddingInfoListVO.getTaxPrice()));
//            biddingInfoListVO.setNotTaxPricePattern(NumberUtil.decimalFormat("#,###.00", biddingInfoListVO.getNotTaxPrice()));
        }
        return biddingInfoList;
    }

    @Override
    public List<BiddingInfoOpenListVO> getOpenList(BiddingInfoQueryVO queryVO) {
        queryVO.setExcludeBiddingStatus(BiddingInfoStatusEnum.HAVE_ABANDON.getState());
        List<BiddingInfoOpenListVO> biddingInfoList = baseMapper.selectBiddingInfoOpenList(queryVO);
        for (BiddingInfoOpenListVO biddingInfoOpenListVO : biddingInfoList) {
            biddingInfoOpenListVO.setBiddingStatusText(BiddingInfoStatusEnum.getValueByCode(biddingInfoOpenListVO.getBiddingStatus()));
            //投标单开标状态
            if (biddingInfoOpenListVO.getBiddingStatus().compareTo(BiddingInfoStatusEnum.OPENED.getState()) >= 0){
                biddingInfoOpenListVO.setBiddingOpenStatus("已开标");
            } else {
                biddingInfoOpenListVO.setBiddingOpenStatus("待开标");
            }
        }
        return biddingInfoList;
    }


    @Override
    public List<BiddingQuotationListVO> getBiddingQuotationList(BiddingQuotationQueryVO queryVO) {
        //查询当前公告流程状态
        TenderNotice tenderNotice = tenderNoticeService.getById(queryVO.getNoticeId());
        //报价次数
        Integer quoteNum = 0;
        //是否填充报价信息
        boolean isFillBiddingInfo = false;
        if (tenderNotice.getNoticeStatus().compareTo(TenderNoticeStatusEnum.EVALUATION_BID.getState()) > 0){
            //当前招标流程在‘评标’节点之后
            isFillBiddingInfo = true;
        }

        //查询设置的评标专家人员
        List<BiddingEvaluatExpert> expertList = biddingEvaluatExpertService.list(new LambdaQueryWrapper<BiddingEvaluatExpert>()
                .eq(BiddingEvaluatExpert::getNoticeId, queryVO.getNoticeId())
                .eq(BiddingEvaluatExpert::getIsJoin, 1));
        //查询首轮供应商报价数据，有首轮数据才能在列表中显示（且状态不为废标）
        List<BiddingQuotationListVO> list = baseMapper.findBiddingQuotationList(queryVO);

        /* 按供应商分组 将招标对象 的 投标数据 分组，并按版本号排序。 */
        Map<Long,LinkedList<BiddingQuotationListVO>> hasMap = new HashMap<>();
        for (BiddingQuotationListVO vo : list) {
            if(hasMap.get(vo.getVendorId())==null || hasMap.get(vo.getVendorId()).isEmpty()){
                hasMap.put(vo.getVendorId(),new LinkedList<>(Arrays.asList(vo)));
            }else {
                LinkedList link = hasMap.get(vo.getVendorId());
                link.add(vo);
                Collections.sort(link, new VersionComparator());
                hasMap.put(vo.getVendorId(),link);
            }
        }

        List<BiddingQuotationListVO> listReturn = new ArrayList<>();
        for(Long vendorId : hasMap.keySet()) {
            /* 获取该供应商所有的投标数据 */
            BiddingQuotationListVO vo = hasMap.get(vendorId).get((Math.max((hasMap.get(vendorId).size() - 1), 0)));
            List<BiddingQuotationDataVO> quotationDataVOList = new ArrayList<>();
            /* 获取该供应商所有的投标数据，处理数据后 存入数值 */
            for (int i = 0; i < hasMap.get(vendorId).size(); i++) {
                BiddingQuotationDataVO bidChild = new BiddingQuotationDataVO();
                bidChild.setTwiceQuotVersion(hasMap.get(vendorId).get(i).getTwiceQuotVersion());/* 版本号 */
                /* 当前报价版本 已经调价才显示数据，不然没有数据 */
                if(bidChild.getTwiceQuotVersion()!=null && bidChild.getTwiceQuotVersion().equals(tenderNotice.getTwiceQuotVersion()) && tenderNotice.getTwiceQuotVersion()!=null){

                    /** 当前招标文件开启调价 */
                    if(tenderNotice.getTwiceQuotState()!=null && tenderNotice.getTwiceQuotState().equals(NumberConstant.ONE)){
                        /* 当前已经调价 */
                        if(hasMap.get(vendorId).get(i).getPriceChangeState()!=null && hasMap.get(vendorId).get(i).getPriceChangeState().equals(NumberConstant.ONE)){
                            bidChild.setTaxPrice(hasMap.get(vendorId).get(i).getTaxPrice());
                            bidChild.setNotTaxPrice(hasMap.get(vendorId).get(i).getNotTaxPrice());
                            bidChild.setNotTaxPricePattern(hasMap.get(vendorId).get(i).getNotTaxPricePattern());
                            bidChild.setPriceChangeState(NumberConstant.ONE);/* 已调价 */
                            vo.setPriceChangeState(NumberConstant.ONE);/* 当前版本 已调价 */
                        }else{
                            /* 当前未调价 */
                            bidChild.setId(hasMap.get(vendorId).get(i).getId());
                            bidChild.setPriceChangeState(NumberConstant.ZERO);/* 未调价 */
                            vo.setPriceChangeState(NumberConstant.ZERO);/* 当前版本 未调价 */
                        }
                        bidChild.setId(hasMap.get(vendorId).get(i).getId());
                    }else{
                        /** 当前招标文件 关闭了调价 */
                        bidChild.setId(hasMap.get(vendorId).get(i).getId());
                        bidChild.setTaxPrice(hasMap.get(vendorId).get(i).getTaxPrice());
                        bidChild.setNotTaxPrice(hasMap.get(vendorId).get(i).getNotTaxPrice());
                        bidChild.setNotTaxPricePattern(hasMap.get(vendorId).get(i).getNotTaxPricePattern());
                        /* 当前已经调价 */
                        if(hasMap.get(vendorId).get(i).getPriceChangeState()!=null && hasMap.get(vendorId).get(i).getPriceChangeState().equals(NumberConstant.ONE)){
                            bidChild.setPriceChangeState(NumberConstant.ONE);/* 已调价 */
                            vo.setPriceChangeState(NumberConstant.ONE);/* 当前版本 已调价 */
                        }else{
                            /* 当前未调价 */
                            bidChild.setPriceChangeState(NumberConstant.TWO);/* 放弃调价 */
                            vo.setPriceChangeState(NumberConstant.TWO);/* 当前版本 放弃调价 */
                        }
                    }
                }else {
                    /* 非当前版本 */
                    bidChild.setId(hasMap.get(vendorId).get(i).getId());
                    bidChild.setTaxPrice(hasMap.get(vendorId).get(i).getTaxPrice());
                    bidChild.setNotTaxPrice(hasMap.get(vendorId).get(i).getNotTaxPrice());
                    bidChild.setNotTaxPricePattern(hasMap.get(vendorId).get(i).getNotTaxPricePattern());
                    /* 不是当前版本的 未调价 投标数据 就是放弃调价。 */
                    if(hasMap.get(vendorId).get(i).getPriceChangeState()!=null && hasMap.get(vendorId).get(i).getPriceChangeState().equals(NumberConstant.ONE)){
                        bidChild.setPriceChangeState(NumberConstant.ONE);/* 已调价 */
                    }else{
                        bidChild.setPriceChangeState(NumberConstant.TWO);/* 不是当前版本的 未调价 投标数据 就是放弃调价。 */
                    }
                }
                quotationDataVOList.add(bidChild);
            }
            vo.setQuotationDataVOList(quotationDataVOList);/* N次报价数组 */
            vo.setTwiceQuotVersion(tenderNotice.getTwiceQuotVersion());/* 当前版本 */


            //赋值最大报价次数
            if (quotationDataVOList.size() > quoteNum){
                quoteNum = quotationDataVOList.size();
            }

            //查询评标附件
            List<AttachmentVO> attachments = attachmentService.listAttachment(AttachmentTypeEnum.EVAL_DOCUMENT, vo.getId());
            vo.setAttachments(attachments);
            //设置需要查看的回标单id（最后一次投标单id）
            vo.setBiddingInfoId(quotationDataVOList.get(quotationDataVOList.size() - 1).getId());
            //展示保留两位小数
//            vo.setTaxPrice(vo.getTaxPrice().setScale(2, ROUND_DOWN));
//            vo.setNotTaxPrice(vo.getNotTaxPrice().setScale(2, ROUND_DOWN));

            //设置综合得分
            BigDecimal score = null;
            BigDecimal avgBusTotalScore = BigDecimal.ZERO;
            BigDecimal avgTechTotalScore = BigDecimal.ZERO;

            //1.倒序循环quotationDataVOList
            //2.根据公告id和投标单id查找有专家评分的报价轮次
            //3.查询当前轮次的所有专家的评分数据
            //4.刷选出同一个专家对同一个投标单重复评分的数据
            //5.计算综合得分
            for (int i = quotationDataVOList.size()-1; i >= 0; i--) {
                List<ExpertScoreExtraVO> expertScores = new ArrayList<>();
                //投标单id
                Long biddingInfoId = quotationDataVOList.get(i).getId();
                ExpertScoreExtraVO expertScoreExtraVO;
                for (BiddingEvaluatExpert evaluatExpert : expertList) {
                    //查询评标专家产生的评分数据
                    ExpertScore expertScore = expertScoreService.getOne(new LambdaQueryWrapper<ExpertScore>()
                            .eq(ExpertScore::getBiddingInfoId, biddingInfoId)
                            .eq(ExpertScore::getExpertId, evaluatExpert.getExpertId())
                            .orderByDesc(ExpertScore::getCreateTime).last("limit 1"));
                    if (!ObjectUtils.isEmpty(expertScore)){
                        expertScoreExtraVO = BeanCopierUtil.copyBean(expertScore, ExpertScoreExtraVO.class);
                        expertScoreExtraVO.setExpertType(evaluatExpert.getExpertType());
                        //产生评分数据
                        expertScores.add(expertScoreExtraVO);
                    } else {
                        //没有产生评分数据
                        expertScores.clear();
                        break;
                    }
                }

                if (!CollectionUtils.isEmpty(expertScores)){
                    //算分数
//                    int expertSize = expertScores.size();
                    BigDecimal busTotalScore = BigDecimal.ZERO;
                    BigDecimal techTotalScore = BigDecimal.ZERO;
                    //商务技术专家人数
                    int expertSizeBus = 0;
                    int expertSizeTech = 0;
                    for (ExpertScoreExtraVO expertScoreExtra : expertScores) {
                        if (expertScoreExtra.getExpertType() == 1){
                            //1.汇总技术分数
                            techTotalScore = techTotalScore.add(expertScoreExtra.getTechScore());
                            expertSizeTech++;
                        } else if (expertScoreExtra.getExpertType() == 2){
                            //2.汇总商务分数
                            busTotalScore = busTotalScore.add(expertScoreExtra.getBusScore());
                            expertSizeBus++;
                        }

                    }

                    if (expertSizeBus > 0){
                        avgBusTotalScore = busTotalScore.divide(new BigDecimal(expertSizeBus), 2, RoundingMode.HALF_UP);
                    }
                    if (expertSizeTech > 0){
                        avgTechTotalScore = techTotalScore.divide(new BigDecimal(expertSizeTech), 2, RoundingMode.HALF_UP);
                    }
                    score = avgBusTotalScore.add(avgTechTotalScore);
                    //得到了最新的综合得分，那就跳出算得分的代码
                    break;
                }

            }
            vo.setAvgBusTotalScore(avgBusTotalScore);
            vo.setAvgTechTotalScore(avgTechTotalScore);
            vo.setScore(score);

            listReturn.add(vo);
        }


        //综合排名（排序）按照综合分由高到低排序，综合分一致按不含税总价排序
        if (!CollectionUtils.isEmpty(listReturn)){
            //根据综合分进行排序
            Collections.sort(listReturn);
            //补充字段内容
            fillFieldBid(listReturn, isFillBiddingInfo, quoteNum);
        }
        return listReturn;
    }

    /** 填充列表列表信息 */
    private void fillFieldBid(List<BiddingQuotationListVO> list, boolean isFillBiddingInfo, Integer quoteNum){
        int rank = 0;
        for (BiddingQuotationListVO quotationListVO : list) {
            rank++;
            quotationListVO.setRank(rank);
            quotationListVO.setCandidate("第" + rank + "中标候选人");
            if (rank == NumberConstant.ONE){
                quotationListVO.setSureBid(NumberConstant.ONE);
            } else {
                quotationListVO.setSureBid(NumberConstant.ZERO);
            }

            List<BiddingQuotationDataVO> quotationDataVOList = quotationListVO.getQuotationDataVOList();
            quotationDataVOList.forEach(quotationDataVO -> {
                quotationDataVO.setTaxPrice(quotationDataVO.getTaxPrice());
                quotationDataVO.setNotTaxPrice(quotationDataVO.getNotTaxPrice());
            });

            int dataSize = quotationDataVOList.size();
            if (quoteNum > dataSize){
                BiddingQuotationDataVO dataVO = new BiddingQuotationDataVO();
                if (isFillBiddingInfo){
                    dataVO = quotationDataVOList.get(dataSize - 1);
                }
                //填充轮次
                for (int i = 0; i < quoteNum - dataSize; i++) {
                    quotationDataVOList.add(dataVO);
                }
            }

        }
    }

    @Override
    public BiddingInfoDetailVO getInfo(Long id) {
        BiddingInfoDetailVO vo = new BiddingInfoDetailVO();
        if (null == id){
            return vo;
        }

        BiddingInfo biddingInfo = this.getById(id);
        vo = BeanCopierUtil.copyBean(biddingInfo, BiddingInfoDetailVO.class);
        vo.setTaxPrice(biddingInfo.getTaxPrice());
        vo.setNotTaxPrice(biddingInfo.getNotTaxPrice());

        //todo su投标总价（超上限价字体颜色变红）

        //采购方案的物料清单
        List<BiddingQuotationDetailVO> quotationDetailVOList = new ArrayList<>();
        List<MaterialsVO> materialsList = procurementSchemeService.listMaterials(
                biddingInfo.getSchemeId());

        List<BiddingListQuotation> quotations = biddingListQuotationService.list(new LambdaQueryWrapper<BiddingListQuotation>()
                .eq(BiddingListQuotation::getBiddingInfoId, id));
        try{
            Map<String, BiddingListQuotation> quotationMap = quotations.stream().collect(Collectors.toMap(item ->
                    item.getSplitId() + "_" + item.getMaterialsId(), Function.identity()));

            //回写物料清单里面的投标物资数据
            //获取采购方案下的计划里的标包里的物料清单信息
            List<CompMaterialsVO> compMaterialsList = procurementSchemeService.listCompMaterials(biddingInfo.getSchemeId());
            for (CompMaterialsVO compMaterialsVO : compMaterialsList) {
                List<CompContractSplitMaterialsVO> compVOList = compMaterialsVO.getCompVOList();
                for (CompContractSplitMaterialsVO compContractSplitMaterialsVO : compVOList) {
                    List<CompMaterialsContentVO> compMaterialsContentVOList = compContractSplitMaterialsVO.getMaterialsLists();
                    for (CompMaterialsContentVO contentVO : compMaterialsContentVOList) {
                        String quotationKey = compContractSplitMaterialsVO.getSplitId() + "_" + contentVO.getId();
                        if (quotationMap.containsKey(quotationKey)){
                            //如果投标物料清单表中有数据匹配上了方案下面的物料清单信息，那就补充信息
                            BiddingListQuotation biddingListQuotation = quotationMap.get(quotationKey);
                            contentVO.setTaxUnitPrice(biddingListQuotation.getTaxUnitPrice());
                            contentVO.setNotTaxUnitPrice(biddingListQuotation.getNotTaxUnitPrice());
                            contentVO.setBillType(biddingListQuotation.getBillType());
                            contentVO.setFloatingPrice(biddingListQuotation.getFloatingPrice());
                            contentVO.setTaxPrice(biddingListQuotation.getTaxPrice());
                            contentVO.setNotTaxPrice(biddingListQuotation.getNotTaxPrice());
                            contentVO.setTaxRate(biddingListQuotation.getTaxRate());
                        } else {
                            contentVO.setTaxUnitPrice(null);
                            contentVO.setNotTaxUnitPrice(null);
                            contentVO.setFloatingPrice(null);
                            contentVO.setTaxPrice(null);
                            contentVO.setNotTaxPrice(null);
                        }
                    }
                }
            }
            vo.setMaterialsList(compMaterialsList);
        } catch (Exception e){
            log.info("获取供应商投标清单信息失败");
            e.printStackTrace();
        }


        //投标单清单报价详情信息
        for (BiddingListQuotation quotation : quotations) {
            BiddingQuotationDetailVO quotationVO = BeanCopierUtil.copyBean(quotation, BiddingQuotationDetailVO.class);

            if (null != quotation.getTaxUnitPrice()){
                quotationVO.setTaxUnitPrice(quotation.getTaxUnitPrice());
            }
            if (null != quotation.getNotTaxUnitPrice()){
                quotationVO.setNotTaxUnitPrice(quotation.getNotTaxUnitPrice());
            }

            quotationVO.setTaxPrice(quotation.getTaxPrice());
            quotationVO.setNotTaxPrice(quotation.getNotTaxPrice());

            for (MaterialsVO mvo : materialsList) {
                if (quotationVO.getMaterialsCode().equals(mvo.getMaterialsCode())){
                    quotationVO.setMaterialsName(mvo.getMaterialsName());
                    quotationVO.setSubjectMatterCode(mvo.getSubjectMatterCode());
                    quotationVO.setSubjectMatterName(mvo.getSubjectMatterName());
                    quotationVO.setSpecification(mvo.getSpecification());
                    quotationVO.setUnitMeasurement(mvo.getUnitMeasurement());
                    quotationVO.setCount(mvo.getCount());
                    quotationVO.setPriceIncludingTax(mvo.getUnitPriceInclTax());
                    break;
                }
            }

            //todo su含税单价（超合约计划单价字体颜色变红）
            quotationDetailVOList.add(quotationVO);
        }
        vo.setQuotationDetailVOList(quotationDetailVOList);

        //查询投标标书附件
        List<AttachmentVO> attachments = attachmentService.listAttachment(AttachmentTypeEnum.BIDING_DOCUMENT, biddingInfo.getId());
        vo.setAttachments(attachments);

        //查询方案信息
        ProcurementScheme scheme = procurementSchemeService.getById(biddingInfo.getSchemeId());
        vo.setPriceType(scheme.getPriceType());
        vo.setSubjectMatterType(scheme.getSubjectMatterType());

        return vo;
    }

    @Override
    public List<BiddingInfoDetailVO> getBiddingHistoryRecords(Long noticeId) {
        List<BiddingInfoDetailVO> voList = new ArrayList<>();
        List<BiddingInfo> biddingInfos = this.list(new LambdaQueryWrapper<BiddingInfo>()
                .eq(BiddingInfo::getNoticeId, noticeId).orderByDesc(BiddingInfo::getCreateTime)
                .select(BiddingInfo::getId));
        for (BiddingInfo biddingInfo : biddingInfos) {
            BiddingInfoDetailVO vo = this.getInfo(biddingInfo.getId());
            voList.add(vo);
        }
        return voList;
    }

    @Override
    public boolean collectDeposit(Long id, Integer collectDeposit) {
        //判断当前操作人是否是选择的财务人员，只允许设置的财务人员确认
        BiddingInfo biddingInfo = this.getById(id);
        ProcurementScheme procurementScheme = procurementSchemeService.getById(biddingInfo.getSchemeId());
        if (!ObjectUtils.isEmpty(procurementScheme) && StringUtils.isNotEmpty(procurementScheme.getFinanceConfirmId())){
            if (!procurementScheme.getFinanceConfirmId().equals(SecurityUtils.getUserId().toString())){
                throw new ParamValidateException("仅允许方案中设置的财务人员操作");
            }
        }
        //设置保证金为已收取状态
        return this.update(new LambdaUpdateWrapper<BiddingInfo>()
                .set(BiddingInfo::getCollectDeposit, collectDeposit)
                .eq(BiddingInfo::getId, id));
    }

    /* 进入下一个环节 */
    @Override
    public boolean intoBidOpeningStage(Long noticeId) {
        //查询当前公告流程状态
        TenderNotice tenderNotice = tenderNoticeService.getById(noticeId);
        if (!TenderNoticeStatusEnum.TENDER_ISSUE.getState().equals(tenderNotice.getNoticeStatus())){
            throw new ParamValidateException("投标公告状态已变更，请确认当前招标公告状态");
        }

        //查询投标单，是否存在未收取保证金的供应商，如果有，不允许进入下一环节（手动进入）
        long count = this.count(new LambdaQueryWrapper<BiddingInfo>()
                .eq(BiddingInfo::getNoticeId, noticeId)
                .eq(BiddingInfo::getSubmitStatus, 1)
                .eq(BiddingInfo::getBiddingStatus, BiddingInfoStatusEnum.HAVE_BACK.getState())
                .eq(BiddingInfo::getCollectDeposit, 0));
        if (count > 0){
            throw new ParamValidateException("有未收取保证金的供应商，不允许进入下一环节");
        }

        /* 招标对象 关闭二次报价 */
        tenderNoticeService.update(new LambdaUpdateWrapper<TenderNotice>()
                .set(TenderNotice::getTwiceQuotState, NumberConstant.ZERO)
                .eq(TenderNotice::getId, noticeId));

        /* 投标对象 关闭当前版本当前招标对象的二次报价 */
        this.update(new LambdaUpdateWrapper<BiddingInfo>()
                .set(BiddingInfo::getTwiceQuot, NumberConstant.ZERO)
                .eq(BiddingInfo::getNoticeId, noticeId)
                .eq(BiddingInfo::getTwiceQuotVersion, tenderNotice.getTwiceQuotVersion()==null?1:tenderNotice.getTwiceQuotVersion())
                .eq(BiddingInfo::getTwiceQuot, NumberConstant.ONE));

        /* 获取招标公告 和 采购方案的类型/名称 */
        TenderNoticeSchemeInfoVO detailVO = tenderNoticeService.getTenderNoticeSchemeInfo(noticeId);
        /* 根据招标公告流程状态 和 采购方案确定下一步流程 */
        Integer nextNoticeStatus = tenderNoticeService.nextTenderNoticeStatus(detailVO.getSchemeType(), tenderNotice.getNoticeStatus());

        //进入下一环节
        return tenderNoticeService.updateStatus(noticeId, nextNoticeStatus);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean abandonBid(AbandonBidVO abandonBidVO) {
        List<Long> biddingInfoIds = abandonBidVO.getBiddingInfoIds();
        //获取投标单未废标条数
        long count = this.count(new LambdaQueryWrapper<BiddingInfo>()
                .eq(BiddingInfo::getNoticeId, abandonBidVO.getNoticeId())
                .ne(BiddingInfo::getBiddingStatus, BiddingInfoStatusEnum.HAVE_ABANDON.getState()));
        if (!CollectionUtils.isEmpty(biddingInfoIds) && count == (long) biddingInfoIds.size()){
            //如果当前废标操作是废除剩下所有投标单，那就修改招标公告状态为废标
            tenderNoticeService.updateStatus(
                    abandonBidVO.getNoticeId(), TenderNoticeStatusEnum.ABANDON_BID.getState());
        }

        //2.修改投标单状态为废标
        boolean res = this.update(new LambdaUpdateWrapper<BiddingInfo>()
                .set(BiddingInfo::getBiddingStatus, BiddingInfoStatusEnum.HAVE_ABANDON.getState())
                .in(BiddingInfo::getId, biddingInfoIds));

        //3.添加废标记录
        BiddingAbandonRecord abandonRecord = BeanCopierUtil.copyBean(abandonBidVO, BiddingAbandonRecord.class);
        String biddingIds = biddingInfoIds.stream().map(Object::toString).collect(Collectors.joining(","));
        abandonRecord.setBiddingInfoIds(biddingIds);
        biddingAbandonRecordService.save(abandonRecord);

        //4.保存废标附件
        attachmentService.addAttachment(abandonBidVO.getAttachmentList(), AttachmentTypeEnum.BIDING_ABANDON_DOCUMENT,
                abandonRecord.getId());

        if (!res){
            throw new ParamValidateException("暂无投标信息，不允许废标");
        }

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean abandonBidMore(AbandonBidVO abandonBidVO) {
        boolean res = false;
        List<AbandonMoreVO> abandonMoreVOList = abandonBidVO.getAbandonMoreVOList();
        if (CollectionUtils.isEmpty(abandonMoreVOList)){
            throw new ParamValidateException("暂无废标参数信息，请重新废标");
        }
        //校验废标参数，判断当前招标公告状态是否允许废标操作
        verifyParam(abandonMoreVOList);

        for (AbandonMoreVO abandonMoreVO : abandonMoreVOList) {
            //1.修改招标公告状态为废标
            res = tenderNoticeService.updateStatus(
                    abandonMoreVO.getNoticeId(), TenderNoticeStatusEnum.ABANDON_BID.getState());
            //查询招标公告下面所有投标单的信息（排除掉已经废标的）
            List<BiddingInfo> biddingInfos = this.list(new LambdaQueryWrapper<BiddingInfo>()
                    .ne(BiddingInfo::getBiddingStatus, BiddingInfoStatusEnum.HAVE_ABANDON.getState())
                    .eq(BiddingInfo::getNoticeId, abandonMoreVO.getNoticeId()));
            List<Long> biddingInfoIds;
            if (!CollectionUtils.isEmpty(biddingInfos)){
                biddingInfoIds = biddingInfos.stream().map(BiddingInfo::getId).collect(Collectors.toList());

                //2.修改投标单状态为废标
                this.update(new LambdaUpdateWrapper<BiddingInfo>()
                        .set(BiddingInfo::getBiddingStatus, BiddingInfoStatusEnum.HAVE_ABANDON.getState())
                        .in(BiddingInfo::getId, biddingInfoIds));
                //3.添加废标记录
                BiddingAbandonRecord abandonRecord = BeanCopierUtil.copyBean(abandonBidVO, BiddingAbandonRecord.class);
                String biddingIds = biddingInfoIds.stream().map(Object::toString).collect(Collectors.joining(","));
                abandonRecord.setBiddingInfoIds(biddingIds);
                biddingAbandonRecordService.save(abandonRecord);
                //4.保存废标附件
                attachmentService.addAttachment(abandonBidVO.getAttachmentList(), AttachmentTypeEnum.BIDING_ABANDON_DOCUMENT,
                        abandonRecord.getId());
            }
        }
        return res;
    }

    /** 校验废标参数 */
    private void verifyParam(List<AbandonMoreVO> abandonMoreVOList){
        AbandonMoreVO abandonMoreVO = abandonMoreVOList.get(0);
        if (ObjectUtils.isEmpty(abandonMoreVO)){
            throw new ParamValidateException("暂无招标数据，无法废标");
        }
        if (null == abandonMoreVO.getNoticeId()){
            throw new ParamValidateException("暂无招标数据，无法废标");
        }
        TenderNotice tenderNotice = tenderNoticeService.getById(abandonMoreVO.getNoticeId());
        if (ObjectUtils.isEmpty(tenderNotice)){
            throw new ParamValidateException("暂无招标数据，无法废标");
        }
    }

    @Override
    public boolean startEvaluat(Long noticeId) {
        return tenderNoticeService.update(new LambdaUpdateWrapper<TenderNotice>()
                .set(TenderNotice::getIsEval, 1)
                .eq(TenderNotice::getId, noticeId));
    }

    @Override
    public List<ExpertEvalStatusVO> getExpertEvalStatus(Long noticeId, Integer evalStatus) {
        List<ExpertEvalStatusVO> expertVos = new ArrayList<>();
        //查询需要查询的专家
        List<BiddingEvaluatExpert> expertList = biddingEvaluatExpertService.list(new LambdaQueryWrapper<BiddingEvaluatExpert>()
                .eq(BiddingEvaluatExpert::getNoticeId, noticeId)
                .eq(BiddingEvaluatExpert::getIsJoin, 1));

        for (BiddingEvaluatExpert expert : expertList) {
            ExpertEvalStatusVO expertVo = new ExpertEvalStatusVO();
            expertVo.setExpertId(expert.getExpertId());
            expertVo.setExpertName(expert.getExpertName());
            //获取几个供应商首轮报价信息
            List<BiddingInfo> biddingInfos = this.list(new LambdaQueryWrapper<BiddingInfo>()
                    .eq(BiddingInfo::getNoticeId, noticeId)
                    .eq(BiddingInfo::getBiddingStatus, BiddingInfoStatusEnum.HAVE_BACK.getState())
                    .isNull(BiddingInfo::getParentId));
            boolean flag = true;
            for (BiddingInfo bid : biddingInfos){
                //获取供应商最新一轮报价数据
                //查询二次报价的数据
                BiddingInfo newestBiddingInfo = this.getOne(new LambdaQueryWrapper<BiddingInfo>()
                        .eq(BiddingInfo::getParentId, bid.getId())
                        .orderByDesc(BiddingInfo::getCreateTime).last("limit 1"));
                if (!ObjectUtils.isEmpty(newestBiddingInfo)){
                    //换成最新一条投标单
                    bid = newestBiddingInfo;
                }
                //查询专家对这条最新的投标单是否有评标数据
                if (expertScoreService.count(new LambdaQueryWrapper<ExpertScore>()
                        .eq(ExpertScore::getBiddingInfoId, bid.getId())
                        .eq(ExpertScore::getExpertId, expert.getExpertId())) == 0) {
                    //如果没产生数据，就代表专家对这个供应商没有评分
                    //只要有一个投标单没有数据，就是该专家未评完标
                    flag = false;
                    break;
                }
            }
            if (flag){
                expertVo.setEvalStatus(1);
                expertVo.setEvalStatusText("已完成");
            } else {
                expertVo.setEvalStatus(0);
                expertVo.setEvalStatusText("未完成");
            }

            if (evalStatus != null && 0 == evalStatus){
                if (!flag){
                    expertVos.add(expertVo);
                }
            } else if (evalStatus != null && 1 == evalStatus){
                if (flag){
                    expertVos.add(expertVo);
                }
            } else {
                expertVos.add(expertVo);
            }

        }
        return expertVos;
    }

    @Override
    public boolean evaluatBid(EvaluatBidVO evaluatBidVO) {
        if (tenderNoticeService.getCountTenderNoticeStatus(
                evaluatBidVO.getNoticeId(), TenderNoticeStatusEnum.EVALUATION_BID.getState()) == 0){
            throw new ParamValidateException("投标公告状态已变更，请确认当前招标公告状态");
        }

        TenderNoticeSchemeInfoVO detailVO = tenderNoticeService.getTenderNoticeSchemeInfo(evaluatBidVO.getNoticeId());
        Integer nextNoticeStatus = tenderNoticeService.nextTenderNoticeStatus(detailVO.getSchemeType(), detailVO.getNoticeStatus());

        //1.修改招标公告状态，关闭专家评标
        boolean res = tenderNoticeService.update(new LambdaUpdateWrapper<TenderNotice>()
                .set(TenderNotice::getIsEval, 0)
                .set(TenderNotice::getNoticeStatus, nextNoticeStatus)
                .eq(TenderNotice::getId, evaluatBidVO.getNoticeId()));
        return res;
    }

    @Override
    public String getTwiceTime(Long noticeId) {
        BiddingInfo biddingInfo = this.getOne(new LambdaQueryWrapper<BiddingInfo>()
                .eq(BiddingInfo::getNoticeId, noticeId)
                .eq(BiddingInfo::getTwiceQuot, NumberConstant.ONE)
                .last("limit 1"));
        if (!ObjectUtils.isEmpty(biddingInfo)){
            Date twiceTime = biddingInfo.getTwiceTime();
            return DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, twiceTime);
        }
        return null;
    }

    /* 管理端 二次报价  */
    @Override
    public boolean twiceBidConf(TwiceBidConfVO twiceBidConfVO) {
        if (tenderNoticeService.getCountTenderNoticeStatus(
                twiceBidConfVO.getNoticeId(), TenderNoticeStatusEnum.EVALUATION_BID.getState()) == 0){
            throw new ParamValidateException("投标公告状态已变更，请确认当前招标公告状态");
        }
        TenderNotice tenderNotice = tenderNoticeService.getOne(new LambdaQueryWrapper<TenderNotice>()
                .eq(TenderNotice::getId, twiceBidConfVO.getNoticeId()));
        /* 招标对象 二次报价状态 */
        if(tenderNotice.getTwiceQuotState()!=null&&tenderNotice.getTwiceQuotState().equals(NumberConstant.ONE) && !tenderNotice.getTwiceQuotVersion().equals(NumberConstant.ONE)){
            throw new ParamValidateException("已开启二次报价，再次开启请先结束当前的二次报价。");
        }
        /* 获取当前版本的所有投标数据 */
        List<BiddingInfo> biddingInfoList = this.list(new LambdaUpdateWrapper<BiddingInfo>()
                    .eq(BiddingInfo::getNoticeId, tenderNotice.getId())
                    .eq(BiddingInfo::getTwiceQuotVersion,( tenderNotice.getTwiceQuotVersion()==null?1: tenderNotice.getTwiceQuotVersion())));
        /* 全部复制一份，防止后面需求变化。 */
        /* 这个版本号是根据招标对象走的，第二次供应商未报价，第三次依然可以选中该供应商继续报价。 */
        Date date = new Date();
        for (BiddingInfo bidInfo : biddingInfoList) {
            bidInfo.setCreateTime(date);
            bidInfo.setTwiceTime(twiceBidConfVO.getTwiceTime());/* 二次报价截至时间 */
            bidInfo.setTwiceQuotVersion((tenderNotice.getTwiceQuotVersion()==null?1:tenderNotice.getTwiceQuotVersion())+1);/* 二次报价版本号加一 */
            /* 选中的供应商 开启调价 */
            if(twiceBidConfVO.getBiddingInfoIds().contains(bidInfo.getId())){
                bidInfo.setTwiceQuot(NumberConstant.ONE);/* 开启调价 */
                bidInfo.setPriceChangeState(NumberConstant.ZERO);/* 默认未调价 */
            }else{
                /* 未选中的判断之前是否放弃调价。是放弃调价就是放弃调价，不是就默认为未调价 */
                bidInfo.setTwiceQuot(NumberConstant.ZERO);/* 关闭调价 */
                if(bidInfo.getPriceChangeState()!=null&&bidInfo.getPriceChangeState().equals(NumberConstant.ONE)){
                    bidInfo.setPriceChangeState(NumberConstant.ONE);/* 已调价 */
                }else {
                    bidInfo.setPriceChangeState(NumberConstant.TWO);/* 否则 放弃调价 */
                }
            }
            List<AttachmentVO> attachments = attachmentService.listAttachment(AttachmentTypeEnum.BIDING_DOCUMENT, bidInfo.getId());
            List<AttachmentRequestVO> attachmentList = new ArrayList<>();
            for (AttachmentVO a:attachments){
                AttachmentRequestVO av = new AttachmentRequestVO();
                av.setFileName(a.getFileName());
                av.setFileUrl(a.getFileUrl());
                attachmentList.add(av);
            }
            bidInfo.setId(null);
            save(bidInfo);
            /* 复制投标标书附件 */
            attachmentService.addAttachment(attachmentList, AttachmentTypeEnum.BIDING_DOCUMENT, bidInfo.getId());
        }
        tenderNoticeService.update(new LambdaUpdateWrapper<TenderNotice>()
                .set(TenderNotice::getTwiceQuotState, NumberConstant.ONE)
                .set(TenderNotice::getTwiceTime, twiceBidConfVO.getTwiceTime())/* 二次报价截至时间 */
                .set(TenderNotice::getTwiceQuotVersion, (tenderNotice.getTwiceQuotVersion()==null?1:tenderNotice.getTwiceQuotVersion())+1)/* 版本号累加 */
                .eq(TenderNotice::getId, twiceBidConfVO.getNoticeId()));
        return true;
    }

    /* 管理端结束 二次报价 */
    @Override
    public boolean twiceBidFinish(TwiceBidConOverVO twiceBidConfVO) {
        if (tenderNoticeService.getCountTenderNoticeStatus(
                twiceBidConfVO.getNoticeId(), TenderNoticeStatusEnum.EVALUATION_BID.getState()) == 0){
            throw new ParamValidateException("投标公告状态已变更，请确认当前招标公告状态");
        }
        TenderNotice tenderNotice = tenderNoticeService.getById(twiceBidConfVO.getNoticeId());
        /* 招标对象 二次报价状态 */
        if(tenderNotice.getTwiceQuotState()!=null&&tenderNotice.getTwiceQuotState().equals(NumberConstant.ZERO)){
            throw new ParamValidateException("当前的二次报价已经是关闭状态");
        }
        /* 招标对象 关闭二次报价 */
        tenderNoticeService.update(new LambdaUpdateWrapper<TenderNotice>()
                .set(TenderNotice::getTwiceQuotState, NumberConstant.ZERO)
                .eq(TenderNotice::getId, twiceBidConfVO.getNoticeId()));

        /* 投标对象 关闭当前版本当前招标对象 所有的投标数据 的二次报价开关 */
        this.update(new LambdaUpdateWrapper<BiddingInfo>()
                .set(BiddingInfo::getTwiceQuot, NumberConstant.ZERO)
                .eq(BiddingInfo::getNoticeId, twiceBidConfVO.getNoticeId())
                .eq(BiddingInfo::getTwiceQuotVersion, tenderNotice.getTwiceQuotVersion()==null?2:tenderNotice.getTwiceQuotVersion())
                .eq(BiddingInfo::getTwiceQuot, NumberConstant.ONE));
        /* 投标对象 关闭当前版本当前招标对象 未调价的 二次报价 */
        this.update(new LambdaUpdateWrapper<BiddingInfo>()
                .set(BiddingInfo::getTwiceQuot, NumberConstant.ZERO)
                /* 将未调价的状态改成放弃调价 */
                .set(BiddingInfo::getPriceChangeState, NumberConstant.TWO)/* 放弃调价 */
                .eq(BiddingInfo::getPriceChangeState, NumberConstant.ZERO)/* 未调价 */

                .eq(BiddingInfo::getNoticeId, twiceBidConfVO.getNoticeId())
                .eq(BiddingInfo::getTwiceQuotVersion, tenderNotice.getTwiceQuotVersion()==null?2:tenderNotice.getTwiceQuotVersion()));
        return true;
    }

    @Override
    public boolean uploadEvalAttach(EvalAttachUploadVO uploadVO) {
        //保存投标标书附件
        attachmentService.addAttachment(uploadVO.getAttachmentList(), AttachmentTypeEnum.EVAL_DOCUMENT,
                uploadVO.getBiddingInfoId());
        return true;
    }

    @Override
    public List<BiddingVendorVO> listBiddingVendor(Long schemeId) {
        return super.list(new LambdaQueryWrapper<BiddingInfo>()
                .eq(BiddingInfo::getSchemeId,schemeId)
                .eq(BiddingInfo::getBiddingStatus,1))
                .stream()
                .collect(Collectors.groupingBy(BiddingInfo::getVendorId))
                .values().stream()
                .map(info -> BeanCopierUtil.copyBean(info, BiddingVendorVO.class))
                .collect(Collectors.toList());
    }

    @Override
    public List<CalibrationReportListVO> getCalibrationReportList(Long noticeId) {
        List<CalibrationReportListVO> calList = baseMapper.findCalibrationReportList(noticeId, TenderNoticeStatusEnum.CALI_REPORT.getState(),
                BiddingInfoStatusEnum.HAVE_ABANDON.getState());
        //计算所有专家的评标得分-综合分
        for (CalibrationReportListVO cal : calList) {
            //投标单id
            Long biddingInfoId = cal.getBiddingInfoId();
            Long schemeId = cal.getSchemeId();
            //获取当前招标公告所有专家的评分数据
            List<ExpertScore> expertScores = expertScoreService.list(new LambdaQueryWrapper<ExpertScore>()
                    .eq(ExpertScore::getBiddingInfoId, biddingInfoId)
                    .eq(ExpertScore::getSchemeId, schemeId));
            BigDecimal avgBusTotalScore = BigDecimal.ZERO;
            BigDecimal avgTechTotalScore = BigDecimal.ZERO;
            if (!CollectionUtils.isEmpty(expertScores)){
                //专家人数
                int expertSize = expertScores.size();
                BigDecimal busTotalScore = BigDecimal.ZERO;
                BigDecimal techTotalScore = BigDecimal.ZERO;
                for (ExpertScore expertScore : expertScores) {
                    //1.汇总商务分数
                    //2.汇总技术分数
                    busTotalScore = busTotalScore.add(expertScore.getBusScore());
                    techTotalScore = techTotalScore.add(expertScore.getTechScore());
                }
                avgBusTotalScore = busTotalScore.divide(new BigDecimal(expertSize), 2, RoundingMode.HALF_UP);
                avgTechTotalScore = techTotalScore.divide(new BigDecimal(expertSize), 2, RoundingMode.HALF_UP);
            }
            cal.setAvgBusTotalScore(avgBusTotalScore);
            cal.setAvgTechTotalScore(avgTechTotalScore);
            cal.setTotalScore(avgBusTotalScore.add(avgTechTotalScore));
        }
        //综合排名（排序）按照综合分由高到低排序，综合分一致按不含税总价排序
        if (!CollectionUtils.isEmpty(calList)){
            //根据综合分进行排序
            Collections.sort(calList);
            //补充字段内容
            fillField(calList);
        }

        return calList;
    }

    private void fillField(List<CalibrationReportListVO> calList){
        int rank = 0;
        for (CalibrationReportListVO calibratDataVO : calList) {
            rank++;
            calibratDataVO.setRank(rank);
            calibratDataVO.setCandidate("第" + rank + "中标候选人");
        }
    }


    @Override
    public List<BiddingQuotationSummaryListVO> getBiddingQuotationSummary(BiddingQuotationQueryVO queryVO) {
        List<BiddingQuotationListVO> list = super.list(new LambdaQueryWrapper<BiddingInfo>()
                        .eq(null!= queryVO.getSchemeId(),BiddingInfo::getSchemeId,queryVO.getSchemeId())
                        .eq(null!= queryVO.getNoticeId(),BiddingInfo::getNoticeId,queryVO.getNoticeId())
                       .notIn(BiddingInfo::getBiddingStatus,BiddingInfoStatusEnum.HAVE_ABANDON.getState()).orderByAsc(BiddingInfo::getNotTaxPrice)
                )
                .stream()
                .map(info -> BeanCopierUtil.copyBean(info, BiddingQuotationListVO.class))
                .collect(Collectors.toList());
        List<BiddingQuotationSummaryListVO> listVOList = new ArrayList<>();
        Map<Long, List<BiddingQuotationListVO>> groupBy = list.stream().collect(Collectors.groupingBy(BiddingQuotationListVO::getVendorId));
        Iterator<Map.Entry<Long, List<BiddingQuotationListVO>>> iterator = groupBy.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, List<BiddingQuotationListVO>> entry = iterator.next();
            List<BiddingQuotationListVO> biddingQuotationListVOS =  entry.getValue();
            biddingQuotationListVOS = biddingQuotationListVOS.stream().sorted(Comparator.comparing(BiddingQuotationListVO::getId)).collect(Collectors.toList());
            BiddingQuotationSummaryListVO biddingQuotationSummaryListVO = new BiddingQuotationSummaryListVO();
            BeanUtil.copyProperties(biddingQuotationListVOS.get(0),biddingQuotationSummaryListVO);
            if (biddingQuotationListVOS.size() > 0){
                BiddingQuotationListVO first = biddingQuotationListVOS.get(0);
//                first.setTaxPrice(first.getTaxPrice().setScale(2, ROUND_DOWN));
//                first.setNotTaxPrice(first.getNotTaxPrice().setScale(2, ROUND_DOWN));
//                first.setTaxPricePattern(NumberUtil.decimalFormat("#,###.00", first.getTaxPrice()));
//                first.setNotTaxPricePattern(NumberUtil.decimalFormat("#,###.00", first.getNotTaxPrice()));
                biddingQuotationSummaryListVO.setFistQuotation(first);

                BiddingQuotationListVO last = biddingQuotationListVOS.get(biddingQuotationListVOS.size() - 1);
//                last.setTaxPrice(last.getTaxPrice().setScale(2, ROUND_DOWN));
//                last.setNotTaxPrice(last.getNotTaxPrice().setScale(2, ROUND_DOWN));
//                last.setTaxPricePattern(NumberUtil.decimalFormat("#,###.00", last.getTaxPrice()));
//                last.setNotTaxPricePattern(NumberUtil.decimalFormat("#,###.00", last.getNotTaxPrice()));
                biddingQuotationSummaryListVO.setLastQuotation(last);
            }
            biddingQuotationSummaryListVO.setAllQuotation(biddingQuotationListVOS);
            listVOList.add(biddingQuotationSummaryListVO);
        }
        return listVOList;
    }

    /* 评标汇总，只显示最终一轮的 */
    @Override
    public List<BidEvaluationVo>  getBidEvaluationList(Long noticeId, int scoreType) {
        List<BidEvaluationVo> bidEvaluationVoList = new ArrayList<>();
        BiddingQuotationQueryVO queryVO = new BiddingQuotationQueryVO();
        queryVO.setNoticeId(noticeId);

        //查询设置的评标专家人员
        List<BiddingEvaluatExpert> expertList = biddingEvaluatExpertService.list(new LambdaQueryWrapper<BiddingEvaluatExpert>()
                .eq(BiddingEvaluatExpert::getNoticeId, noticeId)
                .eq(BiddingEvaluatExpert::getExpertType, scoreType)
                .eq(BiddingEvaluatExpert::getIsJoin, 1));
        //查询首轮供应商报价数据，有首轮数据才能在列表中显示
        List<BiddingQuotationListVO> list = baseMapper.findBiddingQuotationListTongJi(queryVO);


//        /* 按供应商分组 将招标对象 的 投标数据 分组，并按版本号排序。 */
//        Map<Long,LinkedList<BiddingQuotationListVO>> hasMap = new HashMap<>();
//        for (BiddingQuotationListVO vo : list) {
//            if(hasMap.get(vo.getVendorId())==null || hasMap.get(vo.getVendorId()).isEmpty()){
//                hasMap.put(vo.getVendorId(),new LinkedList<>(Arrays.asList(vo)));
//            }else {
//                LinkedList link = hasMap.get(vo.getVendorId());
//                link.add(vo);
//                Collections.sort(link, new VersionComparator());
//                hasMap.put(vo.getVendorId(),link);
//            }
//        }
//        List<BiddingQuotationListVO> listReturn = new ArrayList<>();
//        for(Long vendorId : hasMap.keySet()) {
//            /* 获取该供应商最终的投标数据 */
//            BiddingQuotationListVO vo = hasMap.get(vendorId).get((Math.max((hasMap.get(vendorId).size() - 1), 0)));
//
//            listReturn.add(vo);
//        }



        for (BiddingQuotationListVO vo : list) {

            BigDecimal busTotalScore = BigDecimal.ZERO;
            BigDecimal techTotalScore = BigDecimal.ZERO;
            List<BidEvaluationExpertScoreVo> expertScoreVoList = new ArrayList<>();
            int expertSize = expertList.size();
            for (BiddingEvaluatExpert evaluatExpert : expertList) {
                //查询评标专家产生的评分数据
                ExpertScore expertScore = expertScoreService.getOne(new LambdaQueryWrapper<ExpertScore>()
                        .eq(ExpertScore::getNoticeId, noticeId)
                        .eq(ExpertScore::getVendorId, vo.getVendorId())
                        .eq(ExpertScore::getExpertId, evaluatExpert.getExpertId())
                        .orderByDesc(ExpertScore::getCreateTime).last("limit 1"));
                BidEvaluationExpertScoreVo expertScoreVo = new BidEvaluationExpertScoreVo();
                expertScoreVo.setExpertId(evaluatExpert.getExpertId());
                expertScoreVo.setExpertName(evaluatExpert.getExpertName());
                expertScoreVoList.add(expertScoreVo);
                if (!ObjectUtils.isEmpty(expertScore)){
                    BigDecimal techScore = expertScore.getTechScore() == null ? BigDecimal.ZERO : expertScore.getTechScore();
                    BigDecimal busScore = expertScore.getBusScore() == null ? BigDecimal.ZERO : expertScore.getBusScore();
                    expertScoreVo.setScore(scoreType == 1 ? techScore : busScore);
                    //1.汇总商务分数
                    //2.汇总技术分数
                    busTotalScore = busTotalScore.add(busScore);
                    techTotalScore = techTotalScore.add(techScore);
                } else {
                    expertSize = expertSize-1;
                }
            }
            BigDecimal avgBusTotalScore = BigDecimal.ZERO;
            BigDecimal avgTechTotalScore = BigDecimal.ZERO;
            if (expertSize > 0){
                avgBusTotalScore = busTotalScore.divide(new BigDecimal(expertSize), 2, RoundingMode.HALF_UP);
                avgTechTotalScore = techTotalScore.divide(new BigDecimal(expertSize), 2, RoundingMode.HALF_UP);
            }

            BidEvaluationVo bidEvaluationVo = new BidEvaluationVo();
            bidEvaluationVo.setVendorId(vo.getVendorId());
            bidEvaluationVo.setVendorName(vo.getVendorName());
            bidEvaluationVo.setBidEvaluationExpertScoreVoList(expertScoreVoList);
            bidEvaluationVo.setScore(scoreType==1 ? avgTechTotalScore : avgBusTotalScore);
            bidEvaluationVoList.add(bidEvaluationVo);
        }
        return bidEvaluationVoList;
    }

    @Override
    public boolean urgeExpertMes(UrgeExpertMesVO urgeExpertMesVO) {
        Expert expert = expertService.getOne(new LambdaQueryWrapper<Expert>().eq(Expert::getId, urgeExpertMesVO.getExpertId()));
        TenderNoticeSchemeInfoVO detailVO = tenderNoticeService.getTenderNoticeSchemeInfo(urgeExpertMesVO.getNoticeId());

        //belongOrgName采购人公司取值：成控系统右上角登录人所属公司
        //projectName采购人项目取值：成控系统右上角登录人所在项目
        StringBuilder placeBuff = new StringBuilder();
        placeBuff.append(urgeExpertMesVO.getBelongOrgName()).append("公司");
        placeBuff.append(urgeExpertMesVO.getProjectName()).append("项目");
        placeBuff.append(detailVO.getProcurementSchemeName());

        if (StringUtils.isNotEmpty(expert.getExpertPhone())){
            LinkedHashMap<String, String> varParam = new LinkedHashMap<>();
            varParam.put("name", expert.getExpertName());
            varParam.put("place", placeBuff.toString());
            varParam.put("catalogue", expert.getExpertType() == 1 ? "技术" : "商务");
            smsSenderUtil.sendMessage(SmsTemplateEnum.EXPERT_BID_EVA_NOTICE.getCode(), expert.getExpertPhone(), varParam);
        }
        return true;
    }


    @Override
    public List<BiddingInfo> getMaxPriceVersion(Long noticeId, Long schemeId){
        return baseMapper.getMaxPriceVersion(noticeId,schemeId);
    }

}
class VersionComparator implements Comparator<BiddingQuotationListVO> {
    @Override
    public int compare(BiddingQuotationListVO v1, BiddingQuotationListVO v2) {
        if(v1.getTwiceQuotVersion()!=null&&v2.getTwiceQuotVersion()!=null)
            return v1.getTwiceQuotVersion().compareTo(v2.getTwiceQuotVersion());
        return 0;
    }
}
