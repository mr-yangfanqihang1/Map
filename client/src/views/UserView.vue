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
    <v-chart :options="chartOptions" ref="chart" style="width: 500px; height: 400px;"></v-chart>
  </div>
</template>

<script>
import axios from 'axios';
import { defineComponent } from 'vue';
import VChart from 'vue-echarts'; // 导入 vue-echarts 组件
import 'echarts/lib/chart/pie'; // 导入饼图类型
import 'echarts/lib/component/tooltip'; // 导入 tooltip 组件

export default defineComponent({
  components: {
    VChart // 注册 VChart 组件
  },
  data() {
    return {
      user: {
        id: '',
        username: '',
        preferences: '{}',
      },
      chartOptions: {
        tooltip: {
          trigger: 'item'
        },
        series: [
          {
            name: 'Preferences',
            type: 'pie',
            radius: '50%',
            data: [],
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
          if (this.$message) {
            if (error.response) {
              this.$message.error(`服务器响应错误: ${error.response.status}`);
            } else if (error.request) {
              this.$message.error('没有收到服务器响应');
            } else {
              this.$message.error(`请求错误: ${error.message}`);
            }
          } else {
            alert('请求出错，请检查控制台日志'); // 如果$消息不存在，则使用alert作为备用
          }
        });
    },
    updateChart() {
      try {
        const preferences = JSON.parse(this.user.preferences);
        this.chartOptions.series[0].data = Object.keys(preferences).map(key => ({
          name: key,
          value: preferences[key]
        }));
      } catch (error) {
        this.$message.error('Preferences格式错误，请输入合法的JSON字符串');
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
</style>
