package eu.europeana.cloud.service.dps.storm.service;

import eu.europeana.cloud.service.dps.storm.dao.HarvestedRecordsDAO;
import eu.europeana.cloud.service.dps.storm.incremental.CategorizationParameters;
import eu.europeana.cloud.service.dps.storm.incremental.CategorizationResult;
import eu.europeana.cloud.service.dps.storm.utils.HarvestedRecord;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

public abstract class HarvestedRecordCategorizationService {

    private final HarvestedRecordsDAO harvestedRecordsDAO;

    protected HarvestedRecordCategorizationService(HarvestedRecordsDAO harvestedRecordsDAO) {
        this.harvestedRecordsDAO = harvestedRecordsDAO;
    }

    public CategorizationResult categorize(CategorizationParameters categorizationParameters) {
        Optional<HarvestedRecord> harvestedRecord = readRecordFromDB(categorizationParameters);
        if (harvestedRecord.isEmpty()) {
            var newHarvestedRecord = prepareHarvestedRecordDefinition(categorizationParameters);
            addRecordDefinitionToDB(newHarvestedRecord);
            return categorizeRecordAsReadyForProcessing(categorizationParameters, null);
        } else {
            updateRecordLatestHarvestDate(harvestedRecord.get(), categorizationParameters.getCurrentHarvestDate());
            updateRecordLatestMd5(harvestedRecord.get(), categorizationParameters.getRecordMd5());
            if (isRecordEligibleForProcessing(harvestedRecord.get(), categorizationParameters)) {
                return categorizeRecordAsReadyForProcessing(categorizationParameters, harvestedRecord.get());
            } else {
                return categorizeRecordAsNotReadyForProcessing(categorizationParameters, harvestedRecord.get());
            }
        }
    }

    protected abstract boolean isRecordEligibleForProcessing(HarvestedRecord harvestedRecord, CategorizationParameters categorizationParameters);

    private Optional<HarvestedRecord> readRecordFromDB(CategorizationParameters categorizationParameters) {
        return harvestedRecordsDAO.findRecord(
                categorizationParameters.getDatasetId(),
                categorizationParameters.getRecordId());
    }

    private HarvestedRecord prepareHarvestedRecordDefinition(CategorizationParameters categorizationParameters) {
        return HarvestedRecord
                .builder()
                .metisDatasetId(categorizationParameters.getDatasetId())
                .recordLocalId(categorizationParameters.getRecordId())
                .latestHarvestDate(Date.from(categorizationParameters.getCurrentHarvestDate()))
                .latestHarvestMd5(categorizationParameters.getRecordMd5())
                .build();
    }

    private void addRecordDefinitionToDB(HarvestedRecord harvestedRecord) {
        harvestedRecordsDAO.insertHarvestedRecord(harvestedRecord);
    }

    private void updateRecordLatestHarvestDate(HarvestedRecord harvestedRecord, Instant currentHarvestDate) {
        harvestedRecord.setLatestHarvestDate(Date.from(currentHarvestDate));
        harvestedRecordsDAO.updateLatestHarvestDate(
                harvestedRecord.getMetisDatasetId(),
                harvestedRecord.getRecordLocalId(),
                harvestedRecord.getLatestHarvestDate());
    }

    private void updateRecordLatestMd5(HarvestedRecord harvestedRecord, UUID currentRecordMd5) {
        harvestedRecordsDAO.updateLatestHarvestMd5(
                harvestedRecord.getMetisDatasetId(),
                harvestedRecord.getRecordLocalId(),
                currentRecordMd5);
    }

    private CategorizationResult categorizeRecordAsReadyForProcessing(CategorizationParameters categorizationParameters, HarvestedRecord harvestedRecord) {
        return CategorizationResult
                .builder()
                .category(CategorizationResult.Category.ELIGIBLE_FOR_PROCESSING)
                .categorizationParameters(categorizationParameters)
                .harvestedRecord(harvestedRecord)
                .build();
    }

    private CategorizationResult categorizeRecordAsNotReadyForProcessing(CategorizationParameters categorizationParameters, HarvestedRecord harvestedRecord) {
        return CategorizationResult
                .builder()
                .category(CategorizationResult.Category.ALREADY_PROCESSED)
                .categorizationParameters(categorizationParameters)
                .harvestedRecord(harvestedRecord)
                .build();
    }
}