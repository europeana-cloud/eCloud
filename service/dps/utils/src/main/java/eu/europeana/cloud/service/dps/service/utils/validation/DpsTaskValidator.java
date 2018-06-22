package eu.europeana.cloud.service.dps.service.utils.validation;

import com.google.common.base.Functions;
import com.google.common.collect.Lists;
import eu.europeana.cloud.common.model.Revision;
import eu.europeana.cloud.service.commons.urls.UrlParser;
import eu.europeana.cloud.service.dps.DpsTask;
import eu.europeana.cloud.service.dps.InputDataType;
import eu.europeana.cloud.service.dps.exception.DpsTaskValidationException;
import org.apache.commons.validator.routines.UrlValidator;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import static eu.europeana.cloud.service.dps.service.utils.validation.InputDataValueType.*;

public class DpsTaskValidator {


    private List<DpsTaskConstraint> dpsTaskConstraints = new ArrayList<>();
    private String validatorName;
    private boolean revisionMustExist = false;

    public DpsTaskValidator() {
        this("Default validator");
    }

    public DpsTaskValidator(String validatorName) {
        this.validatorName = validatorName;
    }

    public DpsTaskValidator withParameter(String parameterName) {
        DpsTaskConstraint constraint = DpsTaskConstraint.newDpsTaskConstraint()
                .fieldType(DpsTaskFieldType.PARAMETER)
                .expectedName(parameterName)
                .build();
        dpsTaskConstraints.add(constraint);
        return this;

    }

    public String getValidatorName() {
        return validatorName;
    }

    /**
     * Will check if dps task contains parameter with selected name and selected value
     *
     * @param parameterName
     * @param parameterValue
     * @return
     */
    public DpsTaskValidator withParameter(String parameterName, String parameterValue) {
        DpsTaskConstraint constraint = DpsTaskConstraint.newDpsTaskConstraint()
                .fieldType(DpsTaskFieldType.PARAMETER)
                .expectedName(parameterName)
                .expectedValue(parameterValue)
                .build();
        dpsTaskConstraints.add(constraint);
        return this;
    }


    /**
     * Will check if dps task contains parameter with selected name and any of the allowed values
     *
     * @param paramName     parameter name
     * @param allowedValues list of allowed values
     * @return
     */
    public DpsTaskValidator withParameter(String paramName, List allowedValues) {
        DpsTaskConstraint constraint = DpsTaskConstraint.newDpsTaskConstraint()
                .fieldType(DpsTaskFieldType.PARAMETER)
                .expectedName(paramName)
                .expectedValue(allowedValues)
                .build();

        dpsTaskConstraints.add(constraint);
        return this;
    }

    /**
     * Will check if dps task contains parameter with selected name and no value
     *
     * @param parameterName
     * @return
     */
    public DpsTaskValidator withEmptyParameter(String parameterName) {
        DpsTaskConstraint constraint = DpsTaskConstraint.newDpsTaskConstraint()
                .fieldType(DpsTaskFieldType.PARAMETER)
                .expectedName(parameterName)
                .expectedValue("")
                .build();

        dpsTaskConstraints.add(constraint);
        return this;
    }

    /**
     * Will check if dps task contains input data with selected name (value of this input data will not be validated)
     *
     * @param inputDataName
     * @return
     */
    public DpsTaskValidator withDataEntry(String inputDataName) {
        DpsTaskConstraint constraint = DpsTaskConstraint.newDpsTaskConstraint()
                .fieldType(DpsTaskFieldType.INPUT_DATA)
                .expectedName(inputDataName)
                .build();
        dpsTaskConstraints.add(constraint);
        return this;
    }

    /**
     * Will check if dps task contains input data with selected name and selected value
     *
     * @param entryName
     * @param entryValue
     * @return
     */
    public DpsTaskValidator withDataEntry(String entryName, Object entryValue) {
        DpsTaskConstraint constraint = DpsTaskConstraint.newDpsTaskConstraint()
                .fieldType(DpsTaskFieldType.INPUT_DATA)
                .expectedName(entryName)
                .expectedValue(entryValue)
                .build();
        dpsTaskConstraints.add(constraint);
        return this;
    }

    /**
     * Will check if dps task contains input data with selected name and selected content type
     *
     * @param entryName
     * @param contentType content type of input data entry (can be file url, dataset url, ...)
     * @return
     */
    public DpsTaskValidator withDataEntry(String entryName, InputDataValueType contentType) {
        DpsTaskConstraint constraint = DpsTaskConstraint.newDpsTaskConstraint()
                .fieldType(DpsTaskFieldType.INPUT_DATA)
                .expectedName(entryName)
                .expectedValueType(contentType)
                .build();
        dpsTaskConstraints.add(constraint);
        return this;
    }


    /**
     * Will check if dps task contains selected name
     *
     * @param taskName
     * @return
     */
    public DpsTaskValidator withName(String taskName) {
        DpsTaskConstraint constraint = DpsTaskConstraint.newDpsTaskConstraint()
                .fieldType(DpsTaskFieldType.NAME)
                .expectedName(null)
                .expectedValue(taskName)
                .build();
        dpsTaskConstraints.add(constraint);
        return this;
    }

