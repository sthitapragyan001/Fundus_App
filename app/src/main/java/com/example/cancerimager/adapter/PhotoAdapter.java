package com.example.cancerimager.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.cancerimager.R;

import java.util.ArrayList;
import java.util.List;

public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private Context context;
    private List<String> photoUrls;
    private List<Integer> selectedItems = new ArrayList<>();

    public PhotoAdapter(Context context, List<String> photoUrls) {
        this.context = context;
        this.photoUrls = photoUrls;
    }

    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        String photoUrl = photoUrls.get(position);
        Glide.with(context)
                .load("http://192.168.4.1:8080/photos/" + photoUrl)
                .into(holder.imageView);

        holder.itemView.setSelected(selectedItems.contains(position));

        holder.itemView.setOnClickListener(v -> {
            if (selectedItems.contains(position)) {
                selectedItems.remove(Integer.valueOf(position));
                holder.itemView.setSelected(false);
            } else {
                selectedItems.add(position);
                holder.itemView.setSelected(true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return photoUrls.size();
    }

    public List<String> getSelectedPhotos() {
        List<String> selectedPhotos = new ArrayList<>();
        for (int position : selectedItems) {
            selectedPhotos.add(photoUrls.get(position));
        }
        return selectedPhotos;
    }

    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageView);
        }
    }
}
