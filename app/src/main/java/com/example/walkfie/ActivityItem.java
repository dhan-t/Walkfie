package com.example.walkfie;
public class ActivityItem {
    private String name;
    private String buttonText;

    public ActivityItem(String name, String buttonText) {
        this.name = name;
        this.buttonText = buttonText;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getButtonText() {
        return buttonText;
    }

    public void setButtonText(String buttonText) {
        this.buttonText = buttonText;
    }
}