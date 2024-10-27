<template>
  <div id="map-container">
    <div id="container"></div>
    <!-- 个人中心按钮，位于右上角 -->
    <div class="user-profile">
      <router-link :to="`/user/${calculateInput.userId}`">
        <button>个人中心</button>
      </router-link>
    </div>


    <!-- 路线管理部分，左上角展示已计算的路线信息 -->
    <div class="route-management">
      <button @click="toggleRoadStatus">{{ showRoadStatus ? 'Hide' : 'Show' }} Road Status</button>
      <h3>Calculate Route</h3>
      <form @submit.prevent="calculateRoute">
        <!-- 用户 ID 输入框 -->
        <input
            v-model="calculateInput.userId"
            disabled
            placeholder="User ID"
            :readonly="isUserIdReadonly"
            required
        />

        <!-- 隐形起点输入框 -->
        <input
            type="hidden"
            v-model="calculateInput.startId"
        />

        <!-- 起点输入框，支持动态匹配 -->
        <input
            v-model="startInput"
            @input="searchStartPoints"
            placeholder="Startpoint"
            required
        />
        <ul v-if="startSuggestions.length" class="suggestions-list">
          <li v-for="suggestion in startSuggestions" :key="suggestion.id" @click="selectStartPoint(suggestion)">
            {{ suggestion.name }}
          </li>
        </ul>

        <!-- 隐形终点输入框 -->
        <input
            type="hidden"
            v-model="calculateInput.endId"
        />

        <!-- 终点输入框，支持动态匹配 -->
        <input
            v-model="endInput"
            @input="searchEndPoints"
            placeholder="Endpoint"
            required
        />
        <ul v-if="endSuggestions.length" class="suggestions-list">
          <li v-for="suggestion in endSuggestions" :key="suggestion.id" @click="selectEndPoint(suggestion)">
            {{ suggestion.name }}
          </li>
        </ul>
      </form>

      <form @submit.prevent="outputAndDrawRoute">
        <!-- 其他输入框保持不变 -->
        <button type="submit">Calculate Route</button>
      </form>


      <!-- 显示路线绘制完成的提示 -->
      <p v-if="calculatedRoute" class="calculated-info">{{ calculatedRoute }}</p>
      <p v-if="calcError" class="error">{{ calcError }}</p>
      <p v-if="showPendingMessage" class="pending-info">计算尚未完成，请稍候...</p>


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
        showRoadStatus: false,
        movingIcon: null, // Store moving icon reference
        currentSegmentIndex: 0, // Track current segment index for movement
      },
      startInput: '',  // 用于显示用户输入的起点名称
      endInput: '',    // 用于显示用户输入的终点名称
      startSuggestions: [],
      endSuggestions: [],
      calculatedRoute: '',
      calcError: '',
      routeData: null,       // 用于存储计算后的路线数据
      isUserIdReadonly: false,
      isLoading: false, // 添加加载状态
      isCalculationComplete: false, // 添加计算完成状态
      map: null,
      polyline: null,
      showPendingMessage: false, // 新增变量控制待处理消息的显示
    };
  },
  mounted() {
    this.initMap();
    this.setUserIdFromUrl(); // 设置用户 ID
    this.initWebSocket(); // Initialize WebSocket connection on mount
  },
  methods: {
    setUserIdFromUrl() {
      const url = window.location.href;
      const match = url.match(/\/(\d+)$/); // 匹配最后的数字 ID
      if (match) {
        this.calculateInput.userId = match[1]; // 将 ID 填入 userId 输入框
        this.isUserIdReadonly = true; // 将输入框设为只读
      }
    },
    initMap() {
      const script = document.createElement('script');
      script.src = 'https://webapi.amap.com/maps?v=1.4.15&key=73f31edb64d7baefbc909c8bac5b839f';
      script.onload = () => {
        this.initializeMap();
      };
      document.head.appendChild(script);
    },
    initWebSocket() {
      const socket = new WebSocket('ws://localhost:8080/traffic-updates');

      socket.onmessage = (event) => {
        const update = JSON.parse(event.data);
        this.updateRouteTime(update);
      };

      socket.onerror = (error) => console.error('WebSocket error:', error);
    },

    updateRouteTime(update) {
      // Logic to update estimated time for users on affected routes.
      update.routeId = undefined;
      if (update.routeId === this.calculateInput.startId || update.routeId === this.calculateInput.endId) {
        // Update logic here based on received update data.
        console.log('Route time updated:', update);

        // You might want to notify users or refresh their route information here.
      }
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
    searchStartPoints() {
      if (this.startInput) {
        axios.get(`http://localhost:8080/api/roads/name?name=${this.startInput}`)
            .then(response => {
              this.startSuggestions = response.data;  // 期望返回的是道路数组
            })
            .catch(error => {
              console.error('Error fetching start points:', error.response ? error.response.data : error);
            });
      } else {
        this.startSuggestions = [];
      }
    },
    searchEndPoints() {
      if (this.endInput) {
        axios.get(`http://localhost:8080/api/roads/name?name=${this.endInput}`)
            .then(response => {
              this.endSuggestions = response.data;  // 期望返回的是道路数组
            })
            .catch(error => {
              console.error('Error fetching end points:', error.response ? error.response.data : error);
            });
      } else {
        this.endSuggestions = [];
      }
    },
    selectStartPoint(suggestion) {
      this.calculateInput.startId = suggestion.id;  // 将 ID 存储到隐形输入框
      this.startInput = suggestion.name;              // 将名称填入可见输入框
      this.startSuggestions = [];  // 清空建议列表

    },
    selectEndPoint(suggestion) {
      this.calculateInput.endId = suggestion.id;  // 将 ID 存储到隐形输入框
      this.endInput = suggestion.name;              // 将名称填入可见输入框
      this.endSuggestions = [];  // 清空建议列表

      // 选择终点后立即计算路线
      this.calculateRoute();
    },
    // 其他方法保持不变
    calculateRoute() {
      this.calcError = '';
      this.calculatedRoute = '';
      this.showPendingMessage = true; // 显示计算提示

      // 调用后端计算路线并将数据存储到 routeData 中
      axios.post('http://localhost:8080/api/routes/calculate', this.calculateInput)
          .then(response => {
            // 存储路线数据
            this.routeData = response.data;
            this.isCalculationComplete = true; // 计算完成
            this.showPendingMessage = false;
          })
          .catch(error => {
            console.error('Error calculating route:', error.response ? error.response.data : error);
            this.calcError = '计算路线失败，请重试。';
            this.isCalculationComplete = false; // 计算失败
            this.showPendingMessage = false; // 隐藏计算提示
          })
          .finally(() => {
            this.isLoading = false; // 结束加载
          });
      // 2秒后自动隐藏消息
      setTimeout(() => {
        this.showPendingMessage = false; // 隐藏计算消息
      }, 2000);
    },
    // 在点击按钮时输出结果和绘制线条
    outputAndDrawRoute() {
      if (this.isCalculationComplete && this.routeData && this.routeData.pathData) {
        this.drawRoute(this.routeData.pathData);
        const roundedDuration = Math.round(this.routeData.duration);
        this.calculatedRoute = `路线绘制完成，预计时间：${roundedDuration} 分钟`;
      }
    },

    drawRoute(pathData) {
      if (this.polyline) {
        this.polyline.setMap(null);
      }

      const routePath = pathData.flatMap((segment) => {
        return [
          [segment.startLong, segment.startLat],
          [segment.endLong, segment.endLat]
        ];
      });

      this.polyline = new AMap.Polyline({
        path: routePath,
        borderWeight: 6,
        strokeColor: '#33A1C9',
        strokeOpacity: 0.8,
        strokeWeight: 5,
        lineJoin: 'round',
        strokeStyle: 'solid',
      });

      this.polyline.setMap(this.map);
      this.map.setFitView([this.polyline]);
      this.startMovingIcon(pathData); // Start moving icon after drawing route
    },
    startMovingIcon(pathData) {
      if (this.movingIcon) {
        this.movingIcon.setMap(null); // Remove previous icon if exists
      }

      this.movingIcon = new AMap.Marker({
        position: [pathData[0].startLong, pathData[0].startLat],
        icon: 'path/to/icon.png', // Path to your moving icon image
        map: this.map,
      });

      this.moveAlongRoute(pathData);
    },

    moveAlongRoute(pathData) {
      const interval = setInterval(() => {
        if (this.currentSegmentIndex < pathData.length) {
          const segment = pathData[this.currentSegmentIndex];
          const nextPosition = [segment.endLong, segment.endLat];

          this.movingIcon.setPosition(nextPosition);
          this.uploadTrafficData(segment); // Upload traffic data for current segment

          this.currentSegmentIndex++;
        } else {
          clearInterval(interval); // Stop when route is completed
        }
      }, 1000); // Adjust speed here (in milliseconds)
    },

    uploadRouteData(segment) {
      const routeData = {
        segmentId: segment.id, // Assuming each segment has an ID
        startLong: segment.startLong,
        startLat: segment.startLat,
        endLong: segment.endLong,
        endLat: segment.endLat,
        currentStatus: this.getCurrentRouteStatus(segment), // Function to determine current status
        timestamp: new Date().toISOString(), // Current timestamp
      };

     // Send traffic data to backend
      axios.post('http://localhost:8080/api/route/upload', routeData)
        .then(response => console.log('Route data uploaded:', response))
        .catch(error => console.error('Error uploading route data:', error));
    },

    toggleRoadStatus() {
      this.showRoadStatus = !this.showRoadStatus;
      if (this.showRoadStatus) {
        this.displayRoadStatus(); // Fetch and display road statuses
      } else {
        this.clearRoadStatus(); // Clear displayed statuses
      }
    },

    displayRandomRoadStatus() {
      const roads = this.generateRandomRoadSegments(5); // Generate up to 5 random road segments
      roads.forEach(road => {
        const polyline = new AMap.Polyline({
          path: road.coordinates, // Assuming coordinates are provided as an array of [lng, lat]
          strokeColor: road.color,
          strokeWeight: 6,
        });
      polyline.setMap(this.map);
      });
    },

    clearRoadStatus() {
      this.map.clearMap(); // Clear all overlays from the map
    },

  generateRandomRoadSegments(numSegments) {
    const segments = [];
    for (let i = 0; i < numSegments; i++) {
      const segment = {
        coordinates: this.getRandomCoordinates(), // Generate random coordinates for this segment
        color: this.getRandomColor(), // Assign a random color based on congestion level
      };
      segments.push(segment);
    }
    return segments;
  },

  getRandomCoordinates() {
    // Generate random coordinates (example values)
    const startLong = Math.random() * (116.5 - 116.3) + 116.3; // Random longitude
    const startLat = Math.random() * (40.0 - 39.8) + 39.8; // Random latitude
    const endLong = Math.random() * (116.5 - 116.3) + 116.3; // Random longitude
    const endLat = Math.random() * (40.0 - 39.8) + 39.8; // Random latitude

    return [
      [startLong, startLat],
      [endLong, endLat]
    ];
  },

   getRandomColor() {
      const colors = ['#FF0000', '#FFA500', '#008000']; // Red, Orange, Green
      return colors[Math.floor(Math.random() * colors.length)];
    },
  },
};
</script>

