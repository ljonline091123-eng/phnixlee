<template>
  <div class="app-container">
    <BackButton path="/procurement/sign-contract" title="新增合同信息">
      <div>
        <el-button
          type="primary"
          size="mini"
          @click="handelCalibrationApproval"
        >
          审批详情
        </el-button>
        <el-button type="primary" size="mini" @click="submitForm"
          >保存</el-button>
          <el-button type="primary" v-if="typeContract=='add' && parseInt(firstForm.agreementPaymentItem.totalAmountIncTax)<50000"  size="mini" @click="avoidSubmitForm"
          >免审提交</el-button>
      </div>
    </BackButton>
    <div class="context">
      <el-form ref="firstForm" :model="firstForm" label-width="210px">
      <div class="tabs-box">
      <!-- 基本信息 / 合同附件 -->
      <el-tabs v-model="activeName" @tab-click="attachmenthandleTabClick">
        <el-tab-pane label="基本信息" name="first">
          <!-- 基本信息 -->
          <commonTitle>基本信息</commonTitle>
          <div style="margin-bottom: 8px">
            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="归属本级组织：" prop="agreement.belongOrganizationName"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入归属本级组织' }]">
                  <el-input disabled  v-model="firstForm.agreement.belongOrganizationName" placeholder="请输入归属本级组织"
                    clearable />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="归属最小核算项目：" prop="agreement.belongAccountingItem"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入归属最小核算项目' }]">
                  <el-input disabled v-model="firstForm.agreement.belongAccountingItem" placeholder="请输入采购任务名称"
                    clearable />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="归属最小核算项目编码：" prop="agreement.belongAccountingItemCode"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入归属最小核算项目编码' }]">
                  <el-input disabled v-model="firstForm.agreement.belongAccountingItemCode" placeholder="请输入采购任务名称"
                    clearable />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="合同名称：" prop="agreement.agreementName"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入合同名称' }]">
                  <el-input  v-model="firstForm.agreement.agreementName" placeholder="请输入合同名称" clearable/>
                </el-form-item>
              </el-col>

              <el-col :span="8">
                <el-form-item label="合同编码：" prop="agreement.agreementCode" disabled>
                  <el-input disabled v-model="firstForm.agreement.agreementCode" placeholder="系统自动生成" clearable />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="单位内部合同管理编码：" prop="agreement.innerAgreementCode">
                  <el-input v-model="firstForm.agreement.innerAgreementCode" placeholder="请输入单位内部合同管理编码" clearable />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="甲方名称：" prop="agreement.partyAName"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入甲方名称' }]">
                  <el-input disabled v-model="firstForm.agreement.partyAName" placeholder="请输入甲方名称" clearable />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="乙方名称：" prop="agreement.partyBName"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入乙方名称' }]">
                  <el-input disabled v-model="firstForm.agreement.partyBName" placeholder="请输入乙方名称" clearable />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="支出业务分类：" prop="agreement.expenditureBusinessType"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入支出业务分类' }]">
                  <el-input disabled v-model="firstForm.agreement.expenditureBusinessType" placeholder="请输入支出业务分类"
                    clearable />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="交易标的物：" prop="agreement.subjectMatterName"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入交易标的物' }]">
                  <el-input  v-model="firstForm.agreement.subjectMatterName" disabled placeholder="请输入交易标的物"
                    clearable />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="支付周期：" prop="agreement.paymentCycle"
                  :rules="[{ required: true, trigger: 'change', message: '请选择支付周期' }]">
                  <el-select style="width: 100%" v-model="firstForm.agreement.paymentCycle" placeholder="请选择支付周期">
                    <el-option v-for="dict in dictObj.payment_cycle" :key="dict.value" :label="dict.label"
                      :value="dict.value">
                    </el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="支付方式：" prop="agreement.paymentWay"
                  :rules="[{ required: true, trigger: 'change', message: '请选择支付方式'}]">
                  <el-select style="width: 100%" multiple v-model="firstForm.agreement.paymentWay" placeholder="请选择支付方式">
                    <el-option v-for="dict in dictObj.payment_way" :key="dict.value" :label="dict.label"
                      :value="dict.value">
                    </el-option>
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="乙方法人代表：" prop="agreement.partyBLegalName"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入乙方法人代表' }]">
                  <el-input  disabled v-model="firstForm.agreement.partyBLegalName" placeholder="请输入乙方法人代表" clearable />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="身份证：" prop="agreement.partyBLegalIdCard"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入身份证' },{validator: isCardId, trigger: 'blur'}]">
                  <el-input :disabled="typeContract=='add'?true:false" v-model="firstForm.agreement.partyBLegalIdCard" placeholder="请输入身份证" clearable />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="联系方式：" prop="agreement.partyBLegalPhone"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入联系方式' },{validator: isMobile, trigger: 'blur'}]">
                  <el-input :disabled="typeContract=='add'?true:false" v-model="firstForm.agreement.partyBLegalPhone" placeholder="请输入联系方式" clearable />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="乙方现场实际履职负责人：" prop="agreement.partyBResponsibleName"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入乙方现场实际履职负责人' }]">
                  <el-input :disabled="typeContract=='add'?true:false" v-model="firstForm.agreement.partyBResponsibleName" placeholder="请输入乙方现场实际履职负责人"
                    clearable />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="身份证：" prop="agreement.partyBResponsibleIdCard"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入身份证' },{validator: isCardId, trigger: 'blur'}]">
                  <el-input :disabled="typeContract=='add'?true:false" v-model="firstForm.agreement.partyBResponsibleIdCard" placeholder="请输入身份证" clearable />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="联系方式：" prop="agreement.partyBResponsiblePhone"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入联系方式' },{validator: isMobile, trigger: 'blur'}]">
                  <el-input :disabled="typeContract=='add'?true:false" v-model="firstForm.agreement.partyBResponsiblePhone" placeholder="请输入联系方式" clearable />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="10" v-if="[4, 5].includes(contractType)">
              <el-col :span="8">
                <el-form-item label="进场日期：" prop="agreement.entryDate"
                  :rules="[{ required: true, trigger: 'change', message: '请选择进场日期' }]">
                  <el-date-picker v-model="firstForm.agreement.entryDate" type="date" placeholder="选择日期"
                    value-format="yyyy-MM-dd" format="yyyy-MM-dd" style="width: 100%;"></el-date-picker>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="完工日期：" prop="agreement.finishDate"
                  :rules="[{ required: true, trigger: 'change', message: '请选择完工日期' }]">
                  <el-date-picker v-model="firstForm.agreement.finishDate" type="date" placeholder="选择日期"
                    value-format="yyyy-MM-dd" format="yyyy-MM-dd" style="width: 100%;"></el-date-picker>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="工期(天)" prop="agreement.duration">
                  <el-input :value="durationComputed(firstForm.agreement.entryDate, firstForm.agreement.finishDate)" placeholder="系统自动计算" disabled >
                    <template #append>天</template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="价格形式：" prop="agreement.priceForm"
                              :rules="[{ required: true, trigger: 'change', message: '请选择价格形式' }]">
                  <el-select v-model="firstForm.agreement.priceForm" placeholder="请选择价格形式" style="width: 100%;">
                    <el-option v-for="dict in dictObj.priceForm" :key="dict.value" :label="dict.label"
                        :value="dict.value">
                      </el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8" v-if="firstForm.agreement.priceForm == 2">
                <el-form-item label="下浮比例(%)" prop="agreement.discountRatio"
                              :rules="[{ required: true, trigger: 'blur', message: '请输入下浮比例' }]">
                  <el-input v-model="firstForm.agreement.discountRatio" />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="10" v-if="[6].includes(contractType)">
              <el-col :span="8">
                <el-form-item label="合同履行开始日期：" prop="agreement.contractStartDate"
                  :rules="[{ required: true, trigger: 'change', message: '请选择合同履行开始日期' }]">
                  <el-date-picker v-model="firstForm.agreement.contractStartDate" type="date" placeholder="选择日期"
                    value-format="yyyy-MM-dd" format="yyyy-MM-dd"></el-date-picker>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="合同履行结束日期：" prop="agreement.contractEndDate"
                  :rules="[{ required: true, trigger: 'change', message: '请选择合同履行结束日期' }]">
                  <el-date-picker v-model="firstForm.agreement.contractEndDate" type="date" placeholder="选择日期"
                    value-format="yyyy-MM-dd" format="yyyy-MM-dd"></el-date-picker>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="工期" prop="agreement.duration">
                  <el-input :value="durationComputed(firstForm.agreement.contractStartDate, firstForm.agreement.contractEndDate)" placeholder="系统自动计算" disabled >
                    <template #append>天</template>
                  </el-input>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="合同履行地：" prop="agreement.agreementPerformAddress"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入合同履行地' }]">
                  <el-input disabled v-model="firstForm.agreement.agreementPerformAddress" placeholder="请输入合同履行地"
                    clearable />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="国家地区代码(履行地)：" prop="agreement.agreementPerformCountry"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入国家地区代码(履行地)' }]">
                  <el-input disabled v-model="firstForm.agreement.agreementPerformCountry" placeholder="请输入国家地区代码(履行地)"
                    clearable />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="行政区划代码(履行地)：" prop="agreement.agreementPerformDistrict"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入行政区划代码(履行地)' }]">
                  <el-input disabled v-model="firstForm.agreement.agreementPerformDistrict" placeholder="请输入行政区划代码(履行地)"
                    clearable />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row v-if="typeContract=='add'" :gutter="10">
              <el-col :span="8" class="grid-cell">
                <el-form-item label=" 合同模板："  :rules="[{ required: true, trigger: 'blur', message: '请选择模板' }]"  prop="agreement.contractTemplateName">
                  <!-- <template v-if="formData.contractTemplateName">
                    <a href="javascript:;" @click="getBcTemplateList(1)">{{
                      formData.contractTemplateName
                    }}</a>
                  </template> -->
                  <el-button
                    size="small"
                    type="primary"
                    @click="getBcTemplateList(1)"
                    >{{firstForm.agreement.contractTemplateName?firstForm.agreement.contractTemplateName:'选择模板'}}</el-button
                  >
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="10" v-if="[4, 5].includes(contractType)">
              <el-col :span="24">
                <el-form-item label="工程范围及工作内容：" prop="agreement.scopeOfWork"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入工程范围及工作内容,内容字符上限为1000' }]">
                  <el-input maxlength="1000"  v-model="firstForm.agreement.scopeOfWork" placeholder="请输入工程范围及工作内容" clearable />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="10" v-if="[2, 3].includes(contractType)">
              <el-col :span="8">
                <el-form-item label="计租方式：" prop="agreement.rentalMethod"
                  :rules="[{ required: true, trigger: 'change', message: '请选择计租方式' }]">
                  <el-select v-model="firstForm.agreement.rentalMethod" placeholder="请选择择计租方式" style="width: 100%;">
                    <el-option label="算头不算尾" :value="1" />
                    <el-option label="算头又算尾" :value="2" />
                    <el-option label="算尾不算头" :value="3" />
                    <el-option label="头尾都不算" :value="4" />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <!-- 款项信息 -->
          <commonTitle>款项信息</commonTitle>
          <div style="margin-bottom: 8px">
            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="币种：" prop="agreementPaymentItem.currency"
                  :rules="[{ required: true, trigger: 'change', message: '请选择币种' }]">
                  <el-select style="width: 100%" v-model="firstForm.agreementPaymentItem.currency" placeholder="请选择币种">
                    <el-option v-for="dict in dictObj.currency" :key="dict.value" :label="dict.label"
                      :value="dict.value">
                    </el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="发票类型：" prop="agreementPaymentItem.invoiceType"
                  :rules="[{ required: true, trigger: 'change', message: '请选择发票类型' }]">
                  <el-select style="width: 100%" v-model="firstForm.agreementPaymentItem.invoiceType"
                    placeholder="请选择发票类型">
                    <el-option v-for="dict in dictObj.invoice_type" :key="dict.value" :label="dict.label"
                      :value="dict.value">
                    </el-option>
                  </el-select>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="合同税率：" prop="agreementPaymentItem.contractTaxRate"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入合同税率' }, {validator: validateNumber, trigger: 'blur'}]">
                  <el-input v-model="firstForm.agreementPaymentItem.contractTaxRate" placeholder="请输入合同税率" clearable>
                    <template #append>%</template>
                  </el-input>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="约定预付款比例(%)：" prop="agreementPaymentItem.prepaymentRatio"
                  :rules="[{validator: validateNumber, trigger: 'blur'}]">
                  <el-input v-model="firstForm.agreementPaymentItem.prepaymentRatio" placeholder="请输入约定预付款比例" clearable>
                    <template #append>%</template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="约定预付款金额(元)：" prop="agreementPaymentItem.prepaymentAmount"
                  :rules="[{validator: validateNumber, trigger: 'blur'}]">
                  <el-input v-model="firstForm.agreementPaymentItem.prepaymentAmount" placeholder="请输入约定预付款金额"
                    clearable />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="预付款扣回条件：" prop="agreementPaymentItem.prepaymentDeductionConditions">
                  <el-input v-model="firstForm.agreementPaymentItem.prepaymentDeductionConditions"
                    placeholder="请输入预付款扣回条件" clearable />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="10">
              <el-col :span="8">
                <el-form-item label="预付款全部扣回截止点：" prop="agreementPaymentItem.prepaymentDeductionDeadline"
                  :rules="[{validator: validateNumber, trigger: 'blur'}]">
                  <el-input v-model="firstForm.agreementPaymentItem.prepaymentDeductionDeadline"
                    placeholder="请输入预付款全部扣回截止点" clearable>
                    <template #append>%</template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="允许合同外结算占合同比例：" prop="agreementPaymentItem.outOfSettlementRatio"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入允许合同外结算占合同比例' }, {validator: validateNumber, trigger: 'blur'}]">
                  <el-input v-model="firstForm.agreementPaymentItem.outOfSettlementRatio" placeholder="请输入允许合同外结算占合同比例"
                    clearable>
                    <template #append>%</template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="8" v-if="[3].includes(contractType)">
                <el-form-item label="停滞台班结算比例：" prop="agreementPaymentItem.stagnationRatio"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入停滞台班结算比例' }, {validator: validateNumber, trigger: 'blur'}]">
                  <el-input v-model="firstForm.agreementPaymentItem.stagnationRatio" placeholder="请输入停滞台班结算比例"
                    clearable>
                    <template #append>%</template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="8" v-if="firstForm.agreement.isRelatedMySteelView == 'Y'">
                <el-form-item label="是否关联我的钢铁网价格：" prop="agreementPaymentItem.isRelatedMySteel"
                  :rules="[{ required: true, trigger: 'change', message: '请选择是否关联我的钢铁网价格' }]">
                  <el-select style="width: 100%" v-model="firstForm.agreementPaymentItem.isRelatedMySteel"
                    placeholder="请选择支付周期">
                    <el-option v-for="dict in dict.type.sys_yes_no" :key="dict.value" :label="dict.label"
                      :value="dict.value === 'Y' ? 1 : 0">
                    </el-option>
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="10">
              <el-col :span="8" v-if="firstForm.agreement.isRelatedMySteelView == 'Y' && firstForm.agreementPaymentItem.isRelatedMySteel == 1">
                <el-form-item label="我的钢铁网价格浮动值：" prop="agreementPaymentItem.mySteelPriceFluctuation"
                  :rules="[{ required: true, trigger: 'blur', message: '请输入我的钢铁网价格浮动值' }, {validator: validateFloat, trigger: 'blur'}]">
                  <el-input v-model="firstForm.agreementPaymentItem.mySteelPriceFluctuation" placeholder="请输入我的钢铁网价格浮动值"
                    clearable />
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="合同签订金额(含税)：" prop="agreementPaymentItem.procurementSchemeName">
                  {{ firstForm.agreementPaymentItem.totalAmountIncTaxText }}
                </el-form-item>
              </el-col>
              <el-col :span="8">
                <el-form-item label="合同签订金额(不含税)：" prop="agreementPaymentItem.procurementSchemeName">
                  {{ firstForm.agreementPaymentItem.totalAmountExcTaxText }}
                </el-form-item>
              </el-col>
            </el-row>
          </div>

          <!-- 合同签约方信息 -->
          <commonTitle>
            合同签约方信息
            <template #right>
<!--              <el-button type="success" icon="el-icon-plus" size="mini" @click="addRow('agreementPartyInfoLists')">新增</el-button>-->
            </template>
          </commonTitle>
          <div style="margin-bottom: 24px">
            <el-table :data="firstForm.agreementPartyInfoLists" style="width: 100%">
              <el-table-column label="签约方类型" width="130" align="center" prop="roleTypeText" />
              <el-table-column label="签约单位名称" align="center" prop="signerName">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementPartyInfoLists.' + scope.$index + '.signerName'"
                                :rules="[{ required: true, trigger: 'blur', message: '请输入签约单位名称' }]">
                    <el-input v-model="scope.row.signerName"/>
                  </el-form-item>
                </template>
              </el-table-column>
