package com.nipponhub.nipponhubv0.DTO;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * City Data Transfer Object for API responses.
 * Contains essential city information for location-based product filtering.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CityDto {

    private Long idCity;
    private String cityName;
    private String cityCode;
    private Long countryId;
    private String countryName;
    private LocalDateTime createdAt;

    @Override
    public String toString() {
        return "CityDto{" +
                "idCity=" + idCity +
                ", cityName='" + cityName + '\'' +
                ", cityCode='" + cityCode + '\'' +
                ", countryId=" + countryId +
                ", countryName='" + countryName + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
