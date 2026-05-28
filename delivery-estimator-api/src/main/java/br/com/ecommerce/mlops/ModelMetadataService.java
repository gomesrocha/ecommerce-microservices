package br.com.ecommerce.mlops;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.io.InputStream;

@ApplicationScoped
public class ModelMetadataService {

    private static final String METADATA_RESOURCE = "ml/model-metadata.json";

    @Inject
    ObjectMapper objectMapper;

    public ModelInfoResponse getModelInfo() {
        try (InputStream inputStream = Thread.currentThread()
                .getContextClassLoader()
                .getResourceAsStream(METADATA_RESOURCE)) {

            if (inputStream == null) {
                throw new IllegalStateException("Metadata do modelo não encontrado: " + METADATA_RESOURCE);
            }

            return objectMapper.readValue(inputStream, ModelInfoResponse.class);

        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao carregar metadata do modelo", exception);
        }
    }
}