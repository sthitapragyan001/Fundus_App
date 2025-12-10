package com.example.cancerimager.network;

import com.example.cancerimager.model.DownloadRequestBody;

import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.POST;

public interface ApiService {
    @GET("/api/list")
    Call<List<String>> getPhotos();

    @POST("/api/download")
    Call<ResponseBody> downloadPhotos(@Body DownloadRequestBody body);
}
