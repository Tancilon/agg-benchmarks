package com.tancilon.aggspringboot.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tancilon.aggspringboot.repository.MetricScoreRepository;
import com.tancilon.aggspringboot.repository.MetricScoreHistoryRepository;
import com.tancilon.aggspringboot.entity.MetricScore;
import com.tancilon.aggspringboot.entity.MetricScoreHistory;
import com.tancilon.aggspringboot.enums.ChangeType;
import jakarta.persistence.EntityNotFoundException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MetricScoreVersionService {

    @Autowired
    private MetricScoreRepository metricScoreRepo;

    @Autowired
    private MetricScoreHistoryRepository historyRepo;

    @Transactional
    public void createNewVersion(MetricScore score, String reason, String changedBy) {
        // 将当前版本标记为非最新
        MetricScore oldVersion = metricScoreRepo.findLatestVersion(
                score.getAlgorithm().getId(),
                score.getDataset().getId(),
                score.getMetric().getId());

        if (oldVersion != null) {
            oldVersion.setIsLatest(false);
            metricScoreRepo.save(oldVersion);
        }

        // 创建新版本
        score.setVersion(oldVersion != null ? oldVersion.getVersion() + 1 : 1);
        score.setIsLatest(true);
        metricScoreRepo.save(score);

        // 记录变更历史
        MetricScoreHistory history = new MetricScoreHistory();
        history.setMetricScore(score);
        history.setOldValue(oldVersion != null ? oldVersion.getScore() : null);
        history.setNewValue(score.getScore());
        history.setChangeType(oldVersion == null ? ChangeType.CREATE : ChangeType.UPDATE);
        history.setChangeReason(reason);
        history.setChangedBy(changedBy);
        history.setChangedAt(LocalDateTime.now());
        historyRepo.save(history);
    }

    public List<MetricScore> getVersionHistory(Long metricScoreId) {
        return metricScoreRepo.findAllVersions(metricScoreId);
    }

    @Transactional
    public void deprecateVersion(Long metricScoreId, String reason, String changedBy) {
        MetricScore score = metricScoreRepo.findById(metricScoreId)
                .orElseThrow(() -> new EntityNotFoundException("MetricScore not found"));

        score.setDeprecated(true);
        metricScoreRepo.save(score);

        MetricScoreHistory history = new MetricScoreHistory();
        history.setMetricScore(score);
        history.setChangeType(ChangeType.DEPRECATE);
        history.setChangeReason(reason);
        history.setChangedBy(changedBy);
        history.setChangedAt(LocalDateTime.now());
        historyRepo.save(history);
    }
}