<!--       前端已经隐藏该字段，暂不清楚是否有意义，防止支出合同接口报错先赋值一个值       -->
<!--              <el-table-column label="签约单位编号" align="center" prop="signerCode">-->
<!--                <template slot-scope="scope">-->
<!--                  <el-form-item label-width="0" :prop="'agreementPartyInfoLists.' + scope.$index + '.signerCode'"-->
<!--                                :rules="[{ required: true, trigger: 'blur', message: '请输入签约单位编号' }]">-->
<!--                    <el-input v-model="scope.row.signerCode" clearable />-->
<!--                  </el-form-item>-->
<!--                </template>-->
<!--              </el-table-column>-->
              <el-table-column label="银行账号名称" align="center" prop="signerBankAccount">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementPartyInfoLists.' + scope.$index + '.signerBankAccount'"
                                :rules="[{ required: true, trigger: 'blur', message: '请输入签约单位银行账号' }]">
                    <el-input v-model="scope.row.signerBankAccount" placeholder="点击选择银行账号" @focus="getBankListVisible(scope)" :class="scope.row.roleType == 1 ? 'cursor_pointer':''"/>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="签约单位银行账户名称" align="center" prop="signerBankName">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementPartyInfoLists.' + scope.$index + '.signerBankName'"
                                :rules="[{ required: true, trigger: 'blur', message: '请输入签约单位银行账户名称' }]">
                    <el-input v-model="scope.row.signerBankName" placeholder="点击选择银行账户名称" @focus="getBankListVisible(scope)" :class="scope.row.roleType == 1 ? 'cursor_pointer':''"/>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="签约单位开户支行" align="center" prop="signerBankOpen">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementPartyInfoLists.' + scope.$index + '.signerBankOpen'"
                                :rules="[{ required: true, trigger: 'blur', message: '请输入签约单位开户支行' }]">
                    <el-input v-model="scope.row.signerBankOpen" placeholder="点击选择银行开户支行" @focus="getBankListVisible(scope)" :class="scope.row.roleType == 1 ? 'cursor_pointer':''"/>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="签约单位所占比例(%)" width="180" align="center" prop="signerRate">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementPartyInfoLists.' + scope.$index + '.signerRate'"
                                :rules="[{ required: true, trigger: 'blur', message: '请输入签约单位所占比例' }, {validator: validateNumber, trigger: 'blur'}]">
                    <el-input v-model="scope.row.signerRate" clearable />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="签约单位纳税人识别号" align="center" prop="signerTaxpayerNumber">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementPartyInfoLists.' + scope.$index + '.signerTaxpayerNumber'"
                                :rules="[{ required: true, trigger: 'blur', message: '请输入签约单位纳税人识别号' }]">
                    <el-input v-model="scope.row.signerTaxpayerNumber" clearable />
                  </el-form-item>
                </template>
              </el-table-column>
            </el-table>
          </div>


          <!-- 结算与付款节点信息 -->
          <commonTitle>
            结算与付款节点信息
            <template #right>
              <el-button type="success" icon="el-icon-plus" size="mini" @click="addRow('agreementPaymentLists')">新增</el-button>
            </template>
          </commonTitle>
          <div style="margin-bottom: 24px">
            <el-table :data="firstForm.agreementPaymentLists" style="width: 100%">
              <el-table-column prop="settlementStage" label="结算阶段">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementPaymentLists.' + scope.$index + '.settlementStage'"
                                :rules="[{ required: true, trigger: 'change', message: '请选择结算阶段' }]">
                    <el-select style="width: 100%" v-model="scope.row.settlementStage" @change="value => changeSettlementStage(scope, value)">
                      <el-option v-for="dict in dictObj.settlement_stage" :key="dict.value" :label="dict.label"
                                 :value="dict.value">
                      </el-option>
                    </el-select>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="paymentName" label="付款条件/结算与付款节点">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementPaymentLists.' + scope.$index + '.paymentName'"
                    :rules="[{ required: true, trigger: 'blur', message: '请输入结算与付款节点' }]">
                    <el-tooltip class="item" effect="dark" :content="scope.row.paymentName" placement="top" :disabled="!scope.row.paymentName">
                    <el-input v-model="scope.row.paymentName" type="textarea" :autosize="{maxRows: 2}" clearable />
                    </el-tooltip>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="paymentBasis" label="付款基数（元）">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementPaymentLists.' + scope.$index + '.paymentBasis'"
                    :rules="[{ required: true, trigger: 'blur', message: '请填写付款基数' }, {validator: validateFigure, trigger: 'blur'}]">
                    <!-- <el-select style="width: 100%" v-model="scope.row.paymentBasis" @change="value => changePaymentBasis(scope, value)">
                      <el-option v-for="dict in dictObj.payment_basis" :key="dict.value" :label="dict.label"
                        :value="dict.value">
                      </el-option>
                    </el-select> -->
                    <el-input v-model="scope.row.paymentBasis" clearable>
                    </el-input>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="paymentRatio" label="约定付款比例(%)">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementPaymentLists.' + scope.$index + '.paymentRatio'"
                    :rules="[{ required: true, trigger: 'blur', message: '请输入约定付款比例' }, {validator: validateNumber, trigger: 'blur'}]">
                    <el-input v-model="scope.row.paymentRatio" @blur="computedpaymentSAmount(scope)" clearable>
                    </el-input>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="paymentAmount" label="约定付款金额">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementPaymentLists.' + scope.$index + '.paymentAmount'"
                    :rules="
                      scope.row.paymentBasis === '1'?
                      (scope.row.paymentAmount ? [{validator: validateNumber, trigger: 'blur'}] : [])
                      :
                      [{ required: true, trigger: 'blur', message: '请输入约定付款金额' }, {validator: validateNumber, trigger: 'blur'}]
                    ">
                    <el-input v-model="scope.row.paymentAmount" :disabled="scope.row.paymentBasis === '2'" clearable />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="paymentRemark" label="付款说明">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementPaymentLists.' + scope.$index + '.paymentRemark'">
                    <el-tooltip class="item" effect="dark" :content="scope.row.paymentRemark" placement="top" :disabled="!scope.row.paymentRemark">
                    <el-input v-model="scope.row.paymentRemark" clearable type="textarea" :autosize="{maxRows: 2}"/>
                    </el-tooltip>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="currentPaymentPoint" label="当前付款节点">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementPaymentLists.' + scope.$index + '.currentPaymentPoint'"
                    :rules="[{ required: true, trigger: 'change', message: '请选择当前付款节点' }]">
                    <el-select style="width: 100%" v-model="scope.row.currentPaymentPoint">
                      <el-option v-for="dict in dict.type.sys_yes_no" :key="dict.value" :label="dict.label"
                        :value="dict.value === 'Y' ? 1 : 0">
                      </el-option>
                    </el-select>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="操作" align="center" width="80">
                <template slot-scope="scope">
                  <el-button size="mini" type="text"
                    @click="handleDelete('agreementPaymentLists', scope.$index)" >删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 合同清单 -->
          <commonTitle>合同清单</commonTitle>
          <!-- 物资采购类 -->
          <div style="margin-bottom: 24px" v-if="contractType == 1">
            <el-table v-if="firstForm.agreement.agreementNameYl" :data="firstForm.agreementMaterialsLists" style="width: 100%">
              <el-table-column prop="materialsCode" label="物资编码" width="150" show-overflow-tooltip/>
              <el-table-column prop="materialsName" label="物资名称" width="150" show-overflow-tooltip/>
<!--              <el-table-column prop="subjectMatterName" label="交易标的物" width="150" show-overflow-tooltip/>-->
<!--              <el-table-column prop="specification" label="规格型号" width="150" show-overflow-tooltip/>-->
              <el-table-column label="成本子目名称(导入)" min-width="150" prop="materialsNameImport" show-overflow-tooltip/>
              <el-table-column label="特征值特征项" min-width="150" prop="specification"  show-overflow-tooltip/>
              <el-table-column label="计量规则" min-width="150" align="center" prop="measurementRules"  show-overflow-tooltip/>
              <el-table-column label="工作内容" align="center" prop="workContent"  show-overflow-tooltip/>
              <el-table-column prop="unitMeasurement" label="计量单位" width="100"/>
              <el-table-column prop="brand" label="品牌" align="center" width="180" show-overflow-tooltip>
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementMaterialsLists.' + scope.$index + '.brand'">
                    <el-input v-model="scope.row.brand" type="textarea" :autosize="{maxRows: 2}" clearable />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="costAccount" label="成本科目" width="150" show-overflow-tooltip/>
              <el-table-column prop="paymentTypeText" label="价款类型" align="center" width="120"/>
              <el-table-column prop="countText" label="投标总量" width="120" align="right"/>
              <el-table-column header-align="center" align="center" label="合同价">
                <el-table-column prop="signTaxRate" label="签定税率(%)" align="right" width="150"/>
                <el-table-column prop="signCountText" label="签订量" width="120" align="right"/>
                <el-table-column prop="signUnitPriceInclTax" label="签订含税单价(元)"  width="120" align="right"/>
                <el-table-column prop="signUnitPriceExclTax" label="签订不含税单价(元)" width="120" align="right"/>
                <el-table-column prop="taxPriceText" label="签订含税总价(元)"  width="120" align="right"/>
                <el-table-column prop="notTaxPriceText" label="签订不含税总价(元)"  width="120" align="right"/>
                <el-table-column prop="taxAmountText" label="签订税额" width="180" align="right"/>
              </el-table-column>
              <el-table-column header-align="center" align="center" label="中标价">
                <el-table-column prop="notTaxUnitPrice" label="不含税单价(元)" width="160" align="right"/>
                <el-table-column prop="taxUnitPrice" label="含税单价(元)" width="150" align="right"/>
                <el-table-column prop="taxRateText" label="税率(%)" width="100" align="right"/>
                <el-table-column prop="taxAmountText" label="税额" width="150" align="right"/>
              </el-table-column>
              <el-table-column prop="remark" align="center" width="180" label="备注">
                <template slot-scope="scope">
                  <el-form-item :prop="'agreementMaterialsLists.' + scope.$index + '.remark'" label-width="0">
                    <el-input v-model="scope.row.remark" type="textarea" :autosize="{maxRows: 2}" clearable />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="skuId" align="center" width="180" label="易料商品编码"/>
                <el-table-column prop="goodsName" align="center" width="180" label="易料商品名称"/>
                  <el-table-column prop="offerBrand" align="center" width="180" label="易料品牌"/>
                  <el-table-column  prop="offerPrice" align="center" width="180" label="易料初始报价"/>
                  <el-table-column width="1"/>

            </el-table>
            <el-table v-else  :data="firstForm.agreementMaterialsLists" style="width: 100%">
              <el-table-column prop="materialsCode" label="物资编码" width="150" show-overflow-tooltip/>
              <el-table-column prop="materialsName" label="物资名称" width="150" show-overflow-tooltip/>
<!--              <el-table-column prop="subjectMatterName" label="交易标的物/" width="150" show-overflow-tooltip/>-->
<!--              <el-table-column prop="specification" label="规格型号" width="150" show-overflow-tooltip/>-->
              <el-table-column label="成本子目名称(导入)" min-width="150" prop="materialsNameImport" show-overflow-tooltip/>
              <el-table-column label="特征值特征项" min-width="150" prop="specification"  show-overflow-tooltip/>
              <el-table-column label="计量规则" min-width="150" align="center" prop="measurementRules"  show-overflow-tooltip/>
              <el-table-column label="工作内容" align="center" prop="workContent" show-overflow-tooltip />
              <el-table-column prop="unitMeasurement" label="计量单位" width="100"/>
              <el-table-column prop="brand" label="品牌" align="center" width="180" show-overflow-tooltip>
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementMaterialsLists.' + scope.$index + '.brand'">
                    <el-input v-model="scope.row.brand" type="textarea" :autosize="{maxRows: 2}" clearable />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="costAccount" label="成本科目" width="150" show-overflow-tooltip/>
              <el-table-column prop="paymentTypeText" label="价款类型" align="center" width="120"/>
              <el-table-column prop="countText" label="投标总量" width="120" align="right"/>
              <el-table-column prop="surplusCountText" label="剩余可用量" align="right" width="150"/>
              <el-table-column header-align="center" align="center" label="合同价">
                <el-table-column prop="signCountText" label="签订量" width="120" align="right"/>
                <el-table-column prop="signUnitPriceExclTaxText" label="签订不含税单价(元)" width="180" align="right"/>
                <el-table-column prop="signUnitPriceInclTaxText" label="签订含税单价(元)" width="160" align="right"/>
                <el-table-column prop="signAmountExclTaxText" label="不含税总价(元)" width="150" align="right"/>
                <el-table-column prop="signAmountInclTaxText" label="含税总价(元)" width="150" align="right"/>
              </el-table-column>

              <el-table-column header-align="center" align="center" label="中标价">
                <!-- 基价(元) -->
                <el-table-column prop="basePriceText" label="基价(元)" width="120" align="right">
                  <template #default="{ row }">
                    {{ [2,3,4,5,6,7].includes(priceType) ? row.basePriceText : '/' }}
                  </template>
                </el-table-column>

                <!-- 浮动价(元) -->
                <el-table-column prop="floatingPriceText" label="浮动价(元)" width="120" align="right">
                  <template #default="{ row }">
                    {{ [2,3,4,5,6,7].includes(priceType) && row.priceType === 2 ? row.floatingPriceText : '/' }}
                  </template>
                </el-table-column>

                <!-- 装卸费(元) -->
                <el-table-column prop="floatingRateText" label="浮动率(%)" width="120" align="right">
                  <template #default="{ row }">
                    {{ [2,3,4,5,6,7].includes(priceType) && row.priceType === 4 ? row.floatingRateText : '/' }}
                  </template>
                </el-table-column>

                <!-- 单价(不含税) -->
                <el-table-column prop="notTaxUnitPriceText" label="单价(不含税)" width="150" align="right">
                  <template #default="{ row }">
                    {{ priceType !== 2 && row.priceType !== 2 && row.priceType !== 3 ? row.notTaxUnitPriceText : '/' }}
                  </template>
                </el-table-column>

                <!-- 单价(含税) -->
                <el-table-column prop="taxUnitPriceText" label="单价(含税)" width="150" align="right">
                  <template #default="{ row }">
                    {{  priceType !== 2 && row.priceType !== 2 && row.priceType !== 3 ? row.taxUnitPriceText : '/' }}
                  </template>
                </el-table-column>
                  <!-- 通用列 -->
                  <el-table-column prop="notTaxPriceText" label="金额(不含税)" width="150" align="right" />
                  <el-table-column prop="taxPriceText" label="金额(含税)" width="150" align="right" />
                  <el-table-column prop="taxRateText" label="税率(%)" width="100" align="right" />
                  <el-table-column prop="taxAmountText" label="税额" width="150" align="right" />
              </el-table-column>
              <el-table-column prop="remark" align="center" width="180" label="备注">
                <template slot-scope="scope">
                  <el-form-item :prop="'agreementMaterialsLists.' + scope.$index + '.remark'" label-width="0">
                    <el-input v-model="scope.row.remark" type="textarea" :autosize="{maxRows: 2}" clearable />
                  </el-form-item>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <!-- 物资租赁类 / 机械租赁类 -->
          <div style="margin-bottom: 24px" v-if="contractType == 2 || contractType == 3">
            <el-table :data="firstForm.agreementMaterialsLists" style="width: 100%">
              <el-table-column prop="materialsCode" label="物资编码" width="150" show-overflow-tooltip/>
              <el-table-column prop="materialsName" label="物资名称" width="150" show-overflow-tooltip/>
