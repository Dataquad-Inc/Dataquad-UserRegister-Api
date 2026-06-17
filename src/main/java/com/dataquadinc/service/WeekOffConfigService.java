package com.dataquadinc.service;

import com.dataquadinc.exceptions.InvalidDayOfWeekException;
import com.dataquadinc.exceptions.WeekOffConfigurationException;
import com.dataquadinc.model.WeekOffConfig;
import com.dataquadinc.repository.WeekOffConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WeekOffConfigService {

    private final WeekOffConfigRepository weekOffConfigRepository;

    @Transactional
    public WeekOffConfig configureWeekOff(Integer dayOfWeek, Boolean isWeekOff, String entity) {
        validateDayOfWeek(dayOfWeek);

        List<WeekOffConfig> existingConfigs = weekOffConfigRepository.findByEntity(entity);

        WeekOffConfig config = existingConfigs.stream()
                .filter(c -> c.getDayOfWeek().equals(dayOfWeek))
                .findFirst()
                .orElse(new WeekOffConfig());

        config.setDayOfWeek(dayOfWeek);
        config.setIsWeekOff(isWeekOff);
        config.setEntity(entity);

        log.info("Configured week off for day {}: {} for entity {}", dayOfWeek, isWeekOff, entity);
        return weekOffConfigRepository.save(config);
    }

    public List<Integer> getWeekOffDays(String entity) {
        return weekOffConfigRepository.findByEntity(entity).stream()
                .filter(WeekOffConfig::getIsWeekOff)
                .map(WeekOffConfig::getDayOfWeek)
                .collect(Collectors.toList());
    }

    public boolean isWeekOff(Integer dayOfWeek, String entity) {
        return weekOffConfigRepository.findByEntity(entity).stream()
                .anyMatch(config -> config.getDayOfWeek().equals(dayOfWeek) && Boolean.TRUE.equals(config.getIsWeekOff()));
    }

    private void validateDayOfWeek(Integer dayOfWeek) {
        if (dayOfWeek < 1 || dayOfWeek > 7) {
            throw new InvalidDayOfWeekException("Invalid day of week: " + dayOfWeek + ". Must be between 1 (Monday) and 7 (Sunday)");
        }
    }

    @Transactional
    public void resetToDefault(String entity) {
        List<WeekOffConfig> existingConfigs = weekOffConfigRepository.findByEntity(entity);
        weekOffConfigRepository.deleteAll(existingConfigs);

        // Default: Saturday (6) and Sunday (7) are week offs
        WeekOffConfig saturday = new WeekOffConfig();
        saturday.setDayOfWeek(6);
        saturday.setIsWeekOff(true);
        saturday.setEntity(entity);
        weekOffConfigRepository.save(saturday);

        WeekOffConfig sunday = new WeekOffConfig();
        sunday.setDayOfWeek(7);
        sunday.setIsWeekOff(true);
        sunday.setEntity(entity);
        weekOffConfigRepository.save(sunday);

        log.info("Reset week off configuration to default for entity: {}", entity);
    }
}