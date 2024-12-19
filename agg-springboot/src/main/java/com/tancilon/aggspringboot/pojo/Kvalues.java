package com.tancilon.aggspringboot.pojo;

import java.io.Serializable;

public class Kvalues implements Serializable {
    /**
     * k 值的唯一 ID，自增主键
     *
     * @mbg.generated
     */
    private Integer id;

    /**
     * 外键，引用 t_metrics 表的 id
     *
     * @mbg.generated
     */
    private Integer metricId;

    /**
     * 存储具体的 k 值，使用 INT 类型（整数）
     *
     * @mbg.generated
     */
    private Integer kValue;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getMetricId() {
        return metricId;
    }

    public void setMetricId(Integer metricId) {
        this.metricId = metricId;
    }

    public Integer getkValue() {
        return kValue;
    }

    public void setkValue(Integer kValue) {
        this.kValue = kValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", metricId=").append(metricId);
        sb.append(", kValue=").append(kValue);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}