    /**
     * Will check if dps task contains any name
     *
     * @return
     */
    public DpsTaskValidator withAnyName() {
        DpsTaskConstraint constraint = DpsTaskConstraint.newDpsTaskConstraint()
                .fieldType(DpsTaskFieldType.NAME)
                .build();
        dpsTaskConstraints.add(constraint);
        return this;
    }

    /**
     * Will check if dps task contains selected task id
     *
     * @param taskId
     * @return
     */
    public DpsTaskValidator withId(long taskId) {
        DpsTaskConstraint constraint = DpsTaskConstraint.newDpsTaskConstraint()
                .fieldType(DpsTaskFieldType.ID)
                .expectedName(null)
                .expectedValue(taskId + "")
                .build();
        dpsTaskConstraints.add(constraint);
        return this;
    }

    /**
     * Will check if dps task contains any task id
     *
     * @return
     */
    public DpsTaskValidator withAnyId() {
        DpsTaskConstraint constraint = DpsTaskConstraint.newDpsTaskConstraint()
                .fieldType(DpsTaskFieldType.ID)
                .build();
        dpsTaskConstraints.add(constraint);
        return this;
    }


    /**
     * Will check if dps task contains any task id
     *
     * @return
     */
    public DpsTaskValidator withAnyOutputRevision() {
        revisionMustExist = true;
        DpsTaskConstraint constraint = DpsTaskConstraint.newDpsTaskConstraint()
                .fieldType(DpsTaskFieldType.OUTPUT_REVISION)
                .build();
        dpsTaskConstraints.add(constraint);
        return this;
    }

    public DpsTaskValidator withOptionalOutputRevision() {
        DpsTaskConstraint constraint = DpsTaskConstraint.newDpsTaskConstraint()
                .fieldType(DpsTaskFieldType.OUTPUT_REVISION)
                .build();
        dpsTaskConstraints.add(constraint);
        return this;
    }

    public void validate(DpsTask task) throws DpsTaskValidationException {
        for (DpsTaskConstraint re : dpsTaskConstraints) {
            DpsTaskFieldType fieldType = re.getFieldType();
            if (fieldType.equals(DpsTaskFieldType.NAME)) {
                validateName(task, re);
            } else if (fieldType.equals(DpsTaskFieldType.PARAMETER)) {
                validateParameter(task, re);
            } else if (fieldType.equals(DpsTaskFieldType.INPUT_DATA)) {
                validateInputData(task, re);
            } else if (fieldType.equals(DpsTaskFieldType.ID)) {
                validateId(task, re);
            } else if (fieldType.equals(DpsTaskFieldType.OUTPUT_REVISION)) {
                validateOutputRevision(task, revisionMustExist);
            }
        }
    }


    private void validateName(DpsTask task, DpsTaskConstraint constraint) throws DpsTaskValidationException {
        String taskName = task.getTaskName();
        if (constraint.getExpectedValue() == null && taskName != null) {  //any name
            return;
        }
        if ("".equals(constraint.getExpectedValue()) && "".equals(taskName)) {//empty name
            return;
        }
        if (constraint.getExpectedValue().equals(taskName)) {//exact name
            return;
        }

        throw new DpsTaskValidationException("Task name is not valid.");
    }

    private void validateParameter(DpsTask task, DpsTaskConstraint constraint) throws DpsTaskValidationException {
        String parameterValue = task.getParameter(constraint.getExpectedName());
        if (parameterValue == null) {
            throw new DpsTaskValidationException("Expected parameter does not exist in dpsTask. Parameter name: " + constraint.getExpectedName());
        }
        Object expectedValue = constraint.getExpectedValue();
        if (expectedValue == null) {  //any name
            return;
        }
        if (expectedValue instanceof List) {
            List<String> ls = (List) expectedValue;
            if (ls.contains(parameterValue))
                return;
        } else {
            if ("".equals(expectedValue) && "".equals(parameterValue)) {  //empty value
                return;
            }
            if (parameterValue.equals(expectedValue)) {  //exact value
                return;
            }
        }
        throw new DpsTaskValidationException("Parameter does not meet constraints. Parameter name: " + constraint.getExpectedName());
    }

    private void validateInputData(DpsTask task, DpsTaskConstraint constraint) throws DpsTaskValidationException {
        final InputDataType dataType;
        try {
            dataType = InputDataType.valueOf(constraint.getExpectedName());
        } catch (IllegalArgumentException e) {
            throw new DpsTaskValidationException("Input data is not valid.");
        }
        List<String> expectedInputData = task.getDataEntry(dataType);

        if (expectedInputData == null) {
            throw new DpsTaskValidationException("Expected parameter does not exist in dpsTask. Parameter name: " + constraint.getExpectedName());
        }
        if (constraint.getExpectedValueType() != null) {
            validateInputDataContent(expectedInputData, constraint);
        }
        if (constraint.getExpectedValue() == null) {   //any value
            return;
        }
        if ("".equals(constraint.getExpectedValue()) && expectedInputData.isEmpty()) {    //empty value
            return;
        }
        if (expectedInputData.equals(constraint.getExpectedValue())) {  //exact value
            return;
        }
        throw new DpsTaskValidationException("Input data is not valid.");
    }

