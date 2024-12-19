package com.tancilon.aggspringboot.pojo;

import java.io.Serializable;

public class Algorithms implements Serializable {
    /**
     * 算法的唯一 ID，自增主键
     *
     * @mbg.generated
     */
    private Integer id;

    /**
     * 算法名称，不能为空，且唯一
     *
     * @mbg.generated
     */
    private String name;

    /**
     * 算法类别，0=无监督，1=有监督，2=半监督
     *
     * @mbg.generated
     */
    private Byte category;

    /**
     * 算法的年份，使用 INT 类型
     *
     * @mbg.generated
     */
    private Integer year;

    /**
     * 算法的出处
     *
     * @mbg.generated
     */
    private String venue;

    /**
     * 算法的 BibTeX 引用格式文件链接
     *
     * @mbg.generated
     */
    private String bibtexLink;

    /**
     * 存储 PDF 文件的链接，使用 TEXT 类型
     *
     * @mbg.generated
     */
    private String pdfLink;

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

    public Byte getCategory() {
        return category;
    }

    public void setCategory(Byte category) {
        this.category = category;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public String getBibtexLink() {
        return bibtexLink;
    }

    public void setBibtexLink(String bibtexLink) {
        this.bibtexLink = bibtexLink;
    }

    public String getPdfLink() {
        return pdfLink;
    }

    public void setPdfLink(String pdfLink) {
        this.pdfLink = pdfLink;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(getClass().getSimpleName());
        sb.append(" [");
        sb.append("Hash = ").append(hashCode());
        sb.append(", id=").append(id);
        sb.append(", name=").append(name);
        sb.append(", category=").append(category);
        sb.append(", year=").append(year);
        sb.append(", venue=").append(venue);
        sb.append(", bibtexLink=").append(bibtexLink);
        sb.append(", pdfLink=").append(pdfLink);
        sb.append(", serialVersionUID=").append(serialVersionUID);
        sb.append("]");
        return sb.toString();
    }
}