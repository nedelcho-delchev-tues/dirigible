package org.eclipse.dirigible.components.base.tracing;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.hibernate.annotations.LazyCollection;
import org.hibernate.annotations.LazyCollectionOption;

import com.google.gson.annotations.Expose;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;

/**
 * The Class TaskState.
 */
@Entity
@jakarta.persistence.Table(name = "DIRIGIBLE_TASK_STATE")
public class TaskState {

    /** The id. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "TS_ID", nullable = false)
    private Long id;

    /** The task stauts. */
    @Column(name = "TS_TYPE", columnDefinition = "VARCHAR", nullable = false, length = 20)
    @Expose
    @Enumerated(EnumType.STRING)
    protected TaskType type;

    /** The execution key. */
    @Column(name = "TS_EXECUTION", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    protected String execution;

    /** The execution step. */
    @Column(name = "TS_STEP", columnDefinition = "VARCHAR", nullable = false, length = 255)
    @Expose
    protected String step;

    /** The execution started timestamp. */
    @Column(name = "TS_STARTED", columnDefinition = "TIMESTAMP", nullable = false)
    @Expose
    protected Timestamp started;

    /** The execution started timestamp. */
    @Column(name = "TS_ENDED", columnDefinition = "TIMESTAMP", nullable = true)
    @Expose
    protected Timestamp ended;

    /** The task stauts. */
    @Column(name = "TS_STATUS", columnDefinition = "INTEGER", nullable = false)
    @Expose
    @Enumerated(EnumType.ORDINAL)
    protected TaskStatus status;

    /** The execution step. */
    @Column(name = "TS_ERROR", columnDefinition = "VARCHAR", nullable = true, length = 2000)
    @Expose
    protected String error;

    /** The input variables. */
    @OneToMany(mappedBy = "taskState", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    @Expose
    protected List<TaskStateVariable> input = new ArrayList<TaskStateVariable>();

    /** The output variables. */
    @OneToMany(mappedBy = "taskState", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @LazyCollection(LazyCollectionOption.FALSE)
    @Expose
    protected List<TaskStateVariable> output = new ArrayList<TaskStateVariable>();

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
     * Gets the type.
     *
     * @return the type
     */
    public TaskType getType() {
        return type;
    }

    /**
     * Sets the type.
     *
     * @param type the type to set
     */
    public void setType(TaskType type) {
        this.type = type;
    }

    /**
     * Gets the execution.
     *
     * @return the execution
     */
    public String getExecution() {
        return execution;
    }

    /**
     * Sets the execution.
     *
     * @param execution the execution to set
     */
    public void setExecution(String execution) {
        this.execution = execution;
    }

    /**
     * Gets the step.
     *
     * @return the step
     */
    public String getStep() {
        return step;
    }

    /**
     * Sets the step.
     *
     * @param step the step to set
     */
    public void setStep(String step) {
        this.step = step;
    }

    /**
     * Gets the started.
     *
     * @return the started
     */
    public Timestamp getStarted() {
        return started;
    }

    /**
     * Sets the started.
     *
     * @param started the started to set
     */
    public void setStarted(Timestamp started) {
        this.started = started;
    }

    /**
     * Gets the ended.
     *
     * @return the ended
     */
    public Timestamp getEnded() {
        return ended;
    }

    /**
     * Sets the ended.
     *
     * @param ended the ended to set
     */
    public void setEnded(Timestamp ended) {
        this.ended = ended;
    }

    /**
     * Gets the status.
     *
     * @return the status
     */
    public TaskStatus getStatus() {
        return status;
    }

    /**
     * Sets the status.
     *
     * @param status the status to set
     */
    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    /**
     * Gets the error.
     *
     * @return the error
     */
    public String getError() {
        return error;
    }

    /**
     * Sets the error.
     *
     * @param error the new error
     */
    public void setError(String error) {
        this.error = error;
    }

    /**
     * Gets the input.
     *
     * @return the input
     */
    public List<TaskStateVariable> getInput() {
        return input;
    }

    /**
     * Sets the input.
     *
     * @param input the input to set
     */
    public void setInput(List<TaskStateVariable> input) {
        this.input = input;
    }

    /**
     * Gets the output.
     *
     * @return the output
     */
    public List<TaskStateVariable> getOutput() {
        return output;
    }

    /**
     * Sets the output.
     *
     * @param output the output to set
     */
    public void setOutput(List<TaskStateVariable> output) {
        this.output = output;
    }

    /**
     * To string.
     *
     * @return the string
     */
    @Override
    public String toString() {
        return "TaskState [id=" + id + ", type=" + type + ", execution=" + execution + ", step=" + step + ", started=" + started
                + ", ended=" + ended + ", status=" + status + ", error=" + error + ", input=" + Arrays.toString(input.toArray())
                + ", output=" + Arrays.toString(output.toArray()) + "]";
    }



}
