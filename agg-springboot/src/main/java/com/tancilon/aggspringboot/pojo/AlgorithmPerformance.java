package com.tancilon.aggspringboot.pojo;

import java.io.Serializable;

public class AlgorithmPerformance implements Serializable {
    /**
     * 性能记录的唯一 ID，自增主键
     *
     * @mbg.generated
     */
    private Integer id;

    /**
     * 外键，引用 t_algorithms 表的 id，表示算法
     *
     * @mbg.generated
     */
    private Integer algorithmId;

    /**
     * 外键，引用 t_datasets 表的 id，表示数据集
     *
     * @mbg.generated
     */
    private Integer datasetId;

    /**
     * 外键，引用 t_metrics 表的 id，表示性能指标
     *
     * @mbg.generated
     */
    private Integer metricId;

    /**
     * 外键，引用 t_kvalues 表的 id，表示 k 值
     *
     * @mbg.generated
     */
    private Integer kValueId;

    /**
     * 存储算法性能数据，使用 FLOAT 类型（浮点数）
     *
     * @mbg.generated
     */
    private Float performanceValue;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getAlgorithmId() {
        return algorithmId;
    }

    public void setAlgorithmId(Integer algorithmId) {
        this.algorithmId = algorithmId;
    }

    public Integer getDatasetId() {
        return datasetId;
    }

    public void setDatasetId(Integer datasetId) {
        this.datasetId = datasetId;
    }

    public Integer getMetricId() {
        return metricId;
    }

    public void setMetricId(Integer metricId) {
        this.metricId = metricId;
    }

    public Integer getkValueId() {
        return kValueId;
    }

    public void setkValueId(Integer kValueId) {
        this.kValueId = kValueId;
    }

    public Float getPerformanceValue() {
        return performanceValue;
    }

    public void setPerformanceValue(Float performanceValue) {
        this.performanceValue = performanceValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", algorithmId=").append(algorithmId);
        sb.append(", datasetId=").append(datasetId);
        sb.append(", metricId=").append(metricId);
        sb.append(", kValueId=").append(kValueId);
        sb.append(", performanceValue=").append(performanceValue);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}