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
        <input
            v-model="calculateInput.userId"
            placeholder="User ID"
            required
        />

        <!-- 起点模糊搜索 -->
        <input
            v-model="calculateInput.startId"
            placeholder="Startpoint"
            @input="debouncedSearchItems('start')"
            required
        />
        <ul v-if="startResults.length && calculateInput.startId" class="search-results">
          <li v-for="item in startResults" :key="item.id" @click="selectItem('start', item)">
            {{ item.name }}
          </li>
        </ul>

        <!-- 终点模糊搜索 -->
        <input
            v-model="calculateInput.endId"
            placeholder="Endpoint"
            @input="debouncedSearchItems('end')"
            required
        />
        <ul v-if="endResults.length && calculateInput.endId" class="search-results">
          <li v-for="item in endResults" :key="item.id" @click="selectItem('end', item)">
            {{ item.name }}
          </li>
        </ul>

        <input
            v-model="calculateInput.priority"
            type="number"
            placeholder="Priority"
            required
        />

        <button type="submit">Calculate Route</button>
      </form>
      <p v-if="calculatedRoute">Calculated Route: {{ calculatedRoute }}</p>
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
        startLngLat: null,
        endLngLat: null,
      },
      calculatedRoute: '',
      calcError: '',
      map: null,
      polyline: null,
      startResults: [], // 起点模糊搜索结果
      endResults: [], // 终点模糊搜索结果
      loading: false,
    };
  },
  mounted() {
    this.initMap();
    this.debouncedSearchItems = this.debounce(this.searchItems, 300); // 300ms 防抖
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
          this.calculatedRoute = response.data;

          // 如果后端返回了路径数据，则绘制路线
          if (response.data.pathData) {
            this.drawRoute(response.data.pathData);  // 使用 pathData 绘制路线
          }

          this.calculateInput = { userId: '', startId: '', endId: '', priority: 0 };
        })
        .catch(error => {
          console.error('Error calculating route:', error.response ? error.response.data : error);
          this.calcError = 'Failed to calculate route. Please try again.';
        });
    },
    drawRoute(pathData) {
      if (this.polyline) {
        this.polyline.setMap(null);  // 如果之前绘制过路线，清除它
      }

      // 构建路径点数组
      const routePath = pathData.flatMap((segment) => {
        return [
          [segment.startLong, segment.startLat],  // 起点
          [segment.endLong, segment.endLat]  // 终点
        ];
      });

      // 创建新的 Polyline
      this.polyline = new AMap.Polyline({
        path: routePath,
        borderWeight: 6,
        strokeColor: '#33A1C9',
        strokeOpacity: 0.8,
        strokeWeight: 5,
        lineJoin: 'round',
        strokeStyle: 'solid',
      });

      // 设置路线显示在地图上
      this.polyline.setMap(this.map);

      // 自动缩放地图以适应路线
      this.map.setFitView([this.polyline]);
    },
    searchItems(inputType) {
      this.loading = true;
      const query = inputType === 'start' ? this.calculateInput.startId : this.calculateInput.endId;

      if (!query) {
        if (inputType === 'start') {
          this.startResults = [];
        } else {
          this.endResults = [];
        }
        this.loading = false;
        return;
      }

      // 发送模糊搜索请求
      axios.get(`http://localhost:8080/api/search?query=${query}`)
          .then(response => {
            if (inputType === 'start') {
              this.startResults = response.data;
            } else {
              this.endResults = response.data;
            }
          })
          .catch(error => {
            console.error('Error during search:', error);
          })
          .finally(() => {
            this.loading = false;
          });
    },
    selectItem(inputType, item) {
      if (inputType === 'start') {
        this.calculateInput.startId = item.name;
        this.startResults = [];
      } else {
        this.calculateInput.endId = item.name;
        this.endResults = [];
      }
    },
    debounce(func, delay) {
      let timeout;
      return function (...args) {
        const context = this;
        clearTimeout(timeout);
        timeout = setTimeout(() => func.apply(context, args), delay);
      };
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

/* 搜索结果样式 */
.search-results {
  position: absolute;
  z-index: 1000;
  border: 1px solid #ccc;
  background-color: white;
  list-style-type: none;
  padding: 0;
  margin: 0;
  width: 100%;
}

.search-results li {
  padding: 10px;
  cursor: pointer;
}

.search-results li:hover {
  background-color: #f0f0f0;
}
</style>
