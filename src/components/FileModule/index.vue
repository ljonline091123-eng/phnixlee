<template>
  <div
    :id="el"
    :style="`width:${width};height:${height};background-color:#f5f5f5`"
  >
    <!-- <iframe
      :src="`${this.offerService}/${this.offerRepo}/${this.attachmentId}/${this.type}/content?zdocs_access_token=${this.token}`"
      width="100%"
      height="100%"
      frameborder="0"
      allowfullscreen>
    </iframe> -->
  </div>
</template>
<script>
import { getToken } from "@/utils/auth";
import { offerService, offerRepo } from "@/utils/const";
export default {
  data() {
    return {
      offerService,
      offerRepo,
      token: getToken(),
      Application: null,
    };
  },
  props: {
    width: {
      type: String,
      default: "100%",
    },
    height: {
      type: String,
      default: "100%",
    },
    attachmentId: {
      type: String,
      default: "",
    },
    type: {
      type: String,
      default: "view",
    },
    el: {
      type: String,
      default: "file_box",
    },
    isContract: {
      type: Boolean,
      default: false,
    },
  },
  mounted() {
    this.getFile();
  },
  methods: {
    async getFile() {
      this.Application = await ZOfficeSDK.mount(
        `${this.offerService}/${this.offerRepo}/${this.attachmentId}/${this.type}/content?zdocs_access_token=${this.token}`,
        `#${this.el}`,
        true,
        {
          uiConfig: {
            // WordCommentBox: 'defaultInvisible', // 隐藏文档主体右侧批注区域
            // NaviPanel: 'defaultInvisible',
            // WordCommentMark: 'defaultInvisible',
            // WordRevisionMark: 'defaultInvisible',
            // WordTableGridLine: 'defaultInvisible',
            // WordGridline: 'defaultInvisible',
            // CarriageReturn: 'defaultInvisible',
            // Bookmark: 'defaultInvisible',
            // WordPermMark: 'defaultInvisible',
            // NaviPanel: 'defaultInvisible',
          },
        }
      );
      console.log(this.Application, "Application----Application");
      if (this.isContract) {
        this.Application.addListener("IDocs.Event.FileStatus", (data) => {
          console.log("FileStatus: ", data);
          this.$emit("submitFileZ", data);
        });
      }
    },
    async saveFile() {
      await this.Application.ActiveDocument.save();
    },
  },
};
</script>
<style lang="scss" scoped></style>
