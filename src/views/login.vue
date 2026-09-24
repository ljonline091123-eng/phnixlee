<template>
<div class="login_context">
   <div >
          <img src="@/assets/images/login_tag.png" alt="" style="margin-top: 74px;margin-left: 85px;" >
        </div>

  <div style="display: flex;">
   <div class="logo-img">
          <img src="@/assets/images/login_name.png" alt="" style="position: absolute;width:40%;height:112px;top: 37%;margin-left: 17%;" >
        </div>
  <div class="login">
    <el-form ref="loginForm" :model="loginForm" :rules="loginRules" class="login-form">
      <div style="display: flex;justify-content: center;align-items: center;margin-bottom: 30px;">
         <img src="@/assets/images/login_mes.png" alt="" style="width:20px;height:20px;" >
         <div style="font-weight: 400;font-size: 18px;color: #2B4ACB;">欢迎使用招标采购管理平台！</div>
      </div>

    <el-tabs v-model="activeName" @tab-click="handleClick">
      <el-tab-pane label="帐号登录" name="user"></el-tab-pane>
      <el-tab-pane label="验证码登录" name="code"></el-tab-pane>
    </el-tabs>
      <el-form-item prop="username">
        <el-input
          v-model="loginForm.username"
          type="text"
          auto-complete="off"
          placeholder="账号"
        >
          <svg-icon slot="prefix" icon-class="user" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="password"  v-if="activeName=='user'">
        <el-input
          v-model="loginForm.password"
          type="password"
          auto-complete="off"
          placeholder="密码"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="password" class="el-input__icon input-icon" />
        </el-input>
      </el-form-item>
      <el-form-item prop="code" v-if="captchaEnabled">
        <el-input
          v-model="loginForm.code"
          auto-complete="off"
          placeholder="验证码"
          style="width: 63%"
          @keyup.enter.native="handleLogin"
        >
          <svg-icon slot="prefix" icon-class="validCode" class="el-input__icon input-icon" />
        </el-input>
        <div class="login-code" @click="getCode" title="点击刷新验证码">

          <img v-if="codeUrl" :src="codeUrl" class="login-code-img" alt="验证码" />
        </div>
      </el-form-item>
<!--      <el-checkbox v-model="loginForm.userType" style="margin:5px 0px 25px 0px;">专家</el-checkbox>-->
      <!-- <el-checkbox v-model="loginForm.rememberMe" style="margin:0px 0px 25px 0px;">记住密码</el-checkbox> -->
      <el-form-item style="width:100%;height: 34px;margin-top: 15px;background: #2B4ACB;border-radius: 2px 2px 2px 2px;">
        <el-button
          :loading="loading"
          size="medium"
          type="primary"
          style="width:100%;"
          @click.native.prevent="handleLogin"
        >
          <span v-if="!loading">登 录</span>
          <span v-else>登 录 中...</span>
        </el-button>
        <div style="float: right;" v-if="register">
          <router-link class="link-type" :to="'/register'">立即注册</router-link>
        </div>
      </el-form-item>
    </el-form>
    <!--  底部  -->
    <div class="el-login-footer">
      <span>Copyright © 2018-2024 ruoyi.vip All Rights Reserved.</span>
    </div>
  </div>
  </div>
  </div>
</template>

<script>
import { getCodeImg } from "@/api/login";
import Cookies from "js-cookie";
import { encrypt, decrypt } from '@/utils/jsencrypt'
import appConstant from "@/appConstant"

