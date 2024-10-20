<template>
  <div id="map-container">
    <div id="container"></div>

    <!-- 路线管理部分，左上角展示已计算的路线信息 -->
    <div class="route-management">
      <h3>Calculate Route</h3>
      <form @submit.prevent="calculateRoute">
        <!-- 用户 ID 输入框 -->
        <input
            v-model="calculateInput.userId"
            placeholder="User ID"
            required
        />

        <!-- 起点输入框 -->
        <input
            v-model="calculateInput.startId"
            placeholder="Startpoint"
            required
        />

        <!-- 终点输入框 -->
        <input
            v-model="calculateInput.endId"
            placeholder="Endpoint"
            required
        />

        <!-- 优先级输入框 -->
        <input
            v-model="calculateInput.priority"
            type="number"
            placeholder="Priority"
            required
        />

        <button type="submit">Calculate Route</button>
      </form>

      <!-- 显示路线绘制完成的提示 -->
      <p v-if="calculatedRoute" class="calculated-info">{{ calculatedRoute }}</p>
      <p v-if="calcError" class="error">{{ calcError }}</p>
    </div>
  </div>
</template>

<script>
// eslint-disable-next-line no-undef
/* global AMap */
import axios from 'axios';

export default {
  name: 'MapWithNavigation',
  data() {
    return {
      calculateInput: {
        userId: '',
        startId: '',
        endId: '',
        priority: 0,
      },
      calculatedRoute: '',
      calcError: '',
      map: null,
      polyline: null,
    };
  },
  mounted() {
    this.initMap();
  },
  methods: {
    initMap() {
      const script = document.createElement('script');
      script.src = 'https://webapi.amap.com/maps?v=1.4.15&key=73f31edb64d7baefbc909c8bac5b839f';
      script.onload = () => {
        this.initializeMap();
      };
      document.head.appendChild(script);
    },
    initializeMap() {
      if (window.AMap) {
        this.map = new AMap.Map('container', {
          resizeEnable: true,
          zoom: 14,
          center: [116.397428, 39.90923],
        });
      } else {
        console.error('AMap is not defined');
      }
    },
    calculateRoute() {
      this.calcError = '';
      this.calculatedRoute = '';

      // 调用后端计算路线并绘制路径
      axios.post('http://localhost:8080/api/routes/calculate', this.calculateInput)
          .then(response => {
            // 如果后端返回了路径数据，则绘制路线
            if (response.data.pathData) {
              this.drawRoute(response.data.pathData);  // 使用 pathData 绘制路线
            }

            // 显示简短提示
            this.calculatedRoute = '路线绘制完成';

            // 重置输入框
            this.calculateInput = { userId: '', startId: '', endId: '', priority: 0 };
          })
          .catch(error => {
            console.error('Error calculating route:', error.response ? error.response.data : error);
            this.calcError = '计算路线失败，请重试。';
          });
    },
    drawRoute(pathData) {
      if (this.polyline) {
        this.polyline.setMap(null);  // 如果之前绘制过路线，清除它
      }

      // 构建路径点数组
      const routePath = pathData.flatMap((segment) => {
        return [
          [segment.startLong, segment.startLat],  // 起点经纬度
          [segment.endLong, segment.endLat]  // 终点经纬度
        ];
      });

      // 创建新的 Polyline（多段线）
      this.polyline = new AMap.Polyline({
        path: routePath,  // 路径数据
        borderWeight: 6,  // 边框宽度
        strokeColor: '#33A1C9',  // 线条颜色
        strokeOpacity: 0.8,  // 透明度
        strokeWeight: 5,  // 线条宽度
        lineJoin: 'round',  // 线条连接处样式
        strokeStyle: 'solid',  // 实线
      });

      // 设置路线显示在地图上
      this.polyline.setMap(this.map);

      // 自动缩放地图以适应路线
      this.map.setFitView([this.polyline]);
    },
  },
};
</script>

<style scoped>
#container {
  width: 100%;
  height: 100vh; /* 使用整个视口的高度来确保没有空白部分 */
  position: relative;
  margin: 0;
  padding: 0;
}

/* 将路线管理部分放在地图的左上角并缩小框 */
.route-management {
  position: absolute;
  top: 10px;
  left: 10px;
  background-color: white;
  padding: 6px;  /* 缩小 padding */
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  z-index: 1000;
  font-size: 12px; /* 缩小字体 */
  width: 260px;  /* 控制框的宽度 */
}

input {
  padding: 6px; /* 缩小 padding */
  margin-bottom: 6px; /* 缩小 margin */
  border: 1px solid #ccc;
  border-radius: 4px;
  width: 90%; /* 输入框宽度适应父容器 */
  font-size: 12px; /* 缩小字体 */
}

button {
  padding: 6px; /* 缩小 padding */
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  width: 100%;
  font-size: 12px; /* 缩小字体 */
}

button:hover {
  background-color: #0056b3;
}

.calculated-info {
  margin-top: 8px; /* 缩小 margin */
  font-weight: bold;
}

.error {
  color: red;
  font-weight: bold;
  font-size: 12px; /* 缩小字体 */
}
</style>
