package com.sigma.supabasetodoapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sigma.supabasetodoapp.R;
import com.sigma.supabasetodoapp.models.PersonalGoal;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalVH> {

    public interface OnGoalActionListener {
        void onDelete(PersonalGoal goal);
        void onEdit(PersonalGoal goal);
    }

    private List<PersonalGoal> goals = new ArrayList<>();
    private OnGoalActionListener listener;

    public void setListener(OnGoalActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public GoalVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_goal, parent, false);
        return new GoalVH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull GoalVH h, int position) {
        PersonalGoal g = goals.get(position);

        h.tvGoalText.setText(g.getGoalText());
        h.tvDesiredResult.setText("Результат: " + g.getDesiredResult());

        if (g.getTargetDate() != null) {
            try {
                SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                SimpleDateFormat ru = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru"));
                Date d = iso.parse(g.getTargetDate());
                h.tvTargetDate.setText("Дата: " + ru.format(d));
            } catch (Exception e) {
                h.tvTargetDate.setText("Дата: " + g.getTargetDate());
            }
        } else {
            h.tvTargetDate.setText("Дата: —");
        }

        h.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(g);
        });

        h.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onEdit(g);
            return true;
        });
    }

    @Override
    public int getItemCount() { return goals.size(); }

    public void setGoals(List<PersonalGoal> goals) {
        this.goals = goals;
        notifyDataSetChanged();
    }

    static class GoalVH extends RecyclerView.ViewHolder {
        TextView tvGoalText, tvDesiredResult, tvTargetDate;
        ImageButton btnDelete;

        public GoalVH(@NonNull View itemView) {
            super(itemView);
            tvGoalText = itemView.findViewById(R.id.tvGoalText);
            tvDesiredResult = itemView.findViewById(R.id.tvDesiredResult);
            tvTargetDate = itemView.findViewById(R.id.tvTargetDate);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
