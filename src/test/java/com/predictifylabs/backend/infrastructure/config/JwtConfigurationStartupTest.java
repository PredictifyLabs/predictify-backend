package com.predictifylabs.backend.infrastructure.config;

import com.predictifylabs.backend.infrastructure.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.assertj.core.api.Assertions.assertThat;

class JwtConfigurationStartupTest {

    private static final String CANONICAL_SECRET = "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";
    private static final String LEGACY_SECRET = "ZmVkY2JhOTg3NjU0MzIxMGZlZGNiYTk4NzY1NDMyMTA=";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class
            ))
            .withUserConfiguration(JwtServiceTestConfiguration.class);

    @Test
    void shouldUseCanonicalApplicationJwtPropertiesEvenWhenLegacyNamespaceExists() {
        contextRunner
                .withPropertyValues(
                        "application.jwt.secret=" + CANONICAL_SECRET,
                        "application.jwt.expiration=120000",
                        "application.jwt.refresh-expiration=240000",
                        "application.security.jwt.secret-key=" + LEGACY_SECRET,
                        "application.security.jwt.expiration=999999",
                        "application.security.jwt.refresh-token.expiration=999999"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();

                    JwtService jwtService = context.getBean(JwtService.class);
                    UserDetails userDetails = User.withUsername("admin@predictify.dev")
                            .password("ignored")
                            .authorities("ROLE_ADMIN")
                            .build();

                    String token = jwtService.generateToken(userDetails);
                    Claims claims = Jwts.parserBuilder()
                            .setSigningKey(Keys.hmacShaKeyFor(Decoders.BASE64.decode(CANONICAL_SECRET)))
                            .build()
                            .parseClaimsJws(token)
                            .getBody();

                    long ttlMillis = claims.getExpiration().getTime() - claims.getIssuedAt().getTime();
                    assertThat(ttlMillis).isBetween(118_000L, 122_000L);
                });
    }

    @Test
    void shouldFailStartupWhenCanonicalSecretIsMissing() {
        contextRunner
                .withPropertyValues(
                        "application.jwt.expiration=120000",
                        "application.jwt.refresh-expiration=240000"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void shouldFailStartupWhenCanonicalSecretIsBlank() {
        contextRunner
                .withPropertyValues(
                        "application.jwt.secret=",
                        "application.jwt.expiration=120000",
                        "application.jwt.refresh-expiration=240000"
                )
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    static class JwtServiceTestConfiguration {
        @Bean
        JwtService jwtService(JwtProperties jwtProperties) {
            return new JwtService(jwtProperties);
        }
    }
}
