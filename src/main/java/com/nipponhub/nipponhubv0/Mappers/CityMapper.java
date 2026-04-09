package com.nipponhub.nipponhubv0.Mappers;

import com.nipponhub.nipponhubv0.DTO.CityDto;
import com.nipponhub.nipponhubv0.Models.City;
import org.springframework.stereotype.Component;

/**
 * CityMapper converts between City entities and CityDto objects.
 * Used for API responses to hide internal entity structure and avoid circular references.
 */
@Component
public class CityMapper {

    /**
     * Convert City entity to CityDto.
     * Extracts country information to avoid loading full country object.
     *
     * @param city the City entity
     * @return CityDto with city and country information
     */
    public CityDto cityToDto(City city) {
        if (city == null) {
            return null;
        }

        CityDto dto = new CityDto();
        dto.setIdCity(city.getIdCity());
        dto.setCityName(city.getCityName());
        dto.setCityCode(city.getCityCode());
        dto.setCreatedAt(city.getCreatedAt());

        if (city.getCountry() != null) {
            dto.setCountryId(city.getCountry().getIdCountry());
            dto.setCountryName(city.getCountry().getCountryName());
        }

        return dto;
    }

    /**
     * Convert CityDto to City entity.
     * Note: This does NOT set the country — that should be set separately
     * using the repository to avoid detached entity issues.
     *
     * @param dto the CityDto
     * @return City entity (country not set)
     */
    public City dtoToCity(CityDto dto) {
        if (dto == null) {
            return null;
        }

        City city = new City();
        city.setIdCity(dto.getIdCity());
        city.setCityName(dto.getCityName());
        city.setCityCode(dto.getCityCode());
        city.setCreatedAt(dto.getCreatedAt());

        return city;
    }
}
