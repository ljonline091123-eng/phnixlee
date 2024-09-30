<template>
  <el-card class="box-card" v-if="flowRecordList">
        <div slot="header" class="clearfix">
          <span class="el-icon-notebook-1">审批记录</span>
        </div>
        <el-col :span="16" :offset="4">
          <div class="block">
            <el-timeline>
              <el-timeline-item v-for="(item, index ) in flowRecordList" :key="index" :icon="setIcon(item.finishTime)"
                :color="setColor(item.finishTime)">
                <p style="font-weight: 700">{{ item.taskName }}</p>
                <el-card :body-style="{ padding: '10px' }">
                  <el-descriptions class="margin-top" :column="1" size="small" border>
                    <el-descriptions-item v-if="item.assigneeName" label-class-name="my-label">
                      <template slot="label"><i class="el-icon-user"></i>实际办理</template>
                      {{ item.assigneeName }}
                      <el-tag type="info" size="mini" v-if="item.deptName">{{ item.deptName }}</el-tag>
                    </el-descriptions-item>
                    <el-descriptions-item v-if="item.candidate" label-class-name="my-label">
                      <template slot="label"><i class="el-icon-user"></i>候选办理</template>
                      {{ item.candidate }}
                    </el-descriptions-item>
                    <el-descriptions-item label-class-name="my-label">
                      <template slot="label"><i class="el-icon-date"></i>接收时间</template>
                      {{ item.createTime }}
                    </el-descriptions-item>
                    <el-descriptions-item v-if="item.finishTime" label-class-name="my-label">
                      <template slot="label"><i class="el-icon-date"></i>处理时间</template>
                      {{ item.finishTime }}
                    </el-descriptions-item>
                    <el-descriptions-item v-if="item.duration" label-class-name="my-label">
                      <template slot="label"><i class="el-icon-time"></i>耗时</template>
                      {{ item.duration }}
                    </el-descriptions-item>
                    <el-descriptions-item v-if="item.comment.comment" label-class-name="my-label">
                      <template slot="label"><i class="el-icon-tickets"></i>处理意见</template>
                      {{ item.comment.comment }}
                    </el-descriptions-item>
                  </el-descriptions>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
        </el-col>
      </el-card>
</template>
<script>
import { flowRecord } from "@/api/flowable/finished";
export default {
  props:{
    wfProcessId:{
      type:String,
      default:""
    }
  },
  data(){
    return {
      flowRecordList:[],
      formConfOpen: false,
    }
  },
  created(){
    this.getFlowRecordList()
  },
  methods:{
    getFlowRecordList() {
      flowRecord({procInsId:this.wfProcessId}).then(res => {
        this.flowRecordList = res.data.flowList;
      }).catch(err => {
        console.log(err);
      })
    },
    setIcon(val) {
      if (val) {
        return "el-icon-check";
      } else {
        return "el-icon-time";
      }
    },
    setColor(val) {
      if (val) {
        return "#2bc418";
      } else {
        return "#b3bdbb";
      }
    }
  }
}
</script>