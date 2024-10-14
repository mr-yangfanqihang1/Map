<template>
  <div class="container">
    <!-- 左侧表格 -->
    <div class="form-section">
      <el-form :model="user" label-width="100px">
        <el-form-item label="ID">
          <el-input v-model="user.id" disabled></el-input>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input v-model="user.username"></el-input>
        </el-form-item>
        <el-form-item label-width="100px" style="margin-left:0%;">
            <label style="color: black;font-size: large;font-weight: 800">偏好设置：</label>
        </el-form-item>
        <el-form-item label="时间">
          <el-input v-model.number="preferences.time" @input="validateInput('time')" type="number">
            <template #append>%</template>
          </el-input>
        </el-form-item>

        <el-form-item label="价格">
          <el-input v-model.number="preferences.price" @input="validateInput('price')" type="number">
            <template #append>%</template>
          </el-input>
        </el-form-item>

        <el-form-item label="距离">
          <el-input v-model.number="preferences.distance" @input="validateInput('distance')" type="number">
            <template #append>%</template>
          </el-input>
        </el-form-item>

        <el-form-item>
          <el-button type="primary" @click="submitData">提交</el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 右侧图表 -->
    <div class="chart-section">
      <v-chart :option="chartOptions" style="height: 400px;"></v-chart>
    </div>
  </div>
</template>

<script>
import { defineComponent } from 'vue';
import { use } from 'echarts/core';
import ECharts from 'vue-echarts';
import { CanvasRenderer } from 'echarts/renderers';
import { PieChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components';
import axios from 'axios';

// 注册必须的组件
use([CanvasRenderer, PieChart, TitleComponent, TooltipComponent, LegendComponent]);

export default defineComponent({
  components: {
    'v-chart': ECharts
  },
  data() {
    return {
      user: {
        id: '',
        username: '',
        preferences: '' // Will store JSON string after submission
      },
      preferences: {
        time: 0,      // 替换为 'time'
        price: 0,     // 替换为 'price'
        distance: 0   // 替换为 'distance'
      },
      chartOptions: {
        title: {
          text: '偏好设置',
          left: 'center'
        },
        tooltip: {
          trigger: 'item'
        },
        legend: {
          top: 'bottom'
        },
        series: [
          {
            name: '偏好值',
            type: 'pie',
            radius: '50%',
            data: [], // 初始数据为空
            emphasis: {
              itemStyle: {
                shadowBlur: 10,
                shadowOffsetX: 0,
                shadowColor: 'rgba(0, 0, 0, 0.5)'
              }
            }
          }
        ]
      }
    };
  },
  methods: {
    validateInput(field) {
      // 限制输入值在 0 到 100 之间
      if (this.preferences[field] < 0) {
        this.preferences[field] = 0;
      } else if (this.preferences[field] > 100) {
        this.preferences[field] = 100;
      }
      this.updateChart(); // 更新图表数据
    },
    fetchUserData(id) {
      axios.get(`http://127.0.0.1:8080/api/users/${id}`, {
        timeout: 5000 // 设置超时时间为 5 秒
      })
        .then(response => {
          console.log("Response data:", response.data);
          this.user = response.data;
          this.preferences = JSON.parse(this.user.preferences || '{}');
          this.updateChart(); // 确保在获取数据后更新图表
        })
        .catch(error => {
          console.error('Axios error:', error);
        });
    },
    updateChart() {
      // 更新饼图数据，使用 'time', 'price', 'distance' 作为键名
      const data = [
        { name: '时间', value: this.preferences.time },
        { name: '价格', value: this.preferences.price },
        { name: '距离', value: this.preferences.distance }
      ];
      this.chartOptions.series[0].data = data;
    },
    submitData() {
      const total = this.preferences.time + this.preferences.price + this.preferences.distance;
      if (total === 100) {
        this.user.preferences = JSON.stringify({
          time: this.preferences.time,
          price: this.preferences.price,
          distance: this.preferences.distance
        }); // 将偏好值转换为字符串格式，只包含 time, price, distance
        axios.post('http://127.0.0.1:8080/api/users/update', this.user)
          .then(() => {
            alert('提交成功');
          })
          .catch(error => {
            console.error('提交错误:', error);
          });
      } else {
        alert('时间、价格和距离之和必须等于 100');
      }
    }
  },
  mounted() {
    const userId = this.$route.params.id; // 从路由参数获取用户ID
    this.fetchUserData(userId);
  }
});
</script>

<style scoped>
.container {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}

.form-section {
  flex: 1;
  margin-right: 20px; /* 让表单和图表之间有些间距 */
}

.chart-section {
  flex: 1;
}
</style>