    private void validateInputDataContent(List<String> expectedInputData, DpsTaskConstraint constraint) throws DpsTaskValidationException {
        for (String expectedInputDataValue : expectedInputData) {
            try {
                if (constraint.getExpectedValueType().equals(LINK_TO_FILE)) {
                    tryValidateFileUrl(expectedInputDataValue);
                } else if (constraint.getExpectedValueType().equals(LINK_TO_DATASET)) {
                    tryValidateDatasetUrl(expectedInputDataValue);
                } else if (constraint.getExpectedValueType().equals(LINK_TO_EXTERNAL_URL)) {
                    tryValidateResourceUrl(expectedInputDataValue);
                }
            } catch (MalformedURLException e) {
                throw new DpsTaskValidationException("Wrong input data: " + expectedInputDataValue);
            }
        }
    }

    private void tryValidateResourceUrl(String expectedInputDataValue) throws DpsTaskValidationException {
        UrlValidator urlValidator = new UrlValidator();
        if (!urlValidator.isValid(expectedInputDataValue)) {
            throw new DpsTaskValidationException("Wrong input data: " + expectedInputDataValue);
        }
    }

    private void tryValidateFileUrl(String expectedInputDataValue) throws MalformedURLException, DpsTaskValidationException {
        UrlParser parser = new UrlParser(expectedInputDataValue);
        if (!parser.isUrlToRepresentationVersionFile()) {
            throw new DpsTaskValidationException("Wrong input data: " + expectedInputDataValue);
        }
    }

    private void tryValidateDatasetUrl(String expectedInputDataValue) throws MalformedURLException, DpsTaskValidationException {
        UrlParser parser = new UrlParser(expectedInputDataValue);
        if (!parser.isUrlToDataset()) {
            throw new DpsTaskValidationException("Wrong input data: " + expectedInputDataValue);
        }
    }

    private void validateId(DpsTask task, DpsTaskConstraint constraint) throws DpsTaskValidationException {
        long taskId = task.getTaskId();
        if (constraint.getExpectedValue() == null) {  //any id
            return;
        }
        if (constraint.getExpectedValue().equals(taskId + "")) {//exacted id
            return;
        }
        throw new DpsTaskValidationException("Task id is not valid.");
    }

    private void validateOutputRevision(DpsTask task, boolean revisionMustExist) throws DpsTaskValidationException {
        Revision outputRevision = task.getOutputRevision();
        if (revisionMustExist) {
            if (outputRevision == null) {
                throw new DpsTaskValidationException("Output Revision should not be null!. It is required for this task");
            } else
                checkOutputRevisionContent(outputRevision);
        } else {
            if (outputRevision != null)
                checkOutputRevisionContent(outputRevision);
        }
    }

    private void checkOutputRevisionContent(Revision outputRevision) throws DpsTaskValidationException {
        if (outputRevision.getRevisionName() == null || outputRevision.getRevisionProviderId() == null) {
            throw new DpsTaskValidationException("Revision name and revision provider has to be not null");
        }

        if (outputRevision.getRevisionName().matches("\\s*") || outputRevision.getRevisionProviderId().matches("\\s*")) {
            throw new DpsTaskValidationException("Revision name and revision provider has to be non empty");
        }
    }
}

/**
 * Holds the definition of single constraint that should be fullfiled by dpsTask
 */
class DpsTaskConstraint {
    private DpsTaskFieldType fieldType;
    private Object expectedValue;
    private InputDataValueType expectedValueType;
    private String expectedName;

    private DpsTaskConstraint(Builder builder) {
        this.fieldType = builder.fieldType;
        this.expectedValue = builder.expectedValue;
        this.expectedValueType = builder.expectedValueType;
        this.expectedName = builder.expectedName;
    }

    public static Builder newDpsTaskConstraint() {
        return new Builder();
    }

    public Object getExpectedValue() {
        return expectedValue;
    }

    public InputDataValueType getExpectedValueType() {
        return expectedValueType;
    }

    public DpsTaskFieldType getFieldType() {
        return fieldType;
    }

    public String getExpectedName() {
        return expectedName;
    }



    public static final class Builder {
        private DpsTaskFieldType fieldType;
        private Object expectedValue;
        private InputDataValueType expectedValueType;
        private String expectedName;

        private Builder() {
        }

        public DpsTaskConstraint build() {
            return new DpsTaskConstraint(this);
        }

        public Builder fieldType(DpsTaskFieldType fieldType) {
            this.fieldType = fieldType;
            return this;
        }

        public Builder expectedValue(Object expectedValue) {
            this.expectedValue = expectedValue;
            return this;
        }

        public Builder expectedValueType(InputDataValueType expectedValueType) {
            this.expectedValueType = expectedValueType;
            return this;
        }

        public Builder expectedName(String expectedName) {
            this.expectedName = expectedName;
            return this;
        }
    }
}

enum DpsTaskFieldType {
    PARAMETER,
    INPUT_DATA,
    ID,
    NAME,
    OUTPUT_REVISION
}