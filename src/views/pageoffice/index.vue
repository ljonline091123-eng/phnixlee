<template>
  <el-container>
    <el-main>
      <div class="doc">
        <div
          style="width: 95%; height: 95%; position: absolute"
          v-html="poHtmlCode"
        ></div>
      </div>
    </el-main>
  </el-container>
</template>

<style>
#doc {
  position: absolute;
  width: 100%;
  height: 100%;
}
</style>

<script>
const axios = require("axios");
export default {
  name: "DocView",
  data() {
    return {
      poHtmlCode: "",
      newUrl: null,
    };
  },
  created: function () {
    let paramStr = window.external.UserParams;
    const storage = this.extractContent(paramStr)
    console.log("传过来的参数是:" + paramStr.substring(0,paramStr.length-2),storage.content);
    let url = window.btoa(encodeURIComponent(paramStr.substring(0,paramStr.length-2)));
    // 请求后端项目打开文件的controller方法
    axios
      .post(
        process.env.VUE_APP_BASE_API +
          "/business/agreementTemp/openFile?fileUrl=" +
          url+'&limitEdit='+storage.content
      )
      .then((response) => {
        this.poHtmlCode = response.data;
      })
      .catch(function (err) {});
  },
  methods: {
    extractContent(url) {
      // 匹配最后一个.后面的内容和/后面的内容
      const regex = /.*\.([^./]+)\/([^/]+)$/
      const match = url.match(regex);
      if (match) {
        return {
          suffix: match[1],
          content: match[2],
        };
      } else {
        return null;
      }
    },
    OnPageOfficeCtrlInit() {
      pageofficectrl.Titlebar = false;
      // PageOffice的初始化事件回调函数，您可以在这里添加自定义按钮
    },
    AfterDocumentOpened() {
      // PageOffice的文档打开后事件回调函数
    },
    BeforeDocumentSaved() {
      // PageOffice的文档保存前事件回调函数
    },
    AfterDocumentSaved() {
      // PageOffice的文档保存后事件回调函数
      let result = pageofficectrl.CustomSaveResult;
      this.newUrl = result;
      updateParent(this.newUrl);
    },

    //文档关闭前需要执行的逻辑
    OnBeforeBrowserClosed() {
      updateParent(this.newUrl);
    },
  },
  mounted: function () {
    // 以下的为PageOffice事件的回调函数，名称不能改，否则PageOffice控件调用不到
    window.OnPageOfficeCtrlInit = this.OnPageOfficeCtrlInit;
    window.AfterDocumentOpened = this.AfterDocumentOpened;
    window.BeforeDocumentSaved = this.BeforeDocumentSaved;
    window.AfterDocumentSaved = this.AfterDocumentSaved;
  },
};

function updateParent(newUrl) {
  //newUrl = JSON.stringify(newUrl);
  let url = window.btoa(encodeURIComponent(newUrl));
  window.external.CallParentFunc("updateUrl('" + url + "');");
}
</script>
