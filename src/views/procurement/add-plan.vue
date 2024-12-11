<template>
  <div class="app-container">
    <BackButton path="/procurement/plan" :title="currentContract.type === 'update'? '修改采购计划' : '新增采购计划'">
      <div>
          <el-button type="primary" plain size="mini" :disabled="isSubmit" @click="$router.replace('/procurement/plan')">取消</el-button>
          <el-button type="primary" size="mini" @click="submitForm('form')" :disabled="isSubmit" :loading="isSubmit">{{ isSubmit? '提交中...' : '保存' }}</el-button>
        </div>
    </BackButton>
    <div class="context">
      <el-form :model="formData" ref="form" :rules="rules" label-position="right" label-width="110px" size="small"
               @submit.native.prevent>
        <PageTitle title="基本信息"/>
        <div class="form-body">
          <el-row :gutter="40">
            <el-col :span="8" class="grid-cell">
              <el-form-item label="项目编号" prop="projectCode" class="required label-right-align">
                <el-input type="text" clearable :readonly="true" disabled v-model="formData.projectCode" />
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item label=" 项目名称" prop="projectName" class="required label-right-align">
                <el-input v-model="formData.projectName" disabled type="text" clearable />
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item label="编号" prop="procurementPlanCode" class="required label-right-align">
                <el-input type="text" clearable :readonly="true" disabled v-model="formData.procurementPlanCode"
                          placeholder="系统自动为您生成" />
              </el-form-item>
            </el-col>
          </el-row>
          <el-row :gutter="40">
            <el-col :span="8" class="grid-cell">
              <el-form-item label=" 采购名称" prop="procurementPlanName" class="required label-right-align">
                <el-input v-model="formData.procurementPlanName" type="text" clearable :disabled="isSubmit"/>
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item label="  采购层级" prop="projectHierarchy" class="required label-right-align">
                <el-input v-model="formData.projectHierarchy" type="text" disabled clearable />
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item label="采购人" prop="procurementOfficerName" class="required label-right-align">
                <el-input v-model="formData.procurementOfficerName" readonly @focus="handleClick" size="large" placeholder="请选择">
                  <template slot="suffix"><i class="el-input__icon el-icon-arrow-down"></i></template>
                </el-input>
<!--                <el-select v-model="formData.procurementOfficerName" placeholder="请选择" filterable @change="changeOperator" :disabled="isSubmit" style="width: 100%">-->
<!--                  <el-option-->
<!--                    v-for="item in operatorList"-->
<!--                    :key="item.userId"-->
<!--                    :label="item.nickName"-->
<!--                    :value="item.userId">-->
<!--                  </el-option>-->
<!--                </el-select>-->
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="40">
            <el-col :span="8" class="grid-cell">
              <el-form-item label="开始时间" prop="beginDate" class="required label-right-align">
                <el-date-picker v-model="formData.beginDate" type="date" style="width:100%" placeholder="选择日期" :picker-options="expireTimeOption" format="yyyy年MM月dd日" value-format="yyyy-MM-dd" :disabled="isSubmit"/>
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item label="完成时间" prop="endDate" class="required label-right-align">
                <el-date-picker v-model="formData.endDate" type="date" style="width:100%" placeholder="选择日期" :picker-options="expireTimeOverOttion" format="yyyy年MM月dd日" value-format="yyyy-MM-dd" :disabled="isSubmit"/>
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item label="进场时间" prop="arrivalDate" class="required label-right-align">
                <el-date-picker v-model="formData.arrivalDate" type="date" style="width:100%" placeholder="选择日期" :picker-options="expireTimeOption" format="yyyy年MM月dd日" value-format="yyyy-MM-dd" :disabled="isSubmit"/>
              </el-form-item>
            </el-col>
          </el-row>

          <el-row :gutter="40">
            <el-col :span="8" class="grid-cell">
              <el-form-item label="交易标的物" prop="subjectMatterText" class="required label-right-align">
                <el-input type="text" clearable :readonly="true" disabled v-model="formData.subjectMatterText" />
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item label="上限价(元)" prop="upperLimitPrice" class="required label-right-align">
                <el-input type="text" clearable :readonly="true" disabled v-model="formData.upperLimitPrice" />
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell" v-if="(procurementType == 1)">
              <el-form-item label="指导价" prop="upperLimitPrice" class="required label-right-align">
                <el-input type="text" clearable :readonly="true" disabled v-model="formData.guidance_price" placeholder="对接易料市集"/>
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item label=" 付款方式" prop="paymentType" class="required label-right-align"  v-if="(procurementType == 1)">
              <el-select style="width: 100%" v-model="formData.paymentType" placeholder="请选择付款方式" clearable>
                <el-option v-for="dict in dict.type.procurement_payment_type" :key="dict.value" :label="dict.label"
                  :value="dict.value">
                </el-option>
              </el-select>
            </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item label="计数方式" prop="countingType" v-if="(procurementType == 1)">
                <el-select style="width: 100%" v-model="formData.countingType" placeholder="请选择计数方式" clearable>
                  <el-option v-for="dict in dict.type.procurement_counting_type" :key="dict.value" :label="dict.label"
                    :value="dict.value">
                  </el-option>
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell" v-if="(procurementType == 1)">
              <el-form-item label="价格类型" prop="priceType" class="required label-right-align">
                <!-- 价格类型，1固定价，2浮动价，2固定、浮动价。清单的列根据这个监听来判断是否显示隐藏，反之也通过监听清单的价格类型@chang=changePriceType 来判断赋值该价格类型。 -->
                <el-select
                  v-model="formData.priceType"
                  placeholder="请选择价格类型"
                  clearable
                  style="width: 100%"
                >
                  <el-option
                    v-for="dict in PRICETYPELIST"
                    :key="dict.value"
                    :label="dict.label"
                    :value="dict.value"
                  />
                </el-select>
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell" v-if="(procurementType == 1)">
              <el-form-item label="区域" prop="region">
                <el-cascader
                  v-model="formData.region"
                  :options="regionOptions"
                  :props="{label:'divisionName',value:'divisionCode'}"
                  @change="handleChange"
                  style="width: 100%;"
                  >
                </el-cascader>
						  </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <!--       采购计划表单基价 （前端用来统一刷新列表清单的基价使用。） watch:formData.basePrice监听      -->
              <el-form-item label=" 基价" prop="basePrice" v-if="(isFloat)" class="label-right-align">
                <el-input ref="basePriceInput" v-model="formData.basePrice" clearable :disabled="isSubmit"/>
              </el-form-item>
            </el-col>
          </el-row>
        </div>

        <PageTitle :title="currentContract.contractPlanningCategoryName">
          <div class="page-title-right">
            <el-button  v-if="currentContract.contractPlanningCategory == 1 " type="success" size="small"  @click="pushPlan">易料市集采购</el-button>
            <el-button v-if="currentContract.contractPlanningCategory == 1 "  type="success" size="small"  @click="revokePushPlan">撤销易料市集采购</el-button>
            <el-button type="success" size="small" v-if="formData.isPushData!='Y'" :disabled="isSubmit" @click="splitVisible = true">合约拆分</el-button>
          </div>
        </PageTitle>

        <el-table v-loading="loading" :row-key="getRowKeys" :data="planList" ref="tableRef"  size="small"  border default-expand-all>
          <el-table-column type="expand" v-if="planList[0] && planList[0].children && planList[0].children.length">
            <template slot-scope="props">
              <el-table :data="props.row.children" size="small"    border>
                <!-- <el-table-column type="selection"></el-table-column> -->
                <el-table-column v-if="planList[0].children.length>1" label="拆分合约规划名称" prop="splitContractName" width="150">
                  <template slot-scope="scope">
                    <el-input v-model="scope.row.splitContractName" :disabled="isSubmit"/>
                  </template>
                </el-table-column>
                <el-table-column v-if="planList[0].children.length>1" label="拟签约合同承包范围" prop="contractScope" width="150">
                  <template slot-scope="scope">
                    <div  style="position: absolute;top: 5px;right: 40px;">
                      <el-button  type="danger" size="small"  @click="handleDelete(scope.$index)">删除标包</el-button>
                    </div>
                    <el-input v-model="scope.row.contractScope" :disabled="isSubmit"/>
                  </template>
                </el-table-column>
                <el-table-column label="清单" align="center" props="inventory">
                  <template slot-scope="inventory">
                    <el-table  size="small" :data="inventory.row.children"  border @select="handleSelect" :row-key="getRowKeys2"  :ref="inventory.row.planTable"  :row-class-name="tableRowClassName">
                      <el-table-column type="selection" width="55" :reserve-selection="true"/>
                      <el-table-column label="序号" type="index" width="50" align="center" fixed/>
                      <el-table-column label="清单编码" min-width="150" prop="materialsCode" fixed show-overflow-tooltip/>
                      <el-table-column label="清单名称" min-width="150" prop="materialsName" fixed show-overflow-tooltip/>
                      <el-table-column label="交易标的物" min-width="100" prop="subjectMatterName" show-overflow-tooltip>
                        <template slot-scope="scope">
                          <el-tooltip effect="dark" :content="scope.row.subjectMatterName || '-'" placement="top" v-if="scope.row.subjectMatterFlag == 1">
                            <el-input v-model="scope.row.subjectMatterName" :disabled="isSubmit" readonly @focus="matterFocus(scope.row.materialsId)"/>
                          </el-tooltip>
                          <span v-else>{{ scope.row.subjectMatterName }}</span>
                        </template>
                      </el-table-column>
                      <el-table-column label="规格型号" min-width="150" prop="specification" show-overflow-tooltip/>
                      <el-table-column label="计量单位" align="center" prop="unitMeasurement" />
                      <el-table-column label="价格类型" align="center" prop="priceType" width="200" v-if="procurementType === 1">
                        <template slot-scope="scope">
                          <el-select style="width: 100%" v-model="scope.row.priceType" placeholder="请选择" @change="changePriceType(inventory.$index,scope,$event)">
                            <el-option v-for="dict in PRICETYPEOPTIONS" :key="dict.value" :label="dict.label"
                              :value="dict.value">
                            </el-option>
                          </el-select>
                        </template>
                      </el-table-column>
                      <el-table-column label="租赁方式" align="center" prop="rentMode" v-if="currentContract.contractPlanningCategory == 2 || currentContract.contractPlanningCategory == 3" width="120">
                        <template slot-scope="scope">
                          <el-select style="width: 100%" v-model="scope.row.rentMode" placeholder="请选择" @change="changeRentMode(scope.row.materialsId,$event)">
                            <el-option v-for="dict in rentModeOptions" :key="dict.value" :label="dict.label"
                              :value="dict.value">
                            </el-option>
                          </el-select>
                        </template>
                      </el-table-column>
                      <el-table-column label="工作量" align="right" width="150" v-if="currentContract.contractPlanningCategory == 2 || currentContract.contractPlanningCategory == 3">
                        <template slot-scope="scope">
                          <el-input v-model="scope.row.count" :disabled="isSubmit" v-if="scope.row.rentMode == 3" @blur="changeCount(inventory.$index,scope,$event)" v-thousandth/>
                          <span v-else>{{ scope.row.count || 0 }}</span>
                        </template>
                      </el-table-column>
                      <el-table-column label="清单数量" align="right" width="150" v-else>
                        <template slot-scope="scope">
                          <el-input v-model="scope.row.count" :disabled="isSubmit || scope.row.belongOffer || scope.row.pushFlag === 'Y'" @blur="changeCount(inventory.$index,scope,$event)" v-thousandth/>
                        </template>
                      </el-table-column>
