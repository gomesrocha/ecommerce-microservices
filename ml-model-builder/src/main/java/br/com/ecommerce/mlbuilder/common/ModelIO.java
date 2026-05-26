package br.com.ecommerce.mlbuilder.common;

import org.tribuo.Model;
import org.tribuo.Output;

import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public final class ModelIO {

    private ModelIO() {
    }

    public static <T extends Output<T>> void saveModel(Model<T> model, Path outputPath) {
        try {
            Files.createDirectories(outputPath.getParent());

            try (ObjectOutputStream outputStream = new ObjectOutputStream(
                    Files.newOutputStream(outputPath)
            )) {
                outputStream.writeObject(model);
            }

            System.out.println("Modelo salvo em: " + outputPath.toAbsolutePath());

        } catch (Exception exception) {
            throw new IllegalStateException("Falha ao salvar modelo em " + outputPath, exception);
        }
    }
}