<!--              <el-table-column prop="subjectMatterName" label="交易标的物" width="150" show-overflow-tooltip/>-->
<!--              <el-table-column prop="specification" label="规格型号" width="150" show-overflow-tooltip/>-->
              <el-table-column label="成本子目名称(导入)" min-width="150" prop="materialsNameImport" show-overflow-tooltip/>
              <el-table-column label="特征值特征项" min-width="150" prop="specification" show-overflow-tooltip/>
              <el-table-column label="计量规则" min-width="150" align="center" prop="measurementRules"  show-overflow-tooltip/>
              <el-table-column label="工作内容" align="center" prop="workContent"  show-overflow-tooltip/>
              <el-table-column prop="unitMeasurement" label="计量单位" />
              <el-table-column prop="brand" align="center" label="品牌" width="180">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementMaterialsLists.' + scope.$index + '.brand'">
                    <el-input v-model="scope.row.brand" clearable />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="rentMode" align="center" label="租赁方式" width="120">
                <template slot-scope="scope">
                  <el-form-item label-width="0">
                    {{ scope.row.rentModeText }}
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="rentalUnit" align="center" label="计租单位" width="120">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementMaterialsLists.' + scope.$index + '.rentalUnit'"
                                :rules="[{ required: true, trigger: 'change', message: '请选择计租单位' }]">
                    <template v-if="scope.row.rentMode == 1">日</template>
                    <template v-else-if="scope.row.rentMode == 2">月</template>
                    <el-select v-else-if="scope.row.rentMode == 3" style="width: 100%" v-model="scope.row.rentalUnit" placeholder="请选择">
                      <el-option v-for="dict in dictObj.rentalUnit" :key="dict.value" :label="dict.label"
                        :value="dict.value">
                      </el-option>
                    </el-select>
                    <template v-else> - </template>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="costAccount" label="成本科目" width="150" show-overflow-tooltip/>
              <el-table-column prop="countText" align="right" label="工作量" />
              <el-table-column prop="rentTimeText" label="租赁时间" align="right"
                               width="120">
                <template slot-scope="scope">
                  <span v-if="scope.row.rentMode == 3">-</span>
                  <span v-else>{{ scope.row.rentTimeText }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="rentQuantityText" label="租赁数量" align="right" width="120">
                <template slot-scope="scope">
                  <span v-if="scope.row.rentMode == 3">-</span>
                  <span v-else>{{ scope.row.rentQuantityText }}</span>
                </template>
              </el-table-column>
              <el-table-column prop="surplusCountText" label="剩余可用量" align="right" width="150"/>
              <el-table-column header-align="center" align="center" label="合同价">
                <el-table-column prop="signCountText" label="签订量" width="120" align="right"/>
                <el-table-column prop="taxRateText" label="税率(%)" align="right" width="100"/>
                <el-table-column prop="signUnitPriceExclTaxText" label="签订不含税单价(元)" width="180" align="right"/>
                <el-table-column prop="signUnitPriceInclTaxText" label="签订含税单价(元)" width="160" align="right"/>
                <el-table-column prop="signAmountExclTaxText" label="不含税总价(元)" width="150" align="right"/>
                <el-table-column prop="signAmountInclTaxText" label="含税总价(元)" width="150" align="right"/>
              </el-table-column>
              <el-table-column header-align="center" align="center" label="中标价">
                <el-table-column prop="notTaxUnitPriceText" label="单价(不含税)" align="right" width="120"/>
                <el-table-column prop="taxUnitPriceText" label="单价(含税)" align="right" width="120"/>
                <el-table-column prop="notTaxPriceText" label="金额(不含税)" align="right" width="120"/>
                <el-table-column prop="taxPriceText" label="金额(含税)" align="right" width="120"/>
                <el-table-column prop="taxAmountText" label="税额" align="right" width="120"/>
              </el-table-column>
              <el-table-column prop="remark" align="center" width="180" label="备注">
                <template slot-scope="scope">
                  <el-form-item :prop="'agreementMaterialsLists.' + scope.$index + '.remark'" label-width="0">
                    <el-input v-model="scope.row.remark" clearable />
                  </el-form-item>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <!-- 专业分包类 / 劳务分包类 -->
          <div style="margin-bottom: 24px" v-if="contractType == 4 || contractType == 5">
            <el-table :data="firstForm.agreementMaterialsLists" style="width: 100%">
              <el-table-column prop="materialsCode" label="清单编码" width="150" show-overflow-tooltip/>
              <el-table-column prop="materialsName" label="清单名称" width="150" show-overflow-tooltip/>
<!--              <el-table-column prop="subjectMatterName" label="交易标的物" width="150" show-overflow-tooltip/>-->
<!--              <el-table-column prop="specification" label="规格型号" width="150" show-overflow-tooltip/>-->
              <el-table-column prop="specification" label="特征值及特征项" width="150" show-overflow-tooltip/>
              <el-table-column prop="unitMeasurement" label="计量单位" width="100" show-overflow-tooltip/>
              <el-table-column prop="measurementRules" label="计量规则" width="150" show-overflow-tooltip/>
              <el-table-column prop="workContent" label="基本工作内容" width="150" show-overflow-tooltip/>
              <el-table-column prop="costAccount" label="成本科目" width="150" show-overflow-tooltip/>
              <el-table-column prop="countText" label="投标总量" width="120" align="right"/>
              <el-table-column prop="surplusCountText" label="剩余可用量" align="right" width="150"/>
              <el-table-column header-align="center" align="center" label="合同价">
                <el-table-column prop="signCountText" label="签订量" width="120" align="right"/>
                <el-table-column prop="taxRateText" label="税率(%)" align="center" />
                <el-table-column prop="signUnitPriceExclTaxText" label="签订不含税单价(元)" width="180" align="right"/>
                <el-table-column prop="signUnitPriceInclTaxText" label="签订含税单价(元)" width="160" align="right"/>
                <el-table-column prop="signAmountExclTaxText" label="不含税总价(元)" width="150" align="right"/>
                <el-table-column prop="signAmountInclTaxText" label="含税总价(元)" width="150" align="right"/>
              </el-table-column>
              <el-table-column header-align="center" align="center" label="中标价">
                <el-table-column prop="notTaxUnitPriceText" label="单价(不含税)" width="120" align="right"/>
                <el-table-column prop="taxUnitPriceText" label="单价(含税)" width="120" align="right"/>
                <el-table-column prop="notTaxPriceText" label="金额(不含税)" width="120" align="right"/>
                <el-table-column prop="taxPriceText" label="金额(含税)" width="120" align="right"/>
                <el-table-column prop="taxAmountText" label="税额" width="120" align="right"/>
              </el-table-column>
              <el-table-column label="合计(含税)" align="right" prop="totalPriceText" min-width="150"/>
              <el-table-column prop="remark" align="center" width="180" label="备注">
                <template slot-scope="scope">
                  <el-form-item :prop="'agreementMaterialsLists.' + scope.$index + '.remark'" label-width="0">
                    <el-input v-model="scope.row.remark" clearable />
                  </el-form-item>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <!-- 其它类 -->
          <div style="margin-bottom: 24px" v-if="contractType == 6">
            <el-table :data="firstForm.agreementMaterialsLists" style="width: 100%">
              <el-table-column prop="materialsCode" label="清单编码" width="150" show-overflow-tooltip/>
              <el-table-column prop="materialsName" label="清单名称" width="150" show-overflow-tooltip/>
<!--              <el-table-column prop="subjectMatterName" label="交易标的物" width="150" show-overflow-tooltip/>-->
<!--              <el-table-column prop="specification" label="规格型号" width="150" show-overflow-tooltip/>-->
              <el-table-column prop="specification" label="特征值及特征项" width="150"/>
              <el-table-column prop="unitMeasurement" label="计量单位" width="100"/>
              <el-table-column prop="costAccount" label="成本科目" width="150" show-overflow-tooltip/>
              <el-table-column prop="countText" label="投标总量" width="120" align="right"/>
              <el-table-column prop="surplusCountText" label="剩余可用量" align="right" width="150"/>
              <el-table-column header-align="center" align="center" label="合同价">
                <el-table-column prop="signCountText" label="签订量" width="120" align="right"/>
                <el-table-column prop="taxRateText" label="税率(%)" align="center" />
                <el-table-column prop="signUnitPriceExclTaxText" label="签订不含税单价(元)" width="180" align="right"/>
                <el-table-column prop="signUnitPriceInclTaxText" label="签订含税单价(元)" width="160" align="right"/>
                <el-table-column prop="signAmountExclTaxText" label="不含税总价(元)" width="150" align="right"/>
                <el-table-column prop="signAmountInclTaxText" label="含税总价(元)" width="150" align="right"/>
              </el-table-column>
              <el-table-column header-align="center" align="center" label="中标价">
                <el-table-column prop="notTaxUnitPrice" label="单价(不含税)" width="120" align="right"/>
                <el-table-column prop="taxUnitPrice" label="单价(含税)" width="120" align="right"/>
                <el-table-column prop="notTaxPrice" label="金额(不含税)" width="120" align="right"/>
                <el-table-column prop="taxPrice" label="金额(含税)" width="120" align="right"/>
                <el-table-column prop="taxAmountText" label="税额" width="120" align="right"/>
              </el-table-column>
              <el-table-column prop="remark" align="center" width="180" label="备注">
                <template slot-scope="scope">
                  <el-form-item :prop="'agreementMaterialsLists.' + scope.$index + '.remark'" label-width="0">
                    <el-input v-model="scope.row.remark" clearable />
                  </el-form-item>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 合同外工程签证清单 -->
          <div style="margin-bottom: 24px;" v-if="[4, 5].includes(contractType)">
            <commonTitle>合同外工程签证清单</commonTitle>
            <div class="engineering_visa">
              <div class="item_title">
                <span>计日工</span>
                <el-button type="success" icon="el-icon-plus" size="mini" @click="visaAdd(1)">新增</el-button>
              </div>
              <el-table :data="firstForm.agreementDailyWageList" style="width: 100%">
                <el-table-column prop="jobTitleCode" align="center" label="工种名称">
                  <template slot-scope="scope">
                    <el-form-item label-width="0" :prop="'agreementDailyWageList.' + scope.$index + '.jobTitleCode'"
                      :rules="[{ required: true, trigger: 'change', message: '请选择工种名称' }]">
                    <el-select style="width: 100%" v-model="scope.row.jobTitleCode">
                      <el-option v-for="dict in dictObj.jobTitleCode" :key="dict.value" :label="dict.label"
                        :value="dict.value">
                      </el-option>
                    </el-select>
                    </el-form-item>
                  </template>
                </el-table-column>
                <el-table-column prop="unitMeasurement" align="center" label="计量单位">
                  <template slot-scope="scope">
                    <!-- <el-form-item label-width="0" :prop="'agreementDailyWageList.' + scope.$index + '.unitMeasurement'"
                      :rules="[{ required: true, trigger: 'change', message: '请输入计量单位' }]">
                      <el-input v-model="scope.row.unitMeasurement" placeholder="请输入" clearable />
                    </el-form-item> -->
                    工日
                  </template>
                </el-table-column>
                <el-table-column prop="taxRate" align="center" label="税率(%)">
                  <template slot-scope="scope">
                    <el-form-item label-width="0" :prop="'agreementDailyWageList.' + scope.$index + '.taxRate'"
                      :rules="[{ required: true, trigger: 'change', message: '请输入税率' },{ pattern: /^(?:[1-9]\d*|0)(\.\d+)?$/, trigger: 'blur', message: '请输入正确的值' },{ pattern: /^\d+(\.\d{0,4})?$/, trigger: 'blur', message: '请输入小于4位的小数' }]">
                      <el-input v-model="scope.row.taxRate" placeholder="请输入" clearable v-thousandth>
                        <template slot="append">%</template>
                      </el-input>
                    </el-form-item>
                  </template>
                </el-table-column>
                <el-table-column prop="unitPriceIncTax" align="right" label="单价(含税)">
                  <template slot-scope="scope">
                    <el-form-item label-width="0" :prop="'agreementDailyWageList.' + scope.$index + '.unitPriceIncTax'"
                      :rules="[{ required: true, trigger: 'change', message: '请输入单价(含税)' },{ pattern: /^(?:[1-9]\d*|0)(\.\d+)?$/, trigger: 'blur', message: '请输入正确的值' },{ pattern: /^\d+(\.\d{0,4})?$/, trigger: 'blur', message: '请输入小于4位的小数' }]">
                      <el-input v-model="scope.row.unitPriceIncTax" placeholder="请输入" clearable v-thousandth/>
                    </el-form-item>
                  </template>
                </el-table-column>
                <el-table-column prop="unitPriceExcTax" align="right" label="单价(不含税)">
                  <template slot-scope="scope">
                    <!-- <el-form-item label-width="0" :prop="'agreementDailyWageList.' + scope.$index + '.unitPriceExcTax'"
                      :rules="[{ required: true, trigger: 'change', message: '请输入单价(不含税)' }]">
                      <el-input v-model="scope.row.unitPriceExcTax" placeholder="请输入" clearable />
                    </el-form-item> -->
                    {{ unitPriceExclTaxComputed(scope.row.unitPriceIncTax, scope.row.taxRate) }}
                  </template>
                </el-table-column>
                <el-table-column prop="remark" align="center" label="备注">
                  <template slot-scope="scope">
                    <el-form-item label-width="0">
                      <el-input v-model="scope.row.remark" placeholder="请输入" clearable />
                    </el-form-item>
                  </template>
                </el-table-column>
                <el-table-column label="操作" align="center" width="80">
                  <template slot-scope="scope">
                    <el-button size="mini" type="text"  @click="visaDel(scope.$index, 1)"
                      >删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
            <div class="engineering_visa">
              <div class="item_title">
                <span>机械台班</span>
                <el-button type="success" icon="el-icon-plus" size="mini" @click="focusHandle({type:'machine',title:'请选择机械台班设备',classifyType:1})">新增</el-button>
              </div>
              <el-table :data="firstForm.agreementMachineShifts" style="width: 100%">
                <el-table-column prop="equipmentName" label="设备名称" show-overflow-tooltip/>
<!--                <el-table-column prop="specification" label="规格型号" show-overflow-tooltip/>-->
                <el-table-column label="成本子目名称(导入)" min-width="150" prop="materialsNameImport" show-overflow-tooltip/>
                <el-table-column label="特征值特征项" min-width="150" prop="specification"  show-overflow-tooltip/>
                <el-table-column label="计量规则" min-width="150" align="center" prop="measurementRules" show-overflow-tooltip />
                <el-table-column label="工作内容" align="center" prop="workContent"  show-overflow-tooltip/>
                <el-table-column prop="unitMeasurement" label="计量单位" show-overflow-tooltip/>
                <el-table-column prop="taxRate" align="right" label="税率(%)">
                  <template slot-scope="scope">
                    <el-form-item label-width="0" :prop="'agreementMachineShifts.' + scope.$index + '.taxRate'"
                      :rules="[{ required: true, trigger: 'change', message: '请输入税率' },{ pattern: /^(?:[1-9]\d*|0)(\.\d+)?$/, trigger: 'blur', message: '请输入正确的值' },{ pattern: /^\d+(\.\d{0,4})?$/, trigger: 'blur', message: '请输入小于4位的小数' }]">
                      <el-input v-model="scope.row.taxRate" placeholder="请输入" clearable v-thousandth>
                        <template slot="append">%</template>
                      </el-input>
                    </el-form-item>
                  </template>
                </el-table-column>
                <el-table-column prop="unitPriceIncTax" align="right" label="单价(含税)">
                  <template slot-scope="scope">
                    <el-form-item label-width="0" :prop="'agreementMachineShifts.' + scope.$index + '.unitPriceIncTax'"
                      :rules="[{ required: true, trigger: 'change', message: '请输入单价(含税)' },{ pattern: /^(?:[1-9]\d*|0)(\.\d+)?$/, trigger: 'blur', message: '请输入正确的值' },{ pattern: /^\d+(\.\d{0,4})?$/, trigger: 'blur', message: '请输入小于4位的小数' }]">
                      <el-input v-model="scope.row.unitPriceIncTax" placeholder="请输入" clearable v-thousandth/>
                    </el-form-item>
                  </template>
                </el-table-column>
                <el-table-column prop="unitPriceExcTax" align="right" label="单价(不含税)">
                  <template slot-scope="scope">
                    <!-- <el-form-item label-width="0" :prop="'agreementMachineShifts.' + scope.$index + '.unitPriceExcTax'"
                      :rules="[{ required: true, trigger: 'change', message: '请输入单价(不含税)' }]">
                      <el-input v-model="scope.row.unitPriceExcTax" placeholder="请输入" clearable />
                    </el-form-item> -->
                    {{ unitPriceExclTaxComputed(scope.row.unitPriceIncTax, scope.row.taxRate) }}
                  </template>
                </el-table-column>
                <el-table-column prop="remark" label="备注">
                  <template slot-scope="scope">
                    <el-form-item label-width="0">
                      <el-input v-model="scope.row.remark" placeholder="请输入" clearable />
                    </el-form-item>
                  </template>
                </el-table-column>
                <el-table-column label="操作" align="center" width="80">
                  <template slot-scope="scope">
                    <el-button size="mini" type="text"
                      @click="visaDel(scope.$index, 2)">删除
                    </el-button>
                  </template>
                </el-table-column>
              </el-table>
            </div>
          </div>

          <!-- 甲供设备清单 / 甲供材料清单 -->
          <div style="margin-bottom: 24px;" v-if="contractType == 4">
            <commonTitle>
              甲供设备清单
              <template #right>
                <el-button type="success" icon="el-icon-plus" size="mini" @click="focusHandle({type:'equipment',title:'请选择甲供设备清单',classifyType:1})">新增</el-button>
              </template>
            </commonTitle>
            <el-table :data="firstForm.agreementEquipmentSupplies" style="width: 100%">
              <el-table-column prop="equipmentName" label="设备名称" show-overflow-tooltip/>
<!--              <el-table-column prop="specification" label="规格型号" show-overflow-tooltip/>-->
              <el-table-column label="成本子目名称(导入)" min-width="150" prop="materialsNameImport" show-overflow-tooltip/>
              <el-table-column label="特征值特征项" min-width="150" prop="specification" show-overflow-tooltip/>
              <el-table-column label="计量规则" min-width="150" align="center" prop="measurementRules" show-overflow-tooltip />
              <el-table-column label="工作内容" align="center" prop="workContent"  show-overflow-tooltip/>
              <el-table-column prop="unitMeasurement" label="计量单位" show-overflow-tooltip/>
              <el-table-column prop="estimatedCount" align="center" label="预估数量">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementEquipmentSupplies.' + scope.$index + '.estimatedCount'"
                    :rules="[{ required: true, trigger: 'change', message: '请输入预估数量' },{ pattern: /^(?:[1-9]\d*|0)(\.\d+)?$/, trigger: 'blur', message: '请输入正确的值' },{ pattern: /^\d+(\.\d{0,4})?$/, trigger: 'blur', message: '请输入小于4位的小数' }]">
                    <el-input v-model="scope.row.estimatedCount" placeholder="请输入" clearable v-thousandth/>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="estimatedTaxRate" align="right" label="预估税率(%)">
                <template slot-scope="scope">
                  <el-form-item label-width="0"
                    :prop="'agreementEquipmentSupplies.' + scope.$index + '.estimatedTaxRate'"
                    :rules="[{ required: true, trigger: 'change', message: '请输入预估税率' },{ pattern: /^(?:[1-9]\d*|0)(\.\d+)?$/, trigger: 'blur', message: '请输入正确的值' },{ pattern: /^\d+(\.\d{0,4})?$/, trigger: 'blur', message: '请输入小于4位的小数' }]">
                    <el-input v-model="scope.row.estimatedTaxRate" placeholder="请输入" clearable v-thousandth>
                      <template slot="append">%</template>
                    </el-input>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="estimatedUnitPriceExcTax" align="right" label="预估单价(不含税)">
                <template slot-scope="scope">
                  {{ countComputed(scope.row.estimatedUnitPriceIncTax, scope.row.estimatedTaxRate, scope.row.estimatedCount, 'excludingTax') }}
                </template>
              </el-table-column>
              <el-table-column prop="estimatedUnitPriceIncTax" align="right" label="预估单价(含税)">
                <template slot-scope="scope">
                  <el-form-item label-width="0"
                    :prop="'agreementEquipmentSupplies.' + scope.$index + '.estimatedUnitPriceIncTax'"
                    :rules="[{ required: true, trigger: 'change', message: '请输入单价(含税)' },{ pattern: /^(?:[1-9]\d*|0)(\.\d+)?$/, trigger: 'blur', message: '请输入正确的值' },{ pattern: /^\d+(\.\d{0,4})?$/, trigger: 'blur', message: '请输入小于4位的小数' }]">
                    <el-input v-model="scope.row.estimatedUnitPriceIncTax" placeholder="请输入" clearable v-thousandth/>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="estimatedAmountExcTax" align="right" label="预估金额(不含税)">
                <template slot-scope="scope">
                  {{ countComputed(scope.row.estimatedUnitPriceIncTax, scope.row.estimatedTaxRate, scope.row.estimatedCount, 'excludingTaxTotal') }}
                </template>
              </el-table-column>
              <el-table-column prop="estimatedAmountIncTax" align="right" label="预估金额(含税)">
                <template slot-scope="scope">
                  {{ countComputed(scope.row.estimatedUnitPriceIncTax, scope.row.estimatedTaxRate, scope.row.estimatedCount, 'taxIncludedTotal') }}
                </template>
              </el-table-column>
              <el-table-column prop="estimatedTaxAmount" align="right" label="预估税额">
                <template slot-scope="scope">
                  {{ countComputed(scope.row.estimatedUnitPriceIncTax, scope.row.estimatedTaxRate, scope.row.estimatedCount) }}
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注">
                <template slot-scope="scope">
                  <el-form-item label-width="0">
                    <el-input v-model="scope.row.remark" placeholder="请输入" clearable />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="操作" align="center" width="80">
                <template slot-scope="scope">
                  <el-button size="mini" type="text"
                    @click="handleDelete('agreementEquipmentSupplies', scope.$index)">删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <div style="margin-bottom: 24px;" v-if="contractType == 4">
            <commonTitle>
              甲供材料清单
              <template #right>
                <el-button type="success" icon="el-icon-plus" size="mini" @click="focusHandle({type:'',title:'请选择物资', classifyType:2})">新增</el-button>
              </template>
            </commonTitle>
            <el-table :data="firstForm.agreementMaterialSupplies" style="width: 100%">
              <el-table-column prop="materialName" label="物资名称" show-overflow-tooltip/>
<!--              <el-table-column prop="specification" label="规格型号" show-overflow-tooltip/>-->
              <el-table-column label="成本子目名称(导入)" min-width="150" prop="materialsNameImport" show-overflow-tooltip/>
              <el-table-column label="特征值特征项" min-width="150" prop="specification" show-overflow-tooltip/>
              <el-table-column label="计量规则" min-width="150" align="center" prop="measurementRules"  show-overflow-tooltip/>
              <el-table-column label="工作内容" align="center" prop="workContent"  show-overflow-tooltip/>
              <el-table-column prop="unitMeasurement" label="计量单位" show-overflow-tooltip/>
              <el-table-column prop="estimatedCount" align="center" label="预估数量">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementMaterialSupplies.' + scope.$index + '.estimatedCount'"
                    :rules="[{ required: true, trigger: 'change', message: '请输入预估数量' },{ pattern: /^(?:[1-9]\d*|0)(\.\d+)?$/, trigger: 'blur', message: '请输入正确的值' },{ pattern: /^\d+(\.\d{0,4})?$/, trigger: 'blur', message: '请输入小于4位的小数' }]">
                    <el-input v-model="scope.row.estimatedCount" placeholder="请输入" clearable v-thousandth/>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="estimatedTaxRate" align="center" label="预估税率(%)">
                <template slot-scope="scope">
                  <el-form-item label-width="0"
                    :prop="'agreementMaterialSupplies.' + scope.$index + '.estimatedTaxRate'"
                    :rules="[{ required: true, trigger: 'change', message: '请输入预估税率' },{ pattern: /^(?:[1-9]\d*|0)(\.\d+)?$/, trigger: 'blur', message: '请输入正确的值' },{ pattern: /^\d+(\.\d{0,4})?$/, trigger: 'blur', message: '请输入小于4位的小数' }]">
                    <el-input v-model="scope.row.estimatedTaxRate" placeholder="请输入" clearable v-thousandth>
                      <template slot="append">%</template>
                    </el-input>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="estimatedUnitPriceExcTax" align="right" label="预估单价(不含税)">
                <template slot-scope="scope">
                  {{ countComputed(scope.row.estimatedUnitPriceIncTax, scope.row.estimatedTaxRate, scope.row.estimatedCount, 'excludingTax') }}
                </template>
              </el-table-column>
              <el-table-column prop="estimatedUnitPriceIncTax" align="right" label="预估单价(含税)">
                <template slot-scope="scope">
                  <el-form-item label-width="0"
                    :prop="'agreementMaterialSupplies.' + scope.$index + '.estimatedUnitPriceIncTax'"
                    :rules="[{ required: true, trigger: 'change', message: '请输入预估单价(含税)' },{ pattern: /^(?:[1-9]\d*|0)(\.\d+)?$/, trigger: 'blur', message: '请输入正确的值' },{ pattern: /^\d+(\.\d{0,4})?$/, trigger: 'blur', message: '请输入小于4位的小数' }]">
                    <el-input v-model="scope.row.estimatedUnitPriceIncTax" placeholder="请输入" clearable v-thousandth/>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="estimatedAmountExcTax" align="right" label="预估金额(不含税)">
                <template slot-scope="scope">
                  {{ countComputed(scope.row.estimatedUnitPriceIncTax, scope.row.estimatedTaxRate, scope.row.estimatedCount, 'excludingTaxTotal') }}
                </template>
              </el-table-column>
              <el-table-column prop="estimatedAmountIncTax" align="right" label="预估金额(含税)">
                <template slot-scope="scope">
                  {{ countComputed(scope.row.estimatedUnitPriceIncTax, scope.row.estimatedTaxRate, scope.row.estimatedCount, 'taxIncludedTotal') }}
                </template>
              </el-table-column>
              <el-table-column prop="estimatedTaxAmount" align="right" label="预估税额">
                <template slot-scope="scope">
                  {{ countComputed(scope.row.estimatedUnitPriceIncTax, scope.row.estimatedTaxRate, scope.row.estimatedCount) }}
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注">
                <template slot-scope="scope">
                  <el-form-item label-width="0">
                    <el-input v-model="scope.row.remark" placeholder="请输入" clearable />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="操作" align="center" width="80">
                <template slot-scope="scope">
                  <el-button size="mini" type="text"
                    @click="handleDelete('agreementMaterialSupplies', scope.$index)">删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 押金、保证金信息 -->
          <commonTitle>
            押金、保证金信息
            <template #right>
              <el-button type="success" icon="el-icon-plus" style="justify-self: flex-end" size="mini"
                @click="addRow('agreementDeposits')">新增</el-button>
            </template>
          </commonTitle>
          <div style="margin-bottom: 8px">
            <el-table :data="firstForm.agreementDeposits" style="width: 100%">
              <el-table-column prop="depositType" label="押金/保证金类型">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementDeposits.' + scope.$index + '.depositType'"
                    :rules="[{ required: true, trigger: 'change', message: '请选择押金/保证金类型' }]">
                    <el-select style="width: 100%" v-model="scope.row.depositType">
                      <el-option v-for="dict in dictObj.deposit_type" :key="dict.value" :label="dict.label"
                        :value="dict.value">
                      </el-option>
                    </el-select>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="depositWay" label="押金/保证金方式">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementDeposits.' + scope.$index + '.depositWay'"
                    :rules="[{ required: true, trigger: 'change', message: '请选择押金/保证金方式' }]">
                    <el-select style="width: 100%" v-model="scope.row.depositWay">
                      <el-option v-for="dict in dictObj.deposit_way" :key="dict.value" :label="dict.label"
                        :value="dict.value">
                      </el-option>
                    </el-select>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="depositBaseAmount" label="押金/保证金基数">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementDeposits.' + scope.$index + '.depositBaseAmount'"
                    :rules="[{ required: true, trigger: 'change', message: '请选择押金/保证金基数' }]">
                    <el-select style="width: 100%" v-model="scope.row.depositBaseAmount" @change="value => changeDepositBaseAmount(scope, value)">
                      <el-option v-for="dict in dictObj.deposit_base_amount" :key="dict.value" :label="dict.label"
                        :value="dict.value">
                      </el-option>
                    </el-select>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="paymentAmount" label="缴纳金额">
                <template slot-scope="scope">
                  <template v-if="scope.row.depositBaseAmount === '3'">
                    <el-form-item label-width="0" :prop="'agreementDeposits.' + scope.$index + '.paymentAmount'"
                      :rules="[{ required: true, trigger: 'blur', message: '请输入缴纳金额' }, {validator: validateNumber, trigger: 'blur'}]">
                      <el-input v-model="scope.row.paymentAmount" clearable @blur="computedDepositAmount(scope)"/>
                    </el-form-item>
                  </template>
                  <template v-else>-</template>
                </template>
              </el-table-column>
              <el-table-column prop="depositRatio" label="约定押金/保证金比例(%)">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementDeposits.' + scope.$index + '.depositRatio'"
                    :rules="[{ required: true, trigger: 'blur', message: '请输入约定押金/保证金比例' }, {validator: validateNumber, trigger: 'blur'}]">
                    <el-input v-model="scope.row.depositRatio" clearable @blur="computedDepositAmount(scope)"/>
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="depositAmount" label="约定押金/保证金金额">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementDeposits.' + scope.$index + '.depositAmount'"
                    :rules="
                    scope.row.depositBaseAmount === '2'?
                    (scope.row.depositAmount ? [{validator: validateNumber, trigger: 'blur'}] : [])
                    :
                    [{ required: true, trigger: 'blur', message: '请输入约定押金/保证金金额' }, {validator: validateNumber, trigger: 'blur'}]"
                    >
                    <el-input v-model="scope.row.depositAmount" :disabled="scope.row.depositBaseAmount === '1' || scope.row.depositBaseAmount === '3'" clearable />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="returnCondition" label="返还条件">
                <template slot-scope="scope">
                  <el-form-item label-width="0" :prop="'agreementDeposits.' + scope.$index + '.returnCondition'"
                    :rules="[{ required: true, trigger: 'blur', message: '请输入返还条件' }]">
                    <el-input type="textarea" :autosize="{maxRows: 2}" v-model="scope.row.returnCondition" clearable />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column prop="remark" label="备注">
                <template slot-scope="scope">
                  <el-form-item :prop="'agreementDeposits.' + scope.$index + '.remark'" label-width="0">
                    <el-input v-model="scope.row.remark" type="textarea" :autosize="{maxRows: 2}" clearable />
                  </el-form-item>
                </template>
              </el-table-column>
              <el-table-column label="操作" align="center" width="80">
                <template slot-scope="scope">
                  <el-button size="mini" type="text"
                    @click="handleDelete('agreementDeposits', scope.$index)" >删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>

          <!-- 合同附件 -->
          <commonTitle>
            合同附件
            <template #right>
              <el-upload
                :action="uploadFileUrl"
                :on-success="handleSuccessContract"
                :before-upload="handleBeforeUpload"
                :on-remove="handleRemoveContract"
                :file-list="fileList"
                multiple
                ref="uploadRef"
                :show-file-list="false"
              >
                <el-button type="success" icon="el-icon-plus" size="mini">上传附件</el-button>
              </el-upload>
            </template>
          </commonTitle>
            <div style="margin-bottom: 8px">
                <el-table :data="firstForm.agreementAttachmentList" style="width: 100%">
                  <el-table-column prop="fileName" label="文件名" align="center" />
                  <el-table-column label="操作" align="center" width="200">
                    <template slot-scope="scope">
                      <el-button size="mini" type="text" @click="handleRemoveContract(scope.$index)" >删除</el-button>
                      <el-button size="mini" type="text" @click="handleReUpload(scope.$index)" >重新上传</el-button>
                      <el-button size="mini" type="text" @click="handleView(scope.row.fileName, scope.row.fileUrl)" >预览</el-button>
                    </template>
                  </el-table-column>
                </el-table>
            </div>
          </el-tab-pane>
            <el-tab-pane label="合同附件" name="second" />
          </el-tabs>
          <!-- 合同附件 -->
          <div class="contract-box" :class="activeName !== 'second' && 'hide'">
            <!-- <FileModule
              ref="file"
              v-if="attachmentId"
              :attachmentId="attachmentId"
              @submitFileZ="subForm"
              :isContract="true"
              type="edit"
            /> -->
            <iframe allowfullscreen="true"
              v-if="attachmentId"
              :src= this.editFileUrl
              width="100%"
              height="700px"
              frameborder="0"
            ></iframe>

          </div>
        </div>
      </el-form>
    </div>



    <!--  选择开户银行等信息  -->
    <el-dialog title="选择开户银行" :visible.sync="bankVisible">
      <el-form
        :model="queryParams"
        ref="queryForm"
        :inline="true"
        label-width="100px"
      >
        <el-form-item label="支行名称" prop="name">
          <el-input
            v-model="queryParams.name"
            style="width: 200px"
            placeholder="请输入支行名称"
            clearable
          />
        </el-form-item>
        <el-form-item label="银行名称" prop="parentName">
          <el-input
            v-model="queryParams.parentName"
            style="width: 200px"
            placeholder="请输入银行名称"
            clearable
          />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="queryParams.pageNumber = 1;getBankListVisible(null)">搜索</el-button>
        </el-form-item>
      </el-form>
      <el-table
        :data="bankList"
        empty-text="暂无数据"
        border
        v-loading="bankLoading"
        element-loading-text="加载中..."
      >
        <el-table-column
          label="支行名称"
          align="center"
          prop="name"
        />
        <el-table-column
          label="银行名称"
          prop="parentName"
          width="180"
          align="center"
          show-overflow-tooltip
        />
        <el-table-column
          label="银联号"
          prop="code"
          width="150"
          align="center"
          show-overflow-tooltip
        />
        <el-table-column
          label="操作"
          type="index"
          width="80"
          align="center">
          <template slot-scope="scope">
            <el-button type="primary" size="mini" plain @click="rowClickBank(scope)">选择</el-button>
          </template>
        </el-table-column>
      </el-table>

      <pagination
        v-show="totalBank > 0"
        :total="totalBank"
        :page.sync="queryParams.pageNumber"
        :limit.sync="queryParams.pageSize"
        @pagination="getBankListVisible(null)"
        :page-sizes="[10, 20, 40, 100]"
      />
    </el-dialog>


    <!-- 添加 -->
    <el-dialog
      :title="dialogTitle"
      :visible.sync="dialogOpen"
      @closed="dialogClosed"
      width="60%"
      append-to-body
    >
      <div class="split-page-box flex full">
        <Drag box="box1">
          <template v-slot:left-content>
            <div class="left">
              <el-input
                v-model="deviceName"
                placeholder="请输入部门名称"
                clearable
                size="small"
                prefix-icon="el-icon-search"
                style="margin-bottom: 12px"
              />
              <ul class="node-box">
                <li v-for="item in deptOptionsChildren"  @click="expandLevel(`${item}`)">
                  {{ item }}
                </li>
              </ul>
              <el-tree
                :data="deptOptions"
                :props="defaultProps"
                class="tree_expert"
                :expand-on-click-node="false"
                :filter-node-method="filterNode"
                ref="tree"
                node-key="id"
                highlight-current
                :default-expanded-keys="expandedKeys"
                @node-click="handleNodeClick"
              />
            </div>
          </template>
          <template v-slot:right-content>
            <div class="right-box fill">
              <div class="right-item border box">
                <div class="right-title">
                  <span>特征项</span>
                </div>
                <div class="list-container">
                  <div class="list-item" @click="selectTerm(item.id)" :class="item.id === term && 'selected'" v-for="item in termList" :key="item.id">
                    <el-checkbox class="disabled-checkbox" :value="isIndeterminateObj[item.id].isAll" :indeterminate="isIndeterminateObj[item.id].isHas"/>
                    {{ item.featureName }}
                  </div>
                </div>
              </div>
              <div class="right-item box">
                <div class="right-title">
                  <el-checkbox label="特征值" :indeterminate="isIndeterminate" v-model="termValueAll" @change="termValueAllChange"/>
                </div>
                <div class="list-container">
                  <div class="list-item" v-for="item in termValueMap[term]">
                    <el-checkbox :label="item.featureValueName" :key="item.id" v-model="item.selected" @change="changeTermValue(item.id)">{{item.featureValueName}}</el-checkbox>
                  </div>
                </div>
              </div>
              <div class="right-item">
                <div class="right-title">
                  <span>已选内容</span>
                </div>
                <div class="list-container">
                  <div class="list-item space-between" v-for="(item,i) in selectedList" :key="i">
                    <span>{{ item.value }}</span>
                    <div class="right-delete" @click="deleteItem(i)">
                      <i class="el-icon-delete"></i>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
        </Drag>
      </div>
      <div slot="footer" class="dialog-footer">
        <el-button @click="dialogOpen = false" size="small" style="width: 100px"
          >取 消</el-button
        >
        <el-button
          type="primary"
          @click="confirmSelect"
          size="small"
          style="width: 100px"
          >确 定</el-button
        >
      </div>
    </el-dialog>
     <!-- 选择招标文件模板 -->
     <el-dialog
     :title="bcTemplateTitle"
     class="dialogClass"
     :visible.sync="bcTemplateVisable"
     width="60%"
   >
     <el-tabs v-model="activeTab" @tab-click="handleTabClick">
       <el-tab-pane label="通用模板" name="generalTemplate">
         <el-form
           @submit.native.prevent
           :model="bcTemplateQuery"
           ref="queryForm"
           :inline="true"
         >
           <el-form-item
             label="模板名称"
             prop="templateName"
             label-width="100px"
           >
             <el-input
               v-model="bcTemplateQuery.templateName"
               placeholder="请输入模板名称"
               @change="handleQuery"
             />
           </el-form-item>
           <el-form-item
             v-if="!isScoreMOdel"
             label="合同类型："
             prop="contractType"
           >
             <el-select
               style="width: 100%"
               v-model="bcTemplateQuery.contractType"
               placeholder="请选择"
               @change="handleQuery"
             >
               <el-option
                 v-for="dict in contractTypeList"
                 :key="dict.value"
                 :label="dict.label"
                 :value="dict.value"
               />
             </el-select>
           </el-form-item>
           <el-form-item>
             <el-button
               type="primary"
               icon="el-icon-search"
               size="small"
               @click="handleQuery"
               >查询</el-button
             >
           </el-form-item>
         </el-form>
         <el-table
           v-loading="bcTemplateVisableLoading"
           :data="generalTemplateList"
           @row-click="selectBcTemplate"
           size="small"
           border
           stripe
         >
           <el-table-column label="" width="30" align="center">
             <template slot-scope="scope">
               <el-radio
                 class="table_radio"
                 v-model="templateId"
                 :label="scope.row.id"
               />
             </template>
           </el-table-column>
           <el-table-column
             label="序号"
             type="index"
             width="50"
             align="center"
           />
           <el-table-column
             label="模板名称"
             prop="templateName"
             width="200"
           />
           <el-table-column label="维护人" align="center" prop="createBy" />
           <el-table-column
             label="创建日期"
             align="center"
             prop="createTime"
           />
           <el-table-column label="使用单位" prop="usingUnitName" />
           <el-table-column
             v-if="!isScoreMOdel"
             label="合同类型"
             prop="contractName"
           />
         </el-table>
         <pagination
           v-show="generalTemplateTotal > 0"
           :total="generalTemplateTotal"
           :page.sync="bcTemplateQuery.pageNumber"
           :limit.sync="bcTemplateQuery.pageSize"
           @pagination="getGeneralTemplateList"
         />
       </el-tab-pane>

       <el-tab-pane label="复用模板" name="reusableTemplate">
         <el-form :model="bcTemplateQuery" ref="queryForm" :inline="true">
           <el-form-item
             label="模板名称"
             prop="templateName"
             label-width="100px"
           >
             <el-input
               v-model="bcTemplateQuery.templateName"
               placeholder="请输入模板名称"
               @change="handleQueryReusable"
             />
           </el-form-item>
           <el-form-item
             v-if="!isScoreMOdel"
             label="合同类型："
             prop="contractType"
           >
             <el-select
               style="width: 100%"
               v-model="bcTemplateQuery.contractType"
               placeholder="请选择"
               @change="handleQueryReusable"
             >
               <el-option
                 v-for="dict in contractTypeList"
                 :key="dict.value"
                 :label="dict.label"
                 :value="dict.value"
               />
             </el-select>
           </el-form-item>
           <el-form-item>
             <el-button
               type="primary"
               icon="el-icon-search"
               size="small"
               @click="handleQueryReusable"
               >查询</el-button
             >
           </el-form-item>
         </el-form>
         <el-table
           v-loading="bcTemplateVisableLoading"
           :data="reusableTemplateList"
           @row-click="selectBcTemplate"
           size="small"
           border
           stripe
         >
           <el-table-column label="" width="30" align="center">
             <template slot-scope="scope">
               <el-radio
                 class="table_radio"
                 v-model="templateId"
                 :label="scope.row.id"
               />
             </template>
           </el-table-column>
           <el-table-column
             label="序号"
             type="index"
             width="50"
             align="center"
           />
           <el-table-column
             label="模板名称"
             prop="templateName"
             width="200"
           />
           <el-table-column label="维护人" align="center" prop="createBy" />
           <el-table-column
             label="创建日期"
             align="center"
             prop="createTime"
           />
           <el-table-column label="使用单位" prop="usingUnitName" />
           <el-table-column
             v-if="!isScoreMOdel"
             label="合同类型"
             prop="contractName"
           />
         </el-table>
         <pagination
           v-show="reusableTemplateTotal > 0"
           :total="reusableTemplateTotal"
           :page.sync="bcTemplateQuery.pageNumber"
           :limit.sync="bcTemplateQuery.pageSize"
           @pagination="getReusableTemplateList"
         />
       </el-tab-pane>
     </el-tabs>

     <div slot="footer" class="dialog-footer">
       <el-button
         @click="bcTemplateVisable = false"
         style="width: 100px"
         size="small"
         >取 消</el-button
       >
       <el-button
         type="primary"
         @click="confirmBcTemplate"
         style="width: 100px"
         size="small"
         >确 定</el-button
       >
     </div>
   </el-dialog>

    <el-dialog title="合同附件预览" :visible.sync="viewFileDialog" width="80%">
      <iframe allowfullscreen="true"
              :src= this.viewFileUrl
              width="100%"
              height="600px"
              frameborder="0"
      ></iframe>
    </el-dialog>
  </div>
</template>
<script>
import { Base64 } from "js-base64";
import { create, all } from "mathjs";
import commonTitle from "@/views/procurement/components/common-title.vue";
import { getAgreementEditURL,getAgreementCreateInfo, saveAgreement, listUnderlingDict, listDeviceClass, listDevice, listMaterialsClass, listMaterials, deviceFeatureList, deviceFeatureValueList, listMaterialsFeature, listMaterialsFeatureValue, getAgreementCreateInfoYl,agreementCreateAttachmentHandle,avoidSubmitByMarket } from "@/api/procurement/contract";
import {listAccountBank, getBankList} from "@/api/vendor/vendor";
import { offerService, offerRepo } from "@/utils/const"
import { cardid, isvalidatemobile, validatenull } from "@/utils/validate"
import BackButton from "@/components/BackButton/index.vue"
import FileModule from '@/components/FileModule/index.vue'
import Drag from '@/components/Drag/index.vue'
import {addAttachment, getContractTypeList, getEditFileUrlByID} from "@/api/template/file";
import {
  getTemplateSwitchList,
} from "@/api/procurement/scheme";
import {getLoadTaskDef, getOrgByUserId, getProcessLogList, getSwitchPageList} from "@/api/procurement/manage";
import {showSecretRelatedTips} from "@/utils/MyUtils";
import {uploadFileUrl} from "@/utils/const";
import { getViweFileURL } from "@/api/template/file";

export default {
  name: "add-contract",
  components: { commonTitle, BackButton, FileModule, Drag },
  dicts: ["sys_yes_no", "expenditureBusinessType"],
  data() {
    return {
      viewFileDialog: false,
      viewFileUrl: "",
      uploadFileUrl, // 替换为实际的上传地址
      fileList: [], // el-upload 组件的文件列表
      reuploadIndex: '', // 重新上传索引
      editFileUrl:"", //编辑合同附件URL
      //模板
      isAvoidSubmit:false,
      bcTemplateTitle: "",
      bcTemplateVisableLoading: false,
      generalTemplateLoading: false,
      bcTemplateVisable: false,
      activeTab: "generalTemplate",
      currentTab: "generalScoreTemplate",
      selectedTemplateId: "",
      isScoreMOdel: false,
      contractTypeList: [],
      generalTemplateList:[],
      reusableTemplateList:[],
      generalTemplateTotal: 0,
      reusableTemplateTotal: 0,
      templateId: null,
      templateRow:{},
      templateQuery: {
        pageNumber: 1,
        pageSize: 10,
        state: 1,
        switchTemplateType: 1,
      },
      bcTemplateQuery: {
        pageNumber: 1,
        pageSize: 10,
        state: 1,
        templateType: "",
        templateName: "",
        switchTemplateType: "",
        contractType: "",
      },
      typeContract:'',
      contractType: 1, // 1-物资采购类  2-物资租赁类  3-机械租赁类  4-专业分包类  5-劳务分包类  6-其它
      priceType: "",
      subjectMatter: "",
      activeName: "first",
      options1: [],
      firstForm: {
        agreement: {
          paymentWay:[],
          agreementName:'',
        }, // 合同基本信息
        agreementPaymentItem: {}, // 合同款项信息
        agreementPaymentLists: [], // 结算与付款节点信息
        agreementPartyInfoLists: [{
          roleType: '1', // 合同甲方
        },{
          roleType: '2', // 合同乙方
        }], // 合同签约方信息
        agreementMaterialsLists: [], // 合同清单
        agreementMaterialsListsYl:[],// 物料合同清单
        agreementDeposits: [], // 合同保证金
        agreementDailyWageList: [], // 合同-计日工对象
        agreementMachineShifts: [], // 合同-机械台班对象
        agreementEquipmentSupplies: [], // 合同-甲供设备清单对象
        agreementMaterialSupplies: [], // 合同-甲供材料清单对象
        agreementAttachmentList: [], // 合同附件
      },
      agreementFileName: "",
      agreementFileUrl: "",
      attachmentId: "",
      offerService,
      offerRepo,
      sumitLoding: null,
      templateEditFlag: 0,
      isSubmit: false,
      dictObj: {
        payment_cycle: "", // 结算周期
        payment_way: "", // 付款方式
        con_role_type: "", // 合同签约方信息
        currency: "", // 币种
        invoice_type: "", // 发票类型
        priceForm: "", // 价格形式
        payment_basis: "", // 付款基准
        settlement_stage: "", // 结算阶段
        payment_type: "", // 付款类型
        deposit_type: "", // 保证金类型
        deposit_way: "", // 保证金方式
        deposit_base_amount: "", // 保证金基数
        jobTitleCode: "", // 工种
        rentalType: "", // 租赁方式
        rentalUnit: "", // 租赁单位
      },
      dictObjMap: {
        PAYMENT_CYCLE: "payment_cycle", //支付周期
        PAYMENT_TYPE: "payment_way", //付款方式
        CON_ROLE_TYPE: "con_role_type", //合同签约方信息
        SYS_CURRENCY: "currency", //币种
        INVOICE_TYPE: "invoice_type", //发票类型
        PAYMENT_BASE_TYPE: "payment_basis", //付款基数
        SETTLEMENT_STAGE: "settlement_stage", //结算阶段
        PRICE_TYPE: "payment_type", //价款类型
        DEPOSIT_TYPE: "deposit_type", //押金/保证金类型
        DEPOSIT_BASE_TYPE: "deposit_base_amount", //押金/保证金基数
        PRICE_FORM: "priceForm", //价款形式
        DEPOSIT_MODE: "deposit_way", //押金/保证金方式
        DATALLER_WORK_TYPE: "jobTitleCode", //工种
        RENT_MODE: "rentalType", //租赁方式
        RENT_UNIT: "rentalUnit", //租赁单位
      },
      dialogOpen: false,
      dialogTitle: "",
      queryParam: {},
      deptOptions: undefined,
      deptOptionsChildren:1,
      defaultProps: {
        children: "children",
        label: "name",
      },
      totalBank: 0,
      bankList: [],
      bankLoading: false,
      bankVisible: false,
      queryParams: {
        pageNumber: 1,
        pageSize: 10,
      },
      treeLoading:false,
      treeList:[],
      checkedIndex:0,
      selectType:'',
      classifyType:1,
      listTotal: 0,
      deviceName:undefined,
      term:'',//项
      termList:[],
      termValue:'',//值
      termValueMap:{},
      selectedList:[],
      currentNode:{},
      expandedKeys:[],
      termValueAll:false,
      isIndeterminate:false,
      isIndeterminateObj:{},
    }
  },
  mounted() {
    this.mathjs = create(all);
    this.mathjs.config({
      number: "BigNumber",
    });
    //获取字典
    Object.keys(this.dictObjMap).forEach((key) => {
      this.getListUnderlingDict(key);
    });
  },
      // 组件不具有此钩子
    beforeRouteLeave(to, from, next) {
      this.$destroy(true)
      console.log("destroyed---vue实例销毁后beforeRouteLeave");
      next();
    },
  methods: {
    /* 点击显示银行账户列表 */
    getBankListVisible(scope) {
      /* 如果是甲方就打开选择弹窗 */
      if( (!scope && this.queryParams.roleType === 1) || (scope && scope.row && scope.row.roleType == 1)){
        this.bankVisible = true;
        this.queryParams.roleType = 1;
        this.getBankListFn();
      }else if( (!scope && this.queryParams.roleType === 2) || (scope && scope.row && scope.row.roleType == 2)){
        this.bankVisible = true;
        this.queryParams.roleType = 2;
        this.listAccountBank();
      }else{
        this.bankVisible = false;
      }
    },
    /* 获取甲方银行账户列表 */
    async getBankListFn() {
      this.bankLoading = true;
      const res = await getBankList(this.queryParams)
      this.bankLoading = false;
      this.bankList = res.data.rows
      this.totalBank = res.data.total
    },
    /* 获取乙方银行账户列表 */
    async listAccountBank() {
      this.bankLoading = true;
      this.queryParams.vendorId = this.firstForm.agreement.vendorId;
      const res = await listAccountBank(this.queryParams)
      this.bankLoading = false;
      this.bankList = res.data.rows
      this.bankList.map(account => {
        account.name = account.openingBranch;
        account.parentName = account.affiliatedBank;
        account.code = account.interbankNumber;
        return account;
      })
      this.totalBank = res.data.total
      /* 返回列表 */
      return this.bankList;
    },
    /* 点击选中银行 */
    rowClickBank(scope) {
      this.bankVisible = false;
      this.firstForm.agreementPartyInfoLists.map((obj, index) =>{
        debugger
        /* 甲方 银行账号赋值 */
        if(obj.roleType == 1 && this.queryParams.roleType == 1){
          this.$set(obj, 'signerBankAccount', scope.row.name);
          this.$set(obj, 'signerBankOpen', scope.row.code);
          this.$set(obj, 'signerBankName', scope.row.parentName);
          this.$refs.firstForm.clearValidate(`agreementPartyInfoLists.${index}.signerBankAccount`);
          this.$refs.firstForm.clearValidate(`agreementPartyInfoLists.${index}.signerBankOpen`);
          this.$refs.firstForm.clearValidate(`agreementPartyInfoLists.${index}.signerBankName`);
        }
        /* 乙方 供应商银行账号赋值 */
        if(obj.roleType == 2 && this.queryParams.roleType == 2){
          this.$set(obj, 'signerBankAccount', scope.row.name);
          this.$set(obj, 'signerBankOpen', scope.row.code);
          this.$set(obj, 'signerBankName', scope.row.parentName);
          this.$refs.firstForm.clearValidate(`agreementPartyInfoLists.${index}.signerBankAccount`);
          this.$refs.firstForm.clearValidate(`agreementPartyInfoLists.${index}.signerBankOpen`);
          this.$refs.firstForm.clearValidate(`agreementPartyInfoLists.${index}.signerBankName`);
        }
        return obj;
      });
      this.$forceUpdate(); // 强制刷新视图
    },
    /** 上传附件成功后回调 */
    async handleSuccessContract(res) {
      const { url, name } = res.data;
      try {
        /* 保存到文件表获取返回id */
        const res = await addAttachment({ fileName: name, fileUrl: url });
        if (this.reuploadIndex !== '') {
          // 替换指定索引的附件
          this.$set(this.firstForm.agreementAttachmentList, this.reuploadIndex, {
            businessId: res.data,
            fileName: name,
            fileUrl: url
          });
          this.reuploadIndex = ''; // 清除索引
        }else{
          // 新增附件
          this.firstForm.agreementAttachmentList.push({
            businessId: res.data,
            fileName: name,
            fileUrl: url,
          });}
      } catch (err) {
        console.log(err);
      }
    },

    /** 上传附件前校验 */
    handleBeforeUpload(file) {
      //限制上传的文件名长度
      const fileName = file.name;
      if (fileName.length > 80) {
        this.$message.error('文件名不能超过80个字符');
        return false; // 阻止上传
      }
      return true;  // 返回 true 表示允许继续上传
    },

    /** 删除其他附件 */
    handleRemoveContract(index) {
      this.$confirm("是否删除该附件？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(() => {
        this.firstForm.agreementAttachmentList.splice(index, 1);
        this.$message.success('删除成功');
      }).catch(() => {});
    },

    /** 重新上传文件 */
    handleReUpload(index) {
      this.$confirm("是否重新上传该附件？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning"
      }).then(() => {
        this.$refs.uploadRef.clearFiles(); // 清空上传组件的文件列表
        this.reuploadIndex = index; // 记录当前索引
        this.$refs.uploadRef.$refs["upload-inner"].handleClick(); // 触发文件选择器
      }).catch(() => {});
    },

    /** 合同其他文件预览 */
    async handleView(fileName, fileUrl) {
      this.viewFileDialog = true;
      try {
        const param = {fileName: fileName, fileUrl: fileUrl}
        const res = await getViweFileURL(param);
        this.viewFileUrl = res.data;
      } catch (ex) {
        console.log("预览文件出错", ex);
      }
    },

    //切换页签到合同附件时
    attachmenthandleTabClick(tab){
      // tab.name 是被点击的标签页的 name 属性值
      this.editFileUrl=""; //先清空编辑文档的URL，在重新获取
      if (tab.name === 'second') {
        this.getAttachmentEditURL();
      }
    },
    // 获取合同附件的文档中台编辑URL
    getAttachmentEditURL() {
      // let formData = JSON.parse(JSON.stringify(this.firstForm.agreement));
      // 确保 attachmentId 存在
      if (!this.firstForm.agreement.attachmentId) {
        console.warn('attachmentId 数据未正确加载');
        return;
      }
      let formData = {
        agreement: JSON.parse(JSON.stringify(this.firstForm.agreement)), // 深拷贝 agreement
        agreementPaymentItem: JSON.parse(JSON.stringify(this.firstForm.agreementPaymentItem)) // 深拷贝 agreementPaymentItem
      };
      delete formData.agreement.expenditureBusinessType;
      formData.agreement.paymentWay = this.firstForm.agreement.paymentWay?.join(",") || '';
      //waterMarkContent
      let params = JSON.parse(JSON.stringify(formData));
      console.log("生成编辑文档URL的params数据===>", params);
      console.log('新增合同签订编辑文件的AttachmentID:', params.agreement.attachmentId);
      // 获取文档中台的文档编辑URL
      getAgreementEditURL(params)
        .then((res) => {
          this.editFileUrl = res.data;
          console.log("新增合同签订编辑editFileUrl:", this.editFileUrl);
        })
        .catch((err) => {
          console.error('生成编辑文档URL的错误:', err);
        });
    },

    selectBcTemplate(row) {
      this.templateId = row.templateId;
      this.templateRow=row
      // this.attachmentId = row.attachmentId;
      console.log(row, "rrr");
    },
    async getContractModelList() {
      // 获取通用模板列表数据
      this.bcTemplateVisableLoading = true;
      // API 调用获取数据
      this.bcTemplateQuery.switchTemplateType = "1";
      this.bcTemplateQuery.templateType = "1";
      const res = await getSwitchPageList(this.bcTemplateQuery);
      this.generalTemplateList = res.data.rows;
      this.bcTemplateVisableLoading = false;
    },
    async getGeneralTemplateList() {
      // 获取通用模板列表数据
      this.bcTemplateVisableLoading = true;
      // API 调用获取数据
      this.bcTemplateQuery.switchTemplateType = "1";
      const res = await getSwitchPageList(this.bcTemplateQuery);
      this.generalTemplateList = res.data.rows;
      this.bcTemplateVisableLoading = false;
    },
    async confirmBcTemplate() {
      const templateId = this.templateId;
      if (!templateId) {
        this.$message.error("请先选择一个模板");
        return;
      }
      this.firstForm.agreement.contractTemplateName=this.templateRow.templateName
      // this.attachmentId = templateId
      // this.firstForm.agreement.attachmentId = templateId;
      // this.attachmentId = this.templateRow.attachmentId;
      //   this.firstForm.agreement.attachmentId = this.templateRow.attachmentId;
      //   console.log(this.templateId+"firstForm.agreement.attachmentId ")
      agreementCreateAttachmentHandle(this.templateRow).then((res) => {
        this.attachmentId = res
        this.firstForm.agreement.attachmentId = res;
        console.log(this.templateId+"firstForm.agreement.attachmentId "+JSON.stringify(this.attachmentId))
        // this.getAttachmentEditURL(); //获取文档编辑的URL
      })
      // const templateName = this.bcTemplateList.find(
      //   (item) => item.id === templateId
      // ).templateName;

      // if (this.bcTemplatetType === 2) {
      //   this.$set(this.formData, "biddingTemplateName", templateName);
      //   this.$set(this.formData, "biddingTemplateId", templateId);
      //   this.$refs.form.clearValidate("biddingTemplateName");

      //   // 加载文件框
      //   let fileName = this.bcTemplateList.find(
      //     (item) => item.id === templateId
      //   ).fileName;
      //   const fileUrl = this.bcTemplateList.find(
      //     (item) => item.id === templateId
      //   ).fileUrl;
      //   try {
      //     const res = await addAttachment({
      //       fileName: fileName,
      //       fileUrl: fileUrl,
      //     });
      //     this.$set(this.formData, "biddingAttachmentId", res.data);
      //     // this.viewAttachmentId = res.data;
      //     this.attachmentId = res.data;
      //     console.log("attachmentId"+this.attachmentId)
      //   } catch (err) {
      //     console.log(err);
      //   }
      // } else {
      //   this.$set(this.formData, "contractTemplateName", templateName);
      //   this.$set(this.formData, "contractTemplateId", templateId);

      //   this.$refs.form.clearValidate("contractTemplateName");
      // }

      this.bcTemplateVisable = false;
    },
    async getReusableTemplateList() {
      // 获取复用模板列表数据
      this.bcTemplateVisableLoading = true;
      // API 调用获取数据
      this.bcTemplateQuery.switchTemplateType = "2";
      const res = await getSwitchPageList(this.bcTemplateQuery);
      this.reusableTemplateList = res.data.rows;
      this.bcTemplateVisableLoading = false;
    },
      /** 搜索按钮操作 */
    handleQuery() {
      this.bcTemplateQuery.pageNumber = 1;
      this.getGeneralTemplateList();
    },
    handleQueryReusable() {
      this.bcTemplateQuery.pageNumber = 1;
      this.getReusableTemplateList();
    },
    getContractTypeList() {
      getContractTypeList().then((res) => {
        this.contractTypeList = res.data;
      });
    },
    handleTabClick(tab) {
      // 处理标签页点击事件，根据标签页切换表格数据
      this.activeTab = tab.name;

      if (tab.name === "generalTemplate") {
        this.getGeneralTemplateList();
      } else if (tab.name === "reusableTemplate") {
        this.getReusableTemplateList();
      }
    },
    onTabClick(tab) {
      if (tab.name === "generalScoreTemplate") {
        this.getGeneralScoreTemplateList();
      } else if (tab.name === "reusableScoreTemplate") {
        this.getReusableScoreTemplateList();
      }
    },
    searchGeneralTemplates() {
      this.templateQuery.pageNumber = 1;
      this.getGeneralScoreTemplateList();
    },
    searchReusableTemplates() {
      this.templateQuery.pageNumber = 1;
      this.getReusableScoreTemplateList();
    },
    async getGeneralScoreTemplateList() {
      // 获取通用模板列表数据
      this.generalTemplateLoading = true;
      // API 调用获取数据
      this.templateQuery.switchTemplateType = "1";
      const res = await getTemplateSwitchList(this.templateQuery);
      this.generalScoreTemplateList = res.data.rows;
      this.generalTemplateLoading = false;
    },
    async getReusableScoreTemplateList() {
      // 获取通用模板列表数据
      this.generalTemplateLoading = true;
      // API 调用获取数据
      this.templateQuery.switchTemplateType = "2";
      const res = await getTemplateSwitchList(this.templateQuery);
      this.generalReuScoreTemplateList = res.data.rows;
      this.generalTemplateLoading = false;
    },

    onTemplateSelect(row) {
      this.selectedTemplateId = row.id;
      //  this.attachmentId = row.attachmentId;
    },
    async getBcTemplateList(type) {
      showSecretRelatedTips(async()=>{
        this.bcTemplateTitle = type === 2 ? "选择招标文件模板" : "选择合同模板";
        this.isScoreMOdel = type === 2;
        this.activeTab = "generalTemplate";
        this.bcTemplateVisable = true;
        this.bcTemplatetType = type;
        this.bcTemplateQuery.templateType = type;
        this.bcTemplateQuery.contractType = "";
        // 根据模板类型调用相应的方法
        if (type === 2) {
          this.getGeneralTemplateList();
        } else {
          this.getContractModelList();
        }
        // 设置 switchTemplateType 和 templateType
        this.bcTemplateQuery.switchTemplateType = "1";
        this.bcTemplateQuery.templateType = type === 2 ? "2" : "1";

        // 获取模板列表
        const res = await getSwitchPageList(this.bcTemplateQuery);
        this.bcTemplateList = res.data.rows;
      })

    },


    visaAdd(type) {
      if (type == 1) {
        this.firstForm.agreementDailyWageList.push({
          jobTitleCode: "",
          unitMeasurement: "",
        });
      } else {
        this.firstForm.agreementMachineShifts.push({
          equipmentName: "",
          specification: "",
          unitMeasurement: "",
        });
      }
    },
    visaDel(index, type) {
      this.$confirm("是否删除该行", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          if (type == 1) {
            this.firstForm.agreementDailyWageList.splice(index, 1);
          } else {
            this.firstForm.agreementMachineShifts.splice(index, 1);
          }
        })
        .catch(() => {});
    },
    handleDelete(type, index) {
      // * 先给个提示
      this.$confirm("是否删除该行", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(() => {
          this.firstForm[type].splice(index, 1);
        })
        .catch(() => {});
    },
    avoidSubmitForm(){

      // if(parseInt(this.firstForm.agreementPaymentItem.totalAmountIncTax)>50000) return this.$message({type:'error',message:"合同金额小于5万才允许免审提交"});
      this.$confirm("确定免审提交?(合同金额小于5万才允许免审提交)", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(() => {
        this.isAvoidSubmit=true
        this.submitForm();
      });
    },

    submitForm() {
      this.$refs.firstForm.validate((valid, obj) => {
        let isNull = validatenull(obj);
        if (!isNull) {
          console.log(obj, "obj--obj");
          for (const [key, value] of Object.entries(obj)) {
            this.$message.error(value[0]);
            break;
          }
          return false;
        }
        // * 先校验3个表格是否不为空
        if (this.firstForm.agreementPaymentLists.length === 0) {
          this.$message.error("结算与付款节点信息不完整");
          return false;
        }
        if (this.firstForm.agreementPartyInfoLists.length === 0) {
          this.$message.error("合同签约方信息不完整");
          return false;
        }
        if (this.firstForm.agreementDeposits.length === 0) {
          this.$message.error("押金、保证金信息不完整");
          return false;
        }
        if (this.firstForm.agreementMaterialsLists.length === 0) {
          this.$message.error("合同清单不完整");
          return false;
        }
        // if ([4, 5].includes(this.contractType)) {
        //   // if (this.firstForm.agreementDailyWageList.length === 0) {
        //   //   return this.$message.error('请填写完整计日工表单')
        //   // }
        //   // if (this.firstForm.agreementMachineShifts.length === 0) {
        //   //   return this.$message.error('请填写完整机械台班表单')
        //   // }
        //   if (this.contractType == 4) {
        //     if (this.firstForm.agreementEquipmentSupplies.length === 0) {
        //       return this.$message.error('请填写完整甲供设备表单')
        //     }
        //     if (this.firstForm.agreementMaterialSupplies.length === 0) {
        //       return this.$message.error('请填写完整甲供材料表单')
        //     }
        //   }
        // }
        // if ([4, 5].includes(this.contractType)) {
        //   // if (this.firstForm.agreementDailyWageList.length === 0) {
        //   //   return this.$message.error('请填写完整计日工表单')
        //   // }
        //   // if (this.firstForm.agreementMachineShifts.length === 0) {
        //   //   return this.$message.error('请填写完整机械台班表单')
        //   // }
        //   if (this.contractType == 4) {
        //     if (this.firstForm.agreementEquipmentSupplies.length === 0) {
        //       return this.$message.error('请填写完整甲供设备表单')
        //     }
        //     if (this.firstForm.agreementMaterialSupplies.length === 0) {
        //       return this.$message.error('请填写完整甲供材料表单')
        //     }
        //   }
        // }
        if (valid) {
          this.sumitLoding = this.$loading({
            lock: true,
            text: "数据提交中...",
            spinner: "el-icon-loading",
            background: "rgba(0, 0, 0, 0.7)",
          });
          this.isSubmit = true;
          console.log(this.firstForm, "this.firstForm----------------------");
         // this.$refs.file.saveFile();
          this.subForm();
        } else {
          this.isSubmit = false;
          // this.sumitLoding.close();
          return false;
        }
      });
    },
    subForm() {
      if (this.isSubmit) {
        delete this.firstForm.agreement.expenditureBusinessType;
      //  this.firstForm.templateEditFlag = this.templateEditFlag;
        let formData = JSON.parse(JSON.stringify(this.firstForm));
        formData.agreement.paymentWay = this.firstForm.agreement.paymentWay?.join(",") || '';
        formData.agreement.marketMaterialContractId=this.firstForm.agreement.marketMaterialContractId
        let params = JSON.parse(JSON.stringify(formData));
        console.log("提交数据===>", params);
        // saveAgreement(params)
        //   .then((res) => {
        //     if (res.success) {
        //       _this.$message.success("保存成功");
        //       if(_this.isAvoidSubmit){
        //         // this.$route.query.id
        //         avoidSubmitByMarket(res?.data?.id).then((res) => {

        //         })
        //       }
        //       let param = Base64.encode(
        //         JSON.stringify({
        //           id: res?.data?.id,
        //           type: res?.data?.procurementPlanType,
        //           agreementName: this.firstForm.agreement.agreementName,
        //         })
        //       );
        //       param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
        //       _this.$router.replace(`/procurement/contract-detail/${param}`);
        //       _this.sumitLoding.close();
        //     }
        //   })
        //   .catch((err) => {
        //     console.log(err);
        //     _this.sumitLoding.close();
        //   });
        saveAgreement(params)
          .then((res) => {
            if (res.success) {
              this.$message.success("保存成功");
              console.log("this.isAvoidSubmit的值-》",this.isAvoidSubmit);
              if(this.isAvoidSubmit){
                // this.$route.query.id
                avoidSubmitByMarket(res?.data?.id).then((res) => {

                })
              }
              let param = Base64.encode(
                JSON.stringify({
                  id: res?.data?.id,
                  type: res?.data?.procurementPlanType,
                  agreementName: this.firstForm.agreement.agreementName,
                })
              );
              param = encodeURIComponent(param); //避免base64编码中出现"/"时路由404
              this.$router.replace(`/procurement/contract-detail/${param}`);
              this.sumitLoding.close();
            }
          })
          .catch((err) => {
            console.log(err);
            this.sumitLoding.close();
          });
      }
    },
    addRow(type) {
      this.firstForm[type].push({});
    },
    //身份证号码校验
    isCardId(rule, value, callback) {
      if (cardid(value)[0]) {
        callback(new Error(cardid(value)[1]));
      } else {
        callback();
      }
    },
    //手机号码校验
    isMobile(rule, value, callback) {
      if (isvalidatemobile(value)[0]) {
        callback(new Error(isvalidatemobile(value)[1]));
      } else {
        callback();
      }
    },
    validateNumber(rule, value, callback) {
      const reg = /^\d+(\.\d{1,2})?$/;
      if (value === "" || value === undefined) {
        callback();
      } else if (!reg.test(value)) {
        // callback(new Error("请输入正确的值"));
        callback(new Error("请输入正确的数值且小数点保留两位"));
      } else {
        callback();
      }
    },
    validateNumber(rule, value, callback) {
      const reg = /^\d+(\.\d{1,2})?$/;
      if (value === "" || value === undefined) {
        callback();
      } else if (!reg.test(value)) {
        // callback(new Error("请输入正确的值"));
        callback(new Error("请输入正确的数值且小数点保留两位"));
      } else {
        callback();
      }
    },
    validateFigure(rule, value, callback) {
      const reg =  /^-?\d+(\.\d{1,2})?$/;
      if (value === "" || value === undefined) {
        callback();
      } else if (!reg.test(value)) {
        // callback(new Error("请输入正确的值"));
        callback(new Error("请输入正确的数值且小数点保留两位"));
      } else {
        callback();
      }
    },
    validateFloat(rule, value, callback) {
      const reg =
        /^([+-]?(?:\d+(\.\d{1,2})?)|([+-]?(?:[0-5]?\d|6)(?:\.\d)?|9(?:\.0)?))%?$/;
      if (value === "" || value === undefined) {
        callback();
      } else if (!reg.test(value)) {
        callback(new Error("请输入正确的值"));
      } else {
        callback();
      }
    },
    //获取字典
    async getListUnderlingDict(type) {
      const res = await listUnderlingDict(type);
      const resMap = res.data.map((item) => ({
        value: item.dictValue,
        label: item.dictLabel,
      }));
      const dictType = this.dictObjMap[type];
      this.dictObj[dictType] = resMap;
    },
    filterNode(value, data) {
      if (!value) return true;
      return data.name.indexOf(value) !== -1;
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.currentNode = data;
      this.termList = [];
      this.term = '';
      this.termValue = '';//值
      this.termValueMap = {};
      this.selectedList = [];
      this.isIndeterminateObj = {};
      this.termValueAll = false
      this.isIndeterminate = false
      this.listDevice()
    },
    focusHandle({ index, type, title, classifyType }) {
      this.classifyType = classifyType;
      this.selectType = type;
      this.checkedIndex = index;
      this.dialogOpen = true;
      this.dialogTitle = title;
      this.getTree()
      // this.listDevice()
    },
    /** 确定选择内容 */
    confirmSelect() {
      if (!this.selectedList.length) return this.$message.warning("特征项选择不完整");
      console.log(this.selectedList,'this.selectedList---------0');
      this.dialogOpen = false;
      const { code, name, measureUnit } = this.currentNode
      if(this.selectType === 'machine'){
        this.selectedList.forEach(item => {
          this.firstForm.agreementMachineShifts.push({
            equipmentName:name,
            equipmentNameCode:code,
            specification:item.value,
            unitMeasurement:measureUnit? measureUnit : '-'
          })
        })
      }else if(this.selectType === 'equipment'){
        this.selectedList.forEach(item => {
          this.firstForm.agreementEquipmentSupplies.push({
            equipmentName:name,
            equipmentNameCode:code,
            specification:item.value,
            unitMeasurement:measureUnit? measureUnit : '-'
          })
        })
      }else{
        this.selectedList.forEach(item => {
          this.firstForm.agreementMaterialSupplies.push({
            materialName:name,
            materialNameCode:code,
            specification:item.value,
            unitMeasurement:measureUnit? measureUnit : '-'
          })
        })
      }
    },
    /** 查询树 */
    async getTree() {
     let res = null
     try{
      if(this.classifyType === 1){
        res = await listDeviceClass()
      }else{
        res = await listMaterialsClass()
      }
      this.deptOptions = res.data;
      this.deptOptionsChildren = this.getMaxDepth(this.deptOptions)
     }catch(err){
      console.log(err);
     }
    },
    getMaxDepth(nodes) {
        let maxDepth = 1;

        function traverse(nodes, depth) {
            nodes.forEach(node => {
                if (node.children && node.children.length > 0) {
                    traverse(node.children, depth + 1);
                }
            });
            maxDepth = Math.max(maxDepth, depth);
        }

        traverse(nodes, 1);
        return maxDepth;
    },
    async listDevice(){
      let res = null
      try{
        if(this.classifyType === 1){
          // res = await listDevice(this.queryParams)
          res = await deviceFeatureList(this.currentNode.id)
        }else{
          res = await listMaterialsFeature(this.currentNode.id)
        }
        this.termList = res.data
        this.termList.forEach(item => {
          this.$set(this.termValueMap,item.id, [])
          this.$set(this.isIndeterminateObj,item.id, {})
        })
        console.log(res,'列表');
      }catch(err){
        console.log(err);
      }
    },
    dialogClosed(){
      this.deviceName = "";
      this.term = '';
      this.termList = [];
      this.termValue = '';//值
      this.termValueMap = {};
      this.selectedList = [];
      this.currentNode = {};
      this.isIndeterminateObj = {};
      this.termValueAll = false;
      this.isIndeterminate = false
    },
    //选择项
    async selectTerm(id){
      this.term = id;

      const isHas = this.termValueMap[id] && this.termValueMap[id].length;
      if(!isHas){
        let res = null
        if(this.selectType === 'machine' || this.selectType === 'equipment'){
          res = await deviceFeatureValueList(this.term)
        }else{
          res = await listMaterialsFeatureValue(this.term)
        }
        this.$set(this.termValueMap, id, res.data.map(item => ({...item,selected:false})))
      }
      const isAll = this.termValueMap[id].every(item => item.selected);
      const count = this.termValueMap[this.term].filter(item => item.selected).length;
      this.isIndeterminate = count > 0 && !isAll
      console.log(this.termValueMap[id],'isAll~~~~~~~~~~~~~');
      this.termValueAll = isAll
    },
    changeTermValue(){
      console.log(this.termValueMap,'-this.termValueMap');
      const allHaveTrue = Object.values(this.termValueMap).every(array =>
        array.some(item => item.selected === true)
      );
      this.termValueAll = this.termValueMap[this.term].every(item => item.selected)
      const count = this.termValueMap[this.term].filter(item => item.selected).length;
      this.isIndeterminate = count > 0 && !this.termValueAll
      this.isIndeterminateObj[this.term] = {}
      this.isIndeterminateObj[this.term].isHas = count > 0 && !this.termValueAll
      this.isIndeterminateObj[this.term].isAll = this.termValueAll
      console.log(allHaveTrue,'allHaveTrue');
      if(allHaveTrue){
        // 提取所有符合selected的值
        let filteredValues = Object.values(this.termValueMap).map(arr =>
          arr.filter(item => item.selected).map(item => ({id:item.id,featureValueName:item.featureValueName}))
        );
        console.log(filteredValues,'filteredValues-------------------');
        // 生成结果
        this.selectedList = filteredValues.length > 1?
        this.generateCombinations(filteredValues)
        :
        filteredValues[0].map(cur => ({
          id: `${cur.id}`,
          value: `${cur.featureValueName}`
        }));
        console.log(this.selectedList,'---点单个');
      }else{
        this.selectedList = [];
      }
    },
    //组织已选数据
    generateCombinations(filteredValues) {
      let arr = [];

      function helper(currentIndex, currentIds, currentNames) {
        if (currentIndex === filteredValues.length) {
          arr.push({
            id: currentIds.join('-'),
            value: currentNames.join('-')
          });
          return;
        }

        for (let item of filteredValues[currentIndex]) {
          helper(
            currentIndex + 1,
            [...currentIds, item.id],
            [...currentNames, item.featureValueName]
          );
        }
      }

      // 从第0个子数组开始
      helper(0, [], []);
      console.log(arr,'arr-------------------');
      return arr;
    },
    //删除已选
    deleteItem(i){
      this.selectedList.splice(i,1);
      console.log(this.selectedList,'this.selectedList---this.selectedList---this.selectedList');
      const ids = this.selectedList.flatMap(item => item.id.split('-')).filter((value, index, self) => self.indexOf(value) === index);
      // 处理 obj 中的每个项
      Object.keys(this.termValueMap).forEach(key => {
        console.log(this.termValueMap[key],'this.selectedList[key]---~~~');
        this.termValueMap[key].forEach(item => {
          // 将 id 转换为字符串并检查是否在 arr 中
          item.selected = ids.includes(item.id.toString());
        });
      });
      Object.keys(this.termValueMap).forEach(key => {
        this.termValueMap[key].forEach(item => {
          // 将 id 转换为字符串并检查是否在 arr 中
          console.log(ids,'ids---ids---ids');
          console.log(item.id,'item.id---item.id---item.id');
          item.selected = ids.includes(item.id.toString());
        });
        this.termValueAll = this.termValueMap[key].every(item => item.selected)
        const count = this.termValueMap[key].filter(item => item.selected).length;
        this.isIndeterminate = count > 0 && !this.termValueAll
        this.isIndeterminateObj[key] = {}
        this.isIndeterminateObj[key].isHas = count > 0 && !this.termValueAll
        this.isIndeterminateObj[key].isAll = this.termValueAll
      });
      console.log(ids,'----')
      console.log(this.termValueMap,'termValueMap[term]--termValueMap[term]');
    },
    expandLevel(level) {
      this.expandedKeys = [];  // 重置展开的键
      const arr = JSON.parse(JSON.stringify(this.deptOptions));  // 复制原始数据
      let result = [];

      function traverse(nodes, currentLevel) {
        if (currentLevel === level) {
          nodes.forEach(node => result.push(node.id));  // 收集当前级别节点的ID
        } else if (currentLevel < level) {
          nodes.forEach(node => {
            if (node.children) {
              result.push(node.id);  // 收集上级节点的ID以保证它们展开
              traverse(node.children, currentLevel + 1);  // 递归遍历下一层级
            }
          });
        }
      }
      traverse(arr, 1);  // 从第一层开始遍历
      const treeRef = this.$refs.tree // 组件实例
      const nodes = treeRef.store.nodesMap // 节点映射
      const orgExpand = false // 需要重置的状态
      for (let node in nodes) {
        // 跳过本来是这个状态的节点，不做则性能不好，会很卡
        if (nodes[node].expanded === orgExpand) {
          continue
        }
        nodes[node].expanded = orgExpand
      }
      this.expandedKeys = result;  // 设置要展开的节点ID
    },
    termValueAllChange(value){
      this.termValueAll = value
      this.isIndeterminateObj[this.term].isAll = value
      this.isIndeterminateObj[this.term].isHas = false
      this.isIndeterminate = false
      console.log(value,'value~~~~');
      this.termValueMap[this.term].forEach(item => {
        item.selected = value;
      });
      console.log(this.termValueMap,'-this.termValueMap');
      const allHaveTrue = Object.values(this.termValueMap).every(array =>
        array.some(item => item.selected === true)
      );
      console.log(allHaveTrue,'allHaveTrue');
      if(allHaveTrue){
        // 提取所有符合selected的值
        let filteredValues = Object.values(this.termValueMap).map(arr =>
          arr.filter(item => item.selected).map(item => ({id:item.id,featureValueName:item.featureValueName}))
        );
        console.log(filteredValues,'filteredValues-------------------');
        // 生成结果
        this.selectedList = filteredValues.length > 1?
        this.generateCombinations(filteredValues)
        :
        filteredValues[0].map(cur => ({
          id: `${cur.id}`,
          value: `${cur.featureValueName}`
        }));
        console.log(this.selectedList,'~~~~~this.selectedList~~~');
      }else{
        this.selectedList = [];
      }
    },
    countDecimalPlaces(num) {
      // 将数字转换为字符串
      const numStr = num.toString();
      // 查找小数点的位置
      const decimalIndex = numStr.indexOf('.');
      // 如果小数点存在，计算小数位数
      if (decimalIndex !== -1) {
          return numStr.length - decimalIndex - 1;
      }
      // 如果没有小数点，返回 0
      return 0;
    },
    computedpaymentSAmount(scope){
      if(scope.row.paymentBasis === "1") return
      const { multiply, bignumber, divide } = this.mathjs;
      const totalAmountIncTax = this.totalAmountIncTax
      const ratio = scope.row.paymentRatio
      if(totalAmountIncTax && ratio){
        const totalAmountIncTaxBig = bignumber(totalAmountIncTax);
        const ratioBig = bignumber(ratio);
        // 计算税率百分比
        const taxRatePercent = divide(ratioBig, 100);
        const paymentAmount = multiply(totalAmountIncTaxBig, taxRatePercent)
        this.$set(scope.row, 'paymentAmount', paymentAmount.toFixed(2))
      } else {
        this.$set(scope.row, 'paymentAmount', 0)
      }
    },
    changeSettlementStage(scope, value){
      console.log('%c 结算阶段监听changeSettlementStage ', `font-size: 20px;background-color: #f00;`, scope, value);
    },
    changePaymentBasis(scope, value){
      console.log(scope,'scope');
      if(value === "1"){
        this.$set(scope.row, 'paymentAmount', "")
      }else{
        this.computedpaymentSAmount(scope)
      }
    },
    computedDepositAmount(scope){
      const { depositBaseAmount, paymentAmount, depositRatio  } = scope.row
      const { multiply, bignumber, divide } = this.mathjs;
      if(depositBaseAmount === '1'){
        const totalAmountIncTax = this.totalAmountIncTax
        if(totalAmountIncTax && depositRatio){
          const totalAmountIncTaxBig = bignumber(totalAmountIncTax);
          const depositRatioBig = bignumber(depositRatio);
          // 计算税率百分比
          const taxRatePercent = divide(depositRatioBig, 100);
          const depositAmount = multiply(totalAmountIncTaxBig, taxRatePercent)
          this.$set(scope.row, 'depositAmount', depositAmount.toFixed(2))
        }
      }else if(depositBaseAmount === '3'){
        if(paymentAmount && depositRatio){
          const paymentAmountBig = bignumber(paymentAmount);
          const depositRatioBig = bignumber(depositRatio);
          // 计算税率百分比
          const taxRatePercent = divide(depositRatioBig, 100);
          const depositAmount = multiply(paymentAmountBig, taxRatePercent)
          this.$set(scope.row, 'depositAmount', depositAmount.toFixed(2))
        }
      }
    },
    changeDepositBaseAmount(scope){
      this.$set(scope.row, 'paymentAmount', "")
      this.$set(scope.row, 'depositRatio', "")
      this.$set(scope.row, 'depositAmount', "")
    },
    async handelCalibrationApproval() {
      try {
        this.calibrateVisible = true;
        this.calibrateLoading = true;
        let params = {
          businessId: this.purchaserId,
          processId: this.exampleId,
        };
        let res = null;
        if (this.purchaserId && this.exampleId) {
          res = await getLoadTaskDef(params);
        }else{
          /* 未提交时查看流程执行流程，根据登录人id 获取流程分组 */
          res = await getOrgByUserId(this.$store.state.user.id);
          params = {
            processKey: "jiantou-zhaocai:"+res.data+":ZHAOCAI_AGREEMENT_SIGN",
            businessId: 8888888888,
          };
          res = await getLoadTaskDef(params);
        }
        this.processInformationList = res.data;
        function getActive(nodes) {
          let allFalse = true;
          for (let i = 0; i < nodes.length; i++) {
            if (!nodes[i].completed) {
              if (i === 0) {
                return 0;
              } else {
                return i;
              }
            }
            allFalse = false;
          }
          return nodes.length;
        }
        this.calibrateActive = getActive(this.processInformationList);

        if (this.purchaserId && this.exampleId) {
          const response = await getProcessLogList(params);
          this.approveArr = response.data;
        }
      } catch (error) {}
      this.calibrateLoading = false;
    },
  },
  watch: {
    // 根据名称筛选树
    deviceName(val) {
      this.$refs.tree.filter(val);
    },
    "$route.query.id": {
      handler(newVal) {
        if (newVal && this.$route.query.type=='add') {
          getAgreementCreateInfoYl(newVal).then((res) => {
            this.getContractTypeList();

           // * 此3个字段是必传字段
           this.firstForm.agreement.schemeId = res.data.schemeId;
            this.firstForm.agreement.contractSplitId = res.data.splitId;
            this.firstForm.agreement.vendorId = res.data.vendorId;
            this.firstForm.agreement.partyAOrgId = res.data.partyAOrgId;
            this.firstForm.agreement.agreementName =this.$route.query.agreementName;
              this.firstForm.agreement.marketMaterialContractId = res.data.marketMaterialContractId
            this.agreementFileUrl = res.data.agreementFileUrl;
            this.typeContract= this.$route.query.type;
            this.agreementFileName = res.data.agreementFileName;
            this.firstForm.agreement.agreementNameYl =
              this.$route.query.agreementName;

            this.totalAmountIncTax = res.data.totalAmountIncTax;

            /* 供应商基本信息 */
            this.firstForm.vendorVO = res.data.vendorVO;
            /* 甲方纳税人识别号 */
            this.firstForm.taxpayerNo = res.data.taxpayerNo;

            // 合同类型
            this.contractType = 1;
            (this.priceType = res.data.priceType);
            console.log("合同类型===>", this.contractType);

            // * 以下字段是需要展示的字段
            const agreementNeedShowList = [
              "agreementPerformAddress",
              "agreementPerformCountry",
              "agreementPerformDistrict",
              "belongAccountingItem",
              "belongAccountingItemCode",
              "belongOrganizationName",
              "belongOrganizationId",
              "partyAName",
              "partyBLegalIdCard",
              "partyBLegalName",
              "partyBLegalPhone",
              "partyBName",
              "subjectMatterName",
              "expenditureBusinessType","partyBResponsibleIdCard","partyBResponsibleName","partyBResponsiblePhone"
            ];
            agreementNeedShowList.forEach((item) => {
              this.$set(this.firstForm.agreement, item, res?.data[item]);
            });
            const agreementPaymentItemShowList = [
              "totalAmountIncTaxText","totalAmountIncTax",
              "totalAmountExcTaxText",
            ];
            agreementPaymentItemShowList.forEach((item) => {
              this.$set(
                this.firstForm.agreementPaymentItem,
                item,
                res?.data[item]
              );
            });
            this.firstForm.agreementMaterialsLists = JSON.parse(
              JSON.stringify(res?.data["biddingListQuotation"])
            );


            console.log('%c🪴 this.firstForm \n', `font-size: 14px;background-color: #f00;`, this.firstForm );

            console.log('%c👽 this.dictObj.con_role_type ', `font-size: 20px;background-color: #f00;`, this.dictObj.con_role_type);
            /* 填充字典值，和默认甲乙方 this.dictObj.con_role_type */
            const updatedLists = this.firstForm.agreementPartyInfoLists.map(item => {
              if (item.roleType === '1') {
                return { ...item,
                  signerName: this.firstForm.agreement.partyAName,
                  roleTypeText: '合同甲方',
                  /* 甲方纳税人识别号 */
                  signerTaxpayerNumber: this.firstForm.taxpayerNo,
                };
              } else if (item.roleType === '2') {
                return { ...item,
                  signerName: this.firstForm.agreement.partyBName,
                  roleTypeText: '合同乙方',
                  /* 乙方纳税人识别号 */
                  signerTaxpayerNumber: this.firstForm.vendorVO.socialCreditCode,
                };
              }
              return item;
            });
            this.$set(this.firstForm, 'agreementPartyInfoLists', updatedLists);


          });

          /* 获取账号列表 */
          this.listAccountBank().then(res =>{
            /* 获取默认账号 */
            const firstBank = res.find(bank => bank.status == 1);
            if(firstBank){
              /* 设置给供应商赋值 */
              this.queryParams.roleType = 2;
              this.rowClickBank({row: firstBank});
            }
          })
        }
      },
       immediate: true,
    },
    "$route.query.schemeId": {
      handler(newVal) {
        if (newVal) {
          console.log(newVal, "$route.query.schemeId");
          const { schemeId, splitId, vendorId } = this.$route.query;
          console.log(schemeId, "schemeId------");
          const list = JSON.parse(window.sessionStorage.getItem("contract")) || {};
          getAgreementCreateInfo({
            schemeId,
            splitId,
            vendorId,
            agreementMaterialsList:list.agreementMaterialsList
          }).then((res) => {
            // * 此3个字段是必传字段
            this.typeContract= '';
            this.firstForm.agreement.schemeId = res.data.schemeId;
            this.firstForm.agreement.contractSplitId = res.data.splitId;
            this.firstForm.agreement.vendorId = res.data.vendorId;
            this.firstForm.agreement.partyAOrgId = res.data.partyAOrgId;
            this.firstForm.agreement.agreementName =this.$route.query.schemeName;
            this.agreementFileUrl = res.data.agreementFileUrl;
            this.agreementFileName = res.data.agreementFileName;
            this.attachmentId = res.data.attachmentId;
            console.log("agreementFileUrl",res.data.agreementFileUrl);
            console.log("agreementFileName",res.data.agreementFileName);
            this.firstForm.agreement.attachmentId = res.data.attachmentId;
            console.log("getAgreementCreateInfo获取的attachmentId:",this.firstForm.agreement.attachmentId);
            this.totalAmountIncTax = res.data.totalAmountIncTax;

            /* 供应商基本信息 */
            this.firstForm.vendorVO = res.data.vendorVO;
            /* 甲方纳税人识别号 */
            this.firstForm.taxpayerNo = res.data.taxpayerNo;

            // 合同类型
            this.contractType = res.data.businessType;
            (this.priceType = res.data.priceType);
            console.log("合同类型===>", this.contractType);

            // * 以下字段是需要展示的字段
            const agreementNeedShowList = [
              "agreementPerformAddress",
              "agreementPerformCountry",
              "agreementPerformDistrict",
              "belongAccountingItem",
              "belongAccountingItemCode",
              "belongOrganizationName",
              "belongOrganizationId",
              "partyAName",
              "partyBLegalIdCard",
              "partyBLegalName",
              "partyBLegalPhone",
              "partyBName",
              "subjectMatterName",
              "expenditureBusinessType",
            ];
            agreementNeedShowList.forEach((item) => {
              this.$set(this.firstForm.agreement, item, res?.data[item]);
            });
            const agreementPaymentItemShowList = [
              "totalAmountIncTaxText",
              "totalAmountExcTaxText",
            ];
            agreementPaymentItemShowList.forEach((item) => {
              this.$set(
                this.firstForm.agreementPaymentItem,
                item,
                res?.data[item]
              );
            });
            this.firstForm.agreementMaterialsLists = JSON.parse(
              JSON.stringify(res?.data["biddingListQuotation"])
            );

            // 获取合同附件的文档中台编辑URL
            this.editFileUrl=""; //先清空文档编辑URL
            this.getAttachmentEditURL();

            console.log('%c🪴 this.firstForm \n', `font-size: 14px;background-color: #f00;`, this.firstForm );


            console.log('%c👽 this.dictObj.con_role_type ', `font-size: 20px;background-color: #f00;`, this.dictObj.con_role_type);
            /* 填充字典值，和默认甲乙方 this.dictObj.con_role_type */
            const updatedLists = this.firstForm.agreementPartyInfoLists.map(item => {
              if (item.roleType === '1') {
                return { ...item,
                  signerName: this.firstForm.agreement.partyAName,
                  roleTypeText: '合同甲方',
                  /* 甲方纳税人识别号 */
                  signerTaxpayerNumber: this.firstForm.taxpayerNo,
                };
              } else if (item.roleType === '2') {
                return { ...item,
                  signerName: this.firstForm.agreement.partyBName,
                  roleTypeText: '合同乙方',
                  /* 乙方纳税人识别号 */
                  signerTaxpayerNumber: this.firstForm.vendorVO.socialCreditCode,
                };
              }
              return item;
            });
            this.$set(this.firstForm, 'agreementPartyInfoLists', updatedLists);

          });


          /* 获取账号列表 */
          this.listAccountBank().then(res =>{
            /* 获取默认账号 */
            const firstBank = res.find(bank => bank.status == 1);
            if(firstBank){
              /* 设置给供应商赋值 */
              this.queryParams.roleType = 2;
              this.rowClickBank({row: firstBank});
            }
          })

        }
      },
      immediate: true, // 立即执行一次监听器
    },
  },
  computed: {
    unitPriceExclTaxComputed() {
      return (taxUnitPrice, taxRate) => {
        if ( !taxUnitPrice  || !taxRate ) return "0.00";

        const { add, divide, bignumber, format } = this.mathjs;

        const taxUnitPriceBig = bignumber(taxUnitPrice);
        const taxRateBig = bignumber(taxRate);

        // 计算税率百分比
        const taxRatePercent = divide(taxRateBig, 100);

        console.log(taxRatePercent.toString(), "taxRatePercent");

        // 计算 (1 + 税率百分比)
        const onePlusTaxRate = add(1, taxRatePercent);

        // 计算不含税价格
        const result = divide(taxUnitPriceBig, onePlusTaxRate);

        let formattedResult;
        let newRes = taxUnitPrice.toString().replace(/\.?0+$/, '')
        if(this.countDecimalPlaces(newRes) <= 2){
            formattedResult = format(result, {
              notation: "fixed",
              precision: 2,
            })
          }else if(this.countDecimalPlaces(newRes) >= 3){
            let resultLength = this.countDecimalPlaces(result.toString())
            if(resultLength === 3){
              formattedResult = format(result, {
                notation: "fixed",
                precision: 3,
              })
            }else{
              formattedResult = format(result, {
                notation: "fixed",
                precision: 4,
              })
            }
          }

        return formattedResult;
      };
    },
    countComputed() {
      return (taxUnitPrice, taxRate, count, type) => {
        if ( !taxUnitPrice  || !taxRate || !count ) return "0.00";

        function isNumber(str) {
            const numberPattern = /^-?\d+(\.\d+)?$/;
            return numberPattern.test(str);
        }

        if (!isNumber(taxUnitPrice) || !isNumber(count) || !isNumber(taxRate)) return "0.00";

        const { add, divide, multiply, bignumber, format, subtract } = this.mathjs;

        const taxUnitPriceBig = bignumber(taxUnitPrice);
        const taxRateBig = bignumber(taxRate);
        const countBig = bignumber(count);

        // 计算税率百分比
        const taxRatePercent = divide(taxRateBig, 100);

        // 计算 (1 + 税率百分比)
        const onePlusTaxRate = add(1, taxRatePercent);

        // 计算不含税价格
        const notTaxedPrice = divide(taxUnitPriceBig, onePlusTaxRate);

        // 计算含税总价
        const taxedTotal = (Math.floor(multiply(taxUnitPriceBig, countBig) * 100) / 100).toFixed(2);

        //计算不合税总价

        const notTaxedTotal = (Math.floor(divide(taxedTotal, onePlusTaxRate) * 100) / 100).toFixed(2);

        //计算税额
        const taxAmount = subtract(taxedTotal, notTaxedTotal).toFixed(2);

        // 根据 type 选择不同的结果
        let result;

        switch (type) {
          case 'taxIncludedTotal': //含税总价
            result = taxedTotal
            break;
          case 'excludingTaxTotal': //不含税总价
            result = notTaxedTotal;
            break;
            case 'excludingTax': //不含税单价
            result = notTaxedPrice;
            break;
          default:
            result = taxAmount; //税额
            break;
        }

        let formattedResult;

        let newRes = taxUnitPrice.toString().replace(/\.?0+$/, '')

        if(type === 'excludingTax'){
          if(this.countDecimalPlaces(newRes) <= 2){
            formattedResult = format(result, {
              notation: "fixed",
              precision: 2,
            })
          }else if(this.countDecimalPlaces(newRes) >= 3){
            let resultLength = this.countDecimalPlaces(result.toString())
            if(resultLength === 3){
              formattedResult = format(result, {
                notation: "fixed",
                precision: 3,
              })
            }else{
              formattedResult = format(result, {
                notation: "fixed",
                precision: 4,
              })
            }
          }
        }else{
          formattedResult = result.toString().slice(0, result.toString().indexOf('.') + 3)
        }


        return formattedResult;
      };
    },
    durationComputed(){
      return () => {
        const { entryDate, finishDate } = this.firstForm.agreement
        // 定义开始日期和结束日期
        const startDate = new Date(entryDate);
        const endDate = new Date(finishDate);
        // 计算两个日期之间的时间差
        const timeDifference = endDate - startDate;
        // 计算差异的天数
        const daysDifference = Math.floor(timeDifference / (1000 * 60 * 60 * 24));
        return daysDifference + 1 || ''
      }
    }
  }
};
</script>
<style lang="scss" scoped>
::v-deep .el-dialog .el-dialog__body {
  margin: 0 auto !important;
  height: 72vh;
  overflow: auto;
}
::v-deep .cursor_pointer .el-input__inner{
  cursor: pointer!important;
}
.engineering_visa {
  margin-bottom: 20px;

  .item_title {
    display: flex;
    justify-content: space-between;
    align-items: center;
    font-size: 12px;
    font-weight: 600;
    margin-left: 12px;
    margin-bottom: 12px;
  }
}