export default {
  name: "Login",
  data() {
    return {
      codeUrl: "",
      activeName: 'user',
      isButtonDisabled: false, // 是否开启了倒计时
      secondsLeft: 60, // 倒计时60s
      countdown: null, // 定时器开关

      loginForm: {
        username: "",
        password: "",
        userType:'',
        rememberMe: false,
        code: "",
        uuid: ""
      },
      loginRules: {
        username: [
          { required: true, trigger: "blur", message: "请输入您的账号" }
        ],
        password: [
          { required: true, trigger: "blur", message: "请输入您的密码" }
        ],
        code: [{ required: true, trigger: "change", message: "请输入验证码" }]
      },
      loading: false,
      // 验证码开关
      captchaEnabled: true,
      // 注册开关
      register: false,
      redirect: undefined
    };
  },
  watch: {
    $route: {
      handler: function(route) {
        this.redirect = route.query && route.query.redirect;
      },
      immediate: true
    }
  },
  created() {
    this.getCode();
    this.getCookie();
  },
  methods: {
    handleClick(){

    },
    getMessageCode() {
          if (this.isButtonDisabled) {
            return
          }

          // 调用 API 获取验证码
          // this.$api.project.getMessageCode().then((res) => {
          //   if (res.code === '00000') {
          //     this.$message.success('获取验证码成功')
          //   }
          // })

          // 禁用按钮并开始倒计时
          this.isButtonDisabled = true
          this.secondsLeft = 60

          this.countdown = setInterval(() => {
            this.secondsLeft--
            if (this.secondsLeft <= 0) {
              clearInterval(this.countdown)
              this.isButtonDisabled = false
            }
          }, 1000)
        },

    getCode() {
      getCodeImg().then(res => {
        this.captchaEnabled = res.captchaEnabled === undefined ? true : res.captchaEnabled;
        if (this.captchaEnabled) {
          this.codeUrl = "data:image/gif;base64," + res.img;
          this.loginForm.uuid = res.uuid;
        }
      });
    },
    getCookie() {
      const username = Cookies.get("username");
      const password = Cookies.get("password");
      const rememberMe = Cookies.get('rememberMe')
      this.loginForm = {
        // username: username === undefined ? this.loginForm.username : username,
        // password: password === undefined ? this.loginForm.password : decrypt(password),
        // rememberMe: rememberMe === undefined ? false : Boolean(rememberMe)
        username: '',
        password: '',
        rememberMe: false
      };
    },
    handleLogin() {
      this.$refs.loginForm.validate(valid => {
        if (valid) {
          this.loading = true;
          if (this.loginForm.rememberMe) {
            Cookies.set("username", this.loginForm.username, { expires: 30 });
            Cookies.set("password", encrypt(this.loginForm.password), { expires: 30 });
            Cookies.set('rememberMe', this.loginForm.rememberMe, { expires: 30 });
          } else {
            Cookies.remove("username");
            Cookies.remove("password");
            Cookies.remove('rememberMe');
          }
          this.$store.dispatch("Login", this.loginForm).then(() => {

            if(this.loginForm.userType){
                this.$router.push({ path: '/evaluate-expert/evaluate-bids' || "/" }).catch(()=>{});
                appConstant.platform='2'
            }else{
                // this.$router.push({ path: '/procurement/plan' || "/" }).catch(()=>{});
                this.$router.push({ path: "/" }).catch(()=>{});
                  appConstant.platform='1'
           }

            // this.$router.push({ path: this.redirect || "/" }).catch(()=>{});


          }).catch(() => {
            this.loading = false;
            if (this.captchaEnabled) {
              this.getCode();
            }
          });
        }
      });
    }
  }
};
</script>

<style rel="stylesheet/scss" lang="scss">
.login_context {
 height: 100%;
 width: 100%;
  background-image: url("../assets/images/login_bg.png");
  background-size:100% 100%;
  // position: absolute;
  font-family: Source Han Sans CN, Source Han Sans CN;
}
.login {
  display: flex;
  justify-content: center;
  align-items: center;


}
.title {
  margin: 0px auto 30px auto;
  text-align: center;
  color: #707070;

}

.login-form {

     position: absolute;
    width: 360px;
    // height: 382px;
    background: #C4E8F8;
    border-radius: 14px 14px 14px 14px;
    // opacity: 0.5;
    // margin-left: 230px;
    left: 68%;
    top: 23%;
    padding: 48px 39px 30px 39px;

  // border-radius: 6px;
  // background: #ffffff;
  // width: 400px;
  // padding: 25px 25px 5px 25px;
  .el-input {
    height: 38px;
    input {
      height: 38px;
    }
  }
  .input-icon {
    height: 39px;
    width: 14px;
    margin-left: 2px;
  }
}
.login-tip {
  font-size: 13px;
  text-align: center;
  color: #bfbfbf;
}
.login-code {
  width: 33%;
  height: 38px;
  float: right;
  img {
    cursor: pointer;
    vertical-align: middle;
  }
}
.el-login-footer {
  height: 40px;
  line-height: 40px;
  position: fixed;
  bottom: 0;
  width: 100%;
  text-align: center;
  color: #fff;
  font-family: Arial;
  font-size: 12px;
  letter-spacing: 1px;
}
.login-code-img {
  height: 38px;
}
.el-tabs__item.is-active {
  color: #2B4ACB;
}
.el-tabs__active-bar {
  background-color: #2B4ACB;
}
.el-tabs__nav-wrap::after {
  background-color: transparent;
}
.el-button--primary {
  background-color: #2B4ACB;
    border-color: #2B4ACB;
}
</style>
