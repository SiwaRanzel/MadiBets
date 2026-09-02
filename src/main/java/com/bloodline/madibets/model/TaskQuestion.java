package com.bloodline.madibets.model;

import java.util.ArrayList;
import java.util.List;

/**
 * One question within a Task (quiz). A question is either TRUE_FALSE or
 * MULTIPLE_CHOICE and owns a list of {@link TaskOption}s, exactly one of which
 * is flagged correct.
 */
public class TaskQuestion {
    private int questionID;
    private int taskID;
    private String prompt;
    private String type;      // TRUE_FALSE | MULTIPLE_CHOICE
    private int position;
    private List<TaskOption> options = new ArrayList<>();

    public TaskQuestion() {}

    public int getQuestionID() { return questionID; }
    public void setQuestionID(int questionID) { this.questionID = questionID; }

    public int getTaskID() { return taskID; }
    public void setTaskID(int taskID) { this.taskID = taskID; }

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }

    public List<TaskOption> getOptions() { return options; }
    public void setOptions(List<TaskOption> options) { this.options = options; }
}
