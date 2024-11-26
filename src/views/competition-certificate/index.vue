<template>
  <div class="app-container">
        <div class="competitionTit" style="padding-top: 18px;">
            <div >湖南建设投资集团有限责任公司</div>
        </div>
        <div class="competitionTit">
            <div >第二届商务成控综合技能竞赛参赛证</div>
        </div>
     <div class="context">
      <el-form
        :model="formData"
        ref="form"
        :rules="rules"
        label-position="right"
        label-width="140px"
        size="medium"
        @submit.native.prevent
      >
        <div class="competitionTit" >
          <el-row :gutter="40">
            <el-col :span="24" class="grid-cell">
              <el-form-item
                label="身份证号码："
                prop="identityCardId"
                class="required label-right-align"
              >
                <el-input
                  v-model="formData.identityCardId"
                  type="text"
                  maxlength="18"
                  @input="handleInput"
                  clearable
                />
              </el-form-item>
            </el-col>
          </el-row>
         <el-row :gutter="40">
            <el-col :span="24" class="grid-cell">
              <el-form-item
                label="本人寸照："
                prop="pictureUrl"
                class="required label-right-align"
              >
                <!-- <el-upload
                    action="https://jsonplaceholder.typicode.com/posts/"
                    list-type="picture-card"
                    :on-preview="handlePictureCardPreview"
                    :on-remove="handleRemove">
                    <i class="el-icon-plus"></i>
                    </el-upload> -->
                     <el-upload
                      class="avatar-uploader"
                      action="/dev-api/file/upload"
                      :show-file-list="false"
                      :limit="1"
                      :on-success="handleAvatarSuccess"
                      :before-upload="beforeAvatarUpload"
                      :on-error="errorUpload"
                    >
                      <img v-if="formData.pictureUrl" :src="formData.pictureUrl" width="160px" height="180px" class="avatar">
                      <i v-else class="el-icon-plus avatar-uploader-icon"></i>
                    </el-upload>
              </el-form-item>
            </el-col>
          </el-row>

        </div>
      </el-form>
      <div class="competitionName" >
        <el-button style="margin-left: 70px;"   @click="saveForm('form')" type="primary">{{ isSubmit ? "生成参赛证..." : "生成参赛证" }}</el-button>
     <el-button v-if="iframeUrls" style="margin-left: 70px;"   @click="checkPdf()" type="primary">查看参赛证</el-button>
      </div>

      <el-form>
        <div style="margin-left: 10%;">
        <el-row :gutter="40">
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="考点名称："
                prop="presentUnitDept"
                class="required label-right-align"
              >
               {{name}}
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="考点地址："
                prop="email"
                class="required label-right-align"
              >
               {{place}}
              </el-form-item>
            </el-col>
            <el-col :span="8" class="grid-cell">
              <el-form-item
                label="考试时间："
                prop="presentUnitDept"
                class="required label-right-align"
              >
               {{examTime}}
              </el-form-item>
            </el-col>
          </el-row>
          </div>
      </el-form>
    </div>
      <div class="competitionName">
            <div >参赛人员须知</div>
        </div>
        <div style="margin-left: 10%;color:#666666;font-size: 14px;">
              <p>1.凭参赛证和身份证件原件进入考场，所持身份证件信息须与参赛证载明的信息一致。</p>
              <p>2.考试开始前15分钟可进入考场，考试开始15分钟后不得入场。入场后，须在“座次表”上签到，对号入座，保持安静，并将参赛证和身份证件放在考位桌面，接受监考人员核查。</p>
              <p>收到试卷后，检查试卷印制是否清晰、完整，如遇字迹模糊、缺页重页或者答题纸有折皱、污损等问题，请立即举手向监考人员报告。</p>
              <p>4.服从考场封闭管理，开考150分钟之内不得离开考场。考试开始和结束时间以考点统一信号为准。</p>
              <p>参加考试需携带黑色墨水笔、2B铅笔、橡皮、无声无文本编辑功能的计算器。严禁将手机、智能手表(手环)、蓝牙耳机等具有通信、记录、拍照、存储、传输功能的电子设备带至座位。</p>
              <p>6.答题纸和试卷上面都要写个人信息，考试过程中应妥善保管好自己的试卷，防止他人抄袭，考试结束后被甄别为雷同答卷的，将给予成绩无效处理。</p>
        </div>
        <div class="competitionName">
            <div >特别提示</div>
        </div>
         <div style="margin-left: 10%;color:#666666;font-size: 14px;">
            <p>考试当天考点学校有几批人员考试，交通干道可能出现拥堵，考生车辆不允许进入考点，请提前熟悉考点地址，合理规划好交通路线和出行方式。</p>
         </div>

          <!-- 预览弹窗 -->
    <el-dialog
      :show-close="true"
      :visible.sync="dialogVisible"
      title="参赛证"
      modal
      center
      :append-to-body="false"
      destroy-on-close
    >
      <!-- 直接用iframe嵌套pdf预览模式 "#toolbar=0"是为了隐藏pdf的按钮  -->
      <div class="dialogtext">
        <iframe width="800"  height="1200"   :src="this.iframeUrls + '#toolbar=0'" />
      </div>
    </el-dialog>
  </div>
