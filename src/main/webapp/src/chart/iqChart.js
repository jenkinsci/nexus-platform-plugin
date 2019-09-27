const CHART_HEIGHT = 350;
const CHART_WIDTH = 400;

const ChartLegend = {
  CRITICAL : 'Critical',
  SEVERE: 'Severe',
  MODERATE : 'Moderate'
};

const ChartColor = {
  CRITICAL : '#bc012f',
  SEVERE: '#f4861d',
  MODERATE : '#f5c648'
};

function getChartTitle() {
  return document.currentScript.getAttribute('chartTitle');
}

function getCriticalValues() {
  return [];
}

function getSevereValues() {
  return [];
}

function getModerateValues() {
  return [];
}

function getXAxisLabels() {
  return [];
}

const options = {
  chart: {
    height: CHART_HEIGHT,
    width: CHART_WIDTH,
    type: 'line',
    zoom: {
      enabled: false
    },
    events: {
      markerClick: function(event, series, dataPoint) {
        const buildNumber = getXAxisLabels()[dataPoint.dataPointIndex];
        window.location.href = buildNumber.toString();
      }
    }
  },
  colors: [ChartColor.CRITICAL, ChartColor.SEVERE, ChartColor.MODERATE],
  series: [
    {
      name: ChartLegend.CRITICAL,
      data: getCriticalValues()
    },
    {
      name: ChartLegend.SEVERE,
      data: getSevereValues()
    },
    {
      name: ChartLegend.MODERATE,
      data: getModerateValues()
    }
  ],
  dataLabels: {
    enabled: false
  },
  stroke: {
    curve: 'straight',
    width: 2,
  },
  title: {
    text: getChartTitle(),
    align: 'center',
    style: {
      fontSize:  '16px',
      color:  'black'
    },
  },
  xaxis: {
    categories: getXAxisLabels(),
    labels: {
      formatter: function (value) {
        return '#' + value;
      }
    }
  },
  markers: {
    size: 5,
    shape: "circle"
  },
  legend: {
    position: 'bottom',
    horizontalAlign: 'center'
  }
};


const iqChart = new ApexCharts(
    document.querySelector("#iqChart"),
    options
);

iqChart.render();

