package com.sigma.supabasetodoapp.models;

public class PersonalGoal {
    private String id;
    private String goalText;
    private double desiredResult;
    private String targetDate;

    public PersonalGoal() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getGoalText() { return goalText; }
    public void setGoalText(String goalText) { this.goalText = goalText; }

    public double getDesiredResult() { return desiredResult; }
    public void setDesiredResult(double desiredResult) { this.desiredResult = desiredResult; }

    public String getTargetDate() { return targetDate; }
    public void setTargetDate(String targetDate) { this.targetDate = targetDate; }
}
