package com.tks.dto;

import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

public class FileUploadInput {

    @RestForm("file")
    public FileUpload file;
}