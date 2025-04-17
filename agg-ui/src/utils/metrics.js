// 缓存指标信息
let metricInfoCache = {};

// 默认指标信息
const defaultInfo = {
    title: "",
    yAxis: "",
    xAxis: "Dataset",
    isKMetric: false
};

/**
 * 从后端获取指标信息
 * @param {string} metricName - 指标名称
 * @returns {Promise<Object>} - 指标信息
 */
export const fetchMetricInfo = async (metricName) => {
    try {
        // 如果缓存中已有数据，直接返回
        if (metricInfoCache[metricName]) {
            return metricInfoCache[metricName];
        }

        // 从后端获取指标信息
        const response = await fetch(`/api/metrics/by-name/${metricName}`);
        if (!response.ok) {
            throw new Error(`Failed to fetch metric info for ${metricName}`);
        }

        const data = await response.json();

        // 处理返回的数据
        const metricInfo = {
            title: data.name || metricName,
            yAxis: data.yAxis || metricName,
            xAxis: data.xAxis || "Dataset",
            isKMetric: data.type === "at-k" // 根据 type 字段判断是否为 K 指标
        };

        // 存入缓存
        metricInfoCache[metricName] = metricInfo;

        return metricInfo;
    } catch (error) {
        console.error(`Error fetching metric info for ${metricName}:`, error);
        // 返回默认信息
        return {
            ...defaultInfo,
            title: metricName,
            yAxis: metricName
        };
    }
};

/**
 * 获取指标信息
 * @param {string} metricName - 指标名称
 * @returns {Object} - 指标信息
 */
export const getMetricInfo = (metricName) => {
    // 如果缓存中已有数据，直接返回
    if (metricInfoCache[metricName]) {
        return metricInfoCache[metricName];
    }

    // 如果缓存中没有数据，返回默认信息
    return {
        ...defaultInfo,
        title: metricName,
        yAxis: metricName
    };
};

/**
 * 预加载所有指标信息
 * @returns {Promise<void>}
 */
export const preloadMetricInfo = async () => {
    try {
        const response = await fetch('/api/metrics');
        if (!response.ok) {
            throw new Error('Failed to fetch metrics');
        }

        const metrics = await response.json();

        // 并行获取所有指标信息
        await Promise.all(
            metrics.map(async (metric) => {
                try {
                    const info = await fetchMetricInfo(metric.name);
                    metricInfoCache[metric.name] = info;
                } catch (error) {
                    console.error(`Error preloading metric info for ${metric.name}:`, error);
                }
            })
        );

        console.log('Metric info preloaded successfully');
    } catch (error) {
        console.error('Error preloading metric info:', error);
    }
}; 