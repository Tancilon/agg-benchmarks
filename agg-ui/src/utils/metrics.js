export const getMetricInfo = (metricName) => {
    const defaultInfo = {
        title: metricName,
        yAxis: metricName,
        xAxis: "Dataset",
        isKMetric: false
    }

    const metricInfoMap = {
        'mAP': {
            title: "mAP",
            xAxis: "@k",
            yAxis: "mAP@k",
            isKMetric: true
        },
        'NDCG': {
            title: "NDCG",
            xAxis: "@k",
            yAxis: "NDCG@k",
            isKMetric: true
        },
        'Precision': {
            title: "Precision",
            yAxis: "Precision",
            xAxis: "Dataset",
            isKMetric: false
        },
        'Recall': {
            title: "Recall",
            yAxis: "Recall",
            xAxis: "@k",
            isKMetric: true
        }
    }

    return metricInfoMap[metricName] || defaultInfo
} 