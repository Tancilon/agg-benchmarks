CREATE TABLE t_algorithms (
                              id INT AUTO_INCREMENT PRIMARY KEY COMMENT '算法的唯一 ID，自增主键',
                              name VARCHAR(255) NOT NULL UNIQUE COMMENT '算法名称，不能为空，且唯一',
                              category TINYINT NOT NULL COMMENT '算法类别，0=无监督，1=有监督，2=半监督',
                              year INT NOT NULL COMMENT '算法的年份，使用 INT 类型',
                              venue VARCHAR(255) COMMENT '算法的出处',
                              bibtex_link TEXT COMMENT '算法的 BibTeX 引用格式文件链接',
                              pdf_link TEXT COMMENT '存储 PDF 文件的链接，使用 TEXT 类型'
);

CREATE TABLE t_datasets (
                            id INT AUTO_INCREMENT PRIMARY KEY COMMENT '数据集的唯一 ID，自增主键',
                            name VARCHAR(255) NOT NULL UNIQUE COMMENT '数据集名称，不能为空，且唯一',
                            description TEXT COMMENT '数据集描述，使用 TEXT 存储长文本',
                            download_url TEXT COMMENT '数据集的下载链接，使用 TEXT 存储'
);

CREATE TABLE t_metrics (
                           id INT AUTO_INCREMENT PRIMARY KEY COMMENT '指标的唯一 ID',
                           name VARCHAR(255) NOT NULL COMMENT '指标名称（如 mAP）',
                           description TEXT COMMENT '指标描述'
);

CREATE TABLE t_kvalues (
                           id INT AUTO_INCREMENT PRIMARY KEY COMMENT 'k 值的唯一 ID，自增主键',
                           metric_id INT NOT NULL COMMENT '外键，引用 t_metrics 表的 id',
                           k_value INT NOT NULL COMMENT '存储具体的 k 值，使用 INT 类型（整数）',
                           FOREIGN KEY (metric_id) REFERENCES t_metrics(id) -- 外键约束，引用 t_metrics 表的 id
);

CREATE TABLE t_algorithm_performance (
                                         id INT AUTO_INCREMENT PRIMARY KEY COMMENT '性能记录的唯一 ID，自增主键',
                                         algorithm_id INT NOT NULL COMMENT '外键，引用 t_algorithms 表的 id，表示算法',
                                         dataset_id INT NOT NULL COMMENT '外键，引用 t_datasets 表的 id，表示数据集',
                                         metric_id INT NOT NULL COMMENT '外键，引用 t_metrics 表的 id，表示性能指标',
                                         k_value_id INT NOT NULL COMMENT '外键，引用 t_kvalues 表的 id，表示 k 值',
                                         performance_value FLOAT NOT NULL COMMENT '存储算法性能数据，使用 FLOAT 类型（浮点数）',
                                         FOREIGN KEY (algorithm_id) REFERENCES t_algorithms(id),  -- 外键，引用 t_algorithms 表的 id
                                         FOREIGN KEY (dataset_id) REFERENCES t_datasets(id),      -- 外键，引用 t_datasets 表的 id
                                         FOREIGN KEY (metric_id) REFERENCES t_metrics(id),        -- 外键，引用 t_metrics 表的 id
                                         FOREIGN KEY (k_value_id) REFERENCES t_kvalues(id)        -- 外键，引用 t_kvalues 表的 id
);

CREATE TABLE t_downloads (
                             id INT AUTO_INCREMENT PRIMARY KEY COMMENT '下载记录的唯一标识符，自增主键',
                             email VARCHAR(255) NOT NULL COMMENT '用户邮箱，不能为空',
                             institution VARCHAR(255) NOT NULL COMMENT '用户所属研究机构，不允许为空',
                             download_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '下载时间，默认当前时间'
);

CREATE TABLE t_uploads (
                           id INT AUTO_INCREMENT PRIMARY KEY COMMENT '上传记录的唯一标识符，自增主键',
                           email VARCHAR(255) NOT NULL COMMENT '用户邮箱，不能为空',
                           institution VARCHAR(255) NOT NULL COMMENT '用户所属研究机构，不允许为空',
                           upload_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '上传时间，默认当前时间'
);