/*
 * Copyright (c) 2016-present Sonatype, Inc. All rights reserved.
 *
 * This program is licensed to you under the Apache License Version 2.0,
 * and you may not use this file except in compliance with the Apache License Version 2.0.
 * You may obtain a copy of the Apache License Version 2.0 at http://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the Apache License Version 2.0 is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License Version 2.0 for the specific language governing permissions and limitations there under.
 */

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

function getPolicyEvaluations() {
  try {
    return JSON.parse(document.currentScript.getAttribute('policyEvaluations'));
  }
  catch (e) {
    console.log(`Cannot parse data for a chart: ${e.message}`);
    return undefined;
  }
}

const policyEvaluations = getPolicyEvaluations();

function getChartTitle() {
  return document.currentScript.getAttribute('chartTitle');
}

function getCriticalValues() {
  return policyEvaluations ? policyEvaluations.map(value => value.criticalCount) : [];
}

function getSevereValues() {
  return policyEvaluations ? policyEvaluations.map(value => value.severeCount) : [];
}

function getModerateValues() {
  return policyEvaluations ? policyEvaluations.map(value => value.moderateCount) : [];
}

function getXAxisLabels() {
  return policyEvaluations ? policyEvaluations.map(value => value.buildNumber) : [];
}

function getMaxValue() {
  return getCriticalValues().concat(getSevereValues()).concat(getModerateValues()).max();
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
    },
    toolbar: {
      show: false
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
    },
    tooltip: {
      enabled: false
    }
  },
  yaxis: {
    min: 0,
    max: getMaxValue() + 1,
    forceNiceScale: true,
    labels: {
      formatter: function (value) {
        const intVal = value.toFixed(0);
        return Math.abs(value - intVal) < 0.001 ? intVal : value.toFixed(1);
      }
    }
  },
  markers: {
    size: 5,
    shape: 'circle'
  },
  legend: {
    position: 'bottom',
    horizontalAlign: 'center'
  },
  tooltip: {
    theme: 'light',
    style: {
      fontSize: '14px',
    }
  }
};

function showChart() {
  if (policyEvaluations) {
    const iqChart = new ApexCharts(
        document.querySelector('#iqChart'),
        options
    );

    iqChart.render();
  }
}

showChart();
