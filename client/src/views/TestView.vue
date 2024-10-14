<template>
  <div>
    <el-form :model="user" label-width="100px">
      <el-form-item label="ID">
        <el-input v-model="user.id" disabled></el-input>
      </el-form-item>
      <el-form-item label="用户名">
        <el-input v-model="user.username"></el-input>
      </el-form-item>
      <el-form-item label="Preferences">
        <el-input v-model="user.preferences" @input="updateChart"></el-input>
      </el-form-item>
    </el-form>
    
    <!-- 替换后的饼图 -->
    <v-chart :option="chartOptions" style="height: 400px;"></v-chart>
  </div>
</template>

<script>
import { defineComponent } from 'vue';
import { use } from 'echarts/core';
import ECharts from 'vue-echarts';
import { CanvasRenderer } from 'echarts/renderers';
import { PieChart } from 'echarts/charts';
import { TitleComponent, TooltipComponent, LegendComponent } from 'echarts/components';
import axios from 'axios'; // 导入 axios

// 注册必须的组件
use([
  CanvasRenderer,
  PieChart,
  TitleComponent,
  TooltipComponent,
  LegendComponent
]);

export default defineComponent({
  components: {
    'v-chart': ECharts
  },
  data() {
    return {
      user: {
        id: '',
        username: '',
        preferences: '{}', // 默认值
      },
      chartOptions: {
        title: {
          text: '示例饼图',
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
            name: '访问来源',
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
    fetchUserData(id) {
      axios.get(`http://127.0.0.1:8080/api/users/${id}`)
        .then(response => {
          this.user = response.data;
          this.updateChart(); // 确保在获取数据后更新图表
        })
        .catch(error => {
          console.error('Axios error:', error);
        });
    },
    updateChart() {
      try {
        const preferencesStr = typeof this.user.preferences === 'string'
          ? this.user.preferences
          : JSON.stringify(this.user.preferences);

        const preferences = JSON.parse(preferencesStr);
        this.chartOptions.series[0].data = Object.keys(preferences).map(key => ({
          name: key,
          value: preferences[key]
        }));
      } catch (error) {
        console.error('Parsing error:', error);
        this.chartOptions.series[0].data = [];
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
/* 这里可以添加自定义样式 */
</style>