<style scoped>
#container {
  width: 100%;
  height: 100vh;
  position: relative;
  margin: 0;
  padding: 0;
}

.route-management {
  position: absolute;
  top: 10px;
  left: 10px;
  background-color: white;
  padding: 6px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  z-index: 1000;
  font-size: 12px;
  width: 260px;
}

/* 个人中心按钮样式 */
.user-profile {
  position: absolute;
  top: 10px;
  right: 10px;
  z-index: 1000;
}

.user-profile button {
  padding: 6px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.user-profile button:hover {
  background-color: #0056b3;
}

input {
  padding: 6px;
  margin-bottom: 6px;
  border: 1px solid #ccc;
  border-radius: 4px;
  width: 90%;
  font-size: 12px;
}

button {
  padding: 6px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  width: 100%;
  font-size: 12px;
}

button:hover {
  background-color: #0056b3;
}

.suggestions-list {
  list-style-type: none;
  padding: 0;
  margin: 0;
  background-color: #fff;
  border: 1px solid #ddd;
  max-height: 150px;
  overflow-y: auto;
}

.suggestions-list li {
  padding: 8px;
  cursor: pointer;
}

.suggestions-list li:hover {
  background-color: #f0f0f0;
}

.calculated-info {
  margin-top: 8px;
  font-weight: bold;
}

.error {
  color: red;
  font-weight: bold;
  font-size: 12px;
}
.loading {
  color: orange; /* 加载提示的颜色 */
  font-weight: bold;
}
</style>