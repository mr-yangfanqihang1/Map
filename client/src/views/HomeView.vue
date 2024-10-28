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
import SockJS from "sockjs-client";
import { Client } from "@stomp/stompjs";
export default {
  name: 'MapWithNavigation',
  data() {
    return {
      client:null,
      calculateInput: {
        userId: '',
        startId: '',
        endId: '',
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
      showRoadStatus: false,
      movingIcon: null,
      currentSegmentIndex: 0,
      pathData: [], // Store path data for movement
      isMoving: false,
      moveInterval: null, // Interval for moving the dot
    };
  },
  created() {
    this.setUserIdFromUrl(); // 设置用户 ID并在找到用户 ID 时连接 WebSocket
  },
  mounted() {
    this.initMap();
  },
  methods: {
    initWebSocket() {
    const userId = this.calculateInput.userId;
    
    // 使用 SockJS 作为传输层
    const socket = new SockJS("http://localhost:8080/ws");
    const client = new Client({
      webSocketFactory: () => socket, // 使用 SockJS
      connectHeaders: {
        login: "guest",
        passcode: "guest",
      },
      onConnect: () => {
        console.log('WebSocket connected');
        // 订阅用户专属的队列
        client.subscribe(`/user/${userId}/queue/roadUpdates`, (message) => {
          const update = JSON.parse(message.body);
          console.log(update);
          this.updateDuration(update.roadId, update.durationAdjustment);
        });
      },
      onStompError: (error) => {
        console.error('STOMP 错误:', error);
      },
      onWebSocketError: (error) => {
        console.error('WebSocket error:', error);
      },
      onWebSocketClose: () => {
        console.log('WebSocket closed. Attempting to reconnect...');
        setTimeout(() => this.initWebSocket(), 1000); // 自动重连机制
      }
    });

    client.activate();
  },

  updateDuration(roadId, durationAdjustment) {
    const road = this.routeData.find((r) => r.roadId === roadId);
    if (road) {
      road.duration += durationAdjustment;
      this.totalDuration += durationAdjustment;
    }
  },

  setUserIdFromUrl() {
    const url = window.location.href;
    const match = url.match(/\/(\d+)$/);
    if (match) {
      this.calculateInput.userId = match[1];
      this.isUserIdReadonly = true;
      console.log("connect to WebSocket");
      this.initWebSocket(); // 在设置用户 ID 后连接 WebSocket
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
              console.log(response.data);
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
      if (this.isCalculationComplete && this.routeData && this.routeData.routeData) {
        this.drawRoute(this.routeData.routeData);
        console.log(this.routeData);
        const roundedDuration = Math.round(this.routeData.duration);
        const roundedDistance = Math.round(this.routeData.distance);
        const roundedPrice = Math.round(this.routeData.price);
        this.calculatedRoute = `全程${roundedDistance}公里，预计时间：${roundedDuration} 分钟，共花费${roundedPrice} 元`;
      }
    },
    drawRoute(routeData) {
      this.pathData = routeData; // Store path data

      if (this.polyline) {
        this.polyline.setMap(null);
      }

      const routePath = routeData.flatMap((segment) => {
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

      this.startMovingIcon(); // Start moving icon after drawing route
    },

    startMovingIcon() {
      if (this.movingIcon) {
        this.movingIcon.setMap(null); // Remove previous icon if exists
      }

      const startPosition = [this.routeData[0].startLong, this.routeData[0].startLat];
      this.movingIcon = new AMap.Circle({
        center: startPosition,
        radius: 5, // Radius of the circle
        fillColor: '#0000FF', // Fill color (blue)
        strokeColor: '#0000FF', // Stroke color (blue)
        strokeWeight: 1,
        map: this.map,
      });

      this.isMoving = true; // Set moving state
      this.moveAlongRoute();
    },

    moveAlongRoute() {
      const speed = 2000; 

      this.moveInterval = setInterval(() => {
      if (!this.isMoving || this.currentSegmentIndex >= this.routeData.length) {
        clearInterval(this.moveInterval); // Stop if not moving or end of route
        return;
      }

      const segment = this.routeData[this.currentSegmentIndex];
      const nextPosition = [segment.endLong, segment.endLat];

      this.movingIcon.setCenter(nextPosition); // Move blue dot to next position
      this.uploadTrafficData(segment); // Upload traffic data for current segment

      // Move to the next segment after a delay
      setTimeout(() => {
        this.currentSegmentIndex++;
      }, speed); // Delay between segments (adjust as needed)
      
    }, speed); // Update position every `speed` milliseconds
    },

    uploadTrafficData(segment) {
      const trafficData = {
        segmentId: segment.id, // Assuming each segment has an ID
        startLong: segment.startLong,
        startLat: segment.startLat,
        endLong: segment.endLong,
        endLat: segment.endLat,
        currentStatus: this.getCurrentRouteStatus(segment), // Function to determine current status
        timestamp: new Date().toISOString(), // Current timestamp
      };

      let endpoint = (this.currentSegmentIndex === 0) 
      ? 'http://localhost:8080/api/traffic/upload' 
      : 'http://localhost:8080/api/traffic/update';

    axios.post(endpoint, trafficData)
      .then(response => console.log('Traffic data sent:', response))
      .catch(error => console.error('Error sending traffic data:', error));
  },

  getCurrentRouteStatus(segment) {
    // Logic to determine current traffic status based on your criteria
    // For example, you might have conditions based on speed or congestion level
    if (segment.congestionLevel > 70) {
      return 'red'; // High congestion
    } else if (segment.congestionLevel > 30) {
      return 'orange'; // Moderate congestion
    } else {
      return 'green'; // Low congestion
    }
  },

  toggleRoadStatus() {
    this.showRoadStatus = !this.showRoadStatus;
    if (this.showRoadStatus) {
      this.displayRoadStatus(); // Fetch and display road statuses
    } else {
      this.clearRoadStatus(); // Clear displayed statuses
    }
  },

  displayRoadStatus() {
    axios.get('http://localhost:8080/api/roads/all') // Adjust API endpoint as needed
        .then(response => {
          response.data.forEach(road => {
            const color = this.getColorForStatus(road.status); // Get color based on status
            const polyline = new AMap.Polyline({
              path: road.coordinates, // Assuming coordinates are provided in the response
              strokeColor: color,
              strokeWeight: 6,
            });
            polyline.setMap(this.map);
          });
        })
        .catch(error => console.error('Error fetching road status:', error));
  },

    clearRoadStatus() {
      this.map.clearMap(); // Clear all overlays from the map
    },

  getColorForStatus(status) {
    switch (status) {
      case 'red':
        return '#FF0000'; // Red for congested
      case 'orange':
        return '#FFA500'; // Orange for moderate
      case 'green':
        return '#008000'; // Green for clear
      default:
        return '#CCCCCC'; // Default color if status is unknown
    }
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