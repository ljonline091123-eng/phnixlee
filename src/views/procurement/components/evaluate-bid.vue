<template>
  <div class="app-container">
    <el-dialog
      title="设置二次报价截止时间"
      :visible.sync="dialogVisible"
      width="25%">
      <el-form ref="timeForm" :model="timeForm" label-width="160px">
        <el-row>
          <el-col :span="24">
            <el-form-item
              label="第二次报价截止时间"
              prop="twiceTime"
              class="required label-right-align"
              :rules="[{ required: true, message: '请选择第二次报价截止时间' }]"
            >
              <el-date-picker
                v-model="timeForm.twiceTime"
                type="datetime"
                style="width: 100%"
                placeholder="选择日期"
                value-format="yyyy-MM-dd HH:mm:ss"
                :picker-options="expireTimeOption"
                class="date_picker"
              />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button @click="dialogVisible = false">取 消</el-button>
        <el-button type="primary" @click="twiceBidConf">确 定</el-button>
      </span>
    </el-dialog>
    <div class="context" style="height: calc(100vh - 116px)">
      <div class="btn-box">
        <!-- 开标状态 -->
        <template v-if="activeName === 'open'">
          <el-button
            type="success"
            icon="el-icon-plus"
            size="small"
            @click="setOpenWorker"
            :disabled="
              !(
                noticeDetail.purchaseOfficer &&
                noticeDetail.tenderNotice &&
                noticeDetail.tenderNotice.noticeStatus === 2 &&
                noticeDetail.tenderNotice.isOpenPeople !== 1
              )
            "
            >设置开标人员</el-button
          >
          <el-button
            type="primary"
            size="small"
            @click="confirmSumbmit"
            :disabled="
              !(
                noticeDetail.purchaseOfficer &&
                noticeDetail.tenderNotice &&
                noticeDetail.tenderNotice.noticeStatus === 2 &&
                noticeDetail.tenderNotice.isOpenPeople !== 1
              )
            "
            >提交</el-button
          >
        </template>
        <!-- 评标状态 -->
        <template v-if="activeName === 'evaluate'">
          <div style="position:absolute;right: 0;top: -36px;font-size: 13px;color:#ff0000" v-if="noticeDetail.tenderNotice && noticeDetail.tenderNotice.twiceQuotVersion>1 && noticeDetail.tenderNotice.twiceQuotState === 1">
            二次报价截止时间：{{timeDifferenceElement}}
          </div>
          <el-button
            type="success"
            icon="el-icon-plus"
            size="small"
            :disabled="
              !evaluateStateList.length &&
              noticeDetail.tenderNotice &&
              noticeDetail.tenderNotice.noticeStatus === 3 &&
              noticeDetail.purchaseOfficer
                ? false
                : true
            "
            @click="setExpert"
            >设置评标专家</el-button
          >
          <el-button
            type="primary"
            size="small"
            @click="startEvaluat"
            :disabled="
              evaluateStateList.length &&
              noticeDetail.tenderNotice &&
              noticeDetail.tenderNotice.noticeStatus === 3 &&
              !noticeDetail.tenderNotice.isEval &&
              noticeDetail.purchaseOfficer &&
              !isSubmitEval
                ? false
                : true
            "
            >{{(noticeDetail.tenderNotice && noticeDetail.tenderNotice.noticeStatus === 3)?((noticeDetail.tenderNotice && noticeDetail.tenderNotice.isEval || isSubmitEval)?'评标中':'开始评标'):'无法评标'}}</el-button
          >
