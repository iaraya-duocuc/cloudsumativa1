package com.duoc.cloudsumativa1.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsSessionCredentials;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;

@Configuration
@ConditionalOnProperty(name = "app.s3.enabled", havingValue = "true")
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);

    @Bean
    public AwsCredentialsProvider awsCredentialsProvider(
            @Value("${app.aws.access-key:}") String accessKey,
            @Value("${app.aws.secret-key:}") String secretKey,
            @Value("${app.aws.session-token:}") String sessionToken) {

        if (StringUtils.hasText(accessKey) != StringUtils.hasText(secretKey)
                || (StringUtils.hasText(sessionToken) && !StringUtils.hasText(accessKey))) {
            throw new IllegalArgumentException("Configura access-key y secret-key juntos");
        }

        // Sin llaves configuradas: cadena por defecto de AWS (variables de entorno del sistema
        // o IAM Role de la EC2, que es la práctica recomendada en la industria: cero llaves).
        if (!StringUtils.hasText(accessKey) || !StringUtils.hasText(secretKey)) {
            log.warn("No hay credenciales AWS en la configuración; se usa la cadena por defecto del SDK");
            return DefaultCredentialsProvider.builder().build();
        }

        // AWS Academy (Learner Lab): credenciales temporales -> necesitan el session token.
        // Con AwsBasicCredentials (solo 2 partes) AWS responde "The security token included in the request is invalid".
        if (StringUtils.hasText(sessionToken)) {
            return StaticCredentialsProvider.create(
                    AwsSessionCredentials.create(accessKey.trim(), secretKey.trim(), sessionToken.trim()));
        }

        // Cuenta AWS normal con llaves permanentes (sin token).
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey.trim(), secretKey.trim()));
    }
}
