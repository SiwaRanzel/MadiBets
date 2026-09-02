package com.bloodline.madibets.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One selectable answer option for a {@link TaskQuestion}. The {@code isCorrect}
 * flag is WRITE_ONLY so it is accepted when a lecturer creates a quiz but never
 * serialized back to students taking it (results stay hidden until submit).
 */
public class TaskOption {
    private int optionID;
    private int questionID;
    private String optionText;
    private boolean isCorrect;
    private int position;

    public TaskOption() {}

    public int getOptionID() { return optionID; }
    public void setOptionID(int optionID) { this.optionID = optionID; }

    public int getQuestionID() { return questionID; }
    public void setQuestionID(int questionID) { this.questionID = questionID; }

    public String getOptionText() { return optionText; }
    public void setOptionText(String optionText) { this.optionText = optionText; }

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public boolean isCorrect() { return isCorrect; }

    @JsonProperty  // accept "isCorrect" on the way in
    public void setCorrect(boolean isCorrect) { this.isCorrect = isCorrect; }

    public int getPosition() { return position; }
    public void setPosition(int position) { this.position = position; }
}