<!--          twiceBidConf-->
          <el-button
            type="primary"
            size="small"
            @click="clickTwiceBidConfButton"
            :disabled="
              evaluateStateList.length &&
              noticeDetail.tenderNotice &&
              noticeDetail.tenderNotice.noticeStatus === 3 &&
              noticeDetail.purchaseOfficer
                ? false
                : true
            "
            >{{(noticeDetail.tenderNotice && noticeDetail.tenderNotice.noticeStatus === 3)?noticeDetail.tenderNotice.twiceQuotState === 1?'调价中':'二次报价':'无法调价'}}</el-button
          >
          <el-button
            type="primary"
            size="small"
            @click="twiceBidFinish"
            :disabled="
              evaluateStateList.length &&
              noticeDetail.tenderNotice &&
              noticeDetail.tenderNotice.noticeStatus === 3 &&
              noticeDetail.purchaseOfficer
                ? false
                : true
            "
            >结束报价</el-button
          >
          <el-button
            type="primary"
            size="small"
            @click="evaluatBidOver"
            :disabled="
              evaluateStateList.length &&
              noticeDetail.tenderNotice &&
              noticeDetail.tenderNotice.noticeStatus === 3 &&
              noticeDetail.purchaseOfficer
                ? false
                : true
            "
            >评标结束</el-button
          >
        </template>
        <!-- 二次洽商状态 -->
        <!-- <template v-if="activeName === 'negotiations'">
        <el-button type="primary" size="small">开始评分</el-button>
        <el-button type="primary" size="small">确定</el-button>
        <el-button type="primary" size="small">评标结束</el-button>
      </template> -->
      </div>
      <el-tabs v-model="activeName" :before-leave="beforeLeave">
        <el-tab-pane label="开标" name="open">
          <el-table size="small" :data="backBidList" border stripe>
            <el-table-column
              label="序号"
              type="index"
              width="50"
              align="center"
            />
            <el-table-column
              label="供应商名称"
              width="200"
              prop="vendorName"
              show-overflow-tooltip
            />
            <el-table-column
              label="联系人"
              width="200"
              align="center"
              prop="contact"
            />
            <el-table-column label="联系电话" align="center" prop="phone" />
            <el-table-column
              label="投标时间"
              align="center"
              prop="createTime"
            />
            <el-table-column
              label="回标详情"
              align="center"
              prop="biddingStatusText"
            />
          </el-table>

          <PageTitle title="开标结果" marginBottom="15px" />
          <el-table size="mini" :data="peopleList" border stripe>
            <el-table-column
              label="序号"
              type="index"
              width="50"
              align="center"
            />
            <el-table-column
              label="开标人员"
              width="200"
              align="center"
              prop="userName"
            />
            <el-table-column
              label="是否开标"
              align="center"
              prop="isOpenText"
            />
            <el-table-column
              label="开标时间"
              align="center"
              prop="createTime"
            />
            <el-table-column label="操作" align="center" width="100">
              <template slot-scope="scope">
                <el-button
                  @click="handleOpenUserClick(scope.row)"
                  :disabled="
                    noticeDetail.tenderNotice.isOpenPeople === 0 ||
                    !(
                      noticeDetail.tenderNotice.noticeStatus === 2 &&
                      scope.row.isOpenUser === 1 &&
                      scope.row.isOpen === 0
                    )
                  "
                  type="text"
                  size="small"
                  >开标</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <el-tab-pane
          label="评标"
          name="evaluate"
          v-if="scheme.procurementType !== 4"
        >
          <el-table
            size="small"
            :data="evaluateList"
            border
            stripe
            @selection-change="handleSelectionChange"
            @row-click="rowClick"
          >
            <el-table-column type="selection" width="55" fixed="left" />
            <el-table-column
              label="序号"
              type="index"
              width="50"
              align="center"
            />
            <el-table-column
              label="供应商名称"
              width="200"
              prop="vendorName"
              show-overflow-tooltip
            />
            <el-table-column
              label="联系人"
              width="100"
              align="center"
              prop="contact"
            />
            <el-table-column
              label="联系电话"
              width="200"
              align="center"
              prop="phone"
            />
            <el-table-column
              label="调价状态"
              width="200"
              align="center"
              prop="priceChangeState"
              :formatter="formatterPriceChangeState"
            />
            <el-table-column
              label="回标详情"
              align="center"
              prop="biddingStatusText"
            >
              <template slot-scope="{ row }">
                <el-button
                  type="text"
                  size="mini"
                  @click.prevent="viewDetail(row.biddingInfoId)"
                  >查看详情</el-button
                >
              </template>
            </el-table-column>
            <el-table-column label="供应商报价" align="center">
              <el-table-column
                :label="index === 0 ? ((noticeDetail.tenderNotice && noticeDetail.tenderNotice.noticeStatus === 3)?'首轮报价':scoreLength<=1?'最终轮报价':'首轮报价'):((noticeDetail.tenderNotice && noticeDetail.tenderNotice.noticeStatus === 3)?`${index + 1}轮报价`:((index!==scoreLength-1)?`${index + 1}轮报价`:'最终轮报价'))"
                align="center"
                v-for="(item, index) in scoreLength"
                :key="index"
              >
                <el-table-column
                  width="100"
                  label="含税总价(元)"
                  align="center"
                >
                  <template slot-scope="{ row }">
                    {{
                      row.quotationDataVOList[index] &&
                      row.quotationDataVOList[index].taxPricePattern
                        ? row.quotationDataVOList[index].taxPricePattern
                        : "-"
                    }}
                  </template>
                </el-table-column>
                <el-table-column
                  width="120"
                  label="不含税总价(元)"
                  align="center"
                >
                  <template slot-scope="{ row }">
                    {{
                      row.quotationDataVOList[index] &&
                      row.quotationDataVOList[index].notTaxPricePattern
                        ? row.quotationDataVOList[index].notTaxPricePattern
                        : "-"
                    }}
                  </template>
                </el-table-column>
              </el-table-column>
              <el-table-column label="综合得分" align="center" prop="score">
                <template slot-scope="{ row }">
                  {{ row.score ? row.score : "-" }}
                </template>
              </el-table-column>
              <el-table-column label="综合排名" align="center" prop="rank" />
              <el-table-column label="评标附件上传" align="center" width="150">
                <template slot-scope="{ row }">
                  <a
                    :href="row.attachments[0].fileUrl"
                    target="_blank"
                    class="link-type"
                    v-if="row.attachments.length"
                    >下载</a
                  >
                  <el-upload
                    :action="uploadFileUrl"
                    :limit="1"
                    v-else
                    :on-success="fileSuccess"
                  >
                    <el-button
                      size="small"
                      type="primary"
                      :disabled="!noticeDetail.purchaseOfficer"
                      >点击上传</el-button
                    >
                  </el-upload>
                </template>
              </el-table-column>
            </el-table-column>
          </el-table>
          <div class="page-title">
            <span>评标状态</span>
          </div>
          <el-table size="mini" :data="evaluateStateList" border>
            <!-- <el-table-column type="selection" width="55"/> -->
            <el-table-column
              label="序号"
              type="index"
              width="50"
              align="center"
            />
            <el-table-column
              label="专家姓名"
              align="center"
              prop="expertName"
            />
            <el-table-column
              label="是否已评"
              align="center"
              prop="evalStatusText"
            />
            <el-table-column label="操作" align="center">
              <template slot-scope="{ row }">
                <el-button
                  type="text"
                  :disabled="row.evalStatus === 1"
                  @click="sendMessage(row)"
                  link
                  size="mini"
                  >{{ row.evalStatus === 1 ? "无可用操作" : "催办" }}</el-button
                >
              </template>
            </el-table-column>
          </el-table>
        </el-tab-pane>
        <!-- <el-tab-pane label="二次洽商" name="negotiations">
        <el-form :model="quoteForm" ref="vForm" label-position="left" label-width="80px" size="medium"
            @submit.native.prevent>
            <el-row :gutter="10">
              <el-col :span="8" class="grid-cell">
                <el-form-item label="二次报价截止时间：" prop="twiceTime" class="label-right-align" label-width="140px">
                  <el-date-picker v-model="quoteForm.updateBefore" type="datetime" style="width:100%" placeholder="选择日期"
                  :picker-options="expireTimeOption" value-format="yyyy-MM-dd HH:mm" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-table size="mini" :data="evaluateList" border>
              <el-table-column label="序号" type="index" width="50" align="center" />
              <el-table-column label="供应商名称" width="200" align="center" prop="vendorName" />
              <el-table-column label="联系人" width="200" align="center" prop="contact" />
              <el-table-column label="联系电话" align="center" prop="phone" />
              <el-table-column label="首轮报价" align="center">
                <el-table-column prop="taxPrice" label="含税总价(元)" align="center"/>
                <el-table-column prop="notTaxPrice" label="不含税总价(元)" align="center"/>
                <el-table-column prop="rank" label="报价排名" align="center"/>
              </el-table-column>
              <el-table-column label="二次报价" align="center">
                <el-table-column prop="" label="含税总价(元)" align="center"/>
                <el-table-column prop="" label="不含税总价(元)" align="center"/>
                <el-table-column prop="" label="报价排名" align="center"/>
              </el-table-column>
            </el-table>
        </el-form>
      </el-tab-pane> -->
      </el-tabs>
      <!-- 选择开标人员 -->
      <el-dialog
        title="选择开标人员"
        :visible.sync="openBidVisiable"
        width="80%"
        append-to-body
        custom-class="dialog-class"
      >
        <div class="split-page-box">
          <Drag>
            <template v-slot:left-content>
              <div class="left">
                <el-input
                  v-model="deptName"
                  placeholder="请输入部门名称"
                  clearable
                  size="small"
                  prefix-icon="el-icon-search"
                  style="margin-bottom: 20px"
                />
                <el-tree
                  :data="deptOptions"
                  :props="defaultProps"
                  :expand-on-click-node="false"
                  class="custom-tree"
                  :filter-node-method="filterNode"
                  ref="tree"
                  node-key="id"
                  default-expand-all
                  highlight-current
                  @node-click="handleNodeClick"
                />
              </div>
            </template>
            <template v-slot:right-content>
              <div class="right">
                <el-form
                  :model="openParams"
                  ref="vForm"
                  label-position="left"
                  label-width="50px"
                  size="medium"
                  @submit.native.prevent
                >
                  <el-row :gutter="10">
                    <el-col :span="10" class="grid-cell">
                      <el-form-item
                        label="姓名"
                        prop="nickName"
                        class="label-right-align"
                      >
                        <el-input
                          v-model="openParams.nickName"
                          type="text"
                          clearable
                        ></el-input>
                      </el-form-item>
                    </el-col>
                    <el-col :span="4" class="grid-cell">
                      <div class="static-content-item">
                        <el-button
                          type="primary"
                          icon="el-icon-search"
                          size="medium"
                          @click="searchOpen"
                          >查询</el-button
                        >
                      </div>
                    </el-col>
                  </el-row>
                </el-form>
                <div class="table-transfer">
                  <el-row>
                    <!-- 左边表格 -->
                    <el-col :span="11">
                      <el-table
                        v-loading="workerLoading"
                        ref="leftTable"
                        :data="wokerList"
                        stripe
                        highlight-current-row
                        :row-key="selWorkerKey"
                        @selection-change="handleSelectionLeft"
                      >
                        <el-table-column
                          type="selection"
                          width="55"
                          :reserve-selection="true"
                        />
                        <el-table-column
                          label="序号"
                          type="index"
                          width="50"
                          align="center"
                        />
                        <el-table-column
                          label="姓名"
                          align="center"
                          prop="nickName"
                        />
                        <el-table-column
                          label="组织机构"
                          align="center"
                          prop="orgDeptName"
                        />
                        <el-table-column
                          label="工作部门"
                          align="center"
                          prop="dept.deptName"
                        />
                        <el-table-column
                          label="手机号码"
                          align="center"
                          prop="phonenumber"
                          width="150px"
                        />
                      </el-table>
                      <pagination
                        v-show="openTotal > 0"
                        :total="openTotal"
                        :page.sync="openParams.pageNum"
                        :limit.sync="openParams.pageSize"
                        @pagination="getListUser"
                      />
                    </el-col>

                    <!-- 中间按钮 -->
                    <el-col :span="2" class="buttons-center">
                      <el-button
                        @click="moveToRight"
                        type="primary"
                        :disabled="leftSelection.length === 0"
                        class="buttons-left"
                      >
                        移入
                      </el-button>
                      <el-button
                        @click="moveToLeft"
                        type="primary"
                        :disabled="rightSelection.length === 0"
                        class="buttons-right"
                      >
                        移出
                      </el-button>
                    </el-col>

                    <!-- 右边表格 -->
                    <el-col :span="11">
                      <el-table
                        :data="selectedWorkerList"
                        stripe
                        highlight-current-row
                        @selection-change="handleSelectionRight"
                      >
                        <el-table-column
                          type="selection"
                          width="55"
                          :reserve-selection="true"
                        />
                        <el-table-column
                          label="序号"
                          type="index"
                          width="50"
                          align="center"
                        />
                        <el-table-column
                          label="姓名"
                          align="center"
                          prop="nickName"
                        />
                        <el-table-column
                          label="组织机构"
                          align="center"
                          prop="orgDeptName"
                        />
                        <el-table-column
                          label="工作部门"
                          align="center"
                          prop="dept.deptName"
                        />
                        <el-table-column
                          label="手机号码"
                          align="center"
                          prop="phonenumber"
                          width="150px"
                        />
                      </el-table>
                    </el-col>
                  </el-row>
                </div>
              </div>
            </template>
          </Drag>
        </div>
        <div slot="footer" class="dialog-footer">
          <el-button
            type="primary"
            @click="confirmOpenWorker"
            style="width: 100px"
            size="small"
            >确 定</el-button
          >
          <el-button @click="cancelWorker" style="width: 100px" size="small"
            >取 消</el-button
          >
        </div>
      </el-dialog>

      <!-- 设置评标专家 -->
      <el-dialog
        title=""
        :visible.sync="openExpertVisiable"
        width="80%"
        append-to-body
        custom-class="dialog-class"
      >
        <template #title>
          <span>选择评标专家</span>
          <el-tooltip content="只能选择没有评标任务的专家" placement="top">
            <i
              class="el-icon-question"
              style="
                margin-left: 5px;
                margin-bottom: 1px;
                cursor: pointer;
                color: #909399;
                vertical-align: middle;
              "
            ></i>
          </el-tooltip>
        </template>
        <div class="split-page-box">
          <Drag box="box1">
            <!-- <template v-slot:left-content>
              <div class="left">
                <el-input
                  v-model="deptName"
                  placeholder="请输入部门名称"
                  clearable
                  size="small"
                  prefix-icon="el-icon-search"
                  style="margin-bottom: 20px"
                />
                <el-tree
                  :data="deptOptions"
                  :props="defaultProps"
                  :expand-on-click-node="false"
                  class="custom-tree"
                  :filter-node-method="filterNode"
                  ref="tree"
                  node-key="id"
                  default-expand-all
                  highlight-current
                  @node-click="handleExpertNodeClick"
                />
              </div>
            </template> -->
            <template v-slot:right-content>
              <div class="right">
                <el-form
                  :model="expertQuery"
                  ref="vForm"
                  label-position="left"
                  size="small"
                  @submit.native.prevent
                >
                  <el-row :gutter="24">
                    <el-col :span="6" class="grid-cell">
                      <el-form-item
                        label="组织机构"
                        prop="deptIds"
                        class="label-right-align"
                        label-width="85px"
                      >
                        <treeselect
                          v-model="expertQuery.deptIds"
                          :options="deptOptions"
                          :multiple="true"
                          placeholder="请选择"
                          :async="true"
                          :load-options="deptAsyncOptions"
                          loadingText="正在加载..."
                          searchPromptText="请输入搜索内容..."
                        >
                          <template #option-label="{ node, labelClassName }">
                            <el-tooltip
                              :content="node.label"
                              :class="labelClassName"
                              placement="right-start"
                            >
                              <span>{{ node.label }}</span>
                            </el-tooltip>
                          </template>
                        </treeselect>
                      </el-form-item>
                    </el-col>
                    <el-col :span="4" class="grid-cell">
                      <el-form-item
                        label="业态类别"
                        prop="businessTypes"
                        class="label-right-align"
                        label-width="80px"
                      >
                        <el-select
                          v-model="expertQuery.businessTypes"
                          placeholder="请选择"
                          style="width: 100%"
                          multiple
                          clearable
                        >
                          <el-option
                            v-for="dict in dict.type.expert_business_type"
                            :key="dict.value"
                            :label="dict.label"
                            :value="dict.value"
                          ></el-option>
                        </el-select>
                      </el-form-item>
                    </el-col>

                    <el-col :span="4" class="grid-cell">
                      <el-form-item
                        label="专家类别"
                        prop="expertTypes"
                        class="label-right-align"
                        label-width="80px"
                      >
                        <el-select
                          v-model="expertQuery.expertTypes"
                          placeholder="请选择"
                          style="width: 100%"
                          multiple
                          clearable
                        >
                          <el-option
                            v-for="dict in dict.type.expert_type"
                            :key="dict.value"
                            :label="dict.label"
                            :value="dict.value"
                          ></el-option>
                        </el-select>
                      </el-form-item>
                    </el-col>
                    <el-col :span="4" class="grid-cell">
                      <el-form-item
                        label="专业"
                        prop="major"
                        class="label-right-align"
                        label-width="50px"
                      >
                        <el-input v-model="expertQuery.major"></el-input>
                      </el-form-item>
                    </el-col>
                    <el-col :span="4" class="grid-cell">
                      <el-form-item
                        label="本专业工作年限"
                        prop="workYear"
                        class="label-right-align"
                        label-width="110px"
                      >
                        <el-input v-model="expertQuery.workYear"></el-input>
                      </el-form-item>
                    </el-col>
                  </el-row>
                  <el-row :gutter="24">
                    <el-col :span="6" class="grid-cell">
                      <el-form-item
                        label="职称"
                        prop="technicalTitles"
                        class="label-right-align"
                        label-width="85px"
                      >
                        <el-select
                          v-model="expertQuery.technicalTitles"
                          placeholder="请选择"
                          style="width: 100%"
                          clearable
                        >
                          <el-option
                            v-for="dict in dict.type.technical_titles"
                            :key="dict.value"
                            :label="dict.label"
                            :value="dict.value"
                          ></el-option>
                        </el-select>
                      </el-form-item>
                    </el-col>
                    <el-col :span="4" class="grid-cell">
                      <el-form-item
                        label="职业资格证"
                        prop="registeredCertificate"
                        class="label-right-align"
                        label-width="85px"
                      >
                        <el-select
                          v-model="expertQuery.registeredCertificate"
                          placeholder="请选择"
                          style="width: 100%"
                          clearable
                        >
                          <el-option
                            v-for="dict in dict.type.registered_certificate"
                            :key="dict.value"
                            :label="dict.label"
                            :value="dict.value"
                          ></el-option>
                        </el-select>
                      </el-form-item>
                    </el-col>
                    <el-col :span="4" class="grid-cell">
                      <el-form-item
                        label="专家姓名"
                        prop="expertName"
                        class="label-right-align"
                        label-width="80px"
                      >
                        <el-input
                          v-model="expertQuery.expertName"
                          type="text"
                          clearable
                        ></el-input>
                      </el-form-item>
                    </el-col>
                    <el-col :span="3" class="grid-cell">
                      <div class="static-content-item">
                        <el-button
                          type="primary"
                          icon="el-icon-search"
                          size="small"
                          @click="searchExpert"
                          >查询</el-button
                        >
                      </div>
                    </el-col>
                  </el-row>
                </el-form>
                <div class="placeholder"></div>
                <el-row>
                  <el-col :span="11">
                    <el-table
                      v-loading="expertLoading"
                      ref="expertRef"
                      :data="expertList"
                      border
                      :row-key="selRowKey"
                      @selection-change="handleSelectionExpert"
                    >
                      <el-table-column
                        type="selection"
                        width="55"
                        :reserve-selection="true"
                      />
                      <el-table-column
                        label="序号"
                        type="index"
                        width="50"
                        align="center"
                      />
                      <el-table-column
                        label="专家"
                        align="center"
                        key="expertName"
                        prop="expertName"
                      />
                      <el-table-column
                        label="手机号码"
                        align="center"
                        key="expertPhone"
                        prop="expertPhone"
                      />
                      <el-table-column
                        label="组织机构"
                        width="200"
                        align="center"
                        key="belongOrganization"
                        prop="belongOrganization"
                        show-overflow-tooltip
                      >
                        <template #header>
                          <div class="flex-container">
                            <span>组织机构</span>
                            <span
                              v-if="foldFlag"
                              class="flex align-center cursor_point"
                              @click.stop="clickFoldButton(false)"
                            >
                              <span class="toggle-box flex">收起</span>
                              <i
                                class="el-icon-caret-left"
                                style="font-size: 14px"
                              ></i>
                            </span>
                            <span
                              v-else
                              class="flex align-center cursor_point"
                              @click.stop="clickFoldButton(true)"
                            >
                              <span class="toggle-box flex">展开</span>
                              <i
                                class="el-icon-caret-right"
                                style="font-size: 14px"
                              ></i>
                            </span>
                          </div>
                        </template>
                      </el-table-column>
                      <el-table-column
                        label="专家类别"
                        align="center"
                        key="expertTypeText"
                        prop="expertTypeText"
                        v-if="foldFlag"
                      />
                      <el-table-column
                        label="专业"
                        align="center"
                        key="major"
                        prop="major"
                        v-if="foldFlag"
                      />
                      <el-table-column
                        label="本专业工作年限"
                        align="center"
                        key="yearDiff"
                        prop="yearDiff"
                        v-if="foldFlag"
                      />
                      <el-table-column
                        label="职称"
                        align="center"
                        key="technicalTitlesText"
                        prop="technicalTitlesText"
                        v-if="foldFlag"
                      />
                      <el-table-column
                        label="职业资格"
                        align="center"
                        key="registeredCertificateText"
                        prop="registeredCertificateText"
                        v-if="foldFlag"
                      />
                    </el-table>
                    <pagination
                      v-show="expertTotal > 0"
                      :total="expertTotal"
                      :page.sync="expertQuery.pageNumber"
                      :limit.sync="expertQuery.pageSize"
                      @pagination="getExpertList"
                    />
                  </el-col>
                  <el-col :span="2" class="text-center">
                    <el-button type="primary" @click="moveToSelected"
                      >移入</el-button
                    >
                    <br /><br />
                    <el-button type="primary" @click="moveToAvailable"
                      >移出</el-button
                    >
                  </el-col>
                  <el-col :span="11">
                    <el-table
                      ref="selectedRef"
                      :data="selectedList"
                      border
                      :row-key="selRowKey"
                      @selection-change="handleSelectionSelected"
                    >
                      <el-table-column
                        type="selection"
                        width="55"
                        :reserve-selection="true"
                      />
                      <el-table-column
                        label="序号"
                        type="index"
                        width="50"
                        align="center"
                      />
                      <el-table-column
                        label="专家"
                        align="center"
                        key="expertName"
                        prop="expertName"
                      />
                      <el-table-column
                        label="手机号码"
                        align="center"
                        key="expertPhone"
                        prop="expertPhone"
                      />
                      <el-table-column
                        label="组织机构"
                        width="200"
                        align="center"
                        key="belongOrganization"
                        prop="belongOrganization"
                        show-overflow-tooltip
                      >
                        <template #header>
                          <div class="flex-container">
                            <span>组织机构</span>
                            <span
                              v-if="foldFlagChoose"
                              class="flex align-center cursor_point"
                              @click.stop="clickFoldChooseButton(false)"
                            >
                              <span class="toggle-box flex">收起</span>
                              <i
                                class="el-icon-caret-left"
                                style="font-size: 14px"
                              ></i>
                            </span>
                            <span
                              v-else
                              class="flex align-center cursor_point"
                              @click.stop="clickFoldChooseButton(true)"
                            >
                              <span class="toggle-box flex">展开</span>
                              <i
                                class="el-icon-caret-right"
                                style="font-size: 14px"
                              ></i>
                            </span>
                          </div>
                        </template>
                      </el-table-column>
                      <el-table-column
                        label="专家类别"
                        align="center"
                        key="expertTypeText"
                        prop="expertTypeText"
                        v-if="foldFlagChoose"
                      />
                      <el-table-column
                        label="专业"
                        align="center"
                        key="major"
                        prop="major"
                        v-if="foldFlagChoose"
                      />
                      <el-table-column
                        label="本专业工作年限"
                        align="center"
                        key="yearDiff"
                        prop="yearDiff"
                        v-if="foldFlagChoose"
                      />
                      <el-table-column
                        label="职称"
                        align="center"
                        key="technicalTitlesText"
                        prop="technicalTitlesText"
                        v-if="foldFlagChoose"
                      />
                      <el-table-column
                        label="职业资格"
                        align="center"
                        key="registeredCertificateText"
                        prop="registeredCertificateText"
                        v-if="foldFlagChoose"
                      />
                      <el-table-column
                        width="100"
                        label="是否参加"
                        align="center"
                        key="isJoin"
                        prop="isJoin"
                        show-overflow-tooltip
                        v-if="foldFlagChoose"
                      >
                        <template #default="{ row }">
                          <el-select v-model="row.isJoin" placeholder="">
                            <el-option
                              v-for="option in options"
                              :key="option.value"
                              :label="option.label"
                              :value="option.value"
                            ></el-option>
                          </el-select>
                        </template>
                      </el-table-column>
                    </el-table>
                  </el-col>
                </el-row>
              </div>
            </template>
          </Drag>
        </div>
        <div slot="footer" class="dialog-footer">
          <el-button
            @click="openExpertVisiable = false"
            style="width: 100px"
            size="small"
            >取 消</el-button
          >
          <el-button @click="openRandomDialog" style="width: 100px" size="small"
            >随机抽取</el-button
          >
          <el-button
            type="primary"
            @click="confirmExpert"
            style="width: 100px"
            size="small"
            >确 定</el-button
          >
        </div>
      </el-dialog>
      <el-dialog
        title="随机抽取专家"
        :visible.sync="randomDialogVisible"
        width="30%"
      >
        <el-form :model="drawVO" label-width="100px">
          <div class="selectExpert">
            <span>已选专家：</span>
            <el-tag
              v-for="tag in expertIds"
              :key="tag.expertName"
              closable
              @close="handleClose(tag)"
            >
              {{ tag.expertName }}
            </el-tag>
          </div>
          <el-form-item label-width="120" label="抽取人数(单数)">
          </el-form-item>
          <el-form-item label-width="130" label="经济类专家总人数">
            <el-input
              v-model="drawVO.econExpertNum"
              type="number"
              min="1"
              style="width: 200px"
              @blur="validateSum"
            ></el-input>
            <span style="margin-left: 10px"
              >{{ drawVO.econExpertNum }}/{{ economicNumber }}</span
            >
          </el-form-item>
          <el-form-item label-width="130" label="技术类专家总人数">
            <el-input
              v-model="drawVO.techExpertNum"
              type="number"
              min="1"
              style="width: 200px"
              @blur="validateSum"
            ></el-input>
            <span style="margin-left: 10px"
              >{{ drawVO.techExpertNum }}/{{ techniciansNumber }}</span
            >
          </el-form-item>
        </el-form>
        <div slot="footer" class="dialog-footer">
          <el-button
            @click="randomDialogVisible = false"
            style="width: 100px"
            size="small"
            >取 消</el-button
          >
          <el-button
            type="primary"
            @click="confirmRandomSelection"
            style="width: 100px"
            size="small"
            >确 定</el-button
          >
        </div>
      </el-dialog>
      <!-- 查看回标详情 -->
      <el-dialog
        title="回标详情"
        :visible.sync="bidDetailVisiable"
        width="80%"
        append-to-body
      >
        <BackBidDetail
          :id="biddingInfoId"
          :noticeDetail="noticeDetail"
          :scheme="scheme"
        />
      </el-dialog>
    </div>
  </div>
