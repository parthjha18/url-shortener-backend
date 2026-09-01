package com.parth.urlshortener.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

/**
 * Request DTO for the POST /shorten endpoint.
 *
 * Validated at the controller level:
 *   - @NotBlank ensures the field is present and non-empty.
 *   - @URL    ensures the value is a well-formed URL.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShortenRequest {

    @NotBlank(message = "URL must not be blank")
    @URL(message = "Provided value is not a valid URL")
    private String url;
}