<!--                      基价由原来浮动价不可编辑，变成了可以编辑-->
                      <el-table-column label="基价" align="right" width="130" prop="basePrice"  v-if="procurementType === 1 && formData.priceType !== 1">
                        <template slot-scope="scope">
                          <span v-if="scope.row.priceType === 1">/</span>
                          <div v-else>
                            <el-input v-if="!scope.row.isbasePriceNotLegal"  v-model="scope.row.basePrice"  v-thousandth @blur="checkOtherPrice(scope.row,'basePrice',$event)"/>
                            <el-input v-else  v-model="scope.row.basePrice" :disabled="isSubmit" v-thousandth  @blur="checkOtherPrice(scope.row,'basePrice',$event)" class="checkInput"/>
                          </div>

                        </template>
                      </el-table-column>
                      <el-table-column label="单价(含税)" align="right" prop="unitPriceInclTax" width="180" v-if="formData.priceType !== 2">
                        <template slot-scope="scope">
                          <span v-if="scope.row.priceType !== 1">/</span>
                          <el-input v-else v-model="scope.row.unitPriceInclTax" :disabled="isSubmit || scope.row.belongOffer || scope.row.pushFlag === 'Y'" @blur="changePrice(scope.row,$event)" v-thousandth/>
                        </template>
                      </el-table-column>
                      <el-table-column label="税率(%)" align="right" prop="taxRate"/>
                      <el-table-column label="单价(不含税)" align="right" prop="unitPriceExclTax" width="150">
                        <template slot-scope="scope">
                          {{ scope.row.unitPriceExclTax }}
                        </template>
                      </el-table-column>
                      <el-table-column label="浮动价" align="right" width="130" prop="floatingPrice"  v-if="procurementType === 1 && formData.priceType !== 1">
                        <template slot-scope="scope">
                          <span v-if="scope.row.priceType === 1">/</span>
                          <div v-else>
                            <el-input v-if="!scope.row.isfloatingPriceNotLegal"  v-model="scope.row.floatingPrice" :disabled="isSubmit" v-thousandth  @blur="checkOtherPrice(scope.row,'floatingPrice',$event)"/>
                            <el-input v-else  v-model="scope.row.floatingPrice" :disabled="isSubmit" v-thousandth  @blur="checkOtherPrice(scope.row,'floatingPrice',$event)" class="checkInput"/>
                          </div>

                        </template>
                      </el-table-column>
                      <el-table-column label="装卸费" align="right" width="130" prop="unloadingFee"  v-if="procurementType === 1 && formData.priceType !== 1">
                        <template slot-scope="scope">
                          <span v-if="scope.row.priceType === 1">/</span>
                          <div v-else>
                            <el-input v-if="!scope.row.isunloadingFeeNotLegal"  v-model="scope.row.unloadingFee" :disabled="isSubmit" v-thousandth  @blur="checkOtherPrice(scope.row,'unloadingFee',$event)"/>
                            <el-input v-else  v-model="scope.row.unloadingFee" :disabled="isSubmit" v-thousandth  @blur="checkOtherPrice(scope.row,'unloadingFee',$event)" class="checkInput"/>
                          </div>
                        </template>
                      </el-table-column>

                      <el-table-column width="120" label="租赁时间" align="right" prop="rentTime" v-if="currentContract.contractPlanningCategory == 2 || currentContract.contractPlanningCategory == 3">
                        <template slot-scope="scope">
                          <el-input v-if="scope.row.rentMode == 1 || scope.row.rentMode == 2" v-model="scope.row.rentTime" @blur="changeWorkload(inventory.$index,scope)" :disabled="isSubmit" v-thousandth/>
                          <span v-else>-</span>
                        </template>
                      </el-table-column>
                      <el-table-column width="120" label="租赁数量" align="right" prop="rentQuantity" v-if="currentContract.contractPlanningCategory == 2 || currentContract.contractPlanningCategory == 3">
                        <template slot-scope="scope">
                          <el-input v-if="scope.row.rentMode == 1 || scope.row.rentMode == 2" v-model="scope.row.rentQuantity" @blur="changeWorkload(inventory.$index,scope)" :disabled="isSubmit" v-thousandth/>
                          <span v-else>-</span>
                        </template>
                      </el-table-column>
                      <el-table-column
                      v-if="currentContract.contractPlanningCategory == 1"
                      label="易商品编码"
                      align="center"
                      min-width="150" prop="skuId" show-overflow-tooltip
                    >
                      <template slot-scope="scope">
                        <a class="link-type" @click="goDetail(scope.row.code)">
                          {{ scope.row.skuId }}
                        </a>
                      </template>
                    </el-table-column>
                  <el-table-column v-if="currentContract.contractPlanningCategory == 1" label="易料商品名称" prop="name" width="150">
                    <template slot-scope="scope">
                      {{ scope.row.name }}
                    </template>
                  </el-table-column>

                  <el-table-column v-if="currentContract.contractPlanningCategory == 1" label="易料品牌" min-width="120" prop="offerBrand" show-overflow-tooltip/>
                  <el-table-column v-if="currentContract.contractPlanningCategory == 1" label="易料初始报价"  width="150" prop="offerPrice" >
                    <!-- <template slot-scope="scope">
                      <el-input v-model="scope.row.offerPrice" disabled v-thousandth/>
                    </template> -->
                  </el-table-column>
                    </el-table>
                  </template>
                </el-table-column>



              </el-table>
            </template>
          </el-table-column>
          <el-table-column label="序号" type="index" width="50" align="center" />
          <el-table-column label="合约规划名称" min-width="300" prop="contractPlanningName" show-overflow-tooltip/>
          <el-table-column label="规划金额（含税）" align="right" prop="plannedAmountInclTaxText" />
          <el-table-column label="已发生规划金额（含税）" align="right" prop="incurredPlannedAmountText" />
          <el-table-column label="规划余量(元)" align="right" prop="planningBalanceText" />
          <el-table-column label="拟定招标方式" align="center" prop="biddingMethodName" />
          <el-table-column label="清单" align="center" class-name="small-padding fixed-width">
            <template slot-scope="scope">
              <el-button size="mini" type="text" icon="el-icon-view" @click="handelInventory(scope.row)">查看清单</el-button>
            </template>
          </el-table-column>

        </el-table>
        <pagination v-show="total > 0" :total="total" :page.sync="queryParams.pageNum" :limit.sync="queryParams.pageSize"
                    @pagination="getList" />

      </el-form>

      <!-- 选择采购经办人 -->
      <el-dialog title="采购人" :visible.sync="officerDialog" width="55%">
        <el-form
          :model="searchQuery"
          ref="planForm"
          label-position="left"
          size="small"
          inline
          @submit.native.prevent
        >
          <el-form-item
            label="用户"
            prop="pushRoleList"
            class="label-right-align"
            label-width="40px"
          >
            <el-input
              v-model="searchQuery.nickName"
              placeholder="请输入用户"
              clearable
              style="width: 150px"
              @keyup.enter.native="searchUser"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              icon="el-icon-search"
              size="small"
              style="width: 70px"
              @click="searchUser"
            >查询</el-button>
          </el-form-item>
        </el-form>
        <virtual-scroll
            :data="filteredOperatorList"
            :item-size="62"
            key-prop="virtualId"
            ref="virScroll"
            @change="(renderData) => virtualList = renderData">
        <el-table
          v-loading="officerLoading"
          :data="virtualList"
          stripe
          size="small"
          highlight-current-row
          border
          @selection-change="selectOfficer"
          @row-click="selectOfficer"
          :row-key="selSelectKey"
          max-height="400"
          ref="selectTable">
          <el-table-column width="30" align="center">
            <template slot-scope="scope">
              <el-radio
                v-model="selectedUserId"
                :label="scope.row.userId"
                @change="selectOfficer(scope.row)"
              />
            </template>
          </el-table-column>
          <el-table-column label="序号" prop="virtualId" width="60" align="center" />
          <el-table-column label="用户" prop="nickName" width="100" align="center"/>
          <el-table-column label="电话号码" prop="phonenumber" width="150" align="center"/>
          <el-table-column label="归属当前组织名称" prop="thridOrgName" show-overflow-tooltip align="center"/>
          <el-table-column label="归属管理组织名称" prop="orgDeptName" show-overflow-tooltip align="center"/>
        </el-table>
        </virtual-scroll>
        <div slot="footer" class="dialog-footer">
          <el-button
            @click="officerDialog = false"
            style="width: 100px"
            size="small"
          >取 消</el-button
          >
          <el-button
            type="primary"
            @click="submitOfficer"
            style="width: 100px"
            size="small"
          >确 定</el-button
          >
        </div>
      </el-dialog>

      <!-- 选择项目合约规划 -->
      <el-dialog title="清单" :visible.sync="inventoryVisible" width="70%">
        <el-table v-loading="loading" :data="inventoryList" stripe border size="small">
          <el-table-column label="序号" type="index" width="50" align="center" />
          <el-table-column label="清单编码" min-width="100" prop="materialsCode" show-overflow-tooltip/>
          <el-table-column label="清单名称" min-width="200" prop="materialsName" show-overflow-tooltip/>
          <el-table-column label="规格型号" min-width="200" prop="specification" show-overflow-tooltip/>
          <el-table-column label="计量单位" align="center" prop="unitMeasurement" />
          <el-table-column v-if="currentContract.contractPlanningCategory != 1 && currentContract.contractPlanningCategory != 2" label="工程量" align="right" prop="quantityText" />
          <el-table-column label="已用数量" align="right" prop="usedCountText" />
          <el-table-column label="剩余量" align="right" prop="surplusQuantityText" />
          <el-table-column v-if="currentContract.contractPlanningCategory == 1 || currentContract.contractPlanningCategory == 2" label="转换数量" align="right" prop="transferQuantityText" />
          <!-- <el-table-column label="基准价" align="center" prop="basePrice" />
          <el-table-column label="浮动值" align="center" prop="floatingValue" /> -->
          <el-table-column label="税率(%)" align="right" prop="taxRateText" />
          <el-table-column label="单价(含税)" align="right" prop="unitPriceInclTaxText" min-width="150"/>
        </el-table>
      </el-dialog>

      <!-- 合约拆分弹出层 -->
      <el-dialog title="合约拆分" :visible.sync="splitVisible" class="dialogClass" width="30%" @closed="splitColsed">
        <el-form :model="splitForm" :rules="splitRules" ref="splitFormRef" @submit.native.prevent>
          <el-form-item label="拟拆分合同份数：" prop="num" label-width="140px">
            <el-input v-model.number="splitForm.num" :disabled="numDisable" autocomplete="off" :maxlength="2" clearable></el-input>
          </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button @click="splitVisible = false" style="width: 100px;" size="small">取 消</el-button>
          <el-button type="primary" @click="handleSplit" style="width: 100px;" size="small">确 定</el-button>
        </div>
      </el-dialog>

      <!-- 标的物选择 -->
      <el-dialog title="交易标的物选择" :visible.sync="matterVisible" class="dialogClass" width="30%" @closed="matterColsed">
        <el-input
          v-model="matterName"
          placeholder="请输入搜索内容"
          clearable
          size="small"
          prefix-icon="el-icon-search"
          style="margin-bottom: 12px"
        />
        <el-tree
          :data="matterList"
          show-checkbox
          node-key="serviceClassCode"
          :filter-node-method="filterNode"
          default-expand-all
          ref="tree"
          :props="defaultProps"
          check-strictly
          @check="handleCheckChange"
          >
        </el-tree>
        <div slot="footer" class="dialog-footer">
          <el-button @click="matterVisible = false" style="width: 100px;" size="small">取 消</el-button>
          <el-button type="primary" @click="handleMatter" style="width: 100px;" size="small">确 定</el-button>
        </div>
      </el-dialog>
    </div>
    <el-dialog
      title="应用商城"
      :visible.sync="dialogVisible"
      width="80%"
    >
      <iframe
        :src="yjtUrl"
        width="100%"
        height="500px"
        frameborder="0"
        allowfullscreen
      ></iframe>
    </el-dialog>
  </div>
