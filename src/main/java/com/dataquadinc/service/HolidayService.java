package com.dataquadinc.service;

import com.dataquadinc.dto.HolidayDTO;
import com.dataquadinc.exceptions.HolidayAlreadyExistsException;
import com.dataquadinc.exceptions.HolidayNotFoundException;
import com.dataquadinc.model.Holiday;
import com.dataquadinc.repository.HolidayRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayService {

    private final HolidayRepository holidayRepository;

    @Transactional
    @CacheEvict(value = "holidays", allEntries = true)
    public Holiday createHoliday(HolidayDTO holidayDTO, String createdBy) {
        log.info("Creating holiday: {} on {}", holidayDTO.getHolidayName(), holidayDTO.getHolidayDate());

        if (holidayRepository.existsByHolidayDate(holidayDTO.getHolidayDate())) {
            throw new HolidayAlreadyExistsException("Holiday already exists on date: " + holidayDTO.getHolidayDate());
        }

        Holiday holiday = new Holiday();
        holiday.setHolidayName(holidayDTO.getHolidayName());
        holiday.setHolidayDate(holidayDTO.getHolidayDate());
        holiday.setHolidayType(Holiday.HolidayType.valueOf(holidayDTO.getHolidayType()));
        holiday.setDescription(holidayDTO.getDescription());
        holiday.setIsOptional(holidayDTO.getIsOptional() != null ? holidayDTO.getIsOptional() : false);
        holiday.setCreatedBy(createdBy);

        return holidayRepository.save(holiday);
    }

    @Cacheable("holidays")
    public List<HolidayDTO> getAllHolidays() {
        return holidayRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    public HolidayDTO getHolidayById(Long holidayId) {
        Holiday holiday = holidayRepository.findById(holidayId)
                .orElseThrow(() -> new HolidayNotFoundException("Holiday not found with ID: " + holidayId));
        return convertToDTO(holiday);
    }

    @Transactional
    @CacheEvict(value = "holidays", allEntries = true)
    public Holiday updateHoliday(Long holidayId, HolidayDTO holidayDTO, String updatedBy) {
        Holiday holiday = holidayRepository.findById(holidayId)
                .orElseThrow(() -> new HolidayNotFoundException("Holiday not found with ID: " + holidayId));

        holiday.setHolidayName(holidayDTO.getHolidayName());
        holiday.setHolidayDate(holidayDTO.getHolidayDate());
        holiday.setHolidayType(Holiday.HolidayType.valueOf(holidayDTO.getHolidayType()));
        holiday.setDescription(holidayDTO.getDescription());
        holiday.setIsOptional(holidayDTO.getIsOptional());


        return holidayRepository.save(holiday);
    }

    @Transactional
    @CacheEvict(value = "holidays", allEntries = true)
    public void deleteHoliday(Long holidayId, String deletedBy) {
        log.info("Deleting holiday with ID: {} by {}", holidayId, deletedBy);
        if (!holidayRepository.existsById(holidayId)) {
            throw new HolidayNotFoundException("Holiday not found with ID: " + holidayId);
        }
        holidayRepository.deleteById(holidayId);
    }

    public List<LocalDate> getHolidaysBetweenDates(LocalDate startDate, LocalDate endDate) {
        return holidayRepository.findHolidayDatesBetween(startDate, endDate);
    }

    private HolidayDTO convertToDTO(Holiday holiday) {
        HolidayDTO dto = new HolidayDTO();
        dto.setHolidayId(holiday.getHolidayId());
        dto.setHolidayName(holiday.getHolidayName());
        dto.setHolidayDate(holiday.getHolidayDate());
        dto.setHolidayType(holiday.getHolidayType().name());
        dto.setDescription(holiday.getDescription());
        dto.setIsOptional(holiday.getIsOptional());
        return dto;
    }
}