package com.example.smiledetector;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class FaceDetectionAdapter extends RecyclerView.Adapter<FaceDetectionAdapter.ViewHolder> {
    private List<FaceDetectionModel> faceDetectionModelList;
    private Context context;

    public FaceDetectionAdapter(List<FaceDetectionModel> faceDetectionModelList, Context context) {
        this.faceDetectionModelList = faceDetectionModelList;
        this.context = context;
    }

    @NonNull
    @Override
    public FaceDetectionAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.face_detection,parent,false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FaceDetectionAdapter.ViewHolder holder, int position) {
        FaceDetectionModel faceDetectionModel = faceDetectionModelList.get(position);
        holder.text1.setText(String.valueOf(faceDetectionModel.getId()));
        holder.text2.setText(String.valueOf(faceDetectionModel.getText()));
    }

    @Override
    public int getItemCount() {
        return faceDetectionModelList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView text1;
        public TextView text2;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(R.id.face_detection_tv1);
            text2 = itemView.findViewById(R.id.face_detection_tv2);

        }
    }
}