</template>

<script>

import { submitExpert,getInfo,saveExpert,checkIdentityCardId,saveExam,repaceWord } from "@/api/expert/expert";
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
      planList: [],
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


  },
  methods: {
     downAttachment(file,uelFileName) {
        const a = document.createElement("a")
        a.href = file
        a.download = uelFileName
        a.target = "_blank"
        document.body.appendChild(a)
        a.click()
        document.body.removeChild(a)
    },

       //保存
    saveForm(formName){
      this.isSubmit = true;
      this.$refs[formName].validate(async (valid) => {
        if (valid) {
          console.log(this.formData, "this.formData");
          const loading = this.$loading({
            lock: true,
            text: "数据提交中...",
            background: "rgba(0, 0, 0, 0.7)",
          });
          try {
             const checkRes = await checkIdentityCardId(this.formData.identityCardId);
            //  if(checkRes.success){
                  await saveExam(this.formData);
                  const res = await repaceWord(this.formData.identityCardId);
                  // this.dialogVisible=true
                  this.iframeUrls=res.msg
                  this.checkPdf()
                  // this.downAttachment(this.iframeUrls,'参赛证')
            //  }else{
            //     this.$message.error("您输入的身份证码未在参赛人员信息名单中，请检查身份证号码是否输入错误!");
            //  }
          } catch (err) {
            console.log(err);
          }
          loading.close();
          this.isSubmit = false;
        } else {
          this.isSubmit = false;
          return false;
        }
      });
    },
    handleInput(value) {
      console.log('Input value changed to:', value);
      if(value.length==18){
          this.getCheckIdentityCardId(value)
      }
    },
    //查看身份证是否存在
    async getCheckIdentityCardId(id) {
        const res = await checkIdentityCardId(id);
      },
     checkPdf(url){
      //  this.dialogVisible=true
      //  this.downAttachment(this.iframeUrls,'参赛证')
      axios({
          method: 'get',
          responseType: 'blob',
          url: this.iframeUrls,
        })
          .then((res) => {
            console.log("88888")
            // 获取到文件后浏览器内打开，使用浏览器自带的预览文件功能
                let blob = new Blob([res.data], { type: "application/pdf" });
            // pdfurl即转化后的结果
            let pdfurl = window.URL.createObjectURL(blob);
            // 新标签页打开，即可预览并下载
                window.open(pdfurl);
            })
          .catch((err) => {});

    },
       errorUpload() {
      this.$message.error("图片插入失败");
    },
     beforeAvatarUpload (file) {
      const isImg = file.type === "image/jpeg" || file.type === "image/png"
      if (!isImg) {
      this.$message.success("上传头像图片只能是 JPG/PNG 格式！");
      }
      return isImg
    },
     handleAvatarSuccess (res, file) {
       if (res.code == 200) {
        this.formData.pictureUrl=res.data.url
         } else {
        this.$message.error("图片插入失败");
      }
}


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
    max-width: fit-content;
    margin-left: auto;
    margin-right: auto;
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
</style>
