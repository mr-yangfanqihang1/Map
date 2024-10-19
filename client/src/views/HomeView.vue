<template>
  <div id="map-container">
    <div id="container"></div>

    <!-- 现有道路状态和路线管理部分 -->
    <div class="road-status">
      <input v-model="roadId" placeholder="Enter road ID" />
      <button @click="getRoadStatus">Get Road Status</button>
      <p v-if="roadStatus">Road Status: {{ roadStatus }}</p>
      <p v-if="error" class="error">{{ error }}</p>
    </div>

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
      <p v-if="calculatedRoute">{{ calculatedRoute }}</p>
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
      roadId: '',
      roadStatus: '',
      error: '',
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
    getRoadStatus() {
      if (!this.roadId || isNaN(this.roadId)) {
        this.error = 'Please enter a valid road ID';
        return;
      }
      this.error = '';
      this.roadStatus = '';

      axios.get(`http://localhost:8080/api/roads/status/${this.roadId}`)
          .then(response => {
            this.roadStatus = response.data.status;
          })
          .catch(error => {
            console.error('Error fetching road status:', error.response ? error.response.data : error);
            this.error = 'Failed to fetch road status. Please try again.';
          });
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
  height: 600px;
}

.road-status, .route-management {
  margin-top: 20px;
}

input {
  padding: 10px;
  margin-right: 10px;
  border: 1px solid #ccc;
  border-radius: 4px;
}

button {
  padding: 10px 15px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

button:hover {
  background-color: #0056b3;
}

.error {
  color: red;
  font-weight: bold;
}
</style>
