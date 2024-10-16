<template>
  <div id="map-container">
    <div id="container"></div>
    <div class="road-status">
      <input v-model="roadId" placeholder="Enter road ID" />
      <button @click="getRoadStatus">Get Road Status</button>
      <p v-if="roadStatus">Road Status: {{ roadStatus }}</p>
      <p v-if="error">{{ error }}</p>
    </div>

    <div class="route-management">
      <h3>Create Route</h3>
      <form @submit.prevent="createRoute">
        <input v-model="route.userId" placeholder="User ID" required />
        <input v-model="route.startLocation" placeholder="Startpoint" required />
        <input v-model="route.endLocation" placeholder="Endpoint" required />
        <input v-model="route.priority" type="number" placeholder="Priority" required />
        <button type="submit">Create Route</button>
      </form>
      <p v-if="createdRoute">Route created: {{ createdRoute }}</p>
      <p v-if="createError">{{ createError }}</p>
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
      roadId: '',          // 存储输入的道路ID
      roadStatus: '',      // 显示的道路状态
      error: '',           // 存储错误信息
      route: {            // 存储创建路线所需的信息
        userId: '',
        startLocation: '',
        endLocation: '',
        priority: 0,
      },
      createdRoute: '',    // 显示创建的路线
      createError: '',     // 存储创建路线时的错误信息
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
        center: [116.397428, 39.90923], // 天安门，北京的坐标
      });
    },
    // 调用后端API获取道路状态
    getRoadStatus() {
      if (!this.roadId || isNaN(this.roadId)) {
        this.error = 'Please enter a valid road ID';
        return;
      }
      this.error = '';  // 重置错误状态
      this.roadStatus = '';  // 重置道路状态

      // 发送API请求获取道路状态
      axios.get(`http://localhost:8080/api/roads/status/${this.roadId}`)
          .then(response => {
            this.roadStatus = response.data; // 更新道路状态
          })
          .catch(error => {
            console.error('Error fetching road status:', error);
            this.error = 'Failed to fetch road status. Please try again.';
          });
    },
    // 创建新路线
    createRoute() {
      this.createError = ''; // 重置错误信息
      this.createdRoute = ''; // 重置创建的路线信息

      // 发送请求创建新路线=
      axios.post('http://localhost:8080/api/routes/create', this.route)
          .then(response => {
            this.createdRoute = response.data ? response.data.message : 'Route created successfully!'; // 使用response
          })
          .catch(error => {
            console.error('Error creating route:', error);
            this.createError = 'Failed to create route. Please try again.';
          });
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
}
</style>
