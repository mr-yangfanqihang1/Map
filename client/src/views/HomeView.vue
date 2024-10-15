<template>
  <div id="map-container">
    <button @click="goToUserCenter" class="user-center-btn">个人中心</button>

    <div class="controls">
      <div class="input-group">
        <label for="start-point">起点:</label>
        <input type="text" v-model="startPointName" placeholder="请输入起点">

        <label for="end-point">终点:</label>
        <input type="text" v-model="endPointName" placeholder="请输入终点">

        <button @click="plotRoute" :disabled="loading" class="route-button">规划路线</button>
        <span v-if="loading" class="loading-text">正在加载...</span>
      </div>
    </div>

    <div id="container"></div>
  </div>
</template>

<script>
// eslint-disable-next-line no-undef
/* global AMap */

export default {
  name: 'MapWithNavigation',
  data() {
    return {
      startPointName: '',
      endPointName: '',
      map: null,
      geocoder: null,
      driving: null,
      loading: false,
    };
  },
  mounted() {
    this.initMap();
  },
  methods: {
    initMap() {
      const script = document.createElement('script');
      script.src = 'https://webapi.amap.com/maps?v=1.4.15&key=73f31edb64d7baefbc909c8bac5b839f';
      script.onload = this.initializeMap;
      document.head.appendChild(script);
    },
    initializeMap() {
      this.map = new AMap.Map('container', {
        resizeEnable: true,
        zoom: 14,
        center: [116.397428, 39.90923], // Tiananmen, Beijing coordinates
      });

      AMap.plugin('AMap.Geocoder', () => {
        this.geocoder = new AMap.Geocoder({
          city: "010", // City code for Beijing
        });
      });

      AMap.plugin('AMap.Driving', () => {
        this.driving = new AMap.Driving({
          map: this.map,
        });
      });
    },
    plotRoute() {
      if (this.startPointName && this.endPointName) {
        this.loading = true;
        this.geocoder.getLocation(this.startPointName, (status, result) => {
          if (status === 'complete' && result.geocodes.length) {
            const startLngLat = result.geocodes[0].location;

            this.geocoder.getLocation(this.endPointName, (status, result) => {
              if (status === 'complete' && result.geocodes.length) {
                const endLngLat = result.geocodes[0].location;

                this.driving.search(startLngLat, endLngLat, (status, result) => {
                  this.loading = false;
                  if (status === 'complete' && result.routes && result.routes.length) {
                    console.log('Route successfully plotted');
                    this.map.setFitView();
                  } else {
                    console.error('Failed to plot route: ' + status);
                    alert('无法规划路线，请检查起点和终点');
                  }
                });
              } else {
                this.loading = false;
                alert('无法找到终点');
                console.log('Failed to find end point: ' + status);
              }
            });
          } else {
            this.loading = false;
            alert('无法找到起点');
            console.log('Failed to find start point: ' + status);
          }
        });
      } else {
        alert('请输入起点和终点');
      }
    },
    // 跳转到个人中心的方法
    goToUserCenter() {
      this.$router.push({ name: 'user' }); // 'UserCenter'是您的路由名称，请根据实际情况更改
    },
  },
};
</script>

<style scoped>
#map-container {
  position: relative; /* 使控制和地图相对定位 */
}

#container {
  width: 100%;
  height: 785px;
}

.user-center-btn {
  position: absolute; /* 绝对定位 */
  top: 10px; /* 距离上边距 */
  right: 10px; /* 距离右边距 */
  padding: 8px 12px; /* 按钮内边距 */
  background-color: #4CAF50; /* 按钮背景颜色 */
  color: white; /* 按钮文本颜色 */
  border: none; /* 去掉按钮边框 */
  border-radius: 4px; /* 按钮圆角 */
  cursor: pointer; /* 鼠标悬停时显示为指针 */
  z-index: 10; /* 确保按钮在最上方 */
}

.user-center-btn:hover {
  background-color: #45a049; /* 鼠标悬停时按钮颜色 */
}

.controls {
  display: flex;
  justify-content: flex-start; /* 左对齐 */
  align-items: flex-start; /* 顶部对齐 */
  position: absolute; /* 绝对定位到地图容器的左上角 */
  z-index: 5; /* 确保控件在地图之上，但在按钮之下 */
  margin-bottom: 10px; /* 为控件增加下边距 */
}

.input-group {
  display: flex;
  flex-direction: column; /* 垂直排列输入框 */
  margin-right: 10px; /* 输入组与用户中心按钮之间的间距 */
}

.input-group label {
  margin: 0 0 5px; /* 标签与输入框之间的间距 */
}

.input-group input {
  padding: 8px; /* 输入框的内边距 */
  border: 1px solid #ccc; /* 输入框边框 */
  border-radius: 4px; /* 输入框圆角 */
  width: 200px; /* 输入框宽度 */
}

.route-button {
  margin-top: 10px; /* 按钮与输入框之间的间距 */
  padding: 8px 12px; /* 按钮内边距 */
  background-color: #007BFF; /* 按钮背景颜色 */
  color: white; /* 按钮文本颜色 */
  border: none; /* 去掉按钮边框 */
  border-radius: 4px; /* 按钮圆角 */
  cursor: pointer; /* 鼠标悬停时显示为指针 */
}

.route-button:disabled {
  background-color: #ccc; /* 禁用状态的按钮颜色 */
  cursor: not-allowed; /* 禁用状态的光标 */
}

.route-button:hover:not(:disabled) {
  background-color: #0056b3; /* 鼠标悬停时按钮颜色 */
}

.loading-text {
  margin-top: 5px; /* 加载文本与按钮之间的间距 */
}
</style>
