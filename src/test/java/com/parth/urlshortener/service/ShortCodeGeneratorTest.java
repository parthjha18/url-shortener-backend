package com.parth.urlshortener.service;

import com.parth.urlshortener.repository.UrlRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortCodeGeneratorTest {

    @Mock
    UrlRepository urlRepository;

    ShortCodeGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new ShortCodeGenerator(urlRepository, 7);
    }

    @Test
    void generate_returnsSevenCharBase62Code() {
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);

        String code = generator.generate();

        assertThat(code).hasSize(7);
        assertThat(code).matches("[a-zA-Z0-9]+");
    }

    @Test
    void generate_retriesOnCollisionAndSucceedsOnSecondAttempt() {
        // First call collides, second call is unique
        when(urlRepository.existsByShortCode(anyString()))
                .thenReturn(true)
                .thenReturn(false);

        String code = generator.generate();

        assertThat(code).hasSize(7);
    }

    @Test
    void generate_throwsIllegalStateAfterMaxRetries() {
        // Every candidate collides — generator should give up cleanly
        when(urlRepository.existsByShortCode(anyString())).thenReturn(true);

        assertThatThrownBy(() -> generator.generate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unique short code");
    }

    @Test
    void generate_producesUniqueCodesOnSuccessiveCallsWithNoCollisions() {
        when(urlRepository.existsByShortCode(anyString())).thenReturn(false);

        String first = generator.generate();
        String second = generator.generate();

        // Base62^7 space is ~3.5 trillion — collision in two calls is astronomically rare
        assertThat(first).isNotEqualTo(second);
    }
}
