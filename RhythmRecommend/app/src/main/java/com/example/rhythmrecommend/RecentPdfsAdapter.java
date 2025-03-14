package com.example.rhythmrecommend;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class RecentPdfsAdapter extends RecyclerView.Adapter<RecentPdfsAdapter.RecentPdfViewHolder> {
    private List<Uri> recentPdfs;
    private Context context;

    public RecentPdfsAdapter(List<Uri> recentPdfs, Context context) {
        this.recentPdfs = recentPdfs;
        this.context = context;
    }

    @NonNull
    @Override
    public RecentPdfViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recent_pdf, parent, false);
        return new RecentPdfViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecentPdfViewHolder holder, int position) {
        Uri pdfUri = recentPdfs.get(position);

        // Generate the thumbnail
        Bitmap thumbnail = generatePdfThumbnail(pdfUri);
        if (thumbnail != null) {
            holder.thumbnailImageView.setImageBitmap(thumbnail);
        } else {
            // If there's no thumbnail, you can display a default image
            holder.thumbnailImageView.setImageResource(R.drawable.logo);
        }

        holder.itemView.setOnClickListener(v -> {
            // Handle item click and open the PDF
            Intent intent = new Intent(context, ReadingActivity.class);
            intent.setData(pdfUri);
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return recentPdfs.size();
    }

    private Bitmap generatePdfThumbnail(Uri pdfUri) {
        Bitmap bitmap = null;
        try {
            // Use ContentResolver to open the PDF as InputStream
            InputStream inputStream = context.getContentResolver().openInputStream(pdfUri);

            // Use PdfRenderer with ParcelFileDescriptor from InputStream
            if (inputStream != null) {
                ParcelFileDescriptor parcelFileDescriptor = getFileDescriptorFromInputStream(inputStream);
                if (parcelFileDescriptor != null) {
                    PdfRenderer pdfRenderer = new PdfRenderer(parcelFileDescriptor);
                    PdfRenderer.Page page = pdfRenderer.openPage(0);  // Open the first page

                    // Scale the page to fit within the thumbnail
                    int width = 200;  // Set desired width for the thumbnail
                    int height =200;  // Make 1:1 aspect ratio

                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

                    page.close();
                    pdfRenderer.close();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    // Helper method to get ParcelFileDescriptor from InputStream
    private ParcelFileDescriptor getFileDescriptorFromInputStream(InputStream inputStream) {
        try {
            File file = new File(context.getCacheDir(), "temp.pdf");
            file.deleteOnExit();
            try (FileOutputStream out = new FileOutputStream(file)) {
                // Copy InputStream to a temporary file
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            // Return ParcelFileDescriptor for PdfRenderer
            return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }


    public static class RecentPdfViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnailImageView;

        public RecentPdfViewHolder(View itemView) {
            super(itemView);
            thumbnailImageView = itemView.findViewById(R.id.thumbnail_image_view);
        }
    }
}
