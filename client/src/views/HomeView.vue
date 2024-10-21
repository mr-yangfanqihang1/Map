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
      },
      startInput: '',  // 用于显示用户输入的起点名称
      endInput: '',    // 用于显示用户输入的终点名称
      startSuggestions: [],
      endSuggestions: [],
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
    },
    calculateRoute() {
      this.calcError = '';
      this.calculatedRoute = '';

      // 调用后端计算路线并绘制路径
      axios.post('http://localhost:8080/api/routes/calculate', this.calculateInput)
          .then(response => {
            if (response.data.pathData) {
              this.drawRoute(response.data.pathData);
            }
            this.calculatedRoute = '路线绘制完成';
            // 重置输入框
            this.calculateInput = { userId: '', startId: '', endId: '' };
            this.startInput = '';
            this.endInput = '';
          })
          .catch(error => {
            console.error('Error calculating route:', error.response ? error.response.data : error);
            this.calcError = '计算路线失败，请重试。';
          });
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
</style>