</template>

<script>
import {
  getBackList,
  getBiddingQuotationList,
  addWorker,
  submitWorker,
  addEvaluatExpert,
  evaluatBidOver,
  startEvaluat,
  twiceBidConf,
  twiceBidFinish,
  uploadEvalAttach,
  getExpertEvalStatus,
  getPeopleList,
  openBidPeople,
  saveUrgeExpertMes,
} from "@/api/procurement/manage";
import { uploadFileUrl } from "@/utils/const";
import { deptTreeSelect, listUser } from "@/api/system/user";
import { getExpertList } from "@/api/expert/expert";
import BackBidDetail from "./back-bid-detail.vue";
import PageTitle from "@/components/PageTitle/index.vue";
import Drag from "@/components/Drag/index.vue";
import Treeselect from "@riophae/vue-treeselect";
import "@riophae/vue-treeselect/dist/vue-treeselect.css";
import {PRICECHANGESTATEOPTIONS} from "@/utils/constants";
export default {
  name: "evaluate-bid",
  dicts: [
    "expert_type",
    "expert_business_type",
    "technical_titles",
    "registered_certificate",
  ],
  props: {
    noticeDetail: {
      type: Object,
      default: () => {},
    },
    scheme: {
      type: Object,
      default: () => {},
    },
    changeState: {
      type: Function,
      default: () => {},
    },
  },
  data() {
    const _that = this
    return {
      isSubmitEval: false,
      needTime: true,
      timer: null,
      timeForm: {},
      dialogVisible: false,
      timeDifferenceElement: '00天00分00秒',
      activeName: "",
      openBidVisiable: false,
      // 部门树选项
      deptOptions: [],
      // 岗位选项
      postOptions: [],
      // 角色选项
      roleOptions: [],
      // 部门名称
      deptName: undefined,
      // 表单参数
      form: {},
      defaultProps: {
        children: "children",
        label: "label",
      },
      wokerList: [],
      // 查询参数
      queryParams: {
        pageNum: 1,
        pageSize: 10,
        expertName: undefined,
        id: undefined,
        noticeId: undefined,
      },
      workerLoading: false,
      staffIds: [],
      backBidList: [],
      evaluateList: [],
      expireTimeOption: {
        // 设置日期时间显示格式，只显示年月日时分
        format: "yyyy-MM-dd HH:mm:ss",
        // 设置可选的时间范围
        selectableRange: "00:00:00 - 23:59:59",
        // disabledDate(time) {
        //   return time.getTime() < Date.now() - 8.64e7; // 禁用小于当前日期的日期
        // }
        disabledDate(time) {

          // 获取截止时间
          const next = new Date(_that.noticeDetail.tenderNotice.twiceTime);
          next.setDate(next.getDate() -1);

          // 将传入的时间戳转为日期对象
          const date = new Date(time);

          // 只能选择过5天后的日期【比如今天是24号，则30号及以后可以选择】
          return date < next;
        },
      },
      quoteForm: {}, //二次洽商
      //选择专家数据
      expertLoading: false,
      openExpertVisiable: false,
      expertList: [],
      selectedList: [], // 选中的专家列表数据
      selectedExperts: [], // 当前表格中选中的专家
      availableExperts: [], // 可用表格中选中的专家
      expertIds: [],
      expertTotal: 0,
      expertQuery: {
        pageNumber: 1,
        pageSize: 10,
        deptIds: [],
        businessTypes: [],
        expertTypes: [],
        major: "",
        workYear: "",
        technicalTitles: "",
        registeredCertificate: "",
        expertName: "",
      },
      // 查询参数
      openParams: {
        pageNum: 1,
        pageSize: 10,
        nickName: undefined,
        userType: "purchase",
      },
      openTotal: 0,
      biddingInfoIds: [],
      bidDetailVisiable: false,
      biddingInfoId: "",
      scoreLength: 0,
      uploadFileUrl,
      uploadBiddingInfoId: "",
      evaluateStateList: [], //专家评标状态列表
      peopleList: [], //开标人员信息列表
      options: [
        { value: true, label: "是" },
        { value: false, label: "否" },
      ],
      // 随机抽取专家
      randomDialogVisible: false,
      drawVO: {
        econExpertNum: 2,
        techExpertNum: 1,
      },
      isValidSum: true,
      expertTypeCounts: [],
      techniciansNumber: 1, //技术类专家人数
      economicNumber: 1, //经济类专家人数
      screeningRandom: {
        econExpertNum: 0,
        techExpertNum: 0,
      },
      foldFlag: false,
      foldFlagChoose: false,
      selectedWorkerList: [],
      leftSelection: [],
      rightSelection: [],
      debouncedLoadDeptOptions: null,
      expertQuery: {
        deptIds: [],
      },
      deptOptions: [],
    };
  },
  created() {
    this.getBackList();
    this.getPeopleList();
    this.debouncedLoadDeptOptions = this.debounce(this.loadDeptOptions, 300);
    if (
      this.noticeDetail?.tenderNotice?.noticeStatus === 3 ||
      this.noticeDetail?.tenderNotice?.noticeStatus === 4
    ) {
      this.activeName = "evaluate";
    } else {
      this.activeName = "open";
    }
  },
  components: {
    BackBidDetail,
    PageTitle,
    Drag,
    Treeselect,
  },
  mounted() {
    if(this.noticeDetail?.tenderNotice?.noticeStatus === 3) {
      if(this.noticeDetail?.tenderNotice?.twiceTime) {
        this.updateTimeDifference();
        // 每秒更新一次
        if(this.needTime) {
          this.timer = setInterval(this.updateTimeDifference, 1000);
        }
      }else {
        this.$message.error('截止时间取值有误，请联系管理人员！')
        this.$router.replace("/procurement/bindding");
      }
    }


  },
  methods: {
    formatterPriceChangeState(row,_column,cellvalue){
      if(this.noticeDetail.tenderNotice?.twiceQuotVersion === 1){
        return '-'
      }
      // * 区分状态，为0时区分未调价和调价中
      if(row.priceChangeState === 0) {
        if(row.twiceQuot === 0) {
          return '未调价'
        }else{
          return '调价中'
        }
      }else {
        const findObj = PRICECHANGESTATEOPTIONS.find(item => item.value === cellvalue)
        return findObj ? findObj.label : '-'
      }

    },
    async updateTimeDifference() {
      // 投标截止时间
      const deadline = new Date(this.noticeDetail?.tenderNotice?.twiceTime);
      const now = new Date();
      // 计算时间差
      const diff = deadline - now;
      if(diff<=0){
        this.needTime = false;
        this.timeDifferenceElement = `00天00小时00分00秒！`;
        if(this.timer) {
          clearInterval(this.timer);
        }
        if(this.noticeDetail?.tenderNotice?.twiceQuotState === 1){
          const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
          try {
            const res = await twiceBidFinish(noticeId);
          } catch (err) {
            console.log(err);
          }
        }
        return
      }

      // 计算天数、小时数、分钟数和秒数
      const days = Math.floor(diff / (1000 * 60 * 60 * 24));
      const hours = Math.floor((diff % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60));
      const minutes = Math.floor((diff % (1000 * 60 * 60)) / (1000 * 60));
      const seconds = Math.floor((diff % (1000 * 60)) / 1000);

      // 格式化并显示结果
      this.timeDifferenceElement = `${days<10?'0'+days:days}天${hours<10?'0'+hours:hours}小时${minutes<10?'0'+minutes:minutes}分${seconds<10?'0'+seconds:seconds}秒！`;
    },
    // handleClick(event) {
    //   const { name } = event
    //   this.activeName = name
    //   if (name === 'evaluate') {
    //     this.getBiddingQuotationList()
    //     this.getExpertEvalStatus()
    //   }
    // },
    beforeLeave(activeName) {
      const { noticeStatusText } = this.noticeDetail || {};
      const { noticeStatus } = this.noticeDetail?.tenderNotice || {};
      if (noticeStatus < 3) {
        this.$message.error(
          "当前招标状态为" + noticeStatusText + "，不能进行此操作"
        );
        return false;
      }
    },
    setOpenWorker() {
      this.openBidVisiable = true;
      this.getListUser();
      this.getDeptTree();
    },
    searchOpen() {
      this.openParams.pageNum = 1;
      this.getListUser();
    },
    /** 查询部门下拉树结构 */
    getDeptTree() {
      deptTreeSelect({ onlyQueryOrg: 1 }).then((response) => {
        this.deptOptions = response.data;
      });
    },
    /** 获取开标人员 */
    async getListUser() {
      this.loading = true;
      this.workerLoading = true;
      try {
        const res = await listUser(this.openParams);
        this.loading = false;
        this.wokerList = res.rows;
        this.openTotal = res.total;
      } catch (err) {
        this.loading = true;
        console.log(err);
      } finally {
        this.workerLoading = false; // 数据加载完成时设置为 false
      }
    },
    // 筛选节点
    filterNode(value, data) {
      if (!value) return true;
      return data.label.indexOf(value) !== -1;
    },
    // 节点单击事件
    handleNodeClick(data) {
      this.openParams.deptId = data.id;
      this.getListUser();
    },
    handleExpertNodeClick(data) {
      this.expertQuery.deptId = data.id;
      this.getExpertList();
    },
    /** 搜索开标人员 */
    searchWorker() {
      this.queryParams.pageNum = 1;
      this.getWorker();
    },
    // 取消按钮
    cancelWorker() {
      this.openBidVisiable = false;
      this.checkedItem = {};
      this.departmentId = "";
    },
    searchExpert() {
      this.queryParams.pageNum = 1;
      this.getExpertList();
    },
    /** 确定开标人员 */
    async confirmOpenWorker() {
      const { staffIds } = this;
      if (staffIds.length < 2 || staffIds.length > 2)
        return this.$message.error("请选择2名开标人员");
      const { id: schemeId } = this.scheme;
      const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
      let formData = this.selectedWorkerList.map((item) => ({
        schemeId,
        noticeId,
        userId: item.userId,
        userName: item.nickName,
        redirectUrl: this.$route.fullPath,
      }));
      try {
        const res = await addWorker(formData);
        this.$message.success("设置开标人员成功");
        this.getPeopleList();
        this.openBidVisiable = false;
        // this.activeName = 'evaluate'
        // if(this.scheme.procurementType === 4){
        //   this.$emit('changeState', 3)
        // }
        console.log(res, "设置开标人员");
      } catch (err) {
        console.log(err);
      }
      this.$emit("changeState");
    },
    //确认选择开标人员弹出层
    // confirmStaff() {
    //   const { staffIds } = this;
    //   if (staffIds.length < 2 || staffIds.length > 2)
    //     return this.$message.error("请选择2名开标人员");
    //   this.openBidVisiable = false;
    // },
    // 提交开标人员信息
    async confirmSumbmit() {
      try {
        const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
        await submitWorker(noticeId);
        this.$message.success("提交成功");
        this.$emit("changeState");
      } catch (error) {
        this.$message.error("提交失败");
      }
    },
    /** 获取回标列表 */
    async getBackList() {
      const { id: schemeId } = this.scheme;
      const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
      try {
        const res = await getBackList({ schemeId, noticeId });
        this.backBidList = res.data;
        console.log(res, "rrr");
      } catch (err) {
        console.log(err);
      }
    },
    /** 获取评标列表 */
    async getBiddingQuotationList() {
      try {
        const { id: schemeId } = this.scheme;
        const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
        const res = await getBiddingQuotationList({ schemeId, noticeId });
        this.evaluateList = res.data;
        console.log(res, "res~~~~~~~~~~~~~~~~~");
        this.scoreLength = res.data[0].quotationDataVOList.length;
      } catch (err) {
        console.log(err);
      }
    },
    /** 选择开标人员 */
    // handleSelectionStaff(selection) {
    //   this.staffIds = selection.map((item) => item.userId);
    //   this.selectStaffList = selection;
    // },
    //选择专家操作区
    handleSelectionExpert(selection) {
      // this.expertIds = selection.map((item) => ({
      //   expertId: item.id,
      //   expertName: item.expertName,
      //   expertType: item.expertType,
      // }));
      this.availableExperts = selection;
    },
    handleSelectionSelected(selection) {
      this.selectedExperts = selection;
    },
    //获取专家列表
    setExpert() {
      this.openExpertVisiable = true;
      this.getDeptTree();
      this.getExpertList();
    },
    async getExpertList() {
      this.expertLoading = true;
      const selectedIds = this.selectedList.map((item) => item.id);
      try {
        const dataToSubmit = {
          ...this.expertQuery,
          expertState: 1,
          filterEvalExpert: 1,
          notIncludeExpertIdList: selectedIds,
          drawVO:
            this.screeningRandom.econExpertNum === 0 &&
            this.screeningRandom.techExpertNum === 0
              ? null
              : this.screeningRandom,
        };

        dataToSubmit.deptIds = Array.isArray(dataToSubmit?.deptIds)
          ? dataToSubmit.deptIds.join(",")
          : "";
        dataToSubmit.businessTypes = Array.isArray(dataToSubmit?.businessTypes)
          ? dataToSubmit.businessTypes.join(",")
          : "";
        dataToSubmit.expertTypes = Array.isArray(dataToSubmit?.expertTypes)
          ? dataToSubmit.expertTypes.join(",")
          : "";
        const res = await getExpertList(dataToSubmit);
        this.expertList = res.data.rows;
        this.expertTotal = res.data.total;
      } catch (err) {
        console.log(err);
      }
      this.expertLoading = false;
    },
    deptAsyncOptions({ action, searchQuery, callback }) {
      if (action === "ASYNC_SEARCH") {
        // 检查是否已经正确初始化
        if (typeof this.debouncedLoadDeptOptions === "function") {
          this.debouncedLoadDeptOptions(searchQuery)
            .then((options) => {
              callback(null, options);
            })
            .catch((error) => {
              callback(error, []);
            });
        } else {
          console.error("debouncedLoadDeptOptions is not a function");
        }
      }
    },
    async loadDeptOptions(searchQuery) {
      try {
        const response = await deptTreeSelect({
          onlyQueryOrg: 1,
          deptName: searchQuery,
        });

        const limitedData = response.data.slice(0, 500);
        return limitedData.map((dept) => ({
          id: dept.id,
          label: dept.label,
          children: dept.children,
        }));
      } catch (error) {
        console.error("Error loading department options:", error);
        return Promise.resolve([]); // 返回空数组以确保返回 Promise
      }
    },
    debounce(func, delay) {
      let timeout;
      return function (...args) {
        clearTimeout(timeout);
        return new Promise((resolve, reject) => {
          timeout = setTimeout(() => {
            func.apply(this, args).then(resolve).catch(reject);
          }, delay);
        });
      };
    },

    async confirmExpert() {
      const { selectedList } = this;
      const evaluateUrl = "/evaluate-expert/evaluate-bids";
      const selectedMapList = selectedList.map((item) => ({
        expertId: item.id,
        expertName: item.expertName,
        expertType: item.expertType,
        isJoin: item.isJoin,
        redirectUrl: evaluateUrl,
      }));
      const joinedExperts = selectedMapList.filter(
        (expert) => expert.isJoin === true
      );
      const notJoinedExperts = selectedMapList.filter(
        (expert) => expert.isJoin === false
      );

      const adopt = joinedExperts.length >= 5 && joinedExperts.length % 2 !== 0;

      if (!adopt)
        return this.$message.error("请选择5名或5名以上专家并且总数为奇数");

      const hasTechExpert = joinedExperts.some((item) => item.expertType === 1);
      const hasEconExpert = joinedExperts.some((item) => item.expertType === 2);

      if (!hasTechExpert || !hasEconExpert)
        return this.$message.error("评标专家必须包括经济、技术两类专家");
      const { id: schemeId } = this.scheme;
      const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
      const formData = {
        schemeId,
        noticeId,
        expertListVO: joinedExperts,
        notJoinExpertListVO: notJoinedExperts,
      };
      console.log(formData, "fomrData-formData-formData");
      try {
        const res = await addEvaluatExpert(formData);
        this.$message.success("设置专家成功");
        this.openExpertVisiable = false;
        this.getExpertEvalStatus();
      } catch (err) {
        console.log(err);
      }
    },
    async evaluatBidOver() {
      const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
      let text = "";
      try {
        const res = await getExpertEvalStatus({ noticeId });
        const length = res.data?.length;
        const notList = res.data?.filter((item) => item.evalStatus === 0);
        console.log(notList, "notList-notList");
        if (length === notList.length)
          return this.$message.error("必须有一个专家进行评标才能结束评标流程");
        if (notList.length) {
          let expertName = notList.map((item) => item.expertName).join(",");
          text = `专家${expertName}还未评分，是否确认结束评标?`;
        } else {
          text = "确定要结束评分吗?";
        }
      } catch (err) {
        console.log(err);
      }
      this.$confirm(text, "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(async () => {
        const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
        try {
          const res = await evaluatBidOver(noticeId);
          this.$message.success("提交成功");
          this.$emit("changeState", 3);
        } catch (err) {
          console.log(err);
        }
      });
    },
    startEvaluat() {
      this.$confirm("您确定要开启专家评标吗？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(async () => {
        this.isSubmitEval = true
        const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
        try {
          const res = await startEvaluat(noticeId);
          this.$message.success("已开启评标");
        } catch (err) {
          console.log(err);
        }

      });
    },
    clickTwiceBidConfButton() {
      if (this.biddingInfoIds.length === 0){
        return this.$message.error("请选择调价项目");
      }
      this.dialogVisible = true
    },
    //开始调价
    async twiceBidConf() {
      this.$refs.timeForm.validate(valid => {
        if (valid){
          this.$confirm("您确定要开始调价吗？", "提示", {
            confirmButtonText: "确定",
            cancelButtonText: "取消",
            type: "warning",
          }).then(async () => {
            try {
              const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
              const res = await twiceBidConf(this.biddingInfoIds, noticeId,this.timeForm.twiceTime);
              this.$message.success("已开启调价");
              // this.getBiddingQuotationList();
              // this.getExpertEvalStatus();
              this.dialogVisible = false
              this.$emit('changeState',3)
            } catch (err) {
              console.log(err);
            }
          });
        }else {
          return false;
        }
      })
    },
    //结束调价
    async twiceBidFinish() {
      this.$confirm("您确定要结束调价吗？", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      }).then(async () => {
        const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
        try {
          const res = await twiceBidFinish(noticeId);
          // this.getBiddingQuotationList();
          // this.getExpertEvalStatus();
          this.$message.success("已结束调价");
          this.$emit('changeState',3)
        } catch (err) {
          console.log(err);
        }
      });
    },
    /** 已选择计划 */
    handleSelectionChange(selection) {
      this.biddingInfoIds = selection.map((item) => item.id);
    },
    /** 查看回标详情 */
    viewDetail(id) {
      this.bidDetailVisiable = true;
      this.biddingInfoId = id;
    },
    async fileSuccess(res) {
      const { uploadBiddingInfoId } = this;
      const { url, name } = res.data;
      const formData = {
        biddingInfoId: uploadBiddingInfoId,
        attachmentList: [{ fileName: name, fileUrl: url }],
      };
      console.log(formData, "f-f-f-f-");
      try {
        const res = await uploadEvalAttach(formData);
        this.$message.success("上传成功");
        this.getBiddingQuotationList();
      } catch (err) {
        console.log(err);
      }
    },
    rowClick(row) {
      this.uploadBiddingInfoId = row.id;
    },
    /** 获取专家评标列表 */
    async getExpertEvalStatus() {
      const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
      try {
        const res = await getExpertEvalStatus({ noticeId });
        this.evaluateStateList = res.data;
      } catch (err) {
        console.log(err);
      }
    },
    selRowKey(row) {
      // console.log(row);
      return row.id;
    },
    selWorkerKey(row) {
      return row.userId;
    },
    /** 获取开标人员开标状态 */
    async getPeopleList() {
      const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
      try {
        const res = await getPeopleList(noticeId);
        this.peopleList = res.data;
      } catch (err) {
        console.log(err);
      }
    },
    moveToSelected() {
      const duplicateExperts = this.availableExperts.filter((expert) =>
        this.selectedList.some((selected) => selected.id === expert.id)
      );

      if (duplicateExperts.length > 0) {
        this.$message.error("您勾选的专家重复，请重新勾选");
      } else {
        const expertsToMove = this.availableExperts.map((expert) => ({
          ...expert,
          isJoin: true,
        }));
        this.selectedList = [...this.selectedList, ...expertsToMove];
        this.expertList = this.expertList.filter(
          (expert) => !this.availableExperts.includes(expert)
        );
        const selectedMapList = this.selectedList.map((item) => ({
          expertId: item.id,
          expertName: item.expertName,
          expertType: item.expertType,
          isJoin: item.isJoin,
        }));
        this.expertIds = selectedMapList;
        this.availableExperts = []; // 清空选择列表
        this.$refs.expertRef.clearSelection(); // 清除表格选择状态
      }
    },
    moveToAvailable() {
      // 从 selectedList 中移除 selectedExperts 中的专家
      this.selectedList = this.selectedList.filter(
        (expert) => !this.selectedExperts.includes(expert)
      );
      // 清空选择列表
      this.selectedExperts = [];
      // 清除表格选择状态
      this.$refs.selectedRef.clearSelection();
    },
    openRandomDialog() {
      this.randomDialogVisible = true;
      const joinedExperts = this.selectedList.filter((expert) => expert.isJoin);
      const counts = joinedExperts.reduce(
        (acc, expert) => {
          if (expert.expertTypeText === "技术类") {
            acc.techniciansNumber++;
          } else if (expert.expertTypeText === "经济类") {
            acc.economicNumber++;
          }
          return acc;
        },
        { techniciansNumber: 0, economicNumber: 0 }
      );

      // 将统计结果赋值给 data 中的属性
      this.techniciansNumber = counts.techniciansNumber;
      this.economicNumber = counts.economicNumber;
    },
    validateSum() {
      const sum =
        Number(this.drawVO.econExpertNum) + Number(this.drawVO.techExpertNum);

      if (
        Number(this.drawVO.econExpertNum) < Number(this.economicNumber) ||
        Number(this.drawVO.techExpertNum) < Number(this.techniciansNumber)
      ) {
        this.$message.error("专家总人数必须大于勾选的专家数");
      } else {
        if (sum >= 3 && sum % 2 !== 0) {
          this.isValidSum = true;
        } else {
          this.isValidSum = false;
          this.$message.error("请选择3名专家以上并且专家总数为奇数");
        }
      }
    },
    confirmRandomSelection() {
      const econExperSumNum =
        Number(this.drawVO.econExpertNum) - Number(this.economicNumber);
      const technicianSumNum =
        Number(this.drawVO.techExpertNum) - Number(this.techniciansNumber);
      this.screeningRandom.econExpertNum = econExperSumNum;
      this.screeningRandom.techExpertNum = technicianSumNum;
      if (this.isValidSum) {
        try {
          this.getExpertList();
          this.randomDialogVisible = false;
          this.screeningRandom.econExpertNum = 0;
          this.screeningRandom.techExpertNum = 0;
        } catch (error) {}
      }
    },
    clickFoldButton(flag) {
      this.foldFlag = flag;
      this.$nextTick(() => {
        this.$refs.expertRef.doLayout();
      });
    },
    clickFoldChooseButton(flag) {
      this.foldFlagChoose = flag;
      this.$nextTick(() => {
        this.$refs.selectedRef.doLayout();
      });
    },
    handleOpenUserClick(params) {
      this.$confirm("是否确定开标?", "提示", {
        confirmButtonText: "确定",
        cancelButtonText: "取消",
        type: "warning",
      })
        .then(async () => {
          await openBidPeople({
            schemeId: params.schemeId,
            noticeId: params.noticeId,
          });
          this.$message({
            type: "success",
            message: "开标成功!",
          });
          this.getPeopleList();
        })
        .catch(() => {});
    },
    handleSelectionLeft(selection) {
      this.leftSelection = selection;
    },
    handleSelectionRight(selection) {
      this.rightSelection = selection;
    },
    moveToRight() {
      // 将左侧选中的数据移到右侧，确保不重复
      const filteredSelection = this.leftSelection.filter(
        (item) => !this.selectedWorkerList.includes(item)
      );
      // 合并已有的 selectedWorkerList 和新的 filteredSelection
      const mergedList = [...this.selectedWorkerList, ...filteredSelection];
      // 使用 Set 去重
      this.selectedWorkerList = Array.from(
        new Set(mergedList.map((item) => JSON.stringify(item)))
      ).map((item) => JSON.parse(item));
      this.staffIds = this.selectedWorkerList.map((item) => item.userId);
      // 移除左侧表格中已选中的数据
      this.wokerList = this.wokerList.filter(
        (item) => !this.leftSelection.includes(item)
      );

      // 清空左侧选中项
      this.leftSelection = [];
    },

    moveToLeft() {
      // 从右侧数据中移除选中的数据
      const removedWorkers = this.selectedWorkerList.filter((item) =>
        this.rightSelection.includes(item)
      );

      // 将移出的数据添加回左侧的 wokerList
      this.wokerList = [...this.wokerList, ...removedWorkers];

      // 更新右侧的 selectedWorkerList，移除已选中的数据
      this.selectedWorkerList = this.selectedWorkerList.filter(
        (item) => !this.rightSelection.includes(item)
      );

      // 更新 staffIds
      this.staffIds = this.selectedWorkerList.map((item) => item.userId);

      // 清空右侧选中的数据
      this.rightSelection = [];

      // 清空左侧表格中的选中状态
      this.$nextTick(() => {
        this.$refs.leftTable.clearSelection(); // 通过 ref 调用 clearSelection 方法，清除选中的行
      });
    },

    handleClose(tag) {
      this.selectedList.splice(this.selectedList.indexOf(tag), 1);
      this.expertIds.splice(this.expertIds.indexOf(tag), 1);
    },
    sendMessage(row) {
      const { id: noticeId } = this.noticeDetail?.tenderNotice || {};
      saveUrgeExpertMes({
        expertId: row.expertId,
        noticeId: noticeId,
        projectName: this.$store.getters.project.name,
        belongOrgName: this.$store.state.user.userInfo.thridOrgName,
      }).then(() => {
        this.$message.success("催办成功");
      });
    },
  },
  watch: {
    // 根据名称筛选部门树
    deptName(val) {
      this.$refs.tree.filter(val);
    },
    activeName: {
      handler(val) {
        if (val === "evaluate") {
          this.getBiddingQuotationList();
          this.getExpertEvalStatus();
        } else if (val === "open") {
          this.getPeopleList();
        }
        console.log("来了");
      },
      // immediate: true,
    },
  },
};
</script>
<style lang="scss" scoped>
.page-title {
  width: 100%;
  border-bottom: solid 1px #ccc;
  padding: 10px;
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
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

  .textColor {
    color: red;
  }
}

