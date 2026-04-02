package com.bitiriciler32.cms.media.config;
import io.minio.MinioClient;
import okhttp3.OkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
@Configuration
public class MinioConfig {
    @Value("${minio.endpoint}")
    private String endpoint;
    @Value("${minio.access-key}")
    private String accessKey;
    @Value("${minio.secret-key}")
    private String secretKey;
    /**
     * Optional path to a PEM certificate file (.crt) to add as a trusted CA.
     * Use when MINIO_ENDPOINT is HTTPS with a self-signed certificate.
     * If blank, the default JVM trust store is used (works for HTTP or CA-signed TLS).
     */
    @Value("${minio.ca-cert-path:}")
    private String caCertPath;
    @Bean
    public MinioClient minioClient() {
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey);
        if (caCertPath != null && !caCertPath.isBlank()) {
            builder.httpClient(buildTrustedHttpClient(caCertPath));
        }
        return builder.build();
    }
    /**
     * Builds an OkHttpClient that trusts the certificate loaded from the given PEM file.
     * This is the proper alternative to disabling SSL verification: we trust the specific
     * self-signed cert rather than accepting all certificates blindly.
     */
    private OkHttpClient buildTrustedHttpClient(String certPath) {
        try {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            Certificate cert;
            try (FileInputStream fis = new FileInputStream(certPath)) {
                cert = cf.generateCertificate(fis);
            }
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
            ks.load(null, null);
            ks.setCertificateEntry("minio-ca", cert);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);
            X509TrustManager trustManager =
                    (X509TrustManager) tmf.getTrustManagers()[0];
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sslContext.getSocketFactory(), trustManager)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to load MinIO CA certificate from: " + certPath, e);
        }
    }
}