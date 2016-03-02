package mckenna.colin.hw5;

public class TodoItem {
    public enum State {
        Created,
        Due,
        Done
    }
    private long id;
    private String name;
    private String description;
    private int priority;
    private State state;

    public TodoItem() {
        this.id = -1;
        this.name = "";
        this.description = "";
        this.priority = 1;
        this.state = State.Created;
    }
    public TodoItem(long id, String name, String description, int priority, String stateName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.priority = priority;
        this.state = State.valueOf(stateName);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }
}