.app-container {
  position: relative;

  .btn-box {
    position: absolute;
    right: 20px;
    top: 15px;
    z-index: 1001;
  }
}
.placeholder {
  padding: 10px;
}

.selectExpert {
  padding-bottom: 15px;
  height: 47px;
  display: flex;
  align-items: center;
  span {
    margin-right: 10px;
  }
}
// ::v-deep .el-tree {
//   width: 100%;
//   height: 45vh;
//   overflow: scroll;
// }

.custom-tree {
  height: calc(100vh - 15vh - 55px - 70px - 62px);
  overflow: scroll;
}
.dialog-class .el-dialog__body {
  padding: 0 !important;
}
::v-deep .el-tree > .el-tree-node {
  min-width: 100%;
  display: inline-block;
}
::v-deep .numBox div {
  border-right: none !important;
}
.text-center {
  text-align: center;
  margin-top: 100px;
}
.flex-container {
  display: flex;
  justify-content: center; /* 水平居中 */
  align-items: center; /* 垂直居中 */
  gap: 5px; /* 项目之间的间距，可选 */
  width: 100%; /* 确保容器占满其父级宽度 */
}
.toggle-box {
  font-weight: normal;
  font-size: 12px;
  cursor: pointer;
  align-items: center;
  justify-content: center;
  position: relative;
  color: #2b4acb;
}
.el-icon-caret-right:before {
  content: "\e791";
  color: #2b4acb;
}
.el-icon-caret-left:before {
  content: "\e792";
  color: #2b4acb;
}

.cursor_point {
  margin-left: 15px;
}
// .table-transfer {
//   display: flex;
//   justify-content: space-between;
// }

.buttons-center {
  margin-top: 220px;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100%; /* Ensure the column takes the full height */
}

.buttons-right {
  margin-top: 8px;
  margin-left: 0.1px;
}
</style>
