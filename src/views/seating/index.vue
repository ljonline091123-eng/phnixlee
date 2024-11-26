<template>
<div  class="app-container">
         <div  style=" position: absolute;right: 20px;top: 10px;">
            <el-select style="right: 20px;" @change="handleChange" v-model="examinationroom" placeholder="请选择">
                <el-option
                v-for="item in options"
                :key="item.value"
                :label="item.label"
                :value="item.value">
                </el-option>
            </el-select>
            <el-button
            style="right: 20px;"
            type="primary"
            icon="el-icon-search"
            size="small"
            @click="seatingQuery"
            >搜索</el-button
            >
             <el-button  type="primary" size="small" @click="print">打印</el-button>
           </div>

  <div id="printMe">
        <div class="competitionTit" style="padding: 52px 0 22px 8px;">
            <div ><span style="visibility: hidden;">真真真真真真真真真真真真真真真真</span>湖南建设投资集团第二届商务成控综合技能竞赛{{examinationroomTit}}参赛人员座次表</div>
        </div>
        <div style="display: flex;flex-wrap: wrap;width: 100%;padding: 8px 0 12px 30px; " >
            <div style="width: 9.2%;padding: 2px; border: 1px solid #000;text-align: center;color:#666666;font-size: 14px;" v-for="(item,index1) in examinationRoomList" :key="index1">
                <div style="padding: 12px 0;">座位号：{{item.seatNumber}}号</div>
                 <img style="padding: 6px 0;" :src="item.pictureUrl" width="100px" height="130px" class="avatar">
                 <div style="padding: 6px 0;">姓名：{{item.examineeName}}</div>
                 <div style="padding: 6px 0px;">签字：<span style="visibility: hidden;">姚真真</span></div>
                 <div style="padding: 6px 0;">{{item.phone}}</div>
            </div>

        </div>

  </div>
</div>
</template>

<script>

import { submitExpert,getInfo,saveExpert,checkIdentityCardId,saveExam,repaceWord,getlistByRoom } from "@/api/expert/expert";
import axios from 'axios'
export default {
  name: "competition-certificate",

  data() {

    return {
      formData: {
        identityCardId: "",
        pictureUrl: "",
      }, //form表单数据
      dialogVisible:false,
      iframeUrls:'',
      name:'湖南建筑高级技工学校崇知楼二楼',
      place:'长沙市天心区古堆山巷11号北门',
      examTime:'2024-11-29 14:30-- 17:30',
      examinationroom:'01',
      examinationroomTit:'第1考场',
      examinationRoomList: [],
        options: [{
          value: '01',
          label: '第1考场'
        }, {
          value: '02',
          label: '第2考场'
        }, {
          value: '03',
          label: '第3考场'
        }, {
          value: '04',
          label: '第4考场'
        }, {
          value: '05',
          label: '第5考场'
        }, {
          value: '06',
          label: '第6考场'
        }, {
          value: '07',
          label: '第7考场'
        }],
      rules: {
        pictureUrl: [
          {
            required: true,
            message: "请上传本人寸照",
          },
        ],
        identityCardId: [
          {
            required: true,
            message: "身份证不能为空",
          },
        ],

      },
      isSubmit: false,
    };
  },
  created() {
       this.getlistByRoomFn(this.examinationroom)

  },
  methods: {
      print() {
      let printWindow = window.open('', '_blank');
      const printContent = document.getElementById('printMe').innerHTML;
      printWindow.document.write(printContent);
      printWindow.document.close();
      printWindow.focus();
      printWindow.print();
      printWindow.close();
    },
       handleChange(value) {
      console.log('当前选中的值：', value);
      // 在这里处理选项变化的逻辑
    },

   seatingQuery(){
        this.getlistByRoomFn(this.examinationroom);
   },

    //获取座次表
    async getlistByRoomFn(id) {
        const res = await getlistByRoom(id);
        this.examinationRoomList=res.data
        const selectedOption = this.options.find(option => option.value === this.examinationroom);
        this.examinationroomTit=selectedOption ? selectedOption.label : '';
      },




  },
  components: {

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
.competitionTit {
    // max-width: fit-content;
    // margin-left: auto;
    // margin-right: auto;
    color: #2b4acb;
    font-size: 22px;
    font-weight: 600;
    margin-top: 12px;

}
.competitionName {
    max-width: fit-content;
    margin-left: auto;
    margin-right: auto;
    color: #666666;
    font-size: 18px;
    font-weight: 550;

}
.app-container {
  overflow: auto; /* 需要滚动时保持这个属性 */
  scrollbar-width: none; /* 对于Firefox */
}
.scroll-container::-webkit-scrollbar {
  display: none; /* 对于Chrome, Safari和Opera */

}
.dialogtext {
  width: 100% !important;
  height: 100% !important;
}
  iframe {
    border: none;
    width: 100%;
  }
  @media print {

  #printMe {
    page-break-after: always;
    //  size: portrait;//纵向
      size: landscape;//横向
      text-align: center;
  }
  body {
    padding: 0;
    margin: 0;
    font-size: 12px; /* 根据需要调整字体大小 */
  }
  /* 其他打印样式 */
}
</style>
