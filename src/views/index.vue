<template>
  <div class="app-container home">
    <el-row :gutter="20">
      <el-col :sm="24" :lg="24">
        <a href="javascript:POBrowser.openWindow('/pageoffice','width=1800px;height=900px;','http://192.168.6.151:9010/wh-hnjt/2024/06/21/招标文件-钢材_20240621142106A089.docx ');">
          在线打开文档
        </a>
      </el-col>
    </el-row>
  </div>
</template>

<script>
    export default {
        name: "Index",
        data() {
            return {
                // 版本号
                version: "3.6.4",
            };
        },
        methods: {
            goTarget(href) {
                window.open(href, "_blank");
            },
        },
    };

    function updateCount(value) {
      alert(value);
    }
</script>

<style scoped lang="scss">
  .home {
    blockquote {
      padding: 10px 20px;
      margin: 0 0 20px;
      font-size: 17.5px;
      border-left: 5px solid #eee;
    }

    hr {
      margin-top: 20px;
      margin-bottom: 20px;
      border: 0;
      border-top: 1px solid #eee;
    }

    .col-item {
      margin-bottom: 20px;
    }

    ul {
      padding: 0;
      margin: 0;
    }

    font-family: "open sans", "Helvetica Neue", Helvetica, Arial, sans-serif;
    font-size: 13px;
    color: #676a6c;
    overflow-x: hidden;

    ul {
      list-style-type: none;
    }

    h4 {
      margin-top: 0px;
    }

    h2 {
      margin-top: 10px;
      font-size: 26px;
      font-weight: 100;
    }

    p {
      margin-top: 10px;

      b {
        font-weight: 700;
      }
    }

    .update-log {
      ol {
        display: block;
        list-style-type: decimal;
        margin-block-start: 1em;
        margin-block-end: 1em;
        margin-inline-start: 0;
        margin-inline-end: 0;
        padding-inline-start: 40px;
      }
    }
  }
</style>

