package com.tks.dto;

import java.util.UUID;

public class FileDTO {
    private UUID id; // Nome do arquivo servindo como identificador único
    private String filename;
    private long sizeInBytes;

    public FileDTO(UUID id, String filename, long sizeInBytes) {
        this.id = id;
        this.filename = filename;
        this.sizeInBytes = sizeInBytes;
    }

    public UUID getId() {
        return id;
    }

    public String getFilename() {
        return filename;
    }

    public long getSizeInBytes() {
        return sizeInBytes;
    }
}