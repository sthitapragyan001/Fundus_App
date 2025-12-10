package com.example.cancerimager;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cancerimager.adapter.PhotoAdapter;
import com.example.cancerimager.network.ApiService;
import com.example.cancerimager.network.RetrofitClient;
import com.example.cancerimager.services.DownloadService;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GalleryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private PhotoAdapter photoAdapter;
    private Button downloadButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);

        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 3));

        downloadButton = findViewById(R.id.download_button);
        downloadButton.setOnClickListener(v -> handleDownloadClick());

        fetchPhotos();
    }

    private void fetchPhotos() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<String>> call = apiService.getPhotos();

        call.enqueue(new Callback<List<String>>() {
            @Override
            public void onResponse(Call<List<String>> call, Response<List<String>> response) {
                if (response.isSuccessful()) {
                    List<String> photoUrls = response.body();
                    photoAdapter = new PhotoAdapter(GalleryActivity.this, photoUrls);
                    recyclerView.setAdapter(photoAdapter);
                } else {
                    Toast.makeText(GalleryActivity.this, "Failed to fetch photos", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<String>> call, Throwable t) {
                Toast.makeText(GalleryActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void handleDownloadClick() {
        if (photoAdapter == null) {
            return;
        }

        List<String> selectedPhotos = photoAdapter.getSelectedPhotos();
        if (selectedPhotos.isEmpty()) {
            Toast.makeText(this, "Please select photos to download", Toast.LENGTH_SHORT).show();
            return;
        }

        // Directly start the download service. No permission check is needed.
        startDownloadService(selectedPhotos);
    }

    private void startDownloadService(List<String> photoUrls) {
        Intent intent = new Intent(this, DownloadService.class);
        intent.putStringArrayListExtra("photo_urls", new ArrayList<>(photoUrls));
        startService(intent);
        Toast.makeText(this, "Downloading selected photos...", Toast.LENGTH_SHORT).show();
    }
}
