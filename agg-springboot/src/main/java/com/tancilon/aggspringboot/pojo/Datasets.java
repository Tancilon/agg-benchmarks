package com.tancilon.aggspringboot.pojo;

import java.io.Serializable;

public class Datasets implements Serializable {
    /**
     * 数据集的唯一 ID，自增主键
     *
     * @mbg.generated
     */
    private Integer id;

    /**
     * 数据集名称，不能为空，且唯一
     *
     * @mbg.generated
     */
    private String name;

    /**
     * 数据集描述，使用 TEXT 存储长文本
     *
     * @mbg.generated
     */
    private String description;

    /**
     * 数据集的下载链接，使用 TEXT 存储
     *
     * @mbg.generated
     */
    private String downloadUrl;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", description=").append(description);
        sb.append(", downloadUrl=").append(downloadUrl);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}