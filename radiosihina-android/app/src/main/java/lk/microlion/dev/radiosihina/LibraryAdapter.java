package lk.microlion.dev.radiosihina;

import android.media.MediaPlayer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.ViewHolder> {

    private final ArrayList<ArrayList<String>> localDataSet;
    private final MainActivity mainActivity;

    public LibraryAdapter(ArrayList<ArrayList<String>> dataSet, MainActivity mainActivity){
        this.localDataSet = dataSet;
        this.mainActivity = mainActivity;
    }

    @NonNull
    @Override
    public LibraryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_library_object, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LibraryAdapter.ViewHolder holder, int position) {
        ArrayList<String> libraryList = localDataSet.get(position);
        holder.getTxtName().setText(libraryList.get(0));
        holder.getTxtPresenter().setText(libraryList.get(1));
        holder.getTxtLibraryDate().setText(libraryList.get(2));
        holder.getBtnPlay().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mainActivity.showSelectedLibraryPlayer(libraryList.get(0), libraryList.get(1), libraryList.get(2), libraryList.get(3));
            }
        });
    }

    @Override
    public int getItemCount() {
        return localDataSet.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView txtName;
        private final TextView txtPresenter;
        private final TextView txtLibraryDate;
        private final MaterialButton btnPlay;

        public ViewHolder(View view) {
            super(view);
            txtName = view.findViewById(R.id.txtLibraryName);
            txtPresenter = view.findViewById(R.id.txtLibraryBy);
            txtLibraryDate = view.findViewById(R.id.txtLibraryDate);
            btnPlay = view.findViewById(R.id.btnLibraryPlay);
        }

        public TextView getTxtName() {
            return txtName;
        }

        public TextView getTxtPresenter() {
            return txtPresenter;
        }

        public TextView getTxtLibraryDate() {
            return txtLibraryDate;
        }

        public MaterialButton getBtnPlay() {
            return btnPlay;
        }
    }
}
