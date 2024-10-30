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
      <button @click="toggleRoadStatus">{{ showRoadStatus ? '关闭' : '显示' }} 全城道路状态</button>
      <el-row></el-row>
      <button @click="toggleSmartStatus">{{ smart ? '关闭' : '开启' }} 智能调度 </button>
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
      scheduledDate : new Date('2024-10-29T22:29:00+08:00'), // 北京时间为UTC+8
      "smart":false,
      routeData: {
          
          "userId": 0,
          "startId": 0,
          "endId": 0,
          "distance": 0.0,
          "duration": 0.0,
          "price": 0.0,
          "routeData": [                 //数据结构：
              {
                  "startLat": 0,
                  "startLong": 0,
                  "endLat": 0,
                  "endLong": 0,
                  "distance": 0,
                  "duration": 0,
                  "price": 0,
                  "status": "绿",
                  "roadId":0,
              }
          ],
          "timestamp": null,
          "priority": 0,
          "requestTime": null,
          "distanceWeight": 0,
          "durationWeight": 0,
          "priceWeight": 0
      },       // 用于存储计算后的路线数据
      isUserIdReadonly: false,
      isLoading: false, // 添加加载状态
      isCalculationComplete: false, // 添加计算完成状态
      map: null,
      polyline: null,
      showPendingMessage: false, // 新增变量控制待处理消息的显示
      showRoadStatus: false,
      movingIcon: null, // Store moving icon reference
      currentSegmentIndex: 0, // Track current segment index for movement
    };
  },
  created() {
    this.setUserIdFromUrl(); // 设置用户 ID并在找到用户 ID 时连接 WebSocket
  },
  mounted() {
    this.initMap();
    this.schedule(this.scheduledDate, 77734114, 2.0);
  },
  methods: {

    initWebSocket() {
        const userId = this.calculateInput.userId;
        if (!userId) {
            console.error("User ID is undefined. WebSocket cannot be initialized.");
            return;
        }

        console.log("Initializing WebSocket for user:", userId);
        const socket = new SockJS("http://localhost:8080/ws");
        const client = new Client({
            webSocketFactory: () => socket,
            connectHeaders: {
                login: "guest",
                passcode: "guest",
            },
            onConnect: () => {
                const subscriptionPath = `/user/${userId}/queue/roadUpdates`;
                console.log(`Subscribing to: ${subscriptionPath}`);
                client.subscribe(subscriptionPath, (message) => {
                    console.log("Received message from WebSocket:", message.body); // 确认 message.body 存在
                    if (message.body) {
                        const update = JSON.parse(message.body);
                        console.log("Parsed update:", update);
                        this.updateDuration(update.roadId, update.durationAdjustment);
                    }
                });
            },
            onStompError: (error) => console.error('STOMP error:', error),
            onWebSocketError: (error) => console.error('WebSocket error:', error),
            onWebSocketClose: () => {
                console.log('WebSocket closed. Attempting to reconnect...');
                setTimeout(() => this.initWebSocket(), 1000);
            }
        });

        client.activate();
    },
    schedule(date, roadId, durationAdjustment) {
    const now = new Date();
    const delay = date.getTime() - now.getTime();

    if (delay > 0) {
      setTimeout(() => {
        this.updateDuration(roadId, durationAdjustment);
      }, delay);
    } else {
      console.error("指定时间已过，请提供一个未来的时间。");
    }
  },


    updateDuration(roadId, durationAdjustment) {
      if (Array.isArray(this.routeData.routeData)) {
        const road = this.routeData.routeData.find((r) => r.roadId === roadId);
        if (road) {
          road.duration += durationAdjustment;
          road.status='红';
          alert("old duration:" + this.routeData.duration);
          this.routeData.duration =this.routeData.duration+ durationAdjustment;
          alert("new duration: " + this.routeData.duration);
          this.outputAndDrawRoute();
          alert("new duration: " + this.routeData.duration);
        }
        
      } else {
        alert("routeData is not an array.");
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
        const roundedDuration = Math.round(this.routeData.duration);
        const roundedDistance = Math.round(this.routeData.distance);
        const roundedPrice = Math.round(this.routeData.price);
        this.calculatedRoute = `全程${roundedDistance}公里，预计时间：${roundedDuration} 分钟，共花费${roundedPrice} 元`;
      }
    },

    drawRoute() {
  if (!this.routeData || !Array.isArray(this.routeData.routeData)) {
    console.error("this.routeData.routeData 未定义或不是数组。");
    return;
  }

  if (this.routeData.routeData.length === 0) {
    console.warn("routeData.routeData 数组为空。");
    return;
  }

  if (this.polyline) {
    this.polyline.setMap(null);
  }

  this.routeData.routeData.forEach((segment) => {
    if (
      segment.startLong === undefined || 
      segment.startLat === undefined || 
      segment.endLong === undefined || 
      segment.endLat === undefined
    ) {
      console.error("segment 坐标未定义", segment);
      return;
    }

    const routePath = [
      [segment.startLong, segment.startLat],
      [segment.endLong, segment.endLat]
    ];

    const color = this.getColorForStatus(segment.status);

    const polyline = new AMap.Polyline({
      path: routePath,
      borderWeight: 6,
      strokeColor: color,
      strokeOpacity: 0.8,
      strokeWeight: 5,
      lineJoin: 'round',
      strokeStyle: 'solid',
    });

    polyline.setMap(this.map);
    if (!this.polylines) {
      this.polylines = [];
    }
    this.polylines.push(polyline);
  });

  this.map.setFitView();
  this.startMovingIcon(this.routeData.routeData);
},
startMovingIcon(routeData) {
  // 检查 routeData 是否定义且不为空
  if (!routeData || routeData.length === 0) {
    console.error("routeData 未定义或为空数组，无法启动移动图标。");
    return;
  }

  if (this.movingIcon) {
    this.movingIcon.setMap(null); // 移除上一个图标
  }

  const icon = new AMap.Icon({
    image: require('@/assets/logo.png'), // 图标路径
    size: new AMap.Size(32, 32), // 图标大小
    imageSize: new AMap.Size(32, 32) // 图片大小，保持与图标一致
  });

  this.movingIcon = new AMap.Marker({
    position: [routeData[0].startLong, routeData[0].startLat],
    icon: icon,
    map: this.map,
    offset: new AMap.Pixel(-16, -16) // 调整偏移，使图标居中
  });

  this.moveAlongRoute(routeData);
},

    moveAlongRoute(routeData) {
    // 清除当前的定时器，确保只启动一个
    if (this.interval) clearInterval(this.interval);

    this.interval = setInterval(() => {
      if (this.currentSegmentIndex < routeData.length) {
        const segment = routeData[this.currentSegmentIndex];
        const nextPosition = [segment.endLong, segment.endLat];

        // 平滑移动图标到下一个位置
        this.movingIcon.moveTo(nextPosition, 5000); // 使用 moveTo 平滑过渡，持续 5 秒

        this.updateTrafficData(segment);

        this.currentSegmentIndex++;
      } else {
        clearInterval(this.interval); // 路线完成时停止
      }
    }, 1000); // 每隔1秒更新一次
  },



  updateTrafficData(segment) {
  const data = {
    roadId: segment.roadId,
    userId: Math.floor(Math.random() * 1000) + 1, // 生成1到1000的随机数
    speed: parseFloat((Math.random() * 100).toFixed(2)), // 生成0到100的随机速度，保留两位小数
    timestamp: new Date().toISOString().slice(0, 19).replace("T", " ") // 转换为 MySQL 支持的格式
  };

  fetch("http://127.0.0.1:8080/api/traffic/update", {
    method: "POST",
    headers: {
      "Content-Type": "application/json"
    },
    body: JSON.stringify(data)
  })
    .then(response => {
      if (!response.ok) {
        throw new Error("Failed to update traffic data");
      }
      // 检查响应是否有内容
      const contentLength = response.headers.get("Content-Length");
      if (!contentLength || contentLength === "0" || response.status === 204) {
        console.log("No content returned from the server");
        return; // 如果没有内容，直接返回
      }
      return response.json();
    })
    .then(responseData => {
      if (responseData) {
        console.log("Traffic data updated:", responseData);
      }
    })
    .catch(error => {
      console.error("Error updating traffic data:", error);
    });
  },



  getCurrentRouteStatus(segment) {
    // Logic to determine current traffic status based on your criteria
    // For example, you might have conditions based on speed or congestion level
    if (segment.congestionLevel > 70) {
      return '红'; // High congestion
    } else if (segment.congestionLevel > 30) {
      return '橙'; // Moderate congestion
    } else {
      return '绿'; // Low congestion
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
  toggleSmartStatus() {
    this.smart = !this.smart;
    console.log(this.smart);
    
    if (this.smart) {
        axios.get(`http://localhost:8080/api/smart/getIsStatus`)
            .then(response => {
                // 成功获取状态后的处理逻辑，比如更新某个状态变量
                console.log("Status fetched:", response.data);
            })
            .catch(error => {
                console.error('Error fetching start points:', error.response ? error.response.data : error);
            });
    } else {
        axios.get(`http://localhost:8080/api/smart/returnIsStatus`)
            .then(response => {
                // 成功获取状态后的处理逻辑，比如更新某个状态变量
                console.log("Status fetched:", response.data);
            })
            .catch(error => {
                console.error('Error fetching start points:', error.response ? error.response.data : error);
            });
    }
},


  async displayRoadStatus() {
    const batchSize = 1000; // 每批加载的条数，具体数量可根据后端支持及数据量调整
    let offset = 0;
    let totalRoadsLoaded = false;

    while (!totalRoadsLoaded) {
      try {
        // 分批获取道路状态
        const response = await axios.get(`http://localhost:8080/api/roads/all`, {
          params: { offset, limit: batchSize } // 假设后端支持 offset 和 limit 参数
        });

        const roads = response.data;

        // 检查是否已经加载完全部数据
        if (roads.length < batchSize) {
          totalRoadsLoaded = true;
        } else {
          offset += batchSize;
        }

        // 使用 Promise.all 并发处理当前批次的道路数据
        await Promise.all(
          roads.map(road => this.renderRoadOnMap(road))
        );

      } catch (error) {
        console.error('Error fetching road status in batch:', error);
        totalRoadsLoaded = true; // 出现错误则停止加载
      }
    }
  },

  // 渲染单条道路的方法
  renderRoadOnMap(road) {
    return new Promise(resolve => {
      const path = [
        [road.startLong, road.startLat],
        [road.endLong, road.endLat]
      ];

      const color = this.getColorForStatus(road.status);

      const polyline = new AMap.Polyline({
        path: path,
        strokeColor: color,
        strokeWeight: 6,
        strokeOpacity: 0.8,
        lineJoin: 'round',
        strokeStyle: 'solid'
      });

      // 将折线添加到地图上
      polyline.setMap(this.map);
      resolve(); // 标记当前道路渲染完成
    });
  },

    clearRoadStatus() {
      this.map.clearMap(); // Clear all overlays from the map
    },

  getColorForStatus(status) {
    switch (status) {
      case '红':
        return '#FF0000'; // Red for congested
      case '橙':
        return '#FFA500'; // Orange for moderate
      case '绿':
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
