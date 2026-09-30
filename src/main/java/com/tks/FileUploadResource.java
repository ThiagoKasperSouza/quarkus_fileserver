package com.tks;

// Imports do JAX-RS / Quarkus
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

// Import do MicroProfile Config
import org.eclipse.microprofile.config.inject.ConfigProperty;

// Imports de IO e Utilidades do Java
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.tks.dto.FileDTO;
import com.tks.dto.FileUploadInput;

@Path("/files")
public class FileUploadResource {

    @ConfigProperty(name = "file.upload.directory", defaultValue = "uploads")
    String uploadDir;

    // 1. UPLOAD DE ARQUIVO COM UUID
    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadFile(FileUploadInput input) {
        if (input == null || input.file == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(Map.of("error", "Nenhum arquivo foi enviado."))
                    .build();
        }

        try {
            java.nio.file.Path targetDir = java.nio.file.Path.of(uploadDir);
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }

            String originalFileName = input.file.fileName();
            String extension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalFileName.substring(dotIndex);
            }

            String uuid = UUID.randomUUID().toString();
            String storedFileName = uuid + extension;

            java.nio.file.Path destination = targetDir.resolve(storedFileName);
            Files.copy(input.file.filePath(), destination, StandardCopyOption.REPLACE_EXISTING);

            return Response.ok(Map.of(
                    "message", "Arquivo salvo com sucesso!",
                    "id", storedFileName,
                    "originalName", originalFileName,
                    "path", destination.toString()
            )).build();

        } catch (IOException e) {
            return Response.serverError()
                    .entity(Map.of("error", "Falha ao salvar arquivo: " + e.getMessage()))
                    .build();
        }
    }

    // 2. LISTAR TODOS OS ARQUIVOS
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response listFiles() {
        java.nio.file.Path targetDir = java.nio.file.Path.of(uploadDir);
        if (!Files.exists(targetDir)) {
            return Response.ok(List.of()).build();
        }

        try (Stream<java.nio.file.Path> stream = Files.list(targetDir)) {
            List<FileDTO> files = stream
                    .filter(Files::isRegularFile)
                    .map(this::toFileDTO)
                    .flatMap(Optional::stream)
                    .collect(Collectors.toList());

            return Response.ok(files).build();
        } catch (IOException e) {
            return Response.serverError()
                    .entity(Map.of("error", "Erro ao listar arquivos: " + e.getMessage()))
                    .build();
        }
    }

    // 3. BUSCAR / DOWNLOAD POR ID (NOME SALVO COM UUID)
    @GET
    @Path("/{id}")
    public Response findById(@PathParam("id") String id) {
        java.nio.file.Path filePath = java.nio.file.Path.of(uploadDir).resolve(id);
        File file = filePath.toFile();

        if (!file.exists() || !file.isFile()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Arquivo não encontrado."))
                    .build();
        }

        String contentType;
        try {
            contentType = Files.probeContentType(filePath);
        } catch (IOException e) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return Response.ok(file)
                .header("Content-Disposition", "inline; filename=\"" + file.getName() + "\"")
                .type(contentType)
                .build();
    }

    // 4. DELETAR POR ID
    @DELETE
    @Path("/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteById(@PathParam("id") String id) {
        java.nio.file.Path filePath = java.nio.file.Path.of(uploadDir).resolve(id);
        File file = filePath.toFile();

        if (!file.exists() || !file.isFile()) {
            return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", "Arquivo não encontrado para exclusão."))
                    .build();
        }

        try {
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                return Response.ok(Map.of(
                        "message", "Arquivo removido com sucesso!",
                        "id", id
                )).build();
            } else {
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR)
                        .entity(Map.of("error", "Não foi possível deletar o arquivo."))
                        .build();
            }
        } catch (IOException e) {
            return Response.serverError()
                    .entity(Map.of("error", "Erro ao tentar deletar o arquivo: " + e.getMessage()))
                    .build();
        }
    }

    // Método auxiliar para converter o Path em FileDTO com validação de UUID
    private Optional<FileDTO> toFileDTO(java.nio.file.Path path) {
        String fileName = path.getFileName().toString();
        int lastDotIndex = fileName.lastIndexOf('.');
        String rawUuid = (lastDotIndex > 0) ? fileName.substring(0, lastDotIndex) : fileName;

        try {
            UUID uuid = UUID.fromString(rawUuid);
            return Optional.of(new FileDTO(uuid, fileName, path.toFile().length()));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}