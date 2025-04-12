package org.eclipse.dirigible.components.base.tracing;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;

/**
 * The Class TaskContext.
 */
@Entity
@jakarta.persistence.Table(name = "DIRIGIBLE_TASK_STATE_VARIABLE")
public class TaskStateVariable {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TSV_ID", nullable = false)
    private Long id;

    /** The task state reference. */
    @ManyToOne(fetch = FetchType.EAGER, optional = true)
    @JoinColumn(name = "TSV_TS_ID", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private TaskState taskState;

    /** The variable name. */
    @Column(name = "TSV_NAME", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    protected String name;

    /** The variable value. */
    @Column(name = "TSV_VALUE", columnDefinition = "VARCHAR", nullable = true, length = 20000)
    @Expose
    protected String value;

    /**
     * Instantiates a new task context.
     *
     * @param taskState the task state
     * @param name the name
     * @param value the value
     */
    public TaskStateVariable(TaskState taskState, String name, String value) {
        super();
        this.taskState = taskState;
        this.name = name;
        this.value = value;
    }

    /**
     * Gets the id.
     *
     * @return the id
     */
    public Long getId() {
        return id;
    }

    /**
     * Sets the id.
     *
     * @param id the id to set
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * Gets the task state.
     *
     * @return the taskState
     */
    public TaskState getTaskState() {
        return taskState;
    }

    /**
     * Sets the task state.
     *
     * @param taskState the taskState to set
     */
    public void setTaskState(TaskState taskState) {
        this.taskState = taskState;
    }

    /**
     * Gets the name.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value.
     *
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "TaskContext [id=" + id + ", taskState=" + taskState + ", name=" + name + ", value=" + value + "]";
    }

}
