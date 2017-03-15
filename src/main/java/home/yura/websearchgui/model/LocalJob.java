package home.yura.websearchgui.model;

import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Optional;

import static java.util.Optional.ofNullable;

/**
 * @author yuriy.dunko on 14.03.17.
 */
@AutoValue
public abstract class LocalJob implements AbstractModel {

    public static enum Status {
        STARTED, RUNNING, FINISHED, FAILED
    }

    public static LocalJob create(final String name,
                                  final Long requiredStep,
                                  final Integer destinationId) {
        return create(null, name, null, null, requiredStep, destinationId, Status.STARTED);
    }

    public static LocalJob create(final Integer id,
                                  final String name,
                                  final Long firstStep,
                                  final Long lastStep,
                                  final Long requiredStep,
                                  final Integer destinationId,
                                  final Status status) {
        return new AutoValue_LocalJob(id, name, firstStep, lastStep, ofNullable(requiredStep).orElse(NULL_SIMPLE_VALUE),
                destinationId, status);
    }

    public LocalJob copyWithId(final int id) {
        return create(id,
                getName(),
                getFirstStep(),
                getLastStep(),
                getRequiredStep(),
                getDestinationId(),
                getStatus());
    }

    public LocalJob copyWithRunningStatus(final Long firstStep, final Long lastStep) {
        return create(getId(),
                getName(),
                firstStep,
                lastStep,
                getRequiredStep(),
                getDestinationId(),
                Status.RUNNING);
    }

    public LocalJob copyWithStatus(final Status status) {
        return create(getId(),
                getName(),
                getFirstStep(),
                getLastStep(),
                getRequiredStep(),
                getDestinationId(),
                status);
    }

    public abstract String getName();

    @Nullable
    public abstract Long getFirstStep();

    @Nullable
    public abstract Long getLastStep();

    @Nullable
    public Long getRequiredStep() {
        final long value = getRequiredStepSimpleValue();
        if (value == NULL_SIMPLE_VALUE) {
            return null;
        }
        return value;
    }

    abstract long getRequiredStepSimpleValue();

    public abstract int getDestinationId();

    public abstract Status getStatus();
}
