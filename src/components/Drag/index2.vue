<template>
  <div :id="box" class="box">
    <div class="right_box">
      <div v-if="iframeIsHave" class="opacity_box"></div>
      <slot name="right-content"> </slot>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    box:{
      typeof: String,
      default: 'dragLayoutBox'
    },
    // 是否默认显示拖拽按钮
    dragBtnFlag: {
      typeof: Boolean,
      default: true
    },
    // 盒子中是否有iframme
    iframeIsHave: {
      typeof: Boolean,
      default: false
    },
    // 配置项 注意：色值暂时只支持16进制写法
    setCssProps: {
      typeof: Object,
      default: () => ({
        // 左右盒子最小拖动宽度
        leftMinWidth: '200px',
        rightMinWidth: '500px',
        dragBtnCss: {
          mousedown: {
            background: '#85a4ff',
            cursor: 'ew-resize'
          },
          onmouseup: {
            background: '#eee',
          },
          mousemove:{
            background: '#85a4ff',
            cursor: 'ew-resize'
          }
        }
      })
    }
  },
  mounted() {
    if (this.iframeIsHave) {
      this.changeBoxStyleFn('none')
    }
    this.dragChangeLayoutFn()
  },
  methods: {
    getDomFn(name) {
      console.log(name);
      const ele = document.querySelector(name)
      const ws = new WeakSet()
      ws.add(ele)
      return ele
    },
    changeBoxStyleFn(value) {
      if (this.iframeIsHave) {
        const opacityBox = this.getDomFn('.opacity_box')
        opacityBox.style.display = value
      }
    },
    dragChangeLayoutFn() {
      const that = this
      const dragBtn = this.getDomFn(`#${this.box} .drag_btn`)
      const left_box = this.getDomFn(`#${this.box} .left_box`)
      const right_box = this.getDomFn(`#${this.box} .right_box`)
      const dragLayoutBox = this.getDomFn(`#${this.box}`)
      dragBtn.onmousedown = function (e) {
        const styleStr = that.handleEleStyleFn(that.setCssProps.dragBtnCss.mousedown)
        dragBtn.style.cssText = `${styleStr}`
        const startPos = e.clientX
        dragBtn.left = dragBtn.offsetLeft
        document.onmousemove = e => {
          const styleStr = that.handleEleStyleFn(that.setCssProps.dragBtnCss.mousemove)
          dragBtn.style.cssText = `${styleStr}`
          const endPos = e.clientX
          let moveW = dragBtn.left - 20 + (endPos - startPos)
          const maxW = dragLayoutBox.clientWidth - dragBtn.offsetWidth
          // 左边区域的最小宽度
          const leftValue = parseInt(that.setCssProps.leftMinWidth)
          if (moveW < leftValue) {
            moveW = leftValue
          }
          // 右边区域最小宽度
          const rightValue = parseInt(that.setCssProps.rightMinWidth)
          if (moveW > maxW - rightValue) {
            moveW = maxW - rightValue
          }
          left_box.style.cssText = `width:${moveW}px`
          right_box.style.width = dragLayoutBox.clientWidth - moveW + 'px'
        }
        document.onmouseup = () => {
          const styleStr = that.handleEleStyleFn(that.setCssProps.dragBtnCss.onmouseup)
          dragBtn.style.cssText = `${styleStr}`
          document.onmousemove = document.onmouseup = null
          if (that.iframeIsHave) {
            that.changeBoxStyleFn('none')
          }
          dragBtn.releaseCapture && dragBtn.releaseCapture()
        }
        dragBtn.setCapture && dragBtn.setCapture()
        return false
      }
    },
    // 处理传来的对象：{k1:"v1",k2:"v2"} => "k1:v1;k2:v2"
    handleEleStyleFn(obj) {
      let str = JSON.stringify(obj)
      const indexstart = str.indexOf('{')
      const indexEnd = str.indexOf('}')
      str = str.slice(indexstart + 1).slice(0, indexEnd - 1)
      const reg1 = /,/g
      const reg2 = /"|'/g
      return str.replace(reg1, ';').replace(reg2, '')
    }
  }
}
</script>

<style scoped>
.box {
  width: 100%;
  display: flex;
  box-shadow: 0px 20px 40px 0px rgba(0, 0, 0, 0.08);
}
.left_box{
  width: 250px;
  position: relative;
}
.right_box{
  flex: 1 1 auto;
  overflow: hidden;
}
.drag_btn {
  position: absolute;
  top: 0;
  bottom: 0;
  right: -2px;
  width: 5px;
  height: 100%;
  cursor: col-resize;
  background: #eee;
  border-radius: 5px;
  background-size: cover;
  background-position: center;
  font-size: 32px;
  color: white;
  z-index: 1112;
  border-left: solid 2px #fff;
  border-right: solid 2px #fff;
  cursor: ew-resize;
}
.opacity_box {
  width: 100%;
  height: 100%;
  position: absolute;
  margin-top: 30px;
  filter: alpha(opacity=0);
  opacity: 0;
  background: transparent;
  z-index: 1111;
}
</style>
