package com.example.cancerimager.model;

import java.util.List;

public class DownloadRequestBody {
    private List<String> filenames;

    public DownloadRequestBody(List<String> filenames) {
        this.filenames = filenames;
    }
}
