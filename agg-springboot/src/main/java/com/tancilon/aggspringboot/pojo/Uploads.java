package com.tancilon.aggspringboot.pojo;

import java.io.Serializable;
import java.util.Date;

public class Uploads implements Serializable {
    /**
     * 上传记录的唯一标识符，自增主键
     *
     * @mbg.generated
     */
    private Integer id;

    /**
     * 用户邮箱，不能为空
     *
     * @mbg.generated
     */
    private String email;

    /**
     * 用户所属研究机构，不允许为空
     *
     * @mbg.generated
     */
    private String institution;

    /**
     * 上传时间，默认当前时间
     *
     * @mbg.generated
     */
    private Date uploadTime;

    private static final long serialVersionUID = 1L;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public Date getUploadTime() {
        return uploadTime;
    }

    public void setUploadTime(Date uploadTime) {
        this.uploadTime = uploadTime;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", email=").append(email);
        sb.append(", institution=").append(institution);
        sb.append(", uploadTime=").append(uploadTime);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}