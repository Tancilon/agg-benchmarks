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
            subtitle: "Mean Average Precision at different k values",
            xAxis: "@k",
            yAxis: "mAP@k",
            isKMetric: true
        },
        'NDCG': {
            title: "NDCG",
            subtitle: "Normalized Discounted Cumulative Gain at different k values",
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
            xAxis: "Dataset",
            isKMetric: false
        }
    }

    return metricInfoMap[metricName] || defaultInfo
} 