</template>

<script>
import { Base64 } from 'js-base64';
import { create, all } from "mathjs"
import {
  getContractMaterials,
  saveProcurementPlan,
  getListProcurementOfficer,
  getMinProject,
  getPlanDetail,
  listDwMmServiceSubjectMatter,
  getContractPlanSplitFlag,pushMaterialProcurementList,revokePushMaterialProcurementList,getYjtUrl
} from '@/api/procurement/plan'
import { listUnderlingDict } from "@/api/procurement/contract";
import { listAreaDivisionTree } from '@/api/procurement/manage'
import BackButton from "@/components/BackButton/index.vue"
import { mapGetters } from "vuex"
import PageTitle from "@/components/PageTitle/index.vue"
import {PRICETYPELIST, PRICETYPEOPTIONS} from "@/utils/constants";
import VirtualScroll from 'el-table-virtual-scroll';
import {getTwoLevelDeptByDeptId} from "@/api/system/dept";
export default {
  name: "add-plan",
  dicts: ['plan_type','price_type','procurement_counting_type','procurement_payment_type'],
  data() {
    return this.getInitialData();
  },
  components:{
    BackButton,
    PageTitle,
    VirtualScroll
  },
  created() {
    this.getInitialData();
    console.log('param--param--param!------------------');
    this.mathjs = create(all);
    this.mathjs.config({
      number: 'BigNumber',
      // precision: 4
    })
    // this.getList();
    const param = JSON.parse(Base64.decode(this.$route.params.params))
    console.log(param,'param--param--param!!!!!!!!!!!!!!!!!!!!!!!');
    this.currentContract = param;
    console.log(JSON.stringify(this.currentContract),'获取到的params');
    this.isUpdate = param.type === 'update'? true : false;
    console.log(this.isUpdate,'isUpdate-isUpdate');
    this.formData.projectHierarchy = param.bidResponsibleOrgName;
    this.formData.upperLimitPrice = param.plannedAmountInclTaxText;
    this.formData.projectName = this.project.name;
    this.formData.projectCode = this.project.code;
    this.formData.projectId = this.project.id;
    this.getListUnderlingDict()
    this.getListProcurementOfficer()
    if(this.isUpdate){
      this.getPlanDetail()
    }else{
      this.getContractMaterials().then(()=>{
        // 获取合约规划清单后，默认合约拆分一份
        // this.$set(this.splitForm,"num",1);
        this.handleSplitInit();
        /* 同步将清单内所有的价格类型改成一致的（固定价） */
        this.updateMaterialsFloat(1);
      })
    }
    /* 获取省市区 */
    this.listAreaDivisionTree()
  },
  mounted(){
      this.queryContractPlanSplitFlag();
  },
  methods: {
    /* 代替data初始化 */
    getInitialData() {
      let checkNum = (rule, value, callback) => {
        if (!/^[1-9]\d*$/.test(value)) {
          callback(new Error('请输入正整数'));
        } else {
          callback();
        }
      }
      return {
        // 选择采购经办人
        officerDialog: false, // 控制对话框的显示隐藏
        officerLoading: false,
        filteredOperatorList: [], // 过滤后的用户列表
        virtualList: [],
        selectedUserId: null, // 选中的采购人ID
        selectedUser: {}, // 选中的用户信息
        searchQuery: {   // 搜索查询字符串
          nickName: '',
        },
        PRICETYPELIST:PRICETYPELIST,
        PRICETYPEOPTIONS:PRICETYPEOPTIONS,
        formData: {
          priceType:1
        }, //form表单数据
        planList: [],
        accountTable:'accountTable',
        projectCode:'',
        id:'',
        yjtUrl:'',
        isPushRevoke:null,//区分推送和撤销
        dialogVisible:false,
        materialsLists:[],
        inventoryList: [],
        // isEdit: true,
        rules: {
          procurementPlanName: [{
            required: true,
            message: '采购名称不可为空',
          }],
          beginDate: [{
            required: true,
            message: '开始时间不能为空',
          }],
          endDate: [{
            required: true,
            message: '完成时间不能为空',
          }],
          arrivalDate: [{
            required: true,
            message: '进场时间不能为空',
          }],
          procurementReporterName: [{
            required: true,
            message: '填报人不能为空',
          }],
          procurementOfficerName: [{
            required: true,
            message: '采购人不能为空',
          }],
          priceType: [{
            required: true,
            message: '采购价类型不能为空',
          }],
          paymentType: [{
            required: true,
            message: '付款方式不能为空',
          }],
          countingType: [{
            required: true,
            message: '计价方式不能为空',
          }],
          region: [{
            required: true,
            message: '区域不能为空',
          }],
        },
        // 遮罩层
        loading: false,
        // 显示搜索条件
        showSearch: true,
        // 总条数
        total: 0,
        // 查询参数
        queryParams: {
          pageNum: 1,
          pageSize: 10,
          procurementPlanCode: undefined,
          procurementPlanName: undefined,
          projectName: undefined,
          operator: undefined,
          procurementPlanType: 'all'
        },
        inventoryVisible: false,
        splitVisible: false, //是否显示拆分合同
        splitForm: {
          num: ''
        }, //拆分合同数表单
        splitRules: {
          num: [
            { required: true, message: '请输入拆分的份数', trigger: 'blur' },
            { validator: checkNum, trigger: 'blur' }
          ],
        },
        currentContract: {},
        isSubmit: false,
        // indexs:[],
        operatorList:[],
        isUpdate:false,
        expireTimeOption: {
          disabledDate(time) {
            return time.getTime() < Date.now() - 8.64e7; // 禁用小于当前日期的日期
          }
        },
        expireTimeOverOttion:{
          disabledDate(time) {
            // 获取今天的时间戳
            const today = new Date();
            today.setHours(0, 0, 0, 0); // 设置为当天的零点

            // 明天的时间戳
            const tomorrow = new Date(today);
            tomorrow.setDate(today.getDate() + 1);

            // 将传入的时间戳转为日期对象
            const date = new Date(time);

            // 只能选择明天及之后的日期
            return date <= today || date < tomorrow;
          }
        },
        regionOptions:[],
        /* 是否是浮动价类型，浮动价/固定、浮动价 都是true */
        isFloat:false,
        isSpecific: false,
        mathjs:null,
        rentModeOptions:[],
        /* 交易标的物（在浮动价更新后[已弃用！]该判断逻辑了） */
        subjectMatter:"",
        /* 采购类型 procurementType (浮动价逻辑使用了)
          PURCHASE_MATERIALS(1, "购买材料"),
          LEASED_MATERIAL(2, "租赁材料"),
          RENTAL_MACHINERY(3, "租赁机械（设备）"),
          SPECIALTY_SUBCONTRACT(4, "专业分包"),
          SERVICE_SUBCONTRACT(5, "劳务分包"),
          OTHER_TYPE(6, "其他"),
        */
        procurementType:"",
        matterVisible:false,
        matterList:[],
        defaultProps: {
          children: 'children',
          label: 'serviceClassName'
        },
        selectedMatter:{},
        matterCurrentId:'',
        matterName:undefined,
        initCountObj:{},
        numDisable:false,
      };
    },
    /** 选择采购人-点击行 */
    selectOfficer(val){
      this.selectedUser = val
      this.selectedUserId = val.userId;
    },
    selSelectKey(row){
      return row.virtualId
    },
    /** 选择采购人-点击确定 */
    submitOfficer(){
      if (this.selectedUser && this.selectedUser.userId) {
        this.$set(this.formData,'procurementOfficerName',this.selectedUser.nickName);
        this.$set(this.formData,'procurementOfficer',this.selectedUser.userId);
        this.officerDialog = false;
      } else {
        this.$message.warning('请选择一个采购人');
      }
    },
    /** 选择采购人-打开弹窗 */
    handleClick(){
      this.searchQuery = {
        nickName: '',
      };
      if(this.formData.procurementOfficer){
        this.selectedUserId = this.formData.procurementOfficer
        this.selectedUser = {nickName:this.formData.procurementOfficerName,userId:this.formData.procurementOfficer}
      }
      try{
        this.filteredOperatorList = this.operatorList.map((item, index) => ({
          ...item,
          virtualId: index+1
        }));
      }catch(err){
        console.log(err);
      }
      this.officerDialog = true;
    },
    /** 选择采购人-过滤用户 */
    searchUser() {
      this.officerLoading = true;
      const { nickName } = this.searchQuery;
      this.filteredOperatorList = this.operatorList
        .filter(item => !nickName || item.nickName.includes(nickName))
        .map((item, index) => ({
          ...item,
          virtualId: index + 1
        }));
      this.officerLoading = false;
    },
    getRowKeys2(row) {
      return row.planTable;
    },
    getRowKeys(row) {
      return row.contractPlanningId;
    },
    checkValidate(row,e,regexObj){
      const {regex,text} = regexObj
      if(regex.test(row.basePrice)){
        e.target.style = "border: 1px solid red;"
        this.$message.error(text);
        return true
      }
      return false
    },
    checkIsValidate(row,e) {
      const regexN1 = /^(?:[1-9]\d*|0)(\.\d+)?$/;
      const regexN2 = /^\d+(\.\d{0,4})?$/
      if(row.basePrice == ''){
        e.target.style = "border: 1px solid red;"
        this.$message.error("请输入基价");
        return
      }
      if(!this.checkValidate(row,e,{
        regex:regexN1,
        text:"请输入正确的基价",
      })){
        return;
      }
      if(!this.checkValidate(row,e,{
        regex:regexN2,
        text:"请输入小于4位的小数",
      })){
        return;
      }
      this.checkPriceWithCeilingPrice(row)
    },
    checkPriceWithCeilingPrice(row) {
      debugger
      const { add, subtract,divide,multiply, bignumber, format } = this.mathjs;
      const {basePrice,floatingPrice,unloadingFee} = row
      const basePriceText_bigDecimal = bignumber(Number(basePrice||0))
      const floatingPrice_bigDecimal = bignumber(Number(floatingPrice||0))
      const unloadingFee_bigDecimal = bignumber(Number(unloadingFee||0))
      // const aaa = format(add(basePriceText_bigDecimal,floatingPrice_bigDecimal))
      // console.log('%c 🚀 ~ file:add-plan --method:checkPriceWithCeilingPrice --line:622 --variable:===>', `font-size:16px; font-weight:bold; color:#fff; padding:4px; border-radius:4px; background:linear-gradient(90deg, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]}, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]});`,
      //   aaa);

      console.log('%c 🚀 ~ file:add-plan --method:checkPriceWithCeilingPrice --line:620 --variable:===>', `font-size:16px; font-weight:bold; color:#fff; padding:4px; border-radius:4px; background:linear-gradient(90deg, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]}, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]});`,
        basePriceText_bigDecimal,floatingPrice_bigDecimal,unloadingFee_bigDecimal);
      // const result = divide(multiply(format(multiply(add(add(basePriceText_bigDecimal,floatingPrice_bigDecimal),unloadingFee_bigDecimal), row.count)),100),100)
      const result = format(multiply(add(add(basePriceText_bigDecimal,floatingPrice_bigDecimal),unloadingFee_bigDecimal), row.count)).toString().replace(/([0-9]+.[0-9]{2})[0-9]*/,"$1")
      console.log('%c 🚀 ~ file:add-plan --method:checkPriceWithCeilingPrice --line:621 --variable:===>', `font-size:16px; font-weight:bold; color:#fff; padding:4px; border-radius:4px; background:linear-gradient(90deg, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]}, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]});`,
        result);
    },
    // 多选框选中数据
    handleSelectionChange(selection, row) {

    },
    handleSelect(selection, row){
      console.log("selection"+JSON.stringify(selection))
      console.log("row"+JSON.stringify(row))
    },
        /** 跳转方案详情 */
    async goDetail(code) {
      // this.dialogVisible=true
      // console.log(JSON.stringify(code))

        const res = await getYjtUrl(code);
        this.yjtUrl=res.data || ''
        window.open(this.yjtUrl)
        // console.log(JSON.stringify(res))
      },

    pushPlan(){
      //isPushRevoke为true表示推送
      this.isPushRevoke=true
      this.materialsLists=[]
      if(!this.planList[0].children) return this.$message({type:'error',message:"您还没有可选择的采购清单"});
        for(let i = 0 ; i <  this.planList[0].children.length ; i++){
          let children=this.planList[0].children[i]
          console.log(JSON.stringify(children)+"采购清单")
          const currentSelect = this.$refs[`${children.planTable}`].selection;
          if(this.isUpdate){ //修改
             this.materialsLists = [...this.materialsLists ,...currentSelect];
            }else{ //新增
              for(let j = 0 ; j <  currentSelect.length ; j++){
               currentSelect[j].isSelect='Y'
              }
            }
        }
        console.log(JSON.stringify(this.materialsLists.length))
      if(this.isUpdate && this.materialsLists.length==0) return this.$message({type:'error',message:"请选择易料市集采购清单"});
      this.$confirm("是否确定选中的清单进入易料市集进行采购？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(() => {
        this.submitFormPush('form');
      });
    },
    revokePushPlan(){
      //isPushRevoke为false表示撤销
      this.isPushRevoke=false
      if(!this.planList[0].children) return this.$message({type:'error',message:"您还没有可选择的采购清单"});
      this.materialsLists=[]
        for(let i = 0 ; i <  this.planList[0].children.length ; i++){
          let children=this.planList[0].children[i]
          const currentSelect = this.$refs[`${children.planTable}`].selection;
          console.log(currentSelect.length+"currentSelect"+JSON.stringify(currentSelect))
          this.materialsLists = [...this.materialsLists ,...currentSelect];
        }

      if(this.materialsLists.length<=0) return this.$message({type:'error',message:"请选择撤销易料市集采购清单"});
      this.$confirm("是否确定撤销选中的清单？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(() => {
        for(let i = 0 ; i <  this.materialsLists.length ; i++){
          console.log("撤销"+JSON.stringify(this.materialsLists[i]))
            if(this.materialsLists[i].pushFlag=='N'){
              return this.$message({type:'error',message:"请选择已推送进入易料市集采购清单"})
            }
          }
          console.log(JSON.stringify(this.materialsLists))
        this.submitFormPush('form');

      });
    },
    //撤销
    async revokePushMaterialProcurement(){


      let formData = {
        projectCode:this.projectCode,
          id:this.id,
          materialsLists:this.materialsLists,
        }
        await revokePushMaterialProcurementList(formData)
        this.getPlanDetail()
        this.materialsLists=[]
        //清空数据
        for(let i = 0 ; i <  this.planList[0].children.length ; i++){
          let children=this.planList[0].children[i]
          const currentSelect = this.$refs[`${children.planTable}`].selection;
          this.$refs[`${children.planTable}`].clearSelection();
        }
    },
    //推送
    async  pushMaterialProcurement(){
      let formData = {
        projectCode:this.projectCode,
          id:this.id,
          materialsLists:this.materialsLists,
        }
        await pushMaterialProcurementList(formData)
        //新增推送完成时给id赋值
        if(!this.isUpdate){
            this.currentContract.id=this.id
            }

        this.getPlanDetail()
        this.materialsLists=[]
     //清空数据
       for(let i = 0 ; i <  this.planList[0].children.length ; i++){
          let children=this.planList[0].children[i]
          const currentSelect = this.$refs[`${children.planTable}`].selection;
          this.$refs[`${children.planTable}`].clearSelection();
        }
    },
    tableRowClassName({row, rowIndex}) {
        if (row.pushFlag === 'Y') {
          return 'already-pushed';
        } else if (row.pushFlag === 'N') {
          return 'not-pushed';
        }
        return '';
      },

    //提交
    submitForm(formName) {
      console.log(this.planList,'ppp');
      const { add, subtract,divide,multiply, bignumber, format,floor } = this.mathjs;
      this.isSubmit = true;
      this.$refs[formName].validate(async (valid,done) => {
        if (valid) {
          // * 首先先判断类型为浮动价的单行是否存在数据不合法的情况
          let amount = 0
          if(this.planList.length>0) {
            for(let firstItem of this.planList) {
              for(let secondItem of firstItem.children) {
                for(let thirdItem of secondItem.children) {
                  if(thirdItem.priceType === 2) {
                    if(thirdItem.isbasePriceNotLegal || thirdItem.isfloatingPriceNotLegal || thirdItem.isunloadingFeeNotLegal) {
                      this.isSubmit = false;
                      return this.$message({
                        message: '请检查输入项是否输入正确',
                        type: 'error'
                      });
                    }
                    const {basePrice,floatingPrice,unloadingFee} = thirdItem
                    const basePriceText_bigDecimal = bignumber(Number(basePrice||0))
                    const floatingPrice_bigDecimal = bignumber(Number(floatingPrice||0))
                    const unloadingFee_bigDecimal = bignumber(Number(unloadingFee||0))
                    let itemAmount = format(multiply(add(add(basePriceText_bigDecimal,floatingPrice_bigDecimal),unloadingFee_bigDecimal), thirdItem.count))
                    itemAmount = (Number(itemAmount)+'').toString().replace(/([0-9]+.[0-9]{2})[0-9]*/,"$1")
                    console.log('%c 🚀 ~ file:add-plan --method: --line:795 --variable:===>itemAmount', `font-size:16px; font-weight:bold; color:#fff; padding:4px; border-radius:4px; background:linear-gradient(90deg, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]}, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]});`,
                      itemAmount);
                    amount = format(add(bignumber(amount),bignumber(Number(itemAmount))))
                    console.log('%c 🚀 ~ file:add-plan --method: --line:795 --variable:amount2===>', `font-size:16px; font-weight:bold; color:#fff; padding:4px; border-radius:4px; background:linear-gradient(90deg, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]}, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]});`,
                      amount);
                  // .toString().replace(/([0-9]+.[0-9]{2})[0-9]*/,"$1")
                  }else {
                    console.log('%c 🚀 ~ file:add-plan --method: --line:799 --variable:bignumber(thirdItem.count),bignumber(Number(thirdItem.unitPriceInclTax||0)===>', `font-size:16px; font-weight:bold; color:#fff; padding:4px; border-radius:4px; background:linear-gradient(90deg, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]}, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]});`,
                      amount,bignumber(thirdItem.count),bignumber(Number(thirdItem.unitPriceInclTax||0)),format(multiply(bignumber(thirdItem.count),bignumber(Number(thirdItem.unitPriceInclTax||0)))));
                    console.log('%c 🚀 ~ file:add-plan --method: --line:798 --variable:amount1===>', `font-size:16px; font-weight:bold; color:#fff; padding:4px; border-radius:4px; background:linear-gradient(90deg, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]}, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]});`,
                      amount);
                    console.log('%c 🚀 ~ file:add-plan --method: --line:803 --variable:===>', `font-size:16px; font-weight:bold; color:#fff; padding:4px; border-radius:4px; background:linear-gradient(90deg, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]}, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]});`,
                      format(add(bignumber(1.991719900414e+8),bignumber(99462))));
                    let itemAmount = format(multiply(bignumber(thirdItem.count),bignumber(Number(thirdItem.unitPriceInclTax||0))))
                    // * 保留两位小数（处理科学计数法截取位数有误的问题）
                    itemAmount = (Number(itemAmount)+'').toString().replace(/([0-9]+.[0-9]{2})[0-9]*/,"$1")
                    console.log('%c 🚀 ~ file:add-plan --method: --line:806 --variable:===>', `font-size:16px; font-weight:bold; color:#fff; padding:4px; border-radius:4px; background:linear-gradient(90deg, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]}, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]});`,
                      itemAmount);
                    amount = format(add(bignumber(amount), bignumber(Number(itemAmount))))
                    console.log('%c 🚀 ~ file:add-plan --method: --line:809 --variable:result1===>', `font-size:16px; font-weight:bold; color:#fff; padding:4px; border-radius:4px; background:linear-gradient(90deg, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]}, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]});`,
                      amount);
                  }
                }
              }
            }
            if(Number(amount)>Number(this.planList[0].planningBalance)) {
              return this.$message({
                message: '拆分合约清单中的总金额不能大于规划余量',
                type: 'error'
              });
            }
          }
          const { planList } = this
          console.log(planList,'planListplanList--planListplanList-planListplanList');
          if(!planList[0].children || !planList[0].children.length){
            this.isSubmit = false;
            this.$message({
              message: '拆分合约不能为空',
              type: 'error'
            });
            return false;
          }

          const isAll = planList[0]?.children.every(item => item.splitContractName && item.contractScope)
          //判断长度大于1
          if( planList[0].children.length>1){
          if(!isAll){
            this.isSubmit = false;
            if(planList[0].children.length!=1){
              this.$message({
              message: '拆分合约规划名称/拟签约合同承包范围不能为空',
              type: 'error'
            });
            }
            return false;
          }
        }
          // const isZero = planList[0]?.children.some(item => {
          //   let isLoop = true;
          //   if (isLoop) {
          //     const hasZeroCount = item.children.some(child => {
          //       if (Number(child.count) === 0) {
          //         this.$message({
          //           message: `拆分合规规划名称为${item.splitContractName}的清单中名称为“${child.materialsName}”的数量不能为0`,
          //           type: 'error'
          //         });
          //         return true; // 结束循环
          //       }
          //       return false;
          //     });
          //     return hasZeroCount; // 终止外层循环
          //   }
          //   return false;
          // });

          // if(isZero) {
          //   this.isSubmit = false;
          //   return false;
          // }

          const loading = this.$loading({
            lock: true,
            text: '数据提交中...',
            background: 'rgba(0, 0, 0, 0.7)'
          });
          const { procurementPlanName, beginDate, endDate, arrivalDate, procurementOfficer, procurementOfficerName,projectId,projectName,projectCode, priceType,basePrice, regionProvinceCode, regionCityCode, paymentType, countingType } = this.formData;
          const { contractPlanningCategory, biddingMethodCode, biddingMethodName, contractPlanningCategoryName, contractPlanningId, contractPlanningName, incurredPlannedAmount, incurredPlannedAmountText, plannedAmountInclTax, plannedAmountInclTaxText, planningBalance, planningBalanceText,bidResponsibleOrg, bidResponsibleOrgName, id, contractPlanningCode,brand } = this.currentContract
          const splitRequestList = this.planList[0]?.children.map(item => {
            return {
              splitContractName:item.splitContractName,
              contractScope:item.contractScope,
              materialsLists:item.children.map(child => {
                child.unitPriceInclTax = format(Number(child.unitPriceInclTax), { notation: 'fixed', precision: 4 }).toString().replace(/\.?0+$/, '') || '';
                child.count = format(Number(child.count), { notation: 'fixed', precision: 4 }).toString().replace(/\.?0+$/, '') || '';
                return child
              })
            }
          })
          const formData = {
            procurementPlan:{
              id: id || '',
              procurementPlanName,
              procurementPlanType:contractPlanningCategory,
              procurementType:biddingMethodCode,
              projectHierarchy:bidResponsibleOrgName,
              beginDate,
              endDate,
              arrivalDate,
              procurementOfficer,
              procurementOfficerName,
              //priceType:priceType !== 'undefined'?priceType:'',
              regionProvinceCode,
              /* 基价 （前端用来统一刷新列表清单的基价使用。） */
              basePrice,
              regionCityCode,
              paymentType:paymentType !== 'undefined'?paymentType:'',
              countingType:countingType !== 'undefined'?countingType:'',
            },
            splitRequestList,
            contractPlanning:{
              contractPlanningCategory, biddingMethodCode, biddingMethodName, contractPlanningCategoryName, contractPlanningId, contractPlanningName, incurredPlannedAmount, incurredPlannedAmountText, plannedAmountInclTax, plannedAmountInclTaxText, planningBalance, planningBalanceText,
              projectId,projectName,projectCode,bidResponsibleOrg,bidResponsibleOrgName,subjectMatter:this.subjectMatter,contractPlanningCode,brand
            }
          }
          console.log(formData,'this.formData');
          try{
            const res = await saveProcurementPlan(formData);
            loading.close();
            this.$message({
              message: '保存成功',
              type: 'success'
            });
            this.isSubmit = false;
            console.log(res,'r~~~~~~~~~~~~~~~~~');
            this.currentContract.id=res.data.id
            this.getPlanDetail()
            // this.$tab.closePage().then(() => {
              // 执行结束的逻辑
              // let param = Base64.encode(JSON.stringify(res.data))
              // this.$router.replace(`/procurement/plan-detail/${param}`);
            // })
          }catch(err){
            console.log(err);
            this.isSubmit = false;
            loading.close();
          }
        } else {
          this.isSubmit = false;
          return false;
        }
      });
    },

     //提交推送
     submitFormPush(formName) {
      console.log(this.planList,'ppp');
      const { format } = this.mathjs
      this.isSubmit = true;
      this.$refs[formName].validate(async (valid,done) => {
        if (valid) {
          const { planList } = this
          console.log(planList,'planListplanList--planListplanList-planListplanList');
          if(!planList[0].children || !planList[0].children.length){
            this.isSubmit = false;
            this.$message({
              message: '拆分合约不能为空',
              type: 'error'
            });
            return false;
          }

          const isAll = planList[0]?.children.every(item => item.splitContractName && item.contractScope)
          if(!isAll){
            this.isSubmit = false;
            this.$message({
              message: '拆分合约规划名称/拟签约合同承包范围不能为空',
              type: 'error'
            });
            return false;
          }

          // const isZero = planList[0]?.children.some(item => {
          //   let isLoop = true;
          //   if (isLoop) {
          //     const hasZeroCount = item.children.some(child => {
          //       if (Number(child.count) === 0) {
          //         this.$message({
          //           message: `拆分合规规划名称为${item.splitContractName}的清单中名称为“${child.materialsName}”的数量不能为0`,
          //           type: 'error'
          //         });
          //         return true; // 结束循环
          //       }
          //       return false;
          //     });
          //     return hasZeroCount; // 终止外层循环
          //   }
          //   return false;
          // });

          // if(isZero) {
          //   this.isSubmit = false;
          //   return false;
          // }

          const loading = this.$loading({
            lock: true,
            text: '数据提交中...',
            background: 'rgba(0, 0, 0, 0.7)'
          });
          const { procurementPlanName, beginDate, endDate, arrivalDate, procurementOfficer, procurementOfficerName,projectId,projectName,projectCode, priceType, regionProvinceCode, regionCityCode, paymentType, countingType } = this.formData;
          const { contractPlanningCategory, biddingMethodCode, biddingMethodName, contractPlanningCategoryName, contractPlanningId, contractPlanningName, incurredPlannedAmount, incurredPlannedAmountText, plannedAmountInclTax, plannedAmountInclTaxText, planningBalance, planningBalanceText,bidResponsibleOrg, bidResponsibleOrgName, id, contractPlanningCode,brand } = this.currentContract
          const splitRequestList = this.planList[0]?.children.map(item => {
            console.log(JSON.stringify(item))
            return {

              splitContractName:item.splitContractName,
              contractScope:item.contractScope,
              materialsLists:item.children.map(child => {
                child.unitPriceInclTax = format(Number(child.unitPriceInclTax), { notation: 'fixed', precision: 4 }).toString().replace(/\.?0+$/, '') || '';
                child.count = format(Number(child.count), { notation: 'fixed', precision: 4 }).toString().replace(/\.?0+$/, '') || '';
                child.isSelect=this.isUpdate?'':child.isSelect;
                return child
              })
            }
          })
          const formData = {
            procurementPlan:{
              id: id || '',
              procurementPlanName,
              procurementPlanType:contractPlanningCategory,
              procurementType:biddingMethodCode,
              projectHierarchy:bidResponsibleOrgName,
              beginDate,
              endDate,
              arrivalDate,
              procurementOfficer,
              procurementOfficerName,
              // priceType:priceType !== 'undefined'?priceType:'',
              regionProvinceCode,
              regionCityCode,
              paymentType:paymentType !== 'undefined'?paymentType:'',
              countingType:countingType !== 'undefined'?countingType:'',
            },
            splitRequestList,
            contractPlanning:{
              contractPlanningCategory, biddingMethodCode, biddingMethodName, contractPlanningCategoryName, contractPlanningId, contractPlanningName, incurredPlannedAmount, incurredPlannedAmountText, plannedAmountInclTax, plannedAmountInclTaxText, planningBalance, planningBalanceText,
              projectId,projectName,projectCode,bidResponsibleOrg,bidResponsibleOrgName,subjectMatter:this.subjectMatter,contractPlanningCode,brand
            }
          }
          console.log(formData,'this.formData');
          console.log("---"+JSON.stringify(this.materialsLists))
          try{
            const res = await saveProcurementPlan(formData);
            if(!this.isUpdate && res?.data?.materialsLists.length>0){
              console.log("-//--"+JSON.stringify(this.materialsLists))
              this.projectCode=res.data.projectCode
              this.id=res.data.id
              this.materialsLists=res.data.materialsLists
            }
console.log("-2222--"+JSON.stringify(this.materialsLists))
            loading.close();
            this.$message({
              message: '保存成功',
              type: 'success'
            });
            this.isSubmit = false;

            if(this.isPushRevoke){
              this.pushMaterialProcurement();
            }else{
              this.revokePushMaterialProcurement();
            }


            // console.log(res,'r~~~~~~~~~~~~~~~~~');
            // this.$tab.closePage().then(() => {
            //   // 执行结束的逻辑
            //   let param = Base64.encode(JSON.stringify(res.data))
            //   this.$router.replace(`/procurement/plan-detail/${param}`);
            // })
          }catch(err){
            console.log(err);
            this.isSubmit = false;
            loading.close();
          }
        } else {
          this.isSubmit = false;
          return false;
        }
      });
    },


    //获取物料
    async getContractMaterials(){
      const { currentContract, formData } = this
      this.planList.push(currentContract)
      const { contractPlanningId, contractPlanningCategory, contractPlanningCode } = this.currentContract
      console.log(this.currentContract,'this.currentContract!!!!!!!!!!');
      const res = await getContractMaterials(contractPlanningId, formData.projectId,contractPlanningCategory,contractPlanningCode,formData.projectName)
      console.log(res.data,'res.data~~~~~~~~~~~~~');
      this.inventoryList = res.data.contractMaterialsList;
      this.formData.subjectMatterText = res.data.subjectMatterText || '';
      this.formData.subjectMatterCode = res.data.subjectMatterCode || '';
      this.subjectMatter = res.data.subjectMatter || '';
      /* 采购方案类型(购买材料,劳务分包....) */
      this.procurementType = contractPlanningCategory || '';
      /** 根据分类判断是否可拆分编辑 */
      // if([1,2,3,6].includes(contractPlanningCategory)){
      //     this.isEdit = true;
      //     const children = []
      //     Array.from({ length: 1 }).forEach((_, index) => {
      //       children.push({
      //         index,
      //         // children: this.inventoryList.map(item => ({...item, count:10}))
      //         children: this.inventoryList
      //       })
      //     });
      //     this.$set(this.planList[0], 'children', JSON.parse(JSON.stringify(children)));
      // }else {
      //   this.isEdit = false;
      // }
    },
    //切换tab类型
    handleTypeClick(tab) {
      this.queryParams.procurementPlanType = tab.name;
    },
    handleDelete(index) {
      console.log("this.planList[0].children[index]"+JSON.stringify(this.planList[0].children[index]))
      let arrData=this.planList[0].children[index].materialsLists
      for(let i = 0 ; i <  arrData?.length ; i++){
            if(arrData[i]?.pushFlag=='Y'){
              return this.$message({type:'error',message:"目前是推送状态不可删除！"})
            }
          }
          this.$confirm("是否确定删除标包？", "提示", {
                confirmButtonText: "确定",
                cancelButtonText: "取消",
                type: "warning",
              }).then(() => {
                  // 删除数据
              this.planList[0].children.splice(index, 1);
              // 强制Vue重新渲染
              this.$forceUpdate();
              });
    },
    /** 查询定时任务列表 */
    getList() {
      this.loading = false;

    },
    /** 搜索按钮操作 */
    handleQuery() {
      console.log(this.queryParams, 'this.queryParams');
      this.queryParams.pageNum = 1;
      // this.getList();
    },
    /** 重置按钮操作 */
    resetQuery() {
      this.resetForm("queryForm");
      this.handleQuery();
    },
    //查看清单
    handelInventory(id) {
      this.inventoryVisible = true;
    },
    // 初始化拆分合同
    handleSplitInit(){
      // const { num } = this.splitForm;
      // if(Number(num) > 10) return this.$message.error('最多可拆分10份');
      const num = 1
      const children = []
      Array.from({ length: num }).forEach((_, index) => {
        children.push({
          index,
          planTable:'planTable'+index,
          children: this.inventoryList.map(item => ({
            ...item,
            count: index === 0 ? item.count : 0.00,
            rentTime: index === 0 ? item.rentTime : '',
            rentQuantity:index === 0 ? item.rentQuantity : '',

          }))
        })
      });
      this.$set(this.planList[0], 'children', JSON.parse(JSON.stringify(children)));
    },
    // 拆分合同
    handleSplit() {
      this.$refs['splitFormRef'].validate((valid) => {
        if (valid) {
          const { num } = this.splitForm;
          if(Number(num) > 10) return this.$message.error('最多可拆分10份');
          const children = []
          Array.from({ length: num }).forEach((_, index) => {
            // children.push({
            //   index,
            //   children: this.inventoryList.map(item => ({
            //     materialsCode: item.materialsCode,
            //     materialsName: item.materialsName,
            //     specification: item.specification,
            //     unitMeasurement: item.unitMeasurement,
            //     count: item.count,
            //     unitPriceInclTaxText:item.unitPriceInclTaxText
            //   })),
            // })
            console.log(this.planList[0].children.length)
            console.log(index)
            if(index+1>this.planList[0].children.length){
            children.push({
              index,
              planTable:'planTable'+index,
              children: this.inventoryList.map(item => ({
                ...item,
                priceType: item.priceType || 1,
                count: index === 0 ? item.count : 0.00,
                rentTime: index === 0 ? item.rentTime : '',
                rentQuantity:index === 0 ? item.rentQuantity : '',

              }))
            })
          }else{
            children.push(this.planList[0].children[index])
          }
          });
          this.$set(this.planList[0], 'children', JSON.parse(JSON.stringify(children)));
          this.splitVisible = false;
        } else {
          return false;
        }
      });
    },
    splitColsed() {
      this.$refs['splitFormRef'].resetFields();
    },
    submit(){
      console.log('sub');
    },
    // handleSelectionChange(selection) {
    //   this.indexs = selection.map(item => item.index);
    //   console.log(this.indexs, 'this.indexs');
    // },
    // delHandle(row,i,index){
    //   this.planList[0]?.children[index]?.children.splice(i,1);
    // },

    async getListProcurementOfficer(){
      try{
         // const projectRes = await getMinProject(this.formData.projectCode);
         const dept = await getTwoLevelDeptByDeptId(this.$store.state.user.userInfo.deptId);
        const res = await getListProcurementOfficer(dept.deptId)
        this.operatorList = res.data;
      }catch(err){
        console.log(err);
      }
    },
    changeOperator(val){
      this.formData.procurementOfficer = val;
      this.formData.procurementOfficerName = this.operatorList.find(item => item.userId === val).nickName
    },
    async getPlanDetail() {
      this.loading = true;
      const { id, procurementPlanType } = this.currentContract
      try {
        const res = await getPlanDetail(id);
        console.log(res, '详情!!!!!!!!!!!!!');
        const { procurementPlan, splitMaterials,contractPlanning } = res.data;
        procurementPlan.countingType = procurementPlan.countingType + ''
        procurementPlan.paymentType = procurementPlan.paymentType  + ''
        // procurementPlan.priceType = procurementPlan.priceType  + ''
        this.formData.subjectMatterText = procurementPlan.subjectMatterName;
        this.formData.region = [procurementPlan.regionProvinceCode, procurementPlan.regionCityCode]
        this.id=procurementPlan.id
        this.projectCode=procurementPlan.projectCode
        this.currentContract.contractPlanningCategory = contractPlanning.contractPlanningCategory
        this.subjectMatter = procurementPlan.subjectMatterType;
        this.formData = {...this.formData, ...procurementPlan, upperLimitPrice:contractPlanning.plannedAmountInclTaxText}
        console.log(this.formData, 'this.formData');
        // this.wfProcessId = procurementPlan.wfProcessId

        console.log(this.currentContract,'获取详情后的');
        // if([4,5].includes(contractPlanning.contractPlanningCategory)){
        //   console.log('可修改~~~~');
        //   this.isEdit = false
        // }


        splitMaterials.forEach((item,index) => {
          item.$index = index;
          item.planTable='planTable'+index
          let children = item.materialsLists.map((child,k) => ({...child, $index:k,planTable:item.planTable}))
          item.children = children;
        })
        console.log(JSON.stringify(splitMaterials),'splitMaterials--splitMaterials--splitMaterials')

        this.planList[0] = {...contractPlanning, children: splitMaterials};
        console.log(JSON.stringify(this.planList[0]),'this.planList[0]--this.planList[0]--this.planList[0]')
        // 在数据加载完成后手动展开所有行
        this.$nextTick(() => {
          this.expandAllRows();
        });
        console.log(this.planList[0],'this.planList[0]-this.planList[0]');
        // this.planList = splitMaterials.map((item,index) => {
        //   return {...contractPlanning, children:[{...item, $index:index, children: item.materialsLists.map((child,k) => ({...child, $index:k}))}]}
        // });
        Object.assign(this.currentContract, contractPlanning)

        const { projectId } = this.formData
        getContractMaterials(contractPlanning.contractPlanningId, projectId,contractPlanning.contractPlanningCategory,contractPlanning.contractPlanningCode).then(res => {
          this.inventoryList = res.data.contractMaterialsList;
        })
        console.log(this.planList, 'this.planList');
        /* 采购方案类型(购买材料,劳务分包....) */
        this.procurementType = procurementPlanType || '';
      } catch (err) {
        console.log(err);
      }
      this.loading = false;
    },
    expandAllRows(){
      this.planList.forEach(row => {
        this.$refs.tableRef.toggleRowExpansion(row, true);
      });
    },
    /* 每个清单的价格类型监听，用来给采购计划的价格类型赋值，如果清单出现多种价格类型，就给采购计划赋值为=3 固定、浮动价。 */
    changePriceType(splitIndex, scope, event) {
      // 初始化一个 Map 来存储各类型的数量
      const priceTypeCountMap = new Map();
      // 遍历数据结构，统计各 priceType 的数量
      this.planList[0]?.children.forEach(item => {
        item.children.forEach(subItem => {
          // 清单的价格类型
          const priceType = subItem.priceType;
          // 如果 Map 中还没有该 priceType，初始化数量为 0
          if (!priceTypeCountMap.has(priceType)) {
            priceTypeCountMap.set(priceType, 0);
          }
          // 增加该 priceType 的计数
          priceTypeCountMap.set(priceType, priceTypeCountMap.get(priceType) + 1);
        });
      });

      // 打印所有 priceType 的数量
      console.log('清单的Price Type Counts:', priceTypeCountMap);

      // 根据 priceType 的数量决定 formData.priceType 的值
      if (priceTypeCountMap.size > 1) {
        // 如果有多个不同的 priceType，设置为 3 固定、浮动价
        this.$set(this.formData, 'priceType', 3);
      } else if (priceTypeCountMap.size === 1) {
        // 如果只有一种 priceType，设置为该 priceType 可能是 1 固定价 ，可能是 2 浮动价
        this.$set(this.formData, 'priceType', [...priceTypeCountMap.keys()][0]);
      }
      // 采购计划表单的this.formData.priceType如果清单的priceType存在多种。就设置为3，只有一种就设置为那一种的priceType
    },
    //数量计算
    changeCount(splitIndex,scope,event){
      console.log('%c 🚀 ~ file:add-plan --method:changeCount --line:1135 --variable:splitIndex,scope,event===>', `font-size:16px; font-weight:bold; color:#fff; padding:4px; border-radius:4px; background:linear-gradient(90deg, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]}, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]});`,
        splitIndex,scope,event);
      console.log(event,'~~~~~~~~~~~~~~~~~~');
      const regexN1 = /^-?(?:[1-9]\d*|0)(\.\d+)?$/;
      const regexN2 = /^-?\d+(\.\d{0,4})?$/;

      // 判断为空则默认设置为0
      if (scope.row.count === '' || scope.row.count == null) {
        this.$set(scope.row, 'count', 0);
        scope.row.count = 0;  // 将其值设为0
      }

      if(scope.row.count === ''){
        event.target.style = "border: 1px solid red;"
        this.$message.error("请输入数量");
        return
      }else if(!regexN1.test(scope.row.count)  ){
        event.target.style = "border: 1px solid red;"
        this.$message.error("请输入正确的值");
        return
      }else if(!regexN2.test(scope.row.count)){
        event.target.style = "border: 1px solid red;"
        this.$message.error("请输入小于4位的小数");
        return
      }

      event.target.style = "border: 1px solid #C0C4CC;"
      const { add, subtract, bignumber, format } = this.mathjs;
      console.log(this.inventoryList,'this.inventoryList');
      const countMap = new Map();
      this.inventoryList.forEach(item => {
        countMap.set(item.materialsId, item.count);
        this.initCountObj[item.materialsId] = item.count
      })

      console.log(this.initCountObj,'initCountObj~~~~~~~~~~~~~~~~~~~~~~')

      let splitLength = this.planList[0]?.children.length; //拆分的份数
      let currentMaterialsId = scope.row.materialsId //当前物料id
      let count = bignumber(0); //总数量
      let zeroNumber = 0; //数量为空的个数

      //获取总数量
      this.planList[0]?.children.forEach(item => {
        item.children.forEach(subItem => {
          console.log('%c 🚀 ~ file:add-plan --method: --line:1207 --variable:进来了===>', `font-size:16px; font-weight:bold; color:#fff; padding:4px; border-radius:4px; background:linear-gradient(90deg, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]}, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]});`,
            );
          count = add(count, subItem.materialsId === currentMaterialsId? format(Number(subItem.count), { notation: 'fixed', precision: 4 }) : 0)
        })
      })

      let difference = subtract(countMap.get(currentMaterialsId) , count); //差值
      console.log(JSON.parse(JSON.stringify(difference)),'差值');

      //判断数量是否大于库存数量
      if(count > countMap.get(currentMaterialsId)){
        this.$message({
          type: 'error',
          message: `清单名称为"${scope.row.materialsName}"的数量超过库存数量`
        });
        return false;
      }

      //判断拆分份数是否大于1
      if(splitLength > 1){
        this.planList[0]?.children.forEach(item => {
          item.children.forEach(subItem => {
            if(subItem.materialsId === currentMaterialsId){
              if(subItem.count == ''){
                zeroNumber++;
              }
            }
          })
        })
      }

      // 数量为空的个数为1个时执行以下逻辑
      if(zeroNumber === 1){
        this.planList[0]?.children.forEach(item => {
          item.children.forEach(subItem => {
            if(subItem.materialsId === currentMaterialsId && (subItem.count == "" || subItem.count == 0 || subItem.count == 0.00)){
              console.log('go go go go go');
              const subItemCount = subtract(countMap.get(currentMaterialsId) , count)
              console.log(JSON.parse(JSON.stringify(subItemCount)),'subItemCount##############');
              if(this.countDecimalPlaces(subItemCount) < 2){
                subItem.count =  subItemCount.toFixed(2)
                console.log(subItem.count,typeof subItem.count, 'subItem.count=====================');
              }else if(this.countDecimalPlaces(subItemCount) > 4){
                subItem.count =  format(subItemCount, { notation: 'fixed', precision: 4 }).toString().replace(/\.?0+$/, '')
              }else{
                subItem.count = subItemCount.toString()
              }
            }
          })
        })
      }
    },
    checkOtherPrice(row,key,event) {
      // 如果为空，设置为0
      if (!row[key]) {
        this.$set(row, key, 0);
        row[key] = 0;
      }

      const regexN1 = /^-?(?:[1-9]\d*|0)(\.\d+)?$/;
      const regexN2 = /^-?\d+(\.\d{0,4})?$/;
      if(!regexN2.test(row[key])){
        event.target.style = "border: 1px solid red;"
        this.$message.error("请输入小于4位的小数");
        this.$set(row, `is${key}NotLegal`, true)
      }else {
        event.target.style = "border: 1px solid #C0C4CC;"
        this.$set(row, `is${key}NotLegal`, false)
      }

    },
    //不含税计算
    changePrice(row,event){
      console.log('%c 🚀 ~ file:add-plan --method:changePrice --line:1226 --variable:row,event===>', `font-size:16px; font-weight:bold; color:#fff; padding:4px; border-radius:4px; background:linear-gradient(90deg, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]}, ${["#ff005a", "#ff9900", "#33cc33", "#0099ff", "#ffc300"][Math.floor(Math.random() * 5)]});`,
        row,event);
      const { unitPriceInclTax, taxRate} = row

      const regexN1 = /^-?(?:[1-9]\d*|0)(\.\d+)?$/;
      const regexN2 = /^-?\d+(\.\d{0,4})?$/;

       if(unitPriceInclTax == ''){
          event.target.style = "border: 1px solid red;"
          this.$message.error("请输入数量");
          return
        }else if(!regexN1.test(unitPriceInclTax)  ){
          event.target.style = "border: 1px solid red;"
          this.$message.error("请输入正确的值");
          return
        }else if(!regexN2.test(unitPriceInclTax)){
          event.target.style = "border: 1px solid red;"
          this.$message.error("请输入小于4位的小数");
          return
        }

        event.target.style = "border: 1px solid #C0C4CC;"

        const { add, divide, bignumber, format } = this.mathjs;

        const taxUnitPriceBig = bignumber(unitPriceInclTax);
        const taxRateBig = bignumber(taxRate);

        // 计算税率百分比
        const taxRatePercent = divide(taxRateBig, 100);

        console.log(taxRatePercent.toString(), "taxRatePercent");

        // 计算 (1 + 税率百分比)
        const onePlusTaxRate = add(1, taxRatePercent);

        // 计算不含税价格
        const result = divide(taxUnitPriceBig, onePlusTaxRate);

        let formattedResult;
        console.log(unitPriceInclTax,'unitPriceInclTax---------------');
        let newRes = unitPriceInclTax.toString().replace(/\.?0+$/, '')
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

          row.unitPriceExclTax = this.formatNumberWithThousandsSeparator(formattedResult)
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
    //工作量计算
    changeWorkload(splitIndex,scope){
      const { multiply, round } = this.mathjs;
      console.log(splitIndex,'splitIndex----');
      console.log(scope,'scope+++++++');
      if(scope.row.rentTime && scope.row.rentQuantity){
        // this.planList[0].children[splitIndex].children[scope.$index].workload = multiply(scope.row.rentTime, scope.row.rentQuantity);
        this.$set(this.planList[0].children[splitIndex].children[scope.$index],'count', round(multiply(scope.row.rentTime, scope.row.rentQuantity)), 2);
      }
      console.log(this.planList[0].children[splitIndex].children[scope.$index],'this.planList[0].children[splitIndex].children[scope.$index]-this.planList[0].children[splitIndex].children[scope.$index]');
    },
    //获取省市区
    async listAreaDivisionTree(){
      try{
        const res = await listAreaDivisionTree()
        this.regionOptions = res.data.map( item => ({
          divisionName:item.divisionName,
          divisionCode:item.divisionCode,
          children:item.children.map( subItem => ({
            divisionName:subItem.divisionName,
            divisionCode:subItem.divisionCode,
          }))
        }))
        console.log(res,'省市树');
      }catch(err){
        console.log(err);
      }
    },
    handleChange(value) {
      this.formData.regionProvinceCode = value[0]
      this.formData.regionCityCode = value[1]
    },
    //材料、机械租凭方式字典获取
    async getListUnderlingDict(){
      const res = await listUnderlingDict('RENT_MODE')
      const resMap = res.data.map(item => ({value:item.dictValue,label:item.dictLabel}))
      this.rentModeOptions = resMap
    },
    async matterFocus(id){
      this.matterVisible = true;
      this.matterCurrentId = id;
      try{
        const res = await listDwMmServiceSubjectMatter()
        this.matterList = this.formatData(res.data);
        console.log(res,'标的物');
        console.log(this.matterList,'标的物');
      }catch(err){
        console.log(err);
      }
    },
    handleCheckChange(node, checked) {
      console.log(node,'node');
      if(checked){
        this.selectedMatter = {
          serviceClassCode:node.serviceClassCode,
          serviceClassName:node.serviceClassName
        }
        this.$refs.tree.setCheckedNodes([node]);
      }
    },
    formatData(params){
      let data = params
      data.map(item => {
        if(item.children && item.children.length > 0){
          item.disabled = true;
          this.formatData(item.children)
        }else{
          item.checked = true;
        }
      })
      return data;
    },
    handleMatter(){
      const list = JSON.parse(JSON.stringify(this.planList));
      list.forEach(item => {
        item.children.forEach(subItem => {
          subItem.children.forEach(subSubItem => {
            console.log(subSubItem,'subSubItem---subSubItem');
            if(subSubItem.materialsId === this.matterCurrentId){
              subSubItem.subjectMatterName = this.selectedMatter.serviceClassName
              subSubItem.subjectMatterCode = this.selectedMatter.serviceClassCode
            }
          })
        })
      })
      this.planList = list;
      this.matterVisible = false;
      console.log(this.planList,'this.planList--this.planList');
    },
    filterNode(value, data) {
      if (!value) return true;
      return data.serviceClassName.indexOf(value) !== -1;
    },
    matterColsed(){

    },
    //千分位
    formatNumberWithThousandsSeparator(number) {
      // 将数字转为字符串
      let [integerPart, decimalPart] = number.toString().split('.');

      // 使用正则表达式在整数部分插入千分位分隔符
      let formattedIntegerPart = integerPart.replace(/\B(?=(\d{3})+(?!\d))/g, ',');

      // 如果有小数部分，将其附加到格式化后的整数部分
      return decimalPart !== undefined ? `${formattedIntegerPart}.${decimalPart}` : formattedIntegerPart;
    },
    async queryContractPlanSplitFlag(){
      const res = await getContractPlanSplitFlag();
      if (res.data === '0') {
        this.splitForm.num = 1;
        this.numDisable = true;
      }
    },
    //选择租赁方式
    changeRentMode(materialsId, value){
      const length = this.planList[0].children.length;
      if(length < 2) return
      this.planList[0].children.forEach(item => {
        item.children.forEach(subItem => {
      if(subItem.materialsId === materialsId){
        // subItem.isMarket = value
        this.$set(subItem, 'rentMode', value)
      }
        })
      })
    },
    //操作是否走易料
    changeIsMarKet(materialsId, value){
      const length = this.planList[0].children.length;
      if(length < 2) return
      this.planList[0].children.forEach((item,index) => {
        item.children.forEach(subItem => {
          if(subItem.materialsId === materialsId){
            this.$set(subItem, 'belongOffer', value)
            if(value === true){
              this.$set(subItem, 'count', index === 0? (this.initCountObj[materialsId]? this.initCountObj[materialsId] : subItem.count) : 0)
              this.$set(subItem, 'unitPriceInclTax', subItem.offerPrice? subItem.offerPrice : subItem.unitPriceInclTax)
            }
          }
        })
      })
      console.log(this.planList[0],'0000000000000000000000000000');
    },
    /* 同步将清单内所有的价格类型改成一致的 */
    updateMaterialsFloat(val){
      this.planList.forEach((item) => {
        if (item.children && Array.isArray(item.children)) {
          item.children.forEach((itemChildren) => {
            if (itemChildren.children && Array.isArray(itemChildren.children)) {
              itemChildren.children.forEach((children) => {
                this.$set(children, 'priceType', val);
              });
            }
          });
        }
      });
    }
  },
  computed: {
    ...mapGetters(['project']),
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
        console.log(taxUnitPrice,'taxUnitPrice---------------');
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

        return this.formatNumberWithThousandsSeparator(formattedResult)
      };
    },
  },
  watch:{
    matterName(val) {
      this.$refs.tree.filter(val);
    },
    project:{
      handler(newVal,oldVal){
        if(oldVal === undefined || newVal.id !== oldVal.id){
          this.$router.replace('/procurement/plan')
        }
      }
    },
    /* 表单价格类型监听，同步清单价格类型 */
    "formData.priceType":{
      handler(val){
        if(Number(val) === 1 || Number(val) === 2){
          /* 同步将清单内所有的价格类型改成一致的 */
          this.updateMaterialsFloat(val);
        }
        /* 浮动价显示基价选项 */
        if(Number(val) === 2 || Number(val) === 3){
          this.isFloat = true;
          // 判断 'floatingPrice' 和 'unloadingFee' 浮动价，装卸费 是否为空，如果为空则设置为0
          this.planList.forEach((item) => {
            if (item.children && Array.isArray(item.children)) {
              item.children.forEach((itemChildren) => {
                if (itemChildren.children && Array.isArray(itemChildren.children)) {
                  itemChildren.children.forEach((children) => {
                    // 判断 'floatingPrice' 和 'unloadingFee' 浮动价，装卸费 是否为空，如果为空则设置为0
                    if (!children.floatingPrice) {
                      this.$set(children, 'floatingPrice', 0);
                    }
                    if (!children.unloadingFee) {
                      this.$set(children, 'unloadingFee', 0);
                    }
                  });
                }
              });
            }
          });

        }else {
          this.isFloat = false;
        }
      },
      immediate: true
    },
    /* 表单基价监听 */
    "formData.basePrice":{
      handler(val){
        /* 浮动价 -> 基价 */
        if(this.isFloat){
          const regexN2 = /^\d+(\.\d{0,4})?$/
          const inputEl = this.$refs.basePriceInput.$el.querySelector("input");
          if (!regexN2.test(val)) {
            /* 更新非法状态 */
            inputEl.style = "border: 1px solid red;"
            this.$set(this.planList, `isBasePriceNotLegal`, true);
            this.$message.error("基价格式不正确，请输入小于4位的小数");
          }else {
            /* 合法时清除错误状态 */
            inputEl.style = "border: 1px solid #C0C4CC;"
            this.$set(this.planList, `isBasePriceNotLegal`, false);

            /* 同步将清单内所有的基价改成一致的 */
            this.planList.forEach((item) => {
              if (item.children && Array.isArray(item.children)) {
                item.children.forEach((itemChildren) => {
                  if (itemChildren.children && Array.isArray(itemChildren.children)) {
                    itemChildren.children.forEach((children) => {
                      this.$set(children, 'basePrice', val);
                    });
                  }
                });
              }
            });
          }
        }
      },
      immediate: true
    }
  }
};
</script>
<style lang="scss" scoped>
::v-deep.checkInput {
  .el-input__inner {
    border: 1px solid #ff0000
  }
}
.page-title {
  width: 100%;
  border-bottom: solid 1px #ccc;
  padding: 10px;
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;

  &::before {
    content: "";
    height: 20px;
    width: 5px;
    background-color: rgba(41, 65, 137, 1);
    position: absolute;
    left: 0;
    top: 50%;
    transform: translateY(-50%);
  }
}

.form-body {
  padding: 20px;
}
::v-deep.app-container .dialogClass .el-dialog__body {
  height: initial;
}
::v-deep .el-table__row .already-pushed {
  background: #CCCCCC !important;
}

::v-deep .el-table__body tr:hover > td.el-table__cell {
  background: initial !important;
}
::v-deep  .el-table__row   .not-pushed {
  background: #F9F9FB !important;
}

</style>