.common-title {
  display: flex;
  align-items: center;
  font-weight: bold;
}

.tabs-box {
  position: relative;
  .tabs-box-right {
    position: absolute;
    top: 0;
    right: 0;
  }
}
.contract-box {
  width: 100%;
  height: calc(100vh - 120px);
  &.hide {
    position: absolute;
    top: -99999px;
    top: 0;
    opacity: 0;
    visibility: hidden;
  }
}
::v-deep .el-table .el-form-item {
  margin-bottom: 0;
}
.right-box{width: 100%;height: 100%; display: flex;
  .right-item{
      height: 100%;
      width: 50%;
      border-top: solid 1px #eee;
      border-right: solid 1px #eee;
      &.border{
        border: solid 1px #eee;
      }
      &.box{
        width: 25%;
        height: 100%;
        display: block;
      }
      .right-title{
        width: 100%;
        height: 35px;
        font-size: 14px;
        line-height: 35px;
        padding: 0 10px;
        border-bottom: solid 1px #eee;
      }
      .list-container{
        width: 100%;
        height: calc(100% - 35px);
        overflow-y: scroll;
        .list-item{
          color:#666 !important;
          cursor: pointer;
          width: 100%;
          padding:10px;
          border-bottom: solid 1px #eee;
          &.selected{
            background: #08c4a2;
            color: #fff !important;
          }
          &.space-between{
            width: 100%;
            display: flex;
            justify-content: space-between
          }
          .right-delete{
            width: 30px;
            display: flex;
            justify-content: center;
            height: 100%;
          }
        }
      }
    }
}
.node-box{
  display: flex;
  list-style: none;
  margin: 0;
  padding: 0 0 10px 0;
  li{
    list-style: none;
    background-color: #e8e8ef;
    border-radius: 50%;
    width: 18px;
    height: 18px;
    line-height: 18px;
    font-size: 12px;
    text-align: center;
    margin-right: 10px;
    color: #999;
    cursor: pointer;
    &:hover{
      background: #2b4acb;
      color: #fff;
    }
  }
}
.disabled-checkbox {
  pointer-events: none;
}
::v-deep .el-textarea__inner{
  min-height: 40px !important;
  padding-top: 10px;
}
</style>
