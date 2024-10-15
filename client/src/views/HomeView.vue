<template>
  <div id="map-container">
    <div id="container"></div>
  </div>
</template>

<script>
// eslint-disable-next-line no-undef
/* global AMap */

export default {
  name: 'MapWithNavigation',
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
      const map = new AMap.Map('container', {
        resizeEnable: true,
        zoom: 14,
        center: [116.397428, 39.90923], // Tiananmen, Beijing coordinates
      });

      // Load the Driving plugin after the map is initialized
      AMap.plugin('AMap.Driving', () => {
        const startLngLat = [116.379028, 39.865042]; // Starting point coordinates
        const endLngLat = [116.427281, 39.903719]; // Destination coordinates

        // Create the Driving instance now that the plugin is loaded
        const driving = new AMap.Driving({
          map: map,
          panel: null, // Specify a panel if you want to display route details
        });

        // Plan the route
        driving.search(startLngLat, endLngLat, (status, result) => {
          if (status === 'complete' && result.routes && result.routes.length) {
            console.log('Route successfully plotted');
          } else {
            console.log('Failed to plot route: ' + status);
          }
        });
      });
    },
  },
};
</script>

<style scoped>
#container {
  width: 100%;
  height: 785px;
}
</style>
