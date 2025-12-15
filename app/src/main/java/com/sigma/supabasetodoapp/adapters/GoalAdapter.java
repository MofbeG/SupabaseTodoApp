package com.sigma.supabasetodoapp.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.sigma.supabasetodoapp.R;
import com.sigma.supabasetodoapp.models.PersonalGoal;

import java.util.ArrayList;
import java.util.List;

public class GoalAdapter extends RecyclerView.Adapter<GoalAdapter.GoalVH> {

    private List<PersonalGoal> goals = new ArrayList<>();

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
        h.tvTargetDate.setText("Дата: " + (g.getTargetDate() == null ? "-" : g.getTargetDate()));
    }

    @Override
    public int getItemCount() {
        return goals.size();
    }

    public void setGoals(List<PersonalGoal> goals) {
        this.goals = goals;
        notifyDataSetChanged();
    }

    static class GoalVH extends RecyclerView.ViewHolder {
        TextView tvGoalText, tvDesiredResult, tvTargetDate;

        public GoalVH(@NonNull View itemView) {
            super(itemView);
            tvGoalText = itemView.findViewById(R.id.tvGoalText);
            tvDesiredResult = itemView.findViewById(R.id.tvDesiredResult);
            tvTargetDate = itemView.findViewById(R.id.tvTargetDate);
        }
    }